package com.cy.crm.module.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 数据权限更新请求（按用户整组覆盖）
 *
 * 设计：scopeType 1=业务域 2=区域 3=渠道 4=警种，
 * scopeValue 存具体的字典 code 或渠道 id（字符串形式）。
 */
@Data
@Schema(description = "用户数据权限更新请求")
public class DataPermissionUpdateRequest {

    @NotNull(message = "scopeType 不能为空")
    @Schema(description = "权限维度：1=业务域 2=区域 3=渠道 4=警种")
    private Integer scopeType;

    @Schema(description = "授权值列表（业务域/区域/警种=字典 code；渠道=渠道 id 字符串）")
    private List<String> scopeValues;
}
