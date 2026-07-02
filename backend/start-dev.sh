#!/bin/bash
set -eu

cd "$(dirname "$0")"

PORT=8080
# NOTE: PORT must match server.port in application-dev-local.yml
APP_CLASS="com.cy.crm.CrmApplication"

# 获取占用端口的 PID
# 当多个进程占用同一端口时，仅返回第一个；kill 会释放该端口及其关联连接
find_pid_on_port() {
  local pid
  pid=$(lsof -ti :"$PORT" 2>/dev/null | head -1 || true)
  echo "$pid"
}

# 判断指定 PID 是否为 CRM 后端进程
# 使用 args= 而非 command=，避免长命令行被截断导致匹配失败
is_crm_process() {
  local pid=$1
  if [ -z "$pid" ]; then
    return 1
  fi
  local cmd
  cmd=$(ps -p "$pid" -o args= 2>/dev/null || true)
  [[ "$cmd" == *"$APP_CLASS"* ]]
}

# 等待进程完全退出（最多 10 秒）
wait_for_exit() {
  local pid=$1
  local max_wait=10
  local waited=0
  while [ "$waited" -lt "$max_wait" ]; do
    if ! ps -p "$pid" > /dev/null 2>&1; then
      return 0
    fi
    sleep 1
    waited=$((waited + 1))
  done
  return 1
}

# 等待端口释放（最多 10 秒）
wait_for_port_free() {
  local max_wait=10
  local waited=0
  while [ "$waited" -lt "$max_wait" ]; do
    if [ -z "$(find_pid_on_port)" ]; then
      return 0
    fi
    sleep 1
    waited=$((waited + 1))
  done
  return 1
}

# 停止指定 PID 的 CRM 后端进程，失败返回非零
stop_crm_by_pid() {
  local pid=$1
  echo "==> 正在停止 CRM 后端服务 (PID: $pid) ..."
  kill -15 "$pid"
  if wait_for_exit "$pid"; then
    echo "==> 服务已停止"
  else
    echo "==> 服务未在 10 秒内退出，强制终止 ..."
    kill -9 "$pid" 2>/dev/null || true
    sleep 1
    if [ -z "$(find_pid_on_port)" ]; then
      echo "==> 服务已停止"
    else
      echo "ERROR: 无法停止服务 (PID: $pid)"
      return 1
    fi
  fi

  # 最终确认端口已释放
  if ! wait_for_port_free; then
    echo "ERROR: 端口 $PORT 仍未释放，请手动处理。"
    return 1
  fi
}

# 停止 CRM 后端服务
stop_service() {
  local pid
  pid=$(find_pid_on_port)
  if [ -z "$pid" ]; then
    echo "==> 服务未运行（端口 $PORT 空闲）"
    return 0
  fi

  if is_crm_process "$pid"; then
    stop_crm_by_pid "$pid"
  else
    echo "ERROR: 端口 $PORT 被其他进程占用 (PID: $pid)，请手动处理。"
    exit 1
  fi
}

# 检查服务状态
show_status() {
  local pid
  pid=$(find_pid_on_port)
  if [ -z "$pid" ]; then
    echo "==> 服务未运行"
  elif is_crm_process "$pid"; then
    echo "==> 服务运行中 (PID: $pid)"
  else
    echo "==> 端口 $PORT 被其他进程占用 (PID: $pid)"
  fi
}

# 显示帮助
show_help() {
  cat <<EOF
用法: ./start-dev.sh [command] [gradlew-args...]

命令:
  start     启动后端服务（默认，额外参数将传递给 gradlew bootRun）
  stop      停止后端服务
  restart   重启后端服务（额外参数将传递给 gradlew bootRun）
  status    查看服务状态
  help      显示此帮助
EOF
}

# 启动服务前的准备与启动
start_service() {
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

  # 端口检查
  local pid
  pid=$(find_pid_on_port)
  if [ -n "$pid" ]; then
    if is_crm_process "$pid"; then
      echo "==> 端口 $PORT 已被 CRM 后端占用 (PID: $pid)，正在停止旧进程 ..."
      stop_crm_by_pid "$pid" && echo "==> 旧进程已停止"
    else
      echo "ERROR: 端口 $PORT 被其他进程占用 (PID: $pid)，请手动处理。"
      exit 1
    fi
  fi

  # 数据库连接检查
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
}

# 命令分发
COMMAND="${1:-start}"
if [ $# -gt 0 ]; then
  shift
fi

case "$COMMAND" in
  start)
    start_service "$@"
    ;;
  stop)
    stop_service
    ;;
  restart)
    stop_service || exit 1
    start_service "$@"
    ;;
  status)
    show_status
    ;;
  help|-h|--help)
    show_help
    ;;
  *)
    echo "未知命令: $COMMAND"
    show_help
    exit 1
    ;;
esac
