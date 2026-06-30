package com.cy.crm.module.rebate.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.cy.crm.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_rebate")
public class Rebate extends BaseEntity {

    private Long channelId;
    private Long contractId;
    private String productCategory;
    private BigDecimal rebateRate;
    private BigDecimal totalAmount;
    private BigDecimal actualAmount;
    private Integer confirmStatus;
    private Integer paymentStatus;
    private Integer rebateType;
}
