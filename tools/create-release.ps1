# 用 GitHub API 发一个 Release 并上传 jar。
#
# 令牌只在这个脚本的内存里用，不会写进任何文件，也不会出现在对话里。
# 用法：
#   1. 在 GitHub 建一个 fine-grained token（见下方说明），只勾这个仓库、只给 Contents 读写
#   2. 打开 PowerShell，cd 到项目目录，运行：
#        powershell -ExecutionPolicy Bypass -File .\tools\create-release.ps1
#   3. 提示时粘贴令牌（输入是隐藏的，按回车即可），脚本会建好 Release 并上传 jar
#
# 令牌权限（fine-grained personal access token）：
#   Repository access : Only select repositories -> kalioupo-equipment
#   Permissions       : Contents = Read and write（其它全部留 No access）
#   Expiration        : 7 天（发完版可以立刻在设置里 Revoke）

param(
    [string]$Tag = "v1.2.9",
    [string]$Owner = "Katlioupo",
    [string]$Repo = "kalioupo-equipment",
    [string]$Title = "",
    [string]$NotesFile = "RELEASE_NOTES.md",
    [string]$Jar = "",
    [string]$Token = $env:GH_TOKEN
)

$ErrorActionPreference = "Stop"
$repoRoot = Split-Path -Parent $PSScriptRoot

if (-not $Jar) {
    $version = (Select-String -Path (Join-Path $repoRoot "gradle.properties") -Pattern '^mod_version=(.+)$').Matches[0].Groups[1].Value.Trim()
    $Jar = Join-Path $repoRoot "build\libs\kalioupo-equipment-$version.jar"
}

if (-not (Test-Path $Jar)) {
    throw "找不到 jar：$Jar（先在项目目录跑 .\gradlew.bat build --offline）"
}

if (-not $Title) { $Title = "$Tag 版本" }

$body = ""
if (Test-Path (Join-Path $repoRoot $NotesFile)) {
    $body = Get-Content (Join-Path $repoRoot $NotesFile) -Raw -Encoding UTF8
} elseif (Test-Path $NotesFile) {
    $body = Get-Content $NotesFile -Raw -Encoding UTF8
}

if (-not $Token) {
    $secure = Read-Host "粘贴 GitHub 令牌（输入不回显）" -AsSecureString
    $bstr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
    try { $Token = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($bstr) }
    finally { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstr) }
}

$headers = @{
    Authorization          = "Bearer $Token"
    Accept                 = "application/vnd.github+json"
    "X-GitHub-Api-Version" = "2022-11-28"
    "User-Agent"           = "kalioupo-release-script"
}

$api = "https://api.github.com/repos/$Owner/$Repo"

Write-Host "检查是否已有 $Tag 的 Release ..."
$release = $null
try {
    $release = Invoke-RestMethod -Method Get -Uri "$api/releases/tags/$Tag" -Headers $headers
    Write-Host "已存在 Release #$($release.id)，将更新说明并补传 jar"
    $payload = @{ name = $Title; body = $body } | ConvertTo-Json -Depth 5
    $release = Invoke-RestMethod -Method Patch -Uri "$api/releases/$($release.id)" -Headers $headers -ContentType "application/json; charset=utf-8" -Body ([Text.Encoding]::UTF8.GetBytes($payload))
} catch {
    if ($_.Exception.Response.StatusCode.value__ -ne 404) { throw }
    Write-Host "创建新 Release ..."
    $payload = @{ tag_name = $Tag; name = $Title; body = $body; draft = $false; prerelease = $false } | ConvertTo-Json -Depth 5
    $release = Invoke-RestMethod -Method Post -Uri "$api/releases" -Headers $headers -ContentType "application/json; charset=utf-8" -Body ([Text.Encoding]::UTF8.GetBytes($payload))
}

$assetName = Split-Path -Leaf $Jar

Write-Host "上传 $assetName ..."
$uploaded = Invoke-RestMethod -Method Post `
    -Uri "https://uploads.github.com/repos/$Owner/$Repo/releases/$($release.id)/assets?name=$assetName" `
    -Headers $headers -ContentType "application/java-archive" -InFile $Jar

Write-Host ""
Write-Host "完成：$($release.html_url)"
Write-Host "文件：$($uploaded.name)  $([math]::Round($uploaded.size / 1KB)) KB"
Write-Host ""
Write-Host "记得去 GitHub 设置里把令牌 Revoke 掉（如果设了 7 天过期也可以等它自己失效）。"
