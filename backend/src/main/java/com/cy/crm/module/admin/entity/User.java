package com.cy.crm.module.admin.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cy.crm.common.entity.AuditableEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_user")
public class User extends AuditableEntity {

    private String username;
    @ToString.Exclude
    private String passwordHash;
    private String realName;
    private String phone;
    private String email;
    private Integer status;
    private Integer isInitialPassword;
    private LocalDateTime lastLoginAt;

    /**
     * 上次密码修改时间。null 时视为从未改过（新建用户 / 旧数据）。
     * 用于 90 天强制过期策略；首次登录强制改密以 isInitialPassword=1 为准。
     */
    private LocalDateTime passwordChangedAt;

    @TableField(exist = false)
    private List<Role> roles;
}
