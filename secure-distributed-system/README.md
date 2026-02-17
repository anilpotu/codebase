# Secure Distributed Systems Architecture with Spring Boot

A production-ready microservices architecture demonstrating comprehensive security patterns, service discovery, API gateway, structured logging, observability, and cloud-native deployment using Spring Boot 2.7.18, Spring Cloud, Kubernetes, Istio, and Terraform.

## Architecture Overview

```
                         Internet
                            |
                   [ Istio Gateway ]
                   (HTTPS + mTLS)
                            |
                   [ VirtualService ]
                            |
              +-------------+-------------+
              |                           |
     [ API Gateway :8000 ]        [ Istio Sidecar ]
     - JWT Validation              - Rate Limiting (200 req/min)
     - CORS                        - Circuit Breaking
     - Request Routing             - mTLS Enforcement
              |
    +---------+---------+---------+
    |         |         |         |
[Auth Svc] [User Svc] [Order Svc] [Product Svc]
  :8080      :8081      :8082       :8083
    |         |         |           |
    +----+----+----+----+----+------+
         |              |
  [Eureka Server]  [Config Server]
     :8761            :8888
                        |
                  [config-repo/]

Infrastructure: Redis :6379 | H2 (dev) | PostgreSQL (prod via Terraform)
```

## Project Structure

```
secure-distributed-system/
├── pom.xml                    # Parent POM with dependency management
├── docker-compose.yml         # Complete stack orchestration
├── Dockerfile.template        # Base Dockerfile for services
├── Jenkinsfile                # CI/CD pipeline (build, test, deploy)
│
├── common-lib/               # Shared security utilities
│   ├── security/             # JWT utilities (JwtTokenProvider with @Slf4j)
│   ├── dto/                  # Common DTOs (ApiResponse<T>, ErrorResponse)
│   └── exception/            # Global exception handlers
│
├── config-server/            # Centralized configuration (port 8888)
├── config-repo/              # Git-backed configuration repository
│   ├── application.yml       # Common config (actuator, logging, eureka)
│   ├── application-dev.yml   # Dev profile overrides
│   ├── application-prod.yml  # Prod profile overrides
│   ├── auth-service.yml      # Auth service config
│   ├── user-service.yml      # User service config
│   ├── order-service.yml     # Order service config
│   └── product-service.yml   # Product service config
│
├── eureka-server/            # Service registry (port 8761)
├── api-gateway/              # Edge service (port 8000)
├── auth-service/             # Authentication & JWT (port 8080)
├── user-service/             # User profile management (port 8081)
├── order-service/            # Order management (port 8082)
├── product-service/          # Product catalog (port 8083)
│
├── k8s/                      # Kubernetes manifests
│   ├── namespace.yaml        # Namespace with Istio injection
│   ├── secrets.yaml          # Application secrets
│   ├── redis.yaml            # Redis deployment
│   ├── config-server.yaml    # Config server deployment
│   ├── eureka-server.yaml    # Eureka deployment
│   ├── api-gateway.yaml      # Gateway deployment (2 replicas)
│   ├── auth-service.yaml     # Auth deployment (2 replicas)
│   ├── user-service.yaml     # User deployment (2 replicas)
│   ├── order-service.yaml    # Order deployment (2 replicas)
│   ├── product-service.yaml  # Product deployment (2 replicas)
│   └── istio/                # Istio service mesh configuration
│       ├── gateway.yaml              # HTTPS ingress gateway
│       ├── virtualservice-gateway.yaml # Traffic routing
│       ├── destination-rules.yaml    # Connection pools & outlier detection
│       ├── peer-authentication.yaml  # STRICT mTLS
│       ├── authorization-policies.yaml # Zero-trust access control
│       ├── rate-limiting.yaml        # EnvoyFilter rate limits
│       └── telemetry.yaml            # Tracing & metrics
│
└── terraform/                # AWS infrastructure as code
    ├── main.tf               # Provider & backend config
    ├── variables.tf          # Input variables
    ├── vpc.tf                # VPC, subnets, NAT gateway
    ├── eks.tf                # EKS cluster & node group
    ├── rds.tf                # PostgreSQL RDS
    ├── elasticache.tf        # Redis ElastiCache
    ├── ecr.tf                # Container registries
    ├── security.tf           # Security groups
    └── outputs.tf            # Exported values
```

## Technology Stack

| Category | Technology | Version |
|----------|-----------|---------|
| **Core** | Java | 1.8 |
| | Spring Boot | 2.7.18 |
| | Spring Cloud | 2021.0.8 |
| **Security** | Spring Security + OAuth2 Resource Server | |
| | JJWT (HS512) | 0.11.5 |
| | BCrypt | strength 12 |
| **Service Discovery** | Netflix Eureka | |
| **Configuration** | Spring Cloud Config | |
| **API Gateway** | Spring Cloud Gateway | |
| **Data** | Spring Data JPA, H2 (dev), PostgreSQL (prod) | |
| | Flyway | 9.16.3 |
| **Caching** | Redis | 7.x |
| **Communication** | Spring Cloud OpenFeign | |
| **Logging** | SLF4J + Logback (structured JSON in prod) | |
| **Observability** | Spring Boot Actuator, Micrometer, Prometheus | |
| **Service Mesh** | Istio (mTLS, rate limiting, tracing) | |
| **Container Orchestration** | Kubernetes | 1.28 |
| **Infrastructure** | Terraform (AWS: EKS, RDS, ElastiCache, ECR) | ~5.0 |
| **CI/CD** | Jenkins (Declarative Pipeline, Kubernetes agent) | |
| **Utilities** | Lombok, MapStruct, Bean Validation | |

## Security Features

### Authentication & Authorization
- **JWT-based Authentication**: Stateless authentication with HS512 signing
- **Access Tokens**: 15-minute expiry
- **Refresh Tokens**: 7-day expiry, stored in database
- **Role-Based Access Control (RBAC)**: ROLE_USER, ROLE_ADMIN
- **Method-Level Security**: @PreAuthorize annotations
- **Password Encryption**: BCrypt (strength 12)

### API Gateway Security
- **JWT Validation at Edge**: Validates all incoming tokens
- **Token Propagation**: Forwards authenticated user context to services
- **Rate Limiting**: Redis-backed rate limiting per IP
- **CORS Configuration**: Configurable cross-origin policies
- **Security Headers**: HSTS, X-Frame-Options, X-Content-Type-Options

### Service-to-Service Security
- **OAuth2 Resource Server**: JWT validation in each service
- **Token Propagation**: Automatic JWT forwarding via Feign interceptors
- **Service Discovery Security**: Basic auth for Eureka

### Istio Service Mesh Security
- **STRICT mTLS**: All inter-service traffic is encrypted (PeerAuthentication)
- **Zero-Trust Authorization**: Default-deny with per-service ALLOW policies
- **Rate Limiting**: 200 req/min per pod via EnvoyFilter on api-gateway
- **Ingress Gateway**: HTTPS termination with TLS certificates

## Logging & Observability

### Structured Logging

All services use `@Slf4j` with Logback for structured logging:

- **Development**: Human-readable console output with traceId support
  ```
  2026-02-17 10:30:00.123 [http-nio-8080-exec-1] [abc123] INFO  AuthController - POST /auth/login - username=testuser
  ```
- **Production**: JSON-structured logging for log aggregation
  ```json
  {"timestamp":"2026-02-17T10:30:00.123+0000","level":"INFO","service":"auth-service","thread":"http-nio-8080-exec-1","traceId":"abc123","logger":"AuthController","message":"POST /auth/login - username=testuser"}
  ```
- **Rolling File**: 50MB max file size, 30-day retention, 1GB total cap

Services with `@Slf4j` logging:
- `auth-service`: AuthController, AuthService, TokenService
- `user-service`: UserController, UserService, SecurityService
- `order-service`: OrderController, OrderService
- `product-service`: ProductController, ProductService
- `common-lib`: JwtTokenProvider
- `api-gateway`: JwtAuthenticationFilter

### Spring Boot Actuator

All services expose the following actuator endpoints:

| Endpoint | Description |
|----------|-------------|
| `/actuator/health` | Health status with component details and K8s probes |
| `/actuator/info` | Application info (env + build) |
| `/actuator/prometheus` | Prometheus metrics scraping |
| `/actuator/metrics` | Micrometer metrics |
| `/actuator/loggers` | Runtime log level management |
| `/actuator/env` | Environment properties |
| `/actuator/circuitbreakers` | Resilience4j circuit breaker status |

Configuration in `config-repo/application.yml`:
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus,metrics,env,loggers,circuitbreakers
  endpoint:
    health:
      show-details: always
      show-components: always
      probes:
        enabled: true
  health:
    db:
      enabled: true
    diskspace:
      enabled: true
    redis:
      enabled: true
```

## Getting Started

### Prerequisites

- **Java**: JDK 1.8 or higher
- **Maven**: 3.6.3 or higher
- **Docker** (optional): For containerized deployment
- **kubectl + Istio** (optional): For Kubernetes deployment
- **Terraform** (optional): For AWS infrastructure provisioning

### Quick Start (Local Development)

#### 1. Clone and Build

```bash
cd secure-distributed-system
mvn clean install
```

#### 2. Start Infrastructure Services

```bash
# Terminal 1: Config Server
cd config-server && mvn spring-boot:run

# Terminal 2: Eureka Server (wait for config server)
cd eureka-server && mvn spring-boot:run

# Terminal 3: Redis (requires Docker)
docker run -d --name redis -p 6379:6379 redis:7-alpine
```

#### 3. Start Application Services

```bash
# Terminal 4: Auth Service
cd auth-service && mvn spring-boot:run

# Terminal 5: API Gateway
cd api-gateway && mvn spring-boot:run

# Terminal 6-8: Business services
cd user-service && mvn spring-boot:run
cd order-service && mvn spring-boot:run
cd product-service && mvn spring-boot:run
```

### Docker Deployment

```bash
# Build all services
mvn clean package

# Start complete stack
docker-compose up -d

# View logs
docker-compose logs -f

# Stop all services
docker-compose down
```

## Kubernetes Deployment

### Prerequisites

- A Kubernetes cluster (EKS, GKE, minikube, etc.)
- `kubectl` configured for the cluster
- Istio installed (`istioctl install --set profile=demo`)
- Docker images built and pushed to a registry

### Build & Push Docker Images

```bash
# Build all JARs
mvn clean package -DskipTests

# Build and push Docker images (update registry as needed)
for svc in config-server eureka-server api-gateway auth-service user-service order-service product-service; do
  docker build -t secure-distributed/$svc:latest ./$svc
  docker tag secure-distributed/$svc:latest <YOUR_REGISTRY>/secure-distributed/$svc:latest
  docker push <YOUR_REGISTRY>/secure-distributed/$svc:latest
done
```

### Deploy to Kubernetes

```bash
# 1. Create namespace with Istio injection
kubectl apply -f k8s/namespace.yaml

# 2. Create secrets
kubectl apply -f k8s/secrets.yaml

# 3. Deploy infrastructure (order matters)
kubectl apply -f k8s/redis.yaml
kubectl apply -f k8s/config-server.yaml

# Wait for config-server to be ready
kubectl -n secure-distributed wait --for=condition=ready pod -l app=config-server --timeout=120s

kubectl apply -f k8s/eureka-server.yaml

# Wait for eureka-server to be ready
kubectl -n secure-distributed wait --for=condition=ready pod -l app=eureka-server --timeout=120s

# 4. Deploy business services (initContainers handle dependency ordering)
kubectl apply -f k8s/api-gateway.yaml
kubectl apply -f k8s/auth-service.yaml
kubectl apply -f k8s/user-service.yaml
kubectl apply -f k8s/order-service.yaml
kubectl apply -f k8s/product-service.yaml

# 5. Apply Istio configuration
kubectl apply -f k8s/istio/

# 6. Verify deployment
kubectl -n secure-distributed get pods
kubectl -n secure-distributed get svc
```

### Kubernetes Resources Per Service

| Service | Replicas | CPU Request/Limit | Memory Request/Limit | PDB |
|---------|----------|-------------------|----------------------|-----|
| config-server | 1 | 200m / 500m | 256Mi / 512Mi | - |
| eureka-server | 1 | 200m / 500m | 256Mi / 512Mi | - |
| api-gateway | 2 | 200m / 500m | 256Mi / 512Mi | minAvailable: 1 |
| auth-service | 2 | 200m / 500m | 256Mi / 512Mi | minAvailable: 1 |
| user-service | 2 | 200m / 500m | 256Mi / 512Mi | minAvailable: 1 |
| order-service | 2 | 200m / 500m | 256Mi / 512Mi | minAvailable: 1 |
| product-service | 2 | 200m / 500m | 256Mi / 512Mi | minAvailable: 1 |
| redis | 1 | 50m / 100m | 64Mi / 128Mi | - |

## Istio Service Mesh

### Features

| Feature | Configuration | Details |
|---------|--------------|---------|
| **mTLS** | `peer-authentication.yaml` | STRICT mode - all traffic encrypted |
| **Traffic Routing** | `virtualservice-gateway.yaml` | /api/* routes to api-gateway with 30s timeout, 3 retries |
| **Circuit Breaking** | `destination-rules.yaml` | 5 consecutive 5xx errors triggers ejection for 30s |
| **Connection Pools** | `destination-rules.yaml` | 100 TCP connections, 1000 HTTP/2 requests max |
| **Rate Limiting** | `rate-limiting.yaml` | 200 req/min per pod on api-gateway |
| **Authorization** | `authorization-policies.yaml` | Default-deny + per-service ALLOW rules |
| **Ingress** | `gateway.yaml` | HTTPS on 443, HTTP->HTTPS redirect on 80 |
| **Observability** | `telemetry.yaml` | 100% trace sampling (dev), Prometheus metrics |

### Authorization Policies

```
deny-all (default) -> Only explicitly allowed traffic passes:

Internet -> Istio Ingress Gateway -> api-gateway
api-gateway -> auth-service, user-service, order-service, product-service
All services -> config-server, eureka-server, redis
All pods -> /actuator/health (for K8s probes)
```

## Terraform Infrastructure (AWS)

### Resources Provisioned

| Resource | File | Details |
|----------|------|---------|
| **VPC** | `vpc.tf` | 10.0.0.0/16, 3 AZs, public + private subnets, NAT Gateway |
| **EKS** | `eks.tf` | Kubernetes 1.28, managed node group (t3.medium), vpc-cni/coredns/kube-proxy add-ons |
| **RDS** | `rds.tf` | PostgreSQL 15, 20GB gp3, private subnets |
| **ElastiCache** | `elasticache.tf` | Redis 7.0, transit encryption |
| **ECR** | `ecr.tf` | 7 repositories with lifecycle policies (keep last 10 images) |
| **Security Groups** | `security.tf` | EKS (443), RDS (5432), Redis (6379) - VPC-scoped |

### Provision Infrastructure

```bash
cd terraform

# Initialize Terraform
terraform init

# Review the plan
terraform plan -var="environment=dev"

# Apply infrastructure
terraform apply -var="environment=dev"

# Get outputs
terraform output
```

### Configuration Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `aws_region` | us-east-1 | AWS region |
| `environment` | dev | Environment name |
| `cluster_name` | secure-distributed | EKS cluster name |
| `vpc_cidr` | 10.0.0.0/16 | VPC CIDR block |
| `node_instance_type` | t3.medium | EKS node instance type |
| `node_desired_size` | 3 | Desired node count |
| `node_min_size` | 2 | Minimum node count |
| `node_max_size` | 5 | Maximum node count |
| `db_instance_class` | db.t3.micro | RDS instance class |
| `redis_node_type` | cache.t3.micro | ElastiCache node type |

### Post-Terraform Setup

```bash
# Configure kubectl
aws eks update-kubeconfig --name secure-distributed --region us-east-1

# Install Istio
istioctl install --set profile=demo

# Login to ECR
aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin <ACCOUNT_ID>.dkr.ecr.us-east-1.amazonaws.com

# Push images to ECR
for svc in config-server eureka-server api-gateway auth-service user-service order-service product-service; do
  docker tag secure-distributed/$svc:latest <ACCOUNT_ID>.dkr.ecr.us-east-1.amazonaws.com/secure-distributed/$svc:latest
  docker push <ACCOUNT_ID>.dkr.ecr.us-east-1.amazonaws.com/secure-distributed/$svc:latest
done

# Deploy to K8s (update image references in manifests to ECR URLs)
kubectl apply -f k8s/
kubectl apply -f k8s/istio/
```

## CI/CD Pipeline (Jenkins)

### Pipeline Overview

```
Git Push → Jenkins → Build (Maven) → Unit Tests → Security Scan → Docker Build
                                                                       ↓
Slack ← Verify Deployment ← Deploy to K8s ← Approval (prod) ← Push to ECR
```

The `Jenkinsfile` defines a declarative pipeline that builds, tests, containerizes, and deploys all microservices to Kubernetes via AWS ECR. It runs on a Kubernetes pod agent with four containers: Maven, Docker, kubectl, and AWS CLI.

### Pipeline Parameters

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `DEPLOY_ENV` | Choice | dev | Target environment: dev, staging, prod |
| `SERVICES` | String | all | Comma-separated service names or "all" |
| `SKIP_TESTS` | Boolean | false | Skip unit tests during build |
| `DOCKER_TAG` | String | BUILD_NUMBER | Docker image tag override |

### Pipeline Stages

| Stage | Description | Condition |
|-------|-------------|-----------|
| **Checkout** | SCM checkout, set git commit/branch info | Always |
| **Build** | `mvn clean package` (parallel module build) | Always |
| **Unit Tests** | `mvn test` + JUnit/JaCoCo reports | Unless SKIP_TESTS |
| **Code Quality** | SonarQube analysis | If SONARQUBE_ENABLED=true |
| **Security Scan** | OWASP dependency-check (CVSS ≥ 9 fails build) | prod only |
| **Build Docker Images** | Parallel Docker builds for all selected services | Always |
| **Push to ECR** | Tag and push images to AWS ECR | Always |
| **Production Approval** | Manual approval gate (admin, devops) | prod only |
| **Deploy to Kubernetes** | Apply K8s manifests in dependency order | Always |
| **Verify Deployment** | `kubectl rollout status` for each deployment | Always |
| **Integration Tests** | Health checks on all service endpoints | Always |
| **Cleanup** | Remove local Docker images | Always |

### Deployment Order

The pipeline deploys services in dependency order to ensure availability:

1. **Namespace + Secrets + Redis** — Foundational resources
2. **Config Server** — Wait for ready (180s timeout)
3. **Eureka Server** — Wait for ready (180s timeout)
4. **Business Services** — api-gateway, auth-service, user-service, order-service, product-service (parallel)
5. **Istio Configuration** — Service mesh policies

### Jenkins Prerequisites

#### Required Plugins
- Kubernetes Plugin (pod-based agents)
- Pipeline Plugin (declarative pipeline)
- Credentials Binding Plugin
- JUnit Plugin (test reports)
- JaCoCo Plugin (code coverage)
- OWASP Dependency-Check Plugin
- AnsiColor Plugin
- Timestamps Plugin

#### Required Credentials

| Credential ID | Type | Description |
|---------------|------|-------------|
| `aws-credentials` | AWS Credentials | AWS access key for ECR/EKS |
| `kubeconfig-dev` | Secret File | Kubeconfig for dev cluster |
| `kubeconfig-staging` | Secret File | Kubeconfig for staging cluster |
| `kubeconfig-prod` | Secret File | Kubeconfig for production cluster |

#### Kubernetes Resources
- PersistentVolumeClaim `maven-repo-cache` for Maven dependency caching
- Docker socket access (DinD or host mount)

### Setting Up the Jenkins Job

1. **Create Pipeline Job**: New Item → Pipeline → "secure-distributed-system"
2. **Configure SCM**: Pipeline → Pipeline script from SCM → Git → Repository URL
3. **Add Credentials**: Manage Jenkins → Credentials → Add AWS and kubeconfig credentials
4. **Configure Maven Cache**: Create PVC `maven-repo-cache` in Jenkins namespace
5. **First Build**: Run with default parameters to populate cache

### Triggering Deployments

```bash
# Deploy all services to dev
# Parameters: DEPLOY_ENV=dev, SERVICES=all

# Deploy specific services to staging
# Parameters: DEPLOY_ENV=staging, SERVICES=auth-service,user-service

# Deploy to production (requires approval)
# Parameters: DEPLOY_ENV=prod, SERVICES=all
```

### Post-Build Actions

- **Always**: Archive JAR artifacts, publish test results, clean workspace
- **Success**: Console output with deployment summary (Slack notification ready)
- **Failure**: Console output with failure details (Slack notification ready)

To enable Slack notifications, uncomment the `slackSend` blocks in the Jenkinsfile `post` section and configure the Slack plugin with your workspace.

## API Documentation (Swagger)

All services include interactive API documentation via **springdoc-openapi** (OpenAPI 3.0).

### Swagger UI URLs

| Service | Swagger UI | OpenAPI JSON |
|---------|------------|--------------|
| Auth Service | http://localhost:8080/swagger-ui.html | http://localhost:8080/v3/api-docs |
| User Service | http://localhost:8081/swagger-ui.html | http://localhost:8081/v3/api-docs |
| Order Service | http://localhost:8082/swagger-ui.html | http://localhost:8082/v3/api-docs |
| Product Service | http://localhost:8083/swagger-ui.html | http://localhost:8083/v3/api-docs |
| API Gateway (aggregated) | http://localhost:8000/swagger-ui.html | http://localhost:8000/v3/api-docs |

### Using JWT Authentication in Swagger UI

1. Open Swagger UI for any service
2. Click the **Authorize** button (lock icon)
3. Enter your JWT token in the format: `<your-access-token>`
4. Click **Authorize** to apply the token to all requests
5. Execute secured endpoints directly from the browser

### Gateway Aggregation

The API Gateway aggregates all service documentation. Use the dropdown in Swagger UI at `http://localhost:8000/swagger-ui.html` to switch between services.

## Testing the System

### 1. Verify Services Are Running

```bash
# Config Server
curl http://localhost:8888/actuator/health

# Eureka Dashboard
open http://localhost:8761

# API Gateway
curl http://localhost:8000/actuator/health
```

### 2. Authentication Flow

#### Register a New User
```bash
curl -X POST http://localhost:8000/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "test@example.com",
    "password": "Test123!"
  }'
```

#### Login and Get JWT Token
```bash
curl -X POST http://localhost:8000/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "Test123!"
  }'
```

#### Use Token for Authenticated Requests
```bash
export TOKEN="YOUR_ACCESS_TOKEN_HERE"

curl -X GET http://localhost:8000/api/users/me \
  -H "Authorization: Bearer $TOKEN"
```

### 3. Test Service Endpoints

#### Auth Service (via Gateway)
```bash
# Validate token
curl -X GET http://localhost:8000/api/auth/validate \
  -H "Authorization: Bearer $TOKEN"

# Refresh token
curl -X POST http://localhost:8000/api/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{"refreshToken":"YOUR_REFRESH_TOKEN"}'
```

#### User Service
```bash
# Get current user profile
curl http://localhost:8000/api/users/me \
  -H "Authorization: Bearer $TOKEN"

# List all users (ADMIN only)
curl http://localhost:8000/api/users \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

#### Order Service
```bash
# Create order
curl -X POST http://localhost:8000/api/orders \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"userId": 1, "items": [{"productId": 1, "quantity": 2}]}'

# Get user's orders
curl http://localhost:8000/api/orders \
  -H "Authorization: Bearer $TOKEN"
```

#### Product Service
```bash
# Get all products (PUBLIC)
curl http://localhost:8000/api/products

# Create product (ADMIN only)
curl -X POST http://localhost:8000/api/products \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name": "Laptop", "description": "Gaming laptop", "price": 999.99, "stockQuantity": 50, "category": "Electronics"}'
```

## Default Users

| Username | Password | Roles | Description |
|----------|----------|-------|-------------|
| admin | admin123 | ROLE_USER, ROLE_ADMIN | Administrator access |
| user | user123 | ROLE_USER | Regular user access |

## Service Ports

| Service | Port | Description |
|---------|------|-------------|
| Config Server | 8888 | Centralized configuration |
| Eureka Server | 8761 | Service registry |
| API Gateway | 8000 | **Main entry point** |
| Auth Service | 8080 | Authentication & JWT |
| User Service | 8081 | User profile management |
| Order Service | 8082 | Order management |
| Product Service | 8083 | Product catalog |
| Redis | 6379 | Caching & rate limiting |

## API Documentation

### Authentication Endpoints

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| POST | /api/auth/register | Register new user | No |
| POST | /api/auth/login | Login and get tokens | No |
| POST | /api/auth/refresh | Refresh access token | No |
| POST | /api/auth/logout | Logout and revoke tokens | Yes |
| GET | /api/auth/validate | Validate JWT token | No |

### User Service Endpoints

| Method | Endpoint | Description | Auth | Roles |
|--------|----------|-------------|------|-------|
| GET | /api/users | List all users | Yes | ADMIN |
| GET | /api/users/me | Get current profile | Yes | USER |
| PUT | /api/users/me | Update own profile | Yes | USER |
| GET | /api/users/{id} | Get user by ID | Yes | ADMIN/Owner |
| DELETE | /api/users/{id} | Delete user | Yes | ADMIN |

### Order Service Endpoints

| Method | Endpoint | Description | Auth | Roles |
|--------|----------|-------------|------|-------|
| POST | /api/orders | Create order | Yes | USER |
| GET | /api/orders | Get user's orders | Yes | USER |
| GET | /api/orders/{id} | Get order details | Yes | USER |
| PUT | /api/orders/{id}/cancel | Cancel order | Yes | USER |

### Product Service Endpoints

| Method | Endpoint | Description | Auth | Roles |
|--------|----------|-------------|------|-------|
| GET | /api/products | List all products | No | - |
| GET | /api/products/{id} | Get product | No | - |
| GET | /api/products/search | Search products | No | - |
| POST | /api/products | Create product | Yes | ADMIN |
| PUT | /api/products/{id} | Update product | Yes | ADMIN |
| DELETE | /api/products/{id} | Delete product | Yes | ADMIN |

## Configuration

### Environment Profiles

| Profile | Description | Logging | Database |
|---------|-------------|---------|----------|
| `dev` (default) | Local development | Console (human-readable) | H2 in-memory |
| `prod` | Production | JSON structured + rolling file | PostgreSQL (via RDS) |
| `docker` | Docker Compose | Console | H2 in-memory |

### JWT Configuration

Configured in `config-repo/auth-service.yml`:

```yaml
jwt:
  secret: mySecretKeyForJWTTokenGenerationAndValidation12345678901234567890
  expiration: 900000          # 15 minutes
  refresh-expiration: 604800000  # 7 days
```

> **Warning**: Change the JWT secret in production! Use Kubernetes secrets or a vault.

## Monitoring & Troubleshooting

### Health Checks

```bash
# All services expose health endpoints
curl http://localhost:8000/actuator/health    # API Gateway
curl http://localhost:8080/actuator/health    # Auth Service

# Prometheus metrics
curl http://localhost:8080/actuator/prometheus

# Change log levels at runtime
curl -X POST http://localhost:8080/actuator/loggers/com.secure.auth \
  -H "Content-Type: application/json" \
  -d '{"configuredLevel": "DEBUG"}'
```

### Eureka Dashboard

```
http://localhost:8761
```

### Common Issues

| Issue | Solution |
|-------|----------|
| Service not registering with Eureka | Check config-server is running, verify bootstrap.yml |
| JWT Token Invalid | Check token expiry (15 min), verify JWT secret matches across services |
| Database Connection Issues | Check H2 console at http://localhost:8080/h2-console |
| Config Server Not Loading | Verify `config-repo/` path and Git repo state |
| Istio sidecar not injecting | Verify namespace label: `istio-injection: enabled` |

## Production Readiness Checklist

### Security
- [ ] Change JWT secret to a strong random value (256+ bits)
- [ ] Enable HTTPS/TLS for all services
- [ ] Store secrets in HashiCorp Vault or AWS Secrets Manager
- [ ] Configure proper CORS origins
- [ ] Enable Redis authentication (requirepass)
- [ ] Review Istio authorization policies
- [ ] Enable audit logging

### Infrastructure
- [ ] Provision AWS infra via Terraform (`terraform apply`)
- [ ] Switch from H2 to PostgreSQL RDS
- [ ] Configure ElastiCache Redis for caching
- [ ] Push Docker images to ECR
- [ ] Deploy to EKS with Istio

### Observability
- [ ] Deploy Prometheus + Grafana for metrics
- [ ] Deploy Jaeger for distributed tracing (lower sampling to 1-10%)
- [ ] Set up ELK/EFK stack for centralized log aggregation
- [ ] Configure alerting (PagerDuty, Slack)
- [ ] Set up dashboards per service

### Performance
- [ ] Configure JVM heap: `-Xms512m -Xmx1024m`
- [ ] Enable JPA query caching
- [ ] Configure Redis cache TTL
- [ ] Tune HikariCP connection pools
- [ ] Enable HTTP/2 in gateway
- [ ] Load test with k6 or Gatling

### CI/CD
- [ ] Configure Jenkins with Kubernetes plugin
- [ ] Add AWS and kubeconfig credentials
- [ ] Create Maven cache PVC for faster builds
- [ ] Enable SonarQube integration
- [ ] Enable Slack notifications
- [ ] Set up separate Jenkins jobs per environment

### Resilience
- [ ] Verify PodDisruptionBudgets are active
- [ ] Test rolling deployments (zero downtime)
- [ ] Verify Istio circuit breaking thresholds
- [ ] Test failover scenarios
- [ ] Configure horizontal pod autoscaling (HPA)

## Development

### Running Tests

```bash
# Run all tests
mvn test

# Run tests for specific service
cd auth-service && mvn test

# Run integration tests
mvn verify
```

### Adding a New Service

1. Create service module directory
2. Add module to parent `pom.xml`
3. Create service POM with dependencies
4. Implement main application class with `@EnableEurekaClient`
5. Add `@Slf4j` logging to controllers and services
6. Create `logback-spring.xml` with dev/prod profiles
7. Add configuration in `config-repo/`
8. Implement security configuration (OAuth2 Resource Server)
9. Create Dockerfile
10. Add to `docker-compose.yml`
11. Create Kubernetes manifest in `k8s/`
12. Add Istio DestinationRule and AuthorizationPolicy
13. Add ECR repository in `terraform/ecr.tf`
14. Add service to `ALL_SERVICES` in `Jenkinsfile`

## Migration Path to Spring Boot 3.x

When upgrading to Java 17+ and Spring Boot 3.x:

1. Update Java to 17 or 21
2. Update Spring Boot to 3.2.x
3. Update Spring Cloud to 2023.0.x
4. Replace `javax.*` imports with `jakarta.*`
5. Update Spring Security configuration (WebSecurityConfigurerAdapter deprecated)
6. Update to Micrometer Tracing (replaces Spring Cloud Sleuth)
7. Test thoroughly - breaking changes in security, JPA, validation

## Resources

- [Spring Boot Documentation](https://docs.spring.io/spring-boot/docs/2.7.x/reference/html/)
- [Spring Cloud Documentation](https://spring.io/projects/spring-cloud)
- [Istio Documentation](https://istio.io/latest/docs/)
- [Terraform AWS Provider](https://registry.terraform.io/providers/hashicorp/aws/latest/docs)
- [Kubernetes Documentation](https://kubernetes.io/docs/)

---

**Built with Spring Boot | Spring Cloud | Kubernetes | Istio | Terraform**
