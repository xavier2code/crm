package com.cy.crm.module.rebate.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.cy.crm.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_rebate_rate")
public class RebateRate extends BaseEntity {

    private String productCategory;
    private Long channelId;
    private BigDecimal rate;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
}
