# ─────────────────────────────────────────────────────────────────────────────
# environments/prod/terraform.tfvars
# Configuración para entorno de producción
# ─────────────────────────────────────────────────────────────────────────────

project_name = "nordin"
environment  = "prod"
aws_region   = "us-east-1"

# ─── Red ──────────────────────────────────────────────────────────────────────
vpc_cidr             = "10.1.0.0/16"  # CIDR diferente a dev — evita conflictos
availability_zones   = ["us-east-1a", "us-east-1b", "us-east-1c"]
private_subnet_cidrs = ["10.1.1.0/24", "10.1.2.0/24", "10.1.3.0/24"]
public_subnet_cidrs  = ["10.1.101.0/24", "10.1.102.0/24", "10.1.103.0/24"]

# ─── EKS — instancias más grandes para prod ───────────────────────────────────
kubernetes_version = "1.29"
node_instance_type = "t3.large"   # 2 vCPU, 8 GB — más capacidad para prod
node_desired_count = 3
node_min_count     = 2
node_max_count     = 6

# ─── ECR ──────────────────────────────────────────────────────────────────────
ecr_image_retention_count = 10  # Mayor retención en prod para rollbacks
