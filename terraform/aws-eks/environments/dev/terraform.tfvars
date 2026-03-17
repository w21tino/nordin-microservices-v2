# ─────────────────────────────────────────────────────────────────────────────
# environments/dev/terraform.tfvars
# Configuración para entorno de desarrollo
# ─────────────────────────────────────────────────────────────────────────────

project_name = "nordin"
environment  = "dev"
aws_region   = "us-east-1"

# ─── Red ──────────────────────────────────────────────────────────────────────
vpc_cidr             = "10.0.0.0/16"
availability_zones   = ["us-east-1a", "us-east-1b"]
private_subnet_cidrs = ["10.0.1.0/24", "10.0.2.0/24"]
public_subnet_cidrs  = ["10.0.101.0/24", "10.0.102.0/24"]

# ─── EKS — instancias pequeñas para dev ──────────────────────────────────────
kubernetes_version = "1.29"
node_instance_type = "t3.medium"  # 2 vCPU, 4 GB — económico para desarrollo
node_desired_count = 2
node_min_count     = 1
node_max_count     = 3

# ─── ECR ──────────────────────────────────────────────────────────────────────
ecr_image_retention_count = 5  # Menos retención en dev
