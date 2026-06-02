# prafta-app-001 — 구현 계약서 (이번 세션 범위)

> 범위: 앱 메인화면 **읽기 데이터 동기화**. 쓰기 액션/실시간 GPS는 prafta-app-003 으로 분리.
> 정책 확정(사용자): 공지=보류, TBM=읽기전용 포함, 출퇴근=표시만 포함(액션 분리), 연차=법정+약정 **합산**, 인사말날짜=클라로컬, 이니셜=앞2자.

## 1. 신규 백엔드 엔드포인트

`GET /appApi/home01/home-summary` (매핑: `com.prafta.app.home.home01`, `@RequestMapping("/home01")` → 자동 프리픽스 `/prafta/appApi/home01/home-summary`. **기존 `AppChkLst01Controller` 패턴을 1:1 따른다**. FE 호출 세그먼트는 컨트롤러 `@RequestMapping` 값(`home01`)과 정확히 일치시킨다 — chkLst01/risk01 관례.)

- 인증: `@RequestHeader("Authorization")` → `jwtUtil.getAllClaimsAsMap` → `TokenInfo`(cmpnyCd/siteCd/userCd 도출). **userCd 를 쿼리/바디로 받지 않는다.**
- 응답(camelCase, 모든 시각은 HHMM 문자열 또는 null):

```json
{
  "attendance": {
    "status": "BEFORE_WORK | WORKING | OFF_WORK",
    "scheduleStart": "0930",
    "scheduleEnd": "1800",
    "checkInTime": "0928",
    "checkOutTime": null,
    "isOffsite": false,
    "canCheckIn": false,
    "canCheckOut": true
  },
  "leave":    { "grantedDays": 20.0, "remainingDays": 12.0 },
  "approval": { "pendingCount": 3 },
  "tbm": {
    "hasToday": true,
    "sessionStatus": "OPENED | IN_PROGRESS | COMPLETED | NONE",
    "openedTime": "1000",
    "title": "오늘의 안전교육",
    "presenterName": "박과장",
    "myAttendanceStatus": "NOT_ENTERED | ENTERED | COMPLETED",
    "myEntryTime": "1008"
  }
}
```

## 2. 데이터 매핑 규칙 (운영 스키마 확정값 기준)

### attendance — `tb_user_attd_mgmt` 오늘(WORK_YMD=오늘, DEL_YN='N', 본인 USER_CD)
- 오늘 레코드 없음 → `status=BEFORE_WORK`, checkIn/Out=null
- CHECK_IN_TIME 있고 CHECK_OUT_TIME 없음 → `WORKING`
- CHECK_OUT_TIME 있음 → `OFF_WORK`
- `checkInTime`=CHECK_IN_TIME(varchar4 HHMM), `checkOutTime`=CHECK_OUT_TIME
- 다중 WORK_SEQ 가능 → 최신(가장 큰 WORK_SEQ 또는 미퇴근 레코드 우선) 1건 기준
- `isOffsite`: **이번 범위는 항상 false 고정**(실시간 GPS 는 app-003). 응답필드는 유지.
- `canCheckIn`/`canCheckOut`: **서버 산출**(클라 판정 금지). 이번 범위 최소 규칙 — canCheckIn = 오늘 미퇴근 진행 레코드 없음; canCheckOut = 출근했고 미퇴근. 정책 §5 상세 제한은 app-003 액션 구현 시 정밀화(주석으로 TODO 명시).

### 예정 스케줄 — `tb_user_work_plan`(오늘) → WORK_PLAN_CD → `tb_sch_mgmt`
- work_plan.WORK_PLAN_CD 가 SCH_CD 일 때만 스케줄. (LEAVE_CD 면 연차/휴무 → scheduleStart/End=null)
- `scheduleStart`=FST_SCH_STR_TIME, `scheduleEnd`=FST_SCH_END_TIME (1구간만; 2구간 SEC_* 는 이번 미표시, 주석 TODO).
- sch_mgmt 는 (CMPNY_CD,SITE_CD,SCH_CD) PK + APPLY_DATE/USE_YN — 오늘 적용·USE_YN='Y' 최신 1건.

### leave — `tb_user_leave_grant` (본인 USER_CD, DEL_YN='N')
- 집계 대상: `STATUS='ACTIVE' AND EXPIRE_YN='N'`. **법정+약정 전체 합산**(GRANT_TYPE 무관).
- `grantedDays` = SUM(GRANT_DAYS)
- `remainingDays` = SUM(GRANT_DAYS - USED_DAYS)
- 타입: GRANT_DAYS decimal(5,1), USED_DAYS decimal(8,5)(운영 스냅샷 decimal(5,1) — BigDecimal 처리라 무관). 응답은 소수 1자리 권장.

### approval.pendingCount — `tb_user_attd_req` (본인 USER_CD, DEL_YN='N')
- `REQ_STATUS='01'`(신청) 건수. (확정: SYS033 01=신청. 카드 주석의 'REQUESTED' 는 오류)

### tbm — `tb_tbm_session` + `tb_tbm_attendance` (본인, 오늘)
- "오늘 세션": 본인 사업장(SITE_CD) + 오늘(OPENED_AT date=오늘) + STATUS_CD IN ('OPENED','IN_PROGRESS','COMPLETED') 중 최신 1건. 없으면 `hasToday=false, sessionStatus=NONE`, 나머지 null.
- `sessionStatus`=STATUS_CD, `openedTime`=OPENED_AT 의 HHMM, `title`=TITLE
- `presenterName`: MANAGER_USER_CD → `TB_USER` 조인하여 이름. (이름은 PII 정책 §11.1 의 차단 대상 아님 — 전화/이메일만 차단. 단 security 검토 대상)
- 내 참석: `tb_tbm_attendance`(SESSION_CD=위 세션, USER_CD=본인, USER_TYPE_CD='REGULAR', DEL_YN='N')
  - 레코드 없음 → `myAttendanceStatus=NOT_ENTERED`, myEntryTime=null
  - ENTRY_AT 있고 EXIT_AT 없음 → `ENTERED`, `myEntryTime`=ENTRY_AT HHMM
  - COMPLETION_STATUS_CD='COMPLETED' 또는 EXIT_AT 있음 → `COMPLETED`
- **읽기전용**. 입실/종료 액션은 본 범위 밖(app-003).

## 3. 프론트엔드 (`prafta-app-frontend/src/views/main/MainView.vue` + 카드들)
- 헤더(세션 직결, API 불요): `siteName ← gv_siteNm`, 아바타 이니셜 ← `gv_userNm` 앞 2자(빈값 '?'), AttendanceCard 위치표시도 `gv_siteNm`.
- `onMounted` → `axios.get('/appApi/home/home-summary')` 호출하여 attendance/leave/approval/tbm 주입.
- 기존 mock 4케이스(`CASE1~4`) + dev 케이스 picker UI/스타일 **제거**.
- 인사말 날짜: 클라이언트 로컬 유지(변경 없음).
- 카드 컴포넌트(props) 구조는 유지. 단 TBM 카드의 "장소(location)" 는 소스 없음 → 표시 제거 또는 title 로 대체(카드 수정 최소화).
- 공지(NoticeListCard)/알림벨: DB 소스 없음 → 빈/0 또는 숨김 처리(주석으로 prafta-app-001 보류 명시).
- 출근/퇴근 버튼: 동작은 app-003. 현 alert 유지 또는 비활성.

## 4. 검증
- 빌드: 백엔드 `gradlew.bat`(--no-daemon), 앱 FE `vite build`(이중중첩 루트). 타임아웃 준수.
- 스키마 100% 일치, leading comma, `#{}` 바인딩, `SELECT *` 금지, 명시적 컬럼.
