#!/usr/bin/env bash
# =============================================================================
# teardown.sh  —  Remove Helm releases and (optionally) destroy Terraform infra
# Usage: ./scripts/teardown.sh <project> [--destroy-infra]
#
# Arguments:
#   project          "grpc" | "sds" | "us" | "all"
#   --destroy-infra  Also run terraform destroy (DESTRUCTIVE, requires confirmation)
#
# Examples:
#   ./scripts/teardown.sh grpc
#   ./scripts/teardown.sh us
#   ./scripts/teardown.sh all --destroy-infra
# =============================================================================
set -euo pipefail

PROJECT="${1:?Usage: $0 <grpc|sds|us|all> [--destroy-infra]}"
DESTROY_INFRA="${2:-}"
REPO_ROOT="$(cd "$(dirname "$0")/../.." && pwd)"

RED='\033[0;31m'; GREEN='\033[0;32m'; CYAN='\033[0;36m'; YELLOW='\033[1;33m'; NC='\033[0m'

log()  { echo -e "${CYAN}[TEARDOWN]${NC} $*"; }
ok()   { echo -e "${GREEN}[OK]${NC}       $*"; }
warn() { echo -e "${YELLOW}[WARN]${NC}     $*"; }
fail() { echo -e "${RED}[FAIL]${NC}     $*"; exit 1; }

command -v helm    >/dev/null 2>&1 || fail "helm not found"
command -v kubectl >/dev/null 2>&1 || fail "kubectl not found"

uninstall_grpc() {
  echo ""
  warn "Uninstalling grpc-enterprise-v3 Helm release..."
  helm uninstall grpc-enterprise --namespace grpc-enterprise 2>/dev/null \
    && ok "Release 'grpc-enterprise' uninstalled" \
    || warn "Release 'grpc-enterprise' not found (already removed?)"

  log "Deleting namespace grpc-enterprise..."
  kubectl delete namespace grpc-enterprise --ignore-not-found
  ok "Namespace grpc-enterprise removed"
}

uninstall_sds() {
  echo ""
  warn "Uninstalling secure-distributed-system Helm release..."
  helm uninstall secure-distributed --namespace secure-distributed 2>/dev/null \
    && ok "Release 'secure-distributed' uninstalled" \
    || warn "Release 'secure-distributed' not found (already removed?)"

  log "Deleting namespace secure-distributed..."
  kubectl delete namespace secure-distributed --ignore-not-found
  ok "Namespace secure-distributed removed"
}

destroy_terraform() {
  local name="$1" dir="$2"
  echo ""
  warn "DESTRUCTIVE: About to destroy ALL $name infrastructure."
  warn "This action cannot be undone. This will remove EKS, RDS, VPC, ECR, etc."
  read -rp "Type 'yes-destroy-$name' to confirm: " confirm
  if [[ "$confirm" != "yes-destroy-$name" ]]; then
    warn "Confirmation failed. Skipping terraform destroy for $name."
    return
  fi
  (cd "$dir" && terraform init -input=false -reconfigure && terraform destroy -auto-approve)
  ok "Terraform destroy complete: $name"
}

uninstall_us() {
  echo ""
  warn "Uninstalling userservice Helm release..."
  helm uninstall userservice --namespace userservice 2>/dev/null \
    && ok "Release 'userservice' uninstalled" \
    || warn "Release 'userservice' not found (already removed?)"

  log "Deleting namespace userservice..."
  kubectl delete namespace userservice --ignore-not-found
  ok "Namespace userservice removed"
}

# ── Helm uninstall ────────────────────────────────────────────────────────────
case "$PROJECT" in
  grpc) uninstall_grpc ;;
  sds)  uninstall_sds  ;;
  us)   uninstall_us   ;;
  all)  uninstall_grpc; uninstall_sds; uninstall_us ;;
  *)    fail "Unknown project '$PROJECT'. Use: grpc | sds | us | all" ;;
esac

# ── Terraform destroy (optional) ──────────────────────────────────────────────
if [[ "$DESTROY_INFRA" == "--destroy-infra" ]]; then
  warn "Infrastructure destroy requested."
  command -v terraform >/dev/null 2>&1 || fail "terraform not found"
  case "$PROJECT" in
    grpc) destroy_terraform "grpc-enterprise-v3" "$REPO_ROOT/grpc-enterprise-v3/terraform" ;;
    sds)  destroy_terraform "secure-distributed-system" "$REPO_ROOT/secure-distributed-system/terraform" ;;
    us)   destroy_terraform "userservice" "$REPO_ROOT/userservice/terraform" ;;
    all)
      destroy_terraform "grpc-enterprise-v3" "$REPO_ROOT/grpc-enterprise-v3/terraform"
      destroy_terraform "secure-distributed-system" "$REPO_ROOT/secure-distributed-system/terraform"
      destroy_terraform "userservice" "$REPO_ROOT/userservice/terraform"
      ;;
  esac
fi

ok "Teardown complete."
