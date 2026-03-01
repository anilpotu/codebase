#!/usr/bin/env bash
# =============================================================================
# 06-verify.sh  —  Verify deployments via health checks
# Usage: ./scripts/06-verify.sh [project]
#
# Arguments:
#   project   "grpc" | "sds" | "us" | "all" (default: all)
# =============================================================================
set -euo pipefail

PROJECT="${1:-all}"

RED='\033[0;31m'; GREEN='\033[0;32m'; CYAN='\033[0;36m'; YELLOW='\033[1;33m'; NC='\033[0m'

log()  { echo -e "${CYAN}[VERIFY]${NC} $*"; }
ok()   { echo -e "${GREEN}[OK]${NC}     $*"; }
fail() { echo -e "${RED}[FAIL]${NC}   $*"; }
warn() { echo -e "${YELLOW}[WARN]${NC}   $*"; }

FAILED=0

# ── Health check helper ───────────────────────────────────────────────────────
check_health() {
  local ns="$1" svc="$2" port="$3"
  local status
  status=$(kubectl -n "$ns" exec "deploy/$svc" -- \
    wget -qO- --timeout=15 "http://localhost:$port/actuator/health" 2>/dev/null \
    || echo "UNREACHABLE")
  if echo "$status" | grep -q '"status":"UP"'; then
    ok "$ns/$svc :$port — HEALTHY"
  else
    fail "$ns/$svc :$port — $status"
    FAILED=1
  fi
}

# ── Wait for rollout ──────────────────────────────────────────────────────────
wait_rollout() {
  local ns="$1" svc="$2"
  log "Waiting for rollout: $ns/$svc"
  kubectl rollout status "deployment/$svc" -n "$ns" --timeout=300s \
    && ok "$svc rollout complete" \
    || { fail "$svc rollout timed out"; FAILED=1; }
}

# ── grpc-enterprise-v3 ────────────────────────────────────────────────────────
verify_grpc() {
  echo ""
  echo -e "${YELLOW}═══ Verifying grpc-enterprise-v3 ═══${NC}"
  NS="grpc-enterprise"

  log "--- Pod Status ---"
  kubectl -n "$NS" get pods -o wide

  log "--- Rollout Status ---"
  for svc in grpc-enterprise-v3 financial-service health-service social-service; do
    wait_rollout "$NS" "$svc"
  done

  log "--- Health Checks ---"
  check_health "$NS" "grpc-enterprise-v3" 8080
  check_health "$NS" "financial-service"  8081
  check_health "$NS" "health-service"     8082
  check_health "$NS" "social-service"     8083

  log "--- Service Endpoints ---"
  kubectl -n "$NS" get svc

  log "--- Istio Status ---"
  kubectl -n "$NS" get virtualservices,destinationrules,gateways 2>/dev/null || true
}

# ── secure-distributed-system ─────────────────────────────────────────────────
verify_sds() {
  echo ""
  echo -e "${YELLOW}═══ Verifying secure-distributed-system ═══${NC}"
  NS="secure-distributed"

  log "--- Pod Status ---"
  kubectl -n "$NS" get pods -o wide

  log "--- Rollout Status ---"
  for svc in config-server eureka-server api-gateway auth-service user-service order-service product-service; do
    wait_rollout "$NS" "$svc"
  done

  log "--- Health Checks ---"
  check_health "$NS" "config-server"  8888
  check_health "$NS" "eureka-server"  8761
  check_health "$NS" "api-gateway"    8000
  check_health "$NS" "auth-service"   8080
  check_health "$NS" "user-service"   8081
  check_health "$NS" "order-service"  8082
  check_health "$NS" "product-service" 8083

  log "--- Service Endpoints ---"
  kubectl -n "$NS" get svc

  log "--- Istio Status ---"
  kubectl -n "$NS" get virtualservices,destinationrules,gateways 2>/dev/null || true

  log "--- Eureka Dashboard (port-forward to check) ---"
  echo "  kubectl -n $NS port-forward svc/eureka-server 8761:8761"
  echo "  Then open: http://localhost:8761"
}

# ── userservice ───────────────────────────────────────────────────────────────
verify_us() {
  echo ""
  echo -e "${YELLOW}═══ Verifying userservice ═══${NC}"
  NS="userservice"

  log "--- Pod Status ---"
  kubectl -n "$NS" get pods -o wide

  log "--- Rollout Status ---"
  for svc in config-server eureka-server api-gateway \
             auth-service user-service order-service product-service \
             user-grpc-service financial-service health-service social-service \
             enterprise-ui; do
    wait_rollout "$NS" "$svc"
  done

  log "--- Health Checks ---"
  check_health "$NS" "config-server"      8888
  check_health "$NS" "eureka-server"      8761
  check_health "$NS" "api-gateway"        8000
  check_health "$NS" "auth-service"       8080
  check_health "$NS" "user-service"       8081
  check_health "$NS" "order-service"      8082
  check_health "$NS" "product-service"    8083
  check_health "$NS" "user-grpc-service"  8090
  check_health "$NS" "financial-service"  8084
  check_health "$NS" "health-service"     8085
  check_health "$NS" "social-service"     8086

  log "--- Service Endpoints ---"
  kubectl -n "$NS" get svc

  log "--- Istio Status ---"
  kubectl -n "$NS" get virtualservices,destinationrules,gateways 2>/dev/null || true

  log "--- Eureka Dashboard (port-forward to check) ---"
  echo "  kubectl -n $NS port-forward svc/eureka-server 8761:8761"
  echo "  Then open: http://localhost:8761"
}

case "$PROJECT" in
  grpc) verify_grpc ;;
  sds)  verify_sds  ;;
  us)   verify_us   ;;
  all)  verify_grpc; verify_sds; verify_us ;;
  *)    echo "Unknown project '$PROJECT'. Use: grpc | sds | us | all"; exit 1 ;;
esac

echo ""
if [[ $FAILED -eq 0 ]]; then
  ok "All health checks passed."
else
  fail "One or more health checks failed. Check the logs above."
  exit 1
fi
