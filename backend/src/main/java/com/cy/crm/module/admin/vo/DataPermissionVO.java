package com.cy.crm.module.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 用户数据权限视图。
 * scopeType 维度：1=业务域 2=区域 3=渠道 4=警种。
 */
@Data
@Schema(description = "用户数据权限视图")
public class DataPermissionVO {

    @Schema(description = "权限维度：1=业务域 2=区域 3=渠道 4=警种")
    private Integer scopeType;

    @Schema(description = "授权值列表（业务域/区域/警种=字典 code；渠道=渠道 id 字符串）")
    private List<String> scopeValues;
}
