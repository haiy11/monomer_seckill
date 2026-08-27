# P7 配置中心：把本目录下的 Nacos 配置 dataId 一键发布到 Nacos Config。
# 用法（在普通 PowerShell 中执行，需保证 Nacos 已启动于 localhost:8848）：
#   .\publish-configs.ps1
# 若 Nacos 开启了鉴权，请先设置下面两个变量后重新运行：
#   $script:NacosUser = "nacos"; $script:NacosPassword = "nacos"
# 发布后可在 Nacos 控制台 http://localhost:8848/nacos 「配置管理 → 配置列表」查看。

param(
    [string]$ServerAddr = "http://localhost:8848"
)

$ErrorActionPreference = "Stop"

# 待发布的 dataId 与本地文件名（与各服务 application.yml 中 spring.config.import 一一对应）
$Configs = @(
    @{ DataId = "seckill-common.yml";            File = "seckill-common.yml" },
    @{ DataId = "seckill-service.yml";           File = "seckill-service.yml" },
    @{ DataId = "seckill-service-dev.yml";       File = "seckill-service-dev.yml" },
    @{ DataId = "seckill-service-prod.yml";      File = "seckill-service-prod.yml" },
    @{ DataId = "goods-order-service.yml";       File = "goods-order-service.yml" },
    @{ DataId = "goods-order-service-dev.yml";   File = "goods-order-service-dev.yml" },
    @{ DataId = "goods-order-service-prod.yml";  File = "goods-order-service-prod.yml" },
    @{ DataId = "user-service.yml";              File = "user-service.yml" },
    @{ DataId = "user-service-dev.yml";          File = "user-service-dev.yml" },
    @{ DataId = "user-service-prod.yml";         File = "user-service-prod.yml" },
    @{ DataId = "gateway-service.yml";           File = "gateway-service.yml" },
    @{ DataId = "gateway-service-dev.yml";       File = "gateway-service-dev.yml" },
    @{ DataId = "gateway-service-prod.yml";      File = "gateway-service-prod.yml" }
)

function Format-UrlEncoded([System.Collections.IDictionary]$Params) {
    $parts = foreach ($key in $Params.Keys) {
        "{0}={1}" -f $key, [System.Uri]::EscapeDataString([string]$Params[$key])
    }
    return ($parts -join "&")
}

$api = "$ServerAddr/nacos/v1/cs/configs"

foreach ($cfg in $Configs) {
    $path = Join-Path $PSScriptRoot $cfg.File
    if (-not (Test-Path $path)) {
        Write-Warning "跳过：找不到文件 $path"
        continue
    }
    $content = Get-Content -Raw -Encoding UTF8 $path

    $body = [ordered]@{
        dataId = $cfg.DataId
        group  = "DEFAULT_GROUP"
        type   = "yaml"
        content = $content
    }
    if ($script:NacosUser -and $script:NacosPassword) {
        $body["username"] = $script:NacosUser
        $body["password"] = $script:NacosPassword
    }

    $encoded = Format-UrlEncoded $body
    try {
        $resp = Invoke-RestMethod -Method Post -Uri $api -Body $encoded -ContentType "application/x-www-form-urlencoded"
        Write-Host "[OK] 已发布 $($cfg.DataId) -> $resp"
    } catch {
        Write-Host "[FAIL] 发布 $($cfg.DataId) 失败：$($_.Exception.Message)" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "发布完成。请到 Nacos 控制台确认配置列表：$ServerAddr/nacos"
