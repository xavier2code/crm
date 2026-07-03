package com.cy.crm.security;

import java.util.Arrays;

/**
 * 数据权限维度枚举。
 *
 * 该枚举是数据权限维度的单一事实来源（SSOT）。
 * - t_data_permission.scope_type 列以 code 字符串存储
 * - t_role.data_scope_type 列以 code 字符串存储
 * - DataScope 对象用对应 List 字段承载值
 * - DataScopeInterceptor 仅识别 SQL 可用维度：CHANNEL / REGION / UNIT / SELF / ALL
 * - BUSINESS_DOMAIN / POLICE_TYPE 由 service 层应用层白名单过滤，不参与 SQL 注入
 *
 * 历史数字枚举（1=业务域/2=区域/3=渠道/4=警种/1=ALL/2=CHANNEL/3=REGION/4=DEPT/5=SELF）
 * 之间的歧义已统一为以下 code 字符串。V16 脚本完成历史数据迁移。
 */
public enum DataScopeDimension {
    /** 全部数据（SQL: 不注入条件） */
    ALL("ALL", "全部"),
    /** 渠道维度（SQL: channel_id IN (...)） */
    CHANNEL("CHANNEL", "渠道"),
    /** 区域维度（SQL: region IN (...)） */
    REGION("REGION", "区域"),
    /** 单位维度（SQL: unit_id IN (...)） */
    UNIT("UNIT", "单位"),
    /** 业务域维度（应用层白名单过滤） */
    BUSINESS_DOMAIN("BUSINESS_DOMAIN", "业务域"),
    /** 警种维度（应用层白名单过滤） */
    POLICE_TYPE("POLICE_TYPE", "警种"),
    /** 仅限本人数据（SQL: created_by/owner = currentUser） */
    SELF("SELF", "本人");

    private final String code;
    private final String label;

    DataScopeDimension(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    /**
     * 根据 code 字符串解析维度。未匹配返回 null。
     *
     * 兼容历史数字字符串，仅在 V16 迁移尚未完成时生效：
     * - "1" -> ALL（歧义：曾被当作"业务域"，按"最小特权"原则取 ALL，迁移完成后不会命中）
     * - "2" -> CHANNEL
     * - "3" -> REGION
     * - "4" -> UNIT（V2 字典曾定义为 DEPT；语义上等同于单位）
     * - "5" -> SELF
     */
    public static DataScopeDimension fromCode(String code) {
        if (code == null) {
            return null;
        }
        String trimmed = code.trim();
        for (DataScopeDimension d : values()) {
            if (d.code.equalsIgnoreCase(trimmed)) {
                return d;
            }
        }
        return switch (trimmed) {
            case "1" -> ALL;
            case "2" -> CHANNEL;
            case "3" -> REGION;
            case "4" -> UNIT;
            case "5" -> SELF;
            default -> null;
        };
    }

    /** 是否为 SQL 可注入维度（DataScopeInterceptor 唯一识别的维度） */
    public boolean isSqlApplicable() {
        return this == ALL || this == CHANNEL || this == REGION || this == UNIT || this == SELF;
    }
}
