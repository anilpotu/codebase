# Low-cost profile for userservice AWS infrastructure (dev)
node_instance_type      = "t3.medium"
node_desired_size       = 1
node_min_size           = 1
node_max_size           = 2
node_capacity_type      = "SPOT"

db_instance_class       = "db.t3.micro"
db_allocated_storage    = 20
db_max_allocated_storage = 50

redis_node_type         = "cache.t3.micro"
ecr_image_retention_count = 5
