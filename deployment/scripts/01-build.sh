#!/usr/bin/env bash
# =============================================================================
# 01-build.sh  —  Build all Maven projects
# Usage: ./scripts/01-build.sh [skip-tests] [project]
#
# Arguments:
#   skip-tests   Pass "skip-tests" to skip Maven test execution
#   project      Which project(s) to build: "grpc" | "sds" | "us" | "all" (default: all)
#
# Examples:
#   ./scripts/01-build.sh
#   ./scripts/01-build.sh skip-tests
#   ./scripts/01-build.sh skip-tests us
#   ./scripts/01-build.sh skip-tests grpc
# =============================================================================
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
SKIP_TESTS="${1:-}"
PROJECT="${2:-all}"

RED='\033[0;31m'; GREEN='\033[0;32m'; CYAN='\033[0;36m'; YELLOW='\033[1;33m'; NC='\033[0m'

log()  { echo -e "${CYAN}[BUILD]${NC} $*"; }
ok()   { echo -e "${GREEN}[OK]${NC}    $*"; }
fail() { echo -e "${RED}[FAIL]${NC}  $*"; exit 1; }

MAVEN_OPTS_VAL="-Xmx1024m"
if [[ "$SKIP_TESTS" == "skip-tests" ]]; then
  MVN_FLAGS="--batch-mode --no-transfer-progress -DskipTests"
else
  MVN_FLAGS="--batch-mode --no-transfer-progress"
fi

# ── Check prerequisites ───────────────────────────────────────────────────────
command -v mvn  >/dev/null 2>&1 || fail "mvn not found. Install Maven 3.8+"
command -v java >/dev/null 2>&1 || fail "java not found. Install Java 8 or 11"

JAVA_VER=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d. -f1)
log "Java version: $(java -version 2>&1 | head -1)"
log "Maven version: $(mvn -version 2>&1 | head -1)"

# ── Build grpc-enterprise-v3 (Java 11) ───────────────────────────────────────
build_grpc() {
  echo ""
  echo -e "${YELLOW}═══ grpc-enterprise-v3 (Java 11 / Spring Boot 2.7) ═══${NC}"
  GRPC_DIR="$REPO_ROOT/grpc-enterprise-v3"

  export MAVEN_OPTS="$MAVEN_OPTS_VAL"
  (cd "$GRPC_DIR" && mvn $MVN_FLAGS clean package) \
    && ok "grpc-enterprise-v3 built successfully" \
    || fail "grpc-enterprise-v3 build failed"

  echo "Produced JARs:"
  find "$GRPC_DIR" -path "*/target/*.jar" ! -name "*-sources.jar" | while read -r jar; do
    echo "  $jar"
  done
}

# ── Build secure-distributed-system (Java 8) ─────────────────────────────────
build_sds() {
  echo ""
  echo -e "${YELLOW}═══ secure-distributed-system (Java 8 / Spring Boot 2.7 + Spring Cloud) ═══${NC}"
  SDS_DIR="$REPO_ROOT/secure-distributed-system"

  export MAVEN_OPTS="$MAVEN_OPTS_VAL"
  (cd "$SDS_DIR" && mvn $MVN_FLAGS clean package) \
    && ok "secure-distributed-system built successfully" \
    || fail "secure-distributed-system build failed"

  echo "Produced JARs:"
  find "$SDS_DIR" -path "*/target/*.jar" ! -name "*-sources.jar" | while read -r jar; do
    echo "  $jar"
  done
}

# ── Build userservice (Java 11 / Spring Boot 2.7 + Spring Cloud + gRPC) ──────
# NOTE: The gRPC services (user-grpc-service, financial-service, health-service,
# social-service) use multi-stage Docker builds — they do NOT need a pre-built
# JAR. Only the traditional SDS-origin services need the Maven build here.
build_us() {
  echo ""
  echo -e "${YELLOW}═══ userservice (Java 11 / Spring Boot 2.7 + Spring Cloud + gRPC) ═══${NC}"
  US_DIR="$REPO_ROOT/userservice"

  export MAVEN_OPTS="$MAVEN_OPTS_VAL"
  # Build the full multi-module project. gRPC services are also built here so
  # the JAR cache is warm and docker-compose local runs work without Docker.
  (cd "$US_DIR" && mvn $MVN_FLAGS clean package) \
    && ok "userservice built successfully" \
    || fail "userservice build failed"

  echo "Produced JARs:"
  find "$US_DIR" -path "*/target/*.jar" ! -name "*-sources.jar" | while read -r jar; do
    echo "  $jar"
  done
}

# ── Main ──────────────────────────────────────────────────────────────────────
case "$PROJECT" in
  grpc) build_grpc ;;
  sds)  build_sds  ;;
  us)   build_us   ;;
  all)  build_grpc; build_sds; build_us ;;
  *)    fail "Unknown project '$PROJECT'. Use: grpc | sds | us | all" ;;
esac

echo ""
ok "All projects built. Proceed to 02-docker-push.sh"
