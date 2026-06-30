package com.cy.crm.module.admin.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("t_role_menu")
public class RoleMenu {
    @TableId
    private Long roleId;
    private Long menuId;
}
