# prafta-app-004 — TBM 입실/종료(앱) + 스케줄 없는 날 추가근무 상신 강제 (app-003 분리분)

> 출처: prafta-app-003(앱 출퇴근 GPS) 작업 중 범위 분리. 사용자 지시로 C(TBM 입실)와 §7.5(추가근무 강제)를 별도 작업서로 정리.
> 본 문서는 **명세/분해 착수용**(아직 미구현). 미래 세션은 planner→developer→security→qa 로 진행.
> 선행 재사용: app-003 A0-1 GPS 브리지(`@/utils/gpsBridge` `requestGps()` + Flutter `GET_GPS`), 위치권한 하드게이트.
> ⚠️ 스키마는 메인 세션이 MCP로 확정(아래). 추측 금지.

---

## PART 1 — C: TBM 입실/종료 액션 (앱)

### 1.1 배경/정합
- prafta-app-001에서 앱 메인 TBM 카드는 **읽기전용 상태 표시만** 처리. 본 파트는 **입실/종료 쓰기 액션**.
- 웹 TBM 고도화는 `web_requests/prafta-033-*`(A: DDL+콘텐츠, B: 세션관리, C: 실시간-DEFERRED, D: 이력)에서 진행 — **prafta-033과 정합 필수**. 특히 `tb_tbm_session` 의 STARTED_AT/ENDED_AT 가 "[C단계]"(prafta-033-C 실시간) 표시라, 앱 입실이 세션 상태전이와 어떻게 맞물리는지 prafta-033-C 보류분과 함께 설계.
- TBM은 출퇴근(근태)과 **별개 도메인** — `tb_user_attd_gps`/`tb_site` 지오펜스와 무관하게 **TBM 자체 GPS 필드**를 쓴다.

### 1.2 확정 스키마 (MCP)
**tb_tbm_session** (PK SESSION_CD='T'+YYYYMMDD+SEQ):
- STATUS_CD[SYS046]: DRAFT/OPENED/IN_PROGRESS/COMPLETED/CANCELLED
- ENTRY_PWD(varchar10, 랜덤6자리, OPENED부터 생성), EXIT_PWD(입실≠종료)
- MANAGER_USER_CD(개설자), MANAGER_GPS_LAT/LON(decimal10,7, AUTO 모드 개설 좌표)
- GPS_VERIFY_TYPE_CD[SYS048]: AUTO/MANUAL/DISABLED, GPS_VERIFY_RADIUS_M(int, 기본100, 50~1000), GPS_MANUAL_CONFIRM_YN
- OPENED_AT/STARTED_AT/ENDED_AT/CANCELLED_AT, TITLE, CONTENT_BODY
**tb_tbm_attendance** (PK ATTENDANCE_CD='A'+YYYYMMDD+SEQ, **UNIQUE(CMPNY_CD,SESSION_CD,USER_TYPE_CD,USER_CD)**):
- USER_TYPE_CD[SYS050]: REGULAR(TB_USER)/DAILY(TB_DAILY_USER), USER_CD
- ENTRY_TYPE_CD[SYS051]: SELF_DEVICE(본인디바이스)/MANAGER_QR_SCAN(관리자QR), ENTRY_BY_MANAGER_USER_CD
- ENTRY_AT, ENTRY_GPS_LAT/LON(decimal10,7), ENTRY_DISTANCE_M(int, 개설지점과 거리), ENTRY_SIGN_FILE_MGMT_CD(입실 서명 파일)
- EXIT_TYPE_CD[SYS052]: SELF/MANAGER_QR_SCAN/MANAGER_FORCED, EXIT_AT(NULL=미종료), EXIT_SIGN_FILE_MGMT_CD, EXIT_FORCED_REASON
- COMPLETION_STATUS_CD[SYS053]: COMPLETED/NOT_COMPLETED
- 보조: tb_tbm_pwd_fail(비밀번호 실패 로그), tb_tbm_attendance_event(출결 이벤트 로그), tb_tbm_session_state(실시간 동기화).

### 1.3 입실 흐름 (앱, 정규직 기준 MVP)
1. 사용자가 세션 입실: **입실 비밀번호(ENTRY_PWD) 입력** 경로 또는 **관리자 QR 스캔**(ENTRY_TYPE_CD=SELF_DEVICE / MANAGER_QR_SCAN). QR은 기존 `qr_scan_page.dart`/QrScanner 패턴 활용.
2. **GPS 검증**: GPS_VERIFY_TYPE_CD=AUTO 면 입실 좌표(A0-1 `requestGps`)와 세션 MANAGER_GPS_LAT/LON 거리(haversine) ≤ GPS_VERIFY_RADIUS_M 검증 → ENTRY_GPS_LAT/LON + ENTRY_DISTANCE_M 기록. MANUAL/DISABLED 분기 처리. (출퇴근 지오펜스와 별개 — tb_site 아님, 세션 좌표 기준)
3. **입실 서명**(ENTRY_SIGN_FILE_MGMT_CD): 서명 캡처/업로드 필요 여부 정책 확인(있으면 파일 업로드 연계).
4. tb_tbm_attendance INSERT(UNIQUE로 동일 세션 중복입실 방지 → 중복 시 멱등/안내). 비밀번호 실패는 tb_tbm_pwd_fail 기록.
5. 종료: EXIT_PWD/관리자QR/관리자강제 → EXIT_AT + (EXIT 서명) + COMPLETION_STATUS_CD.

### 1.4 미확정/설계 결정 필요
- 입실/종료 **서명** 필수 여부 + 파일 업로드 경로(기존 파일관리 연계).
- 일용직(DAILY) 입실 포함 범위(MVP는 정규직만? DAILY는 일일계정 흐름과 연계).
- 세션 상태(STATUS_CD) 전이와 앱 입실 가능 구간(OPENED만? IN_PROGRESS까지?) — prafta-033-C(실시간) 정합.
- 비밀번호 실패 잠금 정책(tb_tbm_pwd_fail 임계/잠금).
- 백엔드 신규 엔드포인트(예: `POST /appApi/tbm/enter`, `/exit`) — 신규 모듈 `com.prafta.app.tbm.*`.
- 정책서 출처: prafta-033 기획 + 안전교육/TBM 정책(있으면). SYS046~053 코드값 운영 등록 확인.

---

## PART 2 — §7.5: 스케줄 없는 날 추가근무 상신 강제

### 2.1 정책 (attd §7.5)
- 스케줄 없는 날 출퇴근 등록은 허용(✅ app-003 A1에서 출근 허용 구현됨).
- 그날 근무는 **전량 추가근무(초과근무)** 로 취급.
- **추가근무 상신 + 관리자 승인 필수**.
- **미승인 시 근태 마감 차단**.

### 2.2 현재 상태 (app-003 A1)
- 스케줄 없는 날 출근 INSERT 까지만 구현. "추가근무 상신 강제/마감차단 연계"는 **미구현**(본 파트).

### 2.3 확정 스키마 (MCP) — 초과근무 요청
**tb_user_attd_req** (PK REQ_ID): REQ_TYPE[SYS032]='03'(초과근무 생성), TARGET_ID(생성 시 NULL), REQ_STATUS[SYS033] 01신청/02승인/03반려/04취소, WORK_YMD, WORK_SEQ, START/END_DATE·TIME, **OT_TYPE**(EXTEND연장/NIGHT야간/HOLIDAY휴일), PROCESS_USER_CD/COMMENT/DATE.
- 결재라인: `tb_user_attd_req_approval`(요청별 결재라인, 사용자정의) — prafta-020(결재라인 프리셋)/025/027 흐름 정합.
- 처리이력: prafta-027 — HIST_TYPE[SYS032] 03(초과근무 생성요청)/08(승인)/09(반려), 그날 ATTD_ID 앵커.

### 2.4 설계 방향 (결정 필요)
- **트리거**: 스케줄 없는 날 check-in(또는 check-out 완료) 시점에 (a) 자동으로 초과근무 상신(tb_user_attd_req REQ_TYPE='03') 생성 + 결재라인 부여, 또는 (b) 앱이 "이 날은 추가근무 상신이 필요합니다" 안내 후 사용자가 상신 화면으로 유도. → **(b) 권장**(상신 내용=시간/사유/OT_TYPE 사용자 입력 필요, 자동생성은 OT_TYPE/사유 임의값 문제).
- **OT_TYPE 판정**: 휴일(공휴일/주말)=HOLIDAY, 야간 포함=NIGHT, 그 외 EXTEND. 자동 추정 vs 사용자 선택.
- **마감 차단 연계**: prafta-028 근태 마감(tb_attd_close) 선행조건에 "스케줄 없는 날 근무 중 미승인 초과근무 존재" 차단 사유 추가. (app-002 read의 dayType=ACTION_REQUIRED 판정에도 반영)
- **결재라인 소스**: prafta-020 프리셋/사용자정의 결재라인 재사용.
- 백엔드: 초과근무 상신 엔드포인트(웹 attd07에 OT 생성 로직 있음 — `InsertUserOvertimeCommand` 등 재사용 검토). 앱은 `POST /appApi/attd/overtime-request` 신규 or 기존 재사용.
- 정책서 출처: attd §7.5, §9.3(초과근무 상신, 사후=마감 전까지), §10.3(초과근무 계산), §13(마감 차단). request-approval 재기획서.

---

## 공통 선행/참조
- app-003 A0-1 GPS 브리지(requestGps), 위치권한 하드게이트 재사용(TBM 입실 GPS).
- 스키마: 위 MCP 확정분(tb_tbm_session/attendance, tb_user_attd_req). 코드값 SYS032/033/046/047/048/050/051/052/053 운영 등록 확인.
- 웹 정합: `web_requests/prafta-033-*`(TBM), 초과근무 결재(prafta-020/025/027), 마감(prafta-028).
- 권장 분할: **C(TBM)** 와 **§7.5(추가근무 강제)** 는 도메인이 다르므로 각각 독립 단위로 착수(같은 문서에 정리하되 구현은 분리).

## 권장 착수 순서
1. C-0(조사): prafta-033-C(실시간) 보류분 정독 + TBM 입실 정책/서명 요구 확정.
2. C: TBM 입실/종료(정규직 MVP) → security/qa.
3. §7.5: 마감차단 연계 + 상신 유도(또는 자동생성) → security/qa.
(둘은 독립이라 순서 무관, C가 사용자 가치 더 큼.)
