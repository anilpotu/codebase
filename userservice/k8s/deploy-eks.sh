#!/usr/bin/env bash
# ============================================================
# deploy-eks.sh — Build, push images to ECR, deploy to EKS
#
# Usage:
#   ./deploy-eks.sh [deploy|build-push|apply|status|teardown]
#
# Prerequisites: aws CLI, kubectl, docker
# AWS account: 932888576933, region: us-east-1
# ============================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
USERSERVICE_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
EKS_MANIFESTS="$SCRIPT_DIR/eks"

AWS_REGION="us-east-1"
AWS_ACCOUNT="932888576933"
CLUSTER_NAME="userservice-eks"
ECR_PREFIX="${AWS_ACCOUNT}.dkr.ecr.${AWS_REGION}.amazonaws.com/userservice"
NAMESPACE="userservice"

RDS_HOST="userservice-eks-postgres.co7em4a6q158.us-east-1.rds.amazonaws.com"
REDIS_HOST="userservice-eks-redis.90ltcd.ng.0001.use1.cache.amazonaws.com"
RDS_SECRET_ID="userservice-eks/db-password"

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
  command -v kubectl &>/dev/null || error "kubectl not found"
  command -v aws     &>/dev/null || error "aws CLI not found"
  command -v docker  &>/dev/null || error "docker not found"
  aws sts get-caller-identity &>/dev/null || error "AWS credentials not configured"
  success "All prerequisites found"
}

# --- fetch RDS password from Secrets Manager ----------------
get_rds_password() {
  info "Fetching RDS password from Secrets Manager..."
  local secret
  secret=$(aws secretsmanager get-secret-value \
    --secret-id "$RDS_SECRET_ID" \
    --region "$AWS_REGION" \
    --query SecretString \
    --output text 2>/dev/null) || error "Cannot fetch secret: $RDS_SECRET_ID"
  # Parse JSON: {"username":"dbadmin","password":"...","host":"...","port":5432}
  RDS_PASSWORD=$(echo "$secret" | python3 -c "import sys,json; print(json.load(sys.stdin)['password'])")
  RDS_USERNAME="dbadmin"
  success "RDS credentials fetched"
}

# --- configure kubectl for EKS ------------------------------
configure_kubectl() {
  info "Configuring kubectl for EKS cluster: $CLUSTER_NAME..."
  aws eks update-kubeconfig --region "$AWS_REGION" --name "$CLUSTER_NAME"
  kubectl config use-context "arn:aws:eks:${AWS_REGION}:${AWS_ACCOUNT}:cluster/${CLUSTER_NAME}"
  success "kubectl configured for EKS"
}

# --- authenticate Docker to ECR -----------------------------
ecr_login() {
  info "Authenticating Docker to ECR..."
  aws ecr get-login-password --region "$AWS_REGION" \
    | docker login --username AWS --password-stdin \
      "${AWS_ACCOUNT}.dkr.ecr.${AWS_REGION}.amazonaws.com"
  success "ECR authentication complete"
}

# --- build images in local Docker daemon and push to ECR ----
build_push_images() {
  info "Building Docker images in local daemon..."
  cd "$USERSERVICE_DIR"

  SERVICES_SINGLE_STAGE=(config-server api-gateway user-service order-service product-service)
  SERVICES_MULTI_STAGE=(eureka-server auth-service user-grpc-service financial-service health-service social-service)

  # Single-stage (pre-built JARs, context = service subdir)
  for svc in "${SERVICES_SINGLE_STAGE[@]}"; do
    info "  Building $svc..."
    docker build -t "userservice-${svc}:latest" -f "${svc}/Dockerfile" "${svc}/" \
      2>&1 | tail -3 || warn "  Build failed: $svc"
    docker tag "userservice-${svc}:latest" "${ECR_PREFIX}/${svc}:latest"
    info "  Pushing $svc to ECR..."
    docker push "${ECR_PREFIX}/${svc}:latest" 2>&1 | tail -2
    success "  $svc pushed"
  done

  # Multi-stage (Maven inside Docker, context = parent userservice/)
  for svc in "${SERVICES_MULTI_STAGE[@]}"; do
    info "  Building $svc (multi-stage)..."
    docker build -t "userservice-${svc}:latest" -f "${svc}/Dockerfile" . \
      2>&1 | tail -3 || warn "  Build failed: $svc"
    docker tag "userservice-${svc}:latest" "${ECR_PREFIX}/${svc}:latest"
    info "  Pushing $svc to ECR..."
    docker push "${ECR_PREFIX}/${svc}:latest" 2>&1 | tail -2
    success "  $svc pushed"
  done

  # Enterprise UI
  info "  Building enterprise-ui..."
  docker build -t "userservice-enterprise-ui:latest" \
    -f "enterprise-ui/Dockerfile" enterprise-ui/ \
    2>&1 | tail -3 || warn "  Build failed: enterprise-ui"
  docker tag "userservice-enterprise-ui:latest" "${ECR_PREFIX}/enterprise-ui:latest"
  info "  Pushing enterprise-ui to ECR..."
  docker push "${ECR_PREFIX}/enterprise-ui:latest" 2>&1 | tail -2
  success "  enterprise-ui pushed"

  success "All 12 images built and pushed to ECR"
}

# --- apply manifests ----------------------------------------
apply_manifests() {
  info "Applying Kubernetes manifests to EKS namespace: $NAMESPACE"

  # Apply namespace first
  kubectl apply -f "$EKS_MANIFESTS/00-namespace.yaml"

  # Create/update secrets with live RDS credentials
  info "  Creating userservice-secrets with RDS credentials..."
  kubectl create secret generic userservice-secrets \
    --namespace="$NAMESPACE" \
    --from-literal=jwt-secret="mySecretKeyForJWTTokenGenerationAndValidation12345678901234567890" \
    --from-literal=enterprise-jwt-secret="enterpriseGrpcSecretKeyForJWT256bitMinimumLength!" \
    --from-literal=config-username="config-user" \
    --from-literal=config-password="config-pass" \
    --from-literal=eureka-username="eureka" \
    --from-literal=eureka-password="eureka" \
    --from-literal=db-username="$RDS_USERNAME" \
    --from-literal=db-password="$RDS_PASSWORD" \
    --from-literal=rds-host="$RDS_HOST" \
    --dry-run=client -o yaml | kubectl apply -f -
  success "  Secrets applied"

  # Apply config-repo and init job
  for f in 02-config-repo-configmap.yaml 03-rds-init-job.yaml; do
    info "  Applying $f..."
    kubectl apply -f "$EKS_MANIFESTS/$f"
  done

  # Wait for RDS init job
  info "  Waiting for RDS database init job..."
  kubectl wait job/rds-init \
    --for=condition=complete \
    --namespace="$NAMESPACE" \
    --timeout=120s 2>/dev/null \
    || warn "  RDS init job timed out or already complete"

  # Apply all service manifests in order
  for f in \
    04-config-server.yaml 05-eureka-server.yaml \
    06-auth-service.yaml 07-user-service.yaml \
    08-order-service.yaml 09-product-service.yaml \
    10-api-gateway.yaml 11-user-grpc-service.yaml \
    12-financial-service.yaml 13-health-service.yaml \
    14-social-service.yaml 15-enterprise-ui.yaml; do
    info "  Applying $f..."
    kubectl apply -f "$EKS_MANIFESTS/$f"
  done

  success "All manifests applied to EKS"
}

# --- wait for pods ------------------------------------------
wait_for_pods() {
  info "Waiting for core infrastructure (config-server, eureka)..."
  kubectl rollout status deployment/config-server -n $NAMESPACE --timeout=300s || warn "config-server timeout"
  kubectl rollout status deployment/eureka-server -n $NAMESPACE --timeout=300s || warn "eureka-server timeout"
  success "Core infrastructure ready"

  info "Waiting for application services..."
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
  echo ""
  echo -e "${BLUE}================================================${NC}"
  echo -e "${BLUE}  Userservice EKS Deployment — Status          ${NC}"
  echo -e "${BLUE}================================================${NC}"
  kubectl get pods -n $NAMESPACE 2>/dev/null || true
  echo ""
  kubectl get services -n $NAMESPACE 2>/dev/null || true
  echo ""

  local GW_LB UI_LB
  GW_LB=$(kubectl get svc api-gateway -n $NAMESPACE \
    -o jsonpath='{.status.loadBalancer.ingress[0].hostname}' 2>/dev/null || echo "pending")
  UI_LB=$(kubectl get svc enterprise-ui -n $NAMESPACE \
    -o jsonpath='{.status.loadBalancer.ingress[0].hostname}' 2>/dev/null || echo "pending")

  echo -e "${GREEN}Access Points (LoadBalancer — may take 2-3 min to provision):${NC}"
  echo -e "  API Gateway:   ${GREEN}http://${GW_LB}:8000${NC}"
  echo -e "  Enterprise UI: ${GREEN}http://${UI_LB}${NC}"
  echo ""
  echo -e "${YELLOW}Port-forwards (alternative):${NC}"
  echo "  kubectl port-forward svc/eureka-server 8761:8761 -n $NAMESPACE &"
  echo "  kubectl port-forward svc/api-gateway   8000:8000 -n $NAMESPACE &"
  echo ""
  echo -e "${YELLOW}Health check:${NC}"
  echo "  kubectl exec -n $NAMESPACE deploy/api-gateway -- wget -qO- http://localhost:8000/actuator/health"
}

# --- teardown -----------------------------------------------
teardown() {
  warn "This will delete namespace '$NAMESPACE' and all userservice resources from EKS."
  read -r -p "Continue? [y/N] " confirm
  [[ "$confirm" =~ ^[Yy]$ ]] || { info "Cancelled."; exit 0; }
  kubectl delete namespace $NAMESPACE --ignore-not-found=true
  success "Namespace $NAMESPACE deleted from EKS"
}

# --- main ---------------------------------------------------
case "${1:-deploy}" in
  deploy)
    check_prereqs
    get_rds_password
    configure_kubectl
    ecr_login
    build_push_images
    apply_manifests
    wait_for_pods
    print_status
    ;;
  build-push)
    check_prereqs
    ecr_login
    build_push_images
    ;;
  apply)
    check_prereqs
    get_rds_password
    configure_kubectl
    apply_manifests
    wait_for_pods
    print_status
    ;;
  status)
    configure_kubectl
    print_status
    ;;
  teardown|delete)
    configure_kubectl
    teardown
    ;;
  *)
    echo "Usage: $0 [deploy|build-push|apply|status|teardown]"
    echo "  deploy      Full deploy: build images, push to ECR, apply to EKS"
    echo "  build-push  Build and push Docker images to ECR only"
    echo "  apply       Apply manifests to EKS only (skip image build)"
    echo "  status      Show pod/service status and access points"
    echo "  teardown    Delete the userservice namespace from EKS"
    exit 1
    ;;
esac
