package com.cy.crm.module.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "渠道创建/编辑请求")
public class ChannelRequest {
    private Long id;

    @NotBlank(message = "渠道名称不能为空")
    @Schema(description = "渠道名称")
    private String name;

    @Schema(description = "所属区域")
    private String region;

    @Schema(description = "状态：1启用 0停用")
    private Integer status = 1;
}
