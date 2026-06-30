package com.cy.crm.module.opportunity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import jakarta.validation.constraints.NotNull;

@Data
@Schema(description = "商机报备请求")
public class OpportunityRequest {

    @NotNull(message = "客户ID不能为空")
    @Schema(description = "客户ID")
    private Long customerId;

    @NotNull(message = "业务域不能为空")
    @Schema(description = "业务域")
    private String businessDomain;

    @NotNull(message = "项目类型不能为空")
    @Schema(description = "项目类型：1=新签 2=续签 3=试用")
    private Integer projectType;

    @Schema(description = "预计金额（万元）")
    private BigDecimal amount;
}
