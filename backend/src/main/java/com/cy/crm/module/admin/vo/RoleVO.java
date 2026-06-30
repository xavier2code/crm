package com.cy.crm.module.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "角色视图")
public class RoleVO {
    @Schema(description = "角色ID")
    private Long id;

    @Schema(description = "角色编码")
    private String code;

    @Schema(description = "角色名称")
    private String name;

    @Schema(description = "是否内置")
    private Integer isBuiltin;

    @Schema(description = "数据范围类型")
    private Integer dataScopeType;
}
