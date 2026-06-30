package com.cy.crm.module.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_login_failure")
public class LoginFailure {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;
    private Short failCount;
    private LocalDateTime lockedUntil;
    private LocalDateTime lastFailAt;
    private String clientIp;
}
