#!/bin/bash
# ─────────────────────────────────────────────────────────────────────────────
# build-images.sh — Compila y construye imágenes Docker de todos los servicios
#
# USO:
#   ./build-images.sh              → Build de todos los servicios
#   ./build-images.sh employee     → Build de un servicio específico
# ─────────────────────────────────────────────────────────────────────────────

set -e

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

log_info() { echo -e "${GREEN}[INFO]${NC} $1"; }
log_warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }

# Paso 1: Compilar todos los módulos con Maven
log_info "Compilando proyecto con Maven..."
mvn clean package -DskipTests
log_info "Compilación completada"

# Servicios a construir
declare -A SERVICES=(
    ["auth-service"]="services/auth-service"
    ["employee-service"]="services/employee-service"
    ["department-service"]="services/department-service"
    ["organization-service"]="services/organization-service"
    ["api-gateway"]="infrastructure/api-gateway"
    ["admin-server"]="infrastructure/admin-server"
)

build_service() {
    local name=$1
    local path=$2
    log_info "Construyendo imagen: $name:latest"
    docker build -t $name:latest $path
    log_info "  ✓ $name:latest construida"
}

# Si se pasa un argumento, construir solo ese servicio
if [ -n "$1" ]; then
    if [[ -v SERVICES[$1] ]]; then
        build_service $1 ${SERVICES[$1]}
    else
        log_warn "Servicio no encontrado: $1"
        echo "Servicios disponibles: ${!SERVICES[@]}"
        exit 1
    fi
else
    # Construir todos
    for name in "${!SERVICES[@]}"; do
        build_service $name ${SERVICES[$name]}
    done
fi

echo ""
log_info "Imágenes construidas:"
docker images | grep -E "auth-service|employee-service|department-service|organization-service|api-gateway|admin-server"
