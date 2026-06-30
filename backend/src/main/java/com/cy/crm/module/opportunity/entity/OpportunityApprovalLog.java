package com.cy.crm.module.opportunity.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_opportunity_approval_log")
public class OpportunityApprovalLog {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long opportunityId;
    private Integer action;
    private Long operatorId;
    private String comment;
    private LocalDateTime createdAt;
}
