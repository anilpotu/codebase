#!/usr/bin/env bash
# =============================================================================
# 04-k8s-setup.sh  —  Configure kubectl contexts and pre-deployment K8s setup
# Usage: ./scripts/04-k8s-setup.sh <project>
#
# What it does:
#   1. Updates kubeconfig for the target EKS cluster(s)
#   2. Installs / verifies Istio (if not already installed)
#   3. Creates DB and JWT secrets for the target project
#
# Required environment variables:
#   AWS_DEFAULT_REGION   (default: us-east-1)
#   DB_PASSWORD          RDS master password
#   JWT_SECRET           JWT signing secret (grpc only)
#   REDIS_PASSWORD       Redis password (sds only, optional)
#
# Examples:
#   DB_PASSWORD=secret JWT_SECRET=myjwt ./scripts/04-k8s-setup.sh grpc
#   DB_PASSWORD=secret JWT_SECRET=myjwt ./scripts/04-k8s-setup.sh sds
#   DB_PASSWORD=secret JWT_SECRET=myjwt ./scripts/04-k8s-setup.sh all
# =============================================================================
set -euo pipefail

PROJECT="${1:?Usage: $0 <grpc|sds|us|all>}"
AWS_REGION="${AWS_DEFAULT_REGION:-us-east-1}"

RED='\033[0;31m'; GREEN='\033[0;32m'; CYAN='\033[0;36m'; YELLOW='\033[1;33m'; NC='\033[0m'

log()  { echo -e "${CYAN}[K8S-SETUP]${NC} $*"; }
ok()   { echo -e "${GREEN}[OK]${NC}        $*"; }
warn() { echo -e "${YELLOW}[WARN]${NC}      $*"; }
fail() { echo -e "${RED}[FAIL]${NC}      $*"; exit 1; }

command -v kubectl >/dev/null 2>&1 || fail "kubectl not found"
command -v aws     >/dev/null 2>&1 || fail "aws CLI not found"
aws sts get-caller-identity >/dev/null 2>&1 || fail "AWS credentials not configured"

: "${DB_PASSWORD:?DB_PASSWORD is required}"
: "${JWT_SECRET:?JWT_SECRET is required}"

# ── Check / install Istio ─────────────────────────────────────────────────────
ensure_istio() {
  if kubectl get namespace istio-system >/dev/null 2>&1; then
    ok "Istio already installed"
    return
  fi
  command -v istioctl >/dev/null 2>&1 || {
    warn "istioctl not found. Installing Istio 1.20..."
    curl -L https://istio.io/downloadIstio | ISTIO_VERSION=1.20.0 TARGET_ARCH=x86_64 sh -
    export PATH="$PWD/istio-1.20.0/bin:$PATH"
  }
  log "Installing Istio (demo profile)..."
  istioctl install --set profile=demo -y
  ok "Istio installed"
}

# ── grpc-enterprise-v3 setup ──────────────────────────────────────────────────
setup_grpc() {
  echo ""
  echo -e "${YELLOW}═══ grpc-enterprise-v3 Kubernetes Setup ═══${NC}"
  CLUSTER="grpc-enterprise-v3-eks"
  NAMESPACE="grpc-enterprise"

  log "Updating kubeconfig for cluster: $CLUSTER"
  aws eks update-kubeconfig --region "$AWS_REGION" --name "$CLUSTER"
  ok "kubeconfig updated"

  ensure_istio

  log "Creating namespace: $NAMESPACE"
  kubectl create namespace "$NAMESPACE" --dry-run=client -o yaml | kubectl apply -f -
  kubectl label namespace "$NAMESPACE" istio-injection=enabled --overwrite

  log "Creating database secrets..."
  for svc in grpc-enterprise financial-service health-service social-service; do
    kubectl create secret generic "${svc}-db-secret" \
      --namespace "$NAMESPACE" \
      --from-literal=username=grpcadmin \
      --from-literal=password="$DB_PASSWORD" \
      --dry-run=client -o yaml | kubectl apply -f -
    ok "Secret: ${svc}-db-secret"
  done

  log "Creating JWT secret..."
  kubectl create secret generic grpc-enterprise-jwt \
    --namespace "$NAMESPACE" \
    --from-literal=jwt-secret="$JWT_SECRET" \
    --dry-run=client -o yaml | kubectl apply -f -
  ok "Secret: grpc-enterprise-jwt"
}

# ── secure-distributed-system setup ──────────────────────────────────────────
setup_sds() {
  echo ""
  echo -e "${YELLOW}═══ secure-distributed-system Kubernetes Setup ═══${NC}"
  CLUSTER="secure-distributed-eks"
  NAMESPACE="secure-distributed"

  log "Updating kubeconfig for cluster: $CLUSTER"
  aws eks update-kubeconfig --region "$AWS_REGION" --name "$CLUSTER"
  ok "kubeconfig updated"

  ensure_istio

  log "Creating namespace: $NAMESPACE"
  kubectl create namespace "$NAMESPACE" --dry-run=client -o yaml | kubectl apply -f -
  kubectl label namespace "$NAMESPACE" istio-injection=enabled --overwrite

  log "Creating app-secrets (jwt, config, eureka credentials)..."
  kubectl create secret generic app-secrets \
    --namespace "$NAMESPACE" \
    --from-literal=jwt-secret="$JWT_SECRET" \
    --from-literal=config-user=config-user \
    --from-literal=config-password=config-pass \
    --from-literal=eureka-user=eureka \
    --from-literal=eureka-password=eureka \
    --dry-run=client -o yaml | kubectl apply -f -
  ok "Secret: app-secrets"
}

# ── userservice setup ─────────────────────────────────────────────────────────
setup_us() {
  echo ""
  echo -e "${YELLOW}═══ userservice Kubernetes Setup ═══${NC}"
  CLUSTER="userservice-eks"
  NAMESPACE="userservice"

  log "Updating kubeconfig for cluster: $CLUSTER"
  aws eks update-kubeconfig --region "$AWS_REGION" --name "$CLUSTER"
  ok "kubeconfig updated"

  ensure_istio

  log "Creating namespace: $NAMESPACE"
  kubectl create namespace "$NAMESPACE" --dry-run=client -o yaml | kubectl apply -f -
  kubectl label namespace "$NAMESPACE" istio-injection=enabled --overwrite

  log "Creating database secrets (per enterprise gRPC service)..."
  # user-grpc-service → grpcdb
  kubectl create secret generic userservice-db-secret \
    --namespace "$NAMESPACE" \
    --from-literal=username=postgres \
    --from-literal=password="$DB_PASSWORD" \
    --dry-run=client -o yaml | kubectl apply -f -
  ok "Secret: userservice-db-secret"

  # financial-service → financialdb
  kubectl create secret generic financial-service-db-secret \
    --namespace "$NAMESPACE" \
    --from-literal=username=postgres \
    --from-literal=password="$DB_PASSWORD" \
    --dry-run=client -o yaml | kubectl apply -f -
  ok "Secret: financial-service-db-secret"

  # health-service → healthdb
  kubectl create secret generic health-service-db-secret \
    --namespace "$NAMESPACE" \
    --from-literal=username=postgres \
    --from-literal=password="$DB_PASSWORD" \
    --dry-run=client -o yaml | kubectl apply -f -
  ok "Secret: health-service-db-secret"

  # social-service → socialdb
  kubectl create secret generic social-service-db-secret \
    --namespace "$NAMESPACE" \
    --from-literal=username=postgres \
    --from-literal=password="$DB_PASSWORD" \
    --dry-run=client -o yaml | kubectl apply -f -
  ok "Secret: social-service-db-secret"

  log "Creating app-secrets (jwt, config, eureka credentials)..."
  # SDS-origin services use this secret for Spring Cloud credentials
  kubectl create secret generic app-secrets \
    --namespace "$NAMESPACE" \
    --from-literal=jwt-secret="$JWT_SECRET" \
    --from-literal=config-user=config-user \
    --from-literal=config-password=config-pass \
    --from-literal=eureka-user=eureka \
    --from-literal=eureka-password=eureka \
    --dry-run=client -o yaml | kubectl apply -f -
  ok "Secret: app-secrets"

  log "Creating enterprise JWT secret (for gRPC origin services)..."
  kubectl create secret generic enterprise-jwt-secret \
    --namespace "$NAMESPACE" \
    --from-literal=jwt-secret="$JWT_SECRET" \
    --dry-run=client -o yaml | kubectl apply -f -
  ok "Secret: enterprise-jwt-secret"
}

case "$PROJECT" in
  grpc) setup_grpc ;;
  sds)  setup_sds  ;;
  us)   setup_us   ;;
  all)  setup_grpc; setup_sds; setup_us ;;
  *)    fail "Unknown project '$PROJECT'. Use: grpc | sds | us | all" ;;
esac

ok "Kubernetes pre-deployment setup complete. Proceed to 05-helm-deploy.sh"
