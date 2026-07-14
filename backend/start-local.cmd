@echo off
REM =====================================================================
REM 本地启动脚本（Windows · 不依赖 Docker）
REM 对应 doc/dev-env.md §四
REM 用法：
REM   1. 确保本地 MySQL / Redis 已启动
REM   2. （可选）设置环境变量 SPARK_API_PASSWORD
REM   3. 双击本文件或命令行运行 start-local.cmd
REM =====================================================================

setlocal

REM 设置环境变量（CLAUDE.md §14.1：敏感信息禁止提交代码）
REM 生产环境应从外部注入；这里给本地默认值
if "%DB_PASSWORD%"=="" set DB_PASSWORD=hya20050514
if "%DB_USER%"=="" set DB_USER=root
if "%REDIS_HOST%"=="" set REDIS_HOST=localhost
if "%REDIS_PORT%"=="" set REDIS_PORT=6379
if "%SPARK_HOST%"=="" set SPARK_HOST=spark-api-open.xf-yun.com
if "%SPARK_MODEL%"=="" set SPARK_MODEL=generalv3.5
REM SPARK_API_PASSWORD 从 application-local.yml 读取（已被 .gitignore 排除），不通过环境变量注入

REM Spring Profile = dev
set SPRING_PROFILES_ACTIVE=dev

REM 启动应用
echo ============================================
echo  LearnGen Backend · 本地启动
echo ============================================
echo  Profile: dev
echo  MySQL:   localhost:3306 / Random
echo  Redis:   localhost:%REDIS_PORT%
echo  Spark:   %SPARK_HOST% (model=%SPARK_MODEL%)
echo ============================================
echo.

cd /d "%~dp0"

REM 启动 Maven
call mvn spring-boot:run

endlocal