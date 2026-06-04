# [작업지시서] FCM 공용 PUSH 전송 워커 (Claude Code / developer)

> 대상: Claude Code (다음 세션, planner→developer→qa→security 워크플로우)
> 선행조건: `00_선행조치_사용자작업.md` 의 **A-1(FCM 프로젝트/서비스계정 키), A-2(키 주입 env)** 완료.
>  - A-1/A-2 미완 시 **착수 금지**(전송 인증 불가). A-3(앱 토큰 등록)은 미완이어도 워커 구현은 가능하되 실제 도달 테스트만 보류.

---

## 0. 목표 (이 범위만, 초과 금지)

`tb_noti_outbox` 의 `SEND_STATUS='PENDING'` 행을 주기적으로 집어 **FCM 으로 실제 전송**하고,
결과를 `SENT`/`FAILED` 로 갱신하는 **NOTI_TYPE 무관 공용 워커**를 구현한다.

### 명시적 비범위 (만들지 말 것)
- iOS APNs 전용 설정, 앱 측 토큰 등록 흐름(앱 프로젝트), 알림센터/읽음 UI, opt-out 설정, 통계 대시보드.
- outbox **생산자**(INSERT)는 손대지 않는다 — 이미 각 기능(회수/아차사고/노무수령거부)이 INSERT 중. 이 작업은 **소비자(consumer)**만.

---

## 1. 전제 스키마 (확인됨 — 신규 컬럼/테이블 불필요)

### tb_noti_outbox (소비 대상)
- `NOTI_ID`(PK), `CMPNY_CD`, `SITE_CD`(nullable), `TARGET_USER_CD`, `NOTI_TYPE`[SYS045], `CHANNEL`('PUSH'),
  `TITLE`, `BODY`, `DATA_PAYLOAD`(json, nullable), `SEND_STATUS`('PENDING'/'SENT'/'FAILED'),
  `SENT_DATE`(datetime, nullable), `RETRY_CNT`(int, NOT NULL), `ERROR_MSG`(varchar500, nullable),
  `DEDUP_KEY`, `DEL_YN`, `INSERT_*`, `UPDATE_*`.
- → 워커가 갱신할 컬럼: `SEND_STATUS`, `SENT_DATE`, `RETRY_CNT`, `ERROR_MSG`, `UPDATE_*`. **신규 컬럼 추가 불필요.**

### tb_user_device (토큰 소스)
- `DEVICE_UUID`(PK), `USER_CD`, `DEVICE_TYPE`('IOS'/'ANDROID'), `PUSH_TOKEN`(varchar500, nullable), `LAST_LOGIN_DTIME`, ...
- → `TARGET_USER_CD` 로 조회, `PUSH_TOKEN IS NOT NULL` 인 디바이스(들)에 발송. 한 사용자 다중 디바이스 가능.

> 착수 시 MCP(prafta-mysql)로 위 컬럼 재확인. 무효 토큰 정리 정책에 따라 디바이스 비활성 컬럼이 필요하면 보고(현재 스키마에 soft-delete 컬럼 유무 확인).

---

## 2. 설정 / 의존성 (🤖)

- **Firebase Admin SDK(Java)** 의존성 추가: `build.gradle` 에 `com.google.firebase:firebase-admin:<버전>`.
- 초기화: 서비스 계정 키를 **환경변수로 주입**(00 A-2에서 YJ가 정한 이름. 미정이면 기본 `FIREBASE_CREDENTIALS_PATH`(파일경로) 우선, 없으면 `FIREBASE_CREDENTIALS_JSON`(원문)).
  - `application.properties` 에 `push.fcm.credentials-path=${FIREBASE_CREDENTIALS_PATH:}` 등 placeholder 추가.
  - **키 파일/원문은 절대 커밋·로그 출력 금지.** `.env.example` 에 키 이름만 추가(실값 없이).
- 워커 게이트: `prafta.push.worker.enabled=${PUSH_WORKER_ENABLED:false}` (기본 비활성, 기존 `LeaveGrantScheduler` 게이트 패턴 미러).
- 주기: `@Scheduled(fixedDelayString="${prafta.push.worker.interval-ms:60000}")` 또는 cron(00 B-1 결정 반영. 기본 60초).
- FCM 초기화 실패(키 부재/무효)는 앱 부팅을 막지 않게 — 게이트 off거나 키 없으면 워커가 조용히 skip(부팅 영향 0).

---

## 3. 컴포넌트 구조 (프로젝트 컨벤션)

위치: `com.prafta.common.schedule.push.*` (스케줄러) + `com.prafta.common.cmm.push.*` (전송 서비스/매퍼). 기존 `common.schedule.leave` 패턴 미러.

1. `PushSendScheduler`(@Component, @Scheduled) — 게이트 검사 후 `PushSenderService.dispatchPending()` 호출. 예외는 log.error 후 삼킴(다음 주기 재시도).
2. `PushSenderService`(+impl) — 핵심 로직:
   - PENDING 행을 배치 크기만큼 claim(아래 §4 동시성).
   - 행마다: `TARGET_USER_CD` → 디바이스 토큰 조회 → FCM 전송 → 결과로 상태 갱신.
3. `FcmClient`(래퍼) — Firebase Admin SDK 호출 격리(테스트 mock 용이). `send(token, title, body, dataMap)` → 성공/무효토큰/일시실패 구분 결과 반환.
4. `PushOutboxMapper`(+xml) — `selectPendingForSend`, `claimSending`(상태 전이), `markSent`, `markFailed`, `selectDeviceTokens`, (무효토큰) `clearDeviceToken`.

---

## 4. 처리 로직 (DoD 직결)

1. **조회/클레임(동시성)**: `SEND_STATUS='PENDING' AND DEL_YN='N' AND RETRY_CNT < {maxRetry}` 를 `INSERT_DATE` 오름차순 N건.
   - 단일 인스턴스 전제면 단순 SELECT 후 처리(00 B-3).
   - 다중 인스턴스 가능성 있으면 `SELECT ... FOR UPDATE SKIP LOCKED` 또는 "claim UPDATE"(SEND_STATUS PENDING→SENDING 조건부 UPDATE 후 affected row만 처리)로 **중복 발송 방지**. (B-3 결정 반영. 미정이면 claim UPDATE 방식 채택.)
2. **토큰 조회**: `tb_user_device WHERE USER_CD=#{targetUserCd} AND PUSH_TOKEN IS NOT NULL`. 0건이면 → 전송 대상 없음: `FAILED`(ERROR_MSG="NO_DEVICE_TOKEN") 또는 정책상 보류. (00 A-3 미완 시 흔함 — 명확히 ERROR_MSG 남길 것.)
3. **전송**: 디바이스별 FCM 호출. `TITLE`/`BODY` → notification, `DATA_PAYLOAD`(json) → data. 
   - 다중 디바이스: 하나라도 성공이면 행 `SENT`. 전부 무효/실패면 아래 분기.
4. **결과 갱신(멱등)**:
   - 성공 → `SEND_STATUS='SENT', SENT_DATE=NOW()`.
   - 일시 실패(네트워크/5xx) → `RETRY_CNT+1, ERROR_MSG`. PENDING 유지(다음 주기 재시도). `RETRY_CNT >= maxRetry` 면 `FAILED`.
   - 무효 토큰(FCM UNREGISTERED/INVALID_ARGUMENT) → 해당 `PUSH_TOKEN` 정리(00 B-2 정책: NULL/soft-delete). 그 디바이스는 대상 제외.
   - 이미 `SENT` 인 행은 재처리 skip(claim으로 자연 방지).
5. **PII**: outbox `TITLE/BODY/DATA_PAYLOAD` 에는 코드값만 들어있음(이름 없음). 근로자명 등 PII가 필요한 알림은 **전송 페이로드에서만** 합성하고 **DB/로그에는 평문 저장·출력 금지**. (현재 노무수령거부는 이름 미합성 → 그대로 전송.)

---

## 5. 보안 / 운영 (🤖 준수, security 검토 대상)

- 서비스 계정 키: 커밋 금지, 로그에 키/토큰/PII 평문 출력 금지(`PUSH_TOKEN` 도 로그 시 마스킹).
- 워커는 시스템 주체(INSERT_NO/UPDATE_NO = 'SYSTEM' 또는 'PUSH_WORKER'). 사용자 JWT 무관(스케줄러).
- 멱등: claim 기반 + SENT 재처리 방지로 **중복 발송 차단**(알림 폭탄 방지).
- 게이트 off 또는 FCM 키 부재 시 무동작(운영 안전).

---

## 6. 완료 기준 (DoD)
- [ ] `build.gradle` Firebase Admin SDK 의존성 추가, 컴파일 성공.
- [ ] `application.properties` 키/게이트/주기 placeholder + `.env.example` 키 이름 추가(실값 없이).
- [ ] PushSendScheduler(게이트), PushSenderService(+impl), FcmClient(래퍼), PushOutboxMapper(+xml) 구현.
- [ ] PENDING → 전송 → SENT/FAILED 상태전이, RETRY_CNT/ERROR_MSG 갱신.
- [ ] 무효 토큰 정리(정책대로), NO_DEVICE_TOKEN 케이스 명확 기록.
- [ ] 동시성(claim/SKIP LOCKED)으로 중복 발송 방지(다중 인스턴스 전제 시).
- [ ] 게이트 off/키 부재 시 부팅·동작에 영향 없음.
- [ ] 모든 SQL `/* Mapper.method */` 주석, leading comma, `#{}` 바인딩, SELECT * 금지.
- [ ] 단위테스트: 상태전이/재시도 누적/무효토큰 정리/토큰0건 — `FcmClient` mock(실제 FCM 미호출).
- [ ] 키/토큰/PII 로그 평문 미출력.

## 7. 작업 중단 조건 (착수 전 확인, 위반 시 YJ 질의)
- FCM 서비스 계정 키 주입 경로(00 A-1·A-2) 미확정이면 **중단**.
- 무효토큰 정리 정책(00 B-2), 동시성 전제(00 B-3)가 불명확하면 기본값(NULL 처리 / claim UPDATE)으로 진행하되 보고.
- 앱 토큰 등록(00 A-3) 미완은 **중단 사유 아님**(구현 진행, 도달 테스트만 보류).

---

## 8. 참고 (기존 코드 패턴)
- 스케줄러 게이트/cron: `com.prafta.common.schedule.leave.scheduler.LeaveGrantScheduler` (`@Value` enabled + `@Scheduled`).
- outbox 생산자/컬럼: `com.prafta.common.cmm.leave`(LeaveDashboardMapper.insertNotiOutbox, NotiOutboxInsertVO), `prafta-031-noti-outbox.sql`.
- env/시크릿 운영 방식: `.claude/context/backend-environment.md` (env 주입, .env.example 패턴).
