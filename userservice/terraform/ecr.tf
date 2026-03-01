locals {
  ecr_repositories = toset([
    "config-server",
    "eureka-server",
    "api-gateway",
    "auth-service",
    "user-service",
    "order-service",
    "product-service",
    "user-grpc-service",
    "financial-service",
    "health-service",
    "social-service",
    "enterprise-ui",
  ])
}

resource "aws_ecr_repository" "services" {
  for_each = local.ecr_repositories

  name                 = "userservice/${each.key}"
  image_tag_mutability = "MUTABLE"

  image_scanning_configuration {
    scan_on_push = true
  }

  tags = {
    Name = "userservice-${each.key}"
  }
}

resource "aws_ecr_lifecycle_policy" "services" {
  for_each = local.ecr_repositories

  repository = aws_ecr_repository.services[each.key].name

  policy = jsonencode({
    rules = [{
      rulePriority = 1
      description  = "Keep last ${var.ecr_image_retention_count} images"
      selection = {
        tagStatus   = "any"
        countType   = "imageCountMoreThan"
        countNumber = var.ecr_image_retention_count
      }
      action = { type = "expire" }
    }]
  })
}
