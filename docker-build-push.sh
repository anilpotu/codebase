#!/usr/bin/env bash
set -euo pipefail

REGISTRY="anilpotu"
VERSION="latest"
BASE_DIR="$(cd "$(dirname "$0")" && pwd)"

RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; CYAN='\033[0;36m'; NC='\033[0m'

log()   { echo -e "${CYAN}[BUILD]${NC} $*"; }
ok()    { echo -e "${GREEN}[OK]${NC}    $*"; }
warn()  { echo -e "${YELLOW}[WARN]${NC}  $*"; }
fail()  { echo -e "${RED}[FAIL]${NC}  $*"; exit 1; }

# ── helpers ──────────────────────────────────────────────────────────────────

build_and_push() {
  local tag="$1"; local ctx="$2"; local dockerfile="${3:-}"
  local full="${REGISTRY}/${tag}:${VERSION}"
  log "Building  ${full}"
  if [[ -n "$dockerfile" ]]; then
    docker build -t "$full" -f "$dockerfile" "$ctx"
  else
    docker build -t "$full" "$ctx"
  fi
  ok "Built     ${full}"
  log "Pushing   ${full}"
  docker push "$full"
  ok "Pushed    ${full}"
}

# Inline Dockerfile for pre-built Java 11 JARs
build_java11_from_jar() {
  local tag="$1"; local jar_dir="$2"
  local full="${REGISTRY}/${tag}:${VERSION}"
  log "Building  ${full}  (pre-built JAR)"
  docker build -t "$full" -f - "$jar_dir" <<'DOCKERFILE'
FROM eclipse-temurin:11-jre-alpine
WORKDIR /app
COPY target/*.jar app.jar
ENTRYPOINT ["java","-jar","app.jar"]
DOCKERFILE
  ok "Built     ${full}"
  log "Pushing   ${full}"
  docker push "$full"
  ok "Pushed    ${full}"
}

# ── pre-flight ────────────────────────────────────────────────────────────────

docker info > /dev/null 2>&1 || fail "Docker daemon not running. Start it first."
docker info --format '{{.AuthConfigs}}' | grep -q "docker.io" || \
  warn "Not logged into Docker Hub — run: docker login -u ${REGISTRY}"

echo ""
echo "════════════════════════════════════════════════════"
echo "  Building & pushing 12 images → ${REGISTRY}/*"
echo "════════════════════════════════════════════════════"
echo ""

# ── grpc-enterprise-v3  ───────────────────────────────────────────────────────
echo -e "\n${YELLOW}── grpc-enterprise-v3 (5 images) ──────────────────${NC}"

GRPC_DIR="${BASE_DIR}/grpc-enterprise-v3"

for svc in financial-service health-service social-service user-grpc-service; do
  JAR="${GRPC_DIR}/${svc}/target/*.jar"
  ls ${JAR} > /dev/null 2>&1 || fail "JAR not found for ${svc}. Run: cd grpc-enterprise-v3 && mvn package -DskipTests"
  build_java11_from_jar "$svc" "${GRPC_DIR}/${svc}"
done

# enterprise-ui uses its own Dockerfile (Node.js multi-stage build)
build_and_push "enterprise-ui" "${GRPC_DIR}/enterprise-ui"

# ── secure-distributed-system  ───────────────────────────────────────────────
echo -e "\n${YELLOW}── secure-distributed-system (7 images) ───────────${NC}"

SDS_DIR="${BASE_DIR}/secure-distributed-system"

for svc in config-server eureka-server api-gateway auth-service user-service order-service product-service; do
  JAR="${SDS_DIR}/${svc}/target/*.jar"
  ls ${JAR} > /dev/null 2>&1 || fail "JAR not found for ${svc}. Run: cd secure-distributed-system && mvn package -DskipTests"
  build_and_push "$svc" "${SDS_DIR}/${svc}"
done

# ── summary ──────────────────────────────────────────────────────────────────
echo ""
echo "════════════════════════════════════════════════════"
echo -e "${GREEN}  All 12 images built and pushed successfully!${NC}"
echo "════════════════════════════════════════════════════"
echo ""
echo "Images on Docker Hub:"
for img in financial-service health-service social-service user-grpc-service enterprise-ui \
           config-server eureka-server api-gateway auth-service user-service order-service product-service; do
  echo "  docker.io/${REGISTRY}/${img}:${VERSION}"
done
