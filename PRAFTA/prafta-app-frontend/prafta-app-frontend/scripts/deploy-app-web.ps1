<#
 PRAFTA 앱 프론트(웹뷰 콘텐츠) 원격 호스팅 배포 스크립트 (S3 + CloudFront) — git 기반
 (웹뷰 원격로딩 전환 T5. 참조 구현: prafta-web-frontend/scripts/deploy-web.ps1)

 배포 원칙: 로컬 작업 트리가 아니라 **git 에 커밋·push 된 코드**만 배포한다.
   1) git fetch 후 지정 ref(기본 origin/main)를 임시 worktree 에 체크아웃
   2) npm ci + vite 빌드
   3) dist/index.html <head> 에 배포 식별자(window.__APP_BUILD__) 주입 + 주입 가드
      (마이페이지 빌드 정보의 원격 배포 해시 표기 — §7 판정 수단)
   4) dist/app-manifest.json 생성 — 셸(T4)의 원격/번들 판정 매니페스트(킬 스위치)
   5) S3 업로드(에셋 장기캐시 / index.html·app-manifest.json no-cache)
      → CloudFront 무효화(T6 완료 후 -DistributionId 지정)
   6) 라이브 검증(매니페스트 커밋 해시 대조) 후 배포 이력 기록

 ★ 킬 스위치 운용:
   - 기본은 enabled:false 로 배포된다(롤아웃 §7-8 1단계 — 원격 코드는 올라가 있으나 전원 번들).
   - 원격 활성화는 -RemoteEnabled 를 명시해 재배포. 비상 회귀는 스위치 없이 재실행(즉시 enabled:false).
   - 매니페스트만 바꾸고 싶으면 -ManifestOnly (빌드·에셋 업로드 생략, 수 초 내 반영).

 사용:
   powershell -ExecutionPolicy Bypass -File .\scripts\deploy-app-web.ps1                      # origin/main, enabled:false
   powershell -ExecutionPolicy Bypass -File .\scripts\deploy-app-web.ps1 -RemoteEnabled       # 원격 활성화 배포
   powershell -ExecutionPolicy Bypass -File .\scripts\deploy-app-web.ps1 -ManifestOnly        # 킬 스위치만 OFF (긴급 회귀)
   powershell -ExecutionPolicy Bypass -File .\scripts\deploy-app-web.ps1 -ManifestOnly -RemoteEnabled  # 킬 스위치만 ON

 전제:
   - S3 버킷/CloudFront 배포(T6: app.prafta.com) 생성 완료. prafta-deploy 자격증명 구성됨.
   - CloudFront 무효화 권한은 배포 ID 한정 인라인 정책 — T6 에서 신규 배포 ID 를 정책에 추가해야 함.
   - 상세 배경: .claude/refs/앱_웹뷰_원격로딩_전환_작업지시서.md §4·§7-8
#>
[CmdletBinding()]
param(
    [string]$Ref = 'origin/main',
    [switch]$UseWorkingTree,
    [switch]$SkipFetch,
    [switch]$SkipInvalidation,
    [switch]$RemoteEnabled,                 # 매니페스트 enabled 값(기본 false = 전원 번들)
    [switch]$ManifestOnly,                  # 매니페스트만 갱신(빌드·에셋 업로드 생략)
    [int]$MinShellBridgeVersion = 1,        # 이 값 미만 셸은 번들 폴백(web_app.dart _kBridgeVersion 과 동기)
    [string]$Entry = '/',                   # 원격 진입 경로(버전 핀 시 예: /r/2026-08-04/)
    [string]$Bucket = 'app.prafta.com',
    [string]$DistributionId = '',           # T6 완료 후 실측값 지정. 빈값이면 무효화 생략+경고
    [string]$LiveUrl = 'https://app.prafta.com'
)

$ErrorActionPreference = 'Stop'
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12

$repoRoot = (& git -C $PSScriptRoot rev-parse --show-toplevel).Trim() -replace '/', '\'
if ($LASTEXITCODE -ne 0 -or -not $repoRoot) { throw "git repo 루트를 찾지 못함 ($PSScriptRoot)" }

# worktree 임시 경로 — 시스템 temp(C:\Windows\Temp)는 esbuild 가 상위 열거 권한 문제로 죽는다.
function Resolve-TempRoot {
    foreach ($cand in @($env:TEMP, (Join-Path $env:LOCALAPPDATA 'Temp'))) {
        if (-not $cand) { continue }
        try { Get-ChildItem $cand -ErrorAction Stop | Out-Null; return $cand } catch { }
    }
    throw "쓸 수 있는 임시 디렉토리를 찾지 못함 (TEMP=$env:TEMP)"
}
$worktree   = Join-Path (Resolve-TempRoot) 'prafta-deploy-wt-appweb'
$deployLog  = Join-Path $repoRoot '.claude\refs\deploy-history.log'
$appRelPath = 'PRAFTA\prafta-app-frontend\prafta-app-frontend'

function Write-Step([string]$msg) { Write-Host "`n=== $msg ===" -ForegroundColor Cyan }

# AWS CLI 실행기 결정 — v2 우선, pip 판 폴백 (deploy-web.ps1 과 동일. 단일 원소 배열 return 함정 주의)
$awsExe    = $null
$awsPrefix = @()
$awsCmd    = Get-Command aws -ErrorAction SilentlyContinue
if ($awsCmd) {
    $awsExe = $awsCmd.Source
} else {
    foreach ($p in @("$env:ProgramFiles\Amazon\AWSCLIV2\aws.exe",
                     "${env:ProgramFiles(x86)}\Amazon\AWSCLIV2\aws.exe")) {
        if (Test-Path $p) { $awsExe = $p; break }
    }
}
if (-not $awsExe) { $awsExe = 'python'; $awsPrefix = @('-m', 'awscli') }

function Invoke-Aws([string[]]$AwsArgs) {
    & $awsExe @awsPrefix @AwsArgs
}

function Write-DeployLog([string]$commit, [string]$result) {
    $flag = if ($RemoteEnabled) { 'enabled' } else { 'disabled' }
    $line = "{0} | app-web | {1} | {2} | {3} (remote:{4})" -f (Get-Date -Format 'yyyy-MM-dd HH:mm:ss'), $commit, $Ref, $result, $flag
    Add-Content -Path $deployLog -Value $line -Encoding UTF8
}

function Remove-DeployWorktree {
    if (Test-Path $worktree) {
        & git -C $repoRoot worktree remove --force $worktree 2>$null
        if (Test-Path $worktree) { Remove-Item -Recurse -Force $worktree -ErrorAction SilentlyContinue }
        & git -C $repoRoot worktree prune 2>$null
    }
}

# 매니페스트 JSON 생성(셸 T4 계약: enabled / minShellBridgeVersion / entry. commit·deployedAt 은 검증·추적용)
function New-ManifestJson([string]$commit) {
    $obj = [ordered]@{
        enabled               = [bool]$RemoteEnabled.IsPresent
        minShellBridgeVersion = $MinShellBridgeVersion
        entry                 = $Entry
        commit                = $commit
        deployedAt            = (Get-Date -Format 'yyyy-MM-ddTHH:mm:sszzz')
    }
    return ($obj | ConvertTo-Json -Compress)
}

function Upload-Manifest([string]$manifestPath) {
    Invoke-Aws @('s3', 'cp', $manifestPath, "s3://$Bucket/app-manifest.json",
        '--cache-control', 'no-cache,no-store,must-revalidate',
        '--content-type', 'application/json; charset=utf-8')
    if ($LASTEXITCODE -ne 0) { throw "app-manifest.json 업로드 실패 (exit $LASTEXITCODE)" }
}

# ── 0) 배포 대상 코드 확정 ──────────────────────────────
$commit = $null
$appRoot = $null
try {
    if ($UseWorkingTree) {
        Write-Step "0/6 (비상 모드) 로컬 작업 트리 기준 배포"
        $appRoot = Join-Path $repoRoot $appRelPath
        $head = (& git -C $repoRoot rev-parse --short=8 HEAD).Trim()
        $dirty = if ((& git -C $repoRoot status --porcelain | Measure-Object).Count -gt 0) { '+dirty' } else { '' }
        $commit = "worktree:$head$dirty"
        Write-Host "주의: git 미커밋 변경이 그대로 배포될 수 있음 ($commit)" -ForegroundColor Yellow
    } else {
        Write-Step "0/6 배포 대상 확정: $Ref"
        if (-not $SkipFetch) {
            & git -C $repoRoot fetch origin
            if ($LASTEXITCODE -ne 0) { throw "git fetch 실패" }
        }
        $commit = (& git -C $repoRoot rev-parse --verify --short=8 "$Ref^{commit}").Trim()
        if ($LASTEXITCODE -ne 0 -or -not $commit) { throw "ref 해석 실패: $Ref (push 되었는지 확인)" }
        Write-Host "배포 커밋: $commit ($Ref)"
        & git -C $repoRoot log -1 --format='  %h %s (%ci)' $commit
    }

    # ── ManifestOnly: 킬 스위치 즉시 반영 경로 ───────────
    if ($ManifestOnly) {
        Write-Step "1/2 매니페스트만 갱신 (enabled:$($RemoteEnabled.IsPresent))"
        $tmpManifest = Join-Path (Resolve-TempRoot) 'prafta-app-manifest.json'
        [IO.File]::WriteAllText($tmpManifest, (New-ManifestJson $commit), (New-Object Text.UTF8Encoding($false)))
        Upload-Manifest $tmpManifest

        if (-not $SkipInvalidation -and $DistributionId) {
            Invoke-Aws @('cloudfront', 'create-invalidation', '--distribution-id', $DistributionId, '--paths', '/app-manifest.json')
            if ($LASTEXITCODE -ne 0) { Write-Host "무효화 실패 — 콘솔 수동 실행 필요" -ForegroundColor Yellow }
        }
        Write-Step "2/2 배포 이력 기록"
        Write-DeployLog $commit 'OK(manifest-only)'
        Write-Host "`n매니페스트 갱신 완료 (enabled:$($RemoteEnabled.IsPresent))." -ForegroundColor Green
        exit 0
    }

    if (-not $UseWorkingTree) {
        Remove-DeployWorktree
        & git -C $repoRoot worktree add --detach $worktree $commit
        if ($LASTEXITCODE -ne 0) { throw "git worktree 생성 실패" }
        $appRoot = Join-Path $worktree $appRelPath

        # 빌드타임 env 복사(gitignore 대상). 앱 프론트는 웹과 달리 .env 가 필수 아님 — 있으면 복사.
        $localAppRoot = Join-Path $repoRoot $appRelPath
        foreach ($envName in @('.env', '.env.production')) {
            $envSrc = Join-Path $localAppRoot $envName
            if (Test-Path $envSrc) { Copy-Item $envSrc (Join-Path $appRoot $envName) -Force }
        }
    }

    $distDir      = Join-Path $appRoot 'dist'
    $indexHtml    = Join-Path $distDir 'index.html'
    $manifestPath = Join-Path $distDir 'app-manifest.json'

    # ── 1) 의존성 설치 + 빌드 ────────────────────────────
    Write-Step "1/6 npm ci + vite 빌드 ($appRoot)"
    Push-Location $appRoot
    try {
        if (-not $UseWorkingTree) {
            & npm ci --no-audit --no-fund
            if ($LASTEXITCODE -ne 0) { throw "npm ci 실패 (exit $LASTEXITCODE)" }
        }
        & npm run build
        if ($LASTEXITCODE -ne 0) { throw "npm run build 실패 (exit $LASTEXITCODE)" }
    } finally { Pop-Location }

    if (-not (Test-Path $indexHtml)) { throw "빌드 산출물 없음: $indexHtml" }

    # ── 2) 배포 식별자 주입 + 매니페스트 생성 ────────────
    Write-Step "2/6 __APP_BUILD__ 주입 + app-manifest.json 생성"
    $buildScript = "<script>window.__APP_BUILD__ = { commit: `"$commit`", builtAt: `"$(Get-Date -Format 'yyyy-MM-ddTHH:mm:sszzz')`" };</script>"
    $html = [IO.File]::ReadAllText($indexHtml)
    if ($html -match '__APP_BUILD__') {
        Write-Host "이미 주입되어 있음 — 건너뜀"
    } else {
        $html = $html -replace '</head>', "  $buildScript`n  </head>"
        [IO.File]::WriteAllText($indexHtml, $html, (New-Object Text.UTF8Encoding($false)))
    }
    if (-not ([IO.File]::ReadAllText($indexHtml) -match '__APP_BUILD__')) {
        throw "__APP_BUILD__ 주입 실패 — 업로드 중단."
    }

    [IO.File]::WriteAllText($manifestPath, (New-ManifestJson $commit), (New-Object Text.UTF8Encoding($false)))
    Write-Host "매니페스트: $(Get-Content $manifestPath -Raw)"

    # ── 3) S3 업로드 ─────────────────────────────────────
    Write-Step "3/6 S3 업로드 (s3://$Bucket)"
    # 해시 청크(에셋)는 장기 캐시. index.html·app-manifest.json 은 별도 no-cache 업로드(D5).
    Invoke-Aws @('s3', 'sync', $distDir, "s3://$Bucket/", '--delete',
        '--cache-control', 'public,max-age=31536000,immutable',
        '--exclude', 'index.html', '--exclude', 'app-manifest.json')
    if ($LASTEXITCODE -ne 0) { throw "s3 sync 실패 (exit $LASTEXITCODE)" }

    Invoke-Aws @('s3', 'cp', $indexHtml, "s3://$Bucket/index.html",
        '--cache-control', 'no-cache,no-store,must-revalidate',
        '--content-type', 'text/html; charset=utf-8')
    if ($LASTEXITCODE -ne 0) { throw "index.html 업로드 실패 (exit $LASTEXITCODE)" }

    Upload-Manifest $manifestPath

    # ── 4) CloudFront 캐시 무효화 ────────────────────────
    if (-not $SkipInvalidation -and $DistributionId) {
        Write-Step "4/6 CloudFront 캐시 무효화 ($DistributionId)"
        Invoke-Aws @('cloudfront', 'create-invalidation', '--distribution-id', $DistributionId, '--paths', '/*')
        if ($LASTEXITCODE -ne 0) {
            Write-Host "CloudFront 무효화 실패 — 콘솔에서 수동 실행: CloudFront → $DistributionId → Invalidations → /*" -ForegroundColor Yellow
        }
    } else {
        Write-Step "4/6 무효화 생략 $(if (-not $DistributionId) { '(-DistributionId 미지정 — T6 후 지정)' } else { '(-SkipInvalidation)' })"
    }

    # ── 5) 라이브 검증 — 매니페스트 커밋 해시 대조 ───────
    Write-Step "5/6 라이브 검증 ($LiveUrl/app-manifest.json)"
    $verified = $false
    for ($i = 1; $i -le 6; $i++) {
        Start-Sleep -Seconds 5
        try {
            $resp = Invoke-WebRequest -Uri "$LiveUrl/app-manifest.json?deployCheck=$i" -UseBasicParsing -TimeoutSec 15
            if ($resp.Content -match [regex]::Escape($commit)) { $verified = $true; break }
            Write-Host "응답은 왔으나 커밋 해시 미일치 — 캐시 전파 대기 중... ($i/6)"
        } catch {
            Write-Host "라이브 응답 대기 중... ($i/6)"
        }
    }

    # ── 6) 배포 이력 기록 ────────────────────────────────
    Write-Step "6/6 배포 이력 기록"
    if ($verified) {
        Write-DeployLog $commit 'OK'
        Write-Host "`n배포 완료. 커밋 $commit 라이브 반영 확인됨 (remote enabled:$($RemoteEnabled.IsPresent))." -ForegroundColor Green
        exit 0
    }

    Write-DeployLog $commit 'OK(unverified)'
    Write-Host "`n업로드는 성공했으나 라이브 검증 미통과 — CloudFront 전파 지연 또는 T6 미완료." -ForegroundColor Yellow
    exit 1
} finally {
    if (-not $UseWorkingTree -and -not $ManifestOnly) { Remove-DeployWorktree }
}
