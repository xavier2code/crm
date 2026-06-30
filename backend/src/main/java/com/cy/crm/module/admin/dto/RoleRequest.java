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

    @Schema(description = "数据范围类型：1全部 2本渠道 3本区域 4本部门 5本人")
    private Integer dataScopeType = 1;
}
