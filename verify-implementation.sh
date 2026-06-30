#!/bin/bash

# Order Completion Feature Verification Script
# This script helps verify the order completion workflow implementation

echo "=========================================="
echo "Order Completion Feature Verification"
echo "=========================================="
echo ""

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Configuration
FULFILLMENT_SERVICE_URL="http://localhost:8084/api/fulfillments"
KAFKA_TOPIC="order-completed"

echo "Step 1: Checking Fulfillment Service..."
echo "=========================================="

# Check if fulfillment service is running
if curl -s --max-time 5 "${FULFILLMENT_SERVICE_URL}" > /dev/null; then
    echo -e "${GREEN}✓${NC} Fulfillment service is running on port 8084"
else
    echo -e "${RED}✗${NC} Fulfillment service is not accessible"
    echo "  Please start the fulfillment service first"
fi

echo ""
echo "Step 2: Testing API Endpoints..."
echo "=========================================="

# Test GET all fulfillments
echo "Testing GET /api/fulfillments..."
RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" "${FULFILLMENT_SERVICE_URL}")
if [ "$RESPONSE" = "200" ]; then
    echo -e "${GREEN}✓${NC} GET /api/fulfillments - Status: 200 OK"
    FULFILLMENT_COUNT=$(curl -s "${FULFILLMENT_SERVICE_URL}" | jq '. | length')
    echo "  Found ${FULFILLMENT_COUNT} fulfillments"
else
    echo -e "${RED}✗${NC} GET /api/fulfillments - Status: ${RESPONSE}"
fi

# Test GET audit history
echo ""
echo "Testing GET /api/fulfillments/history..."
RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" "${FULFILLMENT_SERVICE_URL}/history")
if [ "$RESPONSE" = "200" ]; then
    echo -e "${GREEN}✓${NC} GET /api/fulfillments/history - Status: 200 OK"
    AUDIT_COUNT=$(curl -s "${FULFILLMENT_SERVICE_URL}/history" | jq '. | length')
    echo "  Found ${AUDIT_COUNT} audit log entries"
else
    echo -e "${RED}✗${NC} GET /api/fulfillments/history - Status: ${RESPONSE}"
fi

echo ""
echo "Step 3: Checking Database..."
echo "=========================================="

# Check if PostgreSQL is accessible
if command -v psql &> /dev/null; then
    echo "Checking database tables..."
    
    # Check if audit log table exists
    TABLE_EXISTS=$(PGPASSWORD=postgres psql -h localhost -p 5436 -U postgres -d fulfillmentdb -t -c "SELECT EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'fulfillment_audit_log');" 2>/dev/null)
    
    if [[ $TABLE_EXISTS == *"t"* ]]; then
        echo -e "${GREEN}✓${NC} fulfillment_audit_log table exists"
        
        # Count audit records
        AUDIT_DB_COUNT=$(PGPASSWORD=postgres psql -h localhost -p 5436 -U postgres -d fulfillmentdb -t -c "SELECT COUNT(*) FROM fulfillment_audit_log;" 2>/dev/null | xargs)
        echo "  Database has ${AUDIT_DB_COUNT} audit log records"
    else
        echo -e "${RED}✗${NC} fulfillment_audit_log table not found"
        echo "  Run Flyway migrations to create the table"
    fi
else
    echo -e "${YELLOW}!${NC} psql command not found, skipping database checks"
fi

echo ""
echo "Step 4: Checking Kafka..."
echo "=========================================="

if command -v kafka-topics &> /dev/null; then
    echo "Checking Kafka topic: ${KAFKA_TOPIC}..."
    
    # Check if topic exists
    kafka-topics --bootstrap-server localhost:9092 --list 2>/dev/null | grep -q "^${KAFKA_TOPIC}$"
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✓${NC} Kafka topic '${KAFKA_TOPIC}' exists"
    else
        echo -e "${YELLOW}!${NC} Kafka topic '${KAFKA_TOPIC}' not found"
        echo "  Topic will be created automatically when first event is published"
    fi
else
    echo -e "${YELLOW}!${NC} Kafka tools not found, skipping Kafka checks"
fi

echo ""
echo "Step 5: Sample Data Verification..."
echo "=========================================="

# Get a sample fulfillment
echo "Fetching sample fulfillment data..."
SAMPLE=$(curl -s "${FULFILLMENT_SERVICE_URL}" | jq '.[0]' 2>/dev/null)

if [ "$SAMPLE" != "null" ] && [ -n "$SAMPLE" ]; then
    echo -e "${GREEN}✓${NC} Sample fulfillment data:"
    echo "$SAMPLE" | jq '{
        fulfillmentId: .fulfillmentId,
        orderId: .orderId,
        customerId: .customerId,
        trackingNumber: .trackingNumber,
        status: .fulfillmentStatus,
        createdAt: .createdAt
    }'
else
    echo -e "${YELLOW}!${NC} No fulfillment data available yet"
    echo "  Create an order to trigger fulfillment creation"
fi

echo ""
echo "=========================================="
echo "Verification Complete"
echo "=========================================="
echo ""

# Summary
echo "Summary:"
echo "--------"
echo "Backend Implementation:"
echo "  - OrderCompletedEvent: Created"
echo "  - FulfillmentEventProducer: Created"
echo "  - KafkaProducerConfig: Created"
echo "  - FulfillmentAuditLog: Created"
echo "  - Database Migration: Created"
echo ""
echo "Frontend Implementation:"
echo "  - fulfillmentApi: Updated"
echo "  - FulfillmentTable: Updated with API integration"
echo "  - Refresh Button: Implemented"
echo "  - Notifications: Implemented"
echo ""
echo "Next Steps:"
echo "1. Create an order to test the complete workflow"
echo "2. Check Kafka logs for OrderCompletedEvent"
echo "3. Verify audit logs in database"
echo "4. Test frontend refresh functionality"
echo ""
