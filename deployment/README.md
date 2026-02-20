# Deployment Guide

Central deployment project for all microservice platforms in this monorepo. Covers every step from local build to production deployment on AWS EKS, using Helm, raw kubectl, Terraform, GitLab CI, and Jenkins.

---

## Table of Contents

1. [Repository Layout](#1-repository-layout)
2. [Architecture Overview](#2-architecture-overview)
3. [Prerequisites](#3-prerequisites)
4. [AWS Credentials Setup](#4-aws-credentials-setup)
5. [Step 1 — Build Applications](#5-step-1--build-applications)
6. [Step 2 — Build & Push Docker Images](#6-step-2--build--push-docker-images)
7. [Step 3 — Provision AWS Infrastructure (Terraform)](#7-step-3--provision-aws-infrastructure-terraform)
8. [Step 4 — Kubernetes Pre-Deployment Setup](#8-step-4--kubernetes-pre-deployment-setup)
9. [Step 5 — Deploy with Helm (Recommended)](#9-step-5--deploy-with-helm-recommended)
10. [Step 6 — Deploy with Raw kubectl (Alternative)](#10-step-6--deploy-with-raw-kubectl-alternative)
11. [Step 7 — Verify Deployments](#11-step-7--verify-deployments)
12. [CI/CD Pipelines](#12-cicd-pipelines)
13. [Helm Operations Reference](#13-helm-operations-reference)
14. [Secrets Management](#14-secrets-management)
15. [Environment Configuration](#15-environment-configuration)
16. [Observability](#16-observability)
17. [Rollback Procedures](#17-rollback-procedures)
18. [Teardown](#18-teardown)
19. [Troubleshooting](#19-troubleshooting)

---

## 1. Repository Layout

```
codebase/
├── deployment/                         ← THIS project (deployment hub)
│   ├── README.md                       ← This file
│   ├── scripts/
│   │   ├── 01-build.sh                 Build all Maven projects
│   │   ├── 02-docker-push.sh           Build & push Docker images to registry
│   │   ├── 03-terraform.sh             Provision / destroy AWS infrastructure
│   │   ├── 04-k8s-setup.sh             Configure kubectl, Istio, K8s secrets
│   │   ├── 05-helm-deploy.sh           Deploy via Helm (recommended)
│   │   ├── 06-verify.sh                Health checks and rollout verification
│   │   └── teardown.sh                 Remove Helm releases (+ optional infra destroy)
│   └── environments/
│       ├── dev/                        Dev override values for both charts
│       ├── staging/                    Staging override values
│       └── prod/                       Production override values
│
├── grpc-enterprise-v3/                 gRPC + REST microservices platform
│   ├── helm/                           Helm chart (umbrella + 4 sub-charts)
│   ├── k8s/                            Raw Kubernetes manifests
│   ├── terraform/                      AWS infrastructure (EKS, RDS, ECR, VPC)
│   ├── user-grpc-service/              gRPC user service (port 9090 / 8080)
│   ├── financial-service/              REST financial service (port 8081)
│   ├── health-service/                 REST health service (port 8082)
│   ├── social-service/                 REST social service (port 8083)
│   ├── enterprise-ui/                  React 18 frontend (port 80)
│   ├── Jenkinsfile
│   └── .gitlab-ci.yml
│
├── secure-distributed-system/          Spring Cloud microservices platform
│   ├── helm/                           Helm chart (umbrella + 8 sub-charts)
│   ├── k8s/                            Raw Kubernetes manifests
│   ├── terraform/                      AWS infrastructure (EKS, RDS, ECR, ElastiCache)
│   ├── config-server/                  Spring Cloud Config (port 8888)
│   ├── eureka-server/                  Netflix Eureka (port 8761)
│   ├── api-gateway/                    Spring Cloud Gateway (port 8000)
│   ├── auth-service/                   JWT auth (port 8080)
│   ├── user-service/                   User CRUD (port 8081)
│   ├── order-service/                  Order management (port 8082)
│   ├── product-service/                Product catalog + Redis cache (port 8083)
│   ├── Jenkinsfile
│   └── .gitlab-ci.yml
│
├── docker-build-push.sh                Root-level Docker build helper
└── .gitlab-ci.yml                      Monorepo trigger pipeline
```

---

## 2. Architecture Overview

### Project 1 — grpc-enterprise-v3

```
Internet
    │
    ▼
Istio Ingress Gateway  (ports 80/443)
    │         │
    │   HTTPS ▼
    │   enterprise-gateway  (api.enterprise.example.com, grpc.enterprise.example.com)
    │         │
    ├─────────┼──────────────────────────────────┐
    ▼         ▼                    ▼              ▼
user-grpc-service  financial-service  health-service  social-service
  gRPC :9090          REST :8081        REST :8082      REST :8083
  REST :8080               │                │               │
      │                    └────────────────┴───────────────┘
      │                                     │
      └─────────────────┬───────────────────┘
                        ▼
              PostgreSQL (RDS)
              (grpcdb / financialdb / healthdb / socialdb)
```

**Tech stack:** Java 11, Spring Boot 2.7.18, gRPC (port 9090), React 18 frontend, Istio mTLS, Flyway, Resilience4j, Prometheus

**EKS cluster:** `grpc-enterprise-v3-eks` | **Namespace:** `grpc-enterprise`

**Terraform backend:** S3 bucket `grpc-enterprise-v3-tfstate`, DynamoDB table `grpc-enterprise-v3-tflock`

---

### Project 2 — secure-distributed-system

```
Internet
    │
    ▼
Istio Ingress Gateway  (ports 80/443)
    │
    ▼
api-gateway  :8000  (Spring Cloud Gateway, Redis rate-limiting)
    │
    ├── /api/auth     ──► auth-service   :8080  (JWT issuance/validation)
    ├── /api/users    ──► user-service   :8081  (User CRUD)
    ├── /api/orders   ──► order-service  :8082  (Order management)
    └── /api/products ──► product-service :8083 (Product catalog + Redis cache)

All services register with:
    eureka-server  :8761  (Netflix Eureka — service discovery)

All services fetch config from:
    config-server  :8888  (Spring Cloud Config — reads config-repo/)

Shared infrastructure:
    Redis  :6379  (rate limiting for api-gateway, caching for product-service)
    PostgreSQL (RDS)  (per-service databases via Flyway migrations)
```

**Tech stack:** Java 8, Spring Boot 2.7.18, Spring Cloud 2021.0.8, Eureka, Spring Cloud Config, JWT, Redis, Istio mTLS, Resilience4j, Prometheus

**EKS cluster:** `secure-distributed-eks` | **Namespace:** `secure-distributed`

**Terraform backend:** S3 bucket `secure-distributed-tfstate`, DynamoDB table `terraform-locks`

---

## 3. Prerequisites

Install all tools before proceeding:

| Tool | Version | Install |
|---|---|---|
| Java | 8 (sds) / 11 (grpc) | `sdk install java` or OS package manager |
| Maven | 3.8+ | `sdk install maven` |
| Docker | 24+ | https://docs.docker.com/get-docker/ |
| AWS CLI | 2.x | `pip install awscli` or https://aws.amazon.com/cli/ |
| Terraform | 1.5+ | https://developer.hashicorp.com/terraform/install |
| kubectl | 1.28+ | `curl -LO "https://dl.k8s.io/release/$(curl -sL https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"` |
| Helm | 3.12+ | `curl https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3 \| bash` |
| istioctl | 1.20+ | `curl -L https://istio.io/downloadIstio \| sh -` |

Verify all tools are available:

```bash
java -version
mvn -version
docker version
aws --version
terraform version
kubectl version --client
helm version
istioctl version
```

---

## 4. AWS Credentials Setup

All deployment scripts require AWS credentials with permissions for ECR, EKS, RDS, VPC, IAM, and DynamoDB.

### Option A — AWS CLI profile

```bash
aws configure --profile deployment
export AWS_PROFILE=deployment
export AWS_DEFAULT_REGION=us-east-1
```

### Option B — Environment variables

```bash
export AWS_ACCESS_KEY_ID=AKIA...
export AWS_SECRET_ACCESS_KEY=...
export AWS_DEFAULT_REGION=us-east-1
```

### Verify

```bash
aws sts get-caller-identity
```

---

## 5. Step 1 — Build Applications

Build all Maven projects. JARs are required before Docker images can be built.

```bash
# From the repo root
cd /path/to/codebase

# Build all projects (runs tests)
./deployment/scripts/01-build.sh

# Build without tests (faster)
./deployment/scripts/01-build.sh skip-tests
```

### Build individually

```bash
# grpc-enterprise-v3 (requires Java 11)
cd grpc-enterprise-v3
mvn clean package --batch-mode

# secure-distributed-system (requires Java 8)
cd secure-distributed-system
mvn clean package --batch-mode -DskipTests
```

**Expected output:** JAR files in `<service>/target/<service>-*.jar` for each module.

---

## 6. Step 2 — Build & Push Docker Images

### Using the deployment script

```bash
# Push all 12 images to Docker Hub (anilpotu/*)
./deployment/scripts/02-docker-push.sh anilpotu latest all

# Push to ECR with a specific tag
./deployment/scripts/02-docker-push.sh \
  "123456789.dkr.ecr.us-east-1.amazonaws.com" \
  "build-42" \
  all

# Build only grpc-enterprise-v3 images
./deployment/scripts/02-docker-push.sh anilpotu latest grpc

# Build only secure-distributed-system images
./deployment/scripts/02-docker-push.sh anilpotu latest sds
```

### Docker image inventory

| Image | Dockerfile | Project |
|---|---|---|
| `user-grpc-service` | grpc-enterprise-v3/user-grpc-service/Dockerfile | grpc |
| `financial-service` | grpc-enterprise-v3/financial-service/Dockerfile | grpc |
| `health-service` | grpc-enterprise-v3/health-service/Dockerfile | grpc |
| `social-service` | grpc-enterprise-v3/social-service/Dockerfile | grpc |
| `enterprise-ui` | grpc-enterprise-v3/enterprise-ui/Dockerfile | grpc |
| `secure-distributed/config-server` | secure-distributed-system/config-server/Dockerfile | sds |
| `secure-distributed/eureka-server` | secure-distributed-system/eureka-server/Dockerfile | sds |
| `secure-distributed/api-gateway` | secure-distributed-system/api-gateway/Dockerfile | sds |
| `secure-distributed/auth-service` | secure-distributed-system/auth-service/Dockerfile | sds |
| `secure-distributed/user-service` | secure-distributed-system/user-service/Dockerfile | sds |
| `secure-distributed/order-service` | secure-distributed-system/order-service/Dockerfile | sds |
| `secure-distributed/product-service` | secure-distributed-system/product-service/Dockerfile | sds |

### ECR login (if using ECR)

```bash
ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
REGION=us-east-1
aws ecr get-login-password --region $REGION \
  | docker login --username AWS --password-stdin \
    "$ACCOUNT_ID.dkr.ecr.$REGION.amazonaws.com"
```

---

## 7. Step 3 — Provision AWS Infrastructure (Terraform)

Each project has its own Terraform configuration under `<project>/terraform/`. Both target AWS EKS and use an S3 backend for state.

### Terraform state backends

| Project | S3 Bucket | DynamoDB Table |
|---|---|---|
| grpc-enterprise-v3 | `grpc-enterprise-v3-tfstate` | `grpc-enterprise-v3-tflock` |
| secure-distributed-system | `secure-distributed-tfstate` | `terraform-locks` |

> **Before first run:** Create the S3 buckets and DynamoDB tables manually, or with a bootstrapping script. Both buckets must have versioning and server-side encryption enabled.

### Provision grpc-enterprise-v3

```bash
# Initialize (do once per machine/CI runner)
./deployment/scripts/03-terraform.sh grpc init

# Preview changes
./deployment/scripts/03-terraform.sh grpc plan production

# Apply (creates EKS, RDS, ECR, VPC, security groups)
TF_VAR_db_password="$DB_PASSWORD" ./deployment/scripts/03-terraform.sh grpc apply production
```

### Provision secure-distributed-system

```bash
./deployment/scripts/03-terraform.sh sds init
./deployment/scripts/03-terraform.sh sds plan production
TF_VAR_db_password="$DB_PASSWORD" ./deployment/scripts/03-terraform.sh sds apply production
```

### Provision both

```bash
./deployment/scripts/03-terraform.sh all apply production
```

### Key Terraform variables

#### grpc-enterprise-v3/terraform/variables.tf

| Variable | Default | Description |
|---|---|---|
| `aws_region` | `us-east-1` | AWS region |
| `environment` | `production` | Environment tag |
| `eks_cluster_version` | `1.28` | Kubernetes version |
| `eks_node_instance_types` | `["t3.medium"]` | Worker node types |
| `eks_node_desired_size` | `3` | Desired node count |
| `eks_node_min_size` | `2` | Min node count |
| `eks_node_max_size` | `5` | Max node count |
| `db_instance_class` | `db.t3.medium` | RDS instance class |
| `db_password` | *(required)* | RDS master password |
| `ecr_image_retention_count` | `20` | ECR retention count |

#### secure-distributed-system/terraform/variables.tf

| Variable | Default | Description |
|---|---|---|
| `aws_region` | `us-east-1` | AWS region |
| `environment` | `dev` | Environment name |
| `cluster_name` | `secure-distributed` | EKS cluster name |
| `node_instance_type` | `t3.medium` | Worker node type |
| `node_desired_size` | `3` | Desired node count |
| `db_instance_class` | `db.t3.micro` | RDS instance class |
| `redis_node_type` | `cache.t3.micro` | ElastiCache node type |

---

## 8. Step 4 — Kubernetes Pre-Deployment Setup

Configures kubectl, installs Istio, creates namespaces and Kubernetes secrets.

```bash
# Required environment variables
export DB_PASSWORD="your-rds-password"
export JWT_SECRET="your-jwt-secret-at-least-32-chars"

# Setup grpc-enterprise-v3
./deployment/scripts/04-k8s-setup.sh grpc

# Setup secure-distributed-system
./deployment/scripts/04-k8s-setup.sh sds

# Setup both
./deployment/scripts/04-k8s-setup.sh all
```

### What this creates

**grpc-enterprise-v3 (`grpc-enterprise` namespace):**
- Namespace with `istio-injection: enabled`
- Secrets: `grpc-enterprise-db-secret`, `financial-service-db-secret`, `health-service-db-secret`, `social-service-db-secret`, `grpc-enterprise-jwt`

**secure-distributed-system (`secure-distributed` namespace):**
- Namespace with `istio-injection: enabled`
- Secret: `app-secrets` (jwt-secret, config-user, config-password, eureka-user, eureka-password)

### Install Istio manually (if needed)

```bash
# Download and install Istio 1.20
curl -L https://istio.io/downloadIstio | ISTIO_VERSION=1.20.0 sh -
export PATH="$PWD/istio-1.20.0/bin:$PATH"

# Install with demo profile (includes ingress gateway)
istioctl install --set profile=demo -y

# Verify Istio is running
kubectl get pods -n istio-system
```

---

## 9. Step 5 — Deploy with Helm (Recommended)

Helm charts are located at:
- `grpc-enterprise-v3/helm/` — umbrella chart with 4 service sub-charts
- `secure-distributed-system/helm/` — umbrella chart with 8 service sub-charts

### Quick deploy

```bash
# Deploy grpc-enterprise-v3 to dev
./deployment/scripts/05-helm-deploy.sh grpc dev

# Deploy secure-distributed-system to staging with a specific image tag
./deployment/scripts/05-helm-deploy.sh sds staging build-42

# Deploy both to production
./deployment/scripts/05-helm-deploy.sh all prod build-42
```

### Manual Helm install/upgrade

```bash
# grpc-enterprise-v3
helm upgrade --install grpc-enterprise grpc-enterprise-v3/helm/ \
  --namespace grpc-enterprise \
  --create-namespace \
  --values deployment/environments/prod/grpc-enterprise-values.yaml \
  --set "user-grpc-service.image.tag=build-42" \
  --set "financial-service.image.tag=build-42" \
  --set "health-service.image.tag=build-42" \
  --set "social-service.image.tag=build-42" \
  --timeout 10m \
  --wait \
  --atomic

# secure-distributed-system
helm upgrade --install secure-distributed secure-distributed-system/helm/ \
  --namespace secure-distributed \
  --create-namespace \
  --values deployment/environments/prod/secure-distributed-values.yaml \
  --set "config-server.image.tag=build-42" \
  --set "eureka-server.image.tag=build-42" \
  --set "api-gateway.image.tag=build-42" \
  --set "auth-service.image.tag=build-42" \
  --set "user-service.image.tag=build-42" \
  --set "order-service.image.tag=build-42" \
  --set "product-service.image.tag=build-42" \
  --timeout 15m \
  --wait \
  --atomic
```

### Helm chart structure

**grpc-enterprise-v3/helm/**

```
Chart.yaml         — Umbrella chart (lists sub-charts as dependencies)
values.yaml        — Default values for all sub-charts
templates/
  namespace.yaml   — grpc-enterprise namespace (istio-injection: enabled)
  istio/
    gateway.yaml                — Istio Gateway (ports 80/443, TLS)
    virtualservices.yaml        — 4 VirtualServices (grpc, financial, health, social)
    destination-rules.yaml      — 4 DestinationRules (mTLS, circuit breaking)
    authorization-policies.yaml — 8 AuthorizationPolicies (deny-all + per-service)
    peer-authentication.yaml    — mTLS STRICT enforcement
    request-authentication.yaml — JWT validation
    rate-limiting.yaml          — 4 EnvoyFilter rate limiters (token bucket)
    sidecar.yaml                — Sidecar egress scope
    telemetry.yaml              — Jaeger tracing + Prometheus metrics
charts/
  user-grpc-service/   — Deployment, Service, PDB
  financial-service/   — Deployment, Service, PDB
  health-service/      — Deployment, Service, PDB
  social-service/      — Deployment, Service, PDB
```

**secure-distributed-system/helm/**

```
Chart.yaml         — Umbrella chart
values.yaml        — Default values
templates/
  namespace.yaml   — secure-distributed namespace
  secrets.yaml     — app-secrets Secret (toggle with secrets.create)
  istio/
    gateway.yaml                — Istio Gateway
    virtualservice.yaml         — VirtualService (all routes → api-gateway)
    destination-rules.yaml      — 5 DestinationRules (mTLS, circuit breaking)
    authorization-policies.yaml — 10 AuthorizationPolicies
    peer-authentication.yaml    — mTLS STRICT
    rate-limiting.yaml          — EnvoyFilter (api-gateway: 200 req/min)
    telemetry.yaml              — Prometheus metrics
charts/
  config-server/    — Deployment, Service
  eureka-server/    — Deployment (with init container), Service
  redis/            — Deployment, Service
  api-gateway/      — Deployment (with init container), Service, PDB
  auth-service/     — Deployment (with init container), Service, PDB
  user-service/     — Deployment (with init container), Service, PDB
  order-service/    — Deployment (with init container), Service, PDB
  product-service/  — Deployment (with init container), Service, PDB
```

### Key Helm values

#### grpc-enterprise-v3 — top-level values

| Key | Default | Description |
|---|---|---|
| `global.imageRegistry` | `""` | Registry prefix (empty = Docker Hub) |
| `global.namespace` | `grpc-enterprise` | Target namespace |
| `global.istio.enabled` | `true` | Toggle all Istio resources |
| `istio.gateway.apiHost` | `api.enterprise.example.com` | API hostname |
| `istio.gateway.grpcHost` | `grpc.enterprise.example.com` | gRPC hostname |
| `istio.gateway.tlsCredentialName` | `enterprise-tls-credential` | TLS secret name |
| `istio.jwtIssuer` | `grpc-enterprise-v3` | JWT issuer claim |
| `istio.rateLimiting.financial` | `100` | Req/min for financial-service |
| `istio.rateLimiting.health` | `200` | Req/min for health-service |
| `istio.rateLimiting.social` | `300` | Req/min for social-service |
| `istio.rateLimiting.grpc` | `500` | Req/min for user-grpc-service |
| `user-grpc-service.replicaCount` | `3` | Replicas |
| `user-grpc-service.pdb.minAvailable` | `2` | PDB min available |

#### secure-distributed-system — top-level values

| Key | Default | Description |
|---|---|---|
| `global.imageRegistry` | `secure-distributed` | Docker Hub org |
| `global.namespace` | `secure-distributed` | Target namespace |
| `global.istio.enabled` | `true` | Toggle Istio resources |
| `secrets.create` | `true` | Create app-secrets Secret |
| `secrets.jwtSecret` | *(base64)* | JWT secret (base64-encoded) |
| `secrets.configUser` | *(base64)* | Config server username |
| `secrets.configPassword` | *(base64)* | Config server password |
| `secrets.eurekaUser` | *(base64)* | Eureka username |
| `secrets.eurekaPassword` | *(base64)* | Eureka password |
| `istio.rateLimiting.apiGateway.maxTokens` | `200` | API gateway rate limit |

---

## 10. Step 6 — Deploy with Raw kubectl (Alternative)

Use the existing `k8s/` manifests directly. This approach does not use Helm.

### grpc-enterprise-v3

```bash
NAMESPACE="grpc-enterprise"
REGISTRY="123456789.dkr.ecr.us-east-1.amazonaws.com"
TAG="build-42"

# 1. Apply Istio namespace config
kubectl apply -f grpc-enterprise-v3/k8s/istio/namespace.yaml

# 2. Create DB secrets
for SVC in grpc-enterprise financial-service health-service social-service; do
  kubectl create secret generic "${SVC}-db-secret" \
    --namespace "$NAMESPACE" \
    --from-literal=username=grpcadmin \
    --from-literal=password="$DB_PASSWORD" \
    --dry-run=client -o yaml | kubectl apply -f -
done

# 3. Apply workload manifests
kubectl apply -f grpc-enterprise-v3/k8s/ --namespace "$NAMESPACE"

# 4. Update images to the built tag
for SVC in user-grpc-service financial-service health-service social-service; do
  kubectl set image "deployment/$SVC" \
    "app=$REGISTRY/$SVC:$TAG" \
    --namespace "$NAMESPACE"
done

# 5. Apply Istio resources
kubectl apply -f grpc-enterprise-v3/k8s/istio/ --namespace "$NAMESPACE"

# 6. Wait for rollouts
for SVC in grpc-enterprise-v3 financial-service health-service social-service; do
  kubectl rollout status "deployment/$SVC" \
    --namespace "$NAMESPACE" --timeout=300s
done
```

### secure-distributed-system

The SDS services have a strict startup order. Deploy in sequence:

```bash
NAMESPACE="secure-distributed"
REGISTRY="123456789.dkr.ecr.us-east-1.amazonaws.com/secure-distributed"
TAG="build-42"

# 1. Namespace + secrets
kubectl apply -f secure-distributed-system/k8s/namespace.yaml
kubectl apply -f secure-distributed-system/k8s/secrets.yaml

# 2. Redis (no dependencies)
kubectl apply -f secure-distributed-system/k8s/redis.yaml -n "$NAMESPACE"

# 3. Config server (no dependencies, but all other services depend on it)
kubectl apply -f secure-distributed-system/k8s/config-server.yaml -n "$NAMESPACE"
kubectl set image deployment/config-server \
  "config-server=$REGISTRY/config-server:$TAG" -n "$NAMESPACE"
kubectl rollout status deployment/config-server -n "$NAMESPACE" --timeout=180s

# 4. Eureka server (depends on config-server)
kubectl apply -f secure-distributed-system/k8s/eureka-server.yaml -n "$NAMESPACE"
kubectl set image deployment/eureka-server \
  "eureka-server=$REGISTRY/eureka-server:$TAG" -n "$NAMESPACE"
kubectl rollout status deployment/eureka-server -n "$NAMESPACE" --timeout=180s

# 5. Business services in parallel (all depend on eureka-server)
for SVC in api-gateway auth-service user-service order-service product-service; do
  kubectl apply -f "secure-distributed-system/k8s/${SVC}.yaml" -n "$NAMESPACE"
  kubectl set image "deployment/$SVC" \
    "$SVC=$REGISTRY/$SVC:$TAG" -n "$NAMESPACE"
done

# 6. Apply Istio resources
kubectl apply -f secure-distributed-system/k8s/istio/ -n "$NAMESPACE" || true

# 7. Wait for all business service rollouts
for SVC in api-gateway auth-service user-service order-service product-service; do
  kubectl rollout status "deployment/$SVC" -n "$NAMESPACE" --timeout=300s
done
```

---

## 11. Step 7 — Verify Deployments

```bash
# Verify all projects
./deployment/scripts/06-verify.sh all

# Verify individually
./deployment/scripts/06-verify.sh grpc
./deployment/scripts/06-verify.sh sds
```

### Manual verification

```bash
# Check pod status
kubectl -n grpc-enterprise get pods -o wide
kubectl -n secure-distributed get pods -o wide

# Check services
kubectl -n grpc-enterprise get svc
kubectl -n secure-distributed get svc

# Check Helm release status
helm status grpc-enterprise -n grpc-enterprise
helm status secure-distributed -n secure-distributed

# Health check a specific service
kubectl -n grpc-enterprise exec deploy/financial-service -- \
  wget -qO- http://localhost:8081/actuator/health

kubectl -n secure-distributed exec deploy/api-gateway -- \
  wget -qO- http://localhost:8000/actuator/health

# Port-forward Eureka dashboard
kubectl -n secure-distributed port-forward svc/eureka-server 8761:8761
# Open: http://localhost:8761

# Check Istio sidecar injection
kubectl -n grpc-enterprise get pods -o jsonpath='{range .items[*]}{.metadata.name}{"\t"}{.spec.containers[*].name}{"\n"}{end}'

# View Istio proxy status
istioctl proxy-status -n grpc-enterprise
```

---

## 12. CI/CD Pipelines

Both projects have GitLab CI and Jenkins pipelines. The monorepo root `.gitlab-ci.yml` triggers child pipelines based on changed files.

### GitLab CI

#### Pipeline overview

```
[Root .gitlab-ci.yml]
    │ (changes in grpc-enterprise-v3/)
    ├──► grpc-enterprise-v3/.gitlab-ci.yml
    │       test → quality → docker → scan → infrastructure → deploy → smoke-test
    │
    │ (changes in secure-distributed-system/)
    └──► secure-distributed-system/.gitlab-ci.yml
            build → test → quality → docker → scan → deploy → verify → integration-test
```

#### Required GitLab CI/CD Variables

Go to **Project → Settings → CI/CD → Variables** and add:

| Variable | Description | Masked |
|---|---|---|
| `AWS_ACCESS_KEY_ID` | AWS IAM access key | Yes |
| `AWS_SECRET_ACCESS_KEY` | AWS IAM secret key | Yes |
| `AWS_ACCOUNT_ID` | 12-digit AWS account ID | No |
| `DB_PASSWORD` | RDS PostgreSQL master password | Yes |
| `JWT_SECRET` | JWT signing secret | Yes |
| `TF_VAR_db_password` | Terraform RDS password variable | Yes |
| `REDIS_PASSWORD` | Redis password (sds only, optional) | Yes |
| `SONARQUBE_ENABLED` | Set to `true` to enable SonarQube | No |
| `SONAR_HOST_URL` | SonarQube server URL | No |
| `SONAR_TOKEN` | SonarQube token | Yes |

#### Pipeline stages — grpc-enterprise-v3

| Stage | Job | Trigger | Manual? |
|---|---|---|---|
| `test` | Maven verify + JUnit | always | No |
| `quality` | OWASP + SpotBugs | after test | No (non-blocking) |
| `docker` | Build + push 5 ECR images | `main` branch | No |
| `scan` | Trivy HIGH/CRITICAL scan | `main` branch | No (non-blocking) |
| `infrastructure` | `terraform plan` | `main` branch | No |
| `infrastructure` | `terraform apply` | `main` branch | **Yes** |
| `deploy` | kubectl apply to EKS | `main` branch | **Yes** |
| `smoke-test` | Actuator health checks | after deploy | No |

#### Pipeline stages — secure-distributed-system

| Stage | Job | Trigger | Manual? |
|---|---|---|---|
| `build` | Maven package | always | No |
| `test` | Maven test (skippable via `SKIP_TESTS=true`) | always | No |
| `quality` | OWASP (blocking on prod if CVSS≥9) + SonarQube (optional) | always | No |
| `docker` | Build + push 7 ECR images | `main` branch | No |
| `scan` | Trivy scans | `main` branch | No (non-blocking) |
| `deploy` | Ordered K8s apply (config → eureka → business) | `main` branch | **Yes** |
| `verify` | Rollout status + pod/svc summary | after deploy | No |
| `integration-test` | Actuator health checks via kubectl exec | after verify | No |

#### Pipeline variables (secure-distributed-system)

| Variable | Values | Default | Description |
|---|---|---|---|
| `DEPLOY_ENV` | `dev`, `staging`, `prod` | `dev` | Target environment |
| `SKIP_TESTS` | `true`, `false` | `false` | Skip unit tests |

---

### Jenkins

#### grpc-enterprise-v3 Jenkinsfile

Single declarative pipeline (`agent any`). Stages:

1. **Checkout** — `git checkout`
2. **Build & Test** — `mvn clean verify`
3. **Code Quality** — OWASP + SpotBugs (parallel, non-blocking)
4. **Docker Build** — 4 images in parallel
5. **Docker Image Scan** — Trivy (non-blocking)
6. **Push to ECR** — tag and push with `build-<BUILD_NUMBER>`
7. **Terraform** *(main branch only)* — init → validate → plan → apply (with input gate)
8. **Deploy to EKS** *(main branch only)* — kubectl apply + rollout wait
9. **Smoke Test** — pod readiness check

**Required Jenkins credentials:**

| Credential ID | Type | Description |
|---|---|---|
| `aws-credentials` | AWS | IAM access key + secret key |
| `db-password` | Secret text | RDS master password |

#### secure-distributed-system Jenkinsfile

Kubernetes-native Jenkins pipeline (`agent { kubernetes { ... } }`) with dedicated containers per tool:

| Container | Image | Purpose |
|---|---|---|
| `maven` | `maven:3.8-openjdk-8` | Java build, tests, quality |
| `docker` | `docker:24-dind` | Image build and push |
| `kubectl` | `bitnami/kubectl:1.28` | K8s deployment |
| `aws-cli` | `amazon/aws-cli:2.15.0` | ECR auth, EKS kubeconfig |

**Pipeline parameters:**

| Parameter | Type | Default | Description |
|---|---|---|---|
| `DEPLOY_ENV` | Choice | `dev` | Target environment: dev/staging/prod |
| `SERVICES` | String | `all` | Comma-separated services to deploy, or `all` |
| `SKIP_TESTS` | Boolean | `false` | Skip unit tests |
| `DOCKER_TAG` | String | *(empty)* | Image tag (defaults to BUILD_NUMBER) |

**Required Jenkins credentials:**

| Credential ID | Type | Description |
|---|---|---|
| `aws-credentials` | AWS | IAM key pair |
| `kubeconfig-dev` | Secret file | kubeconfig for dev cluster |
| `kubeconfig-staging` | Secret file | kubeconfig for staging cluster |
| `kubeconfig-prod` | Secret file | kubeconfig for prod cluster |

---

## 13. Helm Operations Reference

### List releases

```bash
helm list -A                                        # all namespaces
helm list -n grpc-enterprise
helm list -n secure-distributed
```

### Upgrade with new image tag

```bash
# grpc-enterprise-v3
helm upgrade grpc-enterprise grpc-enterprise-v3/helm/ \
  -n grpc-enterprise \
  --reuse-values \
  --set "user-grpc-service.image.tag=build-99" \
  --set "financial-service.image.tag=build-99" \
  --set "health-service.image.tag=build-99" \
  --set "social-service.image.tag=build-99" \
  --wait --atomic

# secure-distributed-system (update only api-gateway)
helm upgrade secure-distributed secure-distributed-system/helm/ \
  -n secure-distributed \
  --reuse-values \
  --set "api-gateway.image.tag=build-99" \
  --wait --atomic
```

### Scale a service

```bash
# Scale financial-service to 3 replicas via Helm
helm upgrade grpc-enterprise grpc-enterprise-v3/helm/ \
  -n grpc-enterprise \
  --reuse-values \
  --set "financial-service.replicaCount=3"
```

### View rendered templates

```bash
helm template grpc-release grpc-enterprise-v3/helm/ \
  --values deployment/environments/prod/grpc-enterprise-values.yaml

helm template sds-release secure-distributed-system/helm/ \
  --values deployment/environments/prod/secure-distributed-values.yaml
```

### Lint charts

```bash
helm lint grpc-enterprise-v3/helm/
helm lint secure-distributed-system/helm/
```

### Diff a pending upgrade (requires helm-diff plugin)

```bash
helm plugin install https://github.com/databus23/helm-diff

helm diff upgrade grpc-enterprise grpc-enterprise-v3/helm/ \
  -n grpc-enterprise \
  --values deployment/environments/prod/grpc-enterprise-values.yaml \
  --set "financial-service.image.tag=build-99"
```

### Rollback

```bash
# View history
helm history grpc-enterprise -n grpc-enterprise

# Rollback to previous revision
helm rollback grpc-enterprise -n grpc-enterprise

# Rollback to specific revision
helm rollback grpc-enterprise 3 -n grpc-enterprise --wait
```

---

## 14. Secrets Management

### grpc-enterprise-v3

| Secret Name | Namespace | Keys | Used by |
|---|---|---|---|
| `grpc-enterprise-db-secret` | `grpc-enterprise` | `username`, `password` | user-grpc-service |
| `financial-service-db-secret` | `grpc-enterprise` | `username`, `password` | financial-service |
| `health-service-db-secret` | `grpc-enterprise` | `username`, `password` | health-service |
| `social-service-db-secret` | `grpc-enterprise` | `username`, `password` | social-service |
| `grpc-enterprise-jwt` | `grpc-enterprise` | `jwt-secret` | user-grpc-service |
| `enterprise-tls-credential` | `istio-system` | TLS cert+key | Istio Gateway |

### secure-distributed-system

| Secret Name | Namespace | Keys | Used by |
|---|---|---|---|
| `app-secrets` | `secure-distributed` | `jwt-secret`, `config-user`, `config-password`, `eureka-user`, `eureka-password` | all services |
| `secure-distributed-tls` | `istio-system` | TLS cert+key | Istio Gateway |

### Create TLS secret for Istio Gateway

```bash
# Generate self-signed cert (dev/testing only)
openssl req -x509 -newkey rsa:4096 -sha256 -days 365 -nodes \
  -keyout tls.key -out tls.crt \
  -subj "/CN=api.enterprise.example.com" \
  -addext "subjectAltName=DNS:api.enterprise.example.com,DNS:grpc.enterprise.example.com"

# Create secret in istio-system namespace
kubectl create secret tls enterprise-tls-credential \
  --cert=tls.crt --key=tls.key \
  -n istio-system
```

### AWS Secrets Manager integration (production)

For production, manage secrets via AWS Secrets Manager and sync to Kubernetes using the External Secrets Operator:

```bash
# Install External Secrets Operator
helm repo add external-secrets https://charts.external-secrets.io
helm install external-secrets external-secrets/external-secrets \
  -n external-secrets --create-namespace

# Then set secrets.create: false in Helm values and create an ExternalSecret CR
```

---

## 15. Environment Configuration

### Environment values files

| File | Purpose |
|---|---|
| `deployment/environments/dev/grpc-enterprise-values.yaml` | Dev: 1 replica, no PDB, no Istio, reduced resources |
| `deployment/environments/dev/secure-distributed-values.yaml` | Dev: 1 replica, no PDB, no Istio, reduced resources |
| `deployment/environments/staging/grpc-enterprise-values.yaml` | Staging: 2 replicas, PDB, Istio, staging hostnames |
| `deployment/environments/staging/secure-distributed-values.yaml` | Staging: 2 replicas, PDB, Istio |
| `deployment/environments/prod/grpc-enterprise-values.yaml` | Prod: 3/2 replicas, PDB, Istio, 10% tracing |
| `deployment/environments/prod/secure-distributed-values.yaml` | Prod: 2 replicas, PDB, Istio, external secrets |

### Key environment differences

| Setting | dev | staging | prod |
|---|---|---|---|
| `global.istio.enabled` | `false` | `true` | `true` |
| `secrets.create` (sds) | `true` | `false` | `false` |
| Replica count | `1` | `2` | `2–3` |
| PDB enabled | `false` | `true` | `true` |
| `springProfile` (sds) | `dev` | `staging` | `prod` |
| Tracing sampling | N/A | 100% | 10% |

---

## 16. Observability

### Prometheus scraping

All Spring Boot services expose `/actuator/prometheus` and are annotated for scraping:

```yaml
annotations:
  prometheus.io/scrape: "true"
  prometheus.io/port: "<service-port>"
  prometheus.io/path: "/actuator/prometheus"
```

### Grafana dashboards

A pre-built Grafana dashboard is at `grpc-enterprise-v3/grafana-dashboard.json`. Import it via:

```
Grafana → + → Import → Upload JSON file → Select grafana-dashboard.json
```

### Distributed tracing (Jaeger)

Configured via the Telemetry resource in both Helm charts:

```bash
# Port-forward Jaeger UI (after installing Jaeger in cluster)
kubectl port-forward -n istio-system svc/tracing 16686:80
# Open: http://localhost:16686
```

### Actuator endpoints

| Endpoint | Description |
|---|---|
| `/actuator/health` | Overall health (UP/DOWN) |
| `/actuator/health/readiness` | Readiness probe target |
| `/actuator/health/liveness` | Liveness probe target |
| `/actuator/prometheus` | Prometheus metrics |
| `/actuator/info` | Application info |
| `/v3/api-docs` | OpenAPI spec (JSON) |
| `/swagger-ui/index.html` | Swagger UI |

---

## 17. Rollback Procedures

### Helm rollback (fastest)

```bash
# View revision history
helm history grpc-enterprise -n grpc-enterprise
helm history secure-distributed -n secure-distributed

# Rollback to previous revision
helm rollback grpc-enterprise -n grpc-enterprise --wait
helm rollback secure-distributed -n secure-distributed --wait

# Rollback to specific revision number
helm rollback grpc-enterprise 3 -n grpc-enterprise --wait
```

### kubectl rollback (raw manifests approach)

```bash
# View rollout history
kubectl rollout history deployment/financial-service -n grpc-enterprise

# Rollback to previous version
kubectl rollout undo deployment/financial-service -n grpc-enterprise

# Rollback to specific revision
kubectl rollout undo deployment/financial-service \
  --to-revision=2 -n grpc-enterprise
```

### Image-specific rollback

```bash
# Set a specific image tag directly
kubectl set image deployment/api-gateway \
  api-gateway=123456789.dkr.ecr.us-east-1.amazonaws.com/secure-distributed/api-gateway:build-40 \
  -n secure-distributed

kubectl rollout status deployment/api-gateway -n secure-distributed
```

---

## 18. Teardown

### Remove Helm releases (keeps AWS infra)

```bash
# Remove grpc-enterprise-v3 release and namespace
./deployment/scripts/teardown.sh grpc

# Remove secure-distributed-system release and namespace
./deployment/scripts/teardown.sh sds

# Remove both
./deployment/scripts/teardown.sh all
```

### Remove Helm releases + destroy AWS infrastructure

```bash
# WARNING: Destroys EKS, RDS, VPC, ECR repositories, etc.
# Requires double confirmation per project.
./deployment/scripts/teardown.sh all --destroy-infra
```

### Manual Terraform destroy

```bash
cd grpc-enterprise-v3/terraform
terraform destroy -auto-approve

cd secure-distributed-system/terraform
terraform destroy -auto-approve
```

---

## 19. Troubleshooting

### Pods not starting

```bash
# Check pod status and events
kubectl describe pod <pod-name> -n <namespace>

# Check container logs
kubectl logs <pod-name> -n <namespace>
kubectl logs <pod-name> -n <namespace> --previous   # crashed container

# Check init container logs (SDS services)
kubectl logs <pod-name> -c wait-for-eureka-server -n secure-distributed
kubectl logs <pod-name> -c wait-for-config-server -n secure-distributed
```

### Istio sidecar issues

```bash
# Verify sidecar is injected (should see "istio-proxy" container)
kubectl get pod <pod-name> -n <namespace> -o jsonpath='{.spec.containers[*].name}'

# Check proxy configuration
istioctl proxy-config all <pod-name> -n <namespace>

# Check mTLS status
istioctl x describe pod <pod-name> -n <namespace>

# If istio-injection is missing on namespace:
kubectl label namespace grpc-enterprise istio-injection=enabled
kubectl rollout restart deployment -n grpc-enterprise
```

### Helm release stuck

```bash
# Check release status
helm status grpc-enterprise -n grpc-enterprise

# If stuck in "pending-upgrade":
helm rollback grpc-enterprise -n grpc-enterprise

# Force delete and reinstall (last resort)
helm uninstall grpc-enterprise -n grpc-enterprise
helm install grpc-enterprise grpc-enterprise-v3/helm/ ...
```

### ECR push failures

```bash
# Re-authenticate to ECR
REGION=us-east-1
ACCOUNT=$(aws sts get-caller-identity --query Account --output text)
aws ecr get-login-password --region $REGION \
  | docker login --username AWS --password-stdin \
    "$ACCOUNT.dkr.ecr.$REGION.amazonaws.com"

# Ensure ECR repositories exist
aws ecr describe-repositories --region $REGION
# Create missing repository:
aws ecr create-repository --repository-name financial-service --region $REGION
```

### Services can't reach each other (Istio mTLS)

```bash
# Check AuthorizationPolicies
kubectl get authorizationpolicies -n grpc-enterprise
kubectl describe authorizationpolicy allow-financial-service -n grpc-enterprise

# Temporarily disable mTLS (debug only)
kubectl delete peerauthentication default -n grpc-enterprise
```

### secure-distributed-system — services not registering with Eureka

```bash
# 1. Verify config-server is UP
kubectl exec -n secure-distributed deploy/config-server -- \
  wget -qO- http://localhost:8888/actuator/health

# 2. Verify eureka-server is UP
kubectl exec -n secure-distributed deploy/eureka-server -- \
  wget -qO- http://localhost:8761/actuator/health

# 3. Check Eureka registered instances
kubectl port-forward -n secure-distributed svc/eureka-server 8761:8761
# Open: http://localhost:8761

# 4. Check service logs for config fetch errors
kubectl logs -n secure-distributed deploy/auth-service --tail=50
```

### Terraform state lock

```bash
# If a previous apply was interrupted and left a state lock:
# Find the lock ID from the error message, then:
aws dynamodb delete-item \
  --table-name grpc-enterprise-v3-tflock \
  --key '{"LockID": {"S": "<lock-id-from-error>"}}'
```

---

## Quick Reference

### Full deployment from scratch (grpc-enterprise-v3)

```bash
# 1. Build
./deployment/scripts/01-build.sh

# 2. Push images
./deployment/scripts/02-docker-push.sh \
  "$(aws sts get-caller-identity --query Account --output text).dkr.ecr.us-east-1.amazonaws.com" \
  "$(git rev-parse --short HEAD)" \
  grpc

# 3. Provision infrastructure
./deployment/scripts/03-terraform.sh grpc apply production

# 4. Setup K8s
export DB_PASSWORD="..." JWT_SECRET="..."
./deployment/scripts/04-k8s-setup.sh grpc

# 5. Deploy with Helm
./deployment/scripts/05-helm-deploy.sh grpc prod "$(git rev-parse --short HEAD)"

# 6. Verify
./deployment/scripts/06-verify.sh grpc
```

### Full deployment from scratch (secure-distributed-system)

```bash
# 1. Build
./deployment/scripts/01-build.sh

# 2. Push images
./deployment/scripts/02-docker-push.sh \
  "$(aws sts get-caller-identity --query Account --output text).dkr.ecr.us-east-1.amazonaws.com" \
  "$(git rev-parse --short HEAD)" \
  sds

# 3. Provision infrastructure
./deployment/scripts/03-terraform.sh sds apply production

# 4. Setup K8s
export DB_PASSWORD="..." JWT_SECRET="..."
./deployment/scripts/04-k8s-setup.sh sds

# 5. Deploy with Helm
./deployment/scripts/05-helm-deploy.sh sds prod "$(git rev-parse --short HEAD)"

# 6. Verify
./deployment/scripts/06-verify.sh sds
```
