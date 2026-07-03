package com.cy.crm.module.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 用户数据权限视图。
 * scopeType code 与 DataScopeDimension 枚举一致。
 */
@Data
@Schema(description = "用户数据权限视图")
public class DataPermissionVO {

    @Schema(description = "权限维度 code：ALL / CHANNEL / REGION / UNIT / BUSINESS_DOMAIN / POLICE_TYPE / SELF")
    private String scopeType;

    @Schema(description = "权限维度中文标签")
    private String scopeTypeLabel;

    @Schema(description = "授权值列表（业务域/区域/警种=字典 code；渠道/单位=id 字符串）")
    private List<String> scopeValues;
}
