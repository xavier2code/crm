package com.cy.crm.module.auth.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Token 黑名单服务
 * 用于管理退出登录的Token和会话管理
 */
@Slf4j
@Service
public class TokenBlacklistService {

    private static final String BLACKLIST_PREFIX = "token:blacklist:";
    private static final String SESSION_PREFIX = "session:";
    private static final String USER_SESSIONS_PREFIX = "user:sessions:";

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 将Token加入黑名单
     * @param jti JWT ID
     * @param ttl 过期时间（秒）
     */
    public void addToBlacklist(String jti, long ttl) {
        if (redisTemplate == null) {
            log.warn("Redis unavailable, cannot add token to blacklist");
            return;
        }
        String key = BLACKLIST_PREFIX + jti;
        redisTemplate.opsForValue().set(key, ttl, ttl, TimeUnit.SECONDS);
        log.debug("Token {} added to blacklist, TTL: {}s", jti, ttl);
    }

    /**
     * 检查Token是否在黑名单中
     * @param jti JWT ID
     * @return 是否在黑名单中
     */
    public boolean isBlacklisted(String jti) {
        if (redisTemplate == null) {
            log.debug("Redis unavailable, token not considered blacklisted");
            return false;
        }
        String key = BLACKLIST_PREFIX + jti;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * 保存用户会话信息
     * @param username 用户名
     * @param jti JWT ID
     * @param sessionInfo 会话信息
     * @param ttl 过期时间（秒）
     */
    public void saveSession(String username, String jti, SessionInfo sessionInfo, long ttl) {
        if (redisTemplate == null) {
            log.warn("Redis unavailable, cannot save session");
            return;
        }
        String sessionKey = SESSION_PREFIX + jti;
        redisTemplate.opsForValue().set(sessionKey, sessionInfo, ttl, TimeUnit.SECONDS);

        // 将会话添加到用户的会话列表
        String userSessionsKey = USER_SESSIONS_PREFIX + username;
        redisTemplate.opsForSet().add(userSessionsKey, jti);
        redisTemplate.expire(userSessionsKey, Duration.ofDays(30)); // 用户会话列表保留30天

        log.debug("Session saved for user: {}, jti: {}", username, jti);
    }

    /**
     * 获取会话信息
     * @param jti JWT ID
     * @return 会话信息
     */
    public SessionInfo getSession(String jti) {
        if (redisTemplate == null) {
            return null;
        }
        String key = SESSION_PREFIX + jti;
        return (SessionInfo) redisTemplate.opsForValue().get(key);
    }

    /**
     * 删除会话
     * @param jti JWT ID
     */
    public void removeSession(String jti) {
        if (redisTemplate == null) {
            log.warn("Redis unavailable, cannot remove session");
            return;
        }
        String sessionKey = SESSION_PREFIX + jti;
        SessionInfo sessionInfo = (SessionInfo) redisTemplate.opsForValue().get(sessionKey);
        if (sessionInfo != null) {
            // 从用户的会话列表中移除
            String userSessionsKey = USER_SESSIONS_PREFIX + sessionInfo.getUsername();
            redisTemplate.opsForSet().remove(userSessionsKey, jti);
        }
        redisTemplate.delete(sessionKey);
        log.debug("Session removed: {}", jti);
    }

    /**
     * 删除用户的所有会话
     * @param username 用户名
     */
    public void removeAllSessions(String username) {
        if (redisTemplate == null) {
            log.warn("Redis unavailable, cannot remove sessions");
            return;
        }
        String userSessionsKey = USER_SESSIONS_PREFIX + username;
        Set<Object> jtis = redisTemplate.opsForSet().members(userSessionsKey);

        if (jtis != null && !jtis.isEmpty()) {
            for (Object jti : jtis) {
                String sessionKey = SESSION_PREFIX + jti;
                redisTemplate.delete(sessionKey);

                // 将Token加入黑名单
                addToBlacklist((String) jti, 3600); // 1小时
            }
        }

        // 清空用户的会话列表
        redisTemplate.delete(userSessionsKey);
        log.info("All sessions removed for user: {}", username);
    }

    /**
     * 获取用户的所有会话
     * @param username 用户名
     * @return 会话信息列表
     */
    public List<SessionInfo> getUserSessions(String username) {
        if (redisTemplate == null) {
            log.debug("Redis unavailable, returning empty session list");
            return Collections.emptyList();
        }
        String userSessionsKey = USER_SESSIONS_PREFIX + username;
        Set<Object> jtis = redisTemplate.opsForSet().members(userSessionsKey);

        if (jtis == null || jtis.isEmpty()) {
            return Collections.emptyList();
        }

        List<SessionInfo> sessions = new ArrayList<>();
        for (Object jti : jtis) {
            SessionInfo info = getSession((String) jti);
            if (info != null) {
                sessions.add(info);
            }
        }

        return sessions;
    }

    /**
     * 会话信息
     */
    @lombok.Data
    public static class SessionInfo {
        private String username;
        private String jti;
        private java.time.LocalDateTime loginTime;
        private java.time.LocalDateTime lastActiveTime;
        private String clientIp;
        private String userAgent;
    }
}
