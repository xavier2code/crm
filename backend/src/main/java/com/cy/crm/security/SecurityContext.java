package com.cy.crm.security;

import lombok.Data;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 安全上下文工具类
 * 用于从认证上下文中提取用户信息和数据权限
 */
public class SecurityContext {

    /**
     * 获取当前认证信息
     */
    public static JwtAuthenticationToken getAuthentication() {
        Object authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            return jwtAuth;
        }
        return null;
    }

    /**
     * 获取当前用户ID
     */
    public static Long getCurrentUserId() {
        JwtAuthenticationToken auth = getAuthentication();
        return auth != null ? auth.getUserId() : null;
    }

    /**
     * 获取当前用户的数据权限范围
     */
    public static DataScope getCurrentDataScope() {
        JwtAuthenticationToken auth = getAuthentication();
        return auth != null ? auth.getDataScope() : null;
    }

    /**
     * 获取当前用户的所有上下文信息
     */
    public static UserContext getCurrentUserContext() {
        JwtAuthenticationToken auth = getAuthentication();
        if (auth == null) {
            return null;
        }
        UserContext context = new UserContext();
        context.setUserId(auth.getUserId());
        context.setUsername(auth.getUsername());
        context.setDataScope(auth.getDataScope());
        context.setRoles(auth.getRoles());
        return context;
    }

    /**
     * 用户上下文信息
     */
    @Data
    public static class UserContext {
        private Long userId;
        private String username;
        private DataScope dataScope;
        private java.util.List<String> roles;
    }
}
