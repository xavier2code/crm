package com.cy.crm.module.sales_team.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 销售梯队配置响应 VO
 */
@Data
@Schema(description = "销售梯队配置响应")
public class SalesTeamConfigVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "梯队编码")
    private String teamCode;

    @Schema(description = "梯队名称")
    private String teamName;

    @Schema(description = "区域编码")
    private String regionCode;

    @Schema(description = "区域名称")
    private String regionName;

    @Schema(description = "包含的单位 code，逗号分隔")
    private String unitCodes;

    @Schema(description = "排序")
    private Integer sort;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "有效期起")
    private LocalDate effectiveFrom;

    @Schema(description = "有效期止")
    private LocalDate effectiveTo;

    @Schema(description = "创建人 ID")
    private Long createdBy;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
