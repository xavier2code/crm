package com.cy.crm.module.task.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Schema(description = "任务响应")
public class TaskVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "所属人ID")
    private Long ownerUserId;

    @Schema(description = "所属人姓名")
    private String ownerUserName;

    @Schema(description = "客户ID")
    private Long customerId;

    @Schema(description = "客户名称")
    private String customerName;

    @Schema(description = "跟进记录ID")
    private Long followUpId;

    @Schema(description = "下一步阶段")
    private String planStage;

    @Schema(description = "阶段名称")
    private String planStageName;

    @Schema(description = "计划时间")
    private LocalDate planDate;

    @Schema(description = "状态：1=待完成 2=已完成 3=已关闭")
    private Integer status;

    @Schema(description = "状态名称")
    private String statusName;

    @Schema(description = "关闭原因")
    private String closeReason;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
