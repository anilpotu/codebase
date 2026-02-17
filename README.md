# grpc-enterprise-v3

A production-grade enterprise microservices platform providing user identity, financial management, health records, and social networking capabilities. Built on Spring Boot with gRPC and REST interfaces, deployed on AWS EKS with Istio service mesh.

---

## Table of Contents

- [Architecture Overview](#architecture-overview)
- [Projects](#projects)
  - [Backend Microservices](#backend-microservices)
    - [user-grpc-service](#user-grpc-service)
    - [financial-service](#financial-service)
    - [health-service](#health-service)
    - [social-service](#social-service)
  - [Frontend — enterprise-ui](#frontend--enterprise-ui)
- [Infrastructure](#infrastructure)
  - [Kubernetes Manifests](#kubernetes-manifests)
  - [Istio Service Mesh](#istio-service-mesh)
  - [Terraform (AWS)](#terraform-aws)
- [CI/CD Pipeline](#cicd-pipeline)
- [Prerequisites](#prerequisites)
- [Local Development Setup](#local-development-setup)
- [Docker Builds](#docker-builds)
- [Production Deployment (AWS EKS)](#production-deployment-aws-eks)
- [API Reference](#api-reference)
- [Authentication](#authentication)
- [Observability](#observability)
- [Security Notes](#security-notes)
- [Production Readiness Checklist](#production-readiness-checklist)

---

## Architecture Overview

```
Internet
    │
    ▼
Istio Ingress Gateway (TLS termination, HTTP→HTTPS redirect)
    │
    ├──► enterprise-ui  (React SPA, Nginx, port 80)
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

All inter-service communication runs through Istio with **STRICT mTLS**. JWT tokens are validated at two layers: the Spring Security filter (per service) and the Istio mesh `RequestAuthentication` policy (defence-in-depth). A default deny-all `AuthorizationPolicy` ensures only explicitly allowed traffic reaches each pod.

---

## Projects

### Backend Microservices

All four services share the same stack:

| Component | Version |
|---|---|
| Java | 11 (runtime), 8 source compatibility |
| Spring Boot | 2.7.18 |
| Spring Security | JWT (HS256, JJWT 0.11.5), BCrypt |
| Resilience4j | 1.7.1 (circuit breaker) |
| Database | PostgreSQL (Flyway migrations) |
| API Docs | springdoc-openapi-ui 1.7.0 (OpenAPI 3.0) |
| Observability | Micrometer + Prometheus + Actuator |

---

#### user-grpc-service

**Role:** Authentication authority and user identity service. The only service that issues JWT tokens. Every other service validates tokens issued here.

**Ports:**
- `8080` — REST (HTTP)
- `9090` — gRPC

**gRPC interface** (`user.proto`):

| RPC | Request | Response |
|---|---|---|
| `CreateUser` | `UserRequest` (name, email, password) | `UserResponse` |
| `GetUser` | `UserIdRequest` (id) | `UserResponse` |
| `DeleteUser` | `UserIdRequest` (id) | `DeleteResponse` |

**REST endpoints:**

| Method | Path | Auth | Description |
|---|---|---|---|
| `POST` | `/api/auth/register` | Public | Register a new user, returns JWT |
| `POST` | `/api/auth/login` | Public | Authenticate, returns JWT |
| `POST` | `/api/users` | Bearer token | Create user via REST |
| `GET` | `/api/users/{id}` | Bearer token | Retrieve user by ID |
| `DELETE` | `/api/users/{id}` | Bearer token | Delete user by ID |

**Database:** `grpcdb` — `users` table (id, name, email, password_hash, role).

**JWT claims:** `sub` = email, `role` = USER or ADMIN, expiration = 24 hours.

---

#### financial-service

**Role:** Manages bank accounts and financial transactions.

**Port:** `8081`

**REST endpoints:**

| Method | Path | Auth | Description |
|---|---|---|---|
| `POST` | `/api/accounts` | Bearer token | Create a new account |
| `GET` | `/api/accounts/user/{userId}` | Bearer token | List accounts for a user |
| `GET` | `/api/accounts/{id}` | Bearer token | Get account by ID |
| `POST` | `/api/transactions` | Bearer token | Create a transaction |
| `GET` | `/api/transactions/account/{accountId}` | Bearer token | List transactions for an account |

**Transaction types:** `DEPOSIT`, `WITHDRAWAL`, `TRANSFER`. Insufficient funds are guarded at the service layer.

**Database:** `financialdb` — `accounts` and `transactions` tables.

**Circuit breaker:** Resilience4j — 50% failure threshold, 5-second wait duration in `OPEN` state.

---

#### health-service

**Role:** Stores personal health records and time-series vital signs.

**Port:** `8082`

**REST endpoints:**

| Method | Path | Auth | Description |
|---|---|---|---|
| `POST` | `/api/health-records` | Bearer token | Create or update a health record |
| `GET` | `/api/health-records/user/{userId}` | Bearer token | Get health record for a user |
| `POST` | `/api/vitals` | Bearer token | Record a vital signs measurement |
| `GET` | `/api/vitals/user/{userId}` | Bearer token | List all vitals for a user |
| `GET` | `/api/vitals/user/{userId}/latest` | Bearer token | Get the most recent vital |

**Database:** `healthdb` — `health_records` (blood type, height, weight, allergies, conditions, medications) and `vitals` (heart rate, blood pressure, temperature, oxygen saturation) tables.

**Health record behaviour:** Upsert — if a record for the userId already exists it is updated, otherwise a new one is created.

---

#### social-service

**Role:** LinkedIn-style social networking — profiles, posts, and connections.

**Port:** `8083`

**REST endpoints:**

| Method | Path | Auth | Description |
|---|---|---|---|
| `POST` | `/api/profiles` | Bearer token | Create or update a social profile |
| `GET` | `/api/profiles/user/{userId}` | Bearer token | Get profile for a user |
| `POST` | `/api/posts` | Bearer token | Create a post |
| `GET` | `/api/posts/user/{userId}` | Bearer token | List posts for a user |
| `POST` | `/api/connections` | Bearer token | Send a connection request (PENDING) |
| `PUT` | `/api/connections/{id}/accept` | Bearer token | Accept a connection request (ACCEPTED) |
| `GET` | `/api/connections/user/{userId}` | Bearer token | List connections for a user |

**Connection lifecycle:** `PENDING` → `ACCEPTED`. On acceptance, the connected user's `followers_count` and the requesting user's `following_count` are incremented.

**Database:** `socialdb` — `social_profiles`, `posts`, and `connections` tables.

---

### Frontend — enterprise-ui

**Role:** React single-page application providing a unified dashboard for all four backend services.

**Tech stack:**

| Component | Version |
|---|---|
| React | 18.2 |
| React Router DOM | 6.20 |
| Axios | 1.6 |
| Build tool | Vite 5 |
| Production server | Nginx (Alpine) |

**Pages:**

| Route | Page |
|---|---|
| `/` | Dashboard — summary cards for all 4 services |
| `/users` | User management |
| `/accounts` | Bank accounts |
| `/transactions` | Financial transactions |
| `/health-records` | Health records |
| `/vitals` | Vital signs |
| `/profiles` | Social profiles |
| `/posts` | Posts feed |
| `/connections` | Social connections |

**Development proxy** (Vite): All `/api/*` requests are proxied to the appropriate backend service running on localhost. No CORS configuration is needed during local development.

**Production** (Nginx): Routes API calls to Kubernetes service names inside the cluster. All non-API routes fall back to `index.html` for client-side routing.

---

## Infrastructure

### Kubernetes Manifests

Located in `grpc-enterprise-v3/k8s/`.

Each backend service has:
- **Deployment** — with rolling-update strategy, non-root security context, read-only root filesystem, all capabilities dropped, resource requests/limits.
- **Service** — ClusterIP.
- **PodDisruptionBudget** — `minAvailable: 1`.

The enterprise-ui additionally has a VirtualService and DestinationRule for Istio.

**Resource sizing per pod:**

| | Requests | Limits |
|---|---|---|
| CPU | 250m | 1000m |
| Memory | 512Mi | 1Gi |

**Replica counts:**

| Service | Replicas |
|---|---|
| user-grpc-service | 3 |
| financial-service | 2 |
| health-service | 2 |
| social-service | 2 |
| enterprise-ui | 2 |

**Pod security (all services):**

```yaml
securityContext:
  runAsNonRoot: true
  runAsUser: 1000
  allowPrivilegeEscalation: false
  readOnlyRootFilesystem: true
  capabilities:
    drop: ["ALL"]
```

---

### Istio Service Mesh

Located in `grpc-enterprise-v3/k8s/istio/`.

| Manifest | Purpose |
|---|---|
| `namespace.yaml` | `grpc-enterprise` namespace, sidecar injection enabled |
| `peer-authentication.yaml` | STRICT mTLS for all pods |
| `destination-rules.yaml` | ISTIO_MUTUAL TLS, outlier detection (5× 5xx → eject), connection pool limits |
| `gateway.yaml` | TLS ingress port 443, HTTP→HTTPS redirect on port 80 |
| `virtualservice-grpc.yaml` | Routes user REST + gRPC traffic |
| `virtualservice-financial.yaml` | Routes `/api/accounts*` and `/api/transactions*` with 5s timeout, 3 retries, CORS |
| `virtualservice-health.yaml` | Routes health records and vitals paths |
| `virtualservice-social.yaml` | Routes profiles, posts, and connections paths |
| `authorization-policies.yaml` | Default deny-all; explicit ALLOW rules per service scoped to paths and ports |
| `request-authentication.yaml` | JWT validation at mesh level (issuer: `grpc-enterprise-v3`); DENY unauthenticated requests to protected paths |
| `rate-limiting.yaml` | Envoy local rate limiting: financial 100 req/min, health 200 req/min, social 300 req/min, user-grpc 500 req/min |
| `telemetry.yaml` | Distributed tracing and metrics |
| `sidecar.yaml` | Egress traffic scoping |

---

### Terraform (AWS)

Located in `grpc-enterprise-v3/terraform/`.

Provisions the following AWS resources:

| Resource | Configuration |
|---|---|
| VPC | 10.0.0.0/16, 3 AZs (us-east-1a/b/c), public + private subnets, NAT Gateway, VPC Flow Logs |
| EKS | v1.28, managed node group (t3.medium, min 2 / desired 3 / max 5) on private subnets |
| RDS | PostgreSQL 15.4, db.t3.medium, gp3 20 GB (autoscale to 100 GB), Multi-AZ, encrypted, deletion protection, 7-day backups, Performance Insights |
| ECR | Immutable tags, AES256 encryption, scan on push, lifecycle: keep last 20 tagged images |
| S3 + DynamoDB | Remote Terraform state with locking and encryption |

**State backend:** S3 bucket `grpc-enterprise-v3-tfstate` with DynamoDB locking.

**Sensitive variables:** `db_password` must be passed via the environment variable `TF_VAR_db_password`. Never commit passwords to version control.

---

## CI/CD Pipeline

Defined in `grpc-enterprise-v3/Jenkinsfile`.

| Stage | Description |
|---|---|
| Checkout | `checkout scm` |
| Build & Test | `mvn clean verify -B`, JUnit results published |
| Code Quality (parallel) | OWASP Dependency Check + SpotBugs static analysis |
| Docker Build (parallel) | All four backend service images built |
| Image Scan (parallel) | Trivy scan for HIGH/CRITICAL CVEs |
| Push to ECR | Tags as `build-${BUILD_NUMBER}` and `latest`, pushes to ECR |
| Terraform (main only) | Init → Validate → Plan → **manual approval gate** → Apply |
| Deploy to EKS (main only) | Updates kubeconfig, creates K8s secrets, applies manifests, waits for rollout |
| Smoke Test (main only) | `kubectl wait --for=condition=ready pod` |

**Post-build:** Removes local Docker images, cleans workspace.

> **Note:** Code quality and image scan stages use `|| true` and are currently non-blocking. See [Production Readiness Checklist](#production-readiness-checklist).

---

## Prerequisites

### Local Development

- Java 11 JDK
- Maven 3.8+
- Node.js 18+ and npm
- Docker
- PostgreSQL 14+ (or Docker)
- Protocol Buffers compiler (`protoc`) — only if you regenerate gRPC stubs

### Production / CI

- kubectl 1.28+
- Helm 3 (for Istio installation)
- istioctl 1.19+
- Terraform >= 1.5
- AWS CLI v2 configured with appropriate IAM permissions
- Jenkins with Docker and kubectl agents

---

## Local Development Setup

### 1. Start PostgreSQL

```bash
docker run -d \
  --name postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 \
  postgres:14

# Create the four databases
docker exec -it postgres psql -U postgres -c "CREATE DATABASE grpcdb;"
docker exec -it postgres psql -U postgres -c "CREATE DATABASE financialdb;"
docker exec -it postgres psql -U postgres -c "CREATE DATABASE healthdb;"
docker exec -it postgres psql -U postgres -c "CREATE DATABASE socialdb;"
```

### 2. Build All Backend Services

```bash
cd grpc-enterprise-v3

# Build all modules (skip tests for a fast build)
mvn clean package -DskipTests

# Run all tests
mvn test
```

### 3. Start Backend Services

Open four terminals:

```bash
# Terminal 1 — User gRPC service (REST :8080, gRPC :9090)
java -jar user-grpc-service/target/user-grpc-service-3.0.0.jar

# Terminal 2 — Financial service (:8081)
java -jar financial-service/target/financial-service-3.0.0.jar

# Terminal 3 — Health service (:8082)
java -jar health-service/target/health-service-3.0.0.jar

# Terminal 4 — Social service (:8083)
java -jar social-service/target/social-service-3.0.0.jar
```

Flyway will automatically run database migrations on each startup.

### 4. Start the React Frontend

```bash
cd grpc-enterprise-v3/enterprise-ui

npm install
npm run dev
# App available at http://localhost:5173
```

Vite's dev proxy forwards all `/api/*` requests to the appropriate backend service — no additional CORS configuration required.

### 5. Verify Services

Once all services are running, you can check health endpoints:

```bash
curl http://localhost:8080/actuator/health   # user-grpc-service
curl http://localhost:8081/actuator/health   # financial-service
curl http://localhost:8082/actuator/health   # health-service
curl http://localhost:8083/actuator/health   # social-service
```

---

## Docker Builds

Multi-stage Dockerfiles keep final images small (Alpine-based JRE / Nginx).

```bash
cd grpc-enterprise-v3

# Backend services
docker build -f user-grpc-service/Dockerfile  -t user-grpc-service:latest  .
docker build -f financial-service/Dockerfile  -t financial-service:latest  .
docker build -f health-service/Dockerfile     -t health-service:latest     .
docker build -f social-service/Dockerfile     -t social-service:latest     .

# Frontend
docker build -t enterprise-ui:latest enterprise-ui/
```

---

## Production Deployment (AWS EKS)

### 1. Provision Infrastructure

```bash
cd grpc-enterprise-v3/terraform

terraform init
terraform plan -var="db_password=$DB_PASSWORD"
terraform apply -var="db_password=$DB_PASSWORD"
```

### 2. Configure kubectl

```bash
aws eks update-kubeconfig \
  --name grpc-enterprise-v3-eks \
  --region us-east-1
```

### 3. Install Istio

```bash
istioctl install --set profile=production
kubectl apply -f k8s/istio/namespace.yaml
```

### 4. Create Kubernetes Secrets

```bash
kubectl create secret generic db-credentials \
  --from-literal=url=jdbc:postgresql://<RDS_ENDPOINT>:5432/ \
  --from-literal=username=postgres \
  --from-literal=password=$DB_PASSWORD \
  -n grpc-enterprise

kubectl create secret generic jwt-secret \
  --from-literal=secret=$JWT_SECRET \
  -n grpc-enterprise
```

### 5. Create Databases on RDS

```bash
psql -h <RDS_ENDPOINT> -U postgres -c "CREATE DATABASE grpcdb;"
psql -h <RDS_ENDPOINT> -U postgres -c "CREATE DATABASE financialdb;"
psql -h <RDS_ENDPOINT> -U postgres -c "CREATE DATABASE healthdb;"
psql -h <RDS_ENDPOINT> -U postgres -c "CREATE DATABASE socialdb;"
```

### 6. Push Images to ECR

```bash
AWS_ACCOUNT=$(aws sts get-caller-identity --query Account --output text)
ECR=$AWS_ACCOUNT.dkr.ecr.us-east-1.amazonaws.com

aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin $ECR

docker tag user-grpc-service:latest  $ECR/grpc-enterprise-v3:user-grpc-service-latest
docker tag financial-service:latest  $ECR/grpc-enterprise-v3:financial-service-latest
docker tag health-service:latest     $ECR/grpc-enterprise-v3:health-service-latest
docker tag social-service:latest     $ECR/grpc-enterprise-v3:social-service-latest
docker tag enterprise-ui:latest      $ECR/grpc-enterprise-v3:enterprise-ui-latest

docker push $ECR/grpc-enterprise-v3 --all-tags
```

### 7. Deploy Services

```bash
# Apply Istio configuration (order matters)
kubectl apply -f k8s/istio/peer-authentication.yaml    -n grpc-enterprise
kubectl apply -f k8s/istio/destination-rules.yaml      -n grpc-enterprise
kubectl apply -f k8s/istio/gateway.yaml                -n grpc-enterprise
kubectl apply -f k8s/istio/virtualservice-grpc.yaml    -n grpc-enterprise
kubectl apply -f k8s/istio/virtualservice-financial.yaml -n grpc-enterprise
kubectl apply -f k8s/istio/virtualservice-health.yaml  -n grpc-enterprise
kubectl apply -f k8s/istio/virtualservice-social.yaml  -n grpc-enterprise
kubectl apply -f k8s/istio/authorization-policies.yaml -n grpc-enterprise
kubectl apply -f k8s/istio/request-authentication.yaml -n grpc-enterprise
kubectl apply -f k8s/istio/rate-limiting.yaml          -n grpc-enterprise
kubectl apply -f k8s/istio/telemetry.yaml              -n grpc-enterprise
kubectl apply -f k8s/istio/sidecar.yaml                -n grpc-enterprise

# Deploy application workloads
kubectl apply -f k8s/ -n grpc-enterprise

# Wait for rollouts
kubectl rollout status deployment/user-grpc-service  -n grpc-enterprise
kubectl rollout status deployment/financial-service   -n grpc-enterprise
kubectl rollout status deployment/health-service      -n grpc-enterprise
kubectl rollout status deployment/social-service      -n grpc-enterprise
kubectl rollout status deployment/enterprise-ui       -n grpc-enterprise
```

### 8. DNS and TLS

Point your DNS records to the Istio ingress gateway's external LoadBalancer IP:

| Hostname | Service |
|---|---|
| `app.enterprise.example.com` | enterprise-ui |
| `api.enterprise.example.com` | Backend REST services |
| `grpc.enterprise.example.com` | user-grpc-service gRPC |

Configure a TLS certificate (cert-manager + Let's Encrypt recommended) and update the Istio Gateway manifest with the secret name.

---

## API Reference

Interactive OpenAPI documentation is available for each backend service when running locally:

| Service | Swagger UI |
|---|---|
| user-grpc-service | http://localhost:8080/swagger-ui.html |
| financial-service | http://localhost:8081/swagger-ui.html |
| health-service | http://localhost:8082/swagger-ui.html |
| social-service | http://localhost:8083/swagger-ui.html |

---

## Authentication

1. **Register** a user:
   ```bash
   curl -X POST http://localhost:8080/api/auth/register \
     -H "Content-Type: application/json" \
     -d '{"name":"Alice","email":"alice@example.com","password":"secret"}'
   ```
   Response: `{ "token": "<JWT>" }`

2. **Login** to get a token:
   ```bash
   curl -X POST http://localhost:8080/api/auth/login \
     -H "Content-Type: application/json" \
     -d '{"email":"alice@example.com","password":"secret"}'
   ```

3. **Use the token** in all subsequent requests:
   ```bash
   curl http://localhost:8081/api/accounts/user/1 \
     -H "Authorization: Bearer <JWT>"
   ```

Tokens expire after **24 hours**. Re-authenticate via `/api/auth/login` to obtain a new token.

---

## Observability

| Tool | Purpose |
|---|---|
| Prometheus | Scrapes Micrometer metrics from each service's `/actuator/prometheus` endpoint |
| Grafana | Pre-built dashboard at `grpc-enterprise-v3/grafana-dashboard.json` — import via Grafana UI |
| Jaeger | Distributed tracing (configured via Istio telemetry) |
| ELK Stack | Centralised log aggregation |

**SLOs (from `SRE-RUNBOOK.md`):**

| Metric | Target |
|---|---|
| Availability | 99.95% |
| P95 latency | < 150 ms |
| Error rate | < 0.5% |

**Rollback procedure:**

- **Traffic shift:** Adjust Istio VirtualService weights to route traffic to a previous stable deployment.
- **Code rollback:** Revert the offending commit; ArgoCD (if configured) will auto-sync.

---

## Security Notes

| Area | Current State | Recommended Action |
|---|---|---|
| JWT secret | Hardcoded in `application.yml` as a string | Store in Kubernetes Secret and inject via env var; rotate regularly |
| Istio JWT validation | Validates at mesh layer using `RequestAuthentication` | Ensure the JWKS URI is kept up to date |
| mTLS | STRICT mode — all pod-to-pod traffic is mutually authenticated | Do not downgrade to PERMISSIVE in production |
| Pod security | Non-root, read-only filesystem, all capabilities dropped | Enforce via PodSecurityAdmission or OPA Gatekeeper |
| CI security scans | OWASP + SpotBugs + Trivy present but non-blocking (`|| true`) | Remove `|| true` to fail the pipeline on HIGH/CRITICAL findings |
| CSRF | Disabled (appropriate for stateless JWT APIs) | No action needed |
| DB passwords | Injected from Kubernetes Secrets via `secretKeyRef` | Rotate credentials; consider AWS Secrets Manager with IRSA |
| Network | Default deny-all AuthorizationPolicy; explicit per-service ALLOW rules | Audit policies when adding new endpoints |
| Rate limiting | Envoy local rate limiting per service | Consider global rate limiting with Redis for multi-replica accuracy |

---

## Production Readiness Checklist

- [ ] Replace hardcoded JWT secret with a Kubernetes Secret (or AWS Secrets Manager)
- [ ] Make CI security scans blocking (remove `|| true` from Jenkinsfile stages)
- [ ] Configure cert-manager and Let's Encrypt for automatic TLS certificate renewal
- [ ] Enable ArgoCD for GitOps-driven continuous deployment
- [ ] Set up Prometheus alerting rules and PagerDuty/OpsGenie integration
- [ ] Import and configure the Grafana dashboard (`grafana-dashboard.json`)
- [ ] Configure Jaeger for production-grade trace sampling and retention
- [ ] Enable EKS control plane audit logs in CloudWatch
- [ ] Review and tighten IAM roles for EKS nodes (IRSA for RDS / Secrets Manager access)
- [ ] Enable RDS Enhanced Monitoring and set up CloudWatch alarms on key DB metrics
- [ ] Test PodDisruptionBudgets by draining a node during peak load
- [ ] Run a load test against rate-limiting configuration and tune thresholds
- [ ] Document and test the rollback procedure end-to-end
