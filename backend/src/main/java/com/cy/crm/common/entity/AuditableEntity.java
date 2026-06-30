package com.cy.crm.common.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 带审计人信息的实体基类
 * 适用于数据库表包含 created_by 字段的实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
public abstract class AuditableEntity extends BaseEntity {

    /**
     * 创建人 ID
     */
    @TableField(fill = FieldFill.INSERT)
    private Long createdBy;
}
