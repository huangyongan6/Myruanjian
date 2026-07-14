@echo off
REM Windows 版环境检查脚本
setlocal

set OK=[√]
set FAIL=[X]

echo ============================================
echo  LearnGen 环境检查
echo ============================================

where java >nul 2>nul
if %ERRORLEVEL%==0 (
    echo %OK% 找到 java
) else (
    echo %FAIL% 未找到 java
)

where mvn >nul 2>nul
if %ERRORLEVEL%==0 (
    echo %OK% 找到 mvn
) else (
    echo %FAIL% 未找到 mvn
)

where mysql >nul 2>nul
if %ERRORLEVEL%==0 (
    echo %OK% 找到 mysql
) else (
    echo [?] 未找到 mysql，建议通过 Docker 运行
)

where redis-cli >nul 2>nul
if %ERRORLEVEL%==0 (
    echo %OK% 找到 redis-cli
) else (
    echo [?] 未找到 redis-cli
)

echo ============================================
endlocal