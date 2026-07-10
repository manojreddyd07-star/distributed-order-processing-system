#!/bin/bash
# Monitoring Service Verification Script
# This script verifies that the monitoring service is properly configured and running

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

echo -e "${CYAN}=====================================${NC}"
echo -e "${CYAN}Monitoring Service Verification${NC}"
echo -e "${CYAN}=====================================${NC}"
echo ""

all_passed=true

# Function to test endpoint
test_endpoint() {
    local name=$1
    local url=$2
    
    echo -n -e "${YELLOW}Testing $name...${NC}"
    
    if curl -s -f -o /dev/null -w "%{http_code}" "$url" --max-time 5 | grep -q "200"; then
        echo -e " ${GREEN}✓ PASSED${NC}"
        return 0
    else
        echo -e " ${RED}✗ FAILED${NC}"
        return 1
    fi
}

# Function to check Docker container
test_container() {
    local container_name=$1
    
    echo -n -e "${YELLOW}Checking container: $container_name...${NC}"
    
    if docker ps --filter "name=$container_name" --format "{{.Names}}" | grep -q "^$container_name$"; then
        echo -e " ${GREEN}✓ RUNNING${NC}"
        return 0
    else
        echo -e " ${RED}✗ NOT RUNNING${NC}"
        return 1
    fi
}

echo -e "${CYAN}1. DOCKER CONTAINERS${NC}"
echo -e "${CYAN}--------------------${NC}"

# Check Docker containers
containers=(
    "monitoring-service"
    "monitoringdb"
    "order-service"
    "validation-service"
    "payment-service"
    "inventory-service"
    "fulfillment-service"
    "kafka"
    "zookeeper"
)

for container in "${containers[@]}"; do
    if ! test_container "$container"; then
        all_passed=false
    fi
done

echo ""
echo -e "${CYAN}2. ACTUATOR HEALTH ENDPOINTS${NC}"
echo -e "${CYAN}----------------------------${NC}"

# Check actuator health endpoints
declare -A health_endpoints=(
    ["Monitoring Service"]="http://localhost:8086/actuator/health"
    ["Order Service"]="http://localhost:8080/actuator/health"
    ["Validation Service"]="http://localhost:8081/actuator/health"
    ["Payment Service"]="http://localhost:8082/actuator/health"
    ["Inventory Service"]="http://localhost:8083/actuator/health"
    ["Fulfillment Service"]="http://localhost:8084/actuator/health"
)

for name in "${!health_endpoints[@]}"; do
    if ! test_endpoint "$name" "${health_endpoints[$name]}"; then
        all_passed=false
    fi
done

echo ""
echo -e "${CYAN}3. MONITORING API ENDPOINTS${NC}"
echo -e "${CYAN}----------------------------${NC}"

if ! test_endpoint "Health Endpoint" "http://localhost:8086/api/monitoring/health"; then
    all_passed=false
fi

if ! test_endpoint "Metrics Endpoint" "http://localhost:8086/api/monitoring/metrics"; then
    all_passed=false
fi

echo ""
echo -e "${CYAN}4. DETAILED HEALTH CHECK${NC}"
echo -e "${CYAN}------------------------${NC}"

health_data=$(curl -s http://localhost:8086/api/monitoring/health 2>/dev/null)

if [ -n "$health_data" ]; then
    overall_status=$(echo "$health_data" | grep -o '"overallStatus":"[^"]*"' | cut -d'"' -f4)
    db_status=$(echo "$health_data" | grep -o '"databaseHealth":{[^}]*"status":"[^"]*"' | grep -o '"status":"[^"]*"' | cut -d'"' -f4)
    kafka_status=$(echo "$health_data" | grep -o '"kafkaHealth":{[^}]*"status":"[^"]*"' | grep -o '"status":"[^"]*"' | cut -d'"' -f4)
    
    echo -n "Overall Status: "
    if [ "$overall_status" = "UP" ]; then
        echo -e "${GREEN}$overall_status${NC}"
    else
        echo -e "${RED}$overall_status${NC}"
        all_passed=false
    fi
    
    echo -n "Database Health: "
    if [ "$db_status" = "UP" ]; then
        echo -e "${GREEN}$db_status${NC}"
    else
        echo -e "${RED}$db_status${NC}"
        all_passed=false
    fi
    
    echo -n "Kafka Health: "
    if [ "$kafka_status" = "UP" ]; then
        echo -e "${GREEN}$kafka_status${NC}"
    else
        echo -e "${RED}$kafka_status${NC}"
        all_passed=false
    fi
else
    echo -e "${RED}Failed to retrieve detailed health data${NC}"
    all_passed=false
fi

echo ""
echo -e "${CYAN}5. METRICS CHECK${NC}"
echo -e "${CYAN}----------------${NC}"

metrics_data=$(curl -s http://localhost:8086/api/monitoring/metrics 2>/dev/null)

if [ -n "$metrics_data" ]; then
    echo -e "${GREEN}Metrics endpoint is responding${NC}"
else
    echo -e "${RED}Failed to retrieve metrics data${NC}"
    all_passed=false
fi

echo ""
echo -e "${CYAN}6. FILE STRUCTURE VERIFICATION${NC}"
echo -e "${CYAN}------------------------------${NC}"

# Check backend files
backend_files=(
    "backend/monitoring-service/pom.xml"
    "backend/monitoring-service/Dockerfile"
    "backend/monitoring-service/src/main/resources/application.yml"
    "backend/monitoring-service/src/main/java/com/project/monitoring/MonitoringServiceApplication.java"
    "backend/monitoring-service/src/main/java/com/project/monitoring/controller/MetricsController.java"
    "backend/monitoring-service/src/main/java/com/project/monitoring/service/MetricsService.java"
)

for file in "${backend_files[@]}"; do
    echo -n -e "${YELLOW}Checking $file...${NC}"
    if [ -f "$file" ]; then
        echo -e " ${GREEN}✓ EXISTS${NC}"
    else
        echo -e " ${RED}✗ MISSING${NC}"
        all_passed=false
    fi
done

# Check frontend files
frontend_files=(
    "frontend/src/pages/Monitoring/MonitoringPage.jsx"
    "frontend/src/pages/Monitoring/MonitoringPage.css"
    "frontend/src/components/monitoring/MetricsCard.jsx"
    "frontend/src/components/monitoring/HealthGrid.jsx"
    "frontend/src/services/monitoringApi.js"
)

for file in "${frontend_files[@]}"; do
    echo -n -e "${YELLOW}Checking $file...${NC}"
    if [ -f "$file" ]; then
        echo -e " ${GREEN}✓ EXISTS${NC}"
    else
        echo -e " ${RED}✗ MISSING${NC}"
        all_passed=false
    fi
done

echo ""
echo -e "${CYAN}=====================================${NC}"
echo -e "${CYAN}VERIFICATION RESULTS${NC}"
echo -e "${CYAN}=====================================${NC}"
echo ""

if [ "$all_passed" = true ]; then
    echo -e "${GREEN}✓ ALL CHECKS PASSED!${NC}"
    echo ""
    echo -e "${GREEN}Your monitoring service is properly configured and running.${NC}"
    echo ""
    echo -e "${YELLOW}Access the monitoring dashboard at:${NC}"
    echo -e "${CYAN}http://localhost:3000/monitoring${NC}"
    echo ""
    echo -e "${YELLOW}Opening dashboard in browser...${NC}"
    
    # Try to open browser
    if command -v xdg-open > /dev/null; then
        xdg-open "http://localhost:3000/monitoring" 2>/dev/null
    elif command -v open > /dev/null; then
        open "http://localhost:3000/monitoring" 2>/dev/null
    fi
else
    echo -e "${RED}✗ SOME CHECKS FAILED${NC}"
    echo ""
    echo -e "${YELLOW}Please review the failed checks above.${NC}"
    echo -e "${YELLOW}Common issues:${NC}"
    echo -e "  1. Services not started: Run ${NC}'docker-compose up -d'"
    echo -e "  2. Services still starting: Wait a few moments and try again"
    echo -e "  3. Port conflicts: Check if ports are already in use"
    echo ""
    echo -e "${YELLOW}For detailed logs, run:${NC}"
    echo -e "  ${NC}docker logs monitoring-service"
fi

echo ""
