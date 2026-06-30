package com.cy.crm.module.customer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

@Data
@Schema(description = "联系人请求")
public class ContactRequest {

    @NotEmpty(message = "联系人姓名不能为空")
    @Schema(description = "姓名")
    private String name;

    @Schema(description = "职务")
    private String title;

    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    @Schema(description = "手机号")
    private String phone;

    @NotNull(message = "联系人类型不能为空")
    @Schema(description = "类型：1=重要决策人 2=业务对接人 3=操作员")
    private Integer contactType;

    @Schema(description = "是否主联系人")
    private Integer isPrimary;
}
