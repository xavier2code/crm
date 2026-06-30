package com.cy.crm.module.project.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_project_score")
public class ProjectScore {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long projectId;
    private String snapshotWeek;
    private String scoreDimension;
    private BigDecimal score;
    private BigDecimal weight;
    private LocalDateTime createdAt;
}
