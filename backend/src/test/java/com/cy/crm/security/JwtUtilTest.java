package com.cy.crm.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JwtUtil 单元测试 - 令牌类型验证
 */
class JwtUtilTest {

    private JwtUtil jwtUtil;
    private String accessJwt;
    private String refreshJwt;

    @BeforeEach
    void setUp() {
        String secret = "this-is-a-test-secret-key-that-is-at-least-32-bytes-long-for-testing";
        jwtUtil = new JwtUtil(secret, 3600, 86400, "test-issuer", new ObjectMapper());

        // 手动创建一个没有 type claim 的访问令牌
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        long now = System.currentTimeMillis();
        accessJwt = Jwts.builder()
                .issuer("test-issuer")
                .subject("testuser")
                .claim("userId", 1L)
                .issuedAt(new Date(now))
                .expiration(new Date(now + 3600000))
                .signWith(key)
                .compact();

        // 手动创建一个有 type="refresh" claim 的刷新令牌
        refreshJwt = Jwts.builder()
                .issuer("test-issuer")
                .subject("testuser")
                .claim("type", "refresh")
                .issuedAt(new Date(now))
                .expiration(new Date(now + 86400000))
                .signWith(key)
                .compact();
    }

    @Test
    void validateAccessToken_shouldReturnTrue_forValidAccessToken() {
        // 访问令牌（没有 type claim）应该通过验证
        assertTrue(jwtUtil.validateAccessToken(accessJwt));
    }

    @Test
    void validateAccessToken_shouldReturnFalse_forRefreshToken() {
        // 刷新令牌（type="refresh"）应该被拒绝
        assertFalse(jwtUtil.validateAccessToken(refreshJwt));
    }

    @Test
    void validateToken_shouldReturnTrue_forRefreshToken() {
        // 旧的 validateToken 方法应该接受刷新令牌（仅验证issuer）
        assertTrue(jwtUtil.validateToken(refreshJwt));
    }

    @Test
    void validateAccessToken_shouldReturnFalse_forInvalidIssuer() {
        // 创建一个错误签发者的令牌
        String secret = "this-is-a-test-secret-key-that-is-at-least-32-bytes-long-for-testing";
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        long now = System.currentTimeMillis();
        String invalidIssuerJwt = Jwts.builder()
                .issuer("wrong-issuer")
                .subject("testuser")
                .issuedAt(new Date(now))
                .expiration(new Date(now + 3600000))
                .signWith(key)
                .compact();

        assertFalse(jwtUtil.validateAccessToken(invalidIssuerJwt));
    }

    @Test
    void extractTokenType_shouldReturnRefresh_forRefreshToken() {
        assertEquals("refresh", jwtUtil.extractTokenType(refreshJwt));
    }

    @Test
    void extractTokenType_shouldReturnNull_forAccessToken() {
        assertNull(jwtUtil.extractTokenType(accessJwt));
    }
}
