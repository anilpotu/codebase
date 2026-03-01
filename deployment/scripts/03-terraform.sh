#!/usr/bin/env bash
# =============================================================================
# 03-terraform.sh  - Provision AWS infrastructure with Terraform
# Usage: ./scripts/03-terraform.sh <project> <action> [env]
#
# Arguments:
#   project   "grpc" | "sds" | "us" | "all"
#   action    "init" | "plan" | "apply" | "destroy"
#   env       Environment name for tagging and tfvars lookup (default: production)
#
# Examples:
#   ./scripts/03-terraform.sh grpc init
#   ./scripts/03-terraform.sh sds plan production
#   ./scripts/03-terraform.sh us apply dev
#   ./scripts/03-terraform.sh all apply production
# =============================================================================
set -euo pipefail

PROJECT="${1:?Usage: $0 <grpc|sds|us|all> <init|plan|apply|destroy> [env]}"
ACTION="${2:?Usage: $0 <project> <init|plan|apply|destroy> [env]}"
ENV="${3:-production}"
REPO_ROOT="$(cd "$(dirname "$0")/../.." && pwd)"

RED='\033[0;31m'; GREEN='\033[0;32m'; CYAN='\033[0;36m'; YELLOW='\033[1;33m'; NC='\033[0m'

log()  { echo -e "${CYAN}[TERRAFORM]${NC} $*"; }
ok()   { echo -e "${GREEN}[OK]${NC}        $*"; }
warn() { echo -e "${YELLOW}[WARN]${NC}      $*"; }
fail() { echo -e "${RED}[FAIL]${NC}      $*"; exit 1; }

command -v terraform >/dev/null 2>&1 || fail "terraform not found. Install Terraform >= 1.5"
command -v aws       >/dev/null 2>&1 || fail "aws CLI not found"
aws sts get-caller-identity >/dev/null 2>&1 || fail "AWS credentials not configured. Run: aws configure"

run_terraform() {
  local name="$1" dir="$2" key="$3"
  local tfvars_file="$REPO_ROOT/deployment/environments/$ENV/${key}-terraform.tfvars"
  local -a tf_args=("-var=environment=$ENV")
  echo ""
  echo -e "${YELLOW}=== Terraform: $name ($ACTION) ===${NC}"

  if [[ ! -d "$dir" ]]; then
    fail "Terraform directory not found: $dir"
  fi

  cd "$dir"

  if [[ -f "$tfvars_file" ]]; then
    log "Using tfvars: $tfvars_file"
    tf_args+=("-var-file=$tfvars_file")
  else
    warn "No tfvars file found for '$key' in environment '$ENV'. Using defaults."
  fi

  case "$ACTION" in
    init)
      log "Initializing Terraform backend..."
      terraform init -input=false
      terraform validate
      ok "Initialized: $name"
      ;;
    plan)
      terraform init -input=false -reconfigure
      log "Planning..."
      terraform plan \
        -input=false \
        "${tf_args[@]}" \
        -out=tfplan
      ok "Plan saved to $dir/tfplan. Review and run 'apply' to proceed."
      ;;
    apply)
      if [[ ! -f "tfplan" ]]; then
        warn "No saved plan found. Running plan first..."
        terraform init -input=false -reconfigure
        terraform plan -input=false "${tf_args[@]}" -out=tfplan
      fi
      log "Applying..."
      terraform apply -input=false tfplan
      echo ""
      log "Outputs:"
      terraform output
      ok "Applied: $name"
      ;;
    destroy)
      warn "DESTRUCTIVE: This will destroy ALL $name infrastructure in '$ENV'."
      read -rp "Type the environment name to confirm: " confirm
      if [[ "$confirm" != "$ENV" ]]; then
        fail "Confirmation failed. Aborting."
      fi
      terraform init -input=false -reconfigure
      terraform destroy -auto-approve "${tf_args[@]}"
      ok "Destroyed: $name"
      ;;
    *)
      fail "Unknown action '$ACTION'. Use: init | plan | apply | destroy"
      ;;
  esac
}

case "$PROJECT" in
  grpc)
    run_terraform "grpc-enterprise-v3" "$REPO_ROOT/grpc-enterprise-v3/terraform" "grpc"
    ;;
  sds)
    run_terraform "secure-distributed-system" "$REPO_ROOT/secure-distributed-system/terraform" "sds"
    ;;
  us)
    run_terraform "userservice" "$REPO_ROOT/userservice/terraform" "userservice"
    ;;
  all)
    run_terraform "grpc-enterprise-v3" "$REPO_ROOT/grpc-enterprise-v3/terraform" "grpc"
    run_terraform "secure-distributed-system" "$REPO_ROOT/secure-distributed-system/terraform" "sds"
    run_terraform "userservice" "$REPO_ROOT/userservice/terraform" "userservice"
    ;;
  *)
    fail "Unknown project '$PROJECT'. Use: grpc | sds | us | all"
    ;;
esac

ok "Terraform $ACTION complete."
