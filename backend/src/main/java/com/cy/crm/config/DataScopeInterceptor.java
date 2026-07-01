package com.cy.crm.config;

import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import com.cy.crm.common.exception.BusinessException;
import com.cy.crm.security.DataScope;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.Parenthesis;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SetOperationList;
import net.sf.jsqlparser.statement.select.SelectItem;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 数据权限拦截器
 * 在 SQL 查询时自动注入数据权限条件
 *
 * 核心设计：
 * 1. 使用 JSqlParser 解析 SQL 为 AST，通过 AST 节点操作注入权限条件，彻底消除字符串拼接 SQL 的注入风险。
 * 2. 通过反射修改 BoundSql.sql（MyBatis-Plus 官方 PaginationInnerInterceptor 亦采用此方式）。
 * 3. 仅处理 SELECT 语句，非 SELECT 直接放行。
 * 4. 失败时抛出 RuntimeException，防止无权限过滤的 SQL 被执行。
 */
@Slf4j
@Component
public class DataScopeInterceptor implements InnerInterceptor {

    /**
     * 表权限配置：表名 -> 该表支持的权限字段映射
     *
     * 说明：
     * - t_customer: 标准单位/区域/创建者模型
     * - t_opportunity: 配置保留原样
     * - t_order: 简化处理，使用 created_by 做 self-only 兜底
     * - t_project: 使用 owner_bd_id / sales_user_id 做权限控制
     * - t_rebate: 标准渠道/创建者模型
     */
    private static final TableConfig T_CUSTOMER = new TableConfig("t_customer",
            new FieldMapping("channel_id", FieldType.CHANNEL_ID),
            new FieldMapping("unit_id", FieldType.UNIT_ID),
            new FieldMapping("region", FieldType.REGION),
            new FieldMapping("owner_user_id", FieldType.OWNER),
            new FieldMapping("created_by", FieldType.CREATED_BY));

    private static final TableConfig T_PROJECT = new TableConfig("t_project",
            new FieldMapping("owner_bd_id", FieldType.OWNER),
            new FieldMapping("sales_user_id", FieldType.OWNER));

    private static final TableConfig[] TABLE_CONFIGS = {
            T_CUSTOMER,
            new TableConfig("t_opportunity",
                    new FieldMapping("channel_id", FieldType.CHANNEL_ID),
                    new FieldMapping("region", FieldType.REGION),
                    new FieldMapping("submitted_by", FieldType.CREATED_BY)),
            new TableConfig("t_order",
                    new FieldMapping("created_by", FieldType.CREATED_BY)),
            T_PROJECT,
            new TableConfig("t_rebate",
                    new FieldMapping("channel_id", FieldType.CHANNEL_ID),
                    new FieldMapping("owner_user_id", FieldType.OWNER),
                    new FieldMapping("created_by", FieldType.CREATED_BY))
    };

    /**
     * 字段类型枚举，用于决定使用哪种 DataScope 数据构建条件
     */
    private enum FieldType {
        CHANNEL_ID,   // 对应 DataScope.channelIds
        REGION,       // 对应 DataScope.regions
        UNIT_ID,      // 对应 DataScope.unitIds
        OWNER,        // 对应当前用户ID（self-only）
        CREATED_BY    // 对应当前用户ID（self-only）
    }


    /**
     * 字段映射：数据库字段名 -> 字段类型
     */
    private record FieldMapping(String columnName, FieldType fieldType) {
    }

    /**
     * 表配置：表名 + 该表支持的字段映射列表
     */
    private record TableConfig(
            String tableName,
            FieldMapping... fields
    ) {
    }

    @Override
    public void beforeQuery(Executor executor, MappedStatement ms, Object parameter,
                            RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) {
        String originalSql = boundSql.getSql();

        // 1. 获取当前用户的数据权限范围
        DataScope dataScope = getCurrentDataScope();
        if (dataScope == null) {
            // 延迟检查：只有在匹配到受控表时才拒绝，允许非受控表查询
        } else if (Boolean.TRUE.equals(dataScope.getAll())) {
            return; // 全部权限，不处理
        }

        // 2. 只处理 SELECT 语句（包括 UNION/CTE 等复杂查询）
        String trimmed = originalSql.trim();
        if (!isSelectQuery(trimmed)) {
            return;
        }

        // 3. 检查是否包含需要数据权限控制的表
        String lowerSql = originalSql.toLowerCase();
        TableConfig matchedConfig = null;
        for (TableConfig config : TABLE_CONFIGS) {
            if (matchesTableName(lowerSql, config.tableName())) {
                matchedConfig = config;
                break;
            }
        }
        if (matchedConfig == null) {
            return; // 无受控表，放行
        }

        // 3.1. 受控表查询必须有数据权限范围（防止无认证用户查询受控表）
        if (dataScope == null) {
            throw BusinessException.dataScopeDenied();
        }

        // 4. 使用 JSqlParser 解析 SQL 并注入权限条件
        String newSql;
        try {
            Statement statement = CCJSqlParserUtil.parse(originalSql);
            if (!(statement instanceof Select select)) {
                return; // 不是 SELECT，放行
            }

            // 只处理 PlainSelect（标准查询），拒绝 UNION/CTE 等复杂查询以确保安全
            if (!(select.getSelectBody() instanceof PlainSelect plainSelect)) {
                // UNION 查询使用 SetOperationList
                if (select.getSelectBody() instanceof SetOperationList) {
                    throw new RuntimeException("UNION 查询暂不支持数据权限过滤，请联系管理员或重写查询");
                }
                throw new RuntimeException("复杂查询暂不支持数据权限过滤");
            }

            // 检查 CTE（WITH 子句）- CTE 在 JSqlParser 中也返回 PlainSelect，但带有 withItemsList
            if (plainSelect.getWithItemsList() != null && !plainSelect.getWithItemsList().isEmpty()) {
                throw new RuntimeException("CTE (WITH) 查询暂不支持数据权限过滤，请联系管理员或重写查询");
            }

            // 构建数据权限条件 Expression（AST 节点）
            Expression dataScopeExpression = buildDataScopeExpression(dataScope, matchedConfig);
            if (dataScopeExpression == null) {
                return; // 没有可注入的条件
            }

            // 将条件注入 WHERE 子句
            Expression existingWhere = plainSelect.getWhere();
            if (existingWhere == null) {
                plainSelect.setWhere(dataScopeExpression);
            } else {
                // 已有 WHERE，用 AND 连接：(existing) AND (dataScope)
                plainSelect.setWhere(
                        new net.sf.jsqlparser.expression.operators.conditional.AndExpression(
                                new Parenthesis(existingWhere),
                                new Parenthesis(dataScopeExpression)
                        )
                );
            }

            newSql = statement.toString();
        } catch (JSQLParserException e) {
            log.error("Failed to parse SQL with JSqlParser, SQL: {}", originalSql, e);
            throw new RuntimeException("数据权限 SQL 解析失败，禁止执行", e);
        } catch (Exception e) {
            log.error("Failed to apply data scope to SQL: {}", originalSql, e);
            throw new RuntimeException("数据权限注入失败，禁止执行", e);
        }

        // 5. 通过反射修改 BoundSql 中的 SQL（MyBatis-Plus 官方亦采用此方式）
        try {
            java.lang.reflect.Field field = BoundSql.class.getDeclaredField("sql");
            field.setAccessible(true);
            field.set(boundSql, newSql);
            log.debug("Applied data scope to SQL: {}", newSql);
        } catch (Exception e) {
            log.error("Failed to modify BoundSql via reflection, SQL: {}", originalSql, e);
            throw new RuntimeException("数据权限 SQL 注入失败，禁止执行", e);
        }
    }

    /**
     * 获取当前用户的数据权限范围
     */
    private DataScope getCurrentDataScope() {
        try {
            Object authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication instanceof com.cy.crm.security.JwtAuthenticationToken jwtAuth) {
                return jwtAuth.getDataScope();
            }
        } catch (Exception e) {
            log.debug("Failed to get data scope from authentication: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 获取当前用户ID
     */
    private Long getCurrentUserId() {
        try {
            Object authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication instanceof com.cy.crm.security.JwtAuthenticationToken jwtAuth) {
                return jwtAuth.getUserId();
            }
        } catch (Exception e) {
            log.debug("Failed to get user id from authentication: {}", e.getMessage());
        }
        return -1L; // 默认返回不存在的用户ID，确保看不到数据
    }

    /**
     * 构建数据权限条件 Expression（JSqlParser AST 节点）
     *
     * 规则：
     * - selfOnly=true: 使用表配置中的 OWNER/CREATED_BY 字段，生成 column = userId
     * - channelIds 非空: 生成 channel_id IN (...)
     * - unitIds 非空: 生成 unit_id IN (...)
     * - regions 非空: 生成 region IN (...)
     * - 多条件之间用 OR 连接（取并集）
     *
     * @param dataScope 当前用户数据范围
     * @param config    匹配到的表配置，决定使用哪些字段
     * @return 构建好的 Expression，或 null 表示无需注入
     */
    private Expression buildDataScopeExpression(DataScope dataScope, TableConfig config) {
        List<Expression> conditions = new ArrayList<>();
        Long currentUserId = getCurrentUserId();

        // selfOnly 模式：只查看自己的数据
        if (Boolean.TRUE.equals(dataScope.getSelfOnly())) {
            for (FieldMapping field : config.fields()) {
                if (field.fieldType() == FieldType.OWNER || field.fieldType() == FieldType.CREATED_BY) {
                    // 使用第一个可用的 owner/created_by 字段
                    return new EqualsTo(
                            new Column(field.columnName()),
                            new LongValue(currentUserId)
                    );
                }
            }
            // 如果表配置中没有 owner/created_by 字段，兜底使用 created_by
            log.warn("Table {} has no owner/created_by field configured for self-only mode, " +
                    "defaulting to created_by = {}", config.tableName(), currentUserId);
            return new EqualsTo(new Column("created_by"), new LongValue(currentUserId));
        }

        // 非 selfOnly 模式：根据 DataScope 中的列表构建 IN 条件
        for (FieldMapping field : config.fields()) {
            switch (field.fieldType()) {
                case CHANNEL_ID -> {
                    if (dataScope.getChannelIds() != null && !dataScope.getChannelIds().isEmpty()) {
                        conditions.add(buildInExpression(field.columnName(), dataScope.getChannelIds(), true));
                    }
                }
                case UNIT_ID -> {
                    if (dataScope.getUnitIds() != null && !dataScope.getUnitIds().isEmpty()) {
                        conditions.add(buildInExpression(field.columnName(), dataScope.getUnitIds(), true));
                    }
                }
                case REGION -> {
                    if (dataScope.getRegions() != null && !dataScope.getRegions().isEmpty()) {
                        conditions.add(buildInExpression(field.columnName(), dataScope.getRegions(), false));
                    }
                }
                case OWNER, CREATED_BY -> {
                    // 非 selfOnly 模式下，OWNER/CREATED_BY 字段不参与列表条件
                    // 如果没有任何列表条件，这些字段可作为 self-only 兜底
                }
            }
        }

        if (conditions.isEmpty()) {
            // 没有任何列表条件，但也不是 selfOnly，使用 self-only 兜底（安全默认）
            for (FieldMapping field : config.fields()) {
                if (field.fieldType() == FieldType.OWNER || field.fieldType() == FieldType.CREATED_BY) {
                    log.debug("No list conditions for table {}, falling back to self-only on {}",
                            config.tableName(), field.columnName());
                    return new EqualsTo(
                            new Column(field.columnName()),
                            new LongValue(currentUserId)
                    );
                }
            }
            log.warn("No applicable data scope conditions for table {}, defaulting to created_by = {}",
                    config.tableName(), currentUserId);
            return new EqualsTo(new Column("created_by"), new LongValue(currentUserId));
        }

        // 多条件用 OR 连接
        return combineWithOr(conditions);
    }

    /**
     * 构建 IN 表达式（AST 节点）
     *
     * @param columnName  字段名
     * @param values      值列表
     * @param isNumeric   是否为数值类型（true 用 LongValue，false 用 StringValue）
     * @return InExpression AST 节点
     */
    private <T> InExpression buildInExpression(String columnName, List<T> values, boolean isNumeric) {
        List<Expression> expressionList = new ArrayList<>();
        for (T value : values) {
            if (isNumeric) {
                if (value instanceof Number number) {
                    expressionList.add(new LongValue(number.longValue()));
                } else {
                    expressionList.add(new LongValue(value.toString()));
                }
            } else {
                expressionList.add(new StringValue(value.toString()));
            }
        }

        InExpression inExpression = new InExpression();
        inExpression.setLeftExpression(new Column(columnName));
        inExpression.setRightExpression(new Parenthesis(new ExpressionList<>(expressionList)));
        return inExpression;
    }

    /**
     * 将多个 Expression 用 OR 连接，并添加 Parenthesis 保证优先级
     */
    private Expression combineWithOr(List<Expression> expressions) {
        if (expressions.size() == 1) {
            return new Parenthesis(expressions.get(0));
        }

        Expression result = new Parenthesis(expressions.get(0));
        for (int i = 1; i < expressions.size(); i++) {
            result = new OrExpression(result, new Parenthesis(expressions.get(i)));
        }
        return new Parenthesis(result);
    }

    /**
     * 精确匹配表名，避免 false positive
     *
     * 例如：t_order 不应匹配 t_order_items
     *
     * 匹配规则：
     * - FROM table_name
     * - JOIN table_name
     * - FROM table_name AS alias
     * - JOIN table_name AS alias
     *
     * @param lowerSql 小写的 SQL
     * @param tableName 要匹配的表名
     * @return 是否匹配
     */
    private boolean matchesTableName(String lowerSql, String tableName) {
        String lowerTableName = tableName.toLowerCase();
        // 使用正则表达式匹配表名边界
        // 匹配 FROM/JOIN 后面的表名，后面紧跟空格、AS、括号或语句结束
        String pattern = "(?:from|join)\\s+" + java.util.regex.Pattern.quote(lowerTableName) +
                        "(?:\\s|$|\\s+as\\s|\\)|\\s*,)";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern,
                java.util.regex.Pattern.CASE_INSENSITIVE);
        return p.matcher(lowerSql).find();
    }

    /**
     * 判断是否为 SELECT 查询（包括 UNION/CTE 等复杂查询）
     *
     * @param sql SQL 语句
     * @return 是否为 SELECT 查询
     */
    private boolean isSelectQuery(String sql) {
        if (sql.length() < 6) {
            return false;
        }
        String upperSql = sql.toUpperCase();
        // 标准的 SELECT ... 查询
        if (upperSql.startsWith("SELECT")) {
            return true;
        }
        // (SELECT ...) UNION ... - 括号包裹的 SELECT
        if (sql.startsWith("(") && upperSql.startsWith("(SELECT")) {
            return true;
        }
        // WITH cte AS (...) SELECT ... - CTE 查询
        if (upperSql.startsWith("WITH")) {
            return true;
        }
        return false;
    }
}
