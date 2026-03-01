# --- ElastiCache Subnet Group ---

resource "aws_elasticache_subnet_group" "main" {
  name       = "${var.cluster_name}-redis-subnet-group"
  subnet_ids = aws_subnet.private[*].id

  tags = {
    Name = "${var.cluster_name}-redis-subnet-group"
  }
}

# --- ElastiCache Redis (single node, no replication for dev/staging) ---

resource "aws_elasticache_replication_group" "main" {
  replication_group_id = "${var.cluster_name}-redis"
  description          = "Redis for userservice (rate-limiting + caching)"

  node_type              = var.redis_node_type
  num_cache_clusters     = 1
  engine_version         = "7.0"
  port                   = 6379
  parameter_group_name   = "default.redis7"

  automatic_failover_enabled = false
  transit_encryption_enabled = false

  subnet_group_name  = aws_elasticache_subnet_group.main.name
  security_group_ids = [aws_security_group.redis.id]

  tags = {
    Name = "${var.cluster_name}-redis"
  }
}
