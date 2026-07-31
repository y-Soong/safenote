<#
 PRAFTA 백엔드 배포 스크립트 (AWS EC2) — git 기반

 배포 원칙: 로컬 작업 트리가 아니라 **git 에 커밋·push 된 코드**만 배포한다.
   1) git fetch 후 지정 ref(기본 origin/main)를 임시 worktree 에 체크아웃
   2) 그 worktree 안에서 gradle bootJar 빌드 (로컬 미커밋 변경은 절대 섞이지 않음)
   3) JAR 를 서버에 임시 이름(.new)으로 업로드 → 현재 JAR .prev 백업 → 교체 → 재기동
   4) https://api.prafta.com 헬스체크 → 실패 시 .prev 로 자동 롤백
   5) 배포 커밋 해시를 로컬 이력(.claude/refs/deploy-history.log)과 서버(DEPLOYED_COMMIT_BACKEND)에 기록

 사용:
   powershell -ExecutionPolicy Bypass -File .\scripts\deploy-backend.ps1                        # origin/main 배포
   powershell -ExecutionPolicy Bypass -File .\scripts\deploy-backend.ps1 -Ref origin/develop    # 특정 브랜치 배포 (개발 WAS 생기면)
   powershell -ExecutionPolicy Bypass -File .\scripts\deploy-backend.ps1 -Ref 03169b5           # 특정 커밋 배포 (핫픽스/롤백)
   powershell -ExecutionPolicy Bypass -File .\scripts\deploy-backend.ps1 -UseWorkingTree        # (비상용) 로컬 작업 트리 그대로 배포

 전제:
   - SSH 키: %USERPROFILE%\.ssh\prafta-key.pem
   - 서버 배치 구조는 .claude/refs/AWS_배포현황_및_운영전환가이드.md §2 참조
#>
[CmdletBinding()]
param(
    [string]$Ref = 'origin/main',
    [switch]$UseWorkingTree,
    [switch]$SkipFetch,
    [int]$HealthTimeoutSec = 180,
    [string]$KeyPath = "$env:USERPROFILE\.ssh\prafta-key.pem",
    [string]$RemoteTarget = "ec2-user@3.38.237.103",
    [string]$HealthUrl = "https://api.prafta.com/prafta/",
    [string]$JavaHome = "C:\Java\jdk-21.0.2"
)

$ErrorActionPreference = 'Stop'
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12

$repoRoot  = (& git -C $PSScriptRoot rev-parse --show-toplevel).Trim() -replace '/', '\'
if ($LASTEXITCODE -ne 0 -or -not $repoRoot) { throw "git repo 루트를 찾지 못함 ($PSScriptRoot)" }
$remoteDir = '/home/ec2-user/prafta'
$remoteJar = "$remoteDir/prafta-backend.jar"

# 임시 디렉토리 선정 — 비대화형 셸에서는 $env:TEMP 가 C:\WINDOWS\TEMP 로 잡히는데
# 일반 계정은 그 디렉토리를 '열거'할 권한이 없어 빌드 도구가 상위를 거슬러 읽다 실패한다.
# 실제로 열거 가능한지 확인하고, 안 되면 사용자 로컬 temp 로 폴백한다(deploy-web.ps1 과 동일).
function Resolve-TempRoot {
    foreach ($cand in @($env:TEMP, (Join-Path $env:LOCALAPPDATA 'Temp'))) {
        if (-not $cand) { continue }
        try { Get-ChildItem $cand -ErrorAction Stop | Out-Null; return $cand } catch { }
    }
    throw "쓸 수 있는 임시 디렉토리를 찾지 못함 (TEMP=$env:TEMP)"
}
$worktree  = Join-Path (Resolve-TempRoot) 'prafta-deploy-wt-backend'
$deployLog = Join-Path $repoRoot '.claude\refs\deploy-history.log'

function Write-Step([string]$msg) { Write-Host "`n=== $msg ===" -ForegroundColor Cyan }

function Invoke-RemoteSsh([string]$remoteCmd) {
    & ssh -i $KeyPath -o BatchMode=yes -o ConnectTimeout=10 $RemoteTarget $remoteCmd
    if ($LASTEXITCODE -ne 0) { throw "SSH 원격 명령 실패 (exit $LASTEXITCODE): $remoteCmd" }
}

# 배포 이력 기록 (로컬 .claude/refs — gitignore 대상이라 커밋되지 않음)
function Write-DeployLog([string]$commit, [string]$result) {
    $line = "{0} | backend | {1} | {2} | {3}" -f (Get-Date -Format 'yyyy-MM-dd HH:mm:ss'), $commit, $Ref, $result
    Add-Content -Path $deployLog -Value $line -Encoding UTF8
}

# 백엔드 생존 판정: Spring 이 응답하면 살아있음, 연결실패/502/503/504(nginx 대리응답) 면 죽어있음
# ※ 이 앱은 미매핑 경로에 500 을 반환한다(전역 예외 처리 특성, 2026-07-19 실측) — 500 도 생존 신호가 맞다
function Test-ApiAlive {
    try {
        Invoke-WebRequest -Uri $HealthUrl -Method Get -UseBasicParsing -TimeoutSec 10 | Out-Null
        return $true
    } catch {
        $status = $null
        if ($_.Exception.Response) { $status = [int]$_.Exception.Response.StatusCode }
        if ($null -eq $status) { return $false }
        if ($status -eq 502 -or $status -eq 503 -or $status -eq 504) { return $false }
        return $true
    }
}

function Wait-ApiAlive([int]$timeoutSec) {
    $elapsed = 0
    while ($elapsed -lt $timeoutSec) {
        Start-Sleep -Seconds 5
        $elapsed += 5
        if (Test-ApiAlive) {
            Write-Host "헬스체크 통과 (경과 ${elapsed}s)" -ForegroundColor Green
            return $true
        }
        Write-Host "기동 대기 중... (${elapsed}s / ${timeoutSec}s)"
    }
    return $false
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
$buildRoot = $null
try {
    if ($UseWorkingTree) {
        Write-Step "0/5 (비상 모드) 로컬 작업 트리 기준 배포"
        $buildRoot = Join-Path $repoRoot 'PRAFTA\prafta-backend'
        $head = (& git -C $repoRoot rev-parse --short=8 HEAD).Trim()
        $dirty = if ((& git -C $repoRoot status --porcelain | Measure-Object).Count -gt 0) { '+dirty' } else { '' }
        $commit = "worktree:$head$dirty"
        Write-Host "주의: git 미커밋 변경이 그대로 배포될 수 있음 ($commit)" -ForegroundColor Yellow
    } else {
        Write-Step "0/5 배포 대상 확정: $Ref"
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
        $buildRoot = Join-Path $worktree 'PRAFTA\prafta-backend'
    }

    # ── 1) 빌드 ──────────────────────────────────────────
    Write-Step "1/5 gradle bootJar 빌드 ($buildRoot)"
    if (-not (Test-Path $JavaHome)) { throw "JAVA_HOME 경로 없음: $JavaHome" }
    $env:JAVA_HOME = $JavaHome
    Push-Location $buildRoot
    try {
        & .\gradlew.bat clean bootJar -x test --no-daemon
        if ($LASTEXITCODE -ne 0) { throw "gradle 빌드 실패 (exit $LASTEXITCODE)" }
    } finally { Pop-Location }

    $localJar = Join-Path $buildRoot 'build\libs\prafta-backend-0.0.1-SNAPSHOT.jar'
    if (-not (Test-Path $localJar)) { throw "빌드 산출물 없음: $localJar" }
    $jarInfo = Get-Item $localJar
    Write-Host ("JAR: {0} ({1:N1} MB)" -f $jarInfo.Name, ($jarInfo.Length / 1MB))

    # ── 2) 업로드 (.new 임시 이름) ───────────────────────
    Write-Step "2/5 JAR 업로드"
    & scp -i $KeyPath -o BatchMode=yes -o ConnectTimeout=10 $localJar "${RemoteTarget}:${remoteJar}.new"
    if ($LASTEXITCODE -ne 0) { throw "scp 업로드 실패 (exit $LASTEXITCODE)" }

    # ── 3) 백업 → 교체 → 재기동 ─────────────────────────
    Write-Step "3/5 서버 교체 및 재기동"
    Invoke-RemoteSsh "cp -f $remoteJar $remoteJar.prev 2>/dev/null; mv -f $remoteJar.new $remoteJar && sudo systemctl restart prafta-backend"

    # ── 4) 헬스체크 → 실패 시 롤백 ──────────────────────
    Write-Step "4/5 헬스체크 ($HealthUrl)"
    if (Wait-ApiAlive $HealthTimeoutSec) {
        # ── 5) 배포 커밋 기록 ────────────────────────────
        Write-Step "5/5 배포 이력 기록"
        Invoke-RemoteSsh "echo '$commit $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')' > $remoteDir/DEPLOYED_COMMIT_BACKEND"
        Write-DeployLog $commit 'OK'
        Write-Host "`n배포 완료. 커밋 $commit 라이브 반영됨." -ForegroundColor Green
        exit 0
    }

    Write-Host "`n헬스체크 실패 — 이전 JAR 로 롤백합니다." -ForegroundColor Yellow
    Invoke-RemoteSsh "mv -f $remoteJar.prev $remoteJar && sudo systemctl restart prafta-backend"

    if (Wait-ApiAlive $HealthTimeoutSec) {
        Write-DeployLog $commit 'ROLLBACK'
        Write-Host "`n롤백 성공: 이전 버전으로 복구됨. 커밋 $commit 은 반영되지 않았음." -ForegroundColor Yellow
        Write-Host "서버 로그 확인: ssh -i $KeyPath $RemoteTarget `"sudo journalctl -u prafta-backend -n 100 --no-pager`""
        exit 1
    }

    Write-DeployLog $commit 'FAIL'
    Write-Host "`n롤백 후에도 응답 없음 — 수동 조치 필요!" -ForegroundColor Red
    Write-Host "  ssh -i $KeyPath $RemoteTarget"
    Write-Host "  sudo journalctl -u prafta-backend -n 200 --no-pager"
    exit 2
} finally {
    if (-not $UseWorkingTree) { Remove-DeployWorktree }
}
