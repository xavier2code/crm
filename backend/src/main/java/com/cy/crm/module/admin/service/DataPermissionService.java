package com.cy.crm.module.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.cy.crm.common.exception.BusinessException;
import com.cy.crm.module.admin.entity.DataPermission;
import com.cy.crm.module.admin.entity.User;
import com.cy.crm.module.admin.mapper.DataPermissionMapper;
import com.cy.crm.module.admin.mapper.UserMapper;
import com.cy.crm.module.admin.vo.DataPermissionVO;
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
 * 维度定义：1=业务域 2=区域 3=渠道 4=警种
 * 存储结构：t_data_permission (user_id, scope_type, scope_value) —— 通用三元组
 */
@Service
@RequiredArgsConstructor
public class DataPermissionService {

    private final DataPermissionMapper dataPermissionMapper;
    private final UserMapper userMapper;

    /** 业务域 */
    public static final int SCOPE_BUSINESS_DOMAIN = 1;
    /** 区域 */
    public static final int SCOPE_REGION = 2;
    /** 渠道 */
    public static final int SCOPE_CHANNEL = 3;
    /** 警种 */
    public static final int SCOPE_POLICE_TYPE = 4;

    public List<DataPermissionVO> listByUser(Long userId) {
        ensureUserExists(userId);
        List<DataPermission> records = dataPermissionMapper.selectList(
                new QueryWrapper<DataPermission>().eq("user_id", userId));
        Map<Integer, List<String>> grouped = records.stream().collect(
                Collectors.groupingBy(
                        DataPermission::getScopeType,
                        Collectors.mapping(DataPermission::getScopeValue, Collectors.toList())));
        List<DataPermissionVO> result = new ArrayList<>();
        // 固定输出 4 个维度，便于前端按维度渲染
        for (int type : new int[]{SCOPE_BUSINESS_DOMAIN, SCOPE_REGION, SCOPE_CHANNEL, SCOPE_POLICE_TYPE}) {
            DataPermissionVO vo = new DataPermissionVO();
            vo.setScopeType(type);
            vo.setScopeValues(grouped.getOrDefault(type, Collections.emptyList()));
            result.add(vo);
        }
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public void replaceScope(Long userId, Integer scopeType, List<String> scopeValues) {
        ensureUserExists(userId);
        validateScopeType(scopeType);
        // 先删后插（整组覆盖）
        dataPermissionMapper.delete(new QueryWrapper<DataPermission>()
                .eq("user_id", userId)
                .eq("scope_type", scopeType));
        if (scopeValues == null || scopeValues.isEmpty()) {
            return;
        }
        for (String value : scopeValues) {
            if (value == null || value.isBlank()) {
                continue;
            }
            DataPermission dp = new DataPermission();
            dp.setUserId(userId);
            dp.setScopeType(scopeType);
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

    private void validateScopeType(Integer scopeType) {
        if (scopeType == null) {
            throw BusinessException.paramError("scopeType 不能为空");
        }
        if (scopeType != SCOPE_BUSINESS_DOMAIN
                && scopeType != SCOPE_REGION
                && scopeType != SCOPE_CHANNEL
                && scopeType != SCOPE_POLICE_TYPE) {
            throw BusinessException.paramError("不支持的 scopeType: " + scopeType);
        }
    }
}
