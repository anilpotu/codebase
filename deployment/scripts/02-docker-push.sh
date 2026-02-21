#!/usr/bin/env bash
# =============================================================================
# 02-docker-push.sh  —  Build Docker images and push to registry
# Usage: ./scripts/02-docker-push.sh <registry> [tag] [project]
#
# Arguments:
#   registry  Docker registry prefix, e.g.:
#               Docker Hub: "anilpotu"
#               ECR:        "123456789.dkr.ecr.us-east-1.amazonaws.com"
#   tag       Image tag (default: latest)
#   project   Which project to build: "grpc" | "sds" | "all" (default: all)
#
# Examples:
#   ./scripts/02-docker-push.sh anilpotu latest all
#   ./scripts/02-docker-push.sh 123456789.dkr.ecr.us-east-1.amazonaws.com build-42 grpc
# =============================================================================
set -euo pipefail

REGISTRY="${1:?Usage: $0 <registry> [tag] [project]}"
TAG="${2:-latest}"
PROJECT="${3:-all}"
REPO_ROOT="$(cd "$(dirname "$0")/../.." && pwd)"

RED='\033[0;31m'; GREEN='\033[0;32m'; CYAN='\033[0;36m'; YELLOW='\033[1;33m'; NC='\033[0m'

log()  { echo -e "${CYAN}[DOCKER]${NC} $*"; }
ok()   { echo -e "${GREEN}[OK]${NC}     $*"; }
fail() { echo -e "${RED}[FAIL]${NC}   $*"; exit 1; }

command -v docker >/dev/null 2>&1 || fail "docker not found"
docker info >/dev/null 2>&1 || fail "Docker daemon is not running"

# ── ECR login (if registry looks like ECR) ────────────────────────────────────
if [[ "$REGISTRY" == *".dkr.ecr."* ]]; then
  REGION=$(echo "$REGISTRY" | sed 's/.*\.ecr\.\([^.]*\)\..*/\1/')
  log "Logging in to ECR (region: $REGION)..."
  aws ecr get-login-password --region "$REGION" \
    | docker login --username AWS --password-stdin "$REGISTRY"
  ok "ECR login successful"
fi

# ── Helper ─────────────────────────────────────────────────────────────────────
build_and_push() {
  local name="$1" ctx="$2" dockerfile="${3:-}"
  local full="$REGISTRY/$name:$TAG"
  log "Building $full"
  if [[ -n "$dockerfile" ]]; then
    docker build --cache-from "$full" -t "$full" -f "$dockerfile" "$ctx" 2>&1
  else
    docker build --cache-from "$full" -t "$full" "$ctx" 2>&1
  fi
  docker push "$full"
  # Also tag as latest if different
  if [[ "$TAG" != "latest" ]]; then
    docker tag "$full" "$REGISTRY/$name:latest"
    docker push "$REGISTRY/$name:latest"
  fi
  ok "Pushed $full"
}

# ── grpc-enterprise-v3 (5 images) ─────────────────────────────────────────────
build_grpc() {
  echo ""
  echo -e "${YELLOW}═══ grpc-enterprise-v3 (5 images) ═══${NC}"
  GRPC_DIR="$REPO_ROOT/grpc-enterprise-v3"
  for svc in user-grpc-service financial-service health-service social-service; do
    build_and_push "$svc" "$GRPC_DIR" "$GRPC_DIR/$svc/Dockerfile"
  done
  build_and_push "enterprise-ui" "$GRPC_DIR/enterprise-ui"
}

# ── secure-distributed-system (7 images) ──────────────────────────────────────
build_sds() {
  echo ""
  echo -e "${YELLOW}═══ secure-distributed-system (7 images) ═══${NC}"
  SDS_DIR="$REPO_ROOT/secure-distributed-system"
  for svc in config-server eureka-server api-gateway auth-service user-service order-service product-service; do
    build_and_push "secure-distributed/$svc" "$SDS_DIR/$svc" "$SDS_DIR/$svc/Dockerfile"
  done
}

# ── Main ───────────────────────────────────────────────────────────────────────
case "$PROJECT" in
  grpc) build_grpc ;;
  sds)  build_sds  ;;
  all)  build_grpc; build_sds ;;
  *)    fail "Unknown project '$PROJECT'. Use: grpc | sds | all" ;;
esac

echo ""
ok "All images pushed to $REGISTRY with tag: $TAG"
