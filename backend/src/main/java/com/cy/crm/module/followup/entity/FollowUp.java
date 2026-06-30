package com.cy.crm.module.followup.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.cy.crm.common.entity.AuditableEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_follow_up")
public class FollowUp extends AuditableEntity {

    private Long customerId;
    private Long projectId;
    private Long opportunityId;
    private String currentStage;
    private String nextStage;
    private String stageFeedback;
    private LocalDate followUpDate;
    private String followUpMethod;
    private Long contactId;
    private String content;
    private String nextPlan;
}
