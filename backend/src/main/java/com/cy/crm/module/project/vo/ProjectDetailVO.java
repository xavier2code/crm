package com.cy.crm.module.project.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "项目详情响应")
public class ProjectDetailVO extends ProjectVO {

    @Schema(description = "客户ID")
    private Long customerId;

    @Schema(description = "客户名称")
    private String customerName;

    @Schema(description = "单位ID")
    private Long unitId;

    @Schema(description = "单位名称")
    private String unitName;

    @Schema(description = "警种")
    private String policeType;

    @Schema(description = "警种名称")
    private String policeTypeName;

    @Schema(description = "9项里程碑")
    private MilestoneVO milestone;

    @Schema(description = "招投标节点")
    private BiddingNodeVO biddingNode;

    @Schema(description = "合同节点")
    private ContractNodeVO contractNode;

    @Schema(description = "回款节点列表")
    private List<PaymentNodeVO> paymentNodes;

    @Schema(description = "双精评分历史")
    private List<ScoreHistoryVO> scoreHistory;

    @Data
    @Schema(description = "里程碑")
    public static class MilestoneVO {
        @Schema(description = "提前开通业务")
        private Integer preOpenBusiness;

        @Schema(description = "招标挂网")
        private Integer biddingPublished;

        @Schema(description = "项目投标")
        private Integer bidSubmitted;

        @Schema(description = "中标挂网")
        private Integer bidWonPublished;

        @Schema(description = "签订合同")
        private Integer contractSigned;

        @Schema(description = "正常开通")
        private Integer serviceOpened;

        @Schema(description = "项目验收")
        private Integer acceptanceDone;

        @Schema(description = "开具发票")
        private Integer invoiceIssued;

        @Schema(description = "支付手续")
        private Integer paymentDone;

        @Schema(description = "支付服务款")
        private Integer serviceFeeReceived;
    }

    @Data
    @Schema(description = "招投标节点")
    public static class BiddingNodeVO {
        @Schema(description = "招标（代理）机构")
        private String biddingAgency;

        @Schema(description = "采购方式")
        private Integer purchaseMethod;

        @Schema(description = "采购方式名称")
        private String purchaseMethodName;

        @Schema(description = "招标/需求公告日期")
        private LocalDate announcementDate;

        @Schema(description = "报名开始日期")
        private LocalDate registrationStart;

        @Schema(description = "报名结束日期")
        private LocalDate registrationEnd;

        @Schema(description = "投标日期")
        private LocalDate bidDate;

        @Schema(description = "中标公告开始日期")
        private LocalDate bidResultStart;

        @Schema(description = "中标公告结束日期")
        private LocalDate bidResultEnd;

        @Schema(description = "领取中标通知书日期")
        private LocalDate noticeReceivedDate;

        @Schema(description = "原件上交公司")
        private Integer noticeOriginalArchived;
    }

    @Data
    @Schema(description = "合同节点")
    public static class ContractNodeVO {
        @Schema(description = "起草日期")
        private LocalDate draftDate;

        @Schema(description = "局内审核部门及流程")
        private String reviewDept;

        @Schema(description = "审定日期")
        private LocalDate approveDate;

        @Schema(description = "原件上交公司")
        private Integer originalArchived;

        @Schema(description = "付款方式")
        private String paymentMethod;

        @Schema(description = "付款比例")
        private String paymentRatio;

        @Schema(description = "付款条件")
        private String paymentTerms;

        @Schema(description = "付款节点")
        private String paymentNodes;

        @Schema(description = "是否有质保金")
        private Integer hasWarranty;

        @Schema(description = "质保金金额")
        private BigDecimal warrantyAmount;

        @Schema(description = "验收部门及流程")
        private String acceptanceDept;

        @Schema(description = "是否有竣工结算审计")
        private Integer hasSettlementAudit;

        @Schema(description = "开具发票日期")
        private LocalDate invoiceDate;

        @Schema(description = "收款单据流转部门")
        private String paymentVoucherDept;

        @Schema(description = "收款到账日期")
        private LocalDate receivedDate;
    }

    @Data
    @Schema(description = "回款节点")
    public static class PaymentNodeVO {
        @Schema(description = "ID")
        private Long id;

        @Schema(description = "第几期")
        private Integer paymentNo;

        @Schema(description = "金额")
        private BigDecimal amount;

        @Schema(description = "收款到账日期")
        private LocalDate receivedDate;

        @Schema(description = "发票号")
        private String invoiceNo;

        @Schema(description = "状态：1=待回款 2=已到账 3=逾期")
        private Integer status;

        @Schema(description = "状态名称")
        private String statusName;
    }

    @Data
    @Schema(description = "评分历史")
    public static class ScoreHistoryVO {
        @Schema(description = "快照周")
        private String snapshotWeek;

        @Schema(description = "总分")
        private BigDecimal totalScore;

        @Schema(description = "评分详情")
        private List<ScoreItemVO> scores;

        @Data
        @Schema(description = "评分项")
        public static class ScoreItemVO {
            @Schema(description = "维度编码")
            private String dimension;

            @Schema(description = "维度名称")
            private String dimensionName;

            @Schema(description = "分数")
            private BigDecimal score;

            @Schema(description = "权重")
            private BigDecimal weight;

            @Schema(description = "加权分数")
            private BigDecimal weightedScore;
        }
    }
}
