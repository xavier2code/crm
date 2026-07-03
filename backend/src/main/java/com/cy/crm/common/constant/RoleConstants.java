package com.cy.crm.common.constant;

import lombok.experimental.UtilityClass;

/**
 * 角色编码常量
 * 用于替代 @PreAuthorize 中的硬编码字符串
 *
 * 符合开发文档 §22 角色-菜单/操作编码体系
 */
@UtilityClass
public class RoleConstants {

    /**
     * 系统管理员
     */
    public static final String ADMIN = "ADMIN";

    /**
     * CYBD（内部管理员 + 报备审批）
     */
    public static final String CYBD = "CYBD";

    /**
     * 大区总
     */
    public static final String REGION_HEAD = "REGION_HEAD";

    /**
     * 渠道负责人
     */
    public static final String CHANNEL_HEAD = "CHANNEL_HEAD";

    /**
     * 渠道 BD
     */
    public static final String CHANNEL_BD = "CHANNEL_BD";

    /**
     * 橙鹰销售（仅查看，无审批权）
     */
    public static final String ORANGE_EAGLE_SALES = "ORANGE_EAGLE_SALES";

    /**
     * 橙鹰负责人（单位主数据维护）
     */
    public static final String ORANGE_EAGLE_HEAD = "ORANGE_EAGLE_HEAD";

    /**
     * 财务
     */
    public static final String FINANCE = "FINANCE";
}
