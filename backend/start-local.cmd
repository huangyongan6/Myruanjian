@echo off
REM =====================================================================
REM 本地启动脚本（Windows · 不依赖 Docker）
REM 对应 doc/dev-env.md §四
REM 用法：
REM   1. 确保本地 MySQL / Redis 已启动
REM   2. 双击本文件或命令行运行 start-local.cmd
REM =====================================================================

REM =====================================================================
REM 加载 .env 环境变量文件（如果存在）
REM =====================================================================
set "ENV_FILE=%~dp0..\.env"
set SPARK_API_PASSWORD_PRIMARY=
set SPARK_API_PASSWORD_SECONDARY=

if exist "%ENV_FILE%" (
    for /f "usebackq tokens=1,* delims==" %%a in ("%ENV_FILE%") do (
        set "KEY=%%a"
        set "VALUE=%%b"
        REM 去除 VALUE 开头和结尾的引号
        set "VALUE=!VALUE:"=!"
        if not "!KEY!"=="" if not "!KEY:~0,1!"=="#" (
            if "!KEY!"=="SPARK_API_PASSWORD_PRIMARY" set SPARK_API_PASSWORD_PRIMARY=!VALUE!
            if "!KEY!"=="SPARK_API_PASSWORD_SECONDARY" set SPARK_API_PASSWORD_SECONDARY=!VALUE!
        )
    )
    echo [INFO] 已从 .env 加载环境变量
)

REM 设置默认值
if "%SPARK_API_PASSWORD_PRIMARY%"=="" (
    echo [WARN] SPARK_API_PASSWORD_PRIMARY 未配置，请在 .env 文件中设置
)

REM Spring Profile = dev
set SPRING_PROFILES_ACTIVE=dev

REM 启动应用（通过 -D 直接传递环境变量给 JVM）
echo ============================================
echo  LearnGen Backend · 本地启动
echo ============================================
echo  Profile: dev
echo  MySQL:   localhost:3306 / Random
echo  Redis:   localhost:6379
echo  Spark:   spark-api-open.xf-yun.com (model=generalv3.5)
echo ============================================
echo.

cd /d "%~dp0"

REM 启动 Maven（通过 -D 传递敏感信息，绕过子进程环境变量继承问题）
call mvn spring-boot:run ^
    -DSPARK_API_PASSWORD_PRIMARY=%SPARK_API_PASSWORD_PRIMARY% ^
    -DSPARK_API_PASSWORD_SECONDARY=%SPARK_API_PASSWORD_SECONDARY%