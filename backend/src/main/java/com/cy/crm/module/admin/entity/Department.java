package com.cy.crm.module.admin.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.cy.crm.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_department")
public class Department extends BaseEntity {

    private String code;
    private String name;
    private Long parentId;
    private Integer status;
}
