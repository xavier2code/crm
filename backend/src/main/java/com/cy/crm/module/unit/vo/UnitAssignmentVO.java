package com.cy.crm.module.unit.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "单位分配记录")
public class UnitAssignmentVO {

    @Schema(description = "分配ID")
    private Long id;

    @Schema(description = "单位ID")
    private Long unitId;

    @Schema(description = "单位名称")
    private String unitName;

    @Schema(description = "单位所属区域")
    private String unitRegion;

    @Schema(description = "被分配的 BD 用户ID")
    private Long userId;

    @Schema(description = "BD 用户名")
    private String username;

    @Schema(description = "BD 真实姓名")
    private String realName;

    @Schema(description = "分配范围：BD / CHANNEL_BD")
    private String assignScope;

    @Schema(description = "渠道ID（CHANNEL_BD 时）")
    private Long channelId;

    @Schema(description = "渠道名称")
    private String channelName;

    @Schema(description = "分配人ID")
    private Long assignedBy;

    @Schema(description = "分配人姓名")
    private String assignedByName;

    @Schema(description = "分配时间")
    private LocalDateTime assignedAt;
}
