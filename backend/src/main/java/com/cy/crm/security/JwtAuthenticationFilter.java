package com.cy.crm.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final com.cy.crm.module.auth.service.TokenBlacklistService tokenBlacklistService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        // 检查令牌是否在黑名单中
        String jti = jwtUtil.extractJti(token);
        if (jti != null && tokenBlacklistService.isBlacklisted(jti)) {
            log.warn("Token in blacklist: {}", jti);
            filterChain.doFilter(request, response);
            return;
        }

        if (jwtUtil.validateAccessToken(token) && !jwtUtil.isTokenExpired(token)) {
            try {
                String username = jwtUtil.extractUsername(token);

                // 从JWT claims中构建权限列表
                // 角色需要以 ROLE_ 前缀加入 authorities，才能被 hasRole/hasAnyRole 识别
                List<String> roles = jwtUtil.extractRoles(token);
                List<String> ops = jwtUtil.extractOps(token);

                // 强制改密临时 token：没有角色/操作权限，但允许访问改密接口
                boolean changePasswordOnly = Boolean.TRUE.equals(jwtUtil.extractChangePasswordFlag(token));
                if (changePasswordOnly) {
                    if (!isChangePasswordRequest(request)) {
                        log.warn("Change-password-only token used for non-change-password request: {}", request.getRequestURI());
                        filterChain.doFilter(request, response);
                        return;
                    }
                    roles = Collections.emptyList();
                    ops = Collections.emptyList();
                }

                List<SimpleGrantedAuthority> authorities = Stream.concat(
                        roles.stream().map(role -> new SimpleGrantedAuthority("ROLE_" + role)),
                        ops.stream().map(SimpleGrantedAuthority::new)
                ).collect(Collectors.toList());

                // 创建认证对象
                Long userId = jwtUtil.extractUserId(token);
                JwtAuthenticationToken authentication = new JwtAuthenticationToken(
                        username,
                        authorities,
                        userId,
                        jwtUtil.extractRoles(token),
                        jwtUtil.extractMenus(token),
                        jwtUtil.extractDataScope(token)
                );
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);

                // 将用户ID写入MDC，便于日志追踪
                if (userId != null) {
                    MDC.put("userId", String.valueOf(userId));
                }

                log.debug("Set authentication for user: {} with authorities: {}", username, ops);
            } catch (Exception e) {
                log.error("JWT authentication failed", e);
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isChangePasswordRequest(HttpServletRequest request) {
        return "POST".equalsIgnoreCase(request.getMethod())
                && request.getRequestURI() != null
                && request.getRequestURI().endsWith("/api/auth/change-password");
    }
}
