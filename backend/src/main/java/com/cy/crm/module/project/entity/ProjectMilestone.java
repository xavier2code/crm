package com.cy.crm.module.project.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_project_milestone")
public class ProjectMilestone {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long projectId;
    private Integer preOpenBusiness;
    private Integer biddingPublished;
    private Integer bidSubmitted;
    private Integer bidWonPublished;
    private Integer contractSigned;
    private Integer serviceOpened;
    private Integer acceptanceDone;
    private Integer invoiceIssued;
    private Integer paymentDone;
    private Integer serviceFeeReceived;
    private LocalDateTime updatedAt;
}
