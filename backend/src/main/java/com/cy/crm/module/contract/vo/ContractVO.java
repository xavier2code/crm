package com.cy.crm.module.contract.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "合同响应")
public class ContractVO {

    @Schema(description = "合同ID")
    private Long id;

    @Schema(description = "项目ID")
    private Long projectId;

    @Schema(description = "项目名称")
    private String projectName;

    @Schema(description = "合同金额")
    private BigDecimal amount;

    @Schema(description = "状态：1=待签 2=已签 3=已开通 4=服务中 5=已到期")
    private Integer status;

    @Schema(description = "状态名称")
    private String statusName;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
