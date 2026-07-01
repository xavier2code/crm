#!/bin/bash
set -e

cd "$(dirname "$0")"

# 开发环境配置
export SPRING_PROFILES_ACTIVE=dev-local

# 如已设置 JWT_SECRET 环境变量则使用，否则使用默认值
if [ -z "$JWT_SECRET" ]; then
  export JWT_SECRET="your-32-byte-or-longer-secret-key-replace-this"
fi

# 数据库连接信息（与 application-dev-local.yml 保持一致）
DB_HOST="${DB_HOST:-192.168.2.222}"
DB_PORT="${DB_PORT:-5432}"
DB_NAME="${DB_NAME:-crm}"
DB_USER="${DB_USERNAME:-postgres}"
DB_PASS="${DB_PASSWORD:-123456}"

echo "==> 检查 PostgreSQL 连接 ($DB_HOST:$DB_PORT) ..."
if ! PGPASSWORD="$DB_PASS" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d postgres -c "SELECT 1;" > /dev/null 2>&1; then
  echo "ERROR: 无法连接到 PostgreSQL，请确认服务已启动且用户名/密码正确。"
  exit 1
fi

echo "==> 检查数据库 $DB_NAME 是否存在 ..."
if ! PGPASSWORD="$DB_PASS" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d postgres -tc "SELECT 1 FROM pg_database WHERE datname = '$DB_NAME';" | grep -q 1; then
  echo "==> 创建数据库 $DB_NAME ..."
  PGPASSWORD="$DB_PASS" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d postgres -c "CREATE DATABASE $DB_NAME;"
else
  echo "==> 数据库 $DB_NAME 已存在"
fi

echo "==> 启动后端服务 ..."
./gradlew bootRun "$@"
