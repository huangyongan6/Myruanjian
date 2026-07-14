# =====================================================================
# 本地启动前检查脚本（可选）
# 对应 doc/dev-env.md §七 首次启动 Checklist
# 用法：bash check-env.sh 或 .\check-env.cmd
# =====================================================================

set -e

ok() { echo "[✓] $1"; }
fail() { echo "[✗] $1"; }

echo "============================================"
echo " LearnGen 环境检查"
echo "============================================"

# JDK
if command -v java >/dev/null 2>&1; then
    JAVA_VER=$(java -version 2>&1 | head -1 | awk -F '"' '{print $2}')
    if [[ "$JAVA_VER" == 17* ]]; then
        ok "JDK 版本：$JAVA_VER"
    else
        fail "JDK 版本 $JAVA_VER 非 17，建议安装 Temurin 17"
    fi
else
    fail "未找到 java，请安装 JDK 17"
fi

# Maven
if command -v mvn >/dev/null 2>&1; then
    ok "Maven：$(mvn -v | head -1)"
else
    fail "未找到 mvn，请安装 Maven 3.9+"
fi

# Node.js
if command -v node >/dev/null 2>&1; then
    ok "Node.js：$(node -v)"
else
    fail "未找到 node，如需前端开发请安装 Node 18+"
fi

# MySQL
if command -v mysql >/dev/null 2>&1; then
    if mysql -h localhost -P 3306 -u root -p"${DB_PASSWORD:-hya20050514}" -e "SELECT 1" >/dev/null 2>&1; then
        ok "MySQL：可连接 localhost:3306"
    else
        fail "MySQL 连接失败，请检查密码 / 端口"
    fi
else
    echo "[?] 未安装 mysql 命令行工具，可通过 Docker 运行"
fi

# Redis
if command -v redis-cli >/dev/null 2>&1; then
    if redis-cli -h localhost -p "${REDIS_PORT:-6379}" ping >/dev/null 2>&1; then
        ok "Redis：可连接 localhost:${REDIS_PORT:-6379}"
    else
        fail "Redis ping 失败"
    fi
else
    echo "[?] 未安装 redis-cli"
fi

echo "============================================"