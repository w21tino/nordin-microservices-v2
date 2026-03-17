#!/bin/bash
# ─────────────────────────────────────────────────────────────────────────────
# setup-cicd.sh — Levanta SonarQube + Jenkins y guía la configuración inicial
#
# USO: ./ci-cd/setup-cicd.sh
# ─────────────────────────────────────────────────────────────────────────────

set -e

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

log_info()  { echo -e "${GREEN}[INFO]${NC}  $1"; }
log_step()  { echo -e "${CYAN}[STEP]${NC}  $1"; }
log_warn()  { echo -e "${YELLOW}[WARN]${NC}  $1"; }

# ─── Requisito: vm.max_map_count para SonarQube ───────────────────────────────
log_step "Configurando vm.max_map_count para SonarQube..."
sudo sysctl -w vm.max_map_count=524288
echo "vm.max_map_count=524288" | sudo tee -a /etc/sysctl.conf > /dev/null
log_info "vm.max_map_count configurado"

# ─── Levantar contenedores ────────────────────────────────────────────────────
log_step "Levantando SonarQube y Jenkins..."
docker compose -f ci-cd/docker-compose-cicd.yml up -d

# ─── Esperar SonarQube ────────────────────────────────────────────────────────
log_step "Esperando SonarQube (puede tardar ~60 segundos)..."
until curl -s http://localhost:9000/api/system/status | grep -q '"status":"UP"'; do
    echo -n "."
    sleep 5
done
echo ""
log_info "SonarQube UP"

# ─── Obtener password inicial de Jenkins ─────────────────────────────────────
log_step "Obteniendo password inicial de Jenkins..."
sleep 10
JENKINS_PASS=$(docker exec jenkins cat /var/jenkins_home/secrets/initialAdminPassword 2>/dev/null || echo "aún no disponible")

# ─── Instrucciones ────────────────────────────────────────────────────────────
echo ""
echo -e "${GREEN}════════════════════════════════════════════════════════${NC}"
echo -e "${GREEN}  CI/CD levantado ✅                                     ${NC}"
echo -e "${GREEN}════════════════════════════════════════════════════════${NC}"
echo ""
echo -e "${CYAN}── SonarQube ────────────────────────────────────────────${NC}"
echo "  URL:      http://localhost:9000"
echo "  Usuario:  admin"
echo "  Password: admin  (te pedirá cambiarlo)"
echo ""
echo -e "${CYAN}  Pasos en SonarQube:${NC}"
echo "  1. Entrar → cambiar password"
echo "  2. My Account → Security → Generate Token"
echo "     Name: jenkins-token"
echo "     Copiar el token → lo necesitas en Jenkins"
echo "  3. Quality Gates → Create → nordin-v2"
echo "     Agregar condición: Coverage < 70% → FAILED"
echo ""
echo -e "${CYAN}── Jenkins ──────────────────────────────────────────────${NC}"
echo "  URL:      http://localhost:8090"
echo "  Password: ${JENKINS_PASS}"
echo ""
echo -e "${CYAN}  Pasos en Jenkins:${NC}"
echo "  1. Instalar plugins sugeridos"
echo "  2. Plugins adicionales a instalar:"
echo "     - SonarQube Scanner"
echo "     - Docker Pipeline"
echo "     - Amazon ECR"
echo "     - AWS Credentials"
echo "     - JaCoCo"
echo "     - Pipeline: Multibranch"
echo ""
echo "  3. Manage Jenkins → System → SonarQube servers"
echo "     Name: SonarQube"
echo "     URL:  http://sonarqube:9000"
echo "     Token: (el que generaste en SonarQube)"
echo ""
echo "  4. Manage Jenkins → Credentials → Add:"
echo "     a) sonar-token   → Secret Text → token de SonarQube"
echo "     b) aws-credentials → AWS Credentials → Access Key + Secret"
echo "     c) kubeconfig-eks  → Secret File → kubeconfig de EKS"
echo ""
echo "  5. New Item → Multibranch Pipeline"
echo "     Branch Sources → GitHub → repo nordin-microservices-v2"
echo "     Build Configuration → Jenkinsfile path: ci-cd/Jenkinsfile"
echo ""
echo -e "${CYAN}── Verificar análisis manual ─────────────────────────────${NC}"
echo "  mvn clean verify sonar:sonar \\"
echo "    -Dsonar.host.url=http://localhost:9000 \\"
echo "    -Dsonar.token=<TU_TOKEN>"
echo ""
