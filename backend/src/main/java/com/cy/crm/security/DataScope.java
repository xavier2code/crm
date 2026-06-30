package com.cy.crm.security;

import lombok.Data;
import java.util.List;

/**
 * 数据权限范围对象
 * 用于JWT claims和运行时权限控制
 */
@Data
public class DataScope {
    /**
     * 可访问的渠道ID列表
     */
    private List<Long> channelIds;

    /**
     * 可访问的区域列表
     */
    private List<String> regions;

    /**
     * 可访问的单位ID列表
     */
    private List<Long> unitIds;

    /**
     * 是否仅限自己创建的数据
     */
    private Boolean selfOnly;

    /**
     * 是否拥有全部数据权限
     */
    private Boolean all;

    /**
     * 创建空权限范围（无权限）
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
}
