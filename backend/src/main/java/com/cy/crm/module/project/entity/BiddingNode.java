package com.cy.crm.module.project.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.cy.crm.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_bidding_node")
public class BiddingNode extends BaseEntity {

    private Long projectId;
    private String biddingAgency;
    private Integer purchaseMethod;
    private LocalDate announcementDate;
    private LocalDate registrationStart;
    private LocalDate registrationEnd;
    private LocalDate bidDate;
    private LocalDate bidResultStart;
    private LocalDate bidResultEnd;
    private LocalDate noticeReceivedDate;
    private Integer noticeOriginalArchived;
}
