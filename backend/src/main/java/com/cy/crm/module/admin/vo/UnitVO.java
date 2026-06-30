package com.cy.crm.module.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "单位视图")
public class UnitVO {
    @Schema(description = "单位ID")
    private Long id;

    @Schema(description = "单位名称")
    private String name;

    @Schema(description = "所属区域")
    private String region;

    @Schema(description = "行政级别：1省厅 2地市 3区县")
    private Integer adminLevel;

    @Schema(description = "地址")
    private String address;

    @Schema(description = "状态：1启用 0停用")
    private Integer status;
}
