# ─────────────────────────────────────────────────────────────────────────────
# main.tf — Infraestructura AWS para nordin-microservices-v2
#
# Recursos creados:
#   - VPC con subnets públicas y privadas en 2 AZs
#   - Internet Gateway + NAT Gateway
#   - EKS Cluster con Node Group gestionado
#   - ECR: un repositorio por microservicio
#   - IAM Roles para EKS y nodos
#   - Security Groups
#
# USO:
#   cd terraform/aws-eks/environments/dev
#   terraform init
#   terraform plan
#   terraform apply
# ─────────────────────────────────────────────────────────────────────────────

terraform {
  required_version = ">= 1.6.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
    kubernetes = {
      source  = "hashicorp/kubernetes"
      version = "~> 2.0"
    }
  }

  # ─── Backend S3 — estado remoto compartido ────────────────────────────────
  # Descomentar y configurar antes de usar en equipo
  # backend "s3" {
  #   bucket         = "nordin-terraform-state"
  #   key            = "nordin-microservices-v2/terraform.tfstate"
  #   region         = "us-east-1"
  #   dynamodb_table = "nordin-terraform-locks"
  #   encrypt        = true
  # }
}

provider "aws" {
  region = var.aws_region

  default_tags {
    tags = {
      Project     = var.project_name
      Environment = var.environment
      ManagedBy   = "terraform"
    }
  }
}

# ─── Módulo VPC ───────────────────────────────────────────────────────────────
module "vpc" {
  source = "./modules/vpc"

  project_name         = var.project_name
  environment          = var.environment
  vpc_cidr             = var.vpc_cidr
  availability_zones   = var.availability_zones
  private_subnet_cidrs = var.private_subnet_cidrs
  public_subnet_cidrs  = var.public_subnet_cidrs
}

# ─── Módulo EKS ───────────────────────────────────────────────────────────────
module "eks" {
  source = "./modules/eks"

  project_name        = var.project_name
  environment         = var.environment
  kubernetes_version  = var.kubernetes_version
  vpc_id              = module.vpc.vpc_id
  private_subnet_ids  = module.vpc.private_subnet_ids
  node_instance_type  = var.node_instance_type
  node_desired_count  = var.node_desired_count
  node_min_count      = var.node_min_count
  node_max_count      = var.node_max_count

  depends_on = [module.vpc]
}

# ─── Módulo ECR ───────────────────────────────────────────────────────────────
module "ecr" {
  source = "./modules/ecr"

  project_name              = var.project_name
  environment               = var.environment
  services                  = var.services
  ecr_image_retention_count = var.ecr_image_retention_count
}
