package com.cy.crm.module.followup.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Schema(description = "跟进记录响应")
public class FollowUpVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "客户ID")
    private Long customerId;

    @Schema(description = "客户名称")
    private String customerName;

    @Schema(description = "项目ID")
    private Long projectId;

    @Schema(description = "项目名称")
    private String projectName;

    @Schema(description = "商机ID")
    private Long opportunityId;

    @Schema(description = "当前阶段")
    private String currentStage;

    @Schema(description = "当前阶段名称")
    private String currentStageName;

    @Schema(description = "下一步阶段")
    private String nextStage;

    @Schema(description = "下一步阶段名称")
    private String nextStageName;

    @Schema(description = "阶段反馈")
    private String stageFeedback;

    @Schema(description = "跟进日期")
    private LocalDate followUpDate;

    @Schema(description = "跟进方式")
    private String followUpMethod;

    @Schema(description = "跟进方式名称")
    private String followUpMethodName;

    @Schema(description = "联系人ID")
    private Long contactId;

    @Schema(description = "联系人姓名")
    private String contactName;

    @Schema(description = "跟进内容")
    private String content;

    @Schema(description = "下一步计划")
    private String nextPlan;

    @Schema(description = "创建人ID")
    private Long createdBy;

    @Schema(description = "创建人姓名")
    private String createdByName;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
