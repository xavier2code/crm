package com.cy.crm.module.project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;

@Data
@Schema(description = "招投标节点请求")
public class BiddingNodeRequest {

    @Schema(description = "招标（代理）机构")
    private String biddingAgency;

    @Schema(description = "采购方式：1=询价内 2=询价外 3=竞争性谈判 4=竞争性磋商 5=公开招标 6=单一来源 7=邀请招标")
    private Integer purchaseMethod;

    @Schema(description = "招标/需求公告日期")
    private LocalDate announcementDate;

    @Schema(description = "报名开始日期")
    private LocalDate registrationStart;

    @Schema(description = "报名结束日期")
    private LocalDate registrationEnd;

    @Schema(description = "投标日期")
    private LocalDate bidDate;

    @Schema(description = "中标公告开始日期")
    private LocalDate bidResultStart;

    @Schema(description = "中标公告结束日期")
    private LocalDate bidResultEnd;

    @Schema(description = "领取中标通知书日期")
    private LocalDate noticeReceivedDate;

    @Schema(description = "是否将原件上交公司")
    private Integer noticeOriginalArchived;
}
