package com.cy.crm.module.opportunity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotEmpty;

@Data
@Schema(description = "商机审批请求")
public class OpportunityApproveRequest {

    @NotNull(message = "操作类型不能为空")
    @Schema(description = "操作类型：2=通过 3=驳回")
    private Integer action;

    @Schema(description = "审批意见（驳回时必填且≥5字）")
    private String comment;
}
