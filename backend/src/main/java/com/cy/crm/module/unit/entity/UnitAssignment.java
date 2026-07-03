package com.cy.crm.module.unit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 单位分配记录（业务侧，4 级分配链路，详见 CRM-渠道版-开发文档.md §9.5）。
 *
 * assign_scope = BD：大区总把单位分配给 BD
 * assign_scope = CHANNEL_BD：渠道负责人把单位分配给渠道 BD（此时 channel_id 必填）
 */
@Data
@TableName("t_unit_assignment")
public class UnitAssignment {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long unitId;
    private Long userId;
    private String assignScope;
    private Long channelId;
    private Long assignedBy;
    private LocalDateTime assignedAt;
}
