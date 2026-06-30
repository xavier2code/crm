package com.cy.crm.module.followup.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

@Data
@Schema(description = "跟进记录请求")
public class FollowUpRequest {

    @NotNull(message = "客户ID不能为空")
    @Schema(description = "客户ID")
    private Long customerId;

    @Schema(description = "项目ID")
    private Long projectId;

    @Schema(description = "商机ID")
    private Long opportunityId;

    @Schema(description = "当前阶段")
    private String currentStage;

    @Schema(description = "下一步阶段")
    private String nextStage;

    @Schema(description = "阶段反馈")
    private String stageFeedback;

    @NotNull(message = "跟进日期不能为空")
    @Schema(description = "跟进日期")
    private LocalDate followUpDate;

    @Schema(description = "跟进方式")
    private String followUpMethod;

    @Schema(description = "联系人ID")
    private Long contactId;

    @NotEmpty(message = "跟进内容不能为空")
    @Schema(description = "跟进详情")
    private String content;

    @Schema(description = "下一步计划")
    private String nextPlan;
}
