package com.cy.crm.module.opportunity.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "商机报备响应")
public class OpportunityVO {

    @Schema(description = "报备ID")
    private Long id;

    @Schema(description = "客户ID")
    private Long customerId;

    @Schema(description = "客户名称")
    private String customerName;

    @Schema(description = "业务域")
    private String businessDomain;

    @Schema(description = "业务域名称")
    private String businessDomainName;

    @Schema(description = "项目类型")
    private Integer projectType;

    @Schema(description = "项目类型名称")
    private String projectTypeName;

    @Schema(description = "预计金额")
    private BigDecimal amount;

    @Schema(description = "状态：1=草稿 2=审批中 3=生效中 4=报备失败 5=报备失效")
    private Integer status;

    @Schema(description = "状态名称")
    private String statusName;

    @Schema(description = "提交次数")
    private Integer submitCount;

    @Schema(description = "最后跟进时间")
    private LocalDateTime lastFollowUpAt;

    @Schema(description = "生效时间")
    private LocalDateTime effectiveAt;

    @Schema(description = "失效时间")
    private LocalDateTime expiredAt;

    @Schema(description = "冷却期截止")
    private LocalDateTime coolingUntil;

    @Schema(description = "提交人ID")
    private Long submittedBy;

    @Schema(description = "提交人姓名")
    private String submittedByName;

    @Schema(description = "审批人ID")
    private Long approvedBy;

    @Schema(description = "审批人姓名")
    private String approvedByName;

    @Schema(description = "审批时间")
    private LocalDateTime approvedAt;

    @Schema(description = "驳回原因")
    private String rejectReason;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "是否可编辑")
    private Boolean editable;

    @Schema(description = "是否可提交（仅草稿状态）")
    private Boolean submittable;

    @Schema(description = "是否可重提（报备失败/报备失效，未用完恢复机会，未在冷却期）")
    private Boolean resubmittable;

    @Schema(description = "是否可审批")
    private Boolean approvable;
}
