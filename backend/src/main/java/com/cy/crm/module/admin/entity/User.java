package com.cy.crm.module.admin.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cy.crm.common.entity.AuditableEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_user")
public class User extends AuditableEntity {

    private String username;
    private String passwordHash;
    private String realName;
    private String phone;
    private String email;
    private Integer status;
    private Integer isInitialPassword;
    private LocalDateTime lastLoginAt;

    @TableField(exist = false)
    private List<Role> roles;
}
