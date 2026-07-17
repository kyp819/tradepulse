# Verify TradePulse Dashboard via PowerShell Invoke-RestMethod

Write-Host "Submitting Limit Buy order..."
$orderPayload = '{"ticker":"BTCUSDT","side":"BUY","type":"LIMIT","quantity":0.01,"price":20000}'
Invoke-RestMethod -Uri http://localhost:8080/api/orders -Method Post -ContentType "application/json" -Body $orderPayload

Write-Host "Fetching orders..."
Invoke-RestMethod -Uri http://localhost:8080/api/orders -Method Get

Write-Host "Fetching positions..."
Invoke-RestMethod -Uri http://localhost:8080/api/positions -Method Get

Write-Host "Running settlement..."
Invoke-RestMethod -Uri http://localhost:8080/api/settlement -Method Post

Write-Host "Fetching audit logs..."
Invoke-RestMethod -Uri http://localhost:8080/api/audit-logs -Method Get
