package com.cy.crm.module.notification.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.cy.crm.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_notification_template")
public class NotificationTemplate extends BaseEntity {

    private String code;
    private String name;
    private String title;
    private String content;
    private String type;
    private Integer status;
}
