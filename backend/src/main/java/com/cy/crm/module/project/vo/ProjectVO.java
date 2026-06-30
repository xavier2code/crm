package com.cy.crm.module.project.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Schema(description = "项目响应")
public class ProjectVO {

    @Schema(description = "项目ID")
    private Long id;

    @Schema(description = "商机ID")
    private Long opportunityId;

    @Schema(description = "项目名称")
    private String name;

    @Schema(description = "业务域")
    private String businessDomain;

    @Schema(description = "业务域名称")
    private String businessDomainName;

    @Schema(description = "产品类别")
    private String productCategory;

    @Schema(description = "行政级别")
    private Integer adminLevel;

    @Schema(description = "行政级别名称")
    private String adminLevelName;

    @Schema(description = "金额（万元）")
    private BigDecimal amount;

    @Schema(description = "业绩数")
    private Integer performanceCount;

    @Schema(description = "销售方式")
    private String salesMethod;

    @Schema(description = "负责BD ID")
    private Long ownerBdId;

    @Schema(description = "负责BD姓名")
    private String ownerBdName;

    @Schema(description = "销售 ID")
    private Long salesUserId;

    @Schema(description = "销售姓名")
    private String salesUserName;

    @Schema(description = "预计签单日期")
    private LocalDate expectedSignDate;

    @Schema(description = "状态：1=项目中 2=项目完成 3=项目中断 4=项目终止")
    private Integer status;

    @Schema(description = "状态名称")
    private String statusName;

    @Schema(description = "当前P级节点：1-8")
    private Integer pNode;

    @Schema(description = "P级节点名称")
    private String pNodeName;

    @Schema(description = "6大阶段")
    private String stage6;

    @Schema(description = "阶段名称")
    private String stage6Name;

    @Schema(description = "客户分层")
    private String customerLayer;

    @Schema(description = "试用账号开通时间")
    private LocalDateTime trialAt;

    @Schema(description = "正式账号开通时间")
    private LocalDateTime formalAt;

    @Schema(description = "到期时间")
    private LocalDateTime expireAt;

    @Schema(description = "完成度百分比")
    private Integer completionRate;

    @Schema(description = "当前双精评分")
    private BigDecimal currentScore;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
