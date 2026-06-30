package com.cy.crm.module.rebate.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class RebateRateRequest {
    private Long id;
    private String productCategory;
    private Long channelId;
    private BigDecimal rate;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
}
