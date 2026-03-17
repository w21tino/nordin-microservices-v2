variable "project_name"              { type = string }
variable "environment"                { type = string }
variable "services"                   { type = list(string) }
variable "ecr_image_retention_count"  { type = number }
