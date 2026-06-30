package com.cy.crm.module.admin.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.cy.crm.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_unit")
public class Unit extends BaseEntity {

    private String name;
    private String region;
    private Integer adminLevel;
    private String address;
    private Integer status;
}
