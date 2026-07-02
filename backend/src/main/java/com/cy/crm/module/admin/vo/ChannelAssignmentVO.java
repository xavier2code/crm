package com.cy.crm.module.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "渠道分配记录")
public class ChannelAssignmentVO {
    @Schema(description = "渠道ID")
    private Long channelId;

    @Schema(description = "渠道名称")
    private String channelName;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "真实姓名")
    private String realName;

    @Schema(description = "分配类型：1=渠道负责人 2=渠道 BD")
    private Integer assignType;

    @Schema(description = "分配人ID")
    private Long assignedBy;

    @Schema(description = "分配人姓名")
    private String assignedByName;

    @Schema(description = "分配时间")
    private LocalDateTime assignedAt;
}
