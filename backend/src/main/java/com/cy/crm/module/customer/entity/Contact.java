package com.cy.crm.module.customer.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.cy.crm.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_contact")
public class Contact extends BaseEntity {

    private Long customerId;
    private String name;
    private String title;
    private String phone;
    private Integer contactType;
    private Integer isPrimary;
}
