package com.cy.crm.module.contract.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.cy.crm.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_contract")
public class Contract extends BaseEntity {

    private Long projectId;
    private BigDecimal amount;
    private Integer status;
}
