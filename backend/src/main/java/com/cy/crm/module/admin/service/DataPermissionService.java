package com.cy.crm.module.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.cy.crm.common.exception.BusinessException;
import com.cy.crm.module.admin.entity.DataPermission;
import com.cy.crm.module.admin.entity.User;
import com.cy.crm.module.admin.mapper.DataPermissionMapper;
import com.cy.crm.module.admin.mapper.UserMapper;
import com.cy.crm.module.admin.vo.DataPermissionVO;
import com.cy.crm.security.DataScopeDimension;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 用户数据权限服务
 *
 * 维度由 DataScopeDimension 枚举定义，作为数据权限的单一事实来源。
 * 存储结构：t_data_permission (user_id, scope_type, scope_value) —— 通用三元组
 *  - scope_type: VARCHAR，存 DataScopeDimension.code
 *  - scope_value: VARCHAR，存具体字典 code（业务域/区域/警种）或 id 字符串（渠道/单位）
 */
@Service
@RequiredArgsConstructor
public class DataPermissionService {

    private final DataPermissionMapper dataPermissionMapper;
    private final UserMapper userMapper;

    /**
     * 列出指定用户在所有 7 个维度上的授权值，便于前端按维度渲染。
     */
    public List<DataPermissionVO> listByUser(Long userId) {
        ensureUserExists(userId);
        List<DataPermission> records = dataPermissionMapper.selectList(
                new QueryWrapper<DataPermission>().eq("user_id", userId));
        Map<String, List<String>> grouped = records.stream()
                .filter(r -> r.getScopeType() != null)
                .collect(Collectors.groupingBy(
                        DataPermission::getScopeType,
                        Collectors.mapping(
                                r -> r.getScopeValue() == null ? "" : r.getScopeValue(),
                                Collectors.toList())));
        List<DataPermissionVO> result = new ArrayList<>();
        for (DataScopeDimension d : DataScopeDimension.values()) {
            DataPermissionVO vo = new DataPermissionVO();
            vo.setScopeType(d.getCode());
            vo.setScopeTypeLabel(d.getLabel());
            vo.setScopeValues(grouped.getOrDefault(d.getCode(), Collections.emptyList()));
            result.add(vo);
        }
        return result;
    }

    /**
     * 按 (userId, scopeType) 整组覆盖授权值。
     * scopeType 必须为 DataScopeDimension.code；scopeValues 为空表示清空该维度。
     */
    @Transactional(rollbackFor = Exception.class)
    public void replaceScope(Long userId, String scopeType, List<String> scopeValues) {
        ensureUserExists(userId);
        DataScopeDimension dimension = DataScopeDimension.fromCode(scopeType);
        if (dimension == null) {
            throw BusinessException.paramError("不支持的 scopeType: " + scopeType);
        }
        // ALL / SELF 是无值维度，传值也忽略
        dataPermissionMapper.delete(new QueryWrapper<DataPermission>()
                .eq("user_id", userId)
                .eq("scope_type", dimension.getCode()));
        if (dimension == DataScopeDimension.ALL || dimension == DataScopeDimension.SELF) {
            // ALL/SELF 仅作为"标记行"存在一条，scope_value 留空字符串
            DataPermission dp = new DataPermission();
            dp.setUserId(userId);
            dp.setScopeType(dimension.getCode());
            dp.setScopeValue("");
            dataPermissionMapper.insert(dp);
            return;
        }
        if (scopeValues == null || scopeValues.isEmpty()) {
            return;
        }
        for (String value : scopeValues) {
            if (value == null || value.isBlank()) {
                continue;
            }
            DataPermission dp = new DataPermission();
            dp.setUserId(userId);
            dp.setScopeType(dimension.getCode());
            dp.setScopeValue(value.trim());
            dataPermissionMapper.insert(dp);
        }
    }

    private void ensureUserExists(Long userId) {
        if (userId == null) {
            throw BusinessException.paramError("用户ID不能为空");
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw BusinessException.resourceNotFound("用户");
        }
    }
}
