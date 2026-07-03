package com.cy.crm.module.reimbursement.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.cy.crm.common.entity.AuditableEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_reimbursement")
public class Reimbursement extends AuditableEntity {

    private Long projectId;
    private String projectNameSnapshot;
    private Long applicantId;
    private String applicantNameSnapshot;
    private String type;
    private String title;
    private String description;
    private BigDecimal amount;
    private LocalDate expenseDate;
    private String status;
    private Long approverId;
    private String approverNameSnapshot;
    private LocalDateTime approvedAt;
    private String approvalComment;
    private LocalDateTime paidAt;
}
