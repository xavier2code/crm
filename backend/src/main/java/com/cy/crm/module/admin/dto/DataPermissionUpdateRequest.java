package com.cy.crm.module.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * 数据权限更新请求（按用户整组覆盖）
 *
 * 维度由 scopeType 字符串决定，取值见 DataScopeDimension 枚举：
 *   ALL / CHANNEL / REGION / UNIT / BUSINESS_DOMAIN / POLICE_TYPE / SELF
 *
 * scopeValue 存具体的字典 code（业务域/区域/警种）或渠道/单位 id（字符串形式）。
 */
@Data
@Schema(description = "用户数据权限更新请求")
public class DataPermissionUpdateRequest {

    @NotBlank(message = "scopeType 不能为空")
    @Schema(description = "权限维度 code：ALL / CHANNEL / REGION / UNIT / BUSINESS_DOMAIN / POLICE_TYPE / SELF")
    private String scopeType;

    @Schema(description = "授权值列表（业务域/区域/警种=字典 code；渠道/单位=id 字符串）")
    private List<String> scopeValues;
}
