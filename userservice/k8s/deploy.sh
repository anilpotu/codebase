#!/usr/bin/env bash
# ============================================================
# deploy.sh — Deploy userservice to local minikube cluster
#
# Strategy: Build Docker images directly into minikube's
# container runtime (containerd) using minikube's Docker daemon.
# This avoids 'minikube image load' which can crash Docker Desktop
# when transferring many large images.
#
# Usage:
#   ./deploy.sh [deploy|status|teardown|build-images]
# ============================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
USERSERVICE_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
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
  if ! minikube status 2>/dev/null | grep -q "Running"; then
    info "Starting minikube (Docker driver, 5.2 GB RAM, 4 CPUs)..."
    minikube start \
      --driver=docker \
      --memory=5200 \
      --cpus=4 \
      --disk-size=25g \
      --kubernetes-version=v1.28.3 \
      --extra-config=kubelet.cgroup-driver=systemd \
      || error "minikube failed to start"
  fi
  success "minikube is running"
  kubectl config use-context minikube
}

# --- build images into minikube's docker daemon -------------
build_images() {
  info "Building Docker images directly into minikube's Docker daemon..."
  info "(This avoids the costly 'minikube image load' transfer step)"

  # Get minikube docker endpoint
  local docker_port
  docker_port=$(minikube docker-env --shell bash 2>/dev/null \
    | grep DOCKER_HOST \
    | sed 's/.*tcp:\/\/127.0.0.1://;s/".*//')

  export DOCKER_TLS_VERIFY="1"
  export DOCKER_HOST="tcp://127.0.0.1:${docker_port}"
  export DOCKER_CERT_PATH="${HOME}/.minikube/certs"

  # Verify connection to minikube docker
  docker info 2>/dev/null | grep -q "minikube" \
    || error "Cannot connect to minikube Docker daemon (port ${docker_port})"

  cd "$USERSERVICE_DIR"

  # Single-stage services (need pre-built JARs in target/)
  for svc in config-server api-gateway user-service order-service product-service; do
    info "  Building $svc..."
    docker build -t "userservice-${svc}:latest" -f "${svc}/Dockerfile" "${svc}/" \
      2>&1 | tail -2 || warn "  Failed: $svc"
  done

  # Multi-stage services (Maven inside Docker — need parent context)
  for svc in eureka-server auth-service user-grpc-service financial-service health-service social-service; do
    info "  Building $svc (multi-stage)..."
    docker build -t "userservice-${svc}:latest" -f "${svc}/Dockerfile" . \
      2>&1 | tail -2 || warn "  Failed: $svc"
  done

  # Enterprise UI
  info "  Building enterprise-ui..."
  docker build -t "userservice-enterprise-ui:latest" \
    -f "enterprise-ui/Dockerfile" enterprise-ui/ \
    2>&1 | tail -2 || warn "  Failed: enterprise-ui"

  unset DOCKER_TLS_VERIFY DOCKER_HOST DOCKER_CERT_PATH
  success "All 12 images built into minikube's Docker daemon"
}

# --- apply manifests ----------------------------------------
deploy() {
  info "Applying Kubernetes manifests to namespace: $NAMESPACE"
  for f in \
    00-namespace.yaml 01-secrets.yaml 02-config-repo-configmap.yaml \
    03-postgres.yaml  04-redis.yaml   05-config-server.yaml \
    06-eureka-server.yaml 07-auth-service.yaml 08-user-service.yaml \
    09-order-service.yaml 10-product-service.yaml 11-api-gateway.yaml \
    12-user-grpc-service.yaml 13-financial-service.yaml \
    14-health-service.yaml 15-social-service.yaml 16-enterprise-ui.yaml; do
    info "  Applying $f..."
    kubectl apply -f "$SCRIPT_DIR/$f"
  done
  success "All 17 manifests applied"
}

# --- wait for pods ------------------------------------------
wait_for_pods() {
  info "Waiting for core infrastructure (postgres, redis, config-server, eureka)..."
  kubectl rollout status statefulset/postgres   -n $NAMESPACE --timeout=180s || warn "postgres timeout"
  kubectl rollout status deployment/redis       -n $NAMESPACE --timeout=90s  || warn "redis timeout"
  kubectl rollout status deployment/config-server -n $NAMESPACE --timeout=300s || warn "config-server timeout"
  kubectl rollout status deployment/eureka-server -n $NAMESPACE --timeout=300s || warn "eureka-server timeout"
  success "Core infrastructure ready"

  info "Waiting for application services (5-8 minutes for JVM warmup)..."
  for dep in auth-service user-service order-service product-service \
             api-gateway user-grpc-service financial-service \
             health-service social-service enterprise-ui; do
    printf "  %-26s" "$dep"
    kubectl rollout status deployment/$dep -n $NAMESPACE --timeout=420s 2>&1 | tail -1 \
      || warn "$dep timed out"
  done
  success "All services deployed"
}

# --- status -------------------------------------------------
print_status() {
  local MINIKUBE_IP
  MINIKUBE_IP=$(minikube ip 2>/dev/null || echo "127.0.0.1")

  echo ""
  echo -e "${BLUE}================================================${NC}"
  echo -e "${BLUE}  Userservice Kubernetes Deployment — Status    ${NC}"
  echo -e "${BLUE}================================================${NC}"
  kubectl get pods -n $NAMESPACE 2>/dev/null || true
  echo ""
  kubectl get services -n $NAMESPACE 2>/dev/null || true
  echo ""
  echo -e "${GREEN}Access Points:${NC}"
  echo -e "  API Gateway:   ${GREEN}http://${MINIKUBE_IP}:30000${NC}"
  echo -e "  Enterprise UI: ${GREEN}http://${MINIKUBE_IP}:30080${NC}"
  echo ""
  echo -e "${YELLOW}Port-forwards:${NC}"
  echo "  kubectl port-forward svc/eureka-server 8761:8761 -n $NAMESPACE &"
  echo "  kubectl port-forward svc/auth-service  8080:8080 -n $NAMESPACE &"
  echo ""
  echo -e "${YELLOW}Health checks:${NC}"
  echo "  kubectl exec -n $NAMESPACE deploy/api-gateway -- wget -qO- http://localhost:8000/actuator/health"
}

# --- teardown -----------------------------------------------
teardown() {
  warn "This will delete namespace '$NAMESPACE' and all userservice resources."
  read -r -p "Continue? [y/N] " confirm
  [[ "$confirm" =~ ^[Yy]$ ]] || { info "Cancelled."; exit 0; }
  kubectl delete namespace $NAMESPACE --ignore-not-found=true
  success "Namespace $NAMESPACE deleted"
}

# --- main ---------------------------------------------------
case "${1:-deploy}" in
  deploy)
    check_prereqs
    ensure_minikube
    build_images
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
  build-images)
    ensure_minikube
    build_images
    ;;
  *)
    echo "Usage: $0 [deploy|status|teardown|build-images]"
    echo "  deploy       Full deploy: start minikube, build images, apply manifests"
    echo "  status       Show pod/service status and access points"
    echo "  teardown     Delete the userservice namespace"
    echo "  build-images Rebuild all Docker images into minikube's daemon"
    exit 1
    ;;
esac
