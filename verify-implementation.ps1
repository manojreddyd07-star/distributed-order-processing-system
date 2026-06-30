# Order Completion Feature Verification Script (PowerShell)
# This script helps verify the order completion workflow implementation on Windows

Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "Order Completion Feature Verification" -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host ""

# Configuration
$FULFILLMENT_SERVICE_URL = "http://localhost:8084/api/fulfillments"
$KAFKA_TOPIC = "order-completed"

Write-Host "Step 1: Checking Fulfillment Service..." -ForegroundColor Yellow
Write-Host "==========================================" -ForegroundColor Yellow

# Check if fulfillment service is running
try {
    $response = Invoke-WebRequest -Uri $FULFILLMENT_SERVICE_URL -Method Get -TimeoutSec 5 -ErrorAction Stop
    Write-Host "[OK] Fulfillment service is running on port 8084" -ForegroundColor Green
} catch {
    Write-Host "[ERROR] Fulfillment service is not accessible" -ForegroundColor Red
    Write-Host "  Please start the fulfillment service first" -ForegroundColor Gray
}

Write-Host ""
Write-Host "Step 2: Testing API Endpoints..." -ForegroundColor Yellow
Write-Host "==========================================" -ForegroundColor Yellow

# Test GET all fulfillments
Write-Host "Testing GET /api/fulfillments..." -ForegroundColor Gray
try {
    $fulfillmentsResponse = Invoke-RestMethod -Uri $FULFILLMENT_SERVICE_URL -Method Get
    Write-Host "[OK] GET /api/fulfillments - Status: 200 OK" -ForegroundColor Green
    $fulfillmentCount = $fulfillmentsResponse.Count
    Write-Host "  Found $fulfillmentCount fulfillments" -ForegroundColor Gray
} catch {
    Write-Host "[ERROR] GET /api/fulfillments - Failed" -ForegroundColor Red
    Write-Host "  $($_.Exception.Message)" -ForegroundColor Gray
}

# Test GET audit history
Write-Host ""
Write-Host "Testing GET /api/fulfillments/history..." -ForegroundColor Gray
try {
    $historyResponse = Invoke-RestMethod -Uri "$FULFILLMENT_SERVICE_URL/history" -Method Get
    Write-Host "[OK] GET /api/fulfillments/history - Status: 200 OK" -ForegroundColor Green
    $auditCount = $historyResponse.Count
    Write-Host "  Found $auditCount audit log entries" -ForegroundColor Gray
} catch {
    Write-Host "[ERROR] GET /api/fulfillments/history - Failed" -ForegroundColor Red
    Write-Host "  $($_.Exception.Message)" -ForegroundColor Gray
}

Write-Host ""
Write-Host "Step 3: Checking Database..." -ForegroundColor Yellow
Write-Host "==========================================" -ForegroundColor Yellow

# Check if PostgreSQL client is available
$psqlPath = Get-Command psql -ErrorAction SilentlyContinue
if ($psqlPath) {
    Write-Host "Checking database tables..." -ForegroundColor Gray
    
    $env:PGPASSWORD = "postgres"
    $tableCheck = psql -h localhost -p 5436 -U postgres -d fulfillmentdb -t -c "SELECT EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'fulfillment_audit_log');" 2>$null
    
    if ($tableCheck -like "*t*") {
        Write-Host "[OK] fulfillment_audit_log table exists" -ForegroundColor Green
        
        $auditDbCount = psql -h localhost -p 5436 -U postgres -d fulfillmentdb -t -c "SELECT COUNT(*) FROM fulfillment_audit_log;" 2>$null
        $auditDbCount = $auditDbCount.Trim()
        Write-Host "  Database has $auditDbCount audit log records" -ForegroundColor Gray
    } else {
        Write-Host "[ERROR] fulfillment_audit_log table not found" -ForegroundColor Red
        Write-Host "  Run Flyway migrations to create the table" -ForegroundColor Gray
    }
    
    Remove-Item Env:PGPASSWORD
} else {
    Write-Host "[WARN] psql command not found, skipping database checks" -ForegroundColor Yellow
    Write-Host "  Install PostgreSQL client tools to enable database verification" -ForegroundColor Gray
}

Write-Host ""
Write-Host "Step 4: Checking Docker Services..." -ForegroundColor Yellow
Write-Host "==========================================" -ForegroundColor Yellow

# Check if Docker is running
try {
    $dockerInfo = docker info 2>$null
    if ($LASTEXITCODE -eq 0) {
        Write-Host "[OK] Docker is running" -ForegroundColor Green
        
        # Check Kafka container
        $kafkaContainer = docker ps --filter "name=kafka" --format "{{.Names}}" 2>$null
        if ($kafkaContainer) {
            Write-Host "[OK] Kafka container is running: $kafkaContainer" -ForegroundColor Green
        } else {
            Write-Host "[WARN] Kafka container not found" -ForegroundColor Yellow
            Write-Host "  Start Kafka: docker-compose up -d kafka" -ForegroundColor Gray
        }
        
        # Check PostgreSQL container
        $postgresContainer = docker ps --filter "name=postgres" --format "{{.Names}}" 2>$null
        if ($postgresContainer) {
            Write-Host "[OK] PostgreSQL container is running: $postgresContainer" -ForegroundColor Green
        } else {
            Write-Host "[WARN] PostgreSQL container not found" -ForegroundColor Yellow
            Write-Host "  Start PostgreSQL: docker-compose up -d postgres" -ForegroundColor Gray
        }
    } else {
        Write-Host "[WARN] Docker is not running" -ForegroundColor Yellow
    }
} catch {
    Write-Host "[WARN] Docker command not found" -ForegroundColor Yellow
    Write-Host "  Install Docker Desktop to manage containers" -ForegroundColor Gray
}

Write-Host ""
Write-Host "Step 5: Sample Data Verification..." -ForegroundColor Yellow
Write-Host "==========================================" -ForegroundColor Yellow

# Get a sample fulfillment
Write-Host "Fetching sample fulfillment data..." -ForegroundColor Gray
try {
    $sampleFulfillments = Invoke-RestMethod -Uri $FULFILLMENT_SERVICE_URL -Method Get
    if ($sampleFulfillments.Count -gt 0) {
        $sample = $sampleFulfillments[0]
        Write-Host "[OK] Sample fulfillment data:" -ForegroundColor Green
        Write-Host "  Fulfillment ID:    $($sample.fulfillmentId)" -ForegroundColor Gray
        Write-Host "  Order ID:          $($sample.orderId)" -ForegroundColor Gray
        Write-Host "  Customer ID:       $($sample.customerId)" -ForegroundColor Gray
        Write-Host "  Tracking Number:   $($sample.trackingNumber)" -ForegroundColor Cyan
        Write-Host "  Status:            $($sample.fulfillmentStatus)" -ForegroundColor Gray
        Write-Host "  Created At:        $($sample.createdAt)" -ForegroundColor Gray
    } else {
        Write-Host "[WARN] No fulfillment data available yet" -ForegroundColor Yellow
        Write-Host "  Create an order to trigger fulfillment creation" -ForegroundColor Gray
    }
} catch {
    Write-Host "[ERROR] Failed to fetch sample data" -ForegroundColor Red
    Write-Host "  $($_.Exception.Message)" -ForegroundColor Gray
}

Write-Host ""
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "Verification Complete" -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host ""

# Summary
Write-Host "Summary:" -ForegroundColor Cyan
Write-Host "--------" -ForegroundColor Cyan
Write-Host ""
Write-Host "Backend Implementation:" -ForegroundColor Yellow
Write-Host "  - OrderCompletedEvent: Created" -ForegroundColor Gray
Write-Host "  - FulfillmentEventProducer: Created" -ForegroundColor Gray
Write-Host "  - KafkaProducerConfig: Created" -ForegroundColor Gray
Write-Host "  - FulfillmentAuditLog: Created" -ForegroundColor Gray
Write-Host "  - Database Migration: Created" -ForegroundColor Gray
Write-Host ""
Write-Host "Frontend Implementation:" -ForegroundColor Yellow
Write-Host "  - fulfillmentApi: Updated" -ForegroundColor Gray
Write-Host "  - FulfillmentTable: Updated with API integration" -ForegroundColor Gray
Write-Host "  - Refresh Button: Implemented" -ForegroundColor Gray
Write-Host "  - Notifications: Implemented" -ForegroundColor Gray
Write-Host ""
Write-Host "Next Steps:" -ForegroundColor Yellow
Write-Host "1. Create an order to test the complete workflow" -ForegroundColor Gray
Write-Host "2. Check Kafka logs for OrderCompletedEvent" -ForegroundColor Gray
Write-Host "3. Verify audit logs in database" -ForegroundColor Gray
Write-Host "4. Test frontend refresh functionality" -ForegroundColor Gray
Write-Host ""
Write-Host "For detailed verification steps, see VERIFICATION_GUIDE.md" -ForegroundColor Cyan
Write-Host ""
