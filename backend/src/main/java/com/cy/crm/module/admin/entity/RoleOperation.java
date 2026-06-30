package com.cy.crm.module.admin.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("t_role_operation")
public class RoleOperation {
    @TableId
    private Long roleId;
    private String operationCode;
}
