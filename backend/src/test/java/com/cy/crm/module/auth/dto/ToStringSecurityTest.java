package com.cy.crm.module.auth.dto;

import com.cy.crm.module.admin.entity.User;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 测试敏感字段不会出现在 toString() 输出中
 * 防止日志泄露敏感信息
 */
class ToStringSecurityTest {

    @Test
    void loginRequest_toString_shouldNotContainSensitiveData() {
        // given
        LoginRequest request = new LoginRequest();
        request.setUsername("admin");
        request.setPassword("SecretPassword123!");
        request.setCaptchaUuid("captcha-uuid-123");
        request.setCaptchaCode("1234");

        // when
        String toStringResult = request.toString();

        // then - 敏感信息不应该出现在 toString 输出中
        assertFalse(toStringResult.contains("admin"), "toString should not contain username");
        assertFalse(toStringResult.contains("SecretPassword123!"), "toString should not contain password");
        assertFalse(toStringResult.contains("captcha-uuid-123"), "toString should not contain captchaUuid");
        assertFalse(toStringResult.contains("1234"), "toString should not contain captchaCode");
    }

    @Test
    void tokenResponse_toString_shouldNotContainTokens() {
        // given
        TokenResponse response = new TokenResponse();
        response.setAccessToken("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.access.token");
        response.setRefreshToken("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.refresh.token");
        response.setTokenType("Bearer");

        // when
        String toStringResult = response.toString();

        // then - 令牌不应该出现在 toString 输出中
        assertFalse(toStringResult.contains("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.access.token"),
                "toString should not contain accessToken");
        assertFalse(toStringResult.contains("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.refresh.token"),
                "toString should not contain refreshToken");
        // tokenType 是非敏感信息，应该出现
        assertTrue(toStringResult.contains("Bearer"), "toString should contain tokenType");
    }

    @Test
    void user_toString_shouldNotContainPasswordHash() {
        // given
        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setPasswordHash("$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy");
        user.setRealName("Test User");
        user.setPhone("13800138000");

        // when
        String toStringResult = user.toString();

        // then - 密码哈希不应该出现在 toString 输出中
        assertFalse(toStringResult.contains("$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy"),
                "toString should not contain passwordHash");
        // 非敏感字段应该出现
        assertTrue(toStringResult.contains("testuser") || toStringResult.contains("Test User") ||
                toStringResult.contains("13800138000"), "toString should contain non-sensitive fields");
    }
}
