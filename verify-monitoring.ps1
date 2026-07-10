#!/usr/bin/env pwsh
# Monitoring Service Verification Script
# This script verifies that the monitoring service is properly configured and running

$ErrorActionPreference = "SilentlyContinue"

Write-Host "=====================================" -ForegroundColor Cyan
Write-Host "Monitoring Service Verification" -ForegroundColor Cyan
Write-Host "=====================================" -ForegroundColor Cyan
Write-Host ""

$allPassed = $true

# Function to test endpoint
function Test-Endpoint {
    param(
        [string]$Name,
        [string]$Url,
        [bool]$ExpectJson = $true
    )
    
    Write-Host "Testing $Name..." -ForegroundColor Yellow -NoNewline
    
    try {
        $response = Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 5
        if ($response.StatusCode -eq 200) {
            if ($ExpectJson) {
                $json = $response.Content | ConvertFrom-Json
                Write-Host " ✓ PASSED" -ForegroundColor Green
                return $true
            } else {
                Write-Host " ✓ PASSED" -ForegroundColor Green
                return $true
            }
        } else {
            Write-Host " ✗ FAILED (Status: $($response.StatusCode))" -ForegroundColor Red
            return $false
        }
    } catch {
        Write-Host " ✗ FAILED (Not reachable)" -ForegroundColor Red
        return $false
    }
}

# Function to check Docker container
function Test-Container {
    param([string]$ContainerName)
    
    Write-Host "Checking container: $ContainerName..." -ForegroundColor Yellow -NoNewline
    
    $container = docker ps --filter "name=$ContainerName" --format "{{.Names}}" 2>$null
    if ($container -eq $ContainerName) {
        Write-Host " ✓ RUNNING" -ForegroundColor Green
        return $true
    } else {
        Write-Host " ✗ NOT RUNNING" -ForegroundColor Red
        return $false
    }
}

Write-Host "1. DOCKER CONTAINERS" -ForegroundColor Cyan
Write-Host "--------------------" -ForegroundColor Cyan

# Check Docker containers
$containers = @(
    "monitoring-service",
    "monitoringdb",
    "order-service",
    "validation-service",
    "payment-service",
    "inventory-service",
    "fulfillment-service",
    "kafka",
    "zookeeper"
)

foreach ($container in $containers) {
    $result = Test-Container -ContainerName $container
    $allPassed = $allPassed -and $result
}

Write-Host ""
Write-Host "2. ACTUATOR HEALTH ENDPOINTS" -ForegroundColor Cyan
Write-Host "----------------------------" -ForegroundColor Cyan

# Check actuator health endpoints
$healthEndpoints = @{
    "Monitoring Service" = "http://localhost:8086/actuator/health"
    "Order Service" = "http://localhost:8080/actuator/health"
    "Validation Service" = "http://localhost:8081/actuator/health"
    "Payment Service" = "http://localhost:8082/actuator/health"
    "Inventory Service" = "http://localhost:8083/actuator/health"
    "Fulfillment Service" = "http://localhost:8084/actuator/health"
}

foreach ($endpoint in $healthEndpoints.GetEnumerator()) {
    $result = Test-Endpoint -Name $endpoint.Key -Url $endpoint.Value
    $allPassed = $allPassed -and $result
}

Write-Host ""
Write-Host "3. MONITORING API ENDPOINTS" -ForegroundColor Cyan
Write-Host "----------------------------" -ForegroundColor Cyan

# Check monitoring API endpoints
$result = Test-Endpoint -Name "Health Endpoint" -Url "http://localhost:8086/api/monitoring/health"
$allPassed = $allPassed -and $result

$result = Test-Endpoint -Name "Metrics Endpoint" -Url "http://localhost:8086/api/monitoring/metrics"
$allPassed = $allPassed -and $result

Write-Host ""
Write-Host "4. DETAILED HEALTH CHECK" -ForegroundColor Cyan
Write-Host "------------------------" -ForegroundColor Cyan

try {
    $healthData = Invoke-RestMethod -Uri "http://localhost:8086/api/monitoring/health" -TimeoutSec 5
    
    Write-Host "Overall Status: " -NoNewline
    if ($healthData.overallStatus -eq "UP") {
        Write-Host "$($healthData.overallStatus)" -ForegroundColor Green
    } else {
        Write-Host "$($healthData.overallStatus)" -ForegroundColor Red
        $allPassed = $false
    }
    
    Write-Host "`nDatabase Health: " -NoNewline
    if ($healthData.databaseHealth.status -eq "UP") {
        Write-Host "$($healthData.databaseHealth.status)" -ForegroundColor Green
    } else {
        Write-Host "$($healthData.databaseHealth.status)" -ForegroundColor Red
        $allPassed = $false
    }
    
    Write-Host "Kafka Health: " -NoNewline
    if ($healthData.kafkaHealth.status -eq "UP") {
        Write-Host "$($healthData.kafkaHealth.status)" -ForegroundColor Green
    } else {
        Write-Host "$($healthData.kafkaHealth.status)" -ForegroundColor Red
        $allPassed = $false
    }
    
    Write-Host "`nMicroservices Status:" -ForegroundColor Yellow
    foreach ($service in $healthData.servicesHealth) {
        Write-Host "  - $($service.serviceName): " -NoNewline
        if ($service.status -eq "UP") {
            Write-Host "$($service.status)" -ForegroundColor Green
        } elseif ($service.status -eq "DOWN") {
            Write-Host "$($service.status)" -ForegroundColor Red
            $allPassed = $false
        } else {
            Write-Host "$($service.status)" -ForegroundColor Yellow
        }
    }
} catch {
    Write-Host "Failed to retrieve detailed health data" -ForegroundColor Red
    $allPassed = $false
}

Write-Host ""
Write-Host "5. METRICS CHECK" -ForegroundColor Cyan
Write-Host "----------------" -ForegroundColor Cyan

try {
    $metricsData = Invoke-RestMethod -Uri "http://localhost:8086/api/monitoring/metrics" -TimeoutSec 5
    
    Write-Host "JVM Memory Usage: " -NoNewline
    Write-Host "$([math]::Round($metricsData.jvmMetrics.memoryUsagePercentage, 2))%" -ForegroundColor Green
    
    Write-Host "JVM Threads Live: " -NoNewline
    Write-Host "$($metricsData.jvmMetrics.threadsLive)" -ForegroundColor Green
    
    Write-Host "CPU Usage: " -NoNewline
    Write-Host "$([math]::Round($metricsData.systemMetrics.cpuUsage, 2))%" -ForegroundColor Green
    
    Write-Host "CPU Count: " -NoNewline
    Write-Host "$($metricsData.systemMetrics.cpuCount)" -ForegroundColor Green
} catch {
    Write-Host "Failed to retrieve metrics data" -ForegroundColor Red
    $allPassed = $false
}

Write-Host ""
Write-Host "6. FILE STRUCTURE VERIFICATION" -ForegroundColor Cyan
Write-Host "------------------------------" -ForegroundColor Cyan

# Check backend files
$backendFiles = @(
    "backend\monitoring-service\pom.xml",
    "backend\monitoring-service\Dockerfile",
    "backend\monitoring-service\src\main\resources\application.yml",
    "backend\monitoring-service\src\main\java\com\project\monitoring\MonitoringServiceApplication.java",
    "backend\monitoring-service\src\main\java\com\project\monitoring\controller\MetricsController.java",
    "backend\monitoring-service\src\main\java\com\project\monitoring\service\MetricsService.java"
)

foreach ($file in $backendFiles) {
    Write-Host "Checking $file..." -ForegroundColor Yellow -NoNewline
    if (Test-Path $file) {
        Write-Host " ✓ EXISTS" -ForegroundColor Green
    } else {
        Write-Host " ✗ MISSING" -ForegroundColor Red
        $allPassed = $false
    }
}

# Check frontend files
$frontendFiles = @(
    "frontend\src\pages\Monitoring\MonitoringPage.jsx",
    "frontend\src\pages\Monitoring\MonitoringPage.css",
    "frontend\src\components\monitoring\MetricsCard.jsx",
    "frontend\src\components\monitoring\HealthGrid.jsx",
    "frontend\src\services\monitoringApi.js"
)

foreach ($file in $frontendFiles) {
    Write-Host "Checking $file..." -ForegroundColor Yellow -NoNewline
    if (Test-Path $file) {
        Write-Host " ✓ EXISTS" -ForegroundColor Green
    } else {
        Write-Host " ✗ MISSING" -ForegroundColor Red
        $allPassed = $false
    }
}

Write-Host ""
Write-Host "=====================================" -ForegroundColor Cyan
Write-Host "VERIFICATION RESULTS" -ForegroundColor Cyan
Write-Host "=====================================" -ForegroundColor Cyan
Write-Host ""

if ($allPassed) {
    Write-Host "✓ ALL CHECKS PASSED!" -ForegroundColor Green
    Write-Host ""
    Write-Host "Your monitoring service is properly configured and running." -ForegroundColor Green
    Write-Host ""
    Write-Host "Access the monitoring dashboard at:" -ForegroundColor Yellow
    Write-Host "http://localhost:3000/monitoring" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "Opening dashboard in browser..." -ForegroundColor Yellow
    Start-Process "http://localhost:3000/monitoring"
} else {
    Write-Host "✗ SOME CHECKS FAILED" -ForegroundColor Red
    Write-Host ""
    Write-Host "Please review the failed checks above." -ForegroundColor Yellow
    Write-Host "Common issues:" -ForegroundColor Yellow
    Write-Host "  1. Services not started: Run 'docker-compose up -d'" -ForegroundColor White
    Write-Host "  2. Services still starting: Wait a few moments and try again" -ForegroundColor White
    Write-Host "  3. Port conflicts: Check if ports are already in use" -ForegroundColor White
    Write-Host ""
    Write-Host "For detailed logs, run:" -ForegroundColor Yellow
    Write-Host "  docker logs monitoring-service" -ForegroundColor White
}

Write-Host ""
