# PRAFTA AWS 배포 현황 및 운영 전환 가이드

> 작성일: 2026-07-18 / 최종 갱신: 2026-07-19 (★07-19 밤 파일 유실 발견 → 세션 기록에서 전문 복원 + 실무 명령어 최상단 재배치)
> 작성 배경: 로컬 개발 환경에서 운영하던 PRAFTA 전체 스택(백엔드·웹·앱·AI)을 AWS 클라우드로 이전 완료.
> 현재 단계: **시연 단계** (실고객 0, 무료 플랜 크레딧 운영)
> **★2026-07-18 앞단 Cloudflare→CloudFront+WAF 전환 완료(§8). ★2026-07-19 git 기반 배포 스크립트 정립 + HTTP 80 폐쇄 + CloudFront 오리진 https-only 수정(§3·§6-14·§8). ★2026-08-04 앱 프론트(웹뷰 콘텐츠) 원격 호스팅 `app.prafta.com` 신설(§2·§3 — 현재 킬 스위치 OFF=전원 번들).**

---

## ★★ 실무 배포 명령어 모음 (평소엔 여기만 보면 됨) ★★

```powershell
# ── 릴리즈 흐름 (develop 에서 작업 완료 후) ──
cd C:\PRAFTA
git checkout main; git merge develop; git push origin main; git checkout develop

# ── 마이그레이션 SQL 적용 (신규 SQL 있을 때만, 백엔드 배포 전 필수) ──
scp -i C:\Users\dudjs\.ssh\prafta-key.pem <SQL파일 전체경로> ec2-user@3.38.237.103:/tmp/
ssh -i C:\Users\dudjs\.ssh\prafta-key.pem ec2-user@3.38.237.103 "mysql prafta < /tmp/<파일명> && rm /tmp/<파일명>"

# ── 백엔드 배포 (origin/main) ──
cd C:\PRAFTA\PRAFTA\prafta-backend
powershell -ExecutionPolicy Bypass -File .\scripts\deploy-backend.ps1

# ── 웹 배포 (origin/main) ──
cd C:\PRAFTA\PRAFTA\prafta-web-frontend\prafta-web-frontend
powershell -ExecutionPolicy Bypass -File .\scripts\deploy-web.ps1

# ── 앱 프론트(웹뷰 콘텐츠) 원격 배포 (origin/main → app.prafta.com) ──
cd C:\PRAFTA\PRAFTA\prafta-app-frontend\prafta-app-frontend
powershell -ExecutionPolicy Bypass -File .\scripts\deploy-app-web.ps1 -DistributionId E20RXW16D6VDSN
#   원격 활성화 배포 = -RemoteEnabled 추가 / 킬 스위치(전원 번들 회귀) = -ManifestOnly (상세 §2 앱 프론트 섹션)

# ── 앱 APK 빌드 ──
cd C:\PRAFTA\PRAFTA_FLUTTER\safenote
powershell -ExecutionPolicy Bypass -File .\scripts\build-apk.ps1 -BaseUrl "https://api.prafta.com"

# ── 롤백 (이전 커밋 재배포) ──
powershell -ExecutionPolicy Bypass -File .\scripts\deploy-backend.ps1 -Ref <이전커밋해시>

# ── 서버 로그 확인 ──
ssh -i C:\Users\dudjs\.ssh\prafta-key.pem ec2-user@3.38.237.103 "sudo journalctl -u prafta-backend -n 100 --no-pager"

# ── AI 서버 (prafta-ai = i-0920b060dee420594) ──
# 상태 확인 (지금 바로 가능)
python -m awscli ec2 describe-instances --instance-ids i-0920b060dee420594 --region ap-northeast-2 --query "Reservations[0].Instances[0].State.Name" --output text
# 켜기 (실행 후 1~2분 대기하면 자동 준비. ※IAM 정책 추가 필요 — 아래 주석 참조)
python -m awscli ec2 start-instances --instance-ids i-0920b060dee420594 --region ap-northeast-2
# 끄기 (중지=Stop. 종료 Terminate 아님!)
python -m awscli ec2 stop-instances --instance-ids i-0920b060dee420594 --region ap-northeast-2
```

> ※ AI 켜기/끄기 CLI 는 `prafta-deploy` 에 인라인 정책 추가 후 동작 (미추가 시 콘솔에서 prafta-ai 시작/중지 — 기존 방식):
> ```json
> { "Version": "2012-10-17", "Statement": [{ "Effect": "Allow",
>   "Action": ["ec2:StartInstances", "ec2:StopInstances"],
>   "Resource": "arn:aws:ec2:ap-northeast-2:727086661681:instance/i-0920b060dee420594" }] }
> ```
> IAM → Users → prafta-deploy → 인라인 정책 생성(JSON) → 이름 `prafta-ai-startstop`. **prafta-ai 인스턴스 한정**이라 백엔드 EC2 는 못 건드림.

- 스크립트 상세 설명·옵션 = §3, 각 대상별 절차·장애 대응 = `배포_운영_매뉴얼.md`, 비상용 완전 수동 명령 = **부록 A(문서 맨 아래)**.
- 어느 커밋이 라이브인지: `.claude/refs/deploy-history.log` 또는 서버 `~/prafta/DEPLOYED_COMMIT_BACKEND`.

---

## ★★ 0. 다음 세션 Claude가 반드시 먼저 읽을 것 (오해 방지) ★★

이 문서는 초기 배포(Cloudflare 프록시 앞단) 기준으로 작성됐다가, **2026-07-18에 앞단을 AWS CloudFront + AWS WAF 로 전환**했다. 아래 5가지를 혼동하지 말 것:

1. **앞단은 이제 CloudFront다. Cloudflare는 "DNS 전용"(전 레코드 회색 구름=DNS only)으로만 남았다.** Cloudflare가 프록시/CDN/WAF 역할을 한다고 서술한 옛 문장은 **폐기됨**. 실제 CDN·WAF·HTTPS 종단은 CloudFront가 한다.
2. **`prafta.com` → CloudFront(prafta-web) → S3 웹사이트 엔드포인트**, **`api.prafta.com` → CloudFront(prafta-api) → `origin.prafta.com`(EC2 nginx :443)**. 도메인·앱APK·웹 런타임설정은 무변경.
3. **EC2 백엔드 443은 CloudFront 프리픽스 리스트로 잠갔고, 80은 완전 폐쇄했다(07-19).** `origin.prafta.com`/EC2 IP로 **직접 443/80 접근하면 타임아웃(차단)이 정상**이다. "백엔드가 안 뜬다"고 오판하지 말 것 — 반드시 `https://api.prafta.com` 경유로 확인.
4. **API용 CloudFront 배포의 오리진 요청 정책은 반드시 `AllViewer`, 오리진 프로토콜은 `https-only`(07-19 수정).** AllViewer 아니면 preflight 의 `Access-Control-Request-Headers`가 오리진에 전달 안 돼 브라우저 로그인이 CORS로 깨진다(§6-11). curl은 통과하지만 브라우저만 깨지므로 진단 시 주의.
5. **배포는 git 기반 스크립트(§3)가 표준.** 헬스체크 판정: 이 앱은 미매핑 경로에 500 을 반환하므로 **500=생존**, 다운=연결실패/502/503/504만.

---

## 1. 전체 아키텍처 (현재 라이브 상태 — 2026-07-19 기준)

```
[사용자 폰: APK]          [관리자 브라우저]
      │ HTTPS                  │ HTTPS
      ▼                        ▼
┌──────────────────────────────────────────────┐
│  AWS CloudFront (서울 엣지 ICN) + AWS WAF       │
│  prafta.com(prafta-web)  api.prafta.com(prafta-api)
│  · WAF: 필수 규칙(실차단) + Shield Standard(자동)  │
└──────────┬──────────────────────┬─────────────┘
           │ (S3 웹사이트 HTTP)      │ (origin.prafta.com HTTPS 443, https-only+TLSv1.2)
           ▼                        ▼
   [S3: prafta.com 버킷]      [EC2: prafta-backend]
   웹 정적파일               nginx :443(certbot) → :8080
                            Spring Boot (systemd)
                            ※443 인바운드 = CloudFront 프리픽스만. 80 폐쇄(07-19)
                                    │
                              ┌─────┴──────────────┐
                              ▼ (VPC 내부)          ▼ (VPC 내부)
                            [RDS MySQL]         [EC2: prafta-ai] ★평소 중지
                            prafta-db            ├ pgvector (코퍼스 50,378청크)
                            (프라이빗)            └ TEI (BGE-m3 임베딩)
                                                      │ (외부 API)
                                                      ▼
                                                [네이버 HyperCLOVA X] HCX-005

[Cloudflare] = DNS 전용(전 레코드 DNS only/회색). 프록시·CDN·WAF 역할 없음.
```

---

## 2. 리소스 상세 목록

### 계정/리전
| 항목 | 값 |
|---|---|
| AWS 계정 | 727086661681 (무료 플랜, $100+활동크레딧) |
| 리전 | **서울 (ap-northeast-2)** ※처음 시드니에 잘못 만들었다 재생성한 이력 있음 |
| IAM 배포 사용자 | `prafta-deploy` — 정책 4개: AmazonS3FullAccess + 인라인 `prafta-cloudfront-invalidation`(웹·앱 프론트 배포 무효화 한정 — ★08-04 앱 배포 ID `E20RXW16D6VDSN` 추가) + AmazonEC2ReadOnlyAccess + CloudFrontReadOnlyAccess(07-19 진단용 부착, 유지) |
| 도메인 | prafta.com (Cloudflare DNS 관리) |

### EC2 #1 — 백엔드 (`prafta-backend`) 🟢 상시 가동
| 항목 | 값 |
|---|---|
| 유형 | t3.small (2vCPU/2GB) + 스왑 2GB |
| OS | Amazon Linux 2023 (x86) |
| 고정 IP (Elastic IP) | **3.38.237.103** ※`.0`으로 끝나는 EIP는 ISP 필터링으로 SSH 불가 → 재할당했음 |
| 프라이빗 IP | 172.31.15.138 |
| 스토리지 | 30GB gp3 (암호화) |
| 보안그룹 | `prafta-backend-sg`(sg-0d78cb1a174d986fe): **SSH 22=내IP(121.161.241.60/32) + HTTPS 443=CloudFront 프리픽스 `pl-22a6434b`만 — 이 두 줄이 전부(★07-19 HTTP 80 삭제 완료)** |
| 실행 서비스 | ① `prafta-backend.service` (systemd, Spring Boot JAR) ② nginx (443 HTTPS[Let's Encrypt] → 8080 프록시). server_name = origin.prafta.com + api.prafta.com |
| SSH 접속 | `ssh -i C:\Users\dudjs\.ssh\prafta-key.pem ec2-user@3.38.237.103` |

**백엔드 배치 구조 (서버 내):**
- JAR: `/home/ec2-user/prafta/prafta-backend.jar` (+ `.prev` 자동 백업, `DEPLOYED_COMMIT_BACKEND` 커밋 마커)
- 시크릿: `/home/ec2-user/prafta/secrets/platform-bootstrap.properties` (chmod 600)
  - systemd가 `SPRING_CONFIG_IMPORT=optional:file:...`로 주입 (로컬과 동일 방식)
  - JWT/AES/pepper는 **로컬과 동일 값** (데이터 복호화·로그인 호환 필수)
  - `SPRING_PROFILES_ACTIVE=prod`
- 업로드 경로: `/home/ec2-user/prafta/uploads` (`FILE_UPLOAD_BASE_DIR`)
- RDS 접속정보: `~/.my.cnf` (mysql CLI용, chmod 600)
- nginx 설정: `/etc/nginx/conf.d/prafta.conf` ※기본 server 블록은 nginx.conf에서 제거함(백업 .bak)
- certbot DNS-01 훅: `/usr/local/bin/certbot-cf-{auth,cleanup}.sh` + 토큰 `/etc/letsencrypt/cloudflare-token`(600)

### RDS — MySQL (`prafta-db`) 🟢 상시 가동
| 항목 | 값 |
|---|---|
| 엔진 | MySQL 8.0.42, db.t4g.micro (프리티어 무료) |
| 엔드포인트 | `prafta-db.cj2yeu0mepo8.ap-northeast-2.rds.amazonaws.com:3306` |
| 마스터 계정 | `prafta` / 비번은 사용자 보관 + 백엔드EC2 `~/.my.cnf` 참조 (★문서 평문 기재 금지 — 과거 세션 노출 이력 있어 재설정은 추후 진행 예정) |
| 파라미터 그룹 | **`prafta-mysql80-params`** ← 핵심! |
| │ | `lower_case_table_names=1` (★Windows 개발DB 호환 — 이거 없으면 전체 500 에러) |
| │ | `log_bin_trust_function_creators=1` (함수/프로시저 로드용) |
| 데이터 | 로컬 `prafta` DB 전체 이전 완료 (117테이블 + 함수4/프로시저3 + 사용자 52명) |
| 접근 | 퍼블릭 액세스 없음. `default` SG에 3306=prafta-backend-sg 소스만 허용. 로컬 Workbench는 SSH 터널(EC2 경유)로 접속 |
| 백업 | 자동백업 1일 (무료 플랜 제한 — §5-4) + 수동 스냅샷 수시 |

### EC2 #2 — AI 스택 (`prafta-ai`) 🟡 평소 중지 (stop-start)
| 항목 | 값 |
|---|---|
| 유형 | m7i-flex.large (2vCPU/8GB) ※무료플랜이 t3.medium/large 차단 → flex만 가능 |
| 프라이빗 IP | **172.31.52.160** (stop/start해도 불변 — 백엔드가 이 주소로 연결) |
| 퍼블릭 IP | stop/start 시 변동 (설정용으로만 사용 — 무관) |
| 스토리지 | 40GB gp3 |
| 보안그룹 | `prafta-ai-sg`: SSH=내IP / 5432·8090=prafta-backend-sg 소스만 |
| 스택 위치 | `/home/ec2-user/prafta-ai-stack/` (docker-compose.yml + init.sql + init_ai_call.sql) |
| 컨테이너 | ① pgvector/pgvector:pg16 (:5432, DB=prafta_ai) ② TEI BGE-m3 CPU (:8090→80) — GPU 불필요 |
| 코퍼스 | **50,378청크 / 5출처** (1024차원, 로컬에서 pg_dump로 전량 이전) |
| 자동 기동 | docker enable + `restart: unless-stopped` + TEI 모델 볼륨캐시 → **start만 하면 1~2분 후 자동 준비** |

### S3 — 웹 프론트
| 항목 | 값 |
|---|---|
| S3 버킷 | **`prafta.com`** (서울) ※버킷명=도메인명 필수 |
| 정적 호스팅 | index/error 둘 다 index.html (SPA 폴백) + 공개읽기 정책 |
| 웹 엔드포인트 | http://prafta.com.s3-website.ap-northeast-2.amazonaws.com |
| 캐시 정책 | assets=장기캐시(immutable) / index.html=no-cache |

### S3 + CloudFront — 앱 프론트(웹뷰 콘텐츠, `app.prafta.com`) ★2026-08-04 신설
| 항목 | 값 |
|---|---|
| 용도 | Flutter 셸 웹뷰의 **원격 로딩** 오리진 — 원격 실패/킬 스위치 OFF 시 앱 내 번들(`assets/vue_app/`)로 자동 폴백 |
| S3 버킷 | **`app.prafta.com`** (서울, 버킷명=도메인명 규칙) — 정적 호스팅 + 퍼블릭 읽기 (웹 버킷과 동일 구성) |
| CloudFront 배포 | **`E20RXW16D6VDSN`** (d3q49w3f2hjcnc.cloudfront.net), 대체도메인 `app.prafta.com`, Free 플랜 + 번들 WAF |
| 오리진 | (Other) S3 웹사이트 엔드포인트 **HTTP only** (§6-12 규칙 동일) |
| 인증서 | ACM us-east-1 `84d3578b…` + 최소 TLSv1.2_2021. **검증 CNAME 2건(`_e01ca….app` / `_e5fd….prafta.com`)은 자동 갱신용 — Cloudflare 에서 영구 유지(삭제 금지)** |
| SPA 폴백 | 오류페이지 403/404 → /index.html + 200 |
| 캐시 정책 | `index.html`·`app-manifest.json`=no-cache / 해시 청크=장기캐시 |
| 킬 스위치 | `https://app.prafta.com/app-manifest.json` 의 `enabled` — **현재 `enabled:false` (롤아웃 1단계 = 전원 번들)** |
| 배포 스크립트 | `PRAFTA/prafta-app-frontend/prafta-app-frontend/scripts/deploy-app-web.ps1` — git 커밋 코드만 배포(worktree) + `__APP_BUILD__` 주입 + 매니페스트 생성. `-ManifestOnly`=매니페스트(enabled 토글)만 즉시 배포. ※param 기본값에 배포 ID 미반영 상태라 `-DistributionId E20RXW16D6VDSN` 지정 필요 |

- 첫 배포(08-04)는 `-UseWorkingTree` 비상 모드였음 — **정식 롤아웃 전 커밋 후 git 기반 재배포로 교체할 것.**
- 배경·로딩 전략·롤아웃 단계·테스트: `.claude/refs/앱_웹뷰_원격로딩_전환_작업지시서.md` §4·§6-2·§7-8 참조.

### Cloudflare DNS 레코드 (전 레코드 DNS only/회색)
| 레코드 | 값 | 상태 |
|---|---|---|
| `prafta.com` (@) | CNAME → **d1vtp7jqcdi3j.cloudfront.net** | 라이브 (웹→CloudFront) |
| `api.prafta.com` | CNAME → **d2iahzc1fk18ru.cloudfront.net** | 라이브 (API→CloudFront) |
| `app.prafta.com` | CNAME → **d3q49w3f2hjcnc.cloudfront.net** | 라이브 (앱 프론트→CloudFront, ★08-04 추가) |
| `origin.prafta.com` | A → 3.38.237.103 | CloudFront의 API 오리진 + certbot 검증용 |
| `_e5fd79f...` | CNAME → acm-validations.aws | ACM 인증서 검증 레코드(유지 — 삭제 시 갱신 실패) |
| `_e01ca....app` | CNAME → acm-validations.aws | 앱 프론트 ACM 검증 레코드(★08-04 추가, **영구 유지 — 삭제 시 갱신 실패**) |
| ~~`web.prafta.com`~~ | ~~Tunnel~~ | **★07-19 삭제 완료(미사용 잔존 정리)** |

> ⚠️ **CNAME 타입 변경 불가**: Cloudflare는 기존 A 레코드를 편집으로 CNAME으로 못 바꾼다 → **삭제 후 새 CNAME 추가**가 정답. / **프리픽스 리스트는 IP 버전 칸이 빈 게 정상**(pl-은 IP 묶음).

### 앱 (Android APK)
| 항목 | 값 |
|---|---|
| 산출물 | `C:\PRAFTA\PRAFTA_FLUTTER\safenote\build\app\outputs\flutter-apk\app-release.apk` (~63MB) |
| API 주소 | `--dart-define=APP_BASE_URL=https://api.prafta.com` 주입됨 |
| 재빌드 명령 | `powershell -File .\scripts\build-apk.ps1 -BaseUrl "https://api.prafta.com"` |
| CSP | connect-src에 `https://*.prafta.com` 이미 포함 (수정 불필요) |
| 배포 방식 | 현재 사이드로드. Play Store 정식 배포는 별도 진행 중(§5-14) |

---

## 3. 배포 체계 (★2026-07-19 git 기반 정립 — 실배포 검증 완료)

**배포 원칙: 로컬 작업 트리가 아니라 git 에 push 된 코드만 배포된다.** 스크립트가 `git fetch` → 지정 ref(기본 `origin/main`)를 임시 worktree 에 체크아웃 → 그 안에서 빌드 → 배포 → **배포 커밋 해시 기록**(`.claude/refs/deploy-history.log` + 서버 `DEPLOYED_COMMIT_BACKEND`). 로컬 미커밋 변경은 절대 섞이지 않는다.

**브랜치 전략(07-19 확정)**: 작업은 `develop` 브랜치에서 → 검증 후 `main` 병합·push → `main` 에서 운영 배포. 추후 개발용 WAS 가 생기면 `-Ref origin/develop` 으로 그쪽에 배포.

| 대상 | 스크립트 | 핵심 동작 |
|---|---|---|
| 백엔드 | `PRAFTA/prafta-backend/scripts/deploy-backend.ps1` | worktree 빌드 + .new 업로드/.prev 백업 + 헬스체크 + **실패 시 자동 롤백** + 커밋 기록 |
| 웹 | `PRAFTA/prafta-web-frontend/prafta-web-frontend/scripts/deploy-web.ps1` | worktree 에서 npm ci+빌드 + `__APP_CONFIG__` **자동 주입+가드** + S3 sync + CloudFront 무효화 + 라이브 검증 + 커밋 기록 |
| 앱 프론트(원격) ★08-04 | `PRAFTA/prafta-app-frontend/prafta-app-frontend/scripts/deploy-app-web.ps1` | worktree 에서 npm ci+빌드 + `__APP_BUILD__` 주입 + `app-manifest.json` 생성(킬 스위치) + S3 sync(index·매니페스트=no-cache) + CloudFront 무효화 + 라이브 검증 + 커밋 기록. `-ManifestOnly`=매니페스트만 즉시 배포 |
| 앱 셸(APK) | `PRAFTA_FLUTTER/safenote/scripts/build-apk.ps1` (기존) | — |

스크립트 공통 옵션: `-Ref origin/develop` / `-Ref <커밋해시>`(핫픽스·롤백) / `-UseWorkingTree`(비상용, 로컬 작업 트리 그대로) / 웹 추가 `-SkipInvalidation`.

- 헬스체크 판정: 이 앱은 미매핑 경로에 500 을 반환(전역 예외 처리 특성, 07-19 실측)하므로 **500 도 생존 신호**. 다운 판정은 연결실패/502/503/504(nginx·CloudFront 대리응답)만.
- ✅ CloudFront 무효화 권한 = 해결됨(07-19) — `prafta-deploy` 인라인 정책 `prafta-cloudfront-invalidation`(CreateInvalidation+GetInvalidation, 웹 E37OL8Q9Q1FSLZ + ★08-04 앱 프론트 E20RXW16D6VDSN 한정). 그 외 CloudFront 액션은 여전히 불가(의도된 최소권한).
- **신규 마이그레이션 SQL 이 있으면 반드시 백엔드 배포 전에 선적용** (Flyway 도입 전까지 수동 — 요청서 `작업지시서_Flyway-마이그레이션-자동적용.md` 착수 대기).
- 완전 수동 절차(스크립트 불가 시)는 **부록 A** 참조.

---

## 4. 월 비용 현황 (시연 단계)

| 리소스 | 상태 | 월 비용(약) |
|---|---|---|
| 백엔드 EC2 (t3.small) | 상시 | ~$19 |
| RDS (db.t4g.micro) | 상시 | $0 (프리티어) |
| AI EC2 (m7i-flex.large) | **중지** | ~$4 (EBS 40GB만) |
| EBS(백엔드30GB)+EIP | 상시 | ~$7 |
| S3 + Cloudflare | 상시 | ~$1 (CF 무료플랜) |
| CloudFront + WAF | 상시 | ~$40 (WAF 필수규칙) |
| **합계** | | **~$71/월** (크레딧에서 차감) |

- 무료 플랜: $100 즉시 + 활동별 $20 = 최대 $200. **크레딧 소진 or 6개월 경과 시 계정 정지** → 유예기간 내 유료 전환 필요.
- 현 소진 속도 기준 크레딧 여유 ~2.5개월(≈9월 말). Budgets $100 알림(07-19 설정)이 전환 타이밍 신호.

---

## 5. 실고객 운영 전환 체크리스트 (고객 ~300명 기준)

### 🔴 필수 — 안 하면 사고
| # | 항목 | 상태/방법 | 왜 |
|---|---|---|---|
| 1 | **AI EC2 상시 가동** | 콘솔에서 start 후 유지 | 고객이 아무때나 AI 사용 |
| 2 | **LLM 호출 쿼터 가드** | ✅ **07-19 구현·배포 완료** — 회사별 월 토큰 한도(기본 80만, sysadmin Platform_03 관리) | 저권한 계정 반복호출 = HCX 요금폭탄 방어 |
| 3 | ~~Cloudflare SSL 평문~~ | ✅ 해소 — CloudFront↔EC2 HTTPS(§8). **07-19 오리진 http-only 오기까지 수정해 진짜 HTTPS 확정(§6-14)** | (완료) |
| 4 | **RDS 백업 보존 7일+** | ⚠️**무료 플랜 계정 제한으로 불가 확인(07-19)** — "backup retention period exceeds the maximum available to free tier customers". **#5 유료 플랜 전환 시 같이 설정할 것.** 그때까지 보완: 수동 스냅샷 수시 생성 | 데이터 유실 대비 |
| 5 | **유료 플랜 전환** | Billing 홈 → 프리 플랜 위젯 → Upgrade. 크레딧 소진 전(≈9월 말) or 실고객 계약 시 | 계정 정지 방지 + 무료 플랜 제한 해제 |

### 🟡 권장 — 안정성·비용
| # | 항목 | 상태/방법 |
|---|---|---|
| 6 | Savings Plan 1년 약정 | 사양 확정 후 EC2/RDS 약정 → ~35% 절감 |
| 7 | RDS/EC2 삭제 방지 켜기 | 각 리소스 설정에서 활성화 |
| 8 | RDS 마스터 비번 재설정 | 세션 노출 이력. 절차: 새 비번 파일 저장 → 서버 시크릿/.my.cnf 선교체 → 콘솔 변경 → 재기동(중단 ~1분) |
| 9 | HOLIDAY_API_SERVICE_KEY 실제 키 | 현재 더미(`dummy_replace_me`) — 공휴일 연동 쓰면 공공데이터포털 키 발급 |
| 10 | AWS Budgets 예산 알림 | ✅ 07-19 설정 완료(월 $100) |
| 11 | 업로드 파일 S3 이전 검토 | 현재 EC2 로컬 디스크 — EC2 재생성 시 유실 위험. EC2 IAM 역할 + S3 백업 설계 필요(별도 과제) |
| 12 | web.prafta.com 터널 레코드 정리 | ✅ 07-19 삭제 완료(Cloudflare API) |
| 13 | 직접가입 QR 링크 도메인 수정 | 웹 .env `VITE_PUBLIC_JOIN_BASE_URL` → `https://prafta.com` 바꿔 재빌드 (그 기능 쓸 때) |
| 14 | Play Store 정식 배포 | 진행 중(별도 세션 — DUNS·keystore·약관 등) |

### 🟢 그대로 둬도 됨 (300명 기준)
- **인스턴스 사양**: t3.small(백엔드)/db.t4g.micro(RDS)/m7i-flex.large(AI) 전부 충분. 느려지면 중지→유형변경→시작.
- **웹 S3+CloudFront**: 정적이라 트래픽 자동 확장.
- **아키텍처**: 구조 변경 불필요. 수천 명 규모로 가면 그때 RDS Multi-AZ / ALB 검토.

---

## 6. 배포 중 만난 함정 (재발 방지 기록)

1. **리전 확인**: 가입 직후 기본 리전이 시드니였음 → 전량 재생성. **콘솔 우상단 리전 먼저 확인**
2. **`.0`으로 끝나는 Elastic IP**: 일부 한국 ISP/공유기가 드롭 → SSH timeout. `.0`/`.255` EIP는 릴리스 후 재할당
3. **RDS 대소문자**: Windows MySQL 덤프 → RDS Linux 기본으로 로드하면 `TB_USER` 못 찾아 전체 500. **`lower_case_table_names=1` 파라미터 그룹을 RDS 생성 시점에 지정** (생성 후 변경 불가)
4. **RDS 함수/프로시저 로드**: `log_bin_trust_function_creators=1` 없으면 전부 실패. DEFINER 절도 덤프에서 제거 필요
5. **HOLIDAY_API_SERVICE_KEY**: 빈 값이면 HolidayApiClient 생성자 예외로 **부팅 자체가 실패** → 더미값이라도 필수
6. **nginx 기본 server 블록**: AL2023 nginx.conf 내장 기본 블록이 프록시 가로챔 → 제거 필요
7. **무료 플랜 인스턴스 제한**: t3.medium/large 선택 불가 → m7i-flex.large 등 flex 계열만 가능
8. **S3 버킷명**: 정적 호스팅 연결하려면 **버킷명=도메인명(prafta.com)** + 글로벌 네임스페이스 필수
9. **AWS CLI 설치**: 관리자 권한 없으면 MSI 실패 → `pip install --user awscli` + `python -m awscli`로 우회
10. **(★CloudFront) ACM 인증서는 반드시 us-east-1**: CloudFront 글로벌이라 서울 ACM 인증서는 드롭다운에 안 뜬다. WAF Web ACL도 "CloudFront(글로벌)" 스코프=us-east-1에서 생성
11. **(★CloudFront) CORS/AllViewer 함정**: API 배포 오리진 요청 정책이 `AllViewer`가 아니면 preflight 의 `Access-Control-Request-Headers`가 오리진에 전달 안 됨 → 브라우저 로그인 차단. **curl은 preflight 강제 안 해서 통과하므로 오진 주의**. 응답 헤더 정책은 "없음" 유지(백엔드가 CORS 직접 처리)
12. **(★CloudFront) S3 웹 오리진**: 웹 배포는 S3를 "Other/커스텀 오리진 + 웹사이트 엔드포인트(HTTP only)"로 연결. SPA 딥링크는 오류 페이지 404/403 → /index.html + 200. **API 배포엔 오류 페이지 절대 넣지 말 것**
13. **(★CloudFront) 보안그룹 프리픽스 리스트**: CIDR 규칙을 편집으로 프리픽스로 못 바꿈 → **규칙 삭제 후 새로 추가**. IPv4(`pl-22a6434b`) 선택. **SSH(22)는 절대 건드리지 말 것**(끊김)
14. **(★2026-07-19 API 장애 40분) 문서 오기 = CloudFront 오리진이 실제로는 http-only 였음**: §8 표에 "HTTPS only 443"으로 기록했지만 실제 prafta-api 배포 설정은 `http-only`(80 평문)이었다. 그래서 SG 80 인바운드를 삭제하자 API 전체 504. SG/NACL/nginx 전수 무죄 확인 후 `get-distribution` 으로 확정 → 오리진 **https-only + 최소 TLSv1.2** 수정(콘솔), 즉시 회복. **교훈: ①문서의 "완료" 서술을 믿지 말고 API 로 실설정을 검증할 것 ②잠금 전에 실제 트래픽이 어느 포트로 흐르는지 tcpdump/로그로 확인할 것.** 이제 CloudFront↔EC2 는 진짜 HTTPS 443
15. **(★07-19) 웹 크로스 오리진 게이트 함정**: `/platformApi` 게이트 인터셉터가 CORS 사전요청(OPTIONS, 인증헤더 없음)을 403 차단 → 운영에서만 조회 불가(로컬 동일오리진은 잠복). `CorsUtils.isPreFlightRequest` 통과 처리로 수정(a8e0c55). **직접 throw 하는 신규 게이트 인터셉터는 사전요청 예외 필수**
16. **(★07-19) AL2023 rpm certbot 에 pip 플러그인 설치 금지**: rpm certbot 은 `python3 -s`(=/usr/local 배제)로 떠서 pip 플러그인이 절대 안 보이고, pip 이 rpm distro 를 지워 **awscli·certbot 동반 파손**됨(실사고 — dnf reinstall python3-distro + /usr/local site-packages 전삭제로 복구). DNS-01 은 hook 방식이 정답
17. **(★07-19) 파일 유실**: 본 문서·배포 매뉴얼 .md 가 refs 폴더에서 삭제된 채 발견(휴지통에도 없음, 원인 미상 — 07-19 밤 세션 기록에서 전문 복원). `.claude/` 는 gitignore 라 git 에도 없다 — **중요 문서는 주기적으로 사본 검토** 필요

---

## 7. 핵심 파일/자격증명 위치

| 항목 | 위치 |
|---|---|
| SSH 키 | `C:\Users\dudjs\.ssh\prafta-key.pem` (양쪽 EC2 공용) |
| 로컬 시크릿 원본 | `C:\PRAFTA\secrets\platform-bootstrap.properties` |
| 서버 시크릿 | 백엔드EC2 `/home/ec2-user/prafta/secrets/platform-bootstrap.properties` |
| IAM 배포 키 CSV | `C:\Users\dudjs\Downloads\prafta-deploy_accessKeys.csv` (안전한 곳으로 이동 권장) |
| RDS 마스터 비번 | 사용자 보관 + 백엔드EC2 `~/.my.cnf` (문서 평문 기재 금지) |
| Cloudflare DNS 토큰 | 서버 `/etc/letsencrypt/cloudflare-token`(600) + 로컬 `C:\PRAFTA\secrets\cloudflare-dns-token.txt` (Zone:Read+DNS:Edit, prafta.com 한정) |
| AI DB 비번 | AI EC2 docker-compose 내(VPC 내부 전용) |
| HCX API 키 | 시크릿 파일 내 `CLOVA_STUDIO_API_KEY` |
| 배포 이력 | `.claude/refs/deploy-history.log` + 서버 `DEPLOYED_COMMIT_BACKEND` |
| 배포 스크린샷 | `C:\PRAFTA\.claude\refs\` (AWS_데이터베이스/, EC2_1/, EC2_AI/, 참고N.png) |

---

## 8. ★CloudFront + WAF 전환 상세 (2026-07-18 완료 → 07-19 보강)

### 배경
Cloudflare 무료플랜이 한국 트래픽을 미국 LAX 엣지로 라우팅(API 왕복 0.6~0.8초) → **앞단을 AWS CloudFront(서울 엣지)로 전환**해 API 왕복 **0.06~0.10초로 개선(약 10배)**.

### CloudFront 배포 2개 (콘솔은 us-east-1)
| 배포 | Distribution ID | 배포 도메인 | 대체도메인 | 오리진 |
|---|---|---|---|---|
| **prafta-web** | `E37OL8Q9Q1FSLZ` | d1vtp7jqcdi3j.cloudfront.net | prafta.com | (Other) S3 웹사이트 엔드포인트 **HTTP only** |
| **prafta-api** | `E1RO89G48H78J7` | d2iahzc1fk18ru.cloudfront.net | api.prafta.com | (Other) `origin.prafta.com` **★https-only 443 + 최소 TLSv1.2 (07-19 수정 — §6-14)** |

> ★2026-08-04 앱 프론트용 배포 `E20RXW16D6VDSN`(app.prafta.com)이 추가로 생겼다 — 구성 상세는 §2 "앱 프론트" 섹션 참조 (위 표는 07-18 전환 당시 2개 기준).

**웹 배포 동작**: 뷰어=Redirect HTTPS / 캐시=CachingOptimized / 기본루트=`index.html` / 오류페이지 404·403 → /index.html + 200(SPA).
**API 배포 동작 (★핵심)**: 허용메서드=GET~DELETE 전체 / 캐시=CachingDisabled / **오리진 요청 정책=`AllViewer`** / 응답헤더 정책=없음 / 오류페이지 없음 / **오리진 프로토콜=https-only**.

### 인증서
- CloudFront 뷰어용: ACM **us-east-1** 발급, `prafta.com`+`*.prafta.com` 와일드카드 1장. DNS 검증(Cloudflare CNAME 유지).
- EC2 nginx 오리진용: **Let's Encrypt**(origin.prafta.com). **★07-19 갱신 체계 전환 완료**: certbot-renew.timer 활성화(그전까지 disabled 상태였음 = 자동갱신 미가동!) + 갱신 방식 nginx(HTTP-01)→**manual DNS-01 + Cloudflare API hook** (`--preferred-challenges dns` 필수 — 빼면 HTTP-01 시도). reconfigure dry-run 통과로 실증. 80 포트 불필요해져 SG 에서 완전 폐쇄.

### AWS WAF
- Web ACL `prafta-waf` (스코프=CloudFront/글로벌, us-east-1). 두 배포 모두 연결.
- "필수 규칙" 세트(실차단 Block). Shield Standard = 자동·무료. 오탐 없음 확인.

### 보안그룹 최종 상태 (07-19)
- `prafta-backend-sg` 인바운드 = **SSH 22(내IP) + HTTPS 443(pl-22a6434b) 딱 2줄**. 80 없음. 직접 443/80 접근 = 타임아웃이 정상.

---

## 부록 A. 비상용 완전 수동 배포 절차 (스크립트 불가 시에만)

> ⚠️ 평소에는 사용 금지 — §3 스크립트가 백업·헬스체크·자동 롤백까지 포함해 아래 전 과정을 대신한다.
> 아래 수동 명령은 로컬 작업 트리 기준(git 무관)이고 안전장치가 없다.

### A-1. 백엔드 수동 배포
```powershell
# 1) 로컬 빌드 (JAVA_HOME=C:\Java\jdk-21.0.2)
$env:JAVA_HOME = "C:\Java\jdk-21.0.2"
cd C:\PRAFTA\PRAFTA\prafta-backend
.\gradlew.bat clean bootJar -x test --no-daemon

# 2) 업로드: 빌드된 JAR 를 서버의 라이브 경로로 복사
scp -i C:\Users\dudjs\.ssh\prafta-key.pem build/libs/prafta-backend-0.0.1-SNAPSHOT.jar ec2-user@3.38.237.103:/home/ec2-user/prafta/prafta-backend.jar

# 3) 재기동: 서버에 접속해 systemd 서비스 재시작(새 JAR 로 기동)
ssh -i C:\Users\dudjs\.ssh\prafta-key.pem ec2-user@3.38.237.103 "sudo systemctl restart prafta-backend"

# 4) 확인: https://api.prafta.com/prafta/ 가 500 응답이면 정상 기동(§0-5 판정 규칙)
```

### A-2. 웹 수동 배포
```powershell
cd C:\PRAFTA\PRAFTA\prafta-web-frontend\prafta-web-frontend
npm run build
# ★dist/index.html 의 </head> 앞에 런타임 설정 수동 주입 필수(빠뜨리면 API 주소 없는 웹이 올라감):
#   <script>window.__APP_CONFIG__ = { API_BASE: "https://api.prafta.com", CONTEXT: "/prafta" };</script>
python -m awscli s3 sync dist s3://prafta.com/ --delete --cache-control "public,max-age=31536000,immutable" --exclude "index.html"
python -m awscli s3 cp dist\index.html s3://prafta.com/index.html --cache-control "no-cache,no-store,must-revalidate" --content-type "text/html; charset=utf-8"
python -m awscli cloudfront create-invalidation --distribution-id E37OL8Q9Q1FSLZ --paths "/*"
```

### A-3. 수동 롤백 (서버에서)
```bash
mv -f ~/prafta/prafta-backend.jar.prev ~/prafta/prafta-backend.jar
sudo systemctl restart prafta-backend
```
