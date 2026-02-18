# Enterprise Microservices Monorepo

A monorepo containing two independent, production-grade microservices platforms deployed on AWS EKS with Istio service mesh and a shared GitLab CI/CD pipeline.

---

## Repository Layout

```
.
├── .gitlab-ci.yml                  # Root pipeline — triggers child pipelines per project
├── grpc-enterprise-v3/             # gRPC + REST enterprise platform
│   ├── .gitlab-ci.yml              # Child pipeline (test → quality → docker → scan → infra → deploy → smoke)
│   ├── Jenkinsfile                 # Legacy Jenkins pipeline (reference only)
│   ├── user-grpc-service/          # Auth authority — REST :8080, gRPC :9090
│   ├── financial-service/          # Bank accounts & transactions — REST :8081
│   ├── health-service/             # Health records & vitals — REST :8082
│   ├── social-service/             # Profiles, posts, connections — REST :8083
│   ├── enterprise-ui/              # React 18 SPA (Vite + Nginx)
│   ├── k8s/                        # Kubernetes + Istio manifests
│   └── terraform/                  # AWS infrastructure (VPC, EKS, RDS, ECR)
└── secure-distributed-system/      # Spring Cloud microservices platform
    ├── .gitlab-ci.yml              # Child pipeline (build → test → quality → docker → scan → deploy → verify → integration-test)
    ├── Jenkinsfile                 # Legacy Jenkins pipeline (reference only)
    ├── common-lib/                 # Shared JWT, DTOs, exception handlers
    ├── config-server/              # Spring Cloud Config Server — :8888
    ├── eureka-server/              # Netflix Eureka service registry — :8761
    ├── api-gateway/                # Spring Cloud Gateway + Redis rate limiting — :8000
    ├── auth-service/               # JWT auth (HS512, 15 min access / 7 day refresh) — :8080
    ├── user-service/               # User profile management — :8081
    ├── order-service/              # Order management — :8082
    ├── product-service/            # Product catalog + Redis cache — :8083
    ├── config-repo/                # Git-backed YAML configuration files
    ├── k8s/                        # Kubernetes + Istio manifests
    └── terraform/                  # AWS infrastructure (VPC, EKS, RDS, ElastiCache, ECR)
```

---

## Table of Contents

- [CI/CD Pipelines (GitLab)](#cicd-pipelines-gitlab)
  - [Pipeline Overview](#pipeline-overview)
  - [Required Variables](#required-variables)
  - [grpc-enterprise-v3 Stages](#grpc-enterprise-v3-stages)
  - [secure-distributed-system Stages](#secure-distributed-system-stages)
- [Project 1 — grpc-enterprise-v3](#project-1--grpc-enterprise-v3)
  - [Architecture](#architecture)
  - [Backend Microservices](#backend-microservices)
  - [Frontend — enterprise-ui](#frontend--enterprise-ui)
  - [Infrastructure](#infrastructure)
  - [Local Development](#local-development)
  - [Docker Builds](#docker-builds)
  - [Production Deployment](#production-deployment)
  - [API Reference](#api-reference)
  - [Authentication](#authentication)
  - [Observability](#observability)
- [Project 2 — secure-distributed-system](#project-2--secure-distributed-system)
  - [Architecture](#architecture-1)
  - [Services](#services)
  - [Local Development (Docker Compose)](#local-development-docker-compose)
  - [Local Development (Maven)](#local-development-maven)
- [Security Notes](#security-notes)
- [Production Readiness Checklist](#production-readiness-checklist)

---

## CI/CD Pipelines (GitLab)

### Pipeline Overview

The root [.gitlab-ci.yml](.gitlab-ci.yml) acts as a monorepo dispatcher. It uses GitLab **child pipeline triggers** with `changes:` rules so each project's pipeline fires only when its own files change — preventing unnecessary builds.

```
Push / MR
    │
    ├─► grpc-enterprise-v3 child pipeline     (when grpc-enterprise-v3/** changes)
    └─► secure-distributed-system child pipeline (when secure-distributed-system/** changes)
```

Both child pipelines run independently and in parallel.

### Required Variables

Set these under **Settings → CI/CD → Variables** in your GitLab project. Mark secrets as **Masked** and **Protected**.

| Variable | Description |
|---|---|
| `AWS_ACCESS_KEY_ID` | IAM access key with ECR / EKS / Terraform permissions |
| `AWS_SECRET_ACCESS_KEY` | Corresponding IAM secret key |
| `AWS_ACCOUNT_ID` | 12-digit AWS account ID (used to build ECR URLs) |
| `DB_PASSWORD` | RDS PostgreSQL master password (written to K8s secrets) |
| `JWT_SECRET` | JWT signing secret (written to K8s secrets) |
| `TF_VAR_db_password` | Terraform variable for RDS master password |
| `SONARQUBE_ENABLED` | Set to `true` to activate SonarQube (optional) |
| `SONAR_HOST_URL` | SonarQube server URL (optional) |
| `SONAR_TOKEN` | SonarQube authentication token (optional) |

### grpc-enterprise-v3 Stages

Defined in [grpc-enterprise-v3/.gitlab-ci.yml](grpc-enterprise-v3/.gitlab-ci.yml).

| Stage | Jobs | When | Notes |
|---|---|---|---|
| `test` | `test` | Always | `mvn clean verify`, JUnit + JaCoCo artifacts |
| `quality` | `quality:owasp`, `quality:spotbugs` | Always | Parallel, non-blocking (`allow_failure: true`) |
| `docker` | 5 jobs in parallel | `main` only | Multi-stage build + push to ECR per service |
| `scan` | 5 Trivy jobs in parallel | `main` only | ECR image scan, non-blocking |
| `infrastructure` | `terraform:plan` (auto), `terraform:apply` (manual) | `main` only | Manual approval gate before infra changes |
| `deploy` | `deploy` | `main`, **manual** | Approval gate; `kubectl apply` + `kubectl set image` |
| `smoke-test` | `smoke-test` | `main` only | Pod readiness + actuator `/health` via `kubectl exec` |

Pipeline DAG (main branch):

```
test ──► quality:owasp    (allow_failure)
     └─► quality:spotbugs (allow_failure)
     └─► docker:user-grpc-service ──► scan:user-grpc-service ──┐
     └─► docker:financial-service ──► scan:financial-service ──┤
     └─► docker:health-service    ──► scan:health-service    ──┼─► terraform:plan ──► terraform:apply (manual)
     └─► docker:social-service    ──► scan:social-service    ──┤                              │
     └─► docker:enterprise-ui     ──► scan:enterprise-ui     ──┘                              │
                                                                └─────────────────────────────┴─► deploy (manual) ──► smoke-test
```

### secure-distributed-system Stages

Defined in [secure-distributed-system/.gitlab-ci.yml](secure-distributed-system/.gitlab-ci.yml).

| Stage | Jobs | When | Notes |
|---|---|---|---|
| `build` | `build` | Always | `mvn clean package`, JARs as artifacts |
| `test` | `test` | Unless `SKIP_TESTS=true` | JUnit + JaCoCo artifacts |
| `quality` | `quality:owasp`, `quality:sonarqube` | Always / if `SONARQUBE_ENABLED` | OWASP blocks at CVSS ≥ 9 on `DEPLOY_ENV=prod` |
| `docker` | 7 jobs in parallel | `main` only | Single-stage build (uses JARs from build stage) + push to ECR |
| `scan` | 7 Trivy jobs in parallel | `main` only | ECR image scan, non-blocking |
| `deploy` | `deploy` | `main`, **manual** | Ordered: namespace → Redis → config-server → eureka → business services |
| `verify` | `verify` | `main` only | Rollout status + pod/svc summary |
| `integration-test` | `integration-test` | `main` only | Actuator `/health` per service via `kubectl exec`, `allow_failure` |

Deployment order enforced inside the `deploy` job:

```
1. Namespace + Secrets + Redis
2. config-server        (kubectl rollout status --timeout=180s)
3. eureka-server        (kubectl rollout status --timeout=180s)
4. api-gateway          ┐
   auth-service         │ parallel kubectl apply
   user-service         │
   order-service        │
   product-service      ┘
5. Istio VirtualServices / DestinationRules
```

#### Triggering with parameters

Pass variables at pipeline trigger time to control behaviour:

```bash
# Deploy to prod with OWASP blocking
curl -X POST "https://gitlab.example.com/api/v4/projects/$PROJECT_ID/trigger/pipeline" \
  --form "token=$TRIGGER_TOKEN" \
  --form "ref=main" \
  --form "variables[DEPLOY_ENV]=prod"

# Skip tests for a hotfix build
curl -X POST "https://gitlab.example.com/api/v4/projects/$PROJECT_ID/trigger/pipeline" \
  --form "token=$TRIGGER_TOKEN" \
  --form "ref=main" \
  --form "variables[SKIP_TESTS]=true"
```

---

## Project 1 — grpc-enterprise-v3

A production-grade enterprise platform providing user identity (gRPC + REST), financial management, health records, and social networking. Deployed on AWS EKS with Istio STRICT mTLS.

### Architecture

```
Internet
    │
    ▼
Istio Ingress Gateway (TLS termination, HTTP → HTTPS redirect)
    │
    ├──► enterprise-ui      (React SPA, Nginx, :80)
    │
    ├──► user-grpc-service  (REST :8080 + gRPC :9090)
    ├──► financial-service  (REST :8081)
    ├──► health-service     (REST :8082)
    └──► social-service     (REST :8083)
              │
              ▼
     PostgreSQL RDS (4 databases)
     grpcdb │ financialdb │ healthdb │ socialdb
```

All inter-service communication runs through Istio with **STRICT mTLS**. JWT tokens are validated at two layers: the Spring Security filter (per service) and the Istio mesh `RequestAuthentication` policy.

### Backend Microservices

All four services share the same stack:

| Component | Version |
|---|---|
| Java | 11 runtime |
| Spring Boot | 2.7.18 |
| Spring Security | JWT HS256 (JJWT 0.11.5), BCrypt |
| Resilience4j | 1.7.1 (circuit breaker) |
| Database | PostgreSQL 14+ with Flyway migrations |
| API Docs | springdoc-openapi-ui 1.7.0 (OpenAPI 3.0) |
| Observability | Micrometer + Prometheus + Actuator |

#### user-grpc-service

**Role:** Authentication authority and user identity. The only service that issues JWT tokens (24-hour expiry, HS256).

**Ports:** `8080` (REST) · `9090` (gRPC)

**gRPC interface** (`user.proto`):

| RPC | Request | Response |
|---|---|---|
| `CreateUser` | `UserRequest` | `UserResponse` |
| `GetUser` | `UserIdRequest` | `UserResponse` |
| `DeleteUser` | `UserIdRequest` | `DeleteResponse` |

**REST endpoints:**

| Method | Path | Auth | Description |
|---|---|---|---|
| `POST` | `/api/auth/register` | Public | Register, returns JWT |
| `POST` | `/api/auth/login` | Public | Authenticate, returns JWT |
| `POST` | `/api/users` | Bearer | Create user |
| `GET` | `/api/users/{id}` | Bearer | Get user by ID |
| `DELETE` | `/api/users/{id}` | Bearer | Delete user |

#### financial-service

**Role:** Bank accounts and financial transactions. **Port:** `8081`

| Method | Path | Auth | Description |
|---|---|---|---|
| `POST` | `/api/accounts` | Bearer | Create account |
| `GET` | `/api/accounts/user/{userId}` | Bearer | List accounts for a user |
| `GET` | `/api/accounts/{id}` | Bearer | Get account by ID |
| `POST` | `/api/transactions` | Bearer | Create transaction (`DEPOSIT` / `WITHDRAWAL` / `TRANSFER`) |
| `GET` | `/api/transactions/account/{accountId}` | Bearer | List transactions |

#### health-service

**Role:** Health records and time-series vitals. **Port:** `8082`

| Method | Path | Auth | Description |
|---|---|---|---|
| `POST` | `/api/health-records` | Bearer | Create or update health record (upsert) |
| `GET` | `/api/health-records/user/{userId}` | Bearer | Get record |
| `POST` | `/api/vitals` | Bearer | Record vital signs measurement |
| `GET` | `/api/vitals/user/{userId}` | Bearer | List all vitals |
| `GET` | `/api/vitals/user/{userId}/latest` | Bearer | Most recent vital |

#### social-service

**Role:** LinkedIn-style profiles, posts, and connections. **Port:** `8083`

| Method | Path | Auth | Description |
|---|---|---|---|
| `POST` | `/api/profiles` | Bearer | Create or update profile |
| `GET` | `/api/profiles/user/{userId}` | Bearer | Get profile |
| `POST` | `/api/posts` | Bearer | Create post |
| `GET` | `/api/posts/user/{userId}` | Bearer | List posts |
| `POST` | `/api/connections` | Bearer | Send connection request (`PENDING`) |
| `PUT` | `/api/connections/{id}/accept` | Bearer | Accept request (`ACCEPTED`) |
| `GET` | `/api/connections/user/{userId}` | Bearer | List connections |

### Frontend — enterprise-ui

React 18.2 SPA built with Vite 5, served by Nginx in production.

| Route | Page |
|---|---|
| `/` | Dashboard |
| `/users` | User management |
| `/accounts` | Bank accounts |
| `/transactions` | Financial transactions |
| `/health-records` | Health records |
| `/vitals` | Vital signs |
| `/profiles` | Social profiles |
| `/posts` | Posts feed |
| `/connections` | Connections |

### Infrastructure

#### Kubernetes

`k8s/` — each service has a Deployment (rolling update, non-root, read-only FS, all caps dropped), ClusterIP Service, and PodDisruptionBudget (`minAvailable: 1`).

| Service | Replicas | CPU req/limit | Mem req/limit |
|---|---|---|---|
| user-grpc-service | 3 | 250m / 1000m | 512Mi / 1Gi |
| financial-service | 2 | 250m / 1000m | 512Mi / 1Gi |
| health-service | 2 | 250m / 1000m | 512Mi / 1Gi |
| social-service | 2 | 250m / 1000m | 512Mi / 1Gi |
| enterprise-ui | 2 | 250m / 1000m | 512Mi / 1Gi |

#### Istio (`k8s/istio/`)

STRICT mTLS peer authentication, default deny-all `AuthorizationPolicy`, per-service VirtualServices with timeouts/retries/CORS, Envoy local rate limiting, and distributed tracing telemetry.

#### Terraform (`terraform/`)

| Resource | Configuration |
|---|---|
| VPC | 10.0.0.0/16, 3 AZs, NAT Gateway, VPC Flow Logs |
| EKS | v1.28, t3.medium nodes (min 2 / desired 3 / max 5) |
| RDS | PostgreSQL 15.4, db.t3.medium, gp3 20 GB (auto-scale 100 GB), Multi-AZ |
| ECR | Immutable tags, scan on push, keep last 20 images |
| State | S3 `grpc-enterprise-v3-tfstate` / DynamoDB `grpc-enterprise-v3-tflock` |

### Local Development

#### 1. Start PostgreSQL

```bash
docker run -d --name postgres -e POSTGRES_PASSWORD=postgres -p 5432:5432 postgres:14
docker exec -it postgres psql -U postgres -c "CREATE DATABASE grpcdb;"
docker exec -it postgres psql -U postgres -c "CREATE DATABASE financialdb;"
docker exec -it postgres psql -U postgres -c "CREATE DATABASE healthdb;"
docker exec -it postgres psql -U postgres -c "CREATE DATABASE socialdb;"
```

#### 2. Build and run backend services

```bash
cd grpc-enterprise-v3
mvn clean package -DskipTests

java -jar user-grpc-service/target/user-grpc-service-3.0.0.jar  # REST :8080, gRPC :9090
java -jar financial-service/target/financial-service-3.0.0.jar   # :8081
java -jar health-service/target/health-service-3.0.0.jar          # :8082
java -jar social-service/target/social-service-3.0.0.jar          # :8083
```

#### 3. Start the React frontend

```bash
cd grpc-enterprise-v3/enterprise-ui
npm install
npm run dev        # http://localhost:5173
```

#### 4. Verify

```bash
curl http://localhost:8080/actuator/health
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health
curl http://localhost:8083/actuator/health
```

### Docker Builds

```bash
cd grpc-enterprise-v3

docker build -f user-grpc-service/Dockerfile -t user-grpc-service:latest .
docker build -f financial-service/Dockerfile  -t financial-service:latest .
docker build -f health-service/Dockerfile     -t health-service:latest    .
docker build -f social-service/Dockerfile     -t social-service:latest    .
docker build -t enterprise-ui:latest enterprise-ui/
```

All backend Dockerfiles are multi-stage (Maven build → Eclipse Temurin JRE Alpine). The UI Dockerfile is multi-stage (Node → Nginx Alpine).

### Production Deployment

```bash
# 1. Provision infrastructure
cd grpc-enterprise-v3/terraform
terraform init
terraform plan  -var="db_password=$DB_PASSWORD"
terraform apply -var="db_password=$DB_PASSWORD"

# 2. Configure kubectl
aws eks update-kubeconfig --name grpc-enterprise-v3-eks --region us-east-1

# 3. Install Istio
istioctl install --set profile=production
kubectl apply -f k8s/istio/namespace.yaml

# 4. Create Kubernetes secrets
kubectl create secret generic db-credentials \
  --from-literal=url=jdbc:postgresql://<RDS_ENDPOINT>:5432/ \
  --from-literal=username=grpcadmin --from-literal=password=$DB_PASSWORD \
  -n grpc-enterprise
kubectl create secret generic jwt-secret \
  --from-literal=secret=$JWT_SECRET -n grpc-enterprise

# 5. Create RDS databases
psql -h <RDS_ENDPOINT> -U postgres -c "CREATE DATABASE grpcdb;"
psql -h <RDS_ENDPOINT> -U postgres -c "CREATE DATABASE financialdb;"
psql -h <RDS_ENDPOINT> -U postgres -c "CREATE DATABASE healthdb;"
psql -h <RDS_ENDPOINT> -U postgres -c "CREATE DATABASE socialdb;"

# 6. Apply Istio config and deploy
kubectl apply -f k8s/istio/ -n grpc-enterprise
kubectl apply -f k8s/       -n grpc-enterprise

# 7. Wait for rollouts
for SVC in user-grpc-service financial-service health-service social-service enterprise-ui; do
  kubectl rollout status deployment/$SVC -n grpc-enterprise
done
```

### API Reference

Swagger UI (local):

| Service | URL |
|---|---|
| user-grpc-service | http://localhost:8080/swagger-ui.html |
| financial-service | http://localhost:8081/swagger-ui.html |
| health-service | http://localhost:8082/swagger-ui.html |
| social-service | http://localhost:8083/swagger-ui.html |

### Authentication

```bash
# Register
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name":"Alice","email":"alice@example.com","password":"secret"}'
# → { "token": "<JWT>" }

# Use the token
curl http://localhost:8081/api/accounts/user/1 \
  -H "Authorization: Bearer <JWT>"
```

Tokens expire after **24 hours** (HS256). Re-authenticate via `/api/auth/login`.

### Observability

| Tool | Endpoint / Config |
|---|---|
| Prometheus | Scrapes `/actuator/prometheus` on each service |
| Grafana | Import `grpc-enterprise-v3/grafana-dashboard.json` |
| Jaeger | Configured via Istio `telemetry.yaml` |

**SLOs** (from `SRE-RUNBOOK.md`): 99.95% availability · P95 < 150 ms · error rate < 0.5%

---

## Project 2 — secure-distributed-system

A Spring Cloud microservices platform demonstrating centralized configuration, service discovery, a reactive API gateway with Redis rate limiting, and stateless JWT authentication with refresh tokens.

### Architecture

```
Internet
    │
    ▼
API Gateway (:8000)           Spring Cloud Gateway + Redis rate limiting
    │                         JWT validation at edge (HS512)
    ├──► auth-service    (:8080)   Access token 15 min / refresh token 7 days
    ├──► user-service    (:8081)
    ├──► order-service   (:8082)
    └──► product-service (:8083)  Redis caching
              │
              ▼
     config-server (:8888)         Spring Cloud Config (Git-backed)
     eureka-server (:8761)         Netflix Eureka service registry
     redis         (:6379)         Rate limiting + product cache
     PostgreSQL RDS                Production database (H2 for dev)
```

### Services

| Service | Port | Description |
|---|---|---|
| `config-server` | 8888 | Serves YAML configs from `config-repo/` to all services |
| `eureka-server` | 8761 | Service registry; dashboard at `http://localhost:8761` |
| `api-gateway` | 8000 | Edge: JWT filter, Redis rate limiting, CORS, routing |
| `auth-service` | 8080 | Register / login / refresh; issues HS512 JWT pairs |
| `user-service` | 8081 | User profile CRUD |
| `order-service` | 8082 | Order management |
| `product-service` | 8083 | Product catalog with Redis caching |
| `common-lib` | — | Shared library: `JwtTokenProvider`, `ApiResponse<T>`, global exception handlers |

**Default dev credentials:** `admin` / `admin123` (ROLE_USER + ROLE_ADMIN) · `user` / `user123` (ROLE_USER)

**H2 console (dev):** http://localhost:8080/h2-console — JDBC URL `jdbc:h2:mem:authdb`, username `sa`

**Startup order:** config-server must be healthy before eureka-server starts; all other services depend on both. This order is enforced in both the Docker Compose `depends_on` chain and the GitLab deploy job.

### Local Development (Docker Compose)

```bash
cd secure-distributed-system
mvn clean package -DskipTests    # build JARs first
docker-compose up -d             # starts full stack in dependency order
docker-compose logs -f
docker-compose down
```

### Local Development (Maven)

```bash
cd secure-distributed-system
mvn clean install

# Start in order (separate terminals)
cd config-server  && mvn spring-boot:run   # :8888 — first
cd eureka-server  && mvn spring-boot:run   # :8761 — wait for config-server
docker run -d -p 6379:6379 redis:7-alpine  # Redis

cd auth-service    && mvn spring-boot:run  # :8080
cd api-gateway     && mvn spring-boot:run  # :8000
cd user-service    && mvn spring-boot:run  # :8081
cd order-service   && mvn spring-boot:run  # :8082
cd product-service && mvn spring-boot:run  # :8083
```

---

## Security Notes

| Area | Status | Recommendation |
|---|---|---|
| JWT secrets | Hardcoded in `config-repo/*.yml` | Store in K8s Secrets / AWS Secrets Manager, inject via env var |
| mTLS | STRICT — all pod-to-pod traffic mutually authenticated | Never downgrade to PERMISSIVE in production |
| Pod security | Non-root, read-only FS, all caps dropped | Enforce with PodSecurityAdmission or OPA Gatekeeper |
| CI scans (grpc-enterprise-v3) | OWASP + SpotBugs + Trivy present, non-blocking | Set `allow_failure: false` on scan jobs to gate the pipeline |
| CI scans (secure-distributed-system) | OWASP blocks at CVSS ≥ 9 on prod; Trivy non-blocking | Promote Trivy to blocking once baseline is established |
| DB passwords | K8s Secrets via `secretKeyRef` | Rotate; consider IRSA + AWS Secrets Manager |
| Rate limiting | Envoy local (grpc) / Redis-backed gateway (sds) | Tune thresholds after load testing |
| CSRF | Disabled (stateless JWT APIs) | No action needed |

---

## Production Readiness Checklist

- [ ] Replace hardcoded JWT secrets with Kubernetes Secrets or AWS Secrets Manager
- [ ] Make CI security scans blocking (`allow_failure: false` on Trivy / OWASP jobs)
- [ ] Configure cert-manager + Let's Encrypt for automatic TLS certificate renewal
- [ ] Enable ArgoCD for GitOps-driven continuous deployment
- [ ] Set up Prometheus alerting rules with PagerDuty / OpsGenie integration
- [ ] Import and configure the Grafana dashboard (`grpc-enterprise-v3/grafana-dashboard.json`)
- [ ] Configure Jaeger for production-grade trace sampling and retention
- [ ] Enable EKS control plane audit logs in CloudWatch
- [ ] Review and tighten IAM roles with IRSA for RDS / Secrets Manager / ElastiCache access
- [ ] Enable RDS Enhanced Monitoring and CloudWatch alarms on key DB metrics
- [ ] Test PodDisruptionBudgets by draining a node during peak load
- [ ] Run load tests against rate-limiting configuration and tune thresholds
- [ ] Document and test the rollback procedure end-to-end (Istio traffic shifting + `kubectl rollout undo`)
- [ ] Rotate `TF_VAR_db_password` and `JWT_SECRET` GitLab CI variables on a schedule
