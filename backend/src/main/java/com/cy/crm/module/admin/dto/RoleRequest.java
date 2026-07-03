package com.cy.crm.module.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "角色创建/编辑请求")
public class RoleRequest {
    private Long id;

    @NotBlank(message = "角色编码不能为空")
    @Schema(description = "角色编码")
    private String code;

    @NotBlank(message = "角色名称不能为空")
    @Schema(description = "角色名称")
    private String name;

    @Schema(description = "数据范围维度 code：ALL / CHANNEL / REGION / UNIT / BUSINESS_DOMAIN / POLICE_TYPE / SELF")
    private String dataScopeType = "SELF";

    @Schema(description = "角色关联的菜单 ID 列表")
    private java.util.List<Long> menuIds;

    @Schema(description = "角色关联的操作编码列表")
    private java.util.List<String> operationCodes;
}
