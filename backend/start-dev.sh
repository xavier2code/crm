#!/bin/bash
set -e

cd "$(dirname "$0")"

export SPRING_PROFILES_ACTIVE=dev-local

# 如已设置 JWT_SECRET 环境变量则使用，否则使用默认值
if [ -z "$JWT_SECRET" ]; then
  export JWT_SECRET="your-32-byte-or-longer-secret-key-replace-this"
fi

./gradlew bootRun "$@"
