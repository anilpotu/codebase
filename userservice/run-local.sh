#!/usr/bin/env bash
# =============================================================================
# run-local.sh  —  Build and run all userservice services locally via Docker
# Run this script from the userservice/ project root.
#
# Usage: ./run-local.sh [command] [options]
#
# Commands:
#   build           Maven build for all Java modules (required for SDS-origin services)
#   up              Build Docker images and start all containers (detached)
#   up --follow     Build Docker images, start containers, then follow logs
#   down            Stop and remove all containers, networks
#   down --volumes  Stop containers and remove named volumes (wipes DB data)
#   logs [service]  Tail logs from all containers, or a specific service
#   status          Show running container status and health
#   restart         Full stop → build → up cycle
#   (no command)    Default: build → up (detached)
#
# Service names for 'logs':
#   config-server  eureka-server  api-gateway  auth-service
#   user-service   order-service  product-service
#   user-grpc-service  financial-service  health-service  social-service
#   enterprise-ui  postgres  redis
#
# Examples:
#   ./run-local.sh
#   ./run-local.sh build
#   ./run-local.sh up --follow
#   ./run-local.sh logs financial-service
#   ./run-local.sh down --volumes
#   ./run-local.sh restart
# =============================================================================
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
COMPOSE_FILE="$PROJECT_DIR/docker-compose.yml"

# ── colours ──────────────────────────────────────────────────────────────────
RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'
CYAN='\033[0;36m'; BOLD='\033[1m'; NC='\033[0m'

log()   { echo -e "${CYAN}[RUN]${NC}    $*"; }
ok()    { echo -e "${GREEN}[OK]${NC}     $*"; }
warn()  { echo -e "${YELLOW}[WARN]${NC}   $*"; }
fail()  { echo -e "${RED}[FAIL]${NC}   $*"; exit 1; }
header(){ echo -e "\n${BOLD}${YELLOW}═══ $* ═══${NC}\n"; }

# ── prerequisite checks ───────────────────────────────────────────────────────
check_prereqs() {
  header "Checking prerequisites"

  command -v docker >/dev/null 2>&1 || fail "docker not found. Install Docker Desktop or Docker Engine."
  # On Windows with Docker Desktop WSL2 backend, docker info exits non-zero in Git Bash
  # even when the daemon is running. Check for "Server Version" in output instead.
  docker info 2>&1 | grep -q "Server Version" \
    || fail "Docker daemon is not running. Start Docker Desktop and retry."
  ok "Docker: $(docker --version)"

  if docker compose version >/dev/null 2>&1; then
    COMPOSE_CMD="docker compose"
  elif command -v docker-compose >/dev/null 2>&1; then
    COMPOSE_CMD="docker-compose"
  else
    fail "Neither 'docker compose' nor 'docker-compose' found. Upgrade Docker Desktop."
  fi
  ok "Compose: $($COMPOSE_CMD version --short 2>/dev/null || $COMPOSE_CMD version)"

  command -v mvn  >/dev/null 2>&1 || fail "mvn not found. Install Maven 3.8+ and add it to PATH."
  command -v java >/dev/null 2>&1 || fail "java not found. Install Java 11 JDK."
  ok "Maven:  $(mvn -version 2>&1 | head -1)"
  ok "Java:   $(java -version 2>&1 | head -1)"
}

# ── phase 1: maven build ──────────────────────────────────────────────────────
# SDS-origin Dockerfiles (config-server, eureka-server, api-gateway, auth-service,
# user-service, order-service, product-service) are single-stage and copy
# target/*.jar — they require a pre-built JAR from this step.
# gRPC-origin Dockerfiles (user-grpc-service, financial-service, health-service,
# social-service) run Maven inside Docker, but building here warms the local
# cache so the same JARs can also be run directly with 'java -jar'.
build_maven() {
  header "Maven build — userservice (all modules)"
  log "Working directory: $PROJECT_DIR"
  log "Running: mvn clean package -DskipTests --batch-mode --no-transfer-progress"
  echo ""

  (
    export MAVEN_OPTS="-Xmx1024m"
    cd "$PROJECT_DIR"
    mvn clean package -DskipTests --batch-mode --no-transfer-progress
  ) && ok "Maven build complete" || fail "Maven build failed. Fix compilation errors and retry."

  echo ""
  log "Produced JARs:"
  find "$PROJECT_DIR" -path "*/target/*.jar" ! -name "*-sources.jar" | sort | while read -r jar; do
    echo "   $jar"
  done
}

# ── phase 2: docker compose up ────────────────────────────────────────────────
# Builds all Docker images (using each service's Dockerfile) then starts every
# container in dependency order.  --build ensures images are rebuilt whenever
# source or Dockerfile changes are present.
compose_up() {
  local follow="${1:-}"
  header "Docker Compose — build images + start containers"
  log "Compose file: $COMPOSE_FILE"
  echo ""

  $COMPOSE_CMD -f "$COMPOSE_FILE" config >/dev/null \
    || fail "docker-compose.yml is invalid. Run '$COMPOSE_CMD -f docker-compose.yml config' for details."

  if [[ "$follow" == "--follow" ]]; then
    log "Starting in foreground — press Ctrl+C to stop"
    $COMPOSE_CMD -f "$COMPOSE_FILE" up --build
  else
    log "Starting in detached mode"
    $COMPOSE_CMD -f "$COMPOSE_FILE" up --build -d
    echo ""
    ok "All containers started. Service endpoints:"
    print_endpoints
    echo ""
    log "Follow logs : ./run-local.sh logs [service]"
    log "Stop all    : ./run-local.sh down"
  fi
}

# ── docker compose down ───────────────────────────────────────────────────────
compose_down() {
  local volumes_flag="${1:-}"
  header "Docker Compose — stopping containers"
  if [[ "$volumes_flag" == "--volumes" ]]; then
    warn "Removing containers AND named volumes (postgres-data, redis-data)"
    $COMPOSE_CMD -f "$COMPOSE_FILE" down --volumes --remove-orphans
    ok "Containers and volumes removed"
  else
    $COMPOSE_CMD -f "$COMPOSE_FILE" down --remove-orphans
    ok "Containers stopped and removed (data volumes retained)"
    log "To also wipe DB data: ./run-local.sh down --volumes"
  fi
}

# ── docker compose logs ───────────────────────────────────────────────────────
compose_logs() {
  local service="${1:-}"
  if [[ -n "$service" ]]; then
    log "Following logs for: $service  (Ctrl+C to exit)"
    $COMPOSE_CMD -f "$COMPOSE_FILE" logs -f "$service"
  else
    log "Following logs for all services  (Ctrl+C to exit)"
    $COMPOSE_CMD -f "$COMPOSE_FILE" logs -f
  fi
}

# ── docker compose ps ─────────────────────────────────────────────────────────
compose_status() {
  header "Container status"
  $COMPOSE_CMD -f "$COMPOSE_FILE" ps
}

# ── service endpoint summary ──────────────────────────────────────────────────
print_endpoints() {
  echo -e "  ${BOLD}Infrastructure${NC}"
  echo    "    Config Server    http://localhost:8888"
  echo    "    Eureka Dashboard http://localhost:8761"
  echo    "    PostgreSQL       localhost:5432"
  echo    "    Redis            localhost:6379"
  echo ""
  echo -e "  ${BOLD}Application Services${NC}"
  echo    "    API Gateway      http://localhost:8000"
  echo    "    Auth Service     http://localhost:8080"
  echo    "    User Service     http://localhost:8081"
  echo    "    Order Service    http://localhost:8082"
  echo    "    Product Service  http://localhost:8083"
  echo    "    Financial Svc    http://localhost:8084"
  echo    "    Health Service   http://localhost:8085"
  echo    "    Social Service   http://localhost:8086"
  echo    "    User gRPC REST   http://localhost:8090"
  echo    "    User gRPC port   localhost:9090"
  echo ""
  echo -e "  ${BOLD}Frontend${NC}"
  echo    "    Enterprise UI    http://localhost:3000"
}

# ── main ──────────────────────────────────────────────────────────────────────
COMMAND="${1:-}"
OPTION="${2:-}"

case "$COMMAND" in
  build)
    check_prereqs
    build_maven
    ;;

  up)
    check_prereqs
    compose_up "$OPTION"
    ;;

  down)
    check_prereqs
    compose_down "$OPTION"
    ;;

  logs)
    compose_logs "$OPTION"
    ;;

  status)
    compose_status
    ;;

  restart)
    check_prereqs
    compose_down
    build_maven
    compose_up
    ;;

  "")
    # Default: full build → run cycle
    check_prereqs
    build_maven
    compose_up
    ;;

  --help|-h|help)
    sed -n '3,30p' "$0" | sed 's/^# \?//'
    ;;

  *)
    fail "Unknown command: '$COMMAND'. Run './run-local.sh --help' for usage."
    ;;
esac
