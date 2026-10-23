#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT_DIR"

FRONTEND_DIR="$ROOT_DIR/frontend"
VITE_LOG="$FRONTEND_DIR/vite.log"
VITE_PID_FILE="$FRONTEND_DIR/.vite.pid"
FRONTEND_PORT=5173
GATEWAY_PORT=8080
NACOS_PORT=8848
NACOS_GROUP=MARKETHUB_GROUP
WAIT_TIMEOUT=300
WAIT_INTERVAL=5
FRONTEND_TIMEOUT=60

HEALTHCHECKED_SERVICES=(mysql redis rabbitmq nacos)
BACKEND_SERVICES=(gateway user-service product-service order-service cart-service review-service recommend-service ai-service)
EXPECTED_SERVICES=(markethub-gateway markethub-user-service markethub-product-service markethub-order-service markethub-cart-service markethub-review-service markethub-recommend-service markethub-ai-service)
REQUIRED_TABLES=(role permission role_permission user merchant_application category product orders order_item cart favorite review user_behavior recommendation_cache recommend_eval_log stock_ledger seckill_activity seckill_grab_ledger ai_doc_chunk ai_chat_log)
SECRET_KEYS=(MYSQL_ROOT_PASSWORD MYSQL_PASSWORD JWT_SECRET INTERNAL_AUTH_SECRET REDIS_PASSWORD RABBIT_USER RABBIT_PASS)

if [ -t 1 ]; then
  C_INFO=$'\033[36m'; C_OK=$'\033[32m'; C_WARN=$'\033[33m'; C_ERR=$'\033[31m'; C_OFF=$'\033[0m'
else
  C_INFO=""; C_OK=""; C_WARN=""; C_ERR=""; C_OFF=""
fi

info() { printf '%s[start]%s %s\n' "$C_INFO" "$C_OFF" "$*"; }
ok()   { printf '%s[ ok ]%s %s\n' "$C_OK" "$C_OFF" "$*"; }
warn() { printf '%s[warn]%s %s\n' "$C_WARN" "$C_OFF" "$*" >&2; }
err()  { printf '%s[fail]%s %s\n' "$C_ERR" "$C_OFF" "$*" >&2; }
fail() { err "$*"; exit 1; }

usage() {
  cat <<'EOF'
用法: ./start.sh [命令] [选项]

命令（默认 start）:
  start          启动中间件 + 后端容器，等就绪，再拉起前端
  stop           停掉前端和容器（数据卷保留）
  status         看容器状态、前端进程、缺表情况、Nacos 注册情况
  logs [服务名]  跟踪日志，服务名可省略（例: ./start.sh logs user-service）
  restart        重启已有容器（不会重新编译；改了代码请用 ./start.sh）

选项:
  --reset-db     删除并重建数据卷（数据库/Redis/RabbitMQ 数据全部丢失，需二次确认）
  --yes          配合 --reset-db 使用：跳过确认。非交互环境（管道、CI）里必须加它
  --no-frontend  只起后端，不动前端
  -h, --help     显示这段帮助

首次启动、或容器里要重新编译代码时用:
  ./start.sh
EOF
}

env_value() {
  [ -f "$ROOT_DIR/.env" ] || return 0
  grep -E "^$1=" "$ROOT_DIR/.env" | tail -1 | cut -d= -f2- || true
}

is_placeholder() {
  case "$1" in
    ""|change-me*) return 0 ;;
    *) return 1 ;;
  esac
}

random_hex() {
  if command -v openssl >/dev/null 2>&1; then
    openssl rand -hex "$1"
  else
    head -c "$1" /dev/urandom | od -An -tx1 | tr -d ' \n'
  fi
}

set_env_value() {
  local key="$1" value="$2" tmp found=0 line
  tmp="$(mktemp)"
  while IFS= read -r line || [ -n "$line" ]; do
    case "$line" in
      "$key="*) printf '%s=%s\n' "$key" "$value" >> "$tmp"; found=1 ;;
      *) printf '%s\n' "$line" >> "$tmp" ;;
    esac
  done < "$ROOT_DIR/.env"
  if [ "$found" = "0" ]; then
    printf '\n%s=%s\n' "$key" "$value" >> "$tmp"
  fi
  mv "$tmp" "$ROOT_DIR/.env"
}

ensure_env_file() {
  if [ ! -f "$ROOT_DIR/.env" ]; then
    cp "$ROOT_DIR/.env.example" "$ROOT_DIR/.env"
    info "已根据 .env.example 生成 .env"
  fi
  local changed=0 key current value
  for key in "${SECRET_KEYS[@]}"; do
    current="$(env_value "$key")"
    if is_placeholder "$current"; then
      if [ "$key" = "RABBIT_USER" ]; then
        value="markethub_$(random_hex 4)"
      else
        value="$(random_hex 32)"
      fi
      set_env_value "$key" "$value"
      info "已生成随机值并写入 .env: $key"
      changed=1
    fi
  done
  current="$(env_value MYSQL_USER)"
  if is_placeholder "$current"; then
    set_env_value MYSQL_USER markethub_user
    info "已补上 .env: MYSQL_USER=markethub_user"
    changed=1
  fi
  if [ "$changed" = "0" ]; then
    ok ".env 已存在，密钥和密码都已填过，未做修改"
  fi
  return 0
}

detect_compose() {
  if docker compose version >/dev/null 2>&1; then
    COMPOSE="docker compose"
  elif command -v docker-compose >/dev/null 2>&1; then
    COMPOSE="docker-compose"
  else
    return 1
  fi
  return 0
}

require_docker() {
  command -v docker >/dev/null 2>&1 || fail "没找到 docker 命令。先装 Docker Desktop，再重开一个终端。"
  docker info >/dev/null 2>&1 || fail "Docker 守护进程没在运行。启动 Docker Desktop，等它完全起来（托盘图标不再转圈）后重试。"
  detect_compose || fail "没找到 docker compose(v2) 或 docker-compose(v1)。"
  command -v curl >/dev/null 2>&1 || fail "没找到 curl，脚本要用它探测服务是否真的能响应请求。"
  return 0
}

container_state() {
  local cid
  cid="$($COMPOSE ps -aq "$1" 2>/dev/null | head -1)"
  if [ -z "$cid" ]; then
    echo "未创建"
    return 0
  fi
  docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' "$cid" 2>/dev/null || echo "未知"
}

nacos_endpoint_ok() {
  curl -fsS --max-time 3 "http://localhost:${NACOS_PORT}/nacos/v1/ns/service/list?pageNo=1&pageSize=1&groupName=${NACOS_GROUP}&namespaceId=public" >/dev/null 2>&1
}

registered_services() {
  curl -fsS --max-time 3 "http://localhost:${NACOS_PORT}/nacos/v1/ns/service/list?pageNo=1&pageSize=100&groupName=${NACOS_GROUP}&namespaceId=public" 2>/dev/null \
    | grep -o '"markethub-[a-z-]*"' | tr -d '"' | sort -u || true
}

gateway_ready() {
  local code
  code="$(curl -s -o /dev/null -w '%{http_code}' --max-time 5 "http://localhost:${GATEWAY_PORT}/api/products/on-sale" 2>/dev/null || true)"
  [ "$code" = "200" ]
}

frontend_ready() {
  local code
  code="$(curl -s -o /dev/null -w '%{http_code}' --max-time 3 "http://localhost:${FRONTEND_PORT}/" 2>/dev/null || true)"
  [ -n "$code" ] && [ "$code" != "000" ]
}

existing_tables() {
  $COMPOSE exec -T -e MYSQL_PWD="$(env_value MYSQL_ROOT_PASSWORD)" mysql \
    mysql -uroot -N -B -e "SELECT table_name FROM information_schema.tables WHERE table_schema = 'market_hub'" 2>/dev/null || true
}

missing_tables() {
  local present="$1" t out=""
  for t in "${REQUIRED_TABLES[@]}"; do
    printf '%s\n' "$present" | grep -qx "$t" || out="$out $t"
  done
  printf '%s' "$out"
}

wait_until_ready() {
  local deadline=$(( SECONDS + WAIT_TIMEOUT )) pending state registered left nacos_checked=1 total=${#EXPECTED_SERVICES[@]}
  while [ "$SECONDS" -lt "$deadline" ]; do
    pending=""
    for state in "${HEALTHCHECKED_SERVICES[@]}"; do
      local hs
      hs="$(container_state "$state")"
      [ "$hs" = "healthy" ] || pending="$pending $state($hs)"
    done
    for state in "${BACKEND_SERVICES[@]}"; do
      local bs
      bs="$(container_state "$state")"
      [ "$bs" = "running" ] || pending="$pending $state($bs)"
    done
    if nacos_endpoint_ok; then
      registered="$(registered_services | wc -l | tr -d ' ')"
      [ "$registered" -ge "$total" ] || pending="$pending Nacos注册($registered/$total)"
    else
      nacos_checked=0
    fi
    gateway_ready || pending="$pending 网关未响应"
    if [ -z "$pending" ]; then
      ok "全部服务已就绪（用了 $(( WAIT_TIMEOUT - (deadline - SECONDS) )) 秒）"
      if [ "$nacos_checked" = "0" ]; then
        warn "Nacos 的服务列表接口没按预期响应，跳过了注册数检查（网关探活已通过，所以继续往下走）"
      fi
      return 0
    fi
    left=$(( deadline - SECONDS ))
    [ "$left" -lt 0 ] && left=0
    info "等待中，还剩 ${left} 秒，未就绪：$pending"
    sleep "$WAIT_INTERVAL"
  done
  return 1
}

report_not_ready() {
  err "等了 ${WAIT_TIMEOUT} 秒仍有服务没就绪。容器保持运行，方便你进去看。"
  echo
  $COMPOSE ps || true
  echo
  warn "看某个服务的日志: ./start.sh logs <服务名>    例: ./start.sh logs user-service"
  warn "常见原因：容器里 Maven 编译失败 / Nacos 没起来 / 端口被占用 / 换了中间件密码但数据卷还是旧的"
  warn "拿不准就把日志最后一屏贴出来。"
}

check_tables() {
  local missing
  missing="$(missing_tables "$(existing_tables)")"
  if [ -z "$missing" ]; then
    ok "数据库表齐全（${#REQUIRED_TABLES[@]} 张）"
    return 0
  fi
  err "数据库缺表:$missing"
  warn "schema.sql 只在 MySQL 数据卷首次创建时执行，旧数据卷不会长出这些新表。"
  warn "要么 ./start.sh --reset-db 清库重建（数据全丢），要么把 schema.sql 里对应表的 CREATE TABLE 手动执行一遍。"
  warn "注意：整份 schema.sql 重跑会重复插入 category 等表的数据（那些 INSERT 没写 ON DUPLICATE KEY），只补缺失表的 DDL 更稳。"
  return 1
}

frontend_pid() {
  [ -f "$VITE_PID_FILE" ] || return 1
  local pid
  pid="$(cat "$VITE_PID_FILE" 2>/dev/null | tr -d ' \r\n')"
  [ -n "$pid" ] || return 1
  kill -0 "$pid" 2>/dev/null || return 1
  printf '%s' "$pid"
}

port_owner_pid() {
  if command -v lsof >/dev/null 2>&1; then
    lsof -ti "tcp:${FRONTEND_PORT}" 2>/dev/null | head -1
  elif command -v netstat >/dev/null 2>&1; then
    netstat -ano 2>/dev/null | tr -d '\r' | awk -v p=":${FRONTEND_PORT} " '$0 ~ p && $0 ~ /LISTENING/ {print $NF; exit}'
  fi
  return 0
}

terminate_pid() {
  local pid="$1"
  case "$(uname -s)" in
    MINGW*|MSYS*|CYGWIN*) taskkill //F //PID "$pid" >/dev/null 2>&1 || true ;;
    *) kill "$pid" 2>/dev/null || true ;;
  esac
}

stop_frontend() {
  local pid
  if pid="$(frontend_pid)"; then
    terminate_pid "$pid"
    info "已停止前端进程 $pid"
  fi
  rm -f "$VITE_PID_FILE"
  sleep 1
  local owner
  owner="$(port_owner_pid | tr -d ' \r\n')"
  if [ -n "$owner" ]; then
    warn "端口 ${FRONTEND_PORT} 仍被进程 $owner 占用（npm 在 Windows 下会再派生一个子进程），强制结束它"
    terminate_pid "$owner"
    sleep 1
  fi
  if frontend_ready; then
    warn "端口 ${FRONTEND_PORT} 还有响应，可能没停干净。查一下占用者：netstat -ano | findstr :${FRONTEND_PORT}"
  fi
  return 0
}

start_frontend() {
  command -v npm >/dev/null 2>&1 || {
    warn "没找到 npm，跳过前端。只起后端可以永远加 --no-frontend。"
    return 0
  }
  local pid
  if pid="$(frontend_pid)"; then
    info "前端已在运行（pid $pid）"
    return 0
  fi
  if [ ! -d "$FRONTEND_DIR/node_modules" ]; then
    info "没有 frontend/node_modules，先执行 npm ci（第一次会比较慢）"
    ( cd "$FRONTEND_DIR" && npm ci )
  fi
  info "后台启动前端，日志写到 frontend/vite.log"
  (
    cd "$FRONTEND_DIR"
    nohup npm run dev > "$VITE_LOG" 2>&1 < /dev/null &
    echo $! > "$VITE_PID_FILE"
  )
  local waited=0
  while ! frontend_ready; do
    waited=$(( waited + 2 ))
    if [ "$waited" -ge "$FRONTEND_TIMEOUT" ]; then
      warn "前端 ${FRONTEND_TIMEOUT} 秒内没起来，看 frontend/vite.log"
      return 0
    fi
    sleep 2
  done
  ok "前端已就绪"
  return 0
}

print_summary() {
  cat <<EOF

前端      http://localhost:${FRONTEND_PORT}
网关      http://localhost:${GATEWAY_PORT}
Nacos     http://localhost:${NACOS_PORT}/nacos
RabbitMQ  http://localhost:15672
Sentinel  http://localhost:8718
Swagger   http://localhost:8081~8087/swagger-ui/index.html

测试账号  admin/admin123（管理员）、merchant1/merchant123（商家）、buyer1/buyer123（买家）

停止      ./start.sh stop
看状态    ./start.sh status
看日志    ./start.sh logs user-service
更多验证  环境配置.docx
EOF
}

rebuild_db() {
  warn "--reset-db 会删掉 markethub 的全部数据卷：数据库、Redis、RabbitMQ 数据都会丢。"
  if [ "$ASSUME_YES" = "1" ]; then
    info "已指定 --yes，直接执行。"
  elif [ -t 0 ]; then
    local answer=""
    read -r -p "确认继续请输入 yes 回车: " answer || answer=""
    [ "$answer" = "yes" ] || fail "没有输入 yes，已取消。"
  else
    fail "当前不是交互终端，为避免误删数据不放行。确认要重建就加 --yes：./start.sh --reset-db --yes"
  fi
  $COMPOSE down -v
  ok "数据卷已删除，下一步会重新创建并执行 schema.sql"
}

cmd_start() {
  ensure_env_file
  require_docker
  if [ "$RESET_DB" = "1" ]; then
    rebuild_db
  fi
  info "启动容器。第一次构建要在容器里下载 Maven 依赖再编译，可能好几分钟，耐心等。"
  $COMPOSE up -d --build
  if ! wait_until_ready; then
    report_not_ready
    exit 1
  fi
  if ! check_tables; then
    exit 1
  fi
  if [ "$WITH_FRONTEND" = "1" ]; then
    start_frontend
  else
    info "--no-frontend：不动前端"
  fi
  print_summary
}

cmd_stop() {
  stop_frontend
  if docker info >/dev/null 2>&1 && detect_compose; then
    $COMPOSE stop
    ok "容器已停止，数据卷还在，./start.sh 可以再起来"
  else
    warn "Docker 没在运行，只处理了前端"
  fi
}

cmd_restart() {
  require_docker
  info "重启容器（不会重新编译，改了代码请用 ./start.sh）"
  $COMPOSE restart
  if ! wait_until_ready; then
    report_not_ready
    exit 1
  fi
  if [ "$WITH_FRONTEND" = "1" ]; then
    stop_frontend
    start_frontend
  fi
  print_summary
}

cmd_status() {
  require_docker
  $COMPOSE ps
  echo
  local pid
  if pid="$(frontend_pid)"; then
    ok "前端：运行中（pid $pid）"
  else
    warn "前端：没在运行"
  fi
  local missing
  if [ "$(container_state mysql)" = "healthy" ]; then
    missing="$(missing_tables "$(existing_tables)")"
    if [ -z "$missing" ]; then
      ok "数据库表：齐全（${#REQUIRED_TABLES[@]} 张）"
    else
      warn "数据库缺表:$missing"
    fi
  else
    warn "MySQL 容器没在跑，跳过缺表检查"
  fi
  local registered
  registered="$(registered_services | tr '\n' ' ')"
  if [ -n "$registered" ]; then
    ok "Nacos 已注册：$registered"
  elif nacos_endpoint_ok; then
    warn "Nacos：一个服务都没注册（Nacos 起来了，但服务还没启动完或启动失败）"
  else
    warn "Nacos：服务列表接口没响应，判断不了注册情况（Nacos 可能没起来）"
  fi
  if gateway_ready; then
    ok "网关：能正常响应匿名请求"
  else
    warn "网关：没响应，看 ./start.sh logs gateway"
  fi
  return 0
}

cmd_logs() {
  require_docker
  if [ -n "$LOG_SERVICE" ]; then
    $COMPOSE logs -f --tail=200 "$LOG_SERVICE"
  else
    $COMPOSE logs -f --tail=100
  fi
}

COMMAND="start"
RESET_DB=0
ASSUME_YES=0
WITH_FRONTEND=1
LOG_SERVICE=""

while [ $# -gt 0 ]; do
  case "$1" in
    start|stop|status|restart) COMMAND="$1" ;;
    logs) COMMAND="logs" ;;
    -h|--help|help) COMMAND="help" ;;
    --reset-db) RESET_DB=1 ;;
    --yes|-y) ASSUME_YES=1 ;;
    --no-frontend) WITH_FRONTEND=0 ;;
    *)
      if [ "$COMMAND" = "logs" ] && [ -z "$LOG_SERVICE" ]; then
        LOG_SERVICE="$1"
      else
        err "不认识这个参数: $1"
        echo
        usage
        exit 2
      fi
      ;;
  esac
  shift
done

case "$COMMAND" in
  start)   cmd_start ;;
  stop)    cmd_stop ;;
  status)  cmd_status ;;
  logs)    cmd_logs ;;
  restart) cmd_restart ;;
  help)    usage ;;
esac
