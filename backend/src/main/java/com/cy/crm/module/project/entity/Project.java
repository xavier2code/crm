package com.cy.crm.module.project.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.cy.crm.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_project")
public class Project extends BaseEntity {

    private Long opportunityId;
    private String name;
    private String businessDomain;
    private String productCategory;
    private Integer adminLevel;
    private BigDecimal amount;
    private Integer performanceCount;
    private String salesMethod;
    private Long ownerBdId;
    private Long salesUserId;
    private LocalDate expectedSignDate;
    private Integer status;
    private Integer pNode;
    private String stage6;
    private String customerLayer;
    private LocalDateTime trialAt;
    private LocalDateTime formalAt;
    private LocalDateTime expireAt;
}
