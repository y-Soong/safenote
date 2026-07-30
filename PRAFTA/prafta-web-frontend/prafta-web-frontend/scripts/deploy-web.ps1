<#
 PRAFTA 웹 프론트 배포 스크립트 (S3 + CloudFront) — git 기반

 배포 원칙: 로컬 작업 트리가 아니라 **git 에 커밋·push 된 코드**만 배포한다.
   1) git fetch 후 지정 ref(기본 origin/main)를 임시 worktree 에 체크아웃
   2) 로컬 .env/.env.production(gitignore 대상)을 worktree 로 복사 후 npm ci + vite 빌드
      (env 미복사 시 VITE_PUBLIC_* 키가 undefined 로 빌드되어 카카오맵 등이 죽는다)
   3) dist/index.html <head> 에 런타임 설정(window.__APP_CONFIG__) 자동 주입 + 주입 가드
   4) S3 업로드(에셋 장기캐시 / index.html no-cache) → CloudFront 무효화(E37OL8Q9Q1FSLZ)
   5) 라이브 검증 후 배포 커밋 해시를 .claude/refs/deploy-history.log 에 기록

 사용:
   powershell -ExecutionPolicy Bypass -File .\scripts\deploy-web.ps1                        # origin/main 배포
   powershell -ExecutionPolicy Bypass -File .\scripts\deploy-web.ps1 -Ref origin/develop    # 특정 브랜치 배포 (개발환경 생기면)
   powershell -ExecutionPolicy Bypass -File .\scripts\deploy-web.ps1 -UseWorkingTree        # (비상용) 로컬 작업 트리 그대로 배포

 전제:
   - AWS CLI: v2(단독 실행파일) 우선 사용, 없으면 pip 판(python -m awscli) 폴백. prafta-deploy 자격증명 구성됨
     (자격증명 위치 %USERPROFILE%\.aws 는 v1/v2 공통이라 CLI 를 바꿔도 재설정 불필요)
   - 상세 배경: .claude/refs/AWS_배포현황_및_운영전환가이드.md §3, §8
#>
[CmdletBinding()]
param(
    [string]$Ref = 'origin/main',
    [switch]$UseWorkingTree,
    [switch]$SkipFetch,
    [switch]$SkipInvalidation,
    [string]$ApiBase = "https://api.prafta.com",
    [string]$AppContext = "/prafta",
    [string]$Bucket = "prafta.com",
    [string]$DistributionId = "E37OL8Q9Q1FSLZ",
    [string]$LiveUrl = "https://prafta.com"
)

$ErrorActionPreference = 'Stop'
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12

$repoRoot = (& git -C $PSScriptRoot rev-parse --show-toplevel).Trim() -replace '/', '\'
if ($LASTEXITCODE -ne 0 -or -not $repoRoot) { throw "git repo 루트를 찾지 못함 ($PSScriptRoot)" }
# worktree 임시 경로 — $env:TEMP 가 시스템 temp(C:\Windows\Temp)로 잡힌 셸에서는 esbuild 가
#   상위 디렉토리를 거슬러 읽다가 "Access is denied" 로 죽는다(일반 계정은 열거 권한 없음).
#   열거 가능한지 실제로 확인하고, 안 되면 사용자 로컬 temp 로 폴백한다.
function Resolve-TempRoot {
    foreach ($cand in @($env:TEMP, (Join-Path $env:LOCALAPPDATA 'Temp'))) {
        if (-not $cand) { continue }
        try { Get-ChildItem $cand -ErrorAction Stop | Out-Null; return $cand } catch { }
    }
    throw "쓸 수 있는 임시 디렉토리를 찾지 못함 (TEMP=$env:TEMP)"
}
$worktree  = Join-Path (Resolve-TempRoot) 'prafta-deploy-wt-web'
$deployLog = Join-Path $repoRoot '.claude\refs\deploy-history.log'
$webRelPath = 'PRAFTA\prafta-web-frontend\prafta-web-frontend'

function Write-Step([string]$msg) { Write-Host "`n=== $msg ===" -ForegroundColor Cyan }

# AWS CLI 실행기 결정 — v2(단독 실행파일) 우선, 없으면 기존 pip 판(python -m awscli) 폴백.
#   2026-07-31: Anaconda 의 pyOpenSSL 19.0.0 과 사용자 site-packages 의 cryptography 45 가
#   충돌해 pip 판 awscli 가 X509_V_FLAG_NOTIFY_POLICY AttributeError 로 죽었다. CLI v2 는
#   파이썬에 의존하지 않아 이 계열 충돌이 재발하지 않는다. PATH 갱신 전 셸에서도 잡히도록
#   표준 설치 경로를 직접 확인한다.
#   ※ 함수에서 단일 원소 배열을 return 하면 PowerShell 이 문자열로 풀어버려($awsExe[0] 이 'C' 가 됨)
#     실행기 결정은 함수 없이 변수에 직접 담는다.
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

# AWS CLI 호출 래퍼 — 호출부가 실행기 형태(v2 단독 exe / python -m)를 몰라도 되게 감싼다.
function Invoke-Aws([string[]]$AwsArgs) {
    & $awsExe @awsPrefix @AwsArgs
}

# 배포 이력 기록 (로컬 .claude/refs — gitignore 대상이라 커밋되지 않음)
function Write-DeployLog([string]$commit, [string]$result) {
    $line = "{0} | web     | {1} | {2} | {3}" -f (Get-Date -Format 'yyyy-MM-dd HH:mm:ss'), $commit, $Ref, $result
    Add-Content -Path $deployLog -Value $line -Encoding UTF8
}

# 남아있는 이전 worktree 정리 (실패 잔재 대비)
function Remove-DeployWorktree {
    if (Test-Path $worktree) {
        & git -C $repoRoot worktree remove --force $worktree 2>$null
        if (Test-Path $worktree) { Remove-Item -Recurse -Force $worktree -ErrorAction SilentlyContinue }
        & git -C $repoRoot worktree prune 2>$null
    }
}

# ── 0) 배포 대상 코드 확정 ──────────────────────────────
$commit = $null
$webRoot = $null
try {
    if ($UseWorkingTree) {
        Write-Step "0/6 (비상 모드) 로컬 작업 트리 기준 배포"
        $webRoot = Join-Path $repoRoot $webRelPath
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

        Remove-DeployWorktree
        & git -C $repoRoot worktree add --detach $worktree $commit
        if ($LASTEXITCODE -ne 0) { throw "git worktree 생성 실패" }
        $webRoot = Join-Path $worktree $webRelPath

        # 빌드타임 env 복사 — .env/.env.production 은 gitignore 대상이라 깨끗한
        # worktree 에는 없다. 없이 빌드하면 VITE_PUBLIC_* 키가 undefined 로 접혀
        # 카카오맵 SDK 로드 코드가 데드코드 제거된다 (2026-07-20 운영 지도 미표시 원인).
        $localWebRoot = Join-Path $repoRoot $webRelPath
        foreach ($envName in @('.env', '.env.production')) {
            $envSrc = Join-Path $localWebRoot $envName
            if (Test-Path $envSrc) { Copy-Item $envSrc (Join-Path $webRoot $envName) -Force }
        }
        if (-not (Test-Path (Join-Path $webRoot '.env.production'))) {
            throw ".env.production 복사 실패 — 로컬 $localWebRoot 에 파일이 있는지 확인"
        }
    }

    $distDir   = Join-Path $webRoot 'dist'
    $indexHtml = Join-Path $distDir 'index.html'

    # ── 1) 의존성 설치 + 빌드 ────────────────────────────
    Write-Step "1/6 npm ci + vite 빌드 ($webRoot)"
    Push-Location $webRoot
    try {
        if (-not $UseWorkingTree) {
            # 깨끗한 worktree 라 node_modules 가 없음 — lockfile 기준 정확 설치
            & npm ci --no-audit --no-fund
            if ($LASTEXITCODE -ne 0) { throw "npm ci 실패 (exit $LASTEXITCODE)" }
        }
        & npm run build
        if ($LASTEXITCODE -ne 0) { throw "npm run build 실패 (exit $LASTEXITCODE)" }
    } finally { Pop-Location }

    if (-not (Test-Path $indexHtml)) { throw "빌드 산출물 없음: $indexHtml" }

    # 빌드 가드: 카카오맵 appkey 인라인 확인 — env 누락 빌드는 SDK 로드 코드가
    # 통째로 제거되어 지도가 전부 죽으므로 업로드 전에 차단한다.
    $kakaoOk = Get-ChildItem (Join-Path $distDir 'assets') -Filter '*.js' |
        Where-Object { Select-String -Path $_.FullName -Pattern 'dapi\.kakao\.com.*appkey=' -Quiet } |
        Select-Object -First 1
    if (-not $kakaoOk) {
        throw "빌드 산출물에 카카오맵 appkey 미포함 — .env.production 의 VITE_PUBLIC_KAKAO_APP_JS_KEY 확인. 업로드 중단."
    }
    Write-Host "카카오맵 appkey 인라인 확인: $($kakaoOk.Name)"

    # ── 2) 런타임 설정 주입 ──────────────────────────────
    Write-Step "2/6 런타임 설정 주입 (window.__APP_CONFIG__)"
    $configScript = "<script>window.__APP_CONFIG__ = { API_BASE: `"$ApiBase`", CONTEXT: `"$AppContext`" };</script>"
    $html = [IO.File]::ReadAllText($indexHtml)
    if ($html -match '__APP_CONFIG__') {
        Write-Host "이미 주입되어 있음 — 건너뜀"
    } else {
        $html = $html -replace '</head>', "  $configScript`n  </head>"
        [IO.File]::WriteAllText($indexHtml, $html, (New-Object Text.UTF8Encoding($false)))
    }
    # 주입 가드: 설정 없는 index.html 은 절대 업로드하지 않는다
    if (-not ([IO.File]::ReadAllText($indexHtml) -match '__APP_CONFIG__')) {
        throw "런타임 설정 주입 실패 — index.html 에 __APP_CONFIG__ 없음. 업로드 중단."
    }
    Write-Host "주입 확인: $configScript"

    # ── 3) S3 업로드 ─────────────────────────────────────
    Write-Step "3/6 S3 업로드 (s3://$Bucket)"
    Invoke-Aws @('s3', 'sync', $distDir, "s3://$Bucket/", '--delete', '--cache-control', 'public,max-age=31536000,immutable', '--exclude', 'index.html')
    if ($LASTEXITCODE -ne 0) { throw "s3 sync 실패 (exit $LASTEXITCODE)" }

    Invoke-Aws @('s3', 'cp', $indexHtml, "s3://$Bucket/index.html", '--cache-control', 'no-cache,no-store,must-revalidate', '--content-type', 'text/html; charset=utf-8')
    if ($LASTEXITCODE -ne 0) { throw "index.html 업로드 실패 (exit $LASTEXITCODE)" }

    # ── 4) CloudFront 캐시 무효화 ────────────────────────
    if (-not $SkipInvalidation) {
        Write-Step "4/6 CloudFront 캐시 무효화 ($DistributionId)"
        Invoke-Aws @('cloudfront', 'create-invalidation', '--distribution-id', $DistributionId, '--paths', '/*')
        if ($LASTEXITCODE -ne 0) {
            # S3 업로드는 이미 성공 — 무효화만 실패한 것이므로 배포 자체를 실패로 처리하지 않는다
            Write-Host "CloudFront 무효화 실패 — 콘솔에서 수동 실행: CloudFront → $DistributionId → Invalidations → /*" -ForegroundColor Yellow
        }
    } else {
        Write-Step "4/6 무효화 생략 (-SkipInvalidation)"
    }

    # ── 5) 라이브 검증 ───────────────────────────────────
    Write-Step "5/6 라이브 검증 ($LiveUrl)"
    $verified = $false
    for ($i = 1; $i -le 6; $i++) {
        Start-Sleep -Seconds 5
        try {
            $resp = Invoke-WebRequest -Uri "$LiveUrl/?deployCheck=$i" -UseBasicParsing -TimeoutSec 15
            if ($resp.Content -match '__APP_CONFIG__') { $verified = $true; break }
            Write-Host "응답은 왔으나 __APP_CONFIG__ 미포함 — 캐시 전파 대기 중... ($i/6)"
        } catch {
            Write-Host "라이브 응답 대기 중... ($i/6)"
        }
    }

    # ── 6) 배포 이력 기록 ────────────────────────────────
    Write-Step "6/6 배포 이력 기록"
    if ($verified) {
        Write-DeployLog $commit 'OK'
        Write-Host "`n배포 완료. 커밋 $commit 라이브 반영 확인됨." -ForegroundColor Green
        exit 0
    }

    Write-DeployLog $commit 'OK(unverified)'
    Write-Host "`n업로드는 성공했으나 라이브 검증 미통과 — CloudFront 전파 지연일 수 있음." -ForegroundColor Yellow
    Write-Host "1~2분 후 브라우저에서 직접 확인: $LiveUrl (개발자도구 콘솔에서 window.__APP_CONFIG__ 출력)"
    exit 1
} finally {
    if (-not $UseWorkingTree) { Remove-DeployWorktree }
}
