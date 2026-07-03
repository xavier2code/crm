package com.cy.crm.module.reimbursement.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Schema(description = "报销申请请求")
public class ReimbursementRequest {

    @NotNull(message = "项目ID不能为空")
    @Schema(description = "项目ID")
    private Long projectId;

    @NotBlank(message = "报销类型不能为空")
    @Pattern(regexp = "^(TRAVEL|ENTERTAIN)$", message = "报销类型必须为 TRAVEL 或 ENTERTAIN")
    @Schema(description = "报销类型：TRAVEL=差旅 ENTERTAIN=招待")
    private String type;

    @NotBlank(message = "标题不能为空")
    @Size(max = 255, message = "标题不能超过 255 字")
    @Schema(description = "报销标题")
    private String title;

    @Size(max = 2000, message = "说明不能超过 2000 字")
    @Schema(description = "报销说明")
    private String description;

    @NotNull(message = "金额不能为空")
    @DecimalMin(value = "0.00", message = "金额不能为负数")
    @Schema(description = "报销金额")
    private BigDecimal amount;

    @NotNull(message = "发生日期不能为空")
    @Schema(description = "发生日期")
    private LocalDate expenseDate;
}
