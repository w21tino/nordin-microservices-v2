#!/bin/bash
# ─────────────────────────────────────────────────────────────────────────────
# terraform.sh — Helper para ejecutar Terraform por entorno
#
# USO:
#   ./terraform/terraform.sh dev plan
#   ./terraform/terraform.sh dev apply
#   ./terraform/terraform.sh prod plan
#   ./terraform/terraform.sh prod destroy
# ─────────────────────────────────────────────────────────────────────────────

set -e

ENV=$1
CMD=$2

GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m'

if [ -z "$ENV" ] || [ -z "$CMD" ]; then
    echo "Uso: $0 <env> <comando>"
    echo "  env:     dev | prod"
    echo "  comando: init | plan | apply | destroy | output"
    exit 1
fi

if [ "$ENV" != "dev" ] && [ "$ENV" != "prod" ]; then
    echo -e "${RED}Entorno inválido: $ENV. Usar 'dev' o 'prod'${NC}"
    exit 1
fi

TERRAFORM_DIR="terraform/aws-eks"
TFVARS_FILE="environments/${ENV}/terraform.tfvars"

echo -e "${GREEN}[Terraform]${NC} Entorno: $ENV | Comando: $CMD"
echo ""

cd $TERRAFORM_DIR

case $CMD in
    init)
        terraform init
        ;;
    plan)
        terraform plan -var-file=$TFVARS_FILE
        ;;
    apply)
        if [ "$ENV" == "prod" ]; then
            echo -e "${RED}⚠️  Estás aplicando cambios en PRODUCCIÓN${NC}"
            read -p "¿Confirmas? (escribe 'prod' para continuar): " confirm
            if [ "$confirm" != "prod" ]; then
                echo "Cancelado"
                exit 0
            fi
        fi
        terraform apply -var-file=$TFVARS_FILE -auto-approve
        echo ""
        echo -e "${GREEN}✅ Apply completado${NC}"
        echo ""
        terraform output configure_kubectl
        ;;
    destroy)
        echo -e "${RED}⚠️  Esto ELIMINARÁ toda la infraestructura de $ENV${NC}"
        read -p "¿Confirmas? (escribe '$ENV' para continuar): " confirm
        if [ "$confirm" != "$ENV" ]; then
            echo "Cancelado"
            exit 0
        fi
        terraform destroy -var-file=$TFVARS_FILE -auto-approve
        ;;
    output)
        terraform output
        ;;
    *)
        echo "Comando inválido: $CMD"
        exit 1
        ;;
esac
