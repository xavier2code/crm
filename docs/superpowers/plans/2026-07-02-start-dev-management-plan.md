# start-dev.sh 服务管理功能实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 扩展 `backend/start-dev.sh`，支持 `start | stop | restart | status | help` 命令，并在启动时自动检测/停止已运行的 CRM 后端进程，避免 8080 端口冲突。

**Architecture:** 在现有 `start-dev.sh` 脚本中增加命令分发和辅助函数（进程查找、CRM 进程识别、停止、状态显示、帮助），保留原有的环境变量设置和数据库检查逻辑。无参数时默认执行 `start`。

**Tech Stack:** Bash, lsof, ps, kill, Gradle

## Global Constraints

- 仅修改 `backend/start-dev.sh`，不新增脚本文件
- 无参数调用 `./start-dev.sh` 行为与当前基本一致：启动服务
- 保留现有 `dev-local` 配置、JWT_SECRET 默认值、数据库连接检查与创建逻辑
- `./gradlew bootRun` 仍为最终启动命令，保持前台输出
- 进程识别以命令行包含 `com.cy.crm.CrmApplication` 为准，避免误杀其他服务
- 端口被非 CRM 进程占用时不自动 kill，提示用户手动处理
- `lsof` 不可用时优雅降级，跳过端口检查直接尝试启动

---

## File Structure

| 文件 | 操作 | 职责 |
|------|------|------|
| `backend/start-dev.sh` | 修改 | 增加服务管理命令与端口占用处理 |

---

### Task 1: 重写 `start-dev.sh` 支持服务管理

**Files:**
- Modify: `backend/start-dev.sh`

**Interfaces:**
- Consumes: 无
- Produces:
  - 命令入口：`./start-dev.sh [start|stop|restart|status|help]`
  - 辅助函数：`find_pid_on_port`, `is_crm_process`, `stop_service`, `start_service`, `show_status`, `show_help`

- [ ] **Step 1: 完整替换 `backend/start-dev.sh`**

将 `backend/start-dev.sh` 完整替换为以下内容：

```bash
#!/bin/bash
set -e

cd "$(dirname "$0")"

PORT=8080
APP_CLASS="com.cy.crm.CrmApplication"

# 获取占用端口的 PID
find_pid_on_port() {
  local pid
  pid=$(lsof -ti :"$PORT" 2>/dev/null || true)
  echo "$pid"
}

# 判断指定 PID 是否为 CRM 后端进程
is_crm_process() {
  local pid=$1
  local cmd
  cmd=$(ps -p "$pid" -o command= 2>/dev/null || true)
  [[ "$cmd" == *"$APP_CLASS"* ]]
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
    echo "==> 正在停止 CRM 后端服务 (PID: $pid) ..."
    kill -9 "$pid"
    sleep 1
    echo "==> 服务已停止"
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
    echo "服务未运行"
  elif is_crm_process "$pid"; then
    echo "服务运行中 (PID: $pid)"
  else
    echo "端口 $PORT 被其他进程占用 (PID: $pid)"
  fi
}

# 显示帮助
show_help() {
  cat <<EOF
用法: ./start-dev.sh [command]

命令:
  start     启动后端服务（默认）
  stop      停止后端服务
  restart   重启后端服务
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
      kill -9 "$pid"
      sleep 1
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
    stop_service
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
```

- [ ] **Step 2: 语法检查**

Run:
```bash
bash -n backend/start-dev.sh
```

Expected: 无输出表示语法正确

- [ ] **Step 3: 测试 help 命令**

Run:
```bash
cd backend
./start-dev.sh help
```

Expected: 显示用法说明

- [ ] **Step 4: 测试 status 命令（服务未运行时）**

Run:
```bash
cd backend
./start-dev.sh status
```

Expected: 输出 `服务未运行`（确保 8080 没有其他服务）

- [ ] **Step 5: 测试 stop 命令（服务未运行时）**

Run:
```bash
cd backend
./start-dev.sh stop
```

Expected: 输出 `==> 服务未运行（端口 8080 空闲）`

- [ ] **Step 6: 测试 start 命令自动处理旧进程**

前置条件：先启动一个后端实例占用 8080，例如：

```bash
cd backend
./gradlew bootRun &
```

等待服务启动后，在另一个终端执行：

```bash
cd backend
./start-dev.sh start
```

Expected:
1. 检测到旧 CRM 进程并停止
2. 执行数据库检查
3. 启动新的 `./gradlew bootRun`
4. 新服务正常监听 8080

注意：此步骤需要手动 Ctrl+C 退出前台运行的 bootRun。

- [ ] **Step 7: 测试 restart 命令**

Run:
```bash
cd backend
./start-dev.sh restart
```

Expected:
1. 如果有旧进程则停止
2. 启动新服务
3. 新服务正常监听 8080

注意：此步骤需要手动 Ctrl+C 退出前台运行的 bootRun。

- [ ] **Step 8: Commit**

```bash
git add backend/start-dev.sh
git commit -m "chore(backend): add start/stop/restart/status to start-dev.sh"
```

---

## Self-Review

### Spec Coverage

| 设计文档要求 | 对应任务 |
|--------------|----------|
| 支持 `start / stop / restart / status / help` 命令 | Task 1 Step 1 `case` 语句 |
| 无参数默认 `start` | Task 1 Step 1 `COMMAND="${1:-start}"` |
| 自动检测并停止占用 8080 的 CRM 后端进程 | Task 1 Step 1 `start_service` 中的端口检查逻辑 |
| 非 CRM 进程占用时提示并退出 | Task 1 Step 1 `start_service` / `stop_service` |
| 保留现有环境变量与数据库检查逻辑 | Task 1 Step 1 `start_service` 前半部分 |
| 保留前台输出 | Task 1 Step 1 最终执行 `./gradlew bootRun` |
| `lsof` 不可用时优雅降级 | Task 1 Step 1 `find_pid_on_port` 中 `2>/dev/null && ... \|\| true` |

### Placeholder Scan

- 无 "TBD"、"TODO"、"implement later" 等占位符
- 每个步骤包含完整脚本代码或具体命令
- 无 "适当处理错误" 等模糊描述

### Type Consistency

- 不适用（Shell 脚本）

---

## Execution Handoff

**Plan complete and saved to `docs/superpowers/plans/2026-07-02-start-dev-management-plan.md`. Two execution options:**

**1. Subagent-Driven (recommended)** - I dispatch a fresh subagent per task, review between tasks, fast iteration

**2. Inline Execution** - Execute tasks in this session using executing-plans, batch execution with checkpoints

**Which approach?**
