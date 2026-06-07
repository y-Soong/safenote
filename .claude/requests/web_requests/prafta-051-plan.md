# prafta-051 작업지시서 — TBM 교육 세션 상태머신 재설계

- 요청서: .claude/requests/web_requests/prafta-051.md
- 참고(모바일 원본 기획): .claude/requests/web_requests/ref/prafta-051/prafta-051-참고.md
- 분해자: planner / 분해일: 2026-06-07
- 영역: 웹(Tbm_02 중심) + 앱(입실) + Flutter(브리지) + DB
- 채번: 작업 ID prafta-051-{NN}, UI 명세 UI-051-{NN}

## 0. 배경 / 재설계 요지
현재 TBM 세션은 개설(OPENED) 시점에 입실/종료 비번 동시 발급 + GPS 중심좌표 수집 → 현장 집결 전 입실 가능(부정 우려).
재설계 후 흐름:
  개설(DRAFT) → 교육준비(OPENED) → 교육시작(IN_PROGRESS) → 교육종료(COMPLETED), 취소(CANCELLED)는 DRAFT/OPENED에서만.

| 단계 | STATUS_CD | 이벤트 |
|---|---|---|
| 개설 | DRAFT | 사업장/제목/내용/GPS검증여부(좌표 미수집)/교육자료/위험성평가만 저장. 비번·좌표 없음 |
| 교육준비 | OPENED | 입실비번 발급 + 관리자 GPS 중심좌표(웹 위치권한) + 앱 입실 허용 + 15분 자동시작 타이머 시작 |
| 교육시작 | IN_PROGRESS | (자동:준비+15분) 또는 수동 전이. 입실 마감 |
| 교육종료 | COMPLETED | 종료비번 발급. 종료 후 미이수 처리 가능 |
| 취소 | CANCELLED | DRAFT/OPENED에서만(기존 유지) |

상태코드는 신규 추가 없이 SYS046 값 재활용 + 표시 라벨만 변경(확정사항 2).

## 1. 현재 구현 사실 (검증 완료)
- TB_TBM_SESSION에 STARTED_AT/ENDED_AT/OPENED_AT/CANCELLED_AT 컬럼은 실재하나 IN_PROGRESS/COMPLETED 전이는 미구현.
- TB_TBM_ATTENDANCE 컬럼은 prafta-033-A 마이그로 확인(APP_FOREGROUND_SEC 없음 → 신규).
- TB_DAILY_USER 만료판정 컬럼: USE_YN / ACCOUNT_STATUS(SYS013) / WORK_EXPIRE_DATE(varchar8) / WITHDRAWAL_DATE (schema-full.sql).
- 앱 exit는 현재 세션 STATUS 무검사.
- 15분 자동전이 스케줄러 없음(PushSendScheduler 게이트 패턴 차용 대상).
- MANAGER_GPS_*는 decimal.
- SYS046은 selectSessionDetail 등에서 SYST_VAL_D_NM 조인으로 라벨 노출.

## 2. 작업 분해
착수 순서: DB(01) → 웹BE(02~06) → 앱BE(07~08) → Flutter+앱FE(09~10) → 웹FE(11~16). tbm 법적책임 +1.

### prafta-051-01 — DB 마이그레이션 + SYS046 라벨 변경 [backend/DB]
핵심:
 1) TB_TBM_SESSION.PREP_START_AT datetime NULL 추가 — 15분 타이머 '가변 기준시각'. OPENED 전이 시 NOW(), 수동연장 시 NOW() 리셋, 자동/수동 시작 시 동결. OPENED_AT은 최초 준비시각 감사기록으로 보존(덮어쓰기 금지). COMMENT '교육준비 타이머 기준시각(15분 자동 교육시작 기준, 수동연장 시 리셋)'.
 2) TB_TBM_ATTENDANCE.APP_FOREGROUND_SEC int NULL 추가 — 앱 포그라운드 누적초(SELF_DEVICE만). COMMENT '앱 포그라운드 누적초(SELF_DEVICE 종료 시 1회 수신, 대리/검색입실 NULL)'.
 3) SYS046 표시명 DML(코드값 불변): DRAFT→개설, OPENED→교육준비, IN_PROGRESS→교육시작, COMPLETED→교육종료, CANCELLED→취소. STATUS_CD 컬럼 COMMENT 라벨도 동반 갱신(DB COMMENT 규칙). 운영 현재 라벨 MCP 확인 후 정확 UPDATE.
 4) SYS051 신규 MANAGER_DIRECT(관리자 웹 직접 입실) 추가 DML(잠정, C1 확정 시 교체).
산출물: prafta-backend/src/main/resources/sql/migration/prafta-051-tbm-session-redesign.sql (운영 선적용 필수, 작성만)
선행: 없음

### prafta-051-02 — 웹: 비밀번호 분리발급 [backend, tbm02]
핵심:
 1) saveSession에서 OPENED 저장 시 entryPwd/exitPwd 발급 폐기(둘 다 null). (C2: 개설=DRAFT 고정 권장)
 2) 입실비번 발급은 교육준비 전이 EP(03)로 이동. regeneratePasswords는 입실비번만 재발급.
 3) 종료비번 발급은 교육종료 전이 EP(05). 종료비번 재발급 EP(regenerate-exit-password, COMPLETED만) 추가.
 4) 기입실/기종료자 무영향 확인·명시: 비번은 enter/exit 시점에만 검증, 결과는 TB_TBM_ATTENDANCE에 확정 → 재발급은 SESSION만 UPDATE하고 ATTENDANCE 미변경(코드 확인).
파일: Tbm02ServiceImpl, SessionCommand, Tbm02Mapper.xml(updateSessionPwd 입실전용 + 종료전용 신규)
EP: POST /webApi/tbm02/save-session, /regenerate-passwords(입실), (신규) /regenerate-exit-password
선행: 01

### prafta-051-03 — 웹: 교육준비(OPENED) 전이 EP + GPS 중심좌표 [backend, tbm02]
핵심:
 1) 신규 POST /webApi/tbm02/prepare-session — sessionCd, managerGpsLat/Lon(웹 위치권한 수집), gpsVerify*.
 2) DRAFT에서만 전이(C3: OPENED 재호출 멱등 여부 확인).
 3) validateGps 재사용(AUTO면 좌표 필수).
 4) 입실비번 발급(6자리, 종료비번 null).
 5) STATUS=OPENED, OPENED_AT=NOW()(최초만), PREP_START_AT=NOW(), MANAGER_GPS_* 저장.
 6) verifyManageAuth + verifyScope.
파일: Tbm02Controller/Service/impl, 신규 DTO, Tbm02Mapper.xml(prepareSession UPDATE), TbmErrorCode
EP: POST /webApi/tbm02/prepare-session
선행: 01, 02

### prafta-051-04 — 웹: 교육시작(IN_PROGRESS) 수동 전이 + 연장 EP [backend, tbm02]
핵심:
 1) 신규 POST /webApi/tbm02/start-session(수동 교육시작): OPENED만, STATUS=IN_PROGRESS, STARTED_AT=NOW(), 입실 마감(07과 정합).
 2) 신규 POST /webApi/tbm02/extend-prep(연장): OPENED + PREP_START_AT+15분 미도래에서만, PREP_START_AT=NOW() 리셋. 경과 후 거부.
    경합 가드: UPDATE ... WHERE STATUS='OPENED' AND PREP_START_AT > NOW()-INTERVAL 15 MINUTE.
 3) verifyManageAuth + verifyScope.
파일: 컨트롤러/서비스/impl/DTO/Mapper(startSession/extendPrep), TbmErrorCode
EP: POST /webApi/tbm02/start-session, /extend-prep
선행: 03

### prafta-051-05 — 웹: 교육종료(COMPLETED) 전이 + 종료비번 [backend, tbm02]
핵심:
 1) 신규 POST /webApi/tbm02/complete-session: IN_PROGRESS만, STATUS=COMPLETED, ENDED_AT=NOW(), 종료비번 발급(입실비번 미변경).
 2) regenerate-exit-password는 COMPLETED만.
 3) verifyManageAuth + verifyScope.
 4) C4: OPENED→곧바로 종료 가능 여부. 잠정 IN_PROGRESS에서만.
EP: POST /webApi/tbm02/complete-session
선행: 04

### prafta-051-06 — 웹: 15분 자동 교육시작 스케줄러(신규) [backend/배치]
핵심:
 1) 신규 TbmAutoStartScheduler — PushSendScheduler 패턴(@Component, @Scheduled fixedDelay, @Value 게이트 prafta.tbm.autostart.enabled:false).
 2) 매 주기 STATUS='OPENED' AND PREP_START_AT <= NOW()-INTERVAL 15 MINUTE AND DEL_YN='N' → IN_PROGRESS(STARTED_AT=NOW()). WHERE에 동일 조건 재포함(04와 원자적 경합).
 3) 예외 log.error 후 삼킴. 전수 스캔(STATUS+시각 조건만으로 안전).
 4) @EnableScheduling은 MainApplication에 이미 존재(확인).
 5) C5: 게이트 기본 false vs 즉시 ON. 잠정 false.
파일: 신규 스케줄러 + 전용 mapper(bulkStartExpiredPrep), application.yml 프로퍼티 문서화
선행: 04

### prafta-051-07 — 앱: enter 상태조건 정합 + APP_FOREGROUND_SEC NULL [backend, app/tbm01]
핵심:
 1) enter는 OPENED만(현행 STATUS_OPENED 게이트 재확인, 코드 무변경 가능).
 2) 입실 INSERT 시 ENTRY_TYPE_CD='SELF_DEVICE', APP_FOREGROUND_SEC 미세팅(NULL).
 3) IN_PROGRESS 후 enter는 TBM_409_030으로 거부(기존 충족).
EP: POST /appApi/tbm01/enter
선행: 03

### prafta-051-08 — 앱: exit 상태조건 + APP_FOREGROUND_SEC 수신/저장 [backend, app/tbm01]
핵심:
 1) exit 요청 DTO에 appForegroundSec(Integer, nullable) 추가(body 도착).
 2) TbmExitCommand + updateExit mapper에 APP_FOREGROUND_SEC=#{appForegroundSec}. SELF_DEVICE 출결만 값(대리/검색입실은 NULL 유지).
 3) C6: exit 허용 상태 — 잠정 IN_PROGRESS+COMPLETED 둘 다. 음수/과대값 방어(0 이상, 상한 클램프).
 4) (부수) tbm04 출결 조회 select에 appForegroundSec 추가(051-16 표시용).
 5) 한계 명시: 앱 강제종료 시 누적 유실 → NULL/과소, 미종료와 겹칠 수 있음(보조지표).
파일: TbmExitRequest/Param/Command, AppTbm01ServiceImpl, AppTbm01Mapper.xml(updateExit), tbm04 select
EP: POST /appApi/tbm01/exit (multipart: signFile+exitPwd+appForegroundSec)
선행: 01, 09

### prafta-051-09 — Flutter: 포그라운드 누적 + GET_APP_FOREGROUND_SEC 브리지 [Flutter]
핵심:
 1) _WebAppState에 int _fgAccumSec=0; DateTime? _fgResumedAt;.
 2) didChangeAppLifecycleState 확장: resumed→_fgResumedAt=now(기존 QR 스캔 분기 보존); paused/inactive/detached→경과초 누적·_fgResumedAt=null.
 3) 신규 addJavaScriptHandler('GET_APP_FOREGROUND_SEC') → {status:'OK', foregroundSec:int}(누적+진행분 합산).
 4) 비즈니스 로직 금지(누적/합산/반환만). 귀속·NULL·저장은 Vue/BE.
 5) 한계: detached 미수신 시 유실.
파일: PRAFTA_FLUTTER/safenote/lib/web_app.dart
선행: 없음(08/10과 계약 동시)

### prafta-051-10 — 앱 FE: 포그라운드 래퍼 + exit 전송 [frontend-component, app]
핵심:
 1) 신규 prafta-app-frontend/src/utils/foregroundBridge.js(gpsBridge.js 패턴): callHandler('GET_APP_FOREGROUND_SEC') 래퍼, 정규화 {status,foregroundSec}, 부재 시 BRIDGE_UNAVAILABLE/null.
 2) TbmEntryView.vue 종료 흐름(exit 직전) 1회 pull → exit payload에 appForegroundSec 포함. 실패 시 생략(BE NULL). (script는 developer)
파일: 신규 foregroundBridge.js, TbmEntryView.vue
선행: 09

### prafta-051-11 — 웹: 대리입실 + 일용직 검색입실 EP(신규) [backend, tbm02]
핵심:
 1) GET /webApi/tbm02/entry-candidates — sessionCd, userTypeCd(REGULAR/DAILY), keyword.
    REGULAR: 세션 사업장 정규직 TB_USER(alreadyEntered 포함). DAILY: 만료 안 된 일용직만 TB_DAILY_USER(USE_YN='Y' AND ACCOUNT_STATUS in 유효 AND WORK_EXPIRE_DATE>=오늘 AND WITHDRAWAL_DATE IS NULL). C7 확인.
    스코프: 회사전체 아니면 자기 사업장 세션만, 세션 사업장≠토큰 사업장 차단(IDOR).
 2) POST /webApi/tbm02/manager-enter — sessionCd, userTypeCd, userCd. OPENED만. INSERT ENTRY_TYPE_CD=MANAGER_DIRECT, ENTRY_BY_MANAGER_USER_CD=gvUserCd, ENTRY_AT=NOW(), GPS/DISTANCE NULL, 비번검증 없음, APP_FOREGROUND_SEC NULL. UNIQUE 충돌=이미입실. verifyManageAuth+세션 사업장 스코프+대상 사용자 세션 사업장 소속 검증.
 3) 정규직/일용직 동일 메커니즘, USER_TYPE_CD만 분기.
파일: Tbm02Controller(2 EP), service/impl, 신규 DTO, Tbm02Mapper.xml(검색2+enter INSERT), TbmErrorCode
EP: GET /webApi/tbm02/entry-candidates, POST /webApi/tbm02/manager-enter
연결 UI: UI-051-02
선행: 01, 03

### prafta-051-12 — 웹: 입실자 명단+GPS 조회 + 내보내기(eject) EP [backend, tbm02/tbm04]
핵심:
 1) 입실자 명단: tbm04 selectSessionAttendances 재사용(거리/입실유형). OPENED에서 호출 가능하도록 권한/상태 확인(필요 시 tbm02 전용 조회).
 2) POST /webApi/tbm02/eject-attendance — sessionCd, attendanceCd(+사유). OPENED만(C8). soft delete DEL_YN='Y'(C9, 감사보존·재입실 UNIQUE 처리). verifyManageAuth+스코프+attendance 세션 소속 검증.
EP: GET /webApi/tbm02/session-attendances(또는 tbm04 재사용), POST /webApi/tbm02/eject-attendance
연결 UI: UI-051-03
선행: 03, 11

### prafta-051-13 — 웹FE: Tbm_02 상태별 콘솔 + 타이머 [frontend-screen, tbm]
(UI-051-01 TbmSessionConsole.vue) 상태별 액션바: DRAFT[교육준비 시작]·OPENED[입실비번/재발급/연장/시작/대리입실/입실자내보내기/카운트다운]·IN_PROGRESS[종료]·COMPLETED[종료비번/참가자미이수]·CANCELLED[사유]. GPS는 navigator.geolocation 재사용. 카운트다운 표시전용. CSS변수/scoped/공통 재사용.
선행: 03/04/05/11/12

### prafta-051-14 — 웹FE: 대리/일용직 검색 입실 팝업(신규) [frontend-screen, tbm]
(UI-051-02 TbmManagerEntryPop.vue) 탭(정규직/일용직)+검색폼+그리드(이름/아이디/사업장/기입실여부)+[입실 처리]. 기입실 disabled.
선행: 11

### prafta-051-15 — 웹FE: 입실자 GPS 패널 + 내보내기(신규) [frontend-screen, tbm]
(UI-051-03 TbmEntryGpsPanel.vue) 명단 그리드(이름/입실시각/거리/반경초과배지/입실유형)+[내보내기](ReasonInputModal).
선행: 12

### prafta-051-16 — 웹FE: 종료 후 참가자 명단+미이수+앱실행시간 [frontend-screen, tbm]
(UI-051-04 기존 TbmAttendanceDetail/TbmCompletionModal 확장) 컬럼 추가: 입실거리, 앱실행시간(appForegroundSec→MM:SS 또는 '-'). 미이수/이수는 update-completion 재사용.
선행: 05, 08

## 5. 의존성 그래프
01 → 02 → 03 → {04→06, 05, 07, 11→12}; 01→08; 09→10→08; FE 13←03/04/05/11/12 · 14←11 · 15←12 · 16←05/08

## 6. 메인세션 확인 필요 (확정 8개 외)
- C1: 대리/일용직 입실 ENTRY_TYPE_CD — 신규 MANAGER_DIRECT vs MANAGER_QR_SCAN 재활용? (잠정 신규)
- C2: 개설 저장 결과 상태 — DRAFT 고정 vs OPENED 직행 허용? (잠정 DRAFT 고정 + prepare 전이)
- C3: OPENED 세션에 prepare 재호출 멱등 vs 거부?
- C4: OPENED→곧바로 교육종료 가능? (잠정 IN_PROGRESS만)
- C5: 자동시작 스케줄러 게이트 기본 false vs 즉시 ON? (잠정 false)
- C6: 앱 exit 허용 상태 IN_PROGRESS만 vs +COMPLETED? (잠정 둘 다)
- C7: 일용직 만료판정 ACCOUNT_STATUS 유효값 집합(SYS013)? (잠정 '01' + WITHDRAWAL_DATE IS NULL + WORK_EXPIRE_DATE>=오늘)
- C8: 내보내기 허용 상태 OPENED만 vs IN_PROGRESS도? (잠정 OPENED만)
- C9: 내보내기 soft delete vs 물리삭제? (잠정 soft delete)
- C10: "휴대폰 사용 불가" 정규직 식별 컬럼 존재 여부? 없으면 전체 정규직 검색 허용(관리자 판단)

## 7. 비고
- 마이그(01) 작성만, 운영 미적용. 선적용 필수: PREP_START_AT/APP_FOREGROUND_SEC 컬럼 + SYS046 라벨 DML(+C1 확정 시 SYS051).
- 보안: manager-enter/eject/entry-candidates cross-site IDOR 3중 정합, 일용직 PII(MBL_NO_LAST4) 노출범위, 대리입실 권한(관리자만).
- QA: 연장↔자동전이 경합, 비번재발급 기입실자 무영향, exit foregroundSec 방어, 일용직 만료 경계(=오늘).

## 8. 산출물 — Vue 골격 (planner 작성, src/views/tbm/popup/)
- TbmSessionConsole.vue (UI-051-01) — 상태별 콘솔/타이머
- TbmManagerEntryPop.vue (UI-051-02) — 대리/일용직 검색 입실
- TbmEntryGpsPanel.vue (UI-051-03) — 입실자 GPS/내보내기
