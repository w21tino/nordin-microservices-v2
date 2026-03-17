# ─────────────────────────────────────────────────────────────────────────────
# modules/ecr/main.tf
#
# Crea un repositorio ECR por cada microservicio con:
#   - Política de retención (elimina imágenes antiguas automáticamente)
#   - Escaneo de vulnerabilidades en cada push
#   - Etiquetado inmutable (no se puede sobreescribir un tag existente en prod)
# ─────────────────────────────────────────────────────────────────────────────

resource "aws_ecr_repository" "services" {
  for_each = toset(var.services)

  name                 = "nordin/${each.value}"
  image_tag_mutability = var.environment == "prod" ? "IMMUTABLE" : "MUTABLE"

  # Escaneo automático de vulnerabilidades en cada push
  image_scanning_configuration {
    scan_on_push = true
  }

  tags = {
    Service = each.value
  }
}

# ─── Política de retención — elimina imágenes antiguas ────────────────────────
resource "aws_ecr_lifecycle_policy" "services" {
  for_each   = aws_ecr_repository.services
  repository = each.value.name

  policy = jsonencode({
    rules = [
      {
        rulePriority = 1
        description  = "Mantener solo las últimas ${var.ecr_image_retention_count} imágenes"
        selection = {
          tagStatus   = "any"
          countType   = "imageCountMoreThan"
          countNumber = var.ecr_image_retention_count
        }
        action = { type = "expire" }
      }
    ]
  })
}
