aws_region  = "us-east-1"
environment = "production"

vpc_cidr             = "10.0.0.0/16"
availability_zones   = ["us-east-1a", "us-east-1b", "us-east-1c"]
private_subnet_cidrs = ["10.0.1.0/24", "10.0.2.0/24", "10.0.3.0/24"]
public_subnet_cidrs  = ["10.0.101.0/24", "10.0.102.0/24", "10.0.103.0/24"]

eks_cluster_version     = "1.28"
eks_node_instance_types = ["t3.medium"]
eks_node_desired_size   = 3
eks_node_min_size       = 2
eks_node_max_size       = 5

db_instance_class = "db.t3.medium"
db_name           = "grpcdb"
db_username       = "grpcadmin"
# db_password should be passed via TF_VAR_db_password or -var flag, never committed

ecr_image_retention_count = 20
