package com.cy.crm.module.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("t_menu")
public class Menu {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;
    private String code;
    private String path;
    private Long parentId;
    private Integer sort;
    private String icon;
    private Integer type;
    private String permission;
    private Integer status;
}
