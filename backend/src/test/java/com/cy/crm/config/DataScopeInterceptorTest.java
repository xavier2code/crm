package com.cy.crm.config;

import com.cy.crm.security.DataScope;
import com.cy.crm.security.JwtAuthenticationToken;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 数据权限拦截器单元测试
 * 验证 EXISTS 子查询正确注入到 t_contract / t_follow_up 查询中
 */
@ExtendWith(MockitoExtension.class)
class DataScopeInterceptorTest {

    @InjectMocks
    private DataScopeInterceptor interceptor;

    @Mock
    private Executor executor;
    @Mock
    private MappedStatement mappedStatement;
    @Mock
    private ResultHandler<?> resultHandler;

    private static final Long CURRENT_USER_ID = 42L;

    @BeforeEach
    void setUp() {
        setAuthentication(selfOnlyScope());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldInjectExistsForContract_withSelfOnly() {
        String sql = "SELECT * FROM t_contract WHERE status = 1";
        BoundSql boundSql = newBoundSql(sql);

        interceptor.beforeQuery(executor, mappedStatement, null, null, resultHandler, boundSql);

        String newSql = boundSql.getSql().toLowerCase();
        assertTrue(newSql.contains("exists"), "应注入 EXISTS 子查询");
        assertTrue(newSql.contains("t_project"), "应关联 t_project");
        assertTrue(newSql.contains("project_id"), "应使用 project_id 关联");
        assertTrue(newSql.contains("owner_bd_id"), "应使用 owner_bd_id 做 self-only 过滤");
        assertFalse(newSql.contains("t_contract.created_by"), "不应引用 t_contract 不存在的 created_by");
    }

    @Test
    void shouldInjectExistsForFollowUp_withSelfOnly() {
        String sql = "SELECT * FROM t_follow_up WHERE customer_id = 10 ORDER BY follow_up_date DESC";
        BoundSql boundSql = newBoundSql(sql);

        interceptor.beforeQuery(executor, mappedStatement, null, null, resultHandler, boundSql);

        String newSql = boundSql.getSql().toLowerCase();
        assertTrue(newSql.contains("exists"), "应注入 EXISTS 子查询");
        assertTrue(newSql.contains("t_customer"), "应关联 t_customer");
        assertTrue(newSql.contains("customer_id"), "应使用 customer_id 关联");
        assertTrue(newSql.contains("owner_user_id"), "应使用 owner_user_id 做 self-only 过滤");
        assertFalse(newSql.contains("t_follow_up.created_by"), "不应在 WHERE 中使用 t_follow_up.created_by 兜底");
    }

    @Test
    void shouldInjectExistsForFollowUp_withUnitIds() {
        setAuthentication(unitScope(List.of(1L, 2L)));
        String sql = "SELECT * FROM t_follow_up";
        BoundSql boundSql = newBoundSql(sql);

        interceptor.beforeQuery(executor, mappedStatement, null, null, resultHandler, boundSql);

        String newSql = boundSql.getSql().toLowerCase();
        assertTrue(newSql.contains("exists"), "应注入 EXISTS 子查询");
        assertTrue(newSql.contains("unit_id"), "应在子查询中注入 unit_id 条件");
        assertTrue(newSql.contains("unit_id in (1, 2)"), "应生成 IN (1, 2) 列表");
    }

    @Test
    void shouldInjectExistsForFollowUp_withRegions() {
        setAuthentication(regionScope(List.of("湖北", "湖南")));
        String sql = "SELECT * FROM t_follow_up";
        BoundSql boundSql = newBoundSql(sql);

        interceptor.beforeQuery(executor, mappedStatement, null, null, resultHandler, boundSql);

        String newSql = boundSql.getSql().toLowerCase();
        assertTrue(newSql.contains("exists"), "应注入 EXISTS 子查询");
        assertTrue(newSql.contains("region in"), "应在子查询中注入 region IN 条件");
        assertTrue(newSql.contains("'湖北'") && newSql.contains("'湖南'"), "应包含区域值");
    }

    @Test
    void shouldHandleTableAlias() {
        String sql = "SELECT c.* FROM t_contract c WHERE c.status = 1";
        BoundSql boundSql = newBoundSql(sql);

        interceptor.beforeQuery(executor, mappedStatement, null, null, resultHandler, boundSql);

        String newSql = boundSql.getSql();
        assertTrue(newSql.contains("c.project_id"), "EXISTS 子查询应使用子表别名 c");
    }

    @Test
    void shouldNotModify_whenAllPermission() {
        setAuthentication(DataScope.all());
        String sql = "SELECT * FROM t_contract";
        BoundSql boundSql = newBoundSql(sql);

        interceptor.beforeQuery(executor, mappedStatement, null, null, resultHandler, boundSql);

        assertEquals(sql, boundSql.getSql(), "全部权限时不应修改 SQL");
    }

    @Test
    void shouldNotModify_whenNonScopeTable() {
        String sql = "SELECT * FROM t_unrelated_table";
        BoundSql boundSql = newBoundSql(sql);

        interceptor.beforeQuery(executor, mappedStatement, null, null, resultHandler, boundSql);

        assertEquals(sql, boundSql.getSql(), "非受控表不应修改 SQL");
    }

    @Test
    void shouldStillBeValidSql_afterInjection() throws Exception {
        String sql = "SELECT * FROM t_contract WHERE status = 1";
        BoundSql boundSql = newBoundSql(sql);

        interceptor.beforeQuery(executor, mappedStatement, null, null, resultHandler, boundSql);

        // 验证注入后的 SQL 仍能被 JSqlParser 解析
        Select select = (Select) CCJSqlParserUtil.parse(boundSql.getSql());
        assertNotNull(select.getSelectBody());
        PlainSelect plainSelect = (PlainSelect) select.getSelectBody();
        assertNotNull(plainSelect.getWhere(), "注入后应具备 WHERE 子句");
    }

    private BoundSql newBoundSql(String sql) {
        return new BoundSql(new Configuration(), sql, Collections.emptyList(), null);
    }

    private void setAuthentication(DataScope dataScope) {
        JwtAuthenticationToken auth = new JwtAuthenticationToken(
                "test_user",
                Collections.emptyList(),
                CURRENT_USER_ID,
                Collections.emptyList(),
                Collections.emptyList(),
                dataScope
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private DataScope selfOnlyScope() {
        DataScope scope = new DataScope();
        scope.setSelfOnly(true);
        return scope;
    }

    private DataScope unitScope(List<Long> unitIds) {
        DataScope scope = new DataScope();
        scope.setUnitIds(unitIds);
        return scope;
    }

    private DataScope regionScope(List<String> regions) {
        DataScope scope = new DataScope();
        scope.setRegions(regions);
        return scope;
    }

    // ========== Security Issue Tests ==========

    @Test
    void shouldRejectQuery_whenNoAuthentication() {
        SecurityContextHolder.clearContext();
        String sql = "SELECT * FROM t_customer";
        BoundSql boundSql = newBoundSql(sql);

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                interceptor.beforeQuery(executor, mappedStatement, null, null, resultHandler, boundSql)
        );
        assertTrue(ex.getMessage().contains("未授权") || ex.getMessage().contains("数据权限"), "应拒绝无认证用户的查询");
    }

    @Test
    void shouldNotFalseMatch_similarTableNames() {
        String sql = "SELECT * FROM t_order_items WHERE id = 1";
        BoundSql boundSql = newBoundSql(sql);

        interceptor.beforeQuery(executor, mappedStatement, null, null, resultHandler, boundSql);

        // t_order_items 不是受控表，SQL 不应被修改
        // 由于存在 t_order 配置，如果使用 contains 会误匹配
        String newSql = boundSql.getSql();
        assertEquals(sql, newSql, "t_order_items 不应被 t_order 配置误匹配");
    }

    @Test
    void shouldHandleUnionQuery() {
        String sql = "(SELECT * FROM t_customer) UNION (SELECT * FROM t_customer)";
        BoundSql boundSql = newBoundSql(sql);

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                interceptor.beforeQuery(executor, mappedStatement, null, null, resultHandler, boundSql)
        );
        assertTrue(ex.getMessage().contains("UNION") || ex.getMessage().contains("暂不支持") || ex.getMessage().contains("数据权限"), "UNION 查询应被处理或拒绝");
    }

    @Test
    void shouldHandleCteQuery() {
        String sql = "WITH cte AS (SELECT * FROM t_customer) SELECT * FROM cte";
        BoundSql boundSql = newBoundSql(sql);

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                interceptor.beforeQuery(executor, mappedStatement, null, null, resultHandler, boundSql)
        );
        assertTrue(ex.getMessage().contains("CTE") || ex.getMessage().contains("WITH") || ex.getMessage().contains("暂不支持") || ex.getMessage().contains("数据权限"), "CTE 查询应被处理或拒绝");
    }
}
