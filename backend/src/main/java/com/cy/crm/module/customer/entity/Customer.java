package com.cy.crm.module.customer.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.cy.crm.common.entity.AuditableEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_customer")
public class Customer extends AuditableEntity {

    private String name;
    private Long unitId;
    private String policeType;
    private String customerLayer;
    private Long ownerUserId;
    private String region;
    private Integer status;
}
