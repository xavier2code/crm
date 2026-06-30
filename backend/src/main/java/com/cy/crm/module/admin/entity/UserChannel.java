package com.cy.crm.module.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_user_channel")
public class UserChannel {
    @TableId(type = IdType.AUTO)
    private Long userId;

    private Long channelId;
    private Integer assignType;
    private Long assignedBy;
    private LocalDateTime assignedAt;
}
