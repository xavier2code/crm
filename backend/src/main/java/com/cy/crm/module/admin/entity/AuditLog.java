package com.cy.crm.module.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_audit_log")
public class AuditLog {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private String username;
    private String operation;
    private String module;
    private String method;
    private String params;
    private String ip;
    private Integer status;
    private String errorMsg;
    private Integer executeTime;
    private LocalDateTime createdAt;
}
