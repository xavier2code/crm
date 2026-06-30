package com.cy.crm.module.opportunity.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.cy.crm.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_opportunity")
public class Opportunity extends BaseEntity {

    private Long customerId;
    private String businessDomain;
    private Integer projectType;
    private BigDecimal amount;
    private Integer status;
    private Integer submitCount;
    private String stage;  // 商机阶段：DRAFT/IN_PROGRESS/IN_PROJECT/SERVICE/COMPLETED
    private LocalDateTime lastFollowUpAt;
    private LocalDateTime effectiveAt;
    private LocalDateTime expiredAt;
    private LocalDateTime coolingUntil;
    private Long submittedBy;
    private Long approvedBy;
    private LocalDateTime approvedAt;
    private String rejectReason;
}
