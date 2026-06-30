package com.cy.crm.module.project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Schema(description = "项目请求")
public class ProjectRequest {

    @NotNull(message = "商机ID不能为空")
    @Schema(description = "商机ID")
    private Long opportunityId;

    @NotEmpty(message = "项目名称不能为空")
    @Schema(description = "项目名称")
    private String name;

    @Schema(description = "业务域")
    private String businessDomain;

    @Schema(description = "产品类别")
    private String productCategory;

    @Schema(description = "行政级别：1=省厅 2=地市 3=区县")
    private Integer adminLevel;

    @Schema(description = "金额（万元）")
    private BigDecimal amount;

    @Schema(description = "业绩数（超级管理员账号数）")
    private Integer performanceCount;

    @Schema(description = "销售方式：直销/经销")
    private String salesMethod;

    @Schema(description = "负责BD ID")
    private Long ownerBdId;

    @Schema(description = "销售 ID")
    private Long salesUserId;

    @Schema(description = "预计签单日期")
    private LocalDate expectedSignDate;

    @Schema(description = "客户分层：A/B/C")
    private String customerLayer;

    @Valid
    @Schema(description = "招投标节点")
    private BiddingNodeRequest biddingNode;

    @Valid
    @Schema(description = "合同节点")
    private ContractNodeRequest contractNode;
}
