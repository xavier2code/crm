package com.cy.crm.module.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "单位创建/编辑请求")
public class UnitRequest {
    private Long id;

    @NotBlank(message = "单位名称不能为空")
    @Schema(description = "单位名称")
    private String name;

    @Schema(description = "所属区域")
    private String region;

    @NotNull(message = "行政级别不能为空")
    @Schema(description = "行政级别：1省厅 2地市 3区县")
    private Integer adminLevel;

    @Schema(description = "地址")
    private String address;

    @Schema(description = "状态：1启用 0停用")
    private Integer status = 1;
}
