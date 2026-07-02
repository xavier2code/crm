package com.cy.crm.module.admin.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.cy.crm.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_dictionary")
public class Dictionary extends BaseEntity {

    private String type;
    private String code;
    private String name;
    private Integer sort;
    private String remark;
    private Integer isBuiltin;
}
