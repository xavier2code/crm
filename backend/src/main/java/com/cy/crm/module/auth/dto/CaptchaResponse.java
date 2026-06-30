package com.cy.crm.module.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "验证码响应")
public class CaptchaResponse {

    @Schema(description = "验证码图片（Base64）")
    private String image;

    @Schema(description = "验证码唯一标识")
    private String uuid;

    public CaptchaResponse(String image, String uuid) {
        this.image = image;
        this.uuid = uuid;
    }
}
