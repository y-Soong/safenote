<#
 PRAFTA 웹 프론트 배포 스크립트 (S3 + CloudFront)

 절차:
   1) vite 빌드 (npm run build)
   2) dist/index.html <head> 에 런타임 설정(window.__APP_CONFIG__) 자동 주입
      - 소스 index.html 은 건드리지 않음 (로컬 개발 영향 없음)
      - 주입 확인 실패 시 업로드 자체를 중단 (설정 없는 웹이 라이브에 올라가는 사고 방지)
   3) S3 업로드: 에셋=장기캐시(immutable) sync, index.html=no-cache 개별 업로드
   4) CloudFront 캐시 무효화 (배포 ID E37OL8Q9Q1FSLZ = prafta-web)
   5) 라이브 검증: https://prafta.com 응답에 __APP_CONFIG__ 포함 여부 확인

 사용:
   powershell -ExecutionPolicy Bypass -File .\scripts\deploy-web.ps1
   powershell -ExecutionPolicy Bypass -File .\scripts\deploy-web.ps1 -SkipBuild   # 빌드 생략(직전 dist 재사용)

 전제:
   - AWS CLI: pip user 설치본 (python -m awscli), prafta-deploy 자격증명 구성됨
   - 상세 배경: .claude/refs/AWS_배포현황_및_운영전환가이드.md §3, §8
#>
[CmdletBinding()]
param(
    [switch]$SkipBuild,
    [switch]$SkipInvalidation,
    [string]$ApiBase = "https://api.prafta.com",
    [string]$AppContext = "/prafta",
    [string]$Bucket = "prafta.com",
    [string]$DistributionId = "E37OL8Q9Q1FSLZ",
    [string]$LiveUrl = "https://prafta.com"
)

$ErrorActionPreference = 'Stop'
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12

$webRoot   = Split-Path -Parent $PSScriptRoot
$distDir   = Join-Path $webRoot 'dist'
$indexHtml = Join-Path $distDir 'index.html'

function Write-Step([string]$msg) { Write-Host "`n=== $msg ===" -ForegroundColor Cyan }

# ── 1) 빌드 ──────────────────────────────────────────────
if (-not $SkipBuild) {
    Write-Step "1/5 vite 빌드 (npm run build)"
    Push-Location $webRoot
    try {
        & npm run build
        if ($LASTEXITCODE -ne 0) { throw "npm run build 실패 (exit $LASTEXITCODE)" }
    } finally { Pop-Location }
} else {
    Write-Step "1/5 빌드 생략 (-SkipBuild)"
}

if (-not (Test-Path $indexHtml)) { throw "빌드 산출물 없음: $indexHtml" }

# ── 2) 런타임 설정 주입 ──────────────────────────────────
Write-Step "2/5 런타임 설정 주입 (window.__APP_CONFIG__)"
$configScript = "<script>window.__APP_CONFIG__ = { API_BASE: `"$ApiBase`", CONTEXT: `"$AppContext`" };</script>"
$html = [IO.File]::ReadAllText($indexHtml)

if ($html -match '__APP_CONFIG__') {
    Write-Host "이미 주입되어 있음 — 건너뜀 (-SkipBuild 재실행 등)"
} else {
    $html = $html -replace '</head>', "  $configScript`n  </head>"
    [IO.File]::WriteAllText($indexHtml, $html, (New-Object Text.UTF8Encoding($false)))
}

# 주입 가드: 설정 없는 index.html 은 절대 업로드하지 않는다
if (-not ([IO.File]::ReadAllText($indexHtml) -match '__APP_CONFIG__')) {
    throw "런타임 설정 주입 실패 — index.html 에 __APP_CONFIG__ 없음. 업로드 중단."
}
Write-Host "주입 확인: $configScript"

# ── 3) S3 업로드 ─────────────────────────────────────────
Write-Step "3/5 S3 업로드 (s3://$Bucket)"
& python -m awscli s3 sync $distDir "s3://$Bucket/" --delete --cache-control "public,max-age=31536000,immutable" --exclude "index.html"
if ($LASTEXITCODE -ne 0) { throw "s3 sync 실패 (exit $LASTEXITCODE)" }

& python -m awscli s3 cp $indexHtml "s3://$Bucket/index.html" --cache-control "no-cache,no-store,must-revalidate" --content-type "text/html; charset=utf-8"
if ($LASTEXITCODE -ne 0) { throw "index.html 업로드 실패 (exit $LASTEXITCODE)" }

# ── 4) CloudFront 캐시 무효화 ────────────────────────────
if (-not $SkipInvalidation) {
    Write-Step "4/5 CloudFront 캐시 무효화 ($DistributionId)"
    & python -m awscli cloudfront create-invalidation --distribution-id $DistributionId --paths "/*"
    if ($LASTEXITCODE -ne 0) {
        # S3 업로드는 이미 성공한 상태 — 무효화만 실패한 것이므로 배포 자체를 실패로 처리하지 않는다
        Write-Host "CloudFront 무효화 실패 (AccessDenied 라면 prafta-deploy IAM 사용자에 CloudFront 권한 필요)" -ForegroundColor Yellow
        Write-Host "index.html 은 no-cache 라 곧 반영되지만, 교체된 에셋의 즉시 반영은 보장되지 않음." -ForegroundColor Yellow
        Write-Host "콘솔에서 수동 무효화: CloudFront → $DistributionId → Invalidations → /*" -ForegroundColor Yellow
    }
} else {
    Write-Step "4/5 무효화 생략 (-SkipInvalidation)"
}

# ── 5) 라이브 검증 ───────────────────────────────────────
Write-Step "5/5 라이브 검증 ($LiveUrl)"
$verified = $false
for ($i = 1; $i -le 6; $i++) {
    Start-Sleep -Seconds 5
    try {
        # 캐시 우회 쿼리로 index.html 최신본 확인
        $resp = Invoke-WebRequest -Uri "$LiveUrl/?deployCheck=$i" -UseBasicParsing -TimeoutSec 15
        if ($resp.Content -match '__APP_CONFIG__') { $verified = $true; break }
        Write-Host "응답은 왔으나 __APP_CONFIG__ 미포함 — 캐시 전파 대기 중... ($i/6)"
    } catch {
        Write-Host "라이브 응답 대기 중... ($i/6)"
    }
}

if ($verified) {
    Write-Host "`n배포 완료. 라이브 index.html 에 런타임 설정 반영 확인됨." -ForegroundColor Green
    exit 0
}

Write-Host "`n업로드는 성공했으나 라이브 검증 미통과 — CloudFront 전파 지연일 수 있음." -ForegroundColor Yellow
Write-Host "1~2분 후 브라우저에서 직접 확인: $LiveUrl (개발자도구 콘솔에서 window.__APP_CONFIG__ 출력)"
exit 1
