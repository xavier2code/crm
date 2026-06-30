package com.cy.crm.module.customer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

@Data
@Schema(description = "客户请求")
public class CustomerRequest {

    @NotNull(message = "单位ID不能为空")
    @Schema(description = "单位ID")
    private Long unitId;

    @NotEmpty(message = "警种不能为空")
    @Schema(description = "警种")
    private String policeType;

    @Schema(description = "客户分层：A/B/C")
    private String customerLayer;

    @Schema(description = "联系人列表")
    private java.util.List<ContactRequest> contacts;
}
