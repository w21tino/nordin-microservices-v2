# ─────────────────────────────────────────────────────────────────────────────
# outputs.tf — Valores exportados después de terraform apply
# Usados por el Jenkinsfile para conectarse al cluster
# ─────────────────────────────────────────────────────────────────────────────

output "vpc_id" {
  description = "ID de la VPC creada"
  value       = module.vpc.vpc_id
}

output "eks_cluster_name" {
  description = "Nombre del cluster EKS"
  value       = module.eks.cluster_name
}

output "eks_cluster_endpoint" {
  description = "Endpoint de la API de Kubernetes"
  value       = module.eks.cluster_endpoint
}

output "eks_cluster_version" {
  description = "Versión de Kubernetes del cluster"
  value       = module.eks.cluster_version
}

output "ecr_registry_url" {
  description = "URL base del registro ECR"
  value       = module.ecr.registry_url
}

output "ecr_repository_urls" {
  description = "URLs de los repositorios ECR por servicio"
  value       = module.ecr.repository_urls
}

output "configure_kubectl" {
  description = "Comando para configurar kubectl con el cluster EKS"
  value       = "aws eks update-kubeconfig --region ${var.aws_region} --name ${module.eks.cluster_name}"
}
