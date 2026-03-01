# --- DB Subnet Group ---

resource "aws_db_subnet_group" "main" {
  name       = "${var.cluster_name}-db-subnet-group"
  subnet_ids = aws_subnet.private[*].id

  tags = {
    Name = "${var.cluster_name}-db-subnet-group"
  }
}

# --- RDS PostgreSQL Instance ---
# Single instance hosts all per-service databases:
#   postgres (default), userdb, orderdb, productdb,
#   grpcdb, financialdb, healthdb, socialdb

resource "aws_db_instance" "main" {
  identifier     = "${var.cluster_name}-postgres"
  engine         = "postgres"
  engine_version = "15"

  instance_class        = var.db_instance_class
  allocated_storage     = 50
  max_allocated_storage = 200
  storage_type          = "gp3"

  db_name              = "postgres"
  username             = "dbadmin"
  password             = random_password.db_password.result
  parameter_group_name = "default.postgres15"
  skip_final_snapshot  = true
  multi_az             = false

  db_subnet_group_name   = aws_db_subnet_group.main.name
  vpc_security_group_ids = [aws_security_group.rds.id]

  tags = {
    Name = "${var.cluster_name}-postgres"
  }
}

resource "random_password" "db_password" {
  length           = 24
  special          = true
  override_special = "!#$%&*()-_=+[]{}<>:?"
}

# Store generated password in Secrets Manager
resource "aws_secretsmanager_secret" "db_password" {
  name                    = "${var.cluster_name}/db-password"
  recovery_window_in_days = 7

  tags = {
    Name = "${var.cluster_name}-db-password"
  }
}

resource "aws_secretsmanager_secret_version" "db_password" {
  secret_id = aws_secretsmanager_secret.db_password.id
  secret_string = jsonencode({
    username = "dbadmin"
    password = random_password.db_password.result
    host     = aws_db_instance.main.address
    port     = 5432
  })
}
