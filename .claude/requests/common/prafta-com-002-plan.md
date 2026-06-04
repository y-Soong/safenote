# prafta-com-002 작업 분해 (planner)

> 작업: FCM 공용 PUSH 전송 워커(consumer). `tb_noti_outbox` PENDING 행 → 실제 단말 FCM 발송 → SENT/FAILED 전이.
> 단일출처 설계: `.claude/requests/common/refs/prafta-com-002/01_작업지시서_FCM전송워커.md` (§0~§8).
> 확정 결정: `.claude/requests/common/prafta-com-002-decisions.md` (YJ 승인 완료, 그대로 반영).
> 워크플로우: planner → developer → qa → security. Notion 기록은 메인 세션 대행.

---

## 0. 정책서 출처 / 참조 근거

- 비즈니스 정책서: 공통 정책서 §10(알림/공지) — outbox 중복 발송 방지(§10.3 DEDUP_KEY), 알림 발송 채널.
  - 본 작업은 비즈니스 룰 신설이 아닌 **인프라(전송 계층)** 구현이라 도메인 정책 의존이 얕다. 발송 대상/내용은 생산자(prafta-com-001/031/040)가 이미 정책에 맞춰 outbox에 적재함. 워커는 채널 전달만.
- 기술/운영 정책: 작업지시서 §0~§8 + decisions B-1~B-3 (단일출처, 충돌 없음).
- 선행 작업: PRAFTA-COM-001(노무수령거부 outbox 생산자), prafta-031(outbox 인프라 tb_noti_outbox/SYS045). 둘 다 적용 완료. 생산자 무수정.

## 1. 스키마 확인 결과 (schema-full.sql + 기존 마이그/매퍼 교차확인)

### tb_noti_outbox (소비 대상, prafta-031-noti-outbox.sql 확인)
- 컬럼: `NOTI_ID`(PK,varchar20), `CMPNY_CD`, `SITE_CD`(nullable), `TARGET_USER_CD`, `NOTI_TYPE`[SYS045], `CHANNEL`(default 'PUSH'), `TITLE`(varchar200), `BODY`(varchar1000), `DATA_PAYLOAD`(json,nullable), `SEND_STATUS`(default 'PENDING'), `SENT_DATE`(datetime,nullable), `RETRY_CNT`(int NOT NULL default 0), `ERROR_MSG`(varchar500,nullable), `DEDUP_KEY`(nullable), `DEL_YN`(varchar1 default 'N'), `INSERT_NO/DATE`, `UPDATE_NO/DATE`.
- 인덱스: `IX_NOTI_OUTBOX_PENDING (CMPNY_CD, SEND_STATUS, INSERT_DATE)` — PENDING claim 조회에 그대로 활용.
- **SEND_STATUS 도메인**: DDL COMMENT 상 'PENDING/SENT/FAILED' 3종만. decisions B-3의 "claim 전이(PENDING→SENDING)"를 도입하려면 **'SENDING' 상태가 SEND_STATUS varchar(10)에 들어가도 컬럼/제약상 문제 없음**(CHECK 제약 없음, varchar10). 단 코드값 카탈로그(주석)와 정합 위해 마이그 COMMENT 갱신 또는 SENDING 미사용 결정 필요 → §6 결정 참조.
- **워커 갱신 컬럼**: SEND_STATUS, SENT_DATE, RETRY_CNT, ERROR_MSG, UPDATE_NO/DATE. **신규 컬럼 불필요.**

### tb_user_device (토큰 소스, schema-full.sql L1064 확인)
- 컬럼: `DEVICE_UUID`(PK,varchar100), `USER_CD`(varchar20), `DEVICE_TYPE`('IOS'/'ANDROID'), `DEVICE_MODEL`, `OS_VERSION`, `APP_VERSION`, `PUSH_TOKEN`(varchar500,nullable), `LAST_LOGIN_DTIME`, `LAST_LOGIN_IP`, `INSERT_NO`(default 'SYSTEM'), `INSERT_DATE`, `UPDATE_NO`, `UPDATE_DATE`.
- **CMPNY_CD 컬럼 없음** (글로벌 유니크 디바이스 테이블 — 회사 가로질러 1디바이스=1계정). 토큰 조회는 `USER_CD`로만. outbox는 회사 스코프지만, 디바이스 조회 키는 `TARGET_USER_CD`(=USER_CD) 단일.
- **DEL_YN 컬럼 없음** → B-2 옵션A(soft-delete) 적용 위해 **신규 컬럼 `DEL_YN char(1) NOT NULL DEFAULT 'N'` 추가 마이그 필수**. 추가 전엔 토큰 조회/무효 마킹 SQL이 "Unknown column"으로 실패(전면 차단). **마이그 선적용 필수.**
- KEY `idx_user_device_user (USER_CD)` 존재 → USER_CD 조회 인덱스 OK.

### 기존 패턴 (미러 대상)
- 스케줄러 게이트: `com.prafta.common.schedule.leave.scheduler.LeaveGrantScheduler` — `@Component` + `@Value("${...enabled:false}")` 게이트 + `@Scheduled` + try/catch log.error 삼킴. `@EnableScheduling`은 `com.prafta.MainApplication`에 이미 존재.
- outbox INSERT 컬럼/채번: `LeaveDashboardMapper.insertNotiOutbox`(NotiOutboxInsertVO). 워커는 INSERT 안 함(소비만).
- 마이그 위치: `prafta-backend/src/main/resources/sql/migration/`. 네이밍 `prafta-com-002-*.sql`. 운영 미적용·수동(MCP read-only).
- build.gradle: Spring Boot 3.5.13 / Java 21. Firebase Admin SDK 미존재 → 신규 추가.

## 2. 작업 분해

총 5개 단위. 우선순위는 마이그/설정 → 매퍼/VO → 전송클라이언트 → 서비스 → 스케줄러 → 테스트 순(의존 역순). PRAFTA-COM-002-1~5로 채번.

---

### PRAFTA-COM-002-1 : DB 마이그레이션 — tb_user_device.DEL_YN 추가 (+롤백)
- **유형**: backend (DB 마이그)
- **영역**: common (백엔드 인프라)
- **모듈**: common/cmm/push (소비), tb_user_device(스키마)
- **작업 유형**: 신규
- **요구사항 요약**: 무효 토큰 soft-delete(B-2 옵션A)를 위해 `tb_user_device`에 `DEL_YN char(1) NOT NULL DEFAULT 'N'` 컬럼 추가. 운영 미적용(수동).
- **상세 설명** [backend]:
  - 핵심 요구사항:
    1) `prafta-com-002-user-device-del-yn.sql` 작성: `ALTER TABLE tb_user_device ADD COLUMN DEL_YN char(1) NOT NULL DEFAULT 'N' COMMENT '삭제 여부(무효 토큰 soft-delete)' AFTER UPDATE_DATE;` (또는 PUSH_TOKEN 근처 — 기존 관례상 감사컬럼 뒤 권장).
    2) 롤백 SQL을 동일 파일 하단에 주석으로 명시(`ALTER TABLE tb_user_device DROP COLUMN DEL_YN;`).
    3) 헤더 주석: 적용 전 부재 확인(`SHOW COLUMNS FROM tb_user_device LIKE 'DEL_YN';`), 멱등성·운영 수동적용·코드 선후행성 경고(prafta-app-008 마이그 스타일 미러).
    4) 정책 컨벤션: DEL_YN char(1) 'N' 기본(프로젝트 표준). 기존 tb_user_device 행 전부 'N' 백필됨(NOT NULL DEFAULT).
  - 영향 파일: (신규) `prafta-backend/src/main/resources/sql/migration/prafta-com-002-user-device-del-yn.sql`
  - 영향 endpoint: 없음
  - 예상 산출물: 마이그 SQL 1종(+롤백 주석)
  - **선후행성(중요)**: §4 매퍼의 토큰 조회/무효 마킹 SQL이 DEL_YN을 참조 → **본 마이그가 운영 DB 선적용되지 않으면 워커 토큰 조회 전면 실패**. 게이트 default false라 운영 사고는 안 나지만, 워커 ON 전 반드시 적용. 보고서에 명시.
- **선행 작업**: 없음
- **우선순위 근거**: 코드(§4)가 DEL_YN 의존. 가장 먼저 확정해야 매퍼 SQL 컬럼 확정.

---

### PRAFTA-COM-002-2 : 의존성/설정 — Firebase Admin SDK + properties + .env.example
- **유형**: backend (빌드/설정)
- **영역**: common
- **모듈**: common/cmm/push
- **작업 유형**: 신규
- **요구사항 요약**: build.gradle에 Firebase Admin SDK 추가, application.properties에 키경로/게이트/주기 placeholder, .env.example에 키 이름만 추가. 부팅 안전(키 부재/게이트 off 시 skip).
- **상세 설명** [backend]:
  - 핵심 요구사항:
    1) build.gradle: `implementation 'com.google.firebase:firebase-admin:9.x'`(developer가 최신 안정버전 확정, 컴파일 성공 확인). transitive로 grpc/guava 대량 유입 가능성 인지(빌드시간↑).
    2) application.properties placeholder 추가:
       - `push.fcm.credentials-path=${FIREBASE_CREDENTIALS_PATH:}` (decisions: 파일경로 주입 확정)
       - `prafta.push.worker.enabled=${PUSH_WORKER_ENABLED:false}` (B-1 게이트 기본 false)
       - `prafta.push.worker.interval-ms=${PUSH_WORKER_INTERVAL_MS:30000}` (B-1 fixedDelay 30초)
       - `prafta.push.worker.batch-size=${PUSH_WORKER_BATCH_SIZE:50}` (1주기 claim 건수, 기본값)
       - `prafta.push.worker.max-retry=${PUSH_WORKER_MAX_RETRY:3}` (B-2 재시도 3회)
    3) .env.example: 선택 섹션에 `# FIREBASE_CREDENTIALS_PATH=/etc/prafta/firebase-sa.json`, `# PUSH_WORKER_ENABLED=false` 등 **키 이름만, 실값 없이** 주석으로 추가. credentials JSON 원문/경로 실값 커밋 금지.
    4) FCM 초기화는 **lazy/조건부**: 게이트 off이거나 credentials-path 비어있거나 파일부재면 FirebaseApp 초기화 시도하지 않고 워커 skip. 초기화 실패가 앱 부팅을 막지 않게(부팅 영향 0). 초기화 위치는 FcmClient(§4-3) 내부 또는 @Configuration의 @ConditionalOnProperty.
  - 영향 파일:
    - `prafta-backend/build.gradle`
    - `prafta-backend/src/main/resources/application.properties`
    - `prafta-backend/.env.example`
  - 영향 endpoint: 없음
  - 예상 산출물: gradle 의존성 1종, properties 5키, .env.example 주석
- **선행 작업**: 없음
- **우선순위 근거**: 의존성/설정이 §3·§4 코드 컴파일·초기화 전제.

---

### PRAFTA-COM-002-3 : PushOutboxMapper(+xml) + VO — outbox claim/조회/상태전이/토큰 SQL
- **유형**: backend (Mapper/VO)
- **영역**: common
- **모듈**: common/cmm/push
- **작업 유형**: 신규
- **요구사항 요약**: PENDING claim, 토큰 조회(DEL_YN='N'), markSent/markFailed/incrementRetry, 무효토큰 soft-delete SQL. 결과 운반 VO.
- **상세 설명** [backend]:
  - 핵심 요구사항:
    1) `PushOutboxMapper`(인터페이스) + `PushOutboxMapper.xml`(namespace `com.prafta.common.cmm.push.mapper.PushOutboxMapper`). xml 위치는 `src/main/resources/com/prafta/common/cmm/push/mapper/`(기존 mapper-locations `classpath*:mapper/**/*.xml` 글롭 일치 확인 — developer가 경로 정합 확인).
    2) `selectPendingForSend`: `SEND_STATUS='PENDING' AND DEL_YN='N' AND RETRY_CNT < #{maxRetry}` ORDER BY INSERT_DATE ASC LIMIT #{batchSize}. 필요 컬럼만(NOTI_ID, CMPNY_CD, TARGET_USER_CD, NOTI_TYPE, TITLE, BODY, DATA_PAYLOAD, RETRY_CNT). SELECT * 금지, IX_NOTI_OUTBOX_PENDING 활용.
    3) `claimSending`(B-3): 조건부 UPDATE `SET SEND_STATUS='SENDING', UPDATE_NO=#{worker}, UPDATE_DATE=NOW() WHERE NOTI_ID=#{notiId} AND SEND_STATUS='PENDING'`. affected row=1일 때만 처리(크래시복구/중복방지). **SENDING 상태 도입 여부는 §6 결정 — 단일 인스턴스라 미도입(PENDING 직접 처리) 가능하나, 작업지시서·decisions가 claim 전이를 명시했으므로 도입.**
    4) `selectDeviceTokens`: `SELECT PUSH_TOKEN FROM tb_user_device WHERE USER_CD=#{targetUserCd} AND DEL_YN='N' AND PUSH_TOKEN IS NOT NULL` (DEVICE_UUID도 함께 반환하면 무효토큰 마킹에 사용). 다중 디바이스 가능.
    5) `markSent`: `SET SEND_STATUS='SENT', SENT_DATE=NOW(), UPDATE_*=...`. 멱등: WHERE에 `SEND_STATUS IN ('SENDING','PENDING')`로 SENT 재처리 방지.
    6) `markFailed`: `SET SEND_STATUS='FAILED', RETRY_CNT=#{retryCnt}, ERROR_MSG=#{errorMsg}, UPDATE_*=...`. ERROR_MSG는 500자 truncate(컬럼 한계) — developer가 substring 가드.
    7) `incrementRetryAndRevertPending`(일시 실패): `SET SEND_STATUS='PENDING', RETRY_CNT=RETRY_CNT+1, ERROR_MSG=#{errorMsg}, UPDATE_*=...` (다음 주기 재시도). claim 후 PENDING으로 되돌려야 다음 주기 재집힘.
    8) `softDeleteDeviceToken`(무효 토큰): `UPDATE tb_user_device SET DEL_YN='Y', UPDATE_NO=#{worker}, UPDATE_DATE=NOW() WHERE DEVICE_UUID=#{deviceUuid}` (B-2 옵션A). PUSH_TOKEN 자체는 보존(감사), 조회에서 DEL_YN으로 제외.
    9) 토큰 0건 → 서비스에서 `markFailed(ERROR_MSG="NO_DEVICE_TOKEN")` (decisions 확정). RETRY 누적 없이 즉시 FAILED.
    10) 모든 SQL `/* Mapper.method */` 주석, leading comma, `#{}` 바인딩, SELECT * 금지(DoD §6).
  - VO/결과 타입: `PushOutboxRowVO`(claim 대상 행), `DeviceTokenVO`(deviceUuid + pushToken). DATA_PAYLOAD는 json → String으로 받아 FcmClient에서 Map 파싱(또는 Map<String,String> typeHandler — developer 판단, 단순 String 후 Jackson 파싱 권장).
  - 영향 파일:
    - (신규) `.../java/com/prafta/common/cmm/push/mapper/PushOutboxMapper.java`
    - (신규) `.../resources/com/prafta/common/cmm/push/mapper/PushOutboxMapper.xml`
    - (신규) `.../java/com/prafta/common/cmm/push/vo/PushOutboxRowVO.java`, `DeviceTokenVO.java`
  - 영향 endpoint: 없음
  - 예상 산출물: mapper(java+xml), VO 2종
  - **선후행성**: DEL_YN 마이그(002-1) 의존.
- **선행 작업**: PRAFTA-COM-002-1 (DEL_YN 컬럼)
- **우선순위 근거**: 서비스(§4-2)가 매퍼 의존.

---

### PRAFTA-COM-002-4 : FcmClient 래퍼 + PushSenderService(+impl) — 전송/결과분기/상태전이
- **유형**: backend (Service)
- **영역**: common
- **모듈**: common/cmm/push
- **작업 유형**: 신규
- **요구사항 요약**: Firebase Admin SDK 호출 격리 래퍼(FcmClient) + dispatchPending() 핵심 로직(claim→토큰조회→전송→상태전이). 멱등·재시도·무효토큰·NO_DEVICE_TOKEN.
- **상세 설명** [backend]:
  - 핵심 요구사항:
    1) `FcmClient`(인터페이스+impl): `FcmSendResult send(String token, String title, String body, Map<String,String> data)`. 결과를 **SUCCESS / INVALID_TOKEN(UNREGISTERED·INVALID_ARGUMENT) / TRANSIENT_FAILURE(네트워크/5xx/기타)** 3분기로 구분 반환(작업지시서 §4-4). Firebase `FirebaseMessagingException`의 `MessagingErrorCode`로 분기. SDK 호출은 이 클래스에만 격리(테스트 mock 용이, §6 DoD).
       - 초기화: credentials-path 파일로 `GoogleCredentials.fromStream` → `FirebaseApp.initializeApp`(앱당 1회, 이미 초기화 시 재사용). 키 부재/게이트 off면 미초기화 → 워커 skip.
    2) `PushSenderService.dispatchPending()`(impl):
       - `@Transactional` 경계는 **행 단위**로 짧게(claim/상태전이 각각). 전체 배치를 한 트랜잭션으로 묶지 말 것(부분 성공 보존 + 락 최소화). developer가 트랜잭션 경계 설계(REQUIRES_NEW 또는 매퍼 단건 커밋).
       - 흐름: `selectPendingForSend(batchSize, maxRetry)` → 행마다 `claimSending`(affected=1만 처리) → `selectDeviceTokens(targetUserCd)`:
         - 0건 → `markFailed("NO_DEVICE_TOKEN")`, continue.
         - 디바이스별 `fcmClient.send(token, title, body, dataMap)`:
           - SUCCESS → 해당 행 성공 플래그.
           - INVALID_TOKEN → `softDeleteDeviceToken(deviceUuid)`, 그 디바이스 제외.
           - TRANSIENT_FAILURE → 일시 실패 누적(에러 메시지 보존).
       - 행 결과 종합(작업지시서 §4-3): **하나라도 SUCCESS → markSent**. 전부 INVALID_TOKEN → markFailed("ALL_TOKENS_INVALID" 등). SUCCESS 0 + TRANSIENT ≥1 → `incrementRetryAndRevertPending`; RETRY_CNT+1 >= maxRetry면 markFailed(마지막 에러). 전부 INVALID + 일부 TRANSIENT 혼재 시 우선순위는 developer가 명세(권장: SUCCESS>INVALID 영구실패>TRANSIENT 재시도; INVALID는 재시도 무의미하므로 TRANSIENT만 재시도 트리거).
    3) DATA_PAYLOAD(json String) → Map<String,String> 파싱(Jackson). null/빈값이면 data 없이 notification만 전송. 파싱 실패는 TRANSIENT 아님 → markFailed("INVALID_PAYLOAD") 권장(developer 판단, 재시도해도 동일 실패).
    4) PII/보안(§5): 로그에 PUSH_TOKEN/credentials/PII 평문 금지. 토큰 로그 시 마스킹(앞4+****). TITLE/BODY/DATA_PAYLOAD는 코드값만(생산자가 PII 미합성) — 그대로 전송, DB 평문 PII 저장 금지(현행 유지).
    5) 워커 주체: 상태전이 UPDATE의 UPDATE_NO/INSERT_NO = `'PUSH_WORKER'`(또는 'SYSTEM'). decisions/§5는 둘 다 허용 → `'PUSH_WORKER'` 채택(주체 식별 명확). 상수로.
    6) 멱등: claim(affected=1) + markSent WHERE 상태가드로 중복 발송 차단(§5).
  - 영향 파일:
    - (신규) `.../java/com/prafta/common/cmm/push/FcmClient.java`, `.../push/impl/FcmClientImpl.java`
    - (신규) `.../java/com/prafta/common/cmm/push/FcmSendResult.java`(enum 또는 record)
    - (신규) `.../java/com/prafta/common/cmm/push/PushSenderService.java`, `.../push/impl/PushSenderServiceImpl.java`
    - (신규) `.../java/com/prafta/common/cmm/push/PushWorkerConst.java`(주체='PUSH_WORKER', ERROR_MSG 상수)
  - 영향 endpoint: 없음
  - 예상 산출물: service(인터페이스+impl), fcm client(인터페이스+impl), result type, const
  - **선후행성**: 매퍼(002-3) + 설정(002-2) 의존.
- **선행 작업**: PRAFTA-COM-002-2, PRAFTA-COM-002-3
- **우선순위 근거**: 핵심 전송 로직. 스케줄러가 이를 호출.

---

### PRAFTA-COM-002-5 : PushSendScheduler + 단위테스트
- **유형**: backend (Scheduler + Test)
- **영역**: common
- **모듈**: common/schedule/push, common/cmm/push (test)
- **작업 유형**: 신규
- **요구사항 요약**: 게이트 검사 후 dispatchPending() 호출하는 @Scheduled 워커 + FcmClient mock 기반 상태전이/재시도/무효토큰/토큰0건 단위테스트.
- **상세 설명** [backend]:
  - 핵심 요구사항:
    1) `PushSendScheduler`(@Component): `@Value("${prafta.push.worker.enabled:false}")` 게이트. `@Scheduled(fixedDelayString="${prafta.push.worker.interval-ms:30000}")` (B-1 fixedDelay 30초, fixedDelay라 비중첩 → 동시성 위험 낮음 B-3). 게이트 off면 즉시 return(log.debug). 활성 시 `pushSenderService.dispatchPending()` 호출, 예외는 log.error 후 삼킴(다음 주기 재시도). `LeaveGrantScheduler` 패턴 미러(단 fixedDelay; cron 아님).
    2) 위치: `com.prafta.common.schedule.push.PushSendScheduler`. `@EnableScheduling`은 MainApplication 기존 보유(확인됨) — 추가 불필요.
    3) 단위테스트(§6 DoD, FcmClient mock으로 실제 FCM 미호출):
       - T1 성공: 토큰 1건 SUCCESS → markSent 호출(SENT_DATE 세팅) 검증.
       - T2 일시실패 재시도 누적: TRANSIENT_FAILURE → incrementRetryAndRevertPending(RETRY_CNT+1, PENDING 유지) 검증. RETRY_CNT+1 == maxRetry(3) 도달 시 markFailed 전이 검증.
       - T3 무효토큰 soft-delete: INVALID_TOKEN → softDeleteDeviceToken(deviceUuid) 호출 검증 + 그 디바이스 제외, 다른 SUCCESS 디바이스 있으면 markSent.
       - T4 토큰 0건: selectDeviceTokens 빈 리스트 → markFailed(ERROR_MSG="NO_DEVICE_TOKEN") 검증.
       - T5 멱등/claim: claimSending affected=0(이미 다른 주기/인스턴스가 가져감)이면 처리 skip 검증.
       - T6 게이트 off: enabled=false면 dispatchPending 미호출 검증(scheduler 단위).
       - mock 대상: FcmClient(전송결과 주입), PushOutboxMapper(@Mock, Mockito). 실제 DB·FCM 미접속. Mockito 기반(기존 LeaveRefusalDetectServiceImplTest 스타일 미러 — developer가 기존 테스트 패턴 정독).
    4) 테스트는 한글 인코딩 함정 주의(메모리 feedback_korean_encoding): 테스트 메시지/주석 한글 가능하나 식별자는 영어.
  - 영향 파일:
    - (신규) `.../java/com/prafta/common/schedule/push/PushSendScheduler.java`
    - (신규) `.../test/java/com/prafta/common/cmm/push/PushSenderServiceImplTest.java`
    - (선택) `.../test/java/com/prafta/common/schedule/push/PushSendSchedulerTest.java`
  - 영향 endpoint: 없음
  - 예상 산출물: scheduler 1종, 테스트 1~2종
- **선행 작업**: PRAFTA-COM-002-4
- **우선순위 근거**: 진입점 + 검증. 마지막.

---

## 3. 의존성 그래프

```
002-1 (DEL_YN 마이그) ─┐
                       ├─→ 002-3 (Mapper/VO) ─┐
002-2 (의존성/설정) ───┴──────────────────────┴─→ 002-4 (FcmClient + Service) ─→ 002-5 (Scheduler + Test)
```
- 002-1, 002-2는 병렬 가능(상호 무관).
- developer 착수 순서 권장: 002-1 → 002-2 → 002-3 → 002-4 → 002-5.

## 4. 비범위 (작업지시서 §0 / decisions 준수)

- iOS APNs 전용 설정, 앱 측 토큰 등록 흐름(Flutter/앱), 알림센터/읽음 UI, opt-out, 통계 대시보드.
- outbox 생산자(INSERT) 무수정 — prafta-com-001/031/040 손대지 않음.
- tb_noti_outbox 신규 컬럼 없음(기존 컬럼만 활용).
- 멀티캐스트(sendMulticast) 최적화는 비범위 — 사용자 다중 디바이스는 디바이스별 개별 send 루프로 충분(발송량 소규모 전제, B-4).

## 5. 확정 결정 반영 체크

| 결정 | 반영 위치 |
|---|---|
| env=FIREBASE_CREDENTIALS_PATH(파일경로) | 002-2 properties `push.fcm.credentials-path` |
| B-1 fixedDelay 30초 | 002-2 `interval-ms:30000` / 002-5 `@Scheduled(fixedDelayString=...)` |
| B-1 게이트 기본 false | 002-2 `prafta.push.worker.enabled:false` / 002-5 게이트 |
| B-2 재시도 3회 후 FAILED | 002-2 `max-retry:3` / 002-4 결과분기 / 002-5 T2 |
| B-2 무효토큰 soft-delete(옵션A) | 002-1 DEL_YN 마이그 / 002-3 softDeleteDeviceToken / 002-4 INVALID_TOKEN 분기 |
| 토큰조회 DEL_YN='N' AND PUSH_TOKEN IS NOT NULL | 002-3 selectDeviceTokens |
| B-3 단일 인스턴스 + claim 전이 | 002-3 claimSending / 002-4 affected=1 처리 |
| 토큰 0건 → FAILED + "NO_DEVICE_TOKEN" | 002-3·002-4 / 002-5 T4 |
| 워커 주체 SYSTEM/PUSH_WORKER | 002-4 PushWorkerConst='PUSH_WORKER' |
| 마이그 운영 미적용·수동 | 002-1 헤더 주석 |

## 6. 채팅 확인 필요(낮은 비중 — developer 기본값으로 진행 가능, 보고만)

1) **SENDING 상태 도입 여부**: decisions B-3·작업지시서 §4가 "PENDING→SENDING claim 전이"를 명시했으나, tb_noti_outbox.SEND_STATUS COMMENT 카탈로그는 'PENDING/SENT/FAILED'만 등재. 단일 인스턴스 전제라 claim 없이 PENDING 직접 처리도 가능. **기본 진행안**: 작업지시서대로 'SENDING' 임시상태 도입(크래시복구/멱등 명확) + 002-1 마이그에 SEND_STATUS COMMENT 갱신 ALTER 1줄 동봉(코드값 카탈로그 정합). 도입하지 않을 경우 단순화되나 크래시 중복발송 리스크 미세 증가. → developer가 SENDING 도입으로 진행, 미동의 시 사용자 회신.
2) **혼재 결과 우선순위**(다중 디바이스에서 INVALID+TRANSIENT 혼재 시): 기본 진행안 = SUCCESS 우선(있으면 SENT), 없으면 TRANSIENT 있으면 재시도, TRANSIENT 없고 전부 INVALID면 FAILED. (002-4에 명세) — 합리적 기본값이라 자율 진행.

위 2건은 모호함이 아니라 구현 선택지로, 작업지시서/decisions 범위 내 합리적 기본값이 존재하므로 분해를 막지 않음. developer 착수 가능.

## 7. 메인 세션 Notion 반영 항목 (서브에이전트 Notion 미접근)

"작업 로그" DB에 5행 등록(상태=분해완료, 담당=planner):

| 작업ID | 영역 | 모듈 | 작업유형 | 요구사항 요약 | 선행 |
|---|---|---|---|---|---|
| PRAFTA-COM-002-1 | common | tb_user_device | 신규 | DEL_YN 컬럼 추가 마이그(soft-delete) | 없음 |
| PRAFTA-COM-002-2 | common | common/cmm/push | 신규 | Firebase Admin SDK + properties + .env.example | 없음 |
| PRAFTA-COM-002-3 | common | common/cmm/push | 신규 | PushOutboxMapper(+xml) claim/조회/상태전이/토큰 SQL + VO | 002-1 |
| PRAFTA-COM-002-4 | common | common/cmm/push | 신규 | FcmClient + PushSenderService 전송/결과분기/상태전이 | 002-2,002-3 |
| PRAFTA-COM-002-5 | common | common/schedule/push | 신규 | PushSendScheduler(게이트) + FcmClient mock 단위테스트 | 002-4 |

- 상세 설명 컬럼: 각 행에 [backend] 태그 + 본 plan §2 핵심 요구사항/영향파일/산출물 요약 + 정책출처(공통 정책서 §10, 작업지시서 §0~§8, decisions B-1~B-3).
- 산출물 컬럼: 백엔드 작업이므로 비움(developer가 채움). frontend 작업 없음 → UI 명세/Vue 골격 없음.
- 화면 작업 없음 → "도메인 지식 베이스" 등록 불필요.
