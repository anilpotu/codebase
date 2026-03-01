# AWS Setup Guide — userservice

Complete reference for provisioning and deploying the **userservice** combined platform on AWS.
All infrastructure is managed by Terraform (`userservice/terraform/`).
All Kubernetes manifests for production are in `userservice/helm/` (Helm charts).

---

## Table of Contents

1. [AWS Account Requirements](#1-aws-account-requirements)
2. [IAM User & Permissions](#2-iam-user--permissions)
3. [Bootstrap: S3 + DynamoDB for Terraform State](#3-bootstrap-s3--dynamodb-for-terraform-state)
4. [Tools Required](#4-tools-required)
5. [Terraform Infrastructure Overview](#5-terraform-infrastructure-overview)
6. [Step-by-Step Terraform Deployment](#6-step-by-step-terraform-deployment)
7. [Build & Push Docker Images to ECR](#7-build--push-docker-images-to-ecr)
8. [Configure kubectl for EKS](#8-configure-kubectl-for-eks)
9. [Create Kubernetes Secrets](#9-create-kubernetes-secrets)
10. [Deploy with Helm](#10-deploy-with-helm)
11. [Post-Deployment Database Setup](#11-post-deployment-database-setup)
12. [Verify Deployment](#12-verify-deployment)
13. [Cost Estimate](#13-cost-estimate)
14. [Teardown](#14-teardown)
15. [Troubleshooting](#15-troubleshooting)

---

## 1. AWS Account Requirements

| Requirement | Details |
|---|---|
| AWS Account | Active account with billing enabled |
| Region | `us-east-1` (default; adjust `aws_region` variable to change) |
| Service Quotas | EKS: 1 cluster, EC2: 6 t3.large instances, RDS: 1 db.t3.medium, ElastiCache: 1 cache.t3.micro |
| Root account | Not recommended — use IAM user with least-privilege |

---

## 2. IAM User & Permissions

### Create a deployment IAM user

```bash
# Create user
aws iam create-user --user-name userservice-deploy

# Create and attach policy
aws iam create-policy \
  --policy-name userservice-deploy-policy \
  --policy-document file://iam-policy.json

aws iam attach-user-policy \
  --user-name userservice-deploy \
  --policy-arn arn:aws:iam::<ACCOUNT_ID>:policy/userservice-deploy-policy

# Create access key
aws iam create-access-key --user-name userservice-deploy
```

### Required IAM permissions (iam-policy.json)

Save the following as `iam-policy.json` before running the commands above:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "EKS",
      "Effect": "Allow",
      "Action": [
        "eks:*"
      ],
      "Resource": "*"
    },
    {
      "Sid": "EC2VPC",
      "Effect": "Allow",
      "Action": [
        "ec2:*"
      ],
      "Resource": "*"
    },
    {
      "Sid": "RDS",
      "Effect": "Allow",
      "Action": [
        "rds:*"
      ],
      "Resource": "*"
    },
    {
      "Sid": "ElastiCache",
      "Effect": "Allow",
      "Action": [
        "elasticache:*"
      ],
      "Resource": "*"
    },
    {
      "Sid": "ECR",
      "Effect": "Allow",
      "Action": [
        "ecr:*"
      ],
      "Resource": "*"
    },
    {
      "Sid": "IAM",
      "Effect": "Allow",
      "Action": [
        "iam:CreateRole",
        "iam:DeleteRole",
        "iam:AttachRolePolicy",
        "iam:DetachRolePolicy",
        "iam:GetRole",
        "iam:ListRolePolicies",
        "iam:ListAttachedRolePolicies",
        "iam:PassRole",
        "iam:CreateInstanceProfile",
        "iam:DeleteInstanceProfile",
        "iam:AddRoleToInstanceProfile",
        "iam:RemoveRoleFromInstanceProfile",
        "iam:GetInstanceProfile"
      ],
      "Resource": "*"
    },
    {
      "Sid": "S3Terraform",
      "Effect": "Allow",
      "Action": [
        "s3:CreateBucket",
        "s3:DeleteBucket",
        "s3:GetObject",
        "s3:PutObject",
        "s3:DeleteObject",
        "s3:ListBucket",
        "s3:GetBucketVersioning",
        "s3:PutBucketVersioning",
        "s3:GetEncryptionConfiguration",
        "s3:PutEncryptionConfiguration",
        "s3:GetBucketPublicAccessBlock",
        "s3:PutBucketPublicAccessBlock"
      ],
      "Resource": [
        "arn:aws:s3:::userservice-tfstate",
        "arn:aws:s3:::userservice-tfstate/*"
      ]
    },
    {
      "Sid": "DynamoDBTerraform",
      "Effect": "Allow",
      "Action": [
        "dynamodb:GetItem",
        "dynamodb:PutItem",
        "dynamodb:DeleteItem",
        "dynamodb:DescribeTable",
        "dynamodb:CreateTable",
        "dynamodb:DeleteTable"
      ],
      "Resource": "arn:aws:dynamodb:us-east-1:*:table/userservice-tflock"
    },
    {
      "Sid": "SecretsManager",
      "Effect": "Allow",
      "Action": [
        "secretsmanager:CreateSecret",
        "secretsmanager:DeleteSecret",
        "secretsmanager:GetSecretValue",
        "secretsmanager:PutSecretValue",
        "secretsmanager:DescribeSecret",
        "secretsmanager:RestoreSecret"
      ],
      "Resource": "arn:aws:secretsmanager:us-east-1:*:secret:userservice/*"
    }
  ]
}
```

### Configure credentials

```bash
# Option A — Profile (recommended)
aws configure --profile userservice-deploy
# Enter: AWS Access Key ID, Secret Key, Region (us-east-1), Output (json)
export AWS_PROFILE=userservice-deploy

# Option B — Environment variables
export AWS_ACCESS_KEY_ID="AKIA..."
export AWS_SECRET_ACCESS_KEY="..."
export AWS_DEFAULT_REGION="us-east-1"

# Verify identity
aws sts get-caller-identity
```

---

## 3. Bootstrap: S3 + DynamoDB for Terraform State

These resources must be created **before** running `terraform init`. Create them once manually:

```bash
REGION="us-east-1"
ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)

# 1. Create S3 bucket for Terraform state
aws s3api create-bucket \
  --bucket userservice-tfstate \
  --region $REGION

# 2. Enable versioning (required for state safety)
aws s3api put-bucket-versioning \
  --bucket userservice-tfstate \
  --versioning-configuration Status=Enabled

# 3. Enable server-side encryption
aws s3api put-bucket-encryption \
  --bucket userservice-tfstate \
  --server-side-encryption-configuration '{
    "Rules": [{
      "ApplyServerSideEncryptionByDefault": {
        "SSEAlgorithm": "AES256"
      }
    }]
  }'

# 4. Block all public access
aws s3api put-public-access-block \
  --bucket userservice-tfstate \
  --public-access-block-configuration \
    BlockPublicAcls=true,IgnorePublicAcls=true,BlockPublicPolicy=true,RestrictPublicBuckets=true

# 5. Create DynamoDB table for state locking
aws dynamodb create-table \
  --table-name userservice-tflock \
  --attribute-definitions AttributeName=LockID,AttributeType=S \
  --key-schema AttributeName=LockID,KeyType=HASH \
  --billing-mode PAY_PER_REQUEST \
  --region $REGION

echo "Bootstrap complete. S3: userservice-tfstate, DynamoDB: userservice-tflock"
```

---

## 4. Tools Required

| Tool | Minimum Version | Install |
|---|---|---|
| AWS CLI | 2.x | `pip install awscli` or [aws.amazon.com/cli](https://aws.amazon.com/cli/) |
| Terraform | 1.5+ | [developer.hashicorp.com/terraform/install](https://developer.hashicorp.com/terraform/install) |
| kubectl | 1.28+ | [kubernetes.io/docs/tasks/tools](https://kubernetes.io/docs/tasks/tools/) |
| Helm | 3.12+ | `curl https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3 \| bash` |
| Docker | 24+ | [docs.docker.com/get-docker](https://docs.docker.com/get-docker/) |
| Java | 11 | `sdk install java 11` or OS package manager |
| Maven | 3.8+ | `sdk install maven` |

---

## 5. Terraform Infrastructure Overview

The Terraform configuration in `userservice/terraform/` provisions:

| Resource | Type | Details |
|---|---|---|
| **VPC** | `aws_vpc` | CIDR `10.2.0.0/16`, DNS enabled |
| **Public Subnets** | `aws_subnet` × 3 | `10.2.1.0/24`, `10.2.2.0/24`, `10.2.3.0/24` — for NAT GW & load balancers |
| **Private Subnets** | `aws_subnet` × 3 | `10.2.10.0/24`, `10.2.20.0/24`, `10.2.30.0/24` — for EKS nodes, RDS, Redis |
| **Internet Gateway** | `aws_internet_gateway` | Public internet access |
| **NAT Gateway** | `aws_nat_gateway` | Private subnet internet egress |
| **EKS Cluster** | `aws_eks_cluster` | `userservice-eks`, Kubernetes 1.28 |
| **EKS Node Group** | `aws_eks_node_group` | `t3.large`, desired 3 / min 2 / max 6 |
| **EKS Add-ons** | `aws_eks_addon` | vpc-cni, coredns, kube-proxy |
| **RDS PostgreSQL** | `aws_db_instance` | `db.t3.medium`, PostgreSQL 15, 50GB gp3, auto-scale to 200GB |
| **ElastiCache Redis** | `aws_elasticache_replication_group` | `cache.t3.micro`, Redis 7.0 |
| **ECR Repositories** | `aws_ecr_repository` × 12 | One per service, image scanning enabled |
| **Secrets Manager** | `aws_secretsmanager_secret` | Stores RDS password (auto-generated) |
| **Security Groups** | `aws_security_group` × 4 | EKS cluster, EKS nodes, RDS, Redis |
| **IAM Roles** | `aws_iam_role` × 2 | EKS cluster role, EKS node role |

### Architecture diagram

```
                        Internet
                           │
                    ┌──────▼──────┐
                    │   IGW       │
                    └──────┬──────┘
                           │
         ┌─────────────────┼─────────────────┐
         │ Public Subnets (3 AZs)             │
         │  10.2.1.0/24, .2.0, .3.0           │
         │  ┌─────────┐   ┌───────────────┐  │
         │  │ NAT GW  │   │  ALB/NLB      │  │
         │  └────┬────┘   └───────┬───────┘  │
         └───────┼────────────────┼───────────┘
                 │                │
         ┌───────┼────────────────┼───────────┐
         │ Private Subnets (3 AZs)            │
         │  10.2.10.0/24, .20.0, .30.0        │
         │                                    │
         │  ┌─────────────────────────────┐   │
         │  │     EKS Node Group          │   │
         │  │     (t3.large × 3)          │   │
         │  │  ┌────────────────────────┐ │   │
         │  │  │  userservice namespace  │ │   │
         │  │  │  12 microservices       │ │   │
         │  │  └────────────────────────┘ │   │
         │  └────────────┬────────────────┘   │
         │               │                    │
         │  ┌────────────┼────────────────┐   │
         │  │ RDS PostgreSQL    Redis      │   │
         │  │ db.t3.medium      t3.micro   │   │
         │  └─────────────────────────────┘   │
         └────────────────────────────────────┘
```

---

## 6. Step-by-Step Terraform Deployment

### Variables

Key variables (override in `terraform.tfvars` or via `-var` flags):

| Variable | Default | Description |
|---|---|---|
| `aws_region` | `us-east-1` | AWS region |
| `environment` | `dev` | Environment tag |
| `cluster_name` | `userservice-eks` | EKS cluster name |
| `vpc_cidr` | `10.2.0.0/16` | VPC CIDR block |
| `node_instance_type` | `t3.large` | EKS worker node type |
| `node_desired_size` | `3` | Desired node count |
| `node_min_size` | `2` | Minimum node count |
| `node_max_size` | `6` | Maximum node count |
| `db_instance_class` | `db.t3.medium` | RDS instance type |
| `redis_node_type` | `cache.t3.micro` | ElastiCache type |
| `eks_cluster_version` | `1.28` | Kubernetes version |
| `ecr_image_retention_count` | `10` | Images to keep per ECR repo |

### Create terraform.tfvars (optional)

```hcl
# userservice/terraform/terraform.tfvars
environment        = "production"
node_instance_type = "t3.large"
node_desired_size  = 3
db_instance_class  = "db.t3.medium"
redis_node_type    = "cache.t3.micro"
```

### Run Terraform

```bash
cd userservice/terraform

# 1. Initialize (downloads providers, configures S3 backend)
terraform init

# 2. Validate configuration
terraform validate

# 3. Preview all changes (no infrastructure created yet)
terraform plan -out=tfplan

# Review the plan output carefully, then:

# 4. Apply (creates all AWS resources — takes ~15-20 minutes)
terraform apply tfplan

# Or apply directly with auto-approve (CI/CD only):
terraform apply -auto-approve
```

### Expected apply output

```
Apply complete! Resources: 38 added, 0 changed, 0 destroyed.

Outputs:

eks_cluster_name     = "userservice-eks"
eks_cluster_endpoint = "https://XXXXXXXX.gr7.us-east-1.eks.amazonaws.com"
rds_endpoint         = "userservice-eks-postgres.XXXXXX.us-east-1.rds.amazonaws.com"
redis_endpoint       = "userservice-eks-redis.XXXXXX.cache.amazonaws.com"
ecr_repository_urls  = {
  "api-gateway"        = "123456789.dkr.ecr.us-east-1.amazonaws.com/userservice/api-gateway"
  "auth-service"       = "123456789.dkr.ecr.us-east-1.amazonaws.com/userservice/auth-service"
  ...
}
kubeconfig_command   = "aws eks update-kubeconfig --region us-east-1 --name userservice-eks"
```

---

## 7. Build & Push Docker Images to ECR

```bash
# 1. Get account ID and ECR endpoint
ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
ECR_ENDPOINT="${ACCOUNT_ID}.dkr.ecr.us-east-1.amazonaws.com"
TAG=$(git rev-parse --short HEAD)

# 2. Authenticate Docker to ECR
aws ecr get-login-password --region us-east-1 \
  | docker login --username AWS --password-stdin "${ECR_ENDPOINT}"

# 3. Build Maven JARs (SDS-origin services need pre-built JARs)
cd userservice
mvn clean package -DskipTests

# 4. Build and push all 12 images
cd ..
./deployment/scripts/02-docker-push.sh "${ECR_ENDPOINT}" "${TAG}" us

# Or push individually:
for SVC in config-server eureka-server api-gateway auth-service user-service \
           order-service product-service; do
  docker build -t "${ECR_ENDPOINT}/userservice/${SVC}:${TAG}" \
    -f userservice/${SVC}/Dockerfile userservice/${SVC}/
  docker push "${ECR_ENDPOINT}/userservice/${SVC}:${TAG}"
done

for SVC in user-grpc-service financial-service health-service social-service; do
  docker build -t "${ECR_ENDPOINT}/userservice/${SVC}:${TAG}" \
    -f userservice/${SVC}/Dockerfile userservice/
  docker push "${ECR_ENDPOINT}/userservice/${SVC}:${TAG}"
done

# enterprise-ui
docker build -t "${ECR_ENDPOINT}/userservice/enterprise-ui:${TAG}" \
  userservice/enterprise-ui/
docker push "${ECR_ENDPOINT}/userservice/enterprise-ui:${TAG}"
```

---

## 8. Configure kubectl for EKS

```bash
# Update kubeconfig (replaces minikube context with EKS context)
aws eks update-kubeconfig \
  --region us-east-1 \
  --name userservice-eks

# Verify connection
kubectl get nodes
kubectl get nodes -o wide

# Expected output:
# NAME                          STATUS   ROLES    AGE   VERSION
# ip-10-2-10-x.ec2.internal     Ready    <none>   5m    v1.28.x
# ip-10-2-20-x.ec2.internal     Ready    <none>   5m    v1.28.x
# ip-10-2-30-x.ec2.internal     Ready    <none>   5m    v1.28.x
```

---

## 9. Create Kubernetes Secrets

```bash
# Set required environment variables
RDS_HOST=$(terraform -chdir=userservice/terraform output -raw rds_endpoint)
REDIS_HOST=$(terraform -chdir=userservice/terraform output -raw redis_endpoint)
DB_PASSWORD=$(aws secretsmanager get-secret-value \
  --secret-id userservice-eks/db-password \
  --query SecretString --output text | jq -r '.password')

JWT_SECRET="your-sds-jwt-secret-at-least-32-characters-long"
ENTERPRISE_JWT_SECRET="your-grpc-jwt-secret-at-least-32-chars"

# Create namespace
kubectl create namespace userservice

# Create main secrets
kubectl create secret generic userservice-secrets \
  --namespace userservice \
  --from-literal=jwt-secret="${JWT_SECRET}" \
  --from-literal=enterprise-jwt-secret="${ENTERPRISE_JWT_SECRET}" \
  --from-literal=config-username=config-user \
  --from-literal=config-password=config-pass \
  --from-literal=eureka-username=eureka \
  --from-literal=eureka-password=eureka \
  --from-literal=db-username=dbadmin \
  --from-literal=db-password="${DB_PASSWORD}" \
  --from-literal=db-host="${RDS_HOST}" \
  --from-literal=redis-host="${REDIS_HOST}"
```

---

## 10. Deploy with Helm

```bash
ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
ECR_ENDPOINT="${ACCOUNT_ID}.dkr.ecr.us-east-1.amazonaws.com"
TAG=$(git rev-parse --short HEAD)

# Deploy userservice Helm chart
helm upgrade --install userservice userservice/helm/ \
  --namespace userservice \
  --create-namespace \
  --values deployment/environments/prod/userservice-values.yaml \
  --set "global.imageRegistry=${ECR_ENDPOINT}" \
  --set "config-server.image.tag=${TAG}" \
  --set "eureka-server.image.tag=${TAG}" \
  --set "api-gateway.image.tag=${TAG}" \
  --set "auth-service.image.tag=${TAG}" \
  --set "user-service.image.tag=${TAG}" \
  --set "order-service.image.tag=${TAG}" \
  --set "product-service.image.tag=${TAG}" \
  --set "user-grpc-service.image.tag=${TAG}" \
  --set "financial-service.image.tag=${TAG}" \
  --set "health-service.image.tag=${TAG}" \
  --set "social-service.image.tag=${TAG}" \
  --set "enterprise-ui.image.tag=${TAG}" \
  --timeout 20m \
  --wait \
  --atomic

# Or use the deployment script:
./deployment/scripts/05-helm-deploy.sh us prod "${TAG}"
```

---

## 11. Post-Deployment Database Setup

The RDS instance starts with only the default `postgres` database. Additional databases must be created for the four gRPC-origin services:

```bash
# Port-forward RDS through a running pod (psql not available locally)
kubectl run psql-client --rm -it --image=postgres:15 \
  --namespace userservice \
  --env="PGPASSWORD=${DB_PASSWORD}" \
  --command -- psql \
    --host="${RDS_HOST}" \
    --username=dbadmin \
    --dbname=postgres \
    --command="
      CREATE DATABASE grpcdb;
      CREATE DATABASE financialdb;
      CREATE DATABASE healthdb;
      CREATE DATABASE socialdb;
      GRANT ALL PRIVILEGES ON DATABASE grpcdb TO dbadmin;
      GRANT ALL PRIVILEGES ON DATABASE financialdb TO dbadmin;
      GRANT ALL PRIVILEGES ON DATABASE healthdb TO dbadmin;
      GRANT ALL PRIVILEGES ON DATABASE socialdb TO dbadmin;
    "

# Verify databases
kubectl run psql-client --rm -it --image=postgres:15 \
  --namespace userservice \
  --env="PGPASSWORD=${DB_PASSWORD}" \
  --command -- psql \
    --host="${RDS_HOST}" \
    --username=dbadmin \
    --dbname=postgres \
    --command="\l"
```

---

## 12. Verify Deployment

```bash
# Check all pods are Running
kubectl get pods -n userservice -o wide

# Check services
kubectl get svc -n userservice

# Health check core services
kubectl exec -n userservice deploy/config-server -- \
  wget -qO- http://localhost:8888/actuator/health | jq .

kubectl exec -n userservice deploy/eureka-server -- \
  wget -qO- http://localhost:8761/actuator/health | jq .

kubectl exec -n userservice deploy/api-gateway -- \
  wget -qO- http://localhost:8000/actuator/health | jq .

# Port-forward Eureka dashboard
kubectl port-forward svc/eureka-server 8761:8761 -n userservice &
# Open: http://localhost:8761 (expect 12 registered services)

# Get load balancer URL (if using AWS LB controller)
kubectl get svc api-gateway -n userservice -o jsonpath='{.status.loadBalancer.ingress[0].hostname}'

# Run full verification
./deployment/scripts/06-verify.sh us
```

---

## 13. Cost Estimate

Estimated monthly AWS cost for the default configuration (us-east-1, dev environment):

| Resource | Type | Qty | Est. Monthly Cost |
|---|---|---|---|
| EKS Cluster | Control plane | 1 | ~$73 |
| EKS Nodes | t3.large | 3 | ~$180 |
| NAT Gateway | Per hour + data | 1 | ~$35 |
| RDS PostgreSQL | db.t3.medium | 1 | ~$50 |
| ElastiCache Redis | cache.t3.micro | 1 | ~$15 |
| ECR Storage | Per GB | 12 repos | ~$5 |
| Data Transfer | Egress | Variable | ~$10 |
| **Total estimate** | | | **~$368/month** |

> **Cost reduction tips:**
> - Use Spot instances for non-prod nodes: set `capacity_type = "SPOT"` in node group
> - Scale down to 1 node for dev: `node_desired_size = 1`, `node_min_size = 1`
> - Use `db.t3.micro` for dev/staging RDS
> - Stop the cluster outside business hours with Karpenter or scheduled scaling

---

## 14. Teardown

```bash
# 1. Remove Helm release (keeps AWS infrastructure)
helm uninstall userservice -n userservice
kubectl delete namespace userservice

# 2. Destroy all AWS infrastructure (IRREVERSIBLE)
cd userservice/terraform
terraform destroy

# Or use the deployment script:
./deployment/scripts/teardown.sh us --destroy-infra

# 3. Remove Terraform state bucket (manual — Terraform won't delete it)
aws s3 rm s3://userservice-tfstate --recursive
aws s3api delete-bucket --bucket userservice-tfstate --region us-east-1

# 4. Remove DynamoDB lock table
aws dynamodb delete-table --table-name userservice-tflock --region us-east-1
```

---

## 15. Troubleshooting

### Terraform init fails — backend bucket not found

```
Error: Failed to get existing workspaces: S3 bucket "userservice-tfstate" does not exist
```

**Fix:** Run the [bootstrap commands](#3-bootstrap-s3--dynamodb-for-terraform-state) first.

### terraform apply fails — insufficient service quotas

```
Error: Error creating EKS Node Group: InvalidParameterException: ...
```

**Fix:** Request quota increase in AWS Service Quotas console for the affected service (EC2, EKS, etc.).

### kubectl not connecting after eks update-kubeconfig

```
error: You must be logged in to the server (Unauthorized)
```

**Fix:** Ensure the IAM user/role running kubectl is added to the EKS `aws-auth` ConfigMap:

```bash
kubectl edit configmap aws-auth -n kube-system
# Add your IAM user under mapUsers:
# - userarn: arn:aws:iam::ACCOUNT_ID:user/your-user
#   username: admin
#   groups: ["system:masters"]
```

### RDS connection refused from pods

**Check:** Security group allows inbound TCP 5432 from the EKS node security group.
**Check:** RDS is in the same VPC private subnets as EKS nodes.

```bash
# Test connectivity from within a pod
kubectl run test --rm -it --image=busybox --namespace userservice \
  --command -- nc -zv "${RDS_HOST}" 5432
```

### ECR pull fails — ImagePullBackOff

```
Failed to pull image "123456789.dkr.ecr.us-east-1.amazonaws.com/userservice/auth-service:latest"
```

**Fix:** The EKS node IAM role must have `AmazonEC2ContainerRegistryReadOnly` attached (this is done by Terraform). If still failing:

```bash
# Check node role policies
aws iam list-attached-role-policies --role-name userservice-eks-node-role
```

### Config server services not fetching config

**Symptom:** Services log `Could not locate PropertySource` or fall back to local defaults.
**Check:** The `SPRING_CLOUD_CONFIG_URI` env var points to `http://config-server:8888` (internal Kubernetes DNS).
**Check:** Services have the correct `SPRING_CLOUD_CONFIG_USERNAME` and `SPRING_CLOUD_CONFIG_PASSWORD` env vars from the `userservice-secrets` Secret.

### Services not registering with Eureka

**Check:** Eureka `password` in `config-repo/eureka-server.yml` is a BCrypt hash (not plain text).
The SecurityConfig.java in eureka-server uses `BCryptPasswordEncoder` — plain text passwords always fail BCrypt comparison.
