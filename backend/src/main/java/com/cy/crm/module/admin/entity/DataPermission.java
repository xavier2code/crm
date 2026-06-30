package com.cy.crm.module.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("t_data_permission")
public class DataPermission {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private Integer scopeType;
    private String scopeValue;
}
