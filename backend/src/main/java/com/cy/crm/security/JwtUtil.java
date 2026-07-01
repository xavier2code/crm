package com.cy.crm.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Component
public class JwtUtil {

    private final SecretKey accessKey;
    private final long accessExpire;
    private final long refreshExpire;
    private final String issuer;

    public JwtUtil(@Value("${jwt.secret}") String secret,
                   @Value("${jwt.access-expire}") long accessExpire,
                   @Value("${jwt.refresh-expire}") long refreshExpire,
                   @Value("${jwt.issuer:crm-system}") String issuer) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException(
                "JWT secret must be at least 32 bytes, configured via JWT_SECRET environment variable");
        }
        this.accessKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessExpire = accessExpire;
        this.refreshExpire = refreshExpire;
        this.issuer = issuer;
    }

    /**
     * 生成访问令牌（完整JWT claims）
     */
    public String generateAccessToken(Long userId, String username, List<String> roles,
                                     List<String> menus, List<String> ops, DataScope dataScope) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + accessExpire * 1000);
        String jti = UUID.randomUUID().toString();

        return Jwts.builder()
                .id(jti)                                    // jti: JWT ID
                .issuer(issuer)                             // iss: issuer
                .subject(username)                          // sub: username
                .claim("userId", userId)                     // 用户ID
                .claim("roles", roles)                      // 角色列表
                .claim("menus", menus)                       // 菜单权限
                .claim("ops", ops)                          // 操作权限
                .claim("dataScope", dataScope)              // 数据权限范围
                .issuedAt(now)                              // iat: issued at
                .expiration(expiry)                          // exp: expiration
                .signWith(accessKey)
                .compact();
    }

    /**
     * 生成刷新令牌
     */
    public String generateRefreshToken(String username) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + refreshExpire * 1000);
        String jti = UUID.randomUUID().toString();

        return Jwts.builder()
                .id(jti)
                .issuer(issuer)
                .subject(username)
                .claim("type", "refresh")
                .issuedAt(now)
                .expiration(expiry)
                .signWith(accessKey)
                .compact();
    }

    /**
     * 提取JWT ID（jti）
     */
    public String extractJti(String token) {
        return parseClaims(token).getId();
    }

    /**
     * 提取签发者（iss）
     */
    public String extractIssuer(String token) {
        return parseClaims(token).getIssuer();
    }

    /**
     * 提取用户名（sub）
     */
    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    /**
     * 提取用户ID
     */
    public Long extractUserId(String token) {
        return parseClaims(token).get("userId", Long.class);
    }

    /**
     * 提取角色列表
     */
    @SuppressWarnings("unchecked")
    public List<String> extractRoles(String token) {
        return parseClaims(token).get("roles", List.class);
    }

    /**
     * 提取菜单权限
     */
    @SuppressWarnings("unchecked")
    public List<String> extractMenus(String token) {
        return parseClaims(token).get("menus", List.class);
    }

    /**
     * 提取操作权限
     */
    @SuppressWarnings("unchecked")
    public List<String> extractOps(String token) {
        return parseClaims(token).get("ops", List.class);
    }

    /**
     * 提取数据权限范围
     */
    public DataScope extractDataScope(String token) {
        return parseClaims(token).get("dataScope", DataScope.class);
    }

    /**
     * 提取令牌类型
     */
    public String extractTokenType(String token) {
        return parseClaims(token).get("type", String.class);
    }

    /**
     * 验证令牌
     */
    public boolean validateToken(String token) {
        try {
            Claims claims = parseClaims(token);
            // 检查签发者
            if (!issuer.equals(claims.getIssuer())) {
                return false;
            }
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * 验证访问令牌
     * 拒绝刷新令牌被用作访问令牌
     */
    public boolean validateAccessToken(String token) {
        try {
            Claims claims = parseClaims(token);
            // 检查签发者
            if (!issuer.equals(claims.getIssuer())) {
                return false;
            }
            // 访问令牌不应有 type claim 或 type 不为 "refresh"
            String type = claims.get("type", String.class);
            if ("refresh".equals(type)) {
                return false;
            }
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * 检查令牌是否过期
     */
    public boolean isTokenExpired(String token) {
        try {
            Date expiration = parseClaims(token).getExpiration();
            return expiration.before(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            return true;
        }
    }

    /**
     * 提取过期时间（秒级时间戳）
     */
    public Long extractExpiration(String token) {
        Date expiration = parseClaims(token).getExpiration();
        return expiration.getTime() / 1000;
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(accessKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
