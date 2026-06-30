package com.cy.crm.module.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "字典创建/编辑请求")
public class DictionaryRequest {
    private Long id;

    @NotBlank(message = "字典类型不能为空")
    @Schema(description = "字典类型")
    private String type;

    @NotBlank(message = "字典编码不能为空")
    @Schema(description = "字典编码")
    private String code;

    @NotBlank(message = "字典名称不能为空")
    @Schema(description = "字典名称")
    private String name;

    @Schema(description = "排序")
    private Integer sort = 0;

    @Schema(description = "备注")
    private String remark;
}
