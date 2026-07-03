package com.cy.crm.security;

import com.cy.crm.module.admin.entity.DataPermission;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 数据权限范围对象 - 运行时权限控制 + JWT claims
 *
 * 维度与 DataScopeDimension 枚举一一对应。
 * 字段语义：
 * - channelIds:           渠道 ID 列表（SQL: channel_id IN (...)）
 * - regions:              区域代码列表（SQL: region IN (...)）
 * - unitIds:              单位 ID 列表（SQL: unit_id IN (...)）
 * - businessDomainCodes:  业务域代码列表（应用层白名单过滤）
 * - policeTypeCodes:      警种代码列表（应用层白名单过滤）
 * - selfOnly:             仅可见自己创建的数据（SQL: created_by/owner = currentUser）
 * - all:                  全部数据权限（不注入任何条件）
 */
@Data
public class DataScope {

    private List<Long> channelIds = new ArrayList<>();
    private List<String> regions = new ArrayList<>();
    private List<Long> unitIds = new ArrayList<>();
    private List<String> businessDomainCodes = new ArrayList<>();
    private List<String> policeTypeCodes = new ArrayList<>();
    private Boolean selfOnly = false;
    private Boolean all = false;

    /**
     * 创建空权限范围（默认 selfOnly - 安全兜底，无授权时仅可看自己数据）
     */
    public static DataScope empty() {
        DataScope scope = new DataScope();
        scope.setSelfOnly(true);
        return scope;
    }

    /**
     * 创建全部权限范围
     */
    public static DataScope all() {
        DataScope scope = new DataScope();
        scope.setAll(true);
        return scope;
    }

    /**
     * 从 t_data_permission 记录构建 DataScope。
     * 遇到未知的 scopeType code 将被忽略（绝不降级为 ALL）。
     */
    public static DataScope fromPermissions(List<DataPermission> permissions) {
        DataScope scope = new DataScope();
        if (permissions == null || permissions.isEmpty()) {
            scope.setSelfOnly(true);
            return scope;
        }
        boolean anyMatched = false;
        for (DataPermission dp : permissions) {
            if (dp.getScopeType() == null) {
                continue;
            }
            DataScopeDimension dimension = DataScopeDimension.fromCode(dp.getScopeType());
            if (dimension == null) {
                continue;
            }
            anyMatched = true;
            String value = dp.getScopeValue();
            switch (dimension) {
                case ALL -> {
                    scope.setAll(true);
                    return scope;
                }
                case CHANNEL -> {
                    Long id = parseLongOrNull(value);
                    if (id != null) {
                        scope.getChannelIds().add(id);
                    }
                }
                case REGION -> {
                    if (value != null && !value.isBlank()) {
                        scope.getRegions().add(value.trim());
                    }
                }
                case UNIT -> {
                    Long id = parseLongOrNull(value);
                    if (id != null) {
                        scope.getUnitIds().add(id);
                    }
                }
                case BUSINESS_DOMAIN -> {
                    if (value != null && !value.isBlank()) {
                        scope.getBusinessDomainCodes().add(value.trim());
                    }
                }
                case POLICE_TYPE -> {
                    if (value != null && !value.isBlank()) {
                        scope.getPoliceTypeCodes().add(value.trim());
                    }
                }
                case SELF -> scope.setSelfOnly(true);
            }
        }
        if (!anyMatched) {
            // 所有行都是未知/空 scopeType，安全兜底
            scope.setSelfOnly(true);
        }
        return scope;
    }

    private static Long parseLongOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
