<#
 PRAFTA 백엔드 배포 스크립트 (AWS EC2)

 절차:
   1) 로컬 gradle 빌드 (bootJar, 테스트 제외)
   2) JAR 를 서버에 임시 이름(.new)으로 업로드 (전송 중 실패가 라이브 JAR 를 오염시키지 않도록)
   3) 서버에서 현재 JAR 를 .prev 로 백업 → .new 를 정식 이름으로 교체 → systemd 재기동
   4) https://api.prafta.com 헬스체크 (Spring 이 응답하면 성공, nginx 502/503/504 면 기동 실패로 간주)
   5) 헬스체크 실패 시 .prev 로 자동 롤백 후 재기동

 사용:
   powershell -ExecutionPolicy Bypass -File .\scripts\deploy-backend.ps1
   powershell -ExecutionPolicy Bypass -File .\scripts\deploy-backend.ps1 -SkipBuild   # 빌드 생략(직전 빌드 재사용)

 전제:
   - SSH 키: %USERPROFILE%\.ssh\prafta-key.pem
   - 서버 배치 구조는 .claude/refs/AWS_배포현황_및_운영전환가이드.md §2 참조
#>
[CmdletBinding()]
param(
    [switch]$SkipBuild,
    [int]$HealthTimeoutSec = 180,
    [string]$KeyPath = "$env:USERPROFILE\.ssh\prafta-key.pem",
    [string]$RemoteTarget = "ec2-user@3.38.237.103",
    [string]$HealthUrl = "https://api.prafta.com/prafta/",
    [string]$JavaHome = "C:\Java\jdk-21.0.2"
)

$ErrorActionPreference = 'Stop'
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12

$backendRoot = Split-Path -Parent $PSScriptRoot
$localJar    = Join-Path $backendRoot 'build\libs\prafta-backend-0.0.1-SNAPSHOT.jar'
$remoteDir   = '/home/ec2-user/prafta'
$remoteJar   = "$remoteDir/prafta-backend.jar"

function Write-Step([string]$msg) { Write-Host "`n=== $msg ===" -ForegroundColor Cyan }

# ssh 원격 명령 실행 (BatchMode: 비밀번호 prompt 로 hang 하지 않도록)
function Invoke-RemoteSsh([string]$remoteCmd) {
    & ssh -i $KeyPath -o BatchMode=yes -o ConnectTimeout=10 $RemoteTarget $remoteCmd
    if ($LASTEXITCODE -ne 0) { throw "SSH 원격 명령 실패 (exit $LASTEXITCODE): $remoteCmd" }
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

# 재기동 후 기동 완료까지 폴링
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

# ── 1) 빌드 ──────────────────────────────────────────────
if (-not $SkipBuild) {
    Write-Step "1/4 gradle bootJar 빌드"
    if (-not (Test-Path $JavaHome)) { throw "JAVA_HOME 경로 없음: $JavaHome" }
    $env:JAVA_HOME = $JavaHome
    Push-Location $backendRoot
    try {
        & .\gradlew.bat clean bootJar -x test --no-daemon
        if ($LASTEXITCODE -ne 0) { throw "gradle 빌드 실패 (exit $LASTEXITCODE)" }
    } finally { Pop-Location }
} else {
    Write-Step "1/4 빌드 생략 (-SkipBuild)"
}

if (-not (Test-Path $localJar)) { throw "빌드 산출물 없음: $localJar" }
$jarInfo = Get-Item $localJar
Write-Host ("JAR: {0} ({1:N1} MB, {2})" -f $jarInfo.Name, ($jarInfo.Length / 1MB), $jarInfo.LastWriteTime)

# ── 2) 업로드 (.new 임시 이름) ───────────────────────────
Write-Step "2/4 JAR 업로드"
& scp -i $KeyPath -o BatchMode=yes -o ConnectTimeout=10 $localJar "${RemoteTarget}:${remoteJar}.new"
if ($LASTEXITCODE -ne 0) { throw "scp 업로드 실패 (exit $LASTEXITCODE)" }

# ── 3) 백업 → 교체 → 재기동 ─────────────────────────────
Write-Step "3/4 서버 교체 및 재기동"
Invoke-RemoteSsh "cp -f $remoteJar $remoteJar.prev 2>/dev/null; mv -f $remoteJar.new $remoteJar && sudo systemctl restart prafta-backend"

# ── 4) 헬스체크 → 실패 시 롤백 ──────────────────────────
Write-Step "4/4 헬스체크 ($HealthUrl)"
if (Wait-ApiAlive $HealthTimeoutSec) {
    Write-Host "`n배포 완료. 신규 JAR 라이브 반영됨." -ForegroundColor Green
    exit 0
}

Write-Host "`n헬스체크 실패 — 이전 JAR 로 롤백합니다." -ForegroundColor Yellow
Invoke-RemoteSsh "mv -f $remoteJar.prev $remoteJar && sudo systemctl restart prafta-backend"

if (Wait-ApiAlive $HealthTimeoutSec) {
    Write-Host "`n롤백 성공: 이전 버전으로 복구됨. 신규 JAR 는 반영되지 않았음." -ForegroundColor Yellow
    Write-Host "서버 로그 확인: ssh -i $KeyPath $RemoteTarget `"sudo journalctl -u prafta-backend -n 100 --no-pager`""
    exit 1
}

Write-Host "`n롤백 후에도 응답 없음 — 수동 조치 필요!" -ForegroundColor Red
Write-Host "  ssh -i $KeyPath $RemoteTarget"
Write-Host "  sudo journalctl -u prafta-backend -n 200 --no-pager"
exit 2
