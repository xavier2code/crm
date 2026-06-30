package com.cy.crm.module.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("t_role")
public class Role {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String code;
    private String name;
    private Integer isBuiltin;
    private Integer dataScopeType;
}
