# [작업지시서] 노무수령거부 통지 + 출근감지 + 관리자 PUSH (MVP)

> 대상: Claude Code (developer agent)
> 선행조건: `00_선행조치_사용자작업.md`의 A·B 항목이 **모두 완료**된 상태여야 함.
> 특히 A-3(사용지정일 저장 구조 존재) 미확정 시 이 작업 착수 금지.

---

## 0. 작업 목표 (이 범위만, 초과 금지)

연차촉진(근로기준법 제61조 1항) 사용지정일에 대해 다음 3개 이벤트를 **영구 기록**하고
관리자에게 PUSH를 발송하는 백엔드 기능을 구현한다.

1. **통지**: 노무수령거부 대상일에 대해 근로자에게 통지(outbox INSERT)
2. **출근 감지**: 대상일에 근로자가 체크인하면 이를 감지하고 영구 기록
3. **관리자 PUSH**: 출근 감지 시 관리자에게 PUSH 발송(outbox INSERT)

### 명시적 비범위 (만들지 말 것)
- 관리자 차단조치 입력/로그, 상태머신, 최종확정 UI
- 출근 기록 삭제/무효화 (출근 원본 `tb_user_attd_mgmt`는 **읽기만**, 수정 금지)
- 실제 FCM 전송 (outbox에 PENDING으로 INSERT까지만. 전송 워커는 별도 작업)
- 제61조 2항(1년 미만)

---

## 1. 신규 테이블: `tb_leave_refusal_log`

노무수령거부 관련 이벤트를 **사실 기록**으로 남기는 전용 테이블. 출퇴근/연차사용 원본과 분리.

### 설계 원칙
- 출근 원본(`tb_user_attd_mgmt`)은 절대 수정하지 않는다. 본 테이블은 "관찰/통지 사실"만 적재.
- 이벤트는 append 위주. 한 (회사+사업장+사용자+대상일) 조합에 대해 통지/감지/알림이 누적 기록될 수 있음.
- 중복 발송 방지를 위해 dedup 키 사용.

### DDL (기존 컨벤션 준수: 대문자 언더스코어, VARCHAR(8) 날짜, 채번 PK)

```sql
CREATE TABLE `tb_leave_refusal_log` (
  `REFUSAL_ID`      varchar(20)  NOT NULL COMMENT '노무수령거부 로그 ID (PK, 회사별 채번: LR + YYYYMMDD + SEQ)',
  `CMPNY_CD`        varchar(50)  NOT NULL COMMENT '회사 코드',
  `SITE_CD`         varchar(50)  NOT NULL COMMENT '사업장 코드',
  `USER_CD`         varchar(20)  NOT NULL COMMENT '대상 근로자 코드',
  `TARGET_YMD`      varchar(8)   NOT NULL COMMENT '노무수령거부 대상일 (YYYYMMDD, =연차촉진 사용지정일)',
  `EVENT_TYPE`      varchar(20)  NOT NULL COMMENT '이벤트 유형[SYS0xx] NOTICED:통지 / CHECKIN_DETECTED:출근감지 / ADMIN_ALERTED:관리자알림',
  `RELATED_NOTI_ID` varchar(20)           COMMENT '연관 알림 ID (tb_noti_outbox.NOTI_ID, 통지/관리자알림 시)',
  `RELATED_ATTD_ID` varchar(20)           COMMENT '연관 근태 ID (tb_user_attd_mgmt.ATTD_ID, 출근감지 시)',
  `DETECT_DTIME`    datetime              COMMENT '출근 감지 일시 (CHECKIN_DETECTED 시)',
  `DETAIL`          json                  COMMENT '추가 페이로드 (PII 평문 금지)',
  `DEL_YN`          varchar(1)   NOT NULL DEFAULT 'N' COMMENT '삭제 여부 (사실기록 무삭제 원칙)',
  `INSERT_NO`       varchar(50)  NOT NULL COMMENT '등록자 (=USER_CD or SYSTEM)',
  `INSERT_DATE`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '등록 일시',
  `UPDATE_NO`       varchar(50)           COMMENT '수정자',
  `UPDATE_DATE`     datetime              COMMENT '수정 일시',
  `DEDUP_KEY`       varchar(120)          COMMENT '중복 방지 키 (CMPNY_CD+USER_CD+TARGET_YMD+EVENT_TYPE)',
  PRIMARY KEY (`REFUSAL_ID`),
  KEY `IX_REFUSAL_LOG_USER`   (`CMPNY_CD`, `SITE_CD`, `USER_CD`, `TARGET_YMD`),
  KEY `IX_REFUSAL_LOG_TARGET` (`CMPNY_CD`, `SITE_CD`, `TARGET_YMD`, `EVENT_TYPE`),
  UNIQUE KEY `UK_REFUSAL_LOG_DEDUP` (`CMPNY_CD`, `DEDUP_KEY`)
) COMMENT='노무수령거부 통지/감지/알림 사실 기록 (출퇴근 원본 무수정)';
```

> 주의: `EVENT_TYPE`의 SYS 코드 그룹 번호(SYS0xx)는 선행조치 B-1에서 YJ가 부여한 값으로 치환할 것.
> 코드값을 임의 생성하지 말고, 미확정이면 작업 중단하고 YJ에게 확인 요청.

---

## 2. 채번
- `REFUSAL_ID`: 기존 `tb_cmm_seq` 채번 패턴 사용. prefix `LR` + YYYYMMDD + SEQ.
- 기존 다른 테이블(예: 알림 N, 감사 A)의 채번 헬퍼/서비스를 그대로 재사용. 새 채번 로직 임의 생성 금지.

---

## 3. 기능 1 — 노무수령거부 통지 발송

### 입력
- 대상: (CMPNY_CD, SITE_CD, USER_CD, TARGET_YMD) 목록
- 호출 방식: 선행조치 A-2 결정에 따름. **기본 가정 = 사용지정일 전일/당일 일괄 통지(배치 또는 관리자 수동 트리거)**.

### 처리
1. 각 대상에 대해 `tb_noti_outbox` INSERT:
   - `NOTI_TYPE` = `LEAVE_REFUSAL_NOTICE` (SYS045, 선행 B-1)
   - `CHANNEL` = `PUSH`
   - `TARGET_USER_CD` = 근로자 USER_CD
   - `TITLE`/`BODY` = §6 템플릿(근로자용)
   - `DATA_PAYLOAD` = `{ "type":"LEAVE_REFUSAL_NOTICE", "targetYmd":"YYYYMMDD" }`
   - `SEND_STATUS` = `PENDING`
   - `DEDUP_KEY` = `LRN_{USER_CD}_{TARGET_YMD}` (이벤트당 1건)
2. `tb_leave_refusal_log` INSERT:
   - `EVENT_TYPE` = `NOTICED`
   - `RELATED_NOTI_ID` = 위 outbox NOTI_ID
   - `DEDUP_KEY` = `{CMPNY_CD}_{USER_CD}_{TARGET_YMD}_NOTICED`
3. dedup 충돌(이미 통지함) 시 INSERT 건너뜀 (멱등 보장). 에러 아님.

### 멱등성
- 동일 (USER_CD, TARGET_YMD) 재호출 시 outbox/로그 모두 UNIQUE로 1건만 유지.
- `INSERT ... AS NEW ... ON DUPLICATE KEY UPDATE` 사용 시 row alias 문법 사용 (VALUES() 금지).

---

## 4. 기능 2 — 출근 감지 (체크인 hook)

### 트리거 위치
선행조치 A-4 결정 = **동기 hook**. 출근 체크인 서비스(`tb_user_attd_mgmt` INSERT 직후) 내부에서 검사.

### 처리 (체크인 성공 직후)
1. 방금 체크인한 (CMPNY_CD, SITE_CD, USER_CD, WORK_YMD) 가 **노무수령거부 대상일**인지 판정.
   - 판정 소스 = 선행조치 A-3에서 확정한 "사용지정일" 데이터.
   - **휴일 게이트**: 대상일이 `tb_holiday`/`tb_holiday_rule`상 휴일이면 노무수령거부 대상이 아님 → 즉시 종료. (앞선 논의: 휴일은 노무수령거부 대상 아님)
2. 대상일이면 `tb_leave_refusal_log` INSERT:
   - `EVENT_TYPE` = `CHECKIN_DETECTED`
   - `RELATED_ATTD_ID` = 방금 INSERT된 ATTD_ID
   - `DETECT_DTIME` = now()
   - `DEDUP_KEY` = `{CMPNY_CD}_{USER_CD}_{TARGET_YMD}_CHECKIN_DETECTED`
3. **출근 원본은 절대 수정/삭제하지 않는다.** 감지 기록만 추가.
4. 감지되면 기능 3(관리자 PUSH) 호출.

### 성능/안전
- 이 검사는 체크인 트랜잭션을 막지 않도록 가볍게. 사용지정일 조회는 인덱스 활용.
- 검사 중 예외가 나도 **체크인 자체는 성공**해야 함 (감지 실패가 출근을 막으면 안 됨). try-catch로 격리, 실패 시 로그만.

---

## 5. 기능 3 — 관리자 PUSH 발송

### 처리 (출근 감지 직후)
1. 대상 관리자 식별:
   - 해당 사업장(SITE_CD)의 관리자 권한 사용자 조회 (`tb_user_site_auth` 기준).
   - 관리자가 여러 명이면 각각 outbox INSERT.
2. 각 관리자에 대해 `tb_noti_outbox` INSERT:
   - `NOTI_TYPE` = `LEAVE_REFUSAL_CHECKIN_ALERT` (SYS045, 선행 B-1)
   - `TARGET_USER_CD` = 관리자 USER_CD
   - `TITLE`/`BODY` = §6 템플릿(관리자용)
   - `DATA_PAYLOAD` = `{ "type":"LEAVE_REFUSAL_CHECKIN_ALERT", "targetUserCd":"...", "targetYmd":"YYYYMMDD", "attdId":"..." }`
   - `DEDUP_KEY` = `LRA_{관리자USER_CD}_{대상USER_CD}_{TARGET_YMD}` (관리자×대상자×날짜당 1건)
3. `tb_leave_refusal_log` INSERT:
   - `EVENT_TYPE` = `ADMIN_ALERTED`
   - `RELATED_NOTI_ID` = (대표 1건 또는 첫 관리자 NOTI_ID)
   - `DEDUP_KEY` = `{CMPNY_CD}_{대상USER_CD}_{TARGET_YMD}_ADMIN_ALERTED`

> 실제 단말 전송은 하지 않음. outbox PENDING INSERT까지가 이 작업의 끝.
> (선행조치 A-1에서 전송 워커가 이미 있다고 확정되면, 기존 워커가 자동으로 집어감)

---

## 6. 메시지 템플릿

> 최종 문구는 선행조치 C(노무사 검토)에서 확정된 것으로 교체. 아래는 초안.

### 근로자용 (LEAVE_REFUSAL_NOTICE)
- TITLE: `[연차 사용지정일 안내]`
- BODY:
  `{근로자명}님, {TARGET_YMD}은 미사용 연차 사용지정일입니다. 회사는 금일 노무 제공을 수령하지 않으며 업무가 부여되지 않습니다. 해당일은 연차휴가 사용일로 처리됩니다.`

### 관리자용 (LEAVE_REFUSAL_CHECKIN_ALERT)
- TITLE: `[노무수령거부일 출근 감지]`
- BODY:
  `{근로자명}님이 노무수령거부 지정일({TARGET_YMD})에 출근 기록을 남겼습니다. 현장 확인 및 노무 미부여 조치가 필요합니다.`

---

## 7. DTO 흐름 (프로젝트 DTO 컨벤션 준수)

제공된 DTO 플로우(request → param → query/command → result → response)를 따른다.

### 7-1. 통지 발송 API (관리자 수동 트리거용, 배치면 생략 가능)
- `POST /leaveRefusal/sendNotices`
- request: `List<LeaveRefusalNoticeRequest>` (대상 USER_CD, TARGET_YMD 목록) → list이므로 **model 경유**
  - `LeaveRefusalNoticeRequest` (Getter/Setter/NoArgsConstructor)
  - `LeaveRefusalNoticeModel` (record, tokenInfo의 gvCmpnyCd/gvUserId 포함)
  - `LeaveRefusalNoticeParam` (record, `from(List<...> requests, TokenInfo)` 팩토리, null·필수값 검증, `ApiException.appendf(CommonErrorCode.COMMON_400_001, ...)`)
- command: `LeaveRefusalLogCommand`, `NotiOutboxCommand` (XML INSERT용)
- response: 없음 또는 처리건수 (`ResponseEntity.ok`)

### 7-2. 출근 감지 hook (내부 서비스, API 아님)
- 체크인 서비스 내부 호출. 별도 controller 불필요.
- 내부 메서드: `leaveRefusalService.detectAndAlert(AttdCheckinModel)` 형태.
- 사용지정일 조회 query: `LeaveRefusalTargetQuery` (CMPNY_CD, SITE_CD, USER_CD, WORK_YMD, gvCmpnyCd)
- 결과: `LeaveRefusalTargetResult` (대상여부, TARGET_YMD)

### 7-3. MyBatis XML
- 모든 SQL에 `/* MapperName.methodName */` 주석 포함.
- 매퍼: `LeaveRefusalMapper` (xml + interface).
  - `selectRefusalTarget` : 사용지정일 + 휴일 게이트 조인 (휴일이면 결과 제외)
  - `insertRefusalLog` : append, dedup UNIQUE 충돌 무시(`INSERT ... ON DUPLICATE KEY UPDATE UPDATE_DATE=NOW()` row alias 문법)
  - `insertNotiOutbox` : 기존 알림 매퍼가 있으면 재사용. 없을 때만 신규.

---

## 8. 트랜잭션 경계
- 통지 발송(기능1): outbox INSERT + 로그 INSERT를 한 트랜잭션.
- 출근 감지(기능2) + 관리자 PUSH(기능3): **체크인 트랜잭션과 분리**.
  - 체크인 커밋 후 별도로 처리(또는 같은 트랜잭션이되 감지/알림 실패가 체크인을 롤백하지 않도록 예외 격리).
  - 권장: 체크인 성공 → 커밋 → afterCommit 시점에 감지·알림 수행. 실패 시 로그만 남기고 삼킴.

---

## 9. 완료 기준 (DoD)
- [ ] `tb_leave_refusal_log` 테이블 생성 (DDL 적용)
- [ ] SYS045에 2개 알림 타입, 신규 SYS0xx 이벤트 코드 시드 등록 (선행 B-1 값 사용)
- [ ] 통지 발송 시 outbox(PENDING) + 로그(NOTICED) 동시 기록, 멱등
- [ ] 노무수령거부 대상일 출근 시 로그(CHECKIN_DETECTED) 기록, 출근 원본 무수정
- [ ] 휴일은 대상에서 제외됨 (휴일 게이트 단위테스트)
- [ ] 출근 감지 시 관리자 outbox(PENDING) + 로그(ADMIN_ALERTED) 기록
- [ ] 감지/알림 실패가 체크인 트랜잭션을 롤백하지 않음 (예외 격리 테스트)
- [ ] 모든 SQL에 `/* Mapper.method */` 주석
- [ ] DTO 흐름 컨벤션 준수 (request→param→query/command→result→response, list는 model 경유)

## 10. 작업 중단 조건 (착수 전 확인, 위반 시 YJ에게 질의)
- 사용지정일 저장 구조(선행 A-3)가 없으면 **중단**.
- SYS 코드값(선행 B-1)이 미확정이면 **중단**.
- `tb_user_device`에 푸시 토큰 컬럼 유무가 불명확해도, 이번 작업은 outbox INSERT까지이므로 **진행 가능**.
