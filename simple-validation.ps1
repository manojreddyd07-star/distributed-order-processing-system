#!/usr/bin/env pwsh
# Simplified Validation Script

Write-Host "========================================" -ForegroundColor Cyan
Write-Host " System Validation" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$Report = @{
    Timestamp = Get-Date -Format 'yyyy-MM-dd HH:mm:ss'
    FrontendBuild = "Unknown"
    FrontendTests = @{ Total = 0; Passed = 0; Failed = 0 }
    APIFiles = @()
    Pages = @()
    Services = @()
    Defects = @()
}

# 1. Frontend Tests
Write-Host "[1/3] Frontend Tests..." -ForegroundColor Yellow
Set-Location "frontend"
$testCmd = "npm test -- --coverage --watchAll=false --passWithNoTests 2>&1 | Select-String 'Test Suites|Tests:' | Select-Object -Last 2"
$testOutput = Invoke-Expression $testCmd | Out-String
Write-Host $testOutput
Set-Location ".."

# 2. API Files Check
Write-Host "`n[2/3] API Files Validation..." -ForegroundColor Yellow
$apiFiles = @("orderApi.js", "validationApi.js", "paymentApi.js", 
              "fulfillmentApi.js", "monitoringApi.js", "retryApi.js")
foreach ($api in $apiFiles) {
    $path = "frontend\src\services\$api"
    if (Test-Path $path) {
        Write-Host "  ✅ $api" -ForegroundColor Green
        $Report.APIFiles += $api
    } else {
        Write-Host "  ❌ $api - MISSING" -ForegroundColor Red
        $Report.Defects += "Missing: $api"
    }
}

# 3. Pages Check
Write-Host "`n[3/3] Page Components..." -ForegroundColor Yellow
$pages = @("Dashboard", "Orders", "Validation", "Payment", 
           "Inventory", "Monitoring", "Fulfillment")
foreach ($page in $pages) {
    $path = "frontend\src\pages\$page\${page}Page.jsx"
    if (Test-Path $path) {
        Write-Host "  ✅ $page Page" -ForegroundColor Green
        $Report.Pages += $page
    } else {
        Write-Host "  ⚠ $page Page - Check needed" -ForegroundColor Yellow
    }
}

# Summary
Write-Host "`n========================================"  -ForegroundColor Cyan
Write-Host "API Files Found: $($Report.APIFiles.Count)/$($apiFiles.Count)" -ForegroundColor Green
Write-Host "Pages Found: $($Report.Pages.Count)/$($pages.Count)" -ForegroundColor Green
if ($Report.Defects.Count -gt 0) {
    Write-Host "Defects: $($Report.Defects.Count)" -ForegroundColor Red
} else {
    Write-Host "Defects: 0" -ForegroundColor Green
}
Write-Host "========================================" -ForegroundColor Cyan
