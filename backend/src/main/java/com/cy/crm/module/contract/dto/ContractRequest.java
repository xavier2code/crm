package com.cy.crm.module.contract.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
@Schema(description = "合同请求")
public class ContractRequest {

    @NotNull(message = "项目ID不能为空")
    @Schema(description = "项目ID")
    private Long projectId;

    @NotNull(message = "合同金额不能为空")
    @Schema(description = "合同金额（万元）")
    private BigDecimal amount;
}
