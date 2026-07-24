#!/usr/bin/env pwsh
# Docker Deployment Validation Script for Windows PowerShell

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Docker Deployment Validation" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$ErrorCount = 0
$WarningCount = 0

# Function to check command existence
function Test-Command {
    param($Command)
    try {
        Get-Command $Command -ErrorAction Stop | Out-Null
        return $true
    }
    catch {
        return $false
    }
}

# Function to log error
function Write-Error-Log {
    param($Message)
    Write-Host "❌ ERROR: $Message" -ForegroundColor Red
    $script:ErrorCount++
}

# Function to log warning
function Write-Warning-Log {
    param($Message)
    Write-Host "⚠️  WARNING: $Message" -ForegroundColor Yellow
    $script:WarningCount++
}

# Function to log success
function Write-Success-Log {
    param($Message)
    Write-Host "✅ $Message" -ForegroundColor Green
}

# 1. Check Prerequisites
Write-Host "Step 1: Checking Prerequisites..." -ForegroundColor Cyan

if (Test-Command "docker") {
    $dockerVersion = docker --version
    Write-Success-Log "Docker installed: $dockerVersion"
} else {
    Write-Error-Log "Docker is not installed"
}

if (Test-Command "docker-compose") {
    $composeVersion = docker-compose --version
    Write-Success-Log "Docker Compose installed: $composeVersion"
} else {
    Write-Error-Log "Docker Compose is not installed"
}

# Check Docker daemon
try {
    docker ps | Out-Null
    Write-Success-Log "Docker daemon is running"
} catch {
    Write-Error-Log "Docker daemon is not running"
}

Write-Host ""

# 2. Validate Docker Files
Write-Host "Step 2: Validating Docker Files..." -ForegroundColor Cyan

$requiredFiles = @(
    "docker-compose.yml",
    "backend\.dockerignore",
    "backend\order-service\Dockerfile",
    "backend\validation-service\Dockerfile",
    "backend\payment-service\Dockerfile",
    "backend\inventory-service\Dockerfile",
    "backend\fulfillment-service\Dockerfile",
    "backend\monitoring-service\Dockerfile",
    "frontend\Dockerfile",
    "frontend\.dockerignore",
    "frontend\nginx.conf"
)

foreach ($file in $requiredFiles) {
    if (Test-Path $file) {
        Write-Success-Log "Found: $file"
    } else {
        Write-Error-Log "Missing: $file"
    }
}

Write-Host ""

# 3. Validate docker-compose.yml
Write-Host "Step 3: Validating docker-compose.yml..." -ForegroundColor Cyan

try {
    docker-compose config | Out-Null
    Write-Success-Log "docker-compose.yml is valid"
} catch {
    Write-Error-Log "docker-compose.yml has syntax errors"
}

Write-Host ""

# 4. Check Frontend Build
Write-Host "Step 4: Checking Frontend Build..." -ForegroundColor Cyan

if (Test-Path "frontend\package.json") {
    Write-Success-Log "package.json found"
    
    $packageJson = Get-Content "frontend\package.json" | ConvertFrom-Json
    
    if ($packageJson.scripts.build) {
        Write-Success-Log "Build script found: $($packageJson.scripts.build)"
    } else {
        Write-Error-Log "Build script not found in package.json"
    }
    
    if ($packageJson.scripts.start) {
        Write-Success-Log "Start script found: $($packageJson.scripts.start)"
    } else {
        Write-Warning-Log "Start script not found in package.json"
    }
} else {
    Write-Error-Log "frontend/package.json not found"
}

Write-Host ""

# 5. Check Backend Structure
Write-Host "Step 5: Checking Backend Structure..." -ForegroundColor Cyan

$services = @(
    "order-service",
    "validation-service",
    "payment-service",
    "inventory-service",
    "fulfillment-service",
    "monitoring-service"
)

foreach ($service in $services) {
    if (Test-Path "backend\$service\pom.xml") {
        Write-Success-Log "$service pom.xml found"
    } else {
        Write-Error-Log "$service pom.xml not found"
    }
    
    if (Test-Path "backend\$service\src") {
        Write-Success-Log "$service src directory found"
    } else {
        Write-Error-Log "$service src directory not found"
    }
}

Write-Host ""

# 6. Check Docker Image Optimization
Write-Host "Step 6: Checking Docker Image Optimization..." -ForegroundColor Cyan

$dockerfiles = Get-ChildItem -Path "backend" -Filter "Dockerfile" -Recurse

foreach ($dockerfile in $dockerfiles) {
    $content = Get-Content $dockerfile.FullName -Raw
    
    if ($content -match "multi-stage") {
        Write-Success-Log "$($dockerfile.Directory.Name): Multi-stage build detected"
    } elseif ($content -match "FROM.*AS build") {
        Write-Success-Log "$($dockerfile.Directory.Name): Multi-stage build found"
    } else {
        Write-Warning-Log "$($dockerfile.Directory.Name): Multi-stage build not detected"
    }
    
    if ($content -match "adduser.*appuser") {
        Write-Success-Log "$($dockerfile.Directory.Name): Non-root user configured"
    } else {
        Write-Warning-Log "$($dockerfile.Directory.Name): Running as root (security risk)"
    }
    
    if ($content -match "HEALTHCHECK") {
        Write-Success-Log "$($dockerfile.Directory.Name): Health check configured"
    } else {
        Write-Warning-Log "$($dockerfile.Directory.Name): No health check configured"
    }
}

Write-Host ""

# 7. Check Network Configuration
Write-Host "Step 7: Checking Network Configuration..." -ForegroundColor Cyan

$composeContent = Get-Content "docker-compose.yml" -Raw

if ($composeContent -match "networks:") {
    Write-Success-Log "Network configuration found"
    
    if ($composeContent -match "order-processing-network") {
        Write-Success-Log "Custom network 'order-processing-network' configured"
    }
} else {
    Write-Warning-Log "No network configuration found"
}

Write-Host ""

# 8. Check Volume Configuration
Write-Host "Step 8: Checking Volume Configuration..." -ForegroundColor Cyan

if ($composeContent -match "volumes:") {
    Write-Success-Log "Volume configuration found"
    
    $expectedVolumes = @(
        "postgres_data",
        "postgres_validation_data",
        "postgres_payment_data",
        "postgres_inventory_data",
        "postgres_fulfillment_data",
        "postgres_monitoring_data"
    )
    
    foreach ($volume in $expectedVolumes) {
        if ($composeContent -match $volume) {
            Write-Success-Log "Volume $volume configured"
        } else {
            Write-Error-Log "Volume $volume not configured"
        }
    }
} else {
    Write-Error-Log "No volume configuration found"
}

Write-Host ""

# 9. Test Docker Build (optional - can be slow)
Write-Host "Step 9: Testing Docker Builds (Optional)..." -ForegroundColor Cyan
$buildTest = Read-Host "Do you want to test building services? (y/N)"

if ($buildTest -eq "y" -or $buildTest -eq "Y") {
    Write-Host "Building frontend..." -ForegroundColor Yellow
    try {
        docker-compose build frontend
        Write-Success-Log "Frontend build successful"
    } catch {
        Write-Error-Log "Frontend build failed"
    }
    
    Write-Host "Building order-service (sample backend)..." -ForegroundColor Yellow
    try {
        docker-compose build order-service
        Write-Success-Log "Order service build successful"
    } catch {
        Write-Error-Log "Order service build failed"
    }
} else {
    Write-Host "Skipping build tests" -ForegroundColor Yellow
}

Write-Host ""

# Summary
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Validation Summary" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

if ($ErrorCount -eq 0 -and $WarningCount -eq 0) {
    Write-Host "✅ All checks passed!" -ForegroundColor Green
    Write-Host "Ready to deploy with: docker-compose up -d --build" -ForegroundColor Green
} elseif ($ErrorCount -eq 0) {
    Write-Host "⚠️  Validation completed with $WarningCount warning(s)" -ForegroundColor Yellow
    Write-Host "Review warnings before deploying" -ForegroundColor Yellow
} else {
    Write-Host "❌ Validation failed with $ErrorCount error(s) and $WarningCount warning(s)" -ForegroundColor Red
    Write-Host "Fix errors before deploying" -ForegroundColor Red
}

Write-Host ""
Write-Host "Next Steps:" -ForegroundColor Cyan
Write-Host "1. Fix any errors or warnings above" -ForegroundColor White
Write-Host "2. Run: docker-compose up -d --build" -ForegroundColor White
Write-Host "3. Check services: docker-compose ps" -ForegroundColor White
Write-Host "4. View logs: docker-compose logs -f" -ForegroundColor White
Write-Host "5. Access frontend: http://localhost:3000" -ForegroundColor White
Write-Host ""

exit $ErrorCount
