package com.cy.crm.module.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "会话信息")
public class SessionInfo {

    @Schema(description = "会话ID（JWT ID）")
    private String sessionId;

    @Schema(description = "登录时间")
    private LocalDateTime loginTime;

    @Schema(description = "最后活跃时间")
    private LocalDateTime lastActiveTime;

    @Schema(description = "客户端IP")
    private String clientIp;

    @Schema(description = "用户代理")
    private String userAgent;

    @Schema(description = "是否为当前会话")
    private Boolean isCurrent;
}
