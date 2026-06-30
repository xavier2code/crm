-- 密码历史表（用于密码策略：不能使用最近5次的密码）
CREATE TABLE t_password_history (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_password_history_user FOREIGN KEY (user_id) REFERENCES t_user(id) ON DELETE CASCADE
);
CREATE INDEX idx_password_history_user ON t_password_history(user_id, changed_at DESC);

-- 登录失败记录表（用于账户锁定策略）
CREATE TABLE t_login_failure (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(64) NOT NULL,
    fail_count SMALLINT NOT NULL DEFAULT 1,
    locked_until TIMESTAMP,
    last_fail_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    client_ip VARCHAR(64),
    UNIQUE (username)
);
