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

    /**
     * 权限维度 code，与 DataScopeDimension 枚举一一对应：
     * ALL / CHANNEL / REGION / UNIT / BUSINESS_DOMAIN / POLICE_TYPE / SELF
     */
    private String scopeType;

    private String scopeValue;
}
