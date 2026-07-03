package com.cy.crm.module.reimbursement.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "报销响应")
public class ReimbursementVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "项目ID")
    private Long projectId;

    @Schema(description = "项目名称（创建时快照）")
    private String projectName;

    @Schema(description = "申请人ID")
    private Long applicantId;

    @Schema(description = "申请人姓名")
    private String applicantName;

    @Schema(description = "类型编码：TRAVEL / ENTERTAIN")
    private String type;

    @Schema(description = "类型名称")
    private String typeName;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "说明")
    private String description;

    @Schema(description = "金额")
    private BigDecimal amount;

    @Schema(description = "发生日期")
    private LocalDate expenseDate;

    @Schema(description = "状态：DRAFT / PENDING / APPROVED / REJECTED / PAID")
    private String status;

    @Schema(description = "审批人ID")
    private Long approverId;

    @Schema(description = "审批人姓名")
    private String approverName;

    @Schema(description = "审批时间")
    private LocalDateTime approvedAt;

    @Schema(description = "审批意见")
    private String approvalComment;

    @Schema(description = "付款时间")
    private LocalDateTime paidAt;

    @Schema(description = "创建人ID")
    private Long createdBy;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;

    @Schema(description = "附件列表")
    private List<ReimbursementAttachmentVO> attachments;
}
