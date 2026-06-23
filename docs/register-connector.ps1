# Debezium Connector 등록 스크립트 (Windows PowerShell용)
$baseUri = "http://localhost:8084/connectors"
$headers = @{
    "Content-Type" = "application/json"
}

# 등록할 커넥터 목록 정의 (설정 파일명과 커넥터명 매핑)
$connectors = @(
    @{ Name = "order-outbox-connector-v3"; File = "debezium-connector-setup.json" },
    @{ Name = "point-outbox-connector-v3"; File = "debezium-point-connector-setup.json" },
    @{ Name = "payment-outbox-connector-v3"; File = "debezium-payment-connector-setup.json" }
)

foreach ($conn in $connectors) {
    $connectorName = $conn.Name
    $fileName = $conn.File
    Write-Host "--------------------------------------------------"
    Write-Host "Processing connector: $connectorName"
    
    # 1. 기존 커넥터 존재 확인 및 삭제 (재등록 시 멱등성 보장)
    try {
        $existing = Invoke-RestMethod -Uri "$baseUri/$connectorName" -Method Get -Headers $headers -ErrorAction SilentlyContinue
        if ($existing) {
            Write-Host "Existing connector '$connectorName' found. Deleting..."
            Invoke-RestMethod -Uri "$baseUri/$connectorName" -Method Delete -Headers $headers | Out-Null
            Start-Sleep -Seconds 2
        }
    } catch {
        # 기존 커넥터가 없어서 발생하는 에러는 무시
    }

    # 2. 신규 커넥터 등록
    $jsonPath = Join-Path $PSScriptRoot $fileName
    if (Test-Path $jsonPath) {
        $body = Get-Content -Raw -Path $jsonPath
        Write-Host "Registering Debezium Connector from $fileName to $baseUri..."
        try {
            $response = Invoke-RestMethod -Uri $baseUri -Method Post -Headers $headers -Body $body
            Write-Host "Success registering $connectorName!"
            $response | ConvertTo-Json
        } catch {
            Write-Error ("Failed to register " + $connectorName + ". Reason: " + $_.Exception.Message)
        }
    } else {
        Write-Error "설정 파일을 찾을 수 없습니다: $jsonPath"
    }
}

