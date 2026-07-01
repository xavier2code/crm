package com.cy.crm.security;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.cy.crm.common.exception.BusinessException;
import com.cy.crm.module.admin.mapper.DataPermissionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 数据权限验证器
 * 用于在业务层验证用户是否有权访问特定资源
 *
 * 设计原则：
 * 1. 从认证上下文获取用户的 DataScope，不从数据库重建
 * 2. 验证单个资源访问权限，补充 DataScopeInterceptor 的列表级过滤
 * 3. 为 UPDATE/DELETE 操作提供资源级权限检查，防止 IDOR 漏洞
 */
@Component
@RequiredArgsConstructor
public class DataScopeValidator {

    private final DataPermissionMapper dataPermissionMapper;

    /**
     * 验证用户是否有访问指定资源的权限
     * 综合验证：支持 ALL、SELF_ONLY、UNIT 三种模式
     *
     * @param userId 当前用户ID
     * @param creatorId 资源创建者ID（用于 self-only 验证）
     * @param unitId 资源所属单位ID（用于 unit 验证）
     * @param dataScope 用户的数据权限范围
     */
    public void validateAccess(Long userId, Long creatorId, Long unitId, DataScope dataScope) {
        if (dataScope == null) {
            throw BusinessException.dataScopeDenied();
        }

        // 1. 全部数据权限 - 直接放行
        if (Boolean.TRUE.equals(dataScope.getAll())) {
            return;
        }

        // 2. Self-only 模式 - 只能访问自己创建的数据
        if (Boolean.TRUE.equals(dataScope.getSelfOnly())) {
            if (userId.equals(creatorId)) {
                return;
            }
            throw BusinessException.dataScopeDenied();
        }

        // 3. 有明确单位权限列表 - 验证 unitId 是否在列表中
        if (dataScope.getUnitIds() != null && !dataScope.getUnitIds().isEmpty()) {
            if (dataScope.getUnitIds().contains(unitId)) {
                return;
            }
            throw BusinessException.dataScopeDenied();
        }

        // 4. 其他权限类型（CHANNEL/REGION）无法验证 - 拒绝
        throw BusinessException.dataScopeDenied();
    }

    /**
     * 验证用户是否有访问指定单位数据的权限
     *
     * @param userId 当前用户ID
     * @param unitId 要访问的单位ID
     * @param dataScope 用户的数据权限范围
     */
    public void validateUnitAccess(Long userId, Long unitId, DataScope dataScope) {
        validateUnitAccess(userId, unitId, dataScope, false);
    }

    /**
     * 验证用户是否有访问指定单位数据的权限
     *
     * @param userId 当前用户ID
     * @param unitId 要访问的单位ID
     * @param dataScope 用户的数据权限范围
     * @param requireOwn 是否要求必须是自己所属单位的数据
     */
    public void validateUnitAccess(Long userId, Long unitId, DataScope dataScope, boolean requireOwn) {
        if (dataScope == null) {
            throw BusinessException.dataScopeDenied();
        }

        // 1. 全部数据权限 - 直接放行
        if (Boolean.TRUE.equals(dataScope.getAll())) {
            return;
        }

        // 2. Self-only 模式 + requireOwn - 验证用户是否属于该单位
        if (requireOwn && Boolean.TRUE.equals(dataScope.getSelfOnly())) {
            if (isUserOwnUnit(userId, unitId)) {
                return;
            }
            throw BusinessException.dataScopeDenied();
        }

        // 3. 有明确单位权限列表 - 验证 unitId 是否在列表中
        if (dataScope.getUnitIds() != null && !dataScope.getUnitIds().isEmpty()) {
            if (dataScope.getUnitIds().contains(unitId)) {
                return;
            }
            throw BusinessException.dataScopeDenied();
        }

        // 4. Self-only 模式但没有 requireOwn - 拒绝（需要明确的单位权限）
        if (Boolean.TRUE.equals(dataScope.getSelfOnly())) {
            throw BusinessException.dataScopeDenied();
        }

        // 5. 其他权限类型（CHANNEL/REGION）无法验证单位访问 - 拒绝
        throw BusinessException.dataScopeDenied();
    }

    /**
     * 验证用户是否有访问指定渠道数据的权限
     *
     * @param userId 当前用户ID
     * @param channelId 要访问的渠道ID
     * @param dataScope 用户的数据权限范围
     */
    public void validateChannelAccess(Long userId, Long channelId, DataScope dataScope) {
        if (dataScope == null) {
            throw BusinessException.dataScopeDenied();
        }

        // 1. 全部数据权限 - 直接放行
        if (Boolean.TRUE.equals(dataScope.getAll())) {
            return;
        }

        // 2. 有明确渠道权限列表 - 验证 channelId 是否在列表中
        if (dataScope.getChannelIds() != null && !dataScope.getChannelIds().isEmpty()) {
            if (dataScope.getChannelIds().contains(channelId)) {
                return;
            }
            throw BusinessException.dataScopeDenied();
        }

        // 3. 其他权限类型（UNIT/REGION/SELF）无法验证渠道访问 - 拒绝
        throw BusinessException.dataScopeDenied();
    }

    /**
     * 验证用户是否有访问指定创建者数据的权限
     * 用于基于 created_by 字段的访问控制
     *
     * @param userId 当前用户ID
     * @param creatorId 资源的创建者ID
     * @param dataScope 用户的数据权限范围
     */
    public void validateCreatorAccess(Long userId, Long creatorId, DataScope dataScope) {
        if (dataScope == null) {
            throw BusinessException.dataScopeDenied();
        }

        // 1. 全部数据权限 - 直接放行
        if (Boolean.TRUE.equals(dataScope.getAll())) {
            return;
        }

        // 2. Self-only 模式 - 只能访问自己创建的数据
        if (Boolean.TRUE.equals(dataScope.getSelfOnly())) {
            if (userId.equals(creatorId)) {
                return;
            }
            throw BusinessException.dataScopeDenied();
        }

        // 3. 其他权限类型 - 无法通过 creatorId 验证，需要有其他维度（unit/channel/region）
        // 对于只有 unit/channel/region 权限的用户，应该使用对应的验证方法
        throw BusinessException.dataScopeDenied();
    }

    /**
     * 检查用户是否属于指定单位
     *
     * @param userId 用户ID
     * @param unitId 单位ID
     * @return 是否属于该单位
     */
    private boolean isUserOwnUnit(Long userId, Long unitId) {
        // 检查用户是否有该单位的数据权限
        Long count = dataPermissionMapper.selectCount(
                new QueryWrapper<com.cy.crm.module.admin.entity.DataPermission>()
                        .eq("user_id", userId)
                        .eq("scope_type", 4) // UNIT 类型
                        .eq("scope_value", String.valueOf(unitId))
        );
        return count != null && count > 0;
    }
}
