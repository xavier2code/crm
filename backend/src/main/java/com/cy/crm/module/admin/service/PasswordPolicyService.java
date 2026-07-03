package com.cy.crm.module.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.cy.crm.common.exception.BusinessException;
import com.cy.crm.module.admin.entity.LoginFailure;
import com.cy.crm.module.admin.entity.PasswordHistory;
import com.cy.crm.module.admin.entity.User;
import com.cy.crm.module.admin.mapper.LoginFailureMapper;
import com.cy.crm.module.admin.mapper.PasswordHistoryMapper;
import com.cy.crm.module.admin.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * 密码策略服务
 * 处理密码历史、登录失败追踪和账户锁定
 */
@Slf4j
@Service
public class PasswordPolicyService {

    private final PasswordHistoryMapper passwordHistoryMapper;
    private final LoginFailureMapper loginFailureMapper;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    @Value("${password.max-history:5}")
    private int maxHistory;

    @Value("${password.max-fail-count:5}")
    private int maxFailCount;

    @Value("${password.lock-duration:1800}")
    private long lockDuration;

    @Value("${password.expire-days:90}")
    private int expireDays;

    @Value("${password.min-length:8}")
    private int minLength = 8;

    @Value("${password.require-uppercase:true}")
    private boolean requireUppercase = true;

    @Value("${password.require-lowercase:true}")
    private boolean requireLowercase = true;

    @Value("${password.require-digit:true}")
    private boolean requireDigit = true;

    @Value("${password.require-special:true}")
    private boolean requireSpecial = true;

    @Value("${password.special-chars:!@#$%^&*()_+-=[]{}|;':\",./<>?}")
    private String specialChars = "!@#$%^&*()_+-=[]{}|;':\",./<>?";

    private static final String LOGIN_LOCK_PREFIX = "login:lock:";

    public PasswordPolicyService(PasswordHistoryMapper passwordHistoryMapper,
                                 LoginFailureMapper loginFailureMapper,
                                 UserMapper userMapper,
                                 PasswordEncoder passwordEncoder) {
        this.passwordHistoryMapper = passwordHistoryMapper;
        this.loginFailureMapper = loginFailureMapper;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 记录密码历史
     */
    @Transactional(rollbackFor = Exception.class)
    public void recordPasswordHistory(Long userId, String passwordHash) {
        // 保存新密码
        PasswordHistory history = new PasswordHistory();
        history.setUserId(userId);
        history.setPasswordHash(passwordHash);
        history.setChangedAt(LocalDateTime.now());
        passwordHistoryMapper.insert(history);

        // 清理超过maxHistory的旧记录
        List<PasswordHistory> histories = passwordHistoryMapper.selectList(
                new QueryWrapper<PasswordHistory>()
                        .eq("user_id", userId)
                        .orderByDesc("changed_at")
                        .last("LIMIT 100")
        );

        // 保留最近的maxHistory条记录
        if (histories.size() > maxHistory) {
            for (int i = maxHistory; i < histories.size(); i++) {
                passwordHistoryMapper.deleteById(histories.get(i).getId());
            }
        }

        log.debug("Password history recorded for user: {}", userId);
    }

    /**
     * 检查密码是否在历史记录中
     */
    public boolean isPasswordInHistory(Long userId, String rawPassword) {
        List<PasswordHistory> histories = passwordHistoryMapper.selectList(
                new QueryWrapper<PasswordHistory>()
                        .eq("user_id", userId)
                        .orderByDesc("changed_at")
                        .last("LIMIT " + maxHistory)
        );

        for (PasswordHistory history : histories) {
            if (passwordEncoder.matches(rawPassword, history.getPasswordHash())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查密码是否过期
     */
    public boolean isPasswordExpired(User user) {
        // 简化实现：如果用户是初始密码，视为已过期
        if (user.getIsInitialPassword() != null && user.getIsInitialPassword() == 1) {
            return true;
        }

        // TODO: 从密码历史表获取最后一次修改时间，检查是否超过90天
        return false;
    }

    /**
     * 校验密码强度。
     * 默认策略：长度 ≥8，且包含大写字母、小写字母、数字、特殊字符。
     * 规则可通过 application.yml 的 password.* 配置调整。
     */
    public void validateStrength(String rawPassword) {
        if (rawPassword == null || rawPassword.length() < minLength) {
            throw BusinessException.passwordTooWeak("密码长度至少" + minLength + "位");
        }
        if (requireUppercase && !rawPassword.matches(".*[A-Z].*")) {
            throw BusinessException.passwordTooWeak("密码需包含大写字母");
        }
        if (requireLowercase && !rawPassword.matches(".*[a-z].*")) {
            throw BusinessException.passwordTooWeak("密码需包含小写字母");
        }
        if (requireDigit && !rawPassword.matches(".*\\d.*")) {
            throw BusinessException.passwordTooWeak("密码需包含数字");
        }
        if (requireSpecial) {
            String specialClass = "[" + Pattern.quote(specialChars) + "]";
            if (!rawPassword.matches(".*" + specialClass + ".*")) {
                throw BusinessException.passwordTooWeak("密码需包含特殊字符");
            }
        }
    }

    /**
     * 检查账户是否被锁定
     */
    public boolean isAccountLocked(String username) {
        // 先检查Redis中的快速锁
        if (redisTemplate != null) {
            String lockKey = LOGIN_LOCK_PREFIX + username;
            Boolean locked = (Boolean) redisTemplate.opsForValue().get(lockKey);
            if (Boolean.TRUE.equals(locked)) {
                return true;
            }
        }

        // 再检查数据库中的锁定状态
        LoginFailure failure = loginFailureMapper.selectOne(
                new QueryWrapper<LoginFailure>().eq("username", username)
        );

        if (failure != null && failure.getLockedUntil() != null) {
            if (failure.getLockedUntil().isAfter(LocalDateTime.now())) {
                return true;
            } else {
                // 锁定期已过，重置失败计数
                resetFailureCount(username);
            }
        }

        return false;
    }

    /**
     * 记录登录失败
     */
    @Transactional(rollbackFor = Exception.class)
    public void recordLoginFailure(String username, String clientIp) {
        LoginFailure failure = loginFailureMapper.selectOne(
                new QueryWrapper<LoginFailure>().eq("username", username)
        );

        if (failure == null) {
            failure = new LoginFailure();
            failure.setUsername(username);
            failure.setFailCount((short) 1);
        } else {
            failure.setFailCount((short) (failure.getFailCount() + 1));
        }

        failure.setLastFailAt(LocalDateTime.now());
        failure.setClientIp(clientIp);

        // 检查是否需要锁定账户
        if (failure.getFailCount() >= maxFailCount) {
            failure.setLockedUntil(LocalDateTime.now().plusSeconds(lockDuration));

            // 同时设置Redis快速锁（提高性能）
            if (redisTemplate != null) {
                String lockKey = LOGIN_LOCK_PREFIX + username;
                redisTemplate.opsForValue().set(lockKey, true, lockDuration, TimeUnit.SECONDS);
            }

            log.warn("Account {} locked after {} failed attempts", username, failure.getFailCount());
        }

        if (failure.getId() == null) {
            loginFailureMapper.insert(failure);
        } else {
            loginFailureMapper.updateById(failure);
        }
    }

    /**
     * 重置登录失败计数
     */
    @Transactional(rollbackFor = Exception.class)
    public void resetFailureCount(String username) {
        loginFailureMapper.delete(
                new QueryWrapper<LoginFailure>().eq("username", username)
        );

        // 同时清除Redis快速锁
        if (redisTemplate != null) {
            String lockKey = LOGIN_LOCK_PREFIX + username;
            redisTemplate.delete(lockKey);
        }

        log.debug("Failure count reset for user: {}", username);
    }

    /**
     * 获取账户锁定剩余时间
     */
    public Long getRemainingLockTime(String username) {
        // 先检查Redis
        if (redisTemplate != null) {
            String lockKey = LOGIN_LOCK_PREFIX + username;
            Long ttl = redisTemplate.getExpire(lockKey, TimeUnit.SECONDS);
            if (ttl != null && ttl > 0) {
                return ttl;
            }
        }

        // 检查数据库
        LoginFailure failure = loginFailureMapper.selectOne(
                new QueryWrapper<LoginFailure>().eq("username", username)
        );

        if (failure != null && failure.getLockedUntil() != null) {
            LocalDateTime now = LocalDateTime.now();
            if (failure.getLockedUntil().isAfter(now)) {
                return java.time.Duration.between(now, failure.getLockedUntil()).getSeconds();
            }
        }

        return 0L;
    }

    /**
     * 获取失败计数
     */
    public int getFailureCount(String username) {
        LoginFailure failure = loginFailureMapper.selectOne(
                new QueryWrapper<LoginFailure>().eq("username", username)
        );
        return failure != null ? failure.getFailCount() : 0;
    }

    /**
     * 密码过期天数（从 application.yml 的 password.expire-days 读取，默认 90）。
     * 暴露 getter 给 AuthService.login 判定强制改密。
     */
    public int getExpireDays() {
        return expireDays;
    }
}
