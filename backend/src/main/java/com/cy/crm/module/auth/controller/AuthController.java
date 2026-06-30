package com.cy.crm.module.auth.controller;

import com.cy.crm.common.response.ApiResult;
import com.cy.crm.module.auth.dto.*;
import com.cy.crm.module.auth.service.AuthService;
import com.cy.crm.module.auth.service.CaptchaService;
import com.cy.crm.security.JwtAuthenticationToken;
import com.cy.crm.security.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "认证管理", description = "登录、登出、令牌刷新、密码管理、验证码")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtUtil jwtUtil;
    private final CaptchaService captchaService;

    @Operation(summary = "获取图形验证码")
    @GetMapping("/captcha")
    public ApiResult<CaptchaResponse> getCaptcha() {
        String uuid = UUID.randomUUID().toString();
        String image = captchaService.generateCaptcha(uuid);
        return ApiResult.ok(new CaptchaResponse(image, uuid));
    }

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public ApiResult<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResult.ok(authService.login(request));
    }

    @Operation(summary = "刷新访问令牌")
    @PostMapping("/refresh")
    public ApiResult<TokenResponse> refreshToken(@RequestBody RefreshTokenRequest request) {
        return ApiResult.ok(authService.refreshToken(request.getRefreshToken()));
    }

    @Operation(summary = "获取当前登录用户")
    @GetMapping("/currentUser")
    public ApiResult<CurrentUserResponse> currentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return ApiResult.ok(authService.getCurrentUser(username));
    }

    @Operation(summary = "修改密码")
    @PostMapping("/change-password")
    public ApiResult<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        authService.changePassword(username, request.getOldPassword(), request.getNewPassword());
        return ApiResult.ok();
    }

    @Operation(summary = "退出登录")
    @PostMapping("/logout")
    public ApiResult<Void> logout(HttpServletRequest request) {
        String token = extractToken(request);
        authService.logout(token);
        return ApiResult.ok();
    }

    @Operation(summary = "退出所有设备")
    @PostMapping("/logout-all")
    public ApiResult<Void> logoutAll() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        authService.logoutAll(username);
        return ApiResult.ok();
    }

    @Operation(summary = "获取当前会话列表")
    @GetMapping("/sessions")
    public ApiResult<List<SessionInfo>> getSessions() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return ApiResult.ok(authService.getUserSessions(username));
    }

    private String extractToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }
}
