#!/usr/bin/env pwsh
# Comprehensive Validation and Defect Report Script

$ErrorActionPreference = "Continue"
$ReportFile = "validation-report-$(Get-Date -Format 'yyyyMMdd-HHmmss').md"
$DefectList = @()
$SuccessList = @()

Write-Host "=======================================" -ForegroundColor Cyan
Write-Host "  Distributed Order Processing System" -ForegroundColor Cyan
Write-Host "  Comprehensive Validation Report" -ForegroundColor Cyan
Write-Host "=======================================" -ForegroundColor Cyan
Write-Host ""

# Initialize Report
"# Validation Report" | Out-File $ReportFile
"Generated: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')`n" | Out-File $ReportFile -Append
"---`n" | Out-File $ReportFile -Append

## SECTION 1: FRONTEND VALIDATION
Write-Host "[1/6] Frontend Validation" -ForegroundColor Yellow
"## 1. Frontend Validation`n" | Out-File $ReportFile -Append

# Test frontend compilation
Write-Host "  Testing frontend compilation..." -ForegroundColor Gray
Push-Location frontend
try {
    $buildOutput = npm run build 2>&1
    if ($LASTEXITCODE -eq 0) {
        $SuccessList += "Frontend builds successfully"
        "✅ Frontend builds successfully`n" | Out-File $ReportFile -Append
        Write-Host "  ✅ Build successful" -ForegroundColor Green
    } else {
        $DefectList += @{
            Component = "Frontend"
            Issue = "Build fails"
            Severity = "High"
            Details = $buildOutput | Select-Object -Last 20 | Out-String
        }
        "❌ Frontend build failed`n" | Out-File $ReportFile -Append
        Write-Host "  ❌ Build failed" -ForegroundColor Red
    }
} catch {
    $DefectList += @{
        Component = "Frontend"
        Issue = "Build error"
        Severity = "High"
        Details = $_.Exception.Message
    }
}
Pop-Location

# Test suite execution
Write-Host "  Running frontend tests..." -ForegroundColor Gray
Push-Location frontend
try {
    $testOutput = npm test -- --coverage --watchAll=false --passWithNoTests --json --outputFile=test-results.json 2>&1
    
    if (Test-Path "test-results.json") {
        $testResults = Get-Content "test-results.json" | ConvertFrom-Json
        $failedTests = $testResults.numFailedTests
        $passedTests = $testResults.numPassedTests
        $totalTests = $testResults.numTotalTests
        
        "### Test Results:`n" | Out-File $ReportFile -Append
        "- Total Tests: $totalTests`n" | Out-File $ReportFile -Append
        "- Passed: $passedTests`n" | Out-File $ReportFile -Append
        "- Failed: $failedTests`n" | Out-File $ReportFile -Append
        
        if ($failedTests -gt 0) {
            $DefectList += @{
                Component = "Frontend Tests"
                Issue = "$failedTests tests failing"
                Severity = "Medium"
                Details = "Failed tests need investigation"
            }
        }
        
        Write-Host "  📊 Tests: $passedTests passed, $failedTests failed" -ForegroundColor $(if ($failedTests -eq 0) { "Green" } else { "Yellow" })
    }
} catch {
    Write-Host "  ⚠ Test execution error" -ForegroundColor Yellow
}
Pop-Location

## SECTION 2: API VALIDATION
Write-Host "`n[2/6] API Integration Validation" -ForegroundColor Yellow
"`n## 2. API Integration Validation`n" | Out-File $ReportFile -Append

# Check API files exist
$apiFiles = @(
    "frontend\src\services\orderApi.js",
    "frontend\src\services\validationApi.js",
    "frontend\src\services\paymentApi.js",
    "frontend\src\services\fulfillmentApi.js",
    "frontend\src\services\monitoringApi.js",
    "frontend\src\services\retryApi.js",
    "frontend\src\services\dlqApi.js"
)

foreach ($apiFile in $apiFiles) {
    if (Test-Path $apiFile) {
        $SuccessList += "API file exists: $apiFile"
        Write-Host "  ✅ $(Split-Path $apiFile -Leaf)" -ForegroundColor Green
    } else {
        $DefectList += @{
            Component = "API"
            Issue = "Missing API file: $apiFile"
            Severity = "High"
            Details = "API file not found"
        }
        Write-Host "  ❌ $(Split-Path $apiFile -Leaf) - Missing" -ForegroundColor Red
    }
}

## SECTION 3: PAGE VALIDATION
Write-Host "`n[3/6] Page Component Validation" -ForegroundColor Yellow
"`n## 3. Page Components`n" | Out-File $ReportFile -Append

$pages = @(
    "frontend\src\pages\Dashboard\DashboardPage.jsx",
    "frontend\src\pages\Orders\OrdersPage.jsx",
    "frontend\src\pages\Validation\ValidationPage.jsx",
    "frontend\src\pages\Payment\PaymentPage.jsx",
    "frontend\src\pages\Inventory\InventoryPage.jsx",
    "frontend\src\pages\Monitoring\MonitoringPage.jsx",
    "frontend\src\pages\Fulfillment\FulfillmentPage.jsx"
)

foreach ($page in $pages) {
    if (Test-Path $page) {
        $pageName = Split-Path (Split-Path $page -Parent) -Leaf
        $SuccessList += "Page exists: $pageName"
        Write-Host "  ✅ $pageName Page" -ForegroundColor Green
    } else {
        $pageName = Split-Path (Split-Path $page -Parent) -Leaf
        $DefectList += @{
            Component = "Pages"
            Issue = "Missing page: $pageName"
            Severity = "High"
            Details = "Page component not found"
        }
        Write-Host "  ❌ $pageName Page - Missing" -ForegroundColor Red
    }
}

## SECTION 4: NAVIGATION VALIDATION
Write-Host "`n[4/6] Navigation Flow Validation" -ForegroundColor Yellow
"`n## 4. Navigation Flow`n" | Out-File $ReportFile -Append

if (Test-Path "frontend\src\components\Navigation\Navigation.jsx") {
    $navContent = Get-Content "frontend\src\components\Navigation\Navigation.jsx" -Raw
    $routes = @("Dashboard", "Orders", "Validation", "Payment", "Inventory", "Fulfillment", "Monitoring")
    
    foreach ($route in $routes) {
        if ($navContent -match $route) {
            $SuccessList += "Navigation route: $route"
            Write-Host "  ✅ $route route configured" -ForegroundColor Green
        } else {
            $DefectList += @{
                Component = "Navigation"
                Issue = "Missing route: $route"
                Severity = "Medium"
                Details = "Route not found in Navigation component"
            }
            Write-Host "  ⚠ $route route missing" -ForegroundColor Yellow
        }
    }
} else {
    $DefectList += @{
        Component = "Navigation"
        Issue = "Navigation component missing"
        Severity = "High"
        Details = "Navigation.jsx not found"
    }
}

## SECTION 5: BACKEND STRUCTURE VALIDATION
Write-Host "`n[5/6] Backend Structure Validation" -ForegroundColor Yellow
"`n## 5. Backend Services`n" | Out-File $ReportFile -Append

$services = @(
    "backend\order-service",
    "backend\validation-service",
    "backend\payment-service",
    "backend\inventory-service",
    "backend\fulfillment-service",
    "backend\monitoring-service"
)

foreach ($service in $services) {
    $serviceName = Split-Path $service -Leaf
    $pomPath = Join-Path $service "pom.xml"
    $dockerfile = Join-Path $service "Dockerfile"
    
    $serviceOk = $true
    if (Test-Path $pomPath) {
        Write-Host "  ✅ $serviceName - pom.xml" -ForegroundColor Green
    } else {
        $serviceOk = $false
        Write-Host "  ❌ $serviceName - pom.xml missing" -ForegroundColor Red
    }
    
    if (Test-Path $dockerfile) {
        Write-Host "  ✅ $serviceName - Dockerfile" -ForegroundColor Green
    } else {
        $serviceOk = $false
        Write-Host "  ❌ $serviceName - Dockerfile missing" -ForegroundColor Red
    }
    
    if ($serviceOk) {
        $SuccessList += "Backend service OK: $serviceName"
    } else {
        $DefectList += @{
            Component = "Backend"
            Issue = "$serviceName incomplete"
            Severity = "High"
            Details = "Missing required files"
        }
    }
}

## SECTION 6: DOCKER CONFIGURATION
Write-Host "`n[6/6] Docker Configuration Validation" -ForegroundColor Yellow
"`n## 6. Docker Configuration`n" | Out-File $ReportFile -Append

if (Test-Path "docker-compose.yml") {
    $composeContent = Get-Content "docker-compose.yml" -Raw
    $expectedServices = @(
        "postgres", "kafka", "zookeeper",
        "order-service", "validation-service", "payment-service",
        "inventory-service", "fulfillment-service", "monitoring-service",
        "frontend"
    )
    
    foreach ($svc in $expectedServices) {
        $pattern = "$svc" + ":"
        if ($composeContent -match [regex]::Escape($pattern)) {
            Write-Host "  ✅ $svc configured" -ForegroundColor Green
            $SuccessList += "Docker service: $svc"
        } else {
            Write-Host "  ❌ $svc missing" -ForegroundColor Red
            $DefectList += @{
                Component = "Docker"
                Issue = "Service $svc not in docker-compose"
                Severity = "High"
                Details = "Service configuration missing"
            }
        }
    }
} else {
    Write-Host "  ❌ docker-compose.yml missing" -ForegroundColor Red
    $DefectList += @{
        Component = "Docker"
        Issue = "docker-compose.yml not found"
        Severity = "Critical"
        Details = "Main docker configuration missing"
    }
}

## SUMMARY
Write-Host "`n=======================================" -ForegroundColor Cyan
Write-Host "  VALIDATION SUMMARY" -ForegroundColor Cyan
Write-Host "=======================================" -ForegroundColor Cyan

"`n## Summary`n" | Out-File $ReportFile -Append
"### ✅ Successful Checks: $($SuccessList.Count)`n" | Out-File $ReportFile -Append
"### ❌ Defects Found: $($DefectList.Count)`n" | Out-File $ReportFile -Append

if ($DefectList.Count -gt 0) {
    "`n## Defects`n" | Out-File $ReportFile -Append
    Write-Host "`nDefects Found:" -ForegroundColor Red
    
    $criticalDefects = $DefectList | Where-Object { $_.Severity -eq "Critical" }
    $highDefects = $DefectList | Where-Object { $_.Severity -eq "High" }
    $mediumDefects = $DefectList | Where-Object { $_.Severity -eq "Medium" }
    
    if ($criticalDefects.Count -gt 0) {
        "`n### Critical ($($criticalDefects.Count))`n" | Out-File $ReportFile -Append
        foreach ($defect in $criticalDefects) {
            "- **$($defect.Component)**: $($defect.Issue)`n" | Out-File $ReportFile -Append
            Write-Host "  🔴 CRITICAL - $($defect.Component): $($defect.Issue)" -ForegroundColor Red
        }
    }
    
    if ($highDefects.Count -gt 0) {
        "`n### High ($($highDefects.Count))`n" | Out-File $ReportFile -Append
        foreach ($defect in $highDefects) {
            "- **$($defect.Component)**: $($defect.Issue)`n" | Out-File $ReportFile -Append
            Write-Host "  🟠 HIGH - $($defect.Component): $($defect.Issue)" -ForegroundColor Red
        }
    }
    
    if ($mediumDefects.Count -gt 0) {
        "`n### Medium ($($mediumDefects.Count))`n" | Out-File $ReportFile -Append
        foreach ($defect in $mediumDefects) {
            "- **$($defect.Component)**: $($defect.Issue)`n" | Out-File $ReportFile -Append
            Write-Host "  🟡 MEDIUM - $($defect.Component): $($defect.Issue)" -ForegroundColor Yellow
        }
    }
}

Write-Host "`n✅ Successful Checks: $($SuccessList.Count)" -ForegroundColor Green
Write-Host "❌ Total Defects: $($DefectList.Count)" -ForegroundColor $(if ($DefectList.Count -eq 0) { "Green" } else { "Red" })

"`n---`n`nReport saved to: $ReportFile" | Out-File $ReportFile -Append
Write-Host "`n📄 Full report saved to: $ReportFile" -ForegroundColor Cyan
Write-Host ""
