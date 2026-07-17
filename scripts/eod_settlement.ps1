# TradePulse EOD Settlement and Reconciliation Script (Windows PowerShell)

$DbHost = if ($env:DB_HOST) { $env:DB_HOST } else { "localhost" }
$DbPort = if ($env:DB_PORT) { $env:DB_PORT } else { "5432" }
$DbUser = if ($env:DB_USER) { $env:DB_USER } else { "postgres" }
$DbName = if ($env:DB_NAME) { $env:DB_NAME } else { "tradepulse" }
$env:PGPASSWORD = if ($env:DB_PASSWORD) { $env:DB_PASSWORD } else { "postgres" }

# Set working directory to script location
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $ScriptDir

$ReportDir = Join-Path $ScriptDir "reports"
If (-not (Test-Path $ReportDir)) {
    New-Item -ItemType Directory -Path $ReportDir | Out-Null
}

$Timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$ReportFile = Join-Path $ReportDir "settlement_report_$Timestamp.txt"

Write-Host "=================================================="
Write-Host "Starting TradePulse EOD Settlement & Reconciliation"
Write-Host "Time: $(Get-Date)"
Write-Host "DB: $DbUser@$DbHost:$DbPort/$DbName"
Write-Host "=================================================="

# 1. Run Reconciliation
Write-Host "Running Position Reconciliation..."
$ReconResult = psql -h $DbHost -p $DbPort -U $DbUser -d $DbName -q -A -t -f ./reconcile.sql 2>&1

# Check for error
if ($LASTEXITCODE -ne 0) {
    Write-Error "Failed to connect to database or execute reconcile.sql"
    Write-Error $ReconResult
    Exit 1
}

# Count discrepancies
$DiscrepanciesCount = 0
if ($ReconResult) {
    $Lines = $ReconResult -split "`r?`n" | Where-Object { $_.Trim().Length -gt 0 }
    $DiscrepanciesCount = $Lines.Count
}

$ReconStatus = "SUCCESS"
$ReconDesc = "Position reconciliation completed successfully. Zero discrepancies found."

if ($DiscrepanciesCount -gt 0) {
    $ReconStatus = "FAILED"
    $ReconDesc = "Position reconciliation failed! Found $DiscrepanciesCount symbols with quantity mismatch. Check reports."
    Write-Warning "Reconciliation Discrepancies Found!"
    $ReconResult | Out-String | Write-Warning
} else {
    Write-Host "Reconciliation PASSED successfully."
}

# 2. Log to audit_log table
Write-Host "Logging EOD settlement status to audit_log..."
$AuditSql = "INSERT INTO audit_log (event_type, description, created_at) VALUES ('EOD_SETTLEMENT_$ReconStatus', '$ReconDesc', NOW());"
psql -h $DbHost -p $DbPort -U $DbUser -d $DbName -q -c $AuditSql | Out-Null

# 3. Generate EOD Report
Write-Host "Generating EOD Settlement Report..."

$Header = @"
==================================================
TradePulse EOD Settlement Report (Windows)
Generated: $(Get-Date)
Reconciliation Status: $ReconStatus
Reconciliation Details: $ReconDesc
==================================================

"@

$Header | Out-File $ReportFile -Encoding utf8

if ($DiscrepanciesCount -gt 0) {
    $DiscrepancyHeader = @"
=== RECONCILIATION DISCREPANCIES ===
symbol | db_position_qty | expected_position_qty | qty_difference
$($ReconResult -join "`r`n")

"@
    $DiscrepancyHeader | Out-File $ReportFile -Append -Encoding utf8
}

psql -h $DbHost -p $DbPort -U $DbUser -d $DbName -q -f ./settle_report.sql | Out-File $ReportFile -Append -Encoding utf8

Write-Host "EOD Settlement completed. Report saved to $ReportFile"
Get-Content $ReportFile
