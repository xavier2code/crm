package com.cy.crm.module.project.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.cy.crm.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_payment_node")
public class PaymentNode extends BaseEntity {

    private Long projectId;
    private Integer paymentNo;
    private BigDecimal amount;
    private LocalDate receivedDate;
    private String invoiceNo;
    private Integer status;
}
