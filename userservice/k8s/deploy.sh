#!/usr/bin/env bash
# ============================================================
# deploy.sh — Deploy userservice to local minikube cluster
# ============================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
NAMESPACE="userservice"

# --- colour helpers -----------------------------------------
RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'
BLUE='\033[0;34m'; NC='\033[0m'
info()    { echo -e "${BLUE}[INFO]${NC}  $*"; }
success() { echo -e "${GREEN}[OK]${NC}    $*"; }
warn()    { echo -e "${YELLOW}[WARN]${NC}  $*"; }
error()   { echo -e "${RED}[ERROR]${NC} $*"; exit 1; }

# --- prerequisite checks ------------------------------------
check_prereqs() {
  info "Checking prerequisites..."
  command -v kubectl  &>/dev/null || error "kubectl not found"
  command -v minikube &>/dev/null || error "minikube not found"
  command -v docker   &>/dev/null || error "docker not found"
  success "All prerequisites found"
}

# --- minikube -----------------------------------------------
ensure_minikube() {
  info "Checking minikube status..."
  if ! minikube status &>/dev/null; then
    info "Starting minikube (Docker driver, 4 GB RAM, 4 CPUs)..."
    minikube start --driver=docker --memory=4096 --cpus=4 \
      || error "minikube failed to start"
  fi
  success "minikube is running"
  kubectl config use-context minikube
}

# --- load local images into minikube ------------------------
load_images() {
  info "Loading Docker images into minikube..."
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
  for img in "${images[@]}"; do
    if docker image inspect "$img" &>/dev/null; then
      info "  Loading $img..."
      minikube image load "$img"
    else
      warn "  Image not found locally: $img (will attempt pull)"
    fi
  done
  success "Images loaded into minikube"
}

# --- deploy -------------------------------------------------
deploy() {
  info "Applying Kubernetes manifests to namespace: $NAMESPACE"
  local manifests=(
    00-namespace.yaml
    01-secrets.yaml
    02-config-repo-configmap.yaml
    03-postgres.yaml
    04-redis.yaml
    05-config-server.yaml
    06-eureka-server.yaml
    07-auth-service.yaml
    08-user-service.yaml
    09-order-service.yaml
    10-product-service.yaml
    11-api-gateway.yaml
    12-user-grpc-service.yaml
    13-financial-service.yaml
    14-health-service.yaml
    15-social-service.yaml
    16-enterprise-ui.yaml
  )
  for f in "${manifests[@]}"; do
    info "  Applying $f..."
    kubectl apply -f "$SCRIPT_DIR/$f"
  done
  success "All manifests applied"
}

# --- wait for pods ------------------------------------------
wait_for_pods() {
  info "Waiting for pods to become ready (this may take 5-10 minutes for Java services)..."

  # Infrastructure first
  for dep in postgres redis config-server eureka-server; do
    info "  Waiting for $dep..."
    kubectl rollout status deployment/$dep -n $NAMESPACE --timeout=300s 2>/dev/null \
      || kubectl rollout status statefulset/$dep -n $NAMESPACE --timeout=300s 2>/dev/null \
      || warn "Timeout waiting for $dep (may still be starting)"
  done

  # Application services
  for dep in auth-service user-service order-service product-service \
             api-gateway user-grpc-service financial-service \
             health-service social-service enterprise-ui; do
    info "  Waiting for $dep..."
    kubectl rollout status deployment/$dep -n $NAMESPACE --timeout=300s \
      || warn "Timeout waiting for $dep"
  done
  success "Rollout complete"
}

# --- status -------------------------------------------------
print_status() {
  echo ""
  echo -e "${BLUE}========================================${NC}"
  echo -e "${BLUE}  Userservice Kubernetes Deployment     ${NC}"
  echo -e "${BLUE}========================================${NC}"
  kubectl get pods -n $NAMESPACE -o wide 2>/dev/null || true
  echo ""
  kubectl get services -n $NAMESPACE 2>/dev/null || true

  local MINIKUBE_IP
  MINIKUBE_IP=$(minikube ip 2>/dev/null || echo "127.0.0.1")
  echo ""
  echo -e "${GREEN}Access Points:${NC}"
  echo -e "  API Gateway:   http://${MINIKUBE_IP}:30000"
  echo -e "  Enterprise UI: http://${MINIKUBE_IP}:30080"
  echo -e "  Eureka (fwd):  kubectl port-forward svc/eureka-server 8761:8761 -n ${NAMESPACE}"
  echo ""
  echo -e "${YELLOW}Quick port-forwards:${NC}"
  echo "  kubectl port-forward svc/config-server  8888:8888 -n $NAMESPACE &"
  echo "  kubectl port-forward svc/eureka-server  8761:8761 -n $NAMESPACE &"
  echo "  kubectl port-forward svc/auth-service   8080:8080 -n $NAMESPACE &"
}

# --- teardown -----------------------------------------------
teardown() {
  warn "Tearing down userservice deployment..."
  kubectl delete namespace $NAMESPACE --ignore-not-found=true
  success "Namespace $NAMESPACE deleted"
}

# --- main ---------------------------------------------------
case "${1:-deploy}" in
  deploy)
    check_prereqs
    ensure_minikube
    load_images
    deploy
    wait_for_pods
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
