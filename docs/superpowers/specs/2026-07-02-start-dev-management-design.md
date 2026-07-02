# start-dev.sh 服务管理功能设计

## 背景

当前 `backend/start-dev.sh` 仅支持启动 dev-local 配置的后端服务。当服务已经运行时再次执行会报 `Port 8080 was already in use`，用户需要手动查找并停止旧进程。为了改善开发体验，需要给脚本增加启动、停止、重启、状态查看等管理能力。

## 目标

扩展 `backend/start-dev.sh`，使其支持 `start | stop | restart | status | help` 命令，无参数时默认执行 `start`。启动时自动检测并停止已存在的 CRM 后端进程，避免端口冲突。

## 方案

采用 **方案 1：扩展 `start-dev.sh`**，在现有脚本基础上增加命令分发和服务管理逻辑。

## 命令行为

| 命令 | 行为 |
|------|------|
| `start`（默认） | 检查 8080 端口；若被 CRM 后端占用则停止旧进程；若被其他进程占用则提示并退出；最后执行 `./gradlew bootRun` |
| `stop` | 查找占用 8080 的 CRM 后端进程并停止；未运行则提示 |
| `restart` | 依次执行 `stop` 和 `start` |
| `status` | 显示服务是否运行及 PID |
| `help` / `-h` / `--help` | 显示用法说明 |

## 进程识别策略

1. 使用 `lsof -i :8080` 获取占用 8080 端口的进程 PID。
2. 使用 `ps -p PID -o command=` 读取该进程的完整命令行。
3. 仅当命令行包含 `com.cy.crm.CrmApplication` 时，才判定为 CRM 后端进程。
4. 对确认是 CRM 后端的进程执行 `kill -9`；对非 CRM 进程输出错误并退出，避免误杀。

## 端口占用处理

| 场景 | 处理方式 |
|------|----------|
| 8080 空闲 | 直接启动 |
| 8080 被 CRM 后端占用 | 自动停止旧进程后启动 |
| 8080 被其他进程占用 | 提示用户，不自动 kill，退出 |
| `lsof` 不可用 | 优雅降级，跳过端口检查并尝试启动 |

## 脚本结构

```bash
#!/bin/bash
set -e

# 1. 环境变量与数据库检查（保留现有逻辑）
# 2. 定义辅助函数：
#    - find_crm_pid()：查找占用 8080 的 CRM 后端 PID
#    - is_crm_process(pid)：判断指定 PID 是否为 CRM 后端
#    - stop_service()：停止 CRM 后端
#    - start_service()：检查端口后启动 ./gradlew bootRun
#    - show_status()：显示运行状态
#    - show_help()：显示用法
# 3. 命令分发：case "$1" in start|stop|restart|status|help|*) ... esac
# 4. 无参数时默认执行 start
```

## 兼容性

- 无参数调用 `./start-dev.sh` 行为与当前基本一致：启动服务。
- 现有环境变量检查（`JWT_SECRET`、数据库连接等）全部保留。
- `./gradlew bootRun` 仍为最终启动命令，保持前台输出和日志直接打印到终端。
- 不引入新的脚本文件或构建工具。

## 错误处理

- 数据库连接检查失败：保持现有行为，脚本退出。
- 端口被非 CRM 进程占用：输出明确错误信息并退出。
- 停止时找不到 CRM 进程：提示"服务未运行"。
- `lsof` 或 `ps` 命令不存在：跳过自动检测，直接尝试启动，避免阻塞。

## 验收标准

- [ ] `./start-dev.sh` 无参数时启动服务
- [ ] `./start-dev.sh start` 在 8080 被旧 CRM 占用时自动停止旧进程并启动
- [ ] `./start-dev.sh stop` 能停止正在运行的 CRM 后端
- [ ] `./start-dev.sh restart` 能先停后启
- [ ] `./start-dev.sh status` 正确显示运行/未运行状态
- [ ] `./start-dev.sh help` 显示用法
- [ ] 不破坏现有 dev-local 启动流程和环境变量检查
