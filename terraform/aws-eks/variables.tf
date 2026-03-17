# ─────────────────────────────────────────────────────────────────────────────
# variables.tf — Variables globales del proyecto
# Los valores se definen en environments/dev/terraform.tfvars
#                          o environments/prod/terraform.tfvars
# ─────────────────────────────────────────────────────────────────────────────

variable "project_name" {
  description = "Nombre del proyecto — se usa como prefijo en todos los recursos"
  type        = string
  default     = "nordin"
}

variable "environment" {
  description = "Entorno: dev o prod"
  type        = string
  validation {
    condition     = contains(["dev", "prod"], var.environment)
    error_message = "El entorno debe ser 'dev' o 'prod'."
  }
}

variable "aws_region" {
  description = "Región de AWS donde se desplegará la infraestructura"
  type        = string
  default     = "us-east-1"
}

# ─── VPC ─────────────────────────────────────────────────────────────────────

variable "vpc_cidr" {
  description = "CIDR block de la VPC"
  type        = string
  default     = "10.0.0.0/16"
}

variable "availability_zones" {
  description = "Zonas de disponibilidad — mínimo 2 para EKS"
  type        = list(string)
  default     = ["us-east-1a", "us-east-1b"]
}

variable "private_subnet_cidrs" {
  description = "CIDRs de subnets privadas (donde corren los nodos EKS)"
  type        = list(string)
  default     = ["10.0.1.0/24", "10.0.2.0/24"]
}

variable "public_subnet_cidrs" {
  description = "CIDRs de subnets públicas (donde corre el Load Balancer)"
  type        = list(string)
  default     = ["10.0.101.0/24", "10.0.102.0/24"]
}

# ─── EKS ─────────────────────────────────────────────────────────────────────

variable "kubernetes_version" {
  description = "Versión de Kubernetes para el cluster EKS"
  type        = string
  default     = "1.29"
}

variable "node_instance_type" {
  description = "Tipo de instancia EC2 para los nodos del cluster"
  type        = string
  default     = "t3.medium"
  # t3.medium: 2 vCPU, 4 GB RAM — suficiente para dev
  # t3.large:  2 vCPU, 8 GB RAM — recomendado para prod
}

variable "node_desired_count" {
  description = "Número deseado de nodos en el node group"
  type        = number
  default     = 2
}

variable "node_min_count" {
  description = "Número mínimo de nodos (autoscaling)"
  type        = number
  default     = 1
}

variable "node_max_count" {
  description = "Número máximo de nodos (autoscaling)"
  type        = number
  default     = 4
}

# ─── ECR ─────────────────────────────────────────────────────────────────────

variable "services" {
  description = "Lista de microservicios — se crea un repositorio ECR por cada uno"
  type        = list(string)
  default = [
    "auth-service",
    "employee-service",
    "department-service",
    "organization-service",
    "api-gateway",
    "admin-server"
  ]
}

variable "ecr_image_retention_count" {
  description = "Número de imágenes a retener en ECR antes de eliminar las más antiguas"
  type        = number
  default     = 10
}
