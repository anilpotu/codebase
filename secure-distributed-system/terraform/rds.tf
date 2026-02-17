# --- DB Subnet Group ---

resource "aws_db_subnet_group" "main" {
  name       = "${var.cluster_name}-db-subnet-group"
  subnet_ids = aws_subnet.private[*].id

  tags = {
    Name = "${var.cluster_name}-db-subnet-group"
  }
}

# --- Random Password for RDS ---

resource "random_password" "db_password" {
  length           = 24
  special          = true
  override_special = "!#$%&*()-_=+[]{}<>:?"
}

# --- RDS PostgreSQL Instance ---

resource "aws_db_instance" "main" {
  identifier     = "${var.cluster_name}-postgres"
  engine         = "postgres"
  engine_version = "15"

  instance_class        = var.db_instance_class
  allocated_storage     = 20
  storage_type          = "gp3"
  db_name               = "securedb"
  username              = "dbadmin"
  password              = random_password.db_password.result
  parameter_group_name  = "default.postgres15"
  skip_final_snapshot   = true
  multi_az              = false

  db_subnet_group_name   = aws_db_subnet_group.main.name
  vpc_security_group_ids = [aws_security_group.rds.id]

  tags = {
    Name = "${var.cluster_name}-postgres"
  }
}
