#!/usr/bin/env bash
# =============================================================================
# 07-k8s-local-deploy.sh  —  Deploy userservice to local minikube
#
# Usage: ./scripts/07-k8s-local-deploy.sh [action]
#   action:  deploy (default) | status | teardown | load-images
#
# What it does:
#   1. Starts minikube if not running (Docker driver, 4 GB RAM)
#   2. Loads all userservice Docker images into minikube
#   3. Applies all Kubernetes manifests from userservice/k8s/
#   4. Waits for rollouts and prints access information
#
# Prerequisites:
#   - minikube v1.30+   (local Kubernetes cluster)
#   - kubectl           (Kubernetes CLI)
#   - docker            (Docker daemon running)
#   - All userservice Docker images built:
#       cd userservice && docker compose build
#
# Examples:
#   ./scripts/07-k8s-local-deploy.sh           # deploy everything
#   ./scripts/07-k8s-local-deploy.sh status    # show pod/service status
#   ./scripts/07-k8s-local-deploy.sh teardown  # remove all resources
# =============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
K8S_DIR="$REPO_ROOT/userservice/k8s"
NAMESPACE="userservice"
ACTION="${1:-deploy}"

RED='\033[0;31m'; GREEN='\033[0;32m'; CYAN='\033[0;36m'
YELLOW='\033[1;33m'; BLUE='\033[0;34m'; NC='\033[0m'

log()     { echo -e "${CYAN}[K8S-LOCAL]${NC}  $*"; }
ok()      { echo -e "${GREEN}[OK]${NC}         $*"; }
warn()    { echo -e "${YELLOW}[WARN]${NC}       $*"; }
fail()    { echo -e "${RED}[FAIL]${NC}       $*"; exit 1; }
section() { echo -e "\n${BLUE}══════════════════════════════════════════${NC}"; \
            echo -e "${BLUE}  $*${NC}"; \
            echo -e "${BLUE}══════════════════════════════════════════${NC}"; }

# ── Prerequisite checks ────────────────────────────────────────────────────
check_prereqs() {
  section "Checking Prerequisites"
  command -v kubectl  >/dev/null 2>&1 || fail "kubectl not found. Install: https://kubernetes.io/docs/tasks/tools/"
  command -v minikube >/dev/null 2>&1 || fail "minikube not found. Install: https://minikube.sigs.k8s.io/docs/start/"
  command -v docker   >/dev/null 2>&1 || fail "Docker not found"
  docker info 2>&1 | grep -q "Server Version" || fail "Docker daemon is not running. Start Docker Desktop."
  [[ -d "$K8S_DIR" ]] || fail "K8s manifests not found at $K8S_DIR"
  ok "All prerequisites satisfied"
}

# ── Minikube setup ─────────────────────────────────────────────────────────
ensure_minikube() {
  section "Minikube Cluster"
  if ! minikube status 2>/dev/null | grep -q "Running"; then
    log "Starting minikube (Docker driver, 4 GB RAM, 4 CPUs)..."
    minikube start --driver=docker --memory=4096 --cpus=4
    ok "minikube started"
  else
    ok "minikube is already running"
  fi
  log "Switching kubectl context to minikube..."
  kubectl config use-context minikube
  ok "kubectl context: minikube"
}

# ── Load images into minikube ──────────────────────────────────────────────
load_images() {
  section "Loading Docker Images into Minikube"
  local images=(
    "userservice-config-server:latest"
    "userservice-eureka-server:latest"
    "userservice-auth-service:latest"
    "userservice-user-service:latest"
    "userservice-order-service:latest"
    "userservice-product-service:latest"
    "userservice-api-gateway:latest"
    "userservice-user-grpc-service:latest"
    "userservice-financial-service:latest"
    "userservice-health-service:latest"
    "userservice-social-service:latest"
    "userservice-enterprise-ui:latest"
  )
  local loaded=0 skipped=0
  for img in "${images[@]}"; do
    if docker image inspect "$img" >/dev/null 2>&1; then
      log "  Loading: $img"
      minikube image load "$img"
      (( loaded++ )) || true
    else
      warn "  Not found locally (will pull from registry): $img"
      (( skipped++ )) || true
    fi
  done
  ok "Images loaded: $loaded | Not found (will pull): $skipped"
}

# ── Apply Kubernetes manifests ─────────────────────────────────────────────
deploy_manifests() {
  section "Applying Kubernetes Manifests"
  local manifests=(
    "00-namespace.yaml"
    "01-secrets.yaml"
    "02-config-repo-configmap.yaml"
    "03-postgres.yaml"
    "04-redis.yaml"
    "05-config-server.yaml"
    "06-eureka-server.yaml"
    "07-auth-service.yaml"
    "08-user-service.yaml"
    "09-order-service.yaml"
    "10-product-service.yaml"
    "11-api-gateway.yaml"
    "12-user-grpc-service.yaml"
    "13-financial-service.yaml"
    "14-health-service.yaml"
    "15-social-service.yaml"
    "16-enterprise-ui.yaml"
  )
  for f in "${manifests[@]}"; do
    log "  $f"
    kubectl apply -f "$K8S_DIR/$f"
  done
  ok "All manifests applied to namespace: $NAMESPACE"
}

# ── Wait for rollouts ──────────────────────────────────────────────────────
wait_rollouts() {
  section "Waiting for Rollouts (up to 10 min per service)"
  log "Infrastructure services..."
  kubectl rollout status statefulset/postgres    -n $NAMESPACE --timeout=300s || warn "postgres not ready yet"
  kubectl rollout status deployment/redis        -n $NAMESPACE --timeout=120s || warn "redis not ready yet"
  kubectl rollout status deployment/config-server -n $NAMESPACE --timeout=300s || warn "config-server not ready yet"
  kubectl rollout status deployment/eureka-server -n $NAMESPACE --timeout=300s || warn "eureka-server not ready yet"

  log "Application services (Java boot can take 2-3 min each)..."
  for svc in auth-service user-service order-service product-service \
             api-gateway user-grpc-service financial-service \
             health-service social-service enterprise-ui; do
    log "  $svc..."
    kubectl rollout status deployment/$svc -n $NAMESPACE --timeout=600s \
      || warn "$svc rollout timed out (may still be starting)"
  done
  ok "Rollout monitoring complete"
}

# ── Print status ───────────────────────────────────────────────────────────
print_status() {
  section "Deployment Status"
  kubectl get pods     -n $NAMESPACE -o wide 2>/dev/null || true
  echo ""
  kubectl get services -n $NAMESPACE          2>/dev/null || true

  local MINIKUBE_IP
  MINIKUBE_IP=$(minikube ip 2>/dev/null || echo "N/A")

  echo ""
  echo -e "${GREEN}╔════════════════════════════════════════════╗${NC}"
  echo -e "${GREEN}║       Access Points (minikube)             ║${NC}"
  echo -e "${GREEN}╠════════════════════════════════════════════╣${NC}"
  echo -e "${GREEN}║${NC}  API Gateway:    http://${MINIKUBE_IP}:30000     ${GREEN}║${NC}"
  echo -e "${GREEN}║${NC}  Enterprise UI:  http://${MINIKUBE_IP}:30080     ${GREEN}║${NC}"
  echo -e "${GREEN}╚════════════════════════════════════════════╝${NC}"
  echo ""
  echo -e "${YELLOW}Port-forward commands for internal services:${NC}"
  echo "  kubectl port-forward svc/config-server  8888:8888 -n $NAMESPACE &"
  echo "  kubectl port-forward svc/eureka-server  8761:8761 -n $NAMESPACE &"
  echo "  kubectl port-forward svc/auth-service   8080:8080 -n $NAMESPACE &"
  echo "  kubectl port-forward svc/user-service   8081:8081 -n $NAMESPACE &"
  echo ""
  echo -e "${YELLOW}Useful kubectl commands:${NC}"
  echo "  kubectl get pods -n $NAMESPACE -w              # watch pods"
  echo "  kubectl logs -f deploy/auth-service -n $NAMESPACE  # tail logs"
  echo "  kubectl describe pod <pod-name> -n $NAMESPACE  # debug a pod"
}

# ── Teardown ──────────────────────────────────────────────────────────────
teardown() {
  section "Teardown: userservice namespace"
  warn "This will delete ALL resources in namespace '$NAMESPACE'."
  read -rp "Are you sure? (yes/no): " confirm
  [[ "$confirm" == "yes" ]] || { log "Aborted."; exit 0; }
  kubectl delete namespace $NAMESPACE --ignore-not-found=true
  ok "Namespace '$NAMESPACE' deleted"
}

# ── Entry point ────────────────────────────────────────────────────────────
case "$ACTION" in
  deploy)
    check_prereqs
    ensure_minikube
    load_images
    deploy_manifests
    wait_rollouts
    print_status
    ;;
  status)
    print_status
    ;;
  teardown|delete)
    teardown
    ;;
  load-images)
    ensure_minikube
    load_images
    ;;
  *)
    echo "Usage: $0 [deploy|status|teardown|load-images]"
    exit 1
    ;;
esac
