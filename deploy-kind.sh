#!/bin/bash
# ─────────────────────────────────────────────────────────────────────────────
# deploy-kind.sh — Despliega nordin-microservices-v2 en Kind
#
# USO:
#   ./deploy-kind.sh           → Despliegue completo
#   ./deploy-kind.sh --delete  → Eliminar todo
#
# PRERREQUISITOS:
#   - Kind instalado
#   - kubectl instalado
#   - Docker corriendo
#   - Imágenes construidas: ./build-images.sh
# ─────────────────────────────────────────────────────────────────────────────

set -e

CLUSTER_NAME="nordin-v2"
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

log_info()    { echo -e "${GREEN}[INFO]${NC} $1"; }
log_warn()    { echo -e "${YELLOW}[WARN]${NC} $1"; }
log_error()   { echo -e "${RED}[ERROR]${NC} $1"; }

# ─── Eliminar cluster ────────────────────────────────────────────────────────
if [ "$1" == "--delete" ]; then
    log_warn "Eliminando cluster $CLUSTER_NAME..."
    kind delete cluster --name $CLUSTER_NAME
    log_info "Cluster eliminado"
    exit 0
fi

# ─── Crear cluster Kind ──────────────────────────────────────────────────────
log_info "Creando cluster Kind: $CLUSTER_NAME"

cat <<EOF | kind create cluster --name $CLUSTER_NAME --config=-
kind: Cluster
apiVersion: kind.x-k8s.io/v1alpha4
nodes:
  - role: control-plane
    extraPortMappings:
      # API Gateway
      - containerPort: 30080
        hostPort: 8080
        protocol: TCP
      # Grafana
      - containerPort: 30300
        hostPort: 3000
        protocol: TCP
EOF

log_info "Cluster creado"

# ─── Cargar imágenes en Kind ─────────────────────────────────────────────────
log_info "Cargando imágenes Docker en Kind..."

for service in auth-service employee-service department-service organization-service api-gateway admin-server; do
    if docker image inspect $service:latest &>/dev/null; then
        kind load docker-image $service:latest --name $CLUSTER_NAME
        log_info "  ✓ $service:latest cargada"
    else
        log_warn "  ⚠ $service:latest no encontrada — ejecuta ./build-images.sh primero"
    fi
done

# ─── Aplicar Secrets (primero — los deployments los referencian) ─────────────
log_info "Aplicando Secrets..."
kubectl apply -f k8s/auth-service/secret.yml
kubectl apply -f k8s/employee-service/secret.yml
kubectl apply -f k8s/department-service/secret.yml
kubectl apply -f k8s/organization-service/secret.yml
kubectl apply -f k8s/api-gateway/secret.yml

# ─── Aplicar ConfigMaps ───────────────────────────────────────────────────────
log_info "Aplicando ConfigMaps..."
kubectl apply -f k8s/auth-service/configmap.yml
kubectl apply -f k8s/employee-service/configmap.yml
kubectl apply -f k8s/department-service/configmap.yml
kubectl apply -f k8s/organization-service/configmap.yml
kubectl apply -f k8s/api-gateway/configmap.yml

# ─── Infraestructura ─────────────────────────────────────────────────────────
log_info "Desplegando bases de datos..."
kubectl apply -f k8s/infrastructure/postgres/emp-db.yml
kubectl apply -f k8s/infrastructure/postgres/dept-db.yml
kubectl apply -f k8s/infrastructure/postgres/org-auth-db.yml

log_info "Desplegando Redis..."
kubectl apply -f k8s/infrastructure/redis/redis.yml

log_info "Desplegando Zipkin..."
kubectl apply -f k8s/infrastructure/zipkin/zipkin.yml

log_info "Desplegando Prometheus..."
kubectl apply -f k8s/infrastructure/prometheus/prometheus.yml

log_info "Desplegando Loki + Promtail..."
kubectl apply -f k8s/infrastructure/loki/loki.yml
kubectl apply -f k8s/infrastructure/loki/promtail.yml

log_info "Desplegando Grafana..."
kubectl apply -f k8s/infrastructure/grafana/grafana.yml

# ─── Esperar bases de datos ───────────────────────────────────────────────────
log_info "Esperando bases de datos..."
kubectl wait --for=condition=ready pod -l app=emp-db  --timeout=120s
kubectl wait --for=condition=ready pod -l app=dept-db --timeout=120s
kubectl wait --for=condition=ready pod -l app=org-db  --timeout=120s
kubectl wait --for=condition=ready pod -l app=auth-db --timeout=120s
kubectl wait --for=condition=ready pod -l app=redis   --timeout=120s
log_info "Bases de datos listas"

# ─── Microservicios (orden: leaf → orquestadores) ────────────────────────────
log_info "Desplegando microservicios..."

kubectl apply -f k8s/auth-service/deployment.yml
kubectl apply -f k8s/auth-service/service.yml

kubectl apply -f k8s/employee-service/deployment.yml
kubectl apply -f k8s/employee-service/service.yml

kubectl apply -f k8s/department-service/deployment.yml
kubectl apply -f k8s/department-service/service.yml

kubectl apply -f k8s/organization-service/deployment.yml
kubectl apply -f k8s/organization-service/service.yml

kubectl apply -f k8s/api-gateway/deployment.yml
kubectl apply -f k8s/api-gateway/service.yml

# ─── Esperar microservicios ───────────────────────────────────────────────────
log_info "Esperando microservicios (puede tardar ~2 minutos)..."
for svc in auth-service employee-service department-service organization-service api-gateway; do
    kubectl wait --for=condition=ready pod -l app=$svc --timeout=180s
    log_info "  ✓ $svc listo"
done

# ─── Resumen ─────────────────────────────────────────────────────────────────
echo ""
echo -e "${GREEN}════════════════════════════════════════${NC}"
echo -e "${GREEN}  nordin-microservices-v2 desplegado ✅  ${NC}"
echo -e "${GREEN}════════════════════════════════════════${NC}"
echo ""
echo "  API Gateway:  http://localhost:8080"
echo "  Swagger UI:   http://localhost:8080/swagger-ui.html"
echo "  Grafana:      http://localhost:3000  (admin/admin)"
echo ""
echo "  Ver todos los pods:"
echo "  kubectl get pods"
echo ""
echo "  Ver logs de un servicio:"
echo "  kubectl logs -f deployment/employee-service"
echo ""
