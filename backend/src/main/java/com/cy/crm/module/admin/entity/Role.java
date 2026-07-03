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

    /**
     * 角色的默认数据权限维度（DataScopeDimension.code）。
     * 用户实际生效的 DataScope 由 t_data_permission 单独控制；该字段保留作角色默认值参考。
     */
    private String dataScopeType;
}
