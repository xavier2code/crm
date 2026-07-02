-- 更新 admin 默认用户密码为 123456
-- 注意：此脚本仅用于开发/演示环境，生产环境应使用强密码并通过安全流程分发
UPDATE t_user
SET password_hash = '$2b$12$sad4qMOuTf2MoBKT/sXQAuhPZN1IlONHvrLZv61h8J3cp6IU29vNa',
    updated_at    = CURRENT_TIMESTAMP
WHERE username = 'admin';
