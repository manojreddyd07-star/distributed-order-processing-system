# Performance Metrics Verification Script
# This script verifies the Prometheus metrics integration and performance metrics endpoint

Write-Host "=====================================" -ForegroundColor Cyan
Write-Host "Performance Metrics Verification" -ForegroundColor Cyan
Write-Host "=====================================" -ForegroundColor Cyan
Write-Host ""

$monitoringServiceUrl = "http://localhost:8086"
$errors = @()
$warnings = @()
$successes = @()

# Function to test endpoint
function Test-Endpoint {
    param (
        [string]$Url,
        [string]$Description
    )
    
    Write-Host "Testing: $Description" -ForegroundColor Yellow
    Write-Host "URL: $Url" -ForegroundColor Gray
    
    try {
        $response = Invoke-WebRequest -Uri $Url -Method GET -TimeoutSec 10 -UseBasicParsing
        
        if ($response.StatusCode -eq 200) {
            Write-Host "✓ SUCCESS: $Description (Status: $($response.StatusCode))" -ForegroundColor Green
            $script:successes += $Description
            return $true
        } else {
            Write-Host "⚠ WARNING: Unexpected status code: $($response.StatusCode)" -ForegroundColor Yellow
            $script:warnings += "$Description - Status: $($response.StatusCode)"
            return $false
        }
    } catch {
        Write-Host "✗ FAILED: $Description" -ForegroundColor Red
        Write-Host "  Error: $($_.Exception.Message)" -ForegroundColor Red
        $script:errors += $Description
        return $false
    }
    
    Write-Host ""
}

# Function to test JSON response
function Test-JsonEndpoint {
    param (
        [string]$Url,
        [string]$Description,
        [string[]]$RequiredFields
    )
    
    Write-Host "Testing: $Description" -ForegroundColor Yellow
    Write-Host "URL: $Url" -ForegroundColor Gray
    
    try {
        $response = Invoke-RestMethod -Uri $Url -Method GET -TimeoutSec 10
        
        $missingFields = @()
        foreach ($field in $RequiredFields) {
            if (-not $response.$field) {
                $missingFields += $field
            }
        }
        
        if ($missingFields.Count -eq 0) {
            Write-Host "✓ SUCCESS: $Description - All fields present" -ForegroundColor Green
            Write-Host "  Response: $($response | ConvertTo-Json -Depth 2 -Compress)" -ForegroundColor Gray
            $script:successes += $Description
            return $true
        } else {
            Write-Host "⚠ WARNING: Missing fields: $($missingFields -join ', ')" -ForegroundColor Yellow
            $script:warnings += "$Description - Missing: $($missingFields -join ', ')"
            return $false
        }
    } catch {
        Write-Host "✗ FAILED: $Description" -ForegroundColor Red
        Write-Host "  Error: $($_.Exception.Message)" -ForegroundColor Red
        $script:errors += $Description
        return $false
    }
    
    Write-Host ""
}

# Test 1: Health Check
Write-Host "`n=== Test 1: Monitoring Service Health ===" -ForegroundColor Cyan
Test-Endpoint -Url "$monitoringServiceUrl/actuator/health" -Description "Monitoring Service Health"

# Test 2: Prometheus Metrics Endpoint
Write-Host "`n=== Test 2: Prometheus Metrics Endpoint ===" -ForegroundColor Cyan
Test-Endpoint -Url "$monitoringServiceUrl/actuator/prometheus" -Description "Prometheus Metrics Endpoint"

# Test 3: Standard Metrics Endpoint
Write-Host "`n=== Test 3: Actuator Metrics Endpoint ===" -ForegroundColor Cyan
Test-Endpoint -Url "$monitoringServiceUrl/actuator/metrics" -Description "Actuator Metrics Endpoint"

# Test 4: Performance Metrics Endpoint (5 minutes)
Write-Host "`n=== Test 4: Performance Metrics API (5 minutes) ===" -ForegroundColor Cyan
$requiredFields = @("timestamp", "throughput", "latency", "failure", "timeWindow")
Test-JsonEndpoint -Url "$monitoringServiceUrl/api/monitoring/performance-metrics?minutes=5" `
    -Description "Performance Metrics (5 min)" -RequiredFields $requiredFields

# Test 5: Performance Metrics Endpoint (15 minutes)
Write-Host "`n=== Test 5: Performance Metrics API (15 minutes) ===" -ForegroundColor Cyan
Test-JsonEndpoint -Url "$monitoringServiceUrl/api/monitoring/performance-metrics?minutes=15" `
    -Description "Performance Metrics (15 min)" -RequiredFields $requiredFields

# Test 6: Performance Metrics Endpoint (default)
Write-Host "`n=== Test 6: Performance Metrics API (default) ===" -ForegroundColor Cyan
Test-JsonEndpoint -Url "$monitoringServiceUrl/api/monitoring/performance-metrics" `
    -Description "Performance Metrics (default)" -RequiredFields $requiredFields

# Test 7: Verify Prometheus Metrics Content
Write-Host "`n=== Test 7: Prometheus Metrics Content ===" -ForegroundColor Cyan
try {
    $prometheusMetrics = Invoke-WebRequest -Uri "$monitoringServiceUrl/actuator/prometheus" -UseBasicParsing
    $content = $prometheusMetrics.Content
    
    $metricsToCheck = @(
        "jvm_memory_used_bytes",
        "jvm_threads_live_threads",
        "system_cpu_usage",
        "http_server_requests"
    )
    
    $foundMetrics = @()
    $missingMetrics = @()
    
    foreach ($metric in $metricsToCheck) {
        if ($content -match $metric) {
            $foundMetrics += $metric
            Write-Host "  ✓ Found metric: $metric" -ForegroundColor Green
        } else {
            $missingMetrics += $metric
            Write-Host "  ✗ Missing metric: $metric" -ForegroundColor Yellow
        }
    }
    
    if ($missingMetrics.Count -eq 0) {
        Write-Host "✓ SUCCESS: All expected Prometheus metrics present" -ForegroundColor Green
        $script:successes += "Prometheus Metrics Content"
    } else {
        Write-Host "⚠ WARNING: Some metrics missing" -ForegroundColor Yellow
        $script:warnings += "Prometheus Metrics - Missing: $($missingMetrics -join ', ')"
    }
} catch {
    Write-Host "✗ FAILED: Could not verify Prometheus metrics content" -ForegroundColor Red
    Write-Host "  Error: $($_.Exception.Message)" -ForegroundColor Red
    $script:errors += "Prometheus Metrics Content"
}

Write-Host ""

# Test 8: Database Connection (EventMetric table)
Write-Host "`n=== Test 8: Database Schema ===" -ForegroundColor Cyan
Write-Host "Note: This test requires database access and will be verified when the application starts" -ForegroundColor Gray
Write-Host "Expected table: event_metrics" -ForegroundColor Gray
Write-Host "Expected indexes: idx_event_type, idx_timestamp, idx_service_name" -ForegroundColor Gray

# Summary
Write-Host "`n=====================================" -ForegroundColor Cyan
Write-Host "Verification Summary" -ForegroundColor Cyan
Write-Host "=====================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Successes: $($successes.Count)" -ForegroundColor Green
Write-Host "Warnings:  $($warnings.Count)" -ForegroundColor Yellow
Write-Host "Errors:    $($errors.Count)" -ForegroundColor Red
Write-Host ""

if ($successes.Count -gt 0) {
    Write-Host "Successful Tests:" -ForegroundColor Green
    $successes | ForEach-Object { Write-Host "  ✓ $_" -ForegroundColor Green }
    Write-Host ""
}

if ($warnings.Count -gt 0) {
    Write-Host "Warnings:" -ForegroundColor Yellow
    $warnings | ForEach-Object { Write-Host "  ⚠ $_" -ForegroundColor Yellow }
    Write-Host ""
}

if ($errors.Count -gt 0) {
    Write-Host "Failed Tests:" -ForegroundColor Red
    $errors | ForEach-Object { Write-Host "  ✗ $_" -ForegroundColor Red }
    Write-Host ""
    Write-Host "Please ensure:" -ForegroundColor Yellow
    Write-Host "  1. Monitoring service is running on port 8086" -ForegroundColor Yellow
    Write-Host "  2. Database is accessible and schema is created" -ForegroundColor Yellow
    Write-Host "  3. Kafka is running and event_metrics topic exists" -ForegroundColor Yellow
    exit 1
} else {
    Write-Host "All tests passed! ✓" -ForegroundColor Green
    Write-Host ""
    Write-Host "Next Steps:" -ForegroundColor Cyan
    Write-Host "  1. Access Prometheus metrics at: $monitoringServiceUrl/actuator/prometheus" -ForegroundColor Gray
    Write-Host "  2. Access Performance API at: $monitoringServiceUrl/api/monitoring/performance-metrics" -ForegroundColor Gray
    Write-Host "  3. Access Frontend at: http://localhost:3000/monitoring" -ForegroundColor Gray
    Write-Host "  4. Process some orders to see metrics populate" -ForegroundColor Gray
    exit 0
}
