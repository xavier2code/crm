package com.cy.crm.module.project.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.cy.crm.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_contract_node")
public class ContractNode extends BaseEntity {

    private Long projectId;
    private LocalDate draftDate;
    private String reviewDept;
    private LocalDate approveDate;
    private Integer originalArchived;
    private String paymentMethod;
    private String paymentRatio;
    private String paymentTerms;
    private String paymentNodes;
    private Integer hasWarranty;
    private BigDecimal warrantyAmount;
    private String acceptanceDept;
    private Integer hasSettlementAudit;
    private LocalDate invoiceDate;
    private String paymentVoucherDept;
    private LocalDate receivedDate;
}
