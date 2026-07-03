package com.cy.crm.module.reimbursement.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "报销审批请求")
public class ReimbursementApproveRequest {

    @NotNull(message = "审批结果不能为空")
    @Pattern(regexp = "^(APPROVED|REJECTED)$", message = "审批结果必须为 APPROVED 或 REJECTED")
    @Schema(description = "审批结果：APPROVED=通过 REJECTED=驳回")
    private String result;

    @Size(max = 1000, message = "审批意见不能超过 1000 字")
    @Schema(description = "审批意见")
    private String comment;
}
