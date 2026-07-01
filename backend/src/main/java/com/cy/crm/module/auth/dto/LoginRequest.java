package com.cy.crm.module.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.ToString;

@Data
@Schema(description = "登录请求")
public class LoginRequest {
    @NotBlank(message = "用户名不能为空")
    @Schema(description = "用户名")
    @ToString.Exclude
    private String username;

    @NotBlank(message = "密码不能为空")
    @Schema(description = "密码")
    @ToString.Exclude
    private String password;

    @Schema(description = "验证码唯一标识")
    @ToString.Exclude
    private String captchaUuid;

    @Schema(description = "验证码")
    @ToString.Exclude
    private String captchaCode;
}
