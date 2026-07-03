package com.cy.crm.module.sales_team.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.cy.crm.common.entity.AuditableEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 销售梯队配置实体
 * 对应表 t_sales_team_config
 *
 * 符合开发文档：
 * - §8 销售分配梯队
 * - §14.1 数据模型（库表设计）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_sales_team_config")
public class SalesTeamConfig extends AuditableEntity {

    /**
     * 梯队编码
     */
    private String teamCode;

    /**
     * 梯队名称
     */
    private String teamName;

    /**
     * 区域字典 code
     */
    private String regionCode;

    /**
     * 包含的单位 code，逗号分隔
     */
    private String unitCodes;

    /**
     * 排序
     */
    private Integer sort;

    /**
     * 备注
     */
    private String remark;

    /**
     * 有效期起
     */
    private LocalDate effectiveFrom;

    /**
     * 有效期止
     */
    private LocalDate effectiveTo;
}
