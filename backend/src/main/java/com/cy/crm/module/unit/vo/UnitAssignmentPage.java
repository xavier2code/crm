package com.cy.crm.module.unit.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "单位分配分页")
public class UnitAssignmentPage {

    @Schema(description = "分配记录列表")
    private List<UnitAssignmentVO> records;

    @Schema(description = "总数")
    private Long total;

    @Schema(description = "当前页")
    private Long current;

    @Schema(description = "每页大小")
    private Long size;
}
