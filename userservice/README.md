# User Service Platform

A unified, production-ready microservices platform combining Spring Cloud infrastructure services with enterprise gRPC/REST business services and a React frontend. All traffic flows through a single API Gateway backed by service discovery (Eureka) and centralized configuration (Spring Cloud Config).

---

## Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Project Structure](#project-structure)
- [Services](#services)
  - [Infrastructure Services](#infrastructure-services)
  - [Traditional Microservices (H2)](#traditional-microservices-h2)
  - [Enterprise gRPC Services (PostgreSQL)](#enterprise-grpc-services-postgresql)
  - [Frontend](#frontend)
- [Port Reference](#port-reference)
- [API Routes](#api-routes)
- [Technology Stack](#technology-stack)
- [Prerequisites](#prerequisites)
- [Running with Docker Compose](#running-with-docker-compose)
- [Running Locally (without Docker)](#running-locally-without-docker)
- [Build System](#build-system)
- [Configuration](#configuration)
  - [Spring Cloud Config](#spring-cloud-config)
  - [Environment Variables](#environment-variables)
  - [Profiles](#profiles)
- [Security & Authentication](#security--authentication)
- [gRPC](#grpc)
- [Database Configuration](#database-configuration)
- [Resilience & Circuit Breakers](#resilience--circuit-breakers)
- [Observability](#observability)
- [API Documentation (Swagger)](#api-documentation-swagger)
- [REST API Reference](#rest-api-reference)
- [Verification & Health Checks](#verification--health-checks)
- [Troubleshooting](#troubleshooting)

---

## Overview

This project merges two independent microservice stacks into one cohesive platform:

| Origin | Modules | Database | Registration |
|---|---|---|---|
| Spring Cloud stack | common-lib, config-server, eureka-server, api-gateway, auth-service, user-service, order-service, product-service | H2 (in-memory) | Eureka (native) |
| Enterprise gRPC stack | user-grpc-service, financial-service, health-service, social-service | PostgreSQL | Eureka (added) |
| Frontend | enterprise-ui | — | — |

The gRPC enterprise services were upgraded to register with Eureka and fetch their configuration from the Config Server, making all services discoverable through the unified API Gateway.

---

## Architecture

```
                    ┌─────────────────────────────┐
                    │      Enterprise UI           │
                    │   React + Vite (port 3000)   │
                    └──────────────┬──────────────┘
                                   │ /api/*
                    ┌──────────────▼──────────────┐
                    │         API Gateway          │
                    │   Spring Cloud (port 8000)   │
                    │  Rate Limiting via Redis      │
                    └─┬───┬───┬───┬───┬───┬───┬───┘
                      │   │   │   │   │   │   │   │
             ┌────────┘   │   │   │   │   │   │   └────────┐
             ▼            │   │   │   │   │   ▼            ▼
        auth-service  user-svc  order  product  financial  social
         (8080)       (8081)   (8082)  (8083)   (8084)    (8086)
                                              health-svc  user-grpc
                                               (8085)    (8090/9090)

    ┌──────────────────┐   ┌──────────────────┐   ┌────────┐
    │  Config Server   │   │  Eureka Server   │   │ Redis  │
    │  (port 8888)     │   │  (port 8761)     │   │ (6379) │
    └──────────────────┘   └──────────────────┘   └────────┘
```

**Startup Order** (enforced by `depends_on` + health checks):

```
config-server → eureka-server → redis → auth-service
                                       ↓
                    user-service, order-service, product-service,
                    user-grpc-service, financial-service,
                    health-service, social-service  (parallel)
                                       ↓
                                 api-gateway
                                       ↓
                               enterprise-ui
```

---

## Project Structure

```
userservice/
├── pom.xml                          ← Parent POM (Java 11, Spring Boot 2.7.18)
│
├── config-repo/                     ← Centralized configuration (not a Maven module)
│   ├── application.yml              ← Shared settings for all services
│   ├── application-dev.yml          ← Dev profile overrides
│   ├── application-prod.yml         ← Prod profile overrides
│   ├── api-gateway.yml
│   ├── auth-service.yml
│   ├── eureka-server.yml
│   ├── user-service.yml
│   ├── order-service.yml
│   ├── product-service.yml
│   ├── user-grpc-service.yml
│   ├── financial-service.yml
│   ├── health-service.yml
│   └── social-service.yml
│
├── common-lib/                      ← Shared DTOs, JWT utils, security config
├── config-server/                   ← Spring Cloud Config Server
├── eureka-server/                   ← Netflix Eureka service registry
├── api-gateway/                     ← Spring Cloud Gateway (edge router)
│
├── auth-service/                    ← JWT authentication + user registration
├── user-service/                    ← User CRUD (SDS stack)
├── order-service/                   ← Order management
├── product-service/                 ← Product catalog + Redis caching
│
├── user-grpc-service/               ← User management with dual REST+gRPC API
│   └── src/main/proto/user.proto    ← Protobuf service definition
├── financial-service/               ← Accounts & transactions
├── health-service/                  ← Health records & vitals
├── social-service/                  ← Profiles, posts, connections
│
├── enterprise-ui/                   ← React frontend (Vite)
│   ├── nginx.conf                   ← Proxies /api/ → api-gateway:8000
│   └── src/
│
└── docker-compose.yml               ← Orchestrates all 13 services
```

---

## Services

### Infrastructure Services

#### Config Server (port 8888)
- Spring Cloud Config Server in **native** mode (reads from `./config-repo/` volume mount)
- All other services fetch their configuration from this server on startup via `bootstrap.yml`
- Must be healthy before any other service starts

#### Eureka Server (port 8761)
- Netflix Eureka service registry
- All 12 business/gateway services register themselves here
- UI dashboard available at `http://localhost:8761`
- Secured with basic auth (`eureka:eureka`)

#### Redis (port 6379)
- Used by **API Gateway** for request rate limiting
- Used by **product-service** for product caching
- Data is persisted with AOF (`appendonly yes`)

---

### Traditional Microservices (H2)

These services use H2 in-memory databases (suitable for development). They use single-stage Dockerfiles and require a pre-built JAR (`mvn package`) before `docker-compose up --build`.

#### Auth Service (port 8080)
- Issues and validates JWT tokens (HMAC-SHA512)
- Provides `/api/auth/register` and `/api/auth/login`
- JWT access token: 15-minute expiration
- Refresh token: 7-day expiration
- H2 console: `http://localhost:8080/h2-console`

#### User Service (port 8081)
- User profile management (CRUD)
- Protected by JWT validation using shared secret
- H2 console: `http://localhost:8081/h2-console`

#### Order Service (port 8082)
- Order lifecycle management
- Calls other services via Feign Client (5s connect/read timeout)
- H2 console: `http://localhost:8082/h2-console`

#### Product Service (port 8083)
- Product catalog management
- Redis caching layer for product reads
- H2 console: `http://localhost:8083/h2-console`

---

### Enterprise gRPC Services (PostgreSQL)

These services were originally standalone gRPC services. They have been integrated with Spring Cloud: they now register with Eureka, fetch configuration from Config Server, and expose REST endpoints routable through the API Gateway. Their Dockerfiles perform a full Maven multi-stage build inside Docker.

Each service has:
- `bootstrap.yml` — loads `spring.application.name` and Config Server URI before `application.yml`
- `@EnableDiscoveryClient` — registers with Eureka on startup
- Three new Spring Cloud dependencies: `eureka-client`, `spring-cloud-config`, `spring-cloud-bootstrap`
- Resilience4j circuit breakers (sliding window: 10 calls, failure threshold: 50%, wait: 5s)

#### User gRPC Service (port 8090 REST / 9090 gRPC)
- Dual API: REST (HTTP) + gRPC (Protocol Buffers)
- User entity with JWT authentication
- Database migrations via Flyway
- gRPC service definition: `UserService` (CreateUser, GetUser, DeleteUser)
- PostgreSQL database: `grpcdb`

#### Financial Service (port 8084)
- Account management (create, fetch by user, fetch by ID)
- Transaction recording and retrieval per account
- PostgreSQL database: `financialdb`

#### Health Service (port 8085)
- Health record management per user
- Vital signs tracking with latest-vitals endpoint
- PostgreSQL database: `healthdb`

#### Social Service (port 8086)
- User profile management
- Post creation and retrieval
- Connection requests with accept/reject flow
- PostgreSQL database: `socialdb`

---

### Frontend

#### Enterprise UI (port 3000)
- React application built with Vite
- Served via Nginx
- All `/api/*` requests proxied to `api-gateway:8000` — no direct service URLs in the frontend
- SPA routing: all non-API paths serve `index.html`
- Build: `npm run build` produces `dist/` copied into Nginx container

---

## Port Reference

| Service | External Port | Internal Port | Protocol |
|---|---|---|---|
| config-server | 8888 | 8888 | HTTP |
| eureka-server | 8761 | 8761 | HTTP |
| redis | 6379 | 6379 | Redis |
| auth-service | 8080 | 8080 | HTTP |
| user-service | 8081 | 8081 | HTTP |
| order-service | 8082 | 8082 | HTTP |
| product-service | 8083 | 8083 | HTTP |
| user-grpc-service | 8090 | 8090 | HTTP (REST) |
| user-grpc-service | 9090 | 9090 | gRPC |
| financial-service | 8084 | 8084 | HTTP |
| health-service | 8085 | 8085 | HTTP |
| social-service | 8086 | 8086 | HTTP |
| api-gateway | 8000 | 8000 | HTTP |
| enterprise-ui | 3000 | 80 | HTTP |

---

## API Routes

All external traffic enters through the **API Gateway** at `http://localhost:8000`. The gateway strips the first two path segments (`StripPrefix=2`) before forwarding, e.g. `/api/auth/login` → `/login` at `auth-service`.

| Gateway Path | Routes To | Service Port |
|---|---|---|
| `/api/auth/**` | auth-service | 8080 |
| `/api/users/**` | user-service | 8081 |
| `/api/orders/**` | order-service | 8082 |
| `/api/products/**` | product-service | 8083 |
| `/api/grpc-users/**` | user-grpc-service | 8090 |
| `/api/accounts/**` | financial-service | 8084 |
| `/api/transactions/**` | financial-service | 8084 |
| `/api/health-records/**` | health-service | 8085 |
| `/api/vitals/**` | health-service | 8085 |
| `/api/profiles/**` | social-service | 8086 |
| `/api/posts/**` | social-service | 8086 |
| `/api/connections/**` | social-service | 8086 |

**OpenAPI docs** are also routed through the gateway:

| Path | Service Docs |
|---|---|
| `/v3/api-docs/auth-service` | Auth Service Swagger JSON |
| `/v3/api-docs/user-service` | User Service Swagger JSON |
| `/v3/api-docs/order-service` | Order Service Swagger JSON |
| `/v3/api-docs/product-service` | Product Service Swagger JSON |

**CORS** is configured on the gateway for:
- `http://localhost:3000` (enterprise-ui)
- `http://localhost:4200` (Angular dev server)

---

## Technology Stack

| Category | Technology | Version |
|---|---|---|
| Language | Java | 11 |
| Build | Maven | 3.9 |
| Spring Boot | Spring Boot | 2.7.18 |
| Spring Cloud | Spring Cloud | 2021.0.8 |
| Service Discovery | Netflix Eureka | (Spring Cloud) |
| Config Management | Spring Cloud Config | (Spring Cloud) |
| API Gateway | Spring Cloud Gateway | (Spring Cloud) |
| Security | Spring Security + JJWT | 0.11.5 |
| gRPC | gRPC Java | 1.58.0 |
| Protobuf | Protocol Buffers | 3.24.4 |
| gRPC Integration | grpc-server-spring-boot-starter | 2.15.0.RELEASE |
| Resilience | Resilience4j | 1.7.1 |
| DB (traditional) | H2 (in-memory) | 2.1.214 |
| DB (enterprise) | PostgreSQL | 42.5.4 driver |
| DB Migrations | Flyway | 9.16.3 |
| Caching | Redis | 7-alpine |
| Metrics | Micrometer + Prometheus | (Spring Boot managed) |
| API Docs | SpringDoc OpenAPI | 1.7.0 |
| Frontend | React + Vite | Node 18 |
| Web Server | Nginx | alpine |
| Containerization | Docker | — |
| Orchestration | Docker Compose | 3.8 |
| Base Images | eclipse-temurin (8/11), nginx:alpine | — |

---

## Prerequisites

- **Docker** ≥ 20.10 and **Docker Compose** ≥ 2.0 (for containerized run)
- **Java 11** (for local Maven builds — gRPC services require Java 11)
- **Maven** ≥ 3.9 (for local builds)
- **Node.js 18+** + **npm** (for local frontend development)
- **PostgreSQL** ≥ 13 (for local runs of enterprise gRPC services)

---

## Running with Docker Compose

### Quick Start with run-local.sh

A convenience script at the project root handles the Maven build and Docker Compose lifecycle:

```bash
cd userservice

./run-local.sh            # default: build JARs + docker-compose up -d --build
./run-local.sh up         # docker-compose up -d --build (no JAR rebuild)
./run-local.sh up --follow # build + up + stream logs
./run-local.sh down       # stop and remove containers
./run-local.sh down -v    # stop and remove containers + volumes (clears Redis data)
./run-local.sh logs [svc] # stream logs (optionally for a specific service)
./run-local.sh status     # show container status (docker-compose ps)
./run-local.sh restart    # restart all containers
```

The script auto-detects whether `docker compose` (V2 plugin) or `docker-compose` (standalone) is available.

---

### 1. Build the JARs (required for SDS single-stage services)

The traditional microservices (auth, user, order, product, config-server, eureka-server, api-gateway) use single-stage Dockerfiles that copy a pre-built JAR. You must build them first:

```bash
cd /path/to/userservice

# Build all modules, skip tests
mvn clean package -DskipTests
```

> The gRPC enterprise services (user-grpc-service, financial-service, health-service, social-service) use **multi-stage** Dockerfiles and build their own JARs inside Docker — no separate Maven step needed for them.

### 2. Start all services

```bash
docker-compose up -d --build
```

This starts all 13 containers. Full startup takes approximately **2–3 minutes** due to the health-check dependency chain.

### 3. Watch startup progress

```bash
docker-compose ps          # view container status
docker-compose logs -f     # stream all logs
docker-compose logs -f config-server eureka-server api-gateway   # specific services
```

### 4. Stop all services

```bash
docker-compose down          # stop and remove containers
docker-compose down -v       # also remove named volumes (clears Redis data)
```

### Rebuild a single service

```bash
# For SDS services — rebuild JAR first:
mvn clean package -DskipTests -pl auth-service -am
docker-compose up -d --build auth-service

# For gRPC services — Docker builds the JAR:
docker-compose up -d --build financial-service
```

---

## Running Locally (without Docker)

### Infrastructure (required first)

Start Redis locally:
```bash
docker run -d -p 6379:6379 redis:7-alpine
```

Start Config Server (reads config-repo from filesystem):
```bash
cd config-server
CONFIG_REPO_PATH=file://$(pwd)/../config-repo \
mvn spring-boot:run
```

Start Eureka Server:
```bash
cd eureka-server
SPRING_CLOUD_CONFIG_URI=http://localhost:8888 \
mvn spring-boot:run
```

### Business Services

Set environment variables and start each service:
```bash
cd auth-service
SPRING_CLOUD_CONFIG_URI=http://localhost:8888 \
EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE=http://eureka:eureka@localhost:8761/eureka \
mvn spring-boot:run
```

For gRPC enterprise services, also provide a running PostgreSQL instance:
```bash
cd financial-service
SPRING_CLOUD_CONFIG_URI=http://localhost:8888 \
EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE=http://eureka:eureka@localhost:8761/eureka \
DB_HOST=localhost DB_PORT=5432 \
DB_USERNAME=postgres DB_PASSWORD=postgres \
JWT_SECRET=enterpriseGrpcSecretKeyForJWT256bitMinimumLength! \
mvn spring-boot:run
```

### Frontend
```bash
cd enterprise-ui
npm install
npm run dev    # starts Vite dev server at http://localhost:5173
```

> In dev mode, configure the Vite proxy in `vite.config.js` to point `/api` at `http://localhost:8000` (the local API Gateway).

---

## Build System

The project uses a **Maven multi-module** setup with a single parent POM at the root.

### Parent POM coordinates
```
groupId:    com.userservice
artifactId: userservice
version:    1.0.0
```

### Build commands

```bash
# Build entire project (all 12 modules)
mvn clean package -DskipTests

# Build a specific module and its dependencies
mvn clean package -DskipTests -pl financial-service -am

# Build only infrastructure services
mvn clean package -DskipTests -pl config-server,eureka-server,api-gateway -am

# Run tests
mvn test

# Run tests for a specific module
mvn test -pl health-service
```

### Module dependency graph

```
userservice (parent)
├── common-lib                  ← no dependencies on other modules
├── config-server               ← no dependencies on other modules
├── eureka-server               ← no dependencies on other modules
├── api-gateway                 ← no dependencies on other modules
├── auth-service → common-lib
├── user-service → common-lib
├── order-service → common-lib
├── product-service → common-lib
├── user-grpc-service           ← standalone (own JWT/security)
├── financial-service           ← standalone
├── health-service              ← standalone
└── social-service              ← standalone
```

### Docker build strategies

| Service Group | Build Strategy | Build Context |
|---|---|---|
| config-server, eureka-server, api-gateway, auth-service, user-service, order-service, product-service | Single-stage: `COPY target/*.jar` | Module directory (`./auth-service`) |
| user-grpc-service, financial-service, health-service, social-service | Multi-stage: Maven inside Docker | Project root (`.`) |
| enterprise-ui | Multi-stage: Node build → Nginx | `./enterprise-ui` |

---

## Configuration

### Spring Cloud Config

All services load their configuration from the Config Server using `bootstrap.yml`. The Config Server reads YAML files from the `./config-repo/` directory (mounted as a Docker volume).

**Config resolution order** (Spring Cloud standard):
1. `application.yml` — shared by all services
2. `application-{profile}.yml` — profile-specific shared config
3. `{service-name}.yml` — service-specific config
4. `{service-name}-{profile}.yml` — service+profile-specific config

**Config Server URL**: `http://config-server:8888` (Docker) or `http://localhost:8888` (local)

### Environment Variables

#### All services

| Variable | Default | Description |
|---|---|---|
| `SPRING_CLOUD_CONFIG_URI` | `http://localhost:8888` | Config Server URL |
| `EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE` | `http://eureka:eureka@localhost:8761/eureka` | Eureka registration URL |
| `SPRING_PROFILES_ACTIVE` | (none) | Active Spring profiles |

#### gRPC Enterprise Services (additional)

| Variable | Default | Description |
|---|---|---|
| `DB_HOST` | `localhost` | PostgreSQL host |
| `DB_PORT` | `5432` | PostgreSQL port |
| `DB_USERNAME` | `postgres` | Database username |
| `DB_PASSWORD` | `postgres` | Database password |
| `JWT_SECRET` | `enterpriseGrpcSecretKeyForJWT256bitMinimumLength!` | JWT signing secret |

#### Config Server

| Variable | Default | Description |
|---|---|---|
| `CONFIG_REPO_PATH` | `file:///${user.home}/secure-distributed-system/config-repo` | Path to config-repo directory |

#### Product Service / API Gateway (additional)

| Variable | Default | Description |
|---|---|---|
| `SPRING_REDIS_HOST` | `localhost` | Redis host |
| `SPRING_REDIS_PORT` | `6379` | Redis port |

### Profiles

| Profile | Description |
|---|---|
| (default) | Local development — uses `localhost` defaults |
| `docker` | Docker Compose — services use container hostnames |
| `dev` | Debug logging, relaxed security |
| `prod` | INFO logging, stricter settings |

Activate with `SPRING_PROFILES_ACTIVE=docker` (set in docker-compose.yml for all services).

---

## Security & Authentication

### JWT Architecture

The platform uses two separate JWT signing secrets:

**Traditional services** (auth-service, user-service, order-service, product-service):
```
Secret: mySecretKeyForJWTTokenGenerationAndValidation12345678901234567890
Algorithm: HMAC-SHA512 (via JJWT 0.11.5)
Access token TTL: 15 minutes
Refresh token TTL: 7 days
```

**Enterprise gRPC services** (user-grpc-service, financial-service, health-service, social-service):
```
Secret: enterpriseGrpcSecretKeyForJWT256bitMinimumLength!
Algorithm: HMAC-SHA512
Token TTL: 24 hours
```

> The API Gateway does **not** validate JWTs — it passes the `Authorization` header through to the downstream service, which validates the token itself. The two JWT stacks coexist independently.

### Typical Authentication Flow

```
1. POST /api/auth/register  →  Create account
2. POST /api/auth/login     →  Receive { accessToken, refreshToken }
3. GET  /api/users/me       →  Pass "Authorization: Bearer <accessToken>"
```

### H2 Console Access (development only)

All traditional microservices expose the H2 console at `/h2-console`:
- Auth: `http://localhost:8080/h2-console` — JDBC URL: `jdbc:h2:mem:authdb`
- User: `http://localhost:8081/h2-console` — JDBC URL: `jdbc:h2:mem:userdb`
- Order: `http://localhost:8082/h2-console` — JDBC URL: `jdbc:h2:mem:orderdb`
- Product: `http://localhost:8083/h2-console` — JDBC URL: `jdbc:h2:mem:productdb`

---

## gRPC

The `user-grpc-service` exposes a **gRPC server on port 9090** in addition to its REST API on port 8090.

### Proto definition (`src/main/proto/user.proto`)

```protobuf
syntax = "proto3";
option java_multiple_files = true;
option java_package = "com.enterprise.grpc";

service UserService {
  rpc CreateUser (UserRequest) returns (UserResponse);
  rpc GetUser    (UserIdRequest) returns (UserResponse);
  rpc DeleteUser (UserIdRequest) returns (DeleteResponse);
}

message UserRequest {
  string name  = 1;
  string email = 2;
}

message UserResponse {
  int64  id    = 1;
  string name  = 2;
  string email = 3;
}

message UserIdRequest {
  int64 id = 1;
}

message DeleteResponse {
  string message = 1;
}
```

### Calling gRPC

Use any gRPC client (e.g., [grpcurl](https://github.com/fullstorydev/grpcurl)):

```bash
# List services
grpcurl -plaintext localhost:9090 list

# Create a user
grpcurl -plaintext -d '{"name":"Alice","email":"alice@example.com"}' \
    localhost:9090 com.enterprise.grpc.UserService/CreateUser

# Get a user
grpcurl -plaintext -d '{"id":1}' \
    localhost:9090 com.enterprise.grpc.UserService/GetUser
```

> Note: gRPC is not routed through the API Gateway. Connect directly to `localhost:9090`.

---

## Database Configuration

### Traditional Services — H2 (in-memory)

Data is recreated on every restart (`ddl-auto: create-drop`). H2 console is enabled for development.

| Service | JDBC URL |
|---|---|
| auth-service | `jdbc:h2:mem:authdb` |
| user-service | `jdbc:h2:mem:userdb` |
| order-service | `jdbc:h2:mem:orderdb` |
| product-service | `jdbc:h2:mem:productdb` |

### Enterprise Services — PostgreSQL

Schema is managed by Flyway or JPA (`ddl-auto: update`). Requires a running PostgreSQL instance.

| Service | Database | Default host |
|---|---|---|
| user-grpc-service | `grpcdb` | `${DB_HOST:localhost}:5432` |
| financial-service | `financialdb` | `${DB_HOST:localhost}:5432` |
| health-service | `healthdb` | `${DB_HOST:localhost}:5432` |
| social-service | `socialdb` | `${DB_HOST:localhost}:5432` |

**Create databases before first run:**

```sql
CREATE DATABASE grpcdb;
CREATE DATABASE financialdb;
CREATE DATABASE healthdb;
CREATE DATABASE socialdb;
```

Or start an ephemeral PostgreSQL container:

```bash
docker run -d --name postgres \
  --network userservice_microservices-network \
  -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 \
  postgres:15-alpine

# Create all databases
docker exec -it postgres psql -U postgres -c "
  CREATE DATABASE grpcdb;
  CREATE DATABASE financialdb;
  CREATE DATABASE healthdb;
  CREATE DATABASE socialdb;
"
```

> When running via docker-compose, add `DB_HOST=postgres` to the gRPC service environment blocks and make them depend on the postgres service.

---

## Resilience & Circuit Breakers

The four enterprise gRPC services use **Resilience4j** circuit breakers.

**Global configuration** (from `config-repo/financial-service.yml`, same pattern for all):

| Parameter | Value |
|---|---|
| Sliding window size | 10 calls |
| Failure rate threshold | 50% |
| Wait duration in open state | 5 seconds |
| Min calls before evaluating | 5 |

The `user-grpc-service` also has a circuit breaker instance named `userService` with a 10-call sliding window and 10-second open wait.

**Actuator endpoint** to view circuit breaker state:
```bash
curl http://localhost:8084/actuator/circuitbreakers
```

---

## Observability

### Actuator Endpoints

All services expose the following actuator endpoints:

| Endpoint | URL pattern | Description |
|---|---|---|
| Health | `/{port}/actuator/health` | Liveness + readiness probes |
| Info | `/{port}/actuator/info` | Build info |
| Prometheus | `/{port}/actuator/prometheus` | Metrics scrape endpoint |
| Metrics | `/{port}/actuator/metrics` | Micrometer metrics |
| Loggers | `/{port}/actuator/loggers` | Runtime log level changes |
| Circuit Breakers | `/{port}/actuator/circuitbreakers` | Resilience4j state |
| Environment | `/{port}/actuator/env` | Applied configuration |

### Logging

All services use a structured console pattern with distributed trace support:

```
%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] [%X{traceId:-}] %-5level %logger{36} - %msg%n
```

Log levels (configurable at runtime via `/actuator/loggers`):
- `com.secure` / `com.enterprise`: DEBUG
- `org.springframework.security`: INFO
- `org.hibernate.SQL`: WARN

### Prometheus

Each service exposes `/actuator/prometheus`. To scrape metrics, point a Prometheus instance at each service's actuator endpoint, or add a Prometheus container to `docker-compose.yml`.

---

## API Documentation (Swagger)

SpringDoc OpenAPI UI is available on each service directly or through the gateway.

| Service | Swagger UI URL |
|---|---|
| auth-service | `http://localhost:8080/swagger-ui.html` |
| user-service | `http://localhost:8081/swagger-ui.html` |
| order-service | `http://localhost:8082/swagger-ui.html` |
| product-service | `http://localhost:8083/swagger-ui.html` |
| user-grpc-service | `http://localhost:8090/swagger-ui.html` |
| financial-service | `http://localhost:8084/swagger-ui.html` |
| health-service | `http://localhost:8085/swagger-ui.html` |
| social-service | `http://localhost:8086/swagger-ui.html` |

**OpenAPI JSON** via the gateway:
```bash
curl http://localhost:8000/v3/api-docs/auth-service
curl http://localhost:8000/v3/api-docs/user-service
curl http://localhost:8000/v3/api-docs/order-service
curl http://localhost:8000/v3/api-docs/product-service
```

---

## REST API Reference

All paths below are the **gateway-facing** paths (prefix `/api/<service>/`).

### Auth Service — `/api/auth/**`

| Method | Path | Description | Auth Required |
|---|---|---|---|
| POST | `/api/auth/register` | Register a new user | No |
| POST | `/api/auth/login` | Login, receive JWT tokens | No |

### User Service — `/api/users/**`

| Method | Path | Description | Auth Required |
|---|---|---|---|
| GET | `/api/users/{id}` | Get user by ID | Yes |
| POST | `/api/users` | Create user | Yes |
| PUT | `/api/users/{id}` | Update user | Yes |
| DELETE | `/api/users/{id}` | Delete user | Yes |

### Order Service — `/api/orders/**`

| Method | Path | Description | Auth Required |
|---|---|---|---|
| GET | `/api/orders/{id}` | Get order | Yes |
| POST | `/api/orders` | Create order | Yes |
| PUT | `/api/orders/{id}` | Update order | Yes |

### Product Service — `/api/products/**`

| Method | Path | Description | Auth Required |
|---|---|---|---|
| GET | `/api/products` | List all products | No |
| GET | `/api/products/{id}` | Get product (cached) | No |
| POST | `/api/products` | Create product | Yes (ADMIN) |
| PUT | `/api/products/{id}` | Update product | Yes (ADMIN) |
| DELETE | `/api/products/{id}` | Delete product | Yes (ADMIN) |

### User gRPC Service — `/api/grpc-users/**`

| Method | Path | Description | Auth Required |
|---|---|---|---|
| POST | `/api/grpc-users/api/users` | Create user | Yes |
| GET | `/api/grpc-users/api/users/{id}` | Get user | Yes |
| DELETE | `/api/grpc-users/api/users/{id}` | Delete user | Yes |
| POST | `/api/grpc-users/api/auth/register` | Register (gRPC service) | No |
| POST | `/api/grpc-users/api/auth/login` | Login (gRPC service) | No |

### Financial Service — `/api/accounts/**` and `/api/transactions/**`

| Method | Path | Description | Auth Required |
|---|---|---|---|
| POST | `/api/accounts` | Create account | Yes |
| GET | `/api/accounts/{id}` | Get account | Yes |
| GET | `/api/accounts/user/{userId}` | List accounts for user | Yes |
| POST | `/api/transactions` | Record transaction | Yes |
| GET | `/api/transactions/account/{accountId}` | List transactions | Yes |

### Health Service — `/api/health-records/**` and `/api/vitals/**`

| Method | Path | Description | Auth Required |
|---|---|---|---|
| POST | `/api/health-records` | Create health record | Yes |
| GET | `/api/health-records/user/{userId}` | List health records | Yes |
| POST | `/api/vitals` | Record vitals | Yes |
| GET | `/api/vitals/user/{userId}` | List vitals for user | Yes |
| GET | `/api/vitals/user/{userId}/latest` | Get latest vitals | Yes |

### Social Service — `/api/profiles/**`, `/api/posts/**`, `/api/connections/**`

| Method | Path | Description | Auth Required |
|---|---|---|---|
| POST | `/api/profiles` | Create profile | Yes |
| GET | `/api/profiles/user/{userId}` | Get profile | Yes |
| POST | `/api/posts` | Create post | Yes |
| GET | `/api/posts/user/{userId}` | List posts for user | Yes |
| POST | `/api/connections` | Send connection request | Yes |
| PUT | `/api/connections/{id}/accept` | Accept connection | Yes |
| GET | `/api/connections/user/{userId}` | List connections | Yes |

---

## Verification & Health Checks

After `docker-compose up -d --build`, wait ~2 minutes then run:

```bash
# Infrastructure
curl http://localhost:8888/actuator/health    # Config Server
curl http://localhost:8761/actuator/health    # Eureka
curl http://localhost:6379/ping              # Redis (use redis-cli)

# Gateway
curl http://localhost:8000/actuator/health

# Traditional services via gateway
curl http://localhost:8000/api/auth/actuator/health
curl http://localhost:8000/api/users/actuator/health
curl http://localhost:8000/api/orders/actuator/health
curl http://localhost:8000/api/products/actuator/health

# Enterprise gRPC services via gateway
curl http://localhost:8000/api/grpc-users/actuator/health
curl http://localhost:8000/api/accounts/actuator/health
curl http://localhost:8000/api/health-records/actuator/health
curl http://localhost:8000/api/profiles/actuator/health

# Eureka dashboard (all registered services)
open http://localhost:8761

# Frontend
open http://localhost:3000
```

**Expected Eureka registrations** (12 services):
`API-GATEWAY`, `AUTH-SERVICE`, `USER-SERVICE`, `ORDER-SERVICE`, `PRODUCT-SERVICE`, `USER-GRPC-SERVICE`, `FINANCIAL-SERVICE`, `HEALTH-SERVICE`, `SOCIAL-SERVICE`

---

## Troubleshooting

### Services fail to start — "Could not fetch config"

The Config Server must be healthy before other services start. Verify:
```bash
docker-compose logs config-server
curl http://localhost:8888/actuator/health
```

Check that the `config-repo` volume is mounted correctly:
```bash
curl http://localhost:8888/application/default
curl http://localhost:8888/auth-service/docker
```

### gRPC services fail — "Connection refused" to PostgreSQL

The enterprise services need PostgreSQL. When running via docker-compose, you must either:
1. Add a `postgres` service to `docker-compose.yml` and set `DB_HOST=postgres` in the gRPC service environments, **or**
2. Start a local PostgreSQL instance and set `DB_HOST=host.docker.internal` (macOS/Windows) or your host IP (Linux)

### Maven build fails — "Child module does not exist"

Make sure to run `mvn clean package` from the **userservice root**, not from a module subdirectory.

### Docker build fails for SDS services — "no such file: target/*.jar"

Run `mvn clean package -DskipTests` before `docker-compose up --build`. The SDS module Dockerfiles copy a pre-built JAR and do not run Maven.

### Port conflicts

If any port is already in use, stop the conflicting process or change the host port in `docker-compose.yml`:
```yaml
ports:
  - "18080:8080"  # use host port 18080
```

### Circuit breaker stays OPEN

Check the service's actuator endpoint:
```bash
curl http://localhost:8084/actuator/circuitbreakers | jq .
```

The circuit resets to HALF_OPEN after 5 seconds. Force a reset by restarting the service:
```bash
docker-compose restart financial-service
```

### View logs for a specific service

```bash
docker-compose logs -f --tail=100 financial-service
docker-compose logs -f --tail=100 api-gateway
```
