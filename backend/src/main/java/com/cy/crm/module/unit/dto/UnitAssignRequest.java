package com.cy.crm.module.unit.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "单位分配请求（业务侧 4 级分配）")
public class UnitAssignRequest {

    @NotNull(message = "用户ID不能为空")
    @Schema(description = "被分配的 BD 用户ID")
    private Long userId;

    @NotBlank(message = "分配范围不能为空")
    @Schema(description = "分配范围：BD=大区总/BD 链路；CHANNEL_BD=渠道负责人→渠道 BD 链路")
    private String assignScope;

    @Schema(description = "渠道ID，仅 assignScope=CHANNEL_BD 时必填")
    private Long channelId;
}
