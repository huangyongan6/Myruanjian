#!/usr/bin/env bash
# =====================================================================
# 本地启动脚本（macOS / Linux · 不依赖 Docker）
# 对应 doc/dev-env.md §四
# 用法：
#   1. 确保本地 MySQL / Redis 已启动
#   2. （可选）export SPARK_API_PASSWORD=xxx
#   3. bash start-local.sh
# =====================================================================

set -e

# 默认值（CLAUDE.md §14.1：敏感信息禁止提交代码，本地默认值仅供参考）
export DB_PASSWORD="${DB_PASSWORD:-hya20050514}"
export DB_USER="${DB_USER:-root}"
export REDIS_HOST="${REDIS_HOST:-localhost}"
export REDIS_PORT="${REDIS_PORT:-6379}"
export SPARK_HOST="${SPARK_HOST:-spark-api-open.xf-yun.com}"
export SPARK_MODEL="${SPARK_MODEL:-generalv3.5}"
export SPRING_PROFILES_ACTIVE=dev

echo "============================================"
echo " LearnGen Backend · 本地启动"
echo "============================================"
echo " Profile: dev"
echo " MySQL:   localhost:3306 / Random"
echo " Redis:   localhost:${REDIS_PORT}"
echo " Spark:   ${SPARK_HOST} (model=${SPARK_MODEL})"
echo "============================================"

cd "$(dirname "$0")"
mvn spring-boot:run