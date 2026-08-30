<#
 PRAFTA 통합 배포 스크립트 — 백엔드 + 웹(관리자) + 앱(웹뷰 콘텐츠)을 한 번에 순서대로 배포한다.

 이 스크립트 자체는 새 배포 로직을 만들지 않는다. 이미 각자 검증된 3개 스크립트를 정해진 순서로
 호출하고, 실패 시 뒷단계를 막는 안전장치만 얹는다:
   1) deploy-backend.ps1  (git worktree 빌드 → 헬스체크 → 실패 시 자동 롤백)
   2) deploy-web.ps1      (S3 + CloudFront, 라이브 검증)
   3) deploy-app-web.ps1  (S3 + CloudFront, 매니페스트 커밋해시 대조)

 실패 처리 원칙:
   - 백엔드가 실패(롤백됐든 안됐든, exit 1/2)하면 웹/앱 배포는 아예 시도하지 않고 즉시 중단한다
     (API 형상이 불안정한 상태에서 프론트만 새로 나가면 화면-서버 계약이 어긋날 수 있음).
   - 웹/앱은 "업로드는 됐으나 라이브 검증만 미통과"(exit 1)면 경고만 남기고 다음 단계는 계속 진행한다
     (CloudFront 캐시 전파 지연은 실제 배포 실패가 아닌 경우가 많음 — 각 스크립트의 기존 판단 기준 그대로).
   - 어느 스크립트든 예상 밖의 종료코드(2 이상, 백엔드의 완전 실패 제외)를 내면 그 자리에서 중단한다.

 사용:
   powershell -ExecutionPolicy Bypass -File .\scripts\deploy-all.ps1                     # 셋 다, origin/main
   powershell -ExecutionPolicy Bypass -File .\scripts\deploy-all.ps1 -SkipApp             # 백엔드+웹만
   powershell -ExecutionPolicy Bypass -File .\scripts\deploy-all.ps1 -SkipBackend -SkipApp # 웹만
   powershell -ExecutionPolicy Bypass -File .\scripts\deploy-all.ps1 -Ref 03169b5         # 특정 커밋(핫픽스/롤백) 셋 다

 전제: 각 하위 스크립트의 전제(SSH 키, AWS CLI 자격증명 등)와 동일 — 이 스크립트는 그걸 대신 갖추지 않는다.
#>
[CmdletBinding()]
param(
    [string]$Ref = 'origin/main',
    [switch]$SkipBackend,
    [switch]$SkipWeb,
    [switch]$SkipApp,
    [switch]$UseWorkingTree,
    [switch]$SkipFetch
)

$ErrorActionPreference = 'Stop'
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12

$repoRoot = (& git -C $PSScriptRoot rev-parse --show-toplevel).Trim() -replace '/', '\'
if ($LASTEXITCODE -ne 0 -or -not $repoRoot) { throw "git repo 루트를 찾지 못함 ($PSScriptRoot)" }

$backendDir = Join-Path $repoRoot 'PRAFTA\prafta-backend'
$webDir     = Join-Path $repoRoot 'PRAFTA\prafta-web-frontend\prafta-web-frontend'
$appDir     = Join-Path $repoRoot 'PRAFTA\prafta-app-frontend\prafta-app-frontend'

function Write-Stage([string]$msg) { Write-Host "`n########## $msg ##########" -ForegroundColor Magenta }

$results = [ordered]@{}
$commonArgs = @('-Ref', $Ref)
if ($UseWorkingTree) { $commonArgs += '-UseWorkingTree' }
if ($SkipFetch)      { $commonArgs += '-SkipFetch' }

function Invoke-DeployScript([string]$label, [string]$dir, [string]$scriptRelPath, [string[]]$extraArgs) {
    Write-Stage $label
    Push-Location $dir
    try {
        & powershell -ExecutionPolicy Bypass -File $scriptRelPath @commonArgs @extraArgs
        return $LASTEXITCODE
    } finally {
        Pop-Location
    }
}

# ── 1) 백엔드 ──────────────────────────────────────────
if ($SkipBackend) {
    Write-Stage "1/3 백엔드 — 건너뜀(-SkipBackend)"
    $results['backend'] = 'SKIPPED'
} else {
    $code = Invoke-DeployScript '1/3 백엔드' $backendDir '.\scripts\deploy-backend.ps1' @()
    if ($code -eq 0) {
        $results['backend'] = 'OK'
    } else {
        $results['backend'] = "FAIL(exit $code)"
        Write-Host "`n백엔드 배포 실패(exit $code) — 웹/앱 배포는 진행하지 않습니다." -ForegroundColor Red
        Write-Host "`n===== 배포 결과 요약 =====" -ForegroundColor Cyan
        $results.GetEnumerator() | ForEach-Object { Write-Host ("  {0,-8} {1}" -f $_.Key, $_.Value) }
        exit 1
    }
}

# ── 2) 웹(관리자) ──────────────────────────────────────
if ($SkipWeb) {
    Write-Stage "2/3 웹 — 건너뜀(-SkipWeb)"
    $results['web'] = 'SKIPPED'
} else {
    $code = Invoke-DeployScript '2/3 웹(관리자)' $webDir '.\scripts\deploy-web.ps1' @()
    if ($code -eq 0) {
        $results['web'] = 'OK'
    } elseif ($code -eq 1) {
        $results['web'] = 'OK(라이브 검증 미확인 — 캐시 전파 대기 가능성)'
        Write-Host "`n웹: 업로드는 됐으나 라이브 검증 미통과 — 계속 진행합니다." -ForegroundColor Yellow
    } else {
        $results['web'] = "FAIL(exit $code)"
        Write-Host "`n웹 배포 실패(exit $code) — 앱 배포는 진행하지 않습니다." -ForegroundColor Red
        Write-Host "`n===== 배포 결과 요약 =====" -ForegroundColor Cyan
        $results.GetEnumerator() | ForEach-Object { Write-Host ("  {0,-8} {1}" -f $_.Key, $_.Value) }
        exit 1
    }
}

# ── 3) 앱(웹뷰 콘텐츠) ─────────────────────────────────
if ($SkipApp) {
    Write-Stage "3/3 앱 — 건너뜀(-SkipApp)"
    $results['app'] = 'SKIPPED'
} else {
    $code = Invoke-DeployScript '3/3 앱(웹뷰 콘텐츠)' $appDir '.\scripts\deploy-app-web.ps1' @()
    if ($code -eq 0) {
        $results['app'] = 'OK'
    } elseif ($code -eq 1) {
        $results['app'] = 'OK(라이브 검증 미확인 — 캐시 전파 대기 가능성)'
        Write-Host "`n앱: 업로드는 됐으나 라이브 검증 미통과." -ForegroundColor Yellow
    } else {
        $results['app'] = "FAIL(exit $code)"
    }
}

Write-Host "`n===== 배포 결과 요약 (ref=$Ref) =====" -ForegroundColor Cyan
$results.GetEnumerator() | ForEach-Object { Write-Host ("  {0,-8} {1}" -f $_.Key, $_.Value) }

if ($results.Values -match '^FAIL') { exit 1 }
exit 0

