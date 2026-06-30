package com.cy.crm.module.notification.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_notification")
public class Notification {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private String title;
    private String content;
    private String type;
    private Integer status;
    private Long relatedId;
    private LocalDateTime createdAt;
    private LocalDateTime readAt;
}
