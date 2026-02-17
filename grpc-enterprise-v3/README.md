# grpc-enterprise-v3

A production-grade microservices platform built with Spring Boot, gRPC, and React. The system manages user data across four domains — identity, financial, health, and social — deployed on AWS EKS with Istio service mesh.

## Architecture

```
                          ┌─────────────────────┐
                          │   Istio Gateway      │
                          │  (TLS termination)   │
                          └─────────┬────────────┘
                                    │
              ┌─────────────────────┼─────────────────────┐
              │                     │                      │
    ┌─────────▼──────┐   ┌────────▼────────┐   ┌────────▼────────┐
    │ enterprise-ui  │   │ REST Services   │   │  gRPC Service   │
    │ (React/Nginx)  │   │ financial:8081  │   │  user-grpc:9090 │
    │    port 80     │   │ health:8082     │   │  actuator:8080  │
    │                │   │ social:8083     │   │                 │
    └────────────────┘   └────────┬────────┘   └────────┬────────┘
                                  │                      │
                          ┌───────▼──────────────────────▼───────┐
                          │       PostgreSQL (RDS)               │
                          │  grpcdb │ financialdb │ healthdb │   │
                          │  socialdb                            │
                          └──────────────────────────────────────┘
```

## Technology Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 8 (source), Java 11 (runtime) |
| Framework | Spring Boot 2.7.18 |
| gRPC | io.grpc 1.58.0, Protobuf 3.24.4 |
| Frontend | React 18, Vite 5, React Router 6 |
| Database | PostgreSQL, Flyway migrations |
| Resilience | Resilience4j 1.7.1 (circuit breaker) |
| Auth | JJWT 0.11.5 (HS256) |
| Container | Docker (multi-stage, non-root) |
| Orchestration | Kubernetes (EKS 1.28) |
| Service Mesh | Istio (STRICT mTLS) |
| CI/CD | Jenkins |
| IaC | Terraform (AWS) |
| Observability | Prometheus + Grafana + Jaeger + ELK |

## Services

| Service | Type | Port | Database | Description |
|---------|------|------|----------|-------------|
| user-grpc-service | gRPC + REST | 9090 (gRPC), 8080 (HTTP) | grpcdb | User CRUD via gRPC and REST |
| financial-service | REST | 8081 | financialdb | Accounts and transactions |
| health-service | REST | 8082 | healthdb | Health records and vitals |
| social-service | REST | 8083 | socialdb | Profiles, posts, connections |
| enterprise-ui | React SPA | 80 (nginx) | - | Web dashboard for all services |

---

## Prerequisites

### Development
- **Java 8+** (JDK)
- **Maven 3.8+**
- **Node.js 18+** and npm (for the UI)
- **PostgreSQL 14+** (local or Docker)
- **Docker** (for building images)

### Production Deployment
- **AWS Account** with IAM permissions for EKS, RDS, ECR, VPC, S3
- **Terraform >= 1.5**
- **kubectl** configured for EKS
- **Istio 1.18+** installed on the cluster
- **Jenkins** with Docker, AWS CLI, kubectl, and Terraform plugins

---

## Quick Start (Local Development)

### 1. Set Up Databases

```bash
# Using Docker for PostgreSQL
docker run -d --name postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 \
  postgres:14

# Create databases
docker exec -it postgres psql -U postgres -c "CREATE DATABASE grpcdb;"
docker exec -it postgres psql -U postgres -c "CREATE DATABASE financialdb;"
docker exec -it postgres psql -U postgres -c "CREATE DATABASE healthdb;"
docker exec -it postgres psql -U postgres -c "CREATE DATABASE socialdb;"
```

### 2. Build All Services

```bash
# From project root
mvn clean package -DskipTests

# Run tests
mvn test
```

### 3. Start Backend Services

Start each service in a separate terminal:

```bash
# Terminal 1 - User gRPC Service (ports 9090 + 8080)
java -jar user-grpc-service/target/user-grpc-service-3.0.0.jar

# Terminal 2 - Financial Service (port 8081)
java -jar financial-service/target/financial-service-3.0.0.jar

# Terminal 3 - Health Service (port 8082)
java -jar health-service/target/health-service-3.0.0.jar

# Terminal 4 - Social Service (port 8083)
java -jar social-service/target/social-service-3.0.0.jar
```

### 4. Start the UI

```bash
cd enterprise-ui
npm install
npm run dev
# Opens at http://localhost:5173
```

The Vite dev server proxies API requests to the correct backend services automatically.

### 5. Verify Services

```bash
# REST - Create a user
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"name":"John Doe","email":"john@example.com"}'

# REST - Create an account
curl -X POST http://localhost:8081/api/accounts \
  -H "Content-Type: application/json" \
  -d '{"userId":1,"accountType":"CHECKING","currency":"USD"}'

# REST - Create a health record
curl -X POST http://localhost:8082/api/health-records \
  -H "Content-Type: application/json" \
  -d '{"userId":1,"bloodType":"O+","heightCm":175,"weightKg":70}'

# REST - Create a social profile
curl -X POST http://localhost:8083/api/profiles \
  -H "Content-Type: application/json" \
  -d '{"userId":1,"displayName":"johndoe","bio":"Hello world"}'

# gRPC - Using grpcurl
grpcurl -plaintext -d '{"name":"Jane","email":"jane@example.com"}' \
  localhost:9090 UserService/CreateUser

# Actuator health check
curl http://localhost:8080/actuator/health
curl http://localhost:8081/actuator/health
```

---

## API Reference

### User Service (gRPC + REST)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/users` | Create user (name, email) |
| GET | `/api/users/{id}` | Get user by ID |
| DELETE | `/api/users/{id}` | Delete user by ID |

gRPC methods: `CreateUser`, `GetUser`, `DeleteUser` on port 9090.

### Financial Service

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/accounts` | Create account (userId, accountType, currency) |
| GET | `/api/accounts/user/{userId}` | List accounts by user |
| GET | `/api/accounts/{id}` | Get account by ID |
| POST | `/api/transactions` | Create transaction (accountId, transactionType, amount, description) |
| GET | `/api/transactions/account/{accountId}` | List transactions by account |

Transaction types: `DEPOSIT`, `WITHDRAWAL`, `TRANSFER`

### Health Service

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/health-records` | Create/update health record (upsert by userId) |
| GET | `/api/health-records/user/{userId}` | Get health record by user |
| POST | `/api/vitals` | Record vitals (heartRate, BP, temperature, O2) |
| GET | `/api/vitals/user/{userId}` | List vital history |
| GET | `/api/vitals/user/{userId}/latest` | Get latest vitals |

### Social Service

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/profiles` | Create/update profile (upsert by userId) |
| GET | `/api/profiles/user/{userId}` | Get profile by user |
| POST | `/api/posts` | Create post (userId, content) |
| GET | `/api/posts/user/{userId}` | List posts by user |
| POST | `/api/connections` | Send connection request |
| PUT | `/api/connections/{id}/accept` | Accept connection |
| GET | `/api/connections/user/{userId}?status=` | List connections (filter: PENDING, ACCEPTED) |

---

## Authentication (JWT)

All REST endpoints are secured with JWT (JSON Web Token) authentication. The **user-grpc-service** acts as the auth issuer.

### Auth Endpoints (Public)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/auth/register` | Register a new user (name, email, password) |
| POST | `/api/auth/login` | Login with email and password |

Both endpoints return a JWT token in the response.

### Auth Flow

```bash
# 1. Register a new user
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name":"John Doe","email":"john@example.com","password":"secret123"}'

# Response: {"token":"eyJhbGciOiJIUzI1NiJ9...","email":"john@example.com","role":"USER"}

# 2. Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"john@example.com","password":"secret123"}'

# 3. Use the token for authenticated requests
TOKEN="eyJhbGciOiJIUzI1NiJ9..."

curl http://localhost:8081/api/accounts/user/1 \
  -H "Authorization: Bearer $TOKEN"

curl http://localhost:8082/api/health-records/user/1 \
  -H "Authorization: Bearer $TOKEN"

curl http://localhost:8083/api/profiles/user/1 \
  -H "Authorization: Bearer $TOKEN"
```

### Public vs Protected Endpoints

| Endpoint | Access |
|----------|--------|
| `POST /api/auth/register` | Public |
| `POST /api/auth/login` | Public |
| `/actuator/**` | Public |
| `/swagger-ui/**`, `/v3/api-docs/**` | Public |
| All other endpoints | Requires Bearer token |

### mTLS (Istio Service Mesh)

In Kubernetes, all inter-service communication is encrypted with **mutual TLS** via Istio:
- `PeerAuthentication` enforces STRICT mTLS across the `grpc-enterprise` namespace
- `DestinationRules` configure `ISTIO_MUTUAL` TLS for all services
- `RequestAuthentication` validates JWT tokens at the mesh level (defense-in-depth)
- `AuthorizationPolicies` control which services can communicate and enforce JWT claims

---

## API Documentation (Swagger)

All REST services include interactive API documentation via **springdoc-openapi** (OpenAPI 3.0).

### Swagger UI URLs

| Service | Swagger UI | OpenAPI JSON |
|---------|------------|--------------|
| User gRPC Service | http://localhost:8080/swagger-ui.html | http://localhost:8080/v3/api-docs |
| Financial Service | http://localhost:8081/swagger-ui.html | http://localhost:8081/v3/api-docs |
| Health Service | http://localhost:8082/swagger-ui.html | http://localhost:8082/v3/api-docs |
| Social Service | http://localhost:8083/swagger-ui.html | http://localhost:8083/v3/api-docs |

> **Note:** Swagger documents only the REST endpoints. The gRPC interface on port 9090 is not included in Swagger — use gRPC reflection or `.proto` files for gRPC documentation.

### Using JWT in Swagger UI

1. Call `POST /api/auth/login` (or `/register`) to get a token
2. Click the **Authorize** button (lock icon) in Swagger UI
3. Enter the token value (without "Bearer " prefix)
4. All subsequent requests from Swagger UI will include the JWT

## Building Docker Images

```bash
# Build all service images
docker build -t user-grpc-service:latest -f user-grpc-service/Dockerfile .
docker build -t financial-service:latest -f financial-service/Dockerfile .
docker build -t health-service:latest -f health-service/Dockerfile .
docker build -t social-service:latest -f social-service/Dockerfile .

# Build UI image
docker build -t enterprise-ui:latest enterprise-ui/

# Verify images
docker images | grep -E "service|enterprise"
```

All backend Dockerfiles use multi-stage builds:
- **Build stage**: `maven:3.9-eclipse-temurin-11` compiles the module with `mvn -pl <module> -am`
- **Runtime stage**: `eclipse-temurin:11-jre-alpine` (~85MB) runs the JAR

The UI Dockerfile uses:
- **Build stage**: `node:18-alpine` runs `npm run build`
- **Runtime stage**: `nginx:alpine` serves the static files with API reverse proxy

---

## Infrastructure Provisioning (Terraform)

### AWS Resources Created

| Resource | Details |
|----------|---------|
| VPC | 10.0.0.0/16, 3 AZs (us-east-1a/b/c), public + private subnets |
| EKS Cluster | v1.28, managed node group (t3.medium, 2-5 nodes) |
| RDS | PostgreSQL on db.t3.medium, private subnet |
| ECR | 4 repositories (one per service), 20-image retention |
| Security Groups | EKS cluster, node, and RDS with least-privilege rules |
| S3 + DynamoDB | Terraform state backend with encryption and locking |

### Provision Infrastructure

```bash
cd terraform

# Initialize Terraform
terraform init

# Review the plan
terraform plan -out=tfplan

# Apply infrastructure
terraform apply tfplan

# Save outputs for deployment
terraform output -json > ../terraform-outputs.json
```

### Required Variables (`terraform.tfvars`)

```hcl
project_name    = "grpc-enterprise-v3"
environment     = "production"
aws_region      = "us-east-1"
db_password     = "<secure-password>"    # Use AWS Secrets Manager in production
eks_node_type   = "t3.medium"
eks_desired_nodes = 3
```

---

## Kubernetes Deployment

### 1. Configure kubectl

```bash
aws eks update-kubeconfig --name grpc-enterprise-v3-cluster --region us-east-1
```

### 2. Create Namespace and Secrets

```bash
# Create namespace with Istio injection
kubectl apply -f k8s/istio/namespace.yaml

# Create database secrets for each service
kubectl create secret generic grpc-enterprise-db-secret \
  -n grpc-enterprise \
  --from-literal=username=grpcadmin \
  --from-literal=password=<db-password>

kubectl create secret generic financial-service-db-secret \
  -n grpc-enterprise \
  --from-literal=username=grpcadmin \
  --from-literal=password=<db-password>

kubectl create secret generic health-service-db-secret \
  -n grpc-enterprise \
  --from-literal=username=grpcadmin \
  --from-literal=password=<db-password>

kubectl create secret generic social-service-db-secret \
  -n grpc-enterprise \
  --from-literal=username=grpcadmin \
  --from-literal=password=<db-password>
```

### 3. Create Databases on RDS

```bash
RDS_ENDPOINT=$(terraform -chdir=terraform output -raw rds_endpoint)

# Connect and create databases
psql -h $RDS_ENDPOINT -U grpcadmin -d postgres -c "CREATE DATABASE grpcdb;"
psql -h $RDS_ENDPOINT -U grpcadmin -d postgres -c "CREATE DATABASE financialdb;"
psql -h $RDS_ENDPOINT -U grpcadmin -d postgres -c "CREATE DATABASE healthdb;"
psql -h $RDS_ENDPOINT -U grpcadmin -d postgres -c "CREATE DATABASE socialdb;"
```

### 4. Push Docker Images to ECR

```bash
AWS_ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
ECR_REGISTRY="${AWS_ACCOUNT_ID}.dkr.ecr.us-east-1.amazonaws.com"

# Authenticate Docker to ECR
aws ecr get-login-password --region us-east-1 | \
  docker login --username AWS --password-stdin $ECR_REGISTRY

# Tag and push each image
for service in user-grpc-service financial-service health-service social-service enterprise-ui; do
  docker tag ${service}:latest ${ECR_REGISTRY}/${service}:latest
  docker push ${ECR_REGISTRY}/${service}:latest
done
```

### 5. Update Image References in Manifests

Replace image references in k8s manifests with ECR URIs:

```bash
# Update deployment manifests to use ECR images
sed -i "s|image: user-grpc-service:latest|image: ${ECR_REGISTRY}/user-grpc-service:latest|" k8s/deployment.yaml
sed -i "s|image: financial-service:latest|image: ${ECR_REGISTRY}/financial-service:latest|" k8s/financial-service.yaml
sed -i "s|image: health-service:latest|image: ${ECR_REGISTRY}/health-service:latest|" k8s/health-service.yaml
sed -i "s|image: social-service:latest|image: ${ECR_REGISTRY}/social-service:latest|" k8s/social-service.yaml
sed -i "s|image: enterprise-ui:latest|image: ${ECR_REGISTRY}/enterprise-ui:latest|" enterprise-ui/k8s/enterprise-ui.yaml

# Update database URLs to point to RDS
RDS_ENDPOINT=$(terraform -chdir=terraform output -raw rds_endpoint)
sed -i "s|postgres-service:5432|${RDS_ENDPOINT}:5432|g" k8s/*.yaml
```

### 6. Deploy Services

```bash
# Deploy backend services
kubectl apply -f k8s/ -n grpc-enterprise

# Deploy UI
kubectl apply -f enterprise-ui/k8s/ -n grpc-enterprise

# Verify deployments
kubectl get pods -n grpc-enterprise
kubectl get svc -n grpc-enterprise
```

### 7. Deploy Istio Configuration

```bash
# Apply Istio resources (order matters)
kubectl apply -f k8s/istio/namespace.yaml
kubectl apply -f k8s/istio/peer-authentication.yaml
kubectl apply -f k8s/istio/destination-rules.yaml
kubectl apply -f k8s/istio/gateway.yaml
kubectl apply -f k8s/istio/virtualservice-grpc.yaml
kubectl apply -f k8s/istio/virtualservice-financial.yaml
kubectl apply -f k8s/istio/virtualservice-health.yaml
kubectl apply -f k8s/istio/virtualservice-social.yaml
kubectl apply -f k8s/istio/authorization-policies.yaml
kubectl apply -f k8s/istio/request-authentication.yaml
kubectl apply -f k8s/istio/rate-limiting.yaml
kubectl apply -f k8s/istio/telemetry.yaml
kubectl apply -f k8s/istio/sidecar.yaml

# Verify Istio configuration
istioctl analyze -n grpc-enterprise
istioctl proxy-status
```

### 8. Create TLS Certificate

```bash
# Option A: Use cert-manager with Let's Encrypt
kubectl apply -f https://github.com/cert-manager/cert-manager/releases/latest/download/cert-manager.yaml

# Option B: Manual TLS secret
kubectl create secret tls enterprise-tls-credential \
  -n istio-system \
  --cert=path/to/tls.crt \
  --key=path/to/tls.key
```

### 9. Configure DNS

Point your DNS records to the Istio ingress gateway's external IP:

```bash
INGRESS_IP=$(kubectl get svc istio-ingressgateway -n istio-system -o jsonpath='{.status.loadBalancer.ingress[0].hostname}')

# Create DNS records:
# api.enterprise.example.com   -> $INGRESS_IP  (REST services)
# grpc.enterprise.example.com  -> $INGRESS_IP  (gRPC service)
# app.enterprise.example.com   -> $INGRESS_IP  (UI)
```

---

## Production Readiness Checklist

### Security

- [ ] **Externalize secrets** — Move database passwords to AWS Secrets Manager or Kubernetes External Secrets Operator. Never hardcode credentials.
- [ ] **Rotate JWT secret** — The JWT secret is in `application.yml`. Move to environment variable or Kubernetes secret for production.
- [x] **Enable RBAC** — All REST services secured with Spring Security + JWT authentication. Auth endpoints at `/api/auth/register` and `/api/auth/login`.
- [ ] **Network policies** — Add Kubernetes NetworkPolicies as defense-in-depth alongside Istio authorization policies.
- [ ] **Pod security standards** — Enforce `restricted` pod security standard at namespace level.
- [ ] **Image scanning** — Trivy is in the pipeline (`|| true`). Make it blocking: remove `|| true` to fail builds on HIGH/CRITICAL CVEs.
- [ ] **HTTPS only** — The Istio gateway already redirects HTTP to HTTPS. Verify with `curl -I http://api.enterprise.example.com`.
- [ ] **Audit logging** — Enable EKS audit logs and CloudTrail for infrastructure changes.

### Reliability

- [ ] **Database backups** — Enable RDS automated backups with 7+ day retention and point-in-time recovery.
- [ ] **Multi-AZ RDS** — Upgrade from single-AZ to Multi-AZ deployment for database failover.
- [ ] **Separate databases per service** — Currently each service has its own database. Ensure no cross-database queries.
- [ ] **Health check tuning** — Adjust `initialDelaySeconds` per service based on actual startup time.
- [ ] **Resource limits** — Current: 250m-1 CPU, 512Mi-1Gi memory. Profile under load and adjust.
- [ ] **HPA (Horizontal Pod Autoscaler)** — Add HPA for each service based on CPU/memory or custom metrics.
- [ ] **PDB enforcement** — PodDisruptionBudgets are already configured (minAvailable: 1-2).

### Observability

- [ ] **Prometheus** — Deploy Prometheus Operator and configure ServiceMonitors for each service.
- [ ] **Grafana dashboards** — Import `grafana-dashboard.json` included in the repo.
- [ ] **Jaeger** — Deploy Jaeger for distributed tracing. Istio telemetry is configured for 100% sampling — reduce to 1-5% for production.
- [ ] **Log aggregation** — Deploy ELK/EFK stack or use AWS CloudWatch Container Insights. Each service has structured JSON logging in the `prod` Spring profile.
- [ ] **Alerting** — Configure Prometheus alerting rules for:
  - Service availability < 99.95%
  - P95 latency > 150ms
  - Error rate > 0.5%
  - Circuit breaker open events
  - Pod restart count

### Performance

- [ ] **Connection pooling** — Configure HikariCP pool size per service (`spring.datasource.hikari.maximum-pool-size`).
- [ ] **JVM tuning** — Containers use `-XX:MaxRAMPercentage=75.0`. Consider adding GC logging and tuning for G1GC.
- [ ] **Database indexing** — Review Flyway migration SQL for missing indexes on frequently queried columns.
- [ ] **Rate limiting tuning** — Current Istio limits (100-500 req/min per pod) are starting points. Adjust based on load testing results.
- [ ] **Load testing** — Run load tests with k6, Gatling, or Locust against all endpoints before go-live.
- [ ] **CDN** — Put the enterprise-ui behind CloudFront for static asset caching.

### CI/CD

- [ ] **Make security scans blocking** — Remove `|| true` from Trivy, OWASP, and SpotBugs stages in Jenkinsfile.
- [ ] **Add integration tests** — Each service has unit tests. Add integration tests that exercise the full stack.
- [ ] **GitOps** — Consider ArgoCD (referenced in SRE-RUNBOOK.md) for declarative deployments instead of `kubectl apply` in Jenkins.
- [ ] **Canary deployments** — Use Istio VirtualService traffic splitting for gradual rollouts (e.g., 90/10 weight between v1/v2).
- [ ] **Rollback automation** — The SRE runbook documents manual rollback. Automate with Argo Rollouts or Flagger.

### Operations

- [ ] **SRE Runbook** — `SRE-RUNBOOK.md` is included. Review and update with team-specific procedures.
- [ ] **On-call rotation** — Define escalation paths and PagerDuty/OpsGenie integration.
- [ ] **Disaster recovery** — Document RTO/RPO. Test RDS restoration and EKS cluster recreation.
- [ ] **Cost optimization** — Use Spot instances for non-critical node groups. Right-size RDS instance.
- [ ] **Compliance** — If handling PII (health data), ensure HIPAA compliance (encryption at rest, audit trails, access controls).

---

## Project Structure

```
grpc-enterprise-v3/
├── pom.xml                         # Parent POM (multi-module aggregator)
├── Jenkinsfile                     # CI/CD pipeline
├── SRE-RUNBOOK.md                  # Operational runbook
├── grafana-dashboard.json          # Grafana dashboard config
│
├── user-grpc-service/              # gRPC + REST user service
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/main/
│       ├── proto/user.proto        # Protobuf definitions
│       ├── java/com/enterprise/
│       │   ├── GrpcApplication.java
│       │   ├── controller/         # REST endpoints
│       │   ├── grpc/               # gRPC service implementation
│       │   ├── service/            # Business logic
│       │   ├── entity/             # JPA entities
│       │   ├── repository/         # Spring Data repositories
│       │   ├── dto/                # Request/response DTOs
│       │   └── security/           # JWT utility
│       └── resources/
│           ├── application.yml
│           └── db/migration/       # Flyway SQL migrations
│
├── financial-service/              # Accounts & transactions REST service
├── health-service/                 # Health records & vitals REST service
├── social-service/                 # Profiles, posts & connections REST service
│   (each follows the same structure as user-grpc-service)
│
├── enterprise-ui/                  # React SPA
│   ├── package.json
│   ├── Dockerfile
│   ├── nginx.conf                  # Production reverse proxy config
│   ├── vite.config.js              # Dev server proxy config
│   ├── src/
│   │   ├── api/                    # Axios API clients
│   │   ├── components/             # Layout, Navbar
│   │   └── pages/                  # 9 page components
│   └── k8s/enterprise-ui.yaml     # K8s + Istio manifests
│
├── k8s/                            # Kubernetes manifests
│   ├── deployment.yaml             # user-grpc-service (3 replicas)
│   ├── financial-service.yaml      # (2 replicas)
│   ├── health-service.yaml         # (2 replicas)
│   ├── social-service.yaml         # (2 replicas)
│   └── istio/                      # Istio service mesh config
│       ├── namespace.yaml          # Namespace with sidecar injection
│       ├── gateway.yaml            # TLS ingress gateway
│       ├── peer-authentication.yaml # STRICT mTLS
│       ├── destination-rules.yaml  # Load balancing, outlier detection
│       ├── authorization-policies.yaml # Access control
│       ├── request-authentication.yaml # JWT validation at mesh level
│       ├── rate-limiting.yaml      # Per-service rate limits
│       ├── telemetry.yaml          # Tracing + metrics config
│       ├── sidecar.yaml            # Egress scoping
│       └── virtualservice-*.yaml   # Per-service routing
│
└── terraform/                      # AWS infrastructure as code
    ├── main.tf                     # Provider, backend
    ├── vpc.tf                      # VPC, subnets, NAT
    ├── eks.tf                      # EKS cluster + node group
    ├── rds.tf                      # PostgreSQL RDS
    ├── ecr.tf                      # Container registries
    ├── security.tf                 # Security groups
    ├── variables.tf                # Input variables
    ├── outputs.tf                  # Exported values
    └── terraform.tfvars            # Environment values
```

---

## SLO Targets

| Metric | Target |
|--------|--------|
| Availability | 99.95% |
| P95 Latency | < 150ms |
| Error Rate | < 0.5% |

See `SRE-RUNBOOK.md` for incident response, rollback procedures, and monitoring details.

---

## License

Proprietary. All rights reserved.
