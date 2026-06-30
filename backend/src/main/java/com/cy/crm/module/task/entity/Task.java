package com.cy.crm.module.task.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.cy.crm.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_task")
public class Task extends BaseEntity {

    private Long ownerUserId;
    private Long customerId;
    private Long followUpId;
    private String planStage;
    private LocalDate planDate;
    private Integer status;
    private String closeReason;
}
