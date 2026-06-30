package com.cy.crm.module.opportunity.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = false)
@Schema(description = "商机报备详情响应")
public class OpportunityDetailVO extends OpportunityVO {

    @Schema(description = "单位ID")
    private Long unitId;

    @Schema(description = "单位名称")
    private String unitName;

    @Schema(description = "警种")
    private String policeType;

    @Schema(description = "警种名称")
    private String policeTypeName;

    @Schema(description = "审批日志")
    private List<ApprovalLogVO> approvalLogs;

    @Data
    @Schema(description = "审批日志")
    public static class ApprovalLogVO {
        @Schema(description = "操作类型：1=提交 2=通过 3=驳回")
        private Integer action;

        @Schema(description = "操作类型名称")
        private String actionName;

        @Schema(description = "操作人ID")
        private Long operatorId;

        @Schema(description = "操作人姓名")
        private String operatorName;

        @Schema(description = "意见/原因")
        private String comment;

        @Schema(description = "操作时间")
        private LocalDateTime createdAt;
    }
}
