package com.cy.crm.module.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "渠道分配请求")
public class ChannelAssignRequest {

    @NotNull(message = "用户ID不能为空")
    @Schema(description = "被分配的用户ID")
    private Long userId;

    @NotNull(message = "分配类型不能为空")
    @Schema(description = "分配类型：1=渠道负责人 2=渠道 BD")
    private Integer assignType;
}
