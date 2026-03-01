#!/usr/bin/env bash
# =============================================================================
# 05-helm-deploy.sh  —  Deploy / upgrade via Helm charts
# Usage: ./scripts/05-helm-deploy.sh <project> <env> [image-tag] [action]
#
# Arguments:
#   project     "grpc" | "sds" | "us" | "all"
#   env         "dev" | "staging" | "prod"
#   image-tag   Docker image tag (default: latest)
#   action      "install" | "upgrade" | "diff" (default: upgrade --install)
#
# Examples:
#   ./scripts/05-helm-deploy.sh grpc dev
#   ./scripts/05-helm-deploy.sh sds prod build-42 upgrade
#   ./scripts/05-helm-deploy.sh us prod build-42 upgrade
#   ./scripts/05-helm-deploy.sh all staging latest
# =============================================================================
set -euo pipefail

PROJECT="${1:?Usage: $0 <grpc|sds|us|all> <env> [tag] [action]}"
ENV="${2:?Usage: $0 <project> <env> [tag] [action]}"
TAG="${3:-latest}"
ACTION="${4:-upgrade --install}"
REPO_ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
SCRIPT_DIR="$(cd "$(dirname "$0")/.." && pwd)"

RED='\033[0;31m'; GREEN='\033[0;32m'; CYAN='\033[0;36m'; YELLOW='\033[1;33m'; NC='\033[0m'

log()  { echo -e "${CYAN}[HELM]${NC} $*"; }
ok()   { echo -e "${GREEN}[OK]${NC}   $*"; }
fail() { echo -e "${RED}[FAIL]${NC} $*"; exit 1; }

command -v helm    >/dev/null 2>&1 || fail "helm not found. Install Helm 3+"
command -v kubectl >/dev/null 2>&1 || fail "kubectl not found"

VALUES_DIR="$SCRIPT_DIR/environments/$ENV"
[[ -d "$VALUES_DIR" ]] || fail "Environment values directory not found: $VALUES_DIR"

# ── Deploy grpc-enterprise-v3 ─────────────────────────────────────────────────
deploy_grpc() {
  echo ""
  echo -e "${YELLOW}═══ Helm Deploy: grpc-enterprise-v3 ($ENV, tag=$TAG) ═══${NC}"
  CHART="$REPO_ROOT/grpc-enterprise-v3/helm"
  VALUES_FILE="$VALUES_DIR/grpc-enterprise-values.yaml"
  RELEASE="grpc-enterprise"
  NAMESPACE="grpc-enterprise"

  [[ -f "$VALUES_FILE" ]] || fail "Values file not found: $VALUES_FILE"

  log "Linting chart..."
  helm lint "$CHART" -f "$VALUES_FILE"

  log "Running $ACTION..."
  # shellcheck disable=SC2086
  helm $ACTION "$RELEASE" "$CHART" \
    --namespace "$NAMESPACE" \
    --create-namespace \
    --values "$VALUES_FILE" \
    --set "user-grpc-service.image.tag=$TAG" \
    --set "financial-service.image.tag=$TAG" \
    --set "health-service.image.tag=$TAG" \
    --set "social-service.image.tag=$TAG" \
    --timeout 10m \
    --wait \
    --atomic

  ok "grpc-enterprise-v3 deployed (release: $RELEASE, namespace: $NAMESPACE)"
  echo ""
  log "Release status:"
  helm status "$RELEASE" --namespace "$NAMESPACE"
}

# ── Deploy secure-distributed-system ──────────────────────────────────────────
deploy_sds() {
  echo ""
  echo -e "${YELLOW}═══ Helm Deploy: secure-distributed-system ($ENV, tag=$TAG) ═══${NC}"
  CHART="$REPO_ROOT/secure-distributed-system/helm"
  VALUES_FILE="$VALUES_DIR/secure-distributed-values.yaml"
  RELEASE="secure-distributed"
  NAMESPACE="secure-distributed"

  [[ -f "$VALUES_FILE" ]] || fail "Values file not found: $VALUES_FILE"

  log "Linting chart..."
  helm lint "$CHART" -f "$VALUES_FILE"

  log "Running $ACTION..."
  # shellcheck disable=SC2086
  helm $ACTION "$RELEASE" "$CHART" \
    --namespace "$NAMESPACE" \
    --create-namespace \
    --values "$VALUES_FILE" \
    --set "config-server.image.tag=$TAG" \
    --set "eureka-server.image.tag=$TAG" \
    --set "api-gateway.image.tag=$TAG" \
    --set "auth-service.image.tag=$TAG" \
    --set "user-service.image.tag=$TAG" \
    --set "order-service.image.tag=$TAG" \
    --set "product-service.image.tag=$TAG" \
    --timeout 15m \
    --wait \
    --atomic

  ok "secure-distributed-system deployed (release: $RELEASE, namespace: $NAMESPACE)"
  echo ""
  log "Release status:"
  helm status "$RELEASE" --namespace "$NAMESPACE"
}

# ── Deploy userservice ────────────────────────────────────────────────────────
deploy_us() {
  echo ""
  echo -e "${YELLOW}═══ Helm Deploy: userservice ($ENV, tag=$TAG) ═══${NC}"
  CHART="$REPO_ROOT/userservice/helm"
  VALUES_FILE="$VALUES_DIR/userservice-values.yaml"
  RELEASE="userservice"
  NAMESPACE="userservice"

  [[ -f "$VALUES_FILE" ]] || fail "Values file not found: $VALUES_FILE"
  [[ -d "$CHART" ]] || fail "Helm chart not found: $CHART (run 'helm create userservice/helm' to scaffold)"

  log "Linting chart..."
  helm lint "$CHART" -f "$VALUES_FILE"

  log "Running $ACTION..."
  # shellcheck disable=SC2086
  helm $ACTION "$RELEASE" "$CHART" \
    --namespace "$NAMESPACE" \
    --create-namespace \
    --values "$VALUES_FILE" \
    --set "config-server.image.tag=$TAG" \
    --set "eureka-server.image.tag=$TAG" \
    --set "api-gateway.image.tag=$TAG" \
    --set "auth-service.image.tag=$TAG" \
    --set "user-service.image.tag=$TAG" \
    --set "order-service.image.tag=$TAG" \
    --set "product-service.image.tag=$TAG" \
    --set "user-grpc-service.image.tag=$TAG" \
    --set "financial-service.image.tag=$TAG" \
    --set "health-service.image.tag=$TAG" \
    --set "social-service.image.tag=$TAG" \
    --set "enterprise-ui.image.tag=$TAG" \
    --timeout 20m \
    --wait \
    --atomic

  ok "userservice deployed (release: $RELEASE, namespace: $NAMESPACE)"
  echo ""
  log "Release status:"
  helm status "$RELEASE" --namespace "$NAMESPACE"
}

case "$PROJECT" in
  grpc) deploy_grpc ;;
  sds)  deploy_sds  ;;
  us)   deploy_us   ;;
  all)  deploy_grpc; deploy_sds; deploy_us ;;
  *)    fail "Unknown project '$PROJECT'. Use: grpc | sds | us | all" ;;
esac

ok "Helm deployment complete. Proceed to 06-verify.sh"
