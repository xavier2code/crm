package com.cy.crm.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 实体公共基类
 * 统一主键、审计字段、逻辑删除字段
 *
 * 符合开发文档：
 * - §35 JPA Entity 映射规则
 * - §37 MyBatis-Plus 配置
 * - §39 软删级联策略
 */
@Data
public abstract class BaseEntity {

    /**
     * 主键 ID，使用数据库自增（BIGSERIAL）
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /**
     * 逻辑删除标志：0=未删除 1=已删除
     */
    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    @JsonIgnore
    private Integer isDeleted;

    /**
     * 乐观锁版本号
     */
    @Version
    private Integer version;
}
