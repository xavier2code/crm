package com.cy.crm.module.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "字典视图")
public class DictionaryVO {
    @Schema(description = "字典ID")
    private Long id;

    @Schema(description = "字典类型")
    private String type;

    @Schema(description = "字典编码")
    private String code;

    @Schema(description = "字典名称")
    private String name;

    @Schema(description = "排序")
    private Integer sort;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "是否预置")
    private Integer isBuiltin;
}
