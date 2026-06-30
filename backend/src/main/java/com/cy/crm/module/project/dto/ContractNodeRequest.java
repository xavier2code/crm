package com.cy.crm.module.project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Schema(description = "合同节点请求")
public class ContractNodeRequest {

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
