# prafta-app-002 작업 분해 — 사용자 본인 근태 조회 화면 (모바일 앱)

> 작성: planner 에이전트
> 작성일: 2026-05-29
> 요청서: `.claude/requests/app_requests/prafta-app-002.md`
> 참조 시안: `refs/prafta-app-002/prafta-request-my-attendance.md` + `prafta_my_attendance_v8.html` (10 케이스)
> 구현 대상: **모바일 앱 프론트** `PRAFTA/prafta-app-frontend/prafta-app-frontend/` (Vite 루트, 이중 중첩)
>            + **백엔드** `PRAFTA/prafta-backend/` (자체 개발 — 시안 §5 "외부 개발자" 표기는 무시)
> Notion 등록은 메인 세션이 담당. 본 문서가 단일 출처.

---

## 0. 핵심 사전 결론 (반드시 먼저 읽을 것)

### 0-1. 정책 충돌: 초과근무 사후 상신 기한 (시안 §3.2 → 폐기됨)

시안 §3.2와 attd §9.3.1은 "사후 상신 = 근무일 + 5일(D+5)"로 적었으나, **요청승인관리 재기획서 §3.2 (단일 출처)** 에서 이를 폐기하고 **"사업장별 근태 마감 전까지"** 로 재정의했다.

- 정책 우선순위(README): 요청승인관리 재기획서 > 근태관리 정책서 → **재기획서가 우선**.
- 따라서 본 화면의 초과근무 신청 활성/비활성 판정은 **"D+5 경과"가 아니라 "해당 일자의 근태 마감 여부"** 로 한다.
- 시안에 "D-N영업일" 잔여 배지가 있었다면 그 의미는 "근태 마감까지 남은 영업일"로 해석한다. (시안 v8 HTML에는 해당 배지 미존재 — 무시)

### 0-2. 스키마 갭: "출퇴근 사업장 다름" 케이스 표현 불가

`tb_user_attd_mgmt`에는 레코드당 단일 `SITE_CD`만 있고, 출근지/퇴근지를 분리 저장하는 컬럼이 없다. 시안 화면 3(출퇴근 사업장 다름)의 `checkInSiteName` ≠ `checkOutSiteName` 비교를 **현재 스키마로는 산출할 수 없다.** → §4의 "스키마 미확인/갭" 참조. 백엔드 작업 착수 전 결정 필요.

### 0-3. 스키마 갭: 스케줄 시간은 work_plan이 아니라 sch_mgmt에 있음

시안 §2.1은 "스케줄 = tb_user_work_plan + 근무코드 시간 정의"라 했는데 정확하다. `tb_user_work_plan.WORK_PLAN_CD`는 SCH_CD 또는 LEAVE_CD를 가리키는 코드일 뿐이며, **실제 시작/종료/휴게 시각은 `tb_sch_mgmt`(FST_SCH_*, SEC_SCH_*)** 에 있다. 2구간 여부는 `SEC_SCH_STR_TIME IS NOT NULL`로 판정한다.

---

## 1. 작업 단위 목록 (총 10건)

ID 채번은 메인 세션이 Notion "작업 로그" 최대 ID 조회 후 확정한다. 본 문서는 `APP002-{n}` 로컬 ID로 표기한다.

| 로컬 ID | 유형 | 영역 | 모듈 | 산출물 | 선행 |
| --- | --- | --- | --- | --- | --- |
| APP002-01 | backend | app | attd | GET /api/app/attd/my/today | 없음 |
| APP002-02 | backend | app | attd | GET /api/app/attd/my/week | 없음 |
| APP002-03 | backend | app | attd | GET /api/app/attd/my/month | 없음 |
| APP002-04 | backend | app | attd | GET /api/app/attd/my/day-detail | 01 (응답 동일 구조 재사용) |
| APP002-05 | frontend-screen | app | attd | MyAttendanceView.vue (컨테이너+세그먼트 탭) | 01~04 |
| APP002-06 | frontend-component | app | attd | AttendanceTodayCard.vue | 05 |
| APP002-07 | frontend-component | app | attd | AttendanceWeekList.vue | 05 |
| APP002-08 | frontend-component | app | attd | AttendanceMonthCalendar.vue | 05 |
| APP002-09 | frontend-component | app | attd | AttendanceDayDetailCard.vue | 05 |
| APP002-10 | frontend-component | app | attd | AttendanceActionSheet.vue | 05, 07 |

선행 관계:
- 프론트 화면(05~10)은 백엔드 응답 계약(01~04)에 의존하나, 본 planner 라운드에서 계약을 §3에 확정했으므로 골격 작성은 mock으로 즉시 가능하다. developer가 실 호출만 연결.
- 09(일 상세 카드)는 06(오늘 카드)과 구조 공용 — 06을 부모 데이터로 받고, 09는 06과 동일 템플릿을 재사용하도록 설계(시안 화면 9/10의 하단 카드 = 화면 1~5 상단 카드와 동일).

---

## 2. 각 작업 단위 상세 (정책서 출처 명시)

### APP002-01 — GET /api/app/attd/my/today  [backend]
- 요구사항: 로그인 사용자의 오늘 근태 1건(스케줄/근태/표준화 3행, 1·2구간) + 액션 활성도 반환.
- 정책서 출처:
  - 출퇴근 기본/구간 → attd `07-checkin-checkout.md` §7.1, `05-checkin-limits.md` §5.1~5.4
  - GPS 정상/외부/미확인 표시 → attd §7.2, §7.3 (사업장 위치만 표시, 미확인 시 "GPS 확인필요")
  - 정규 계산·표준화 → attd `10-attendance-calc.md` §10.1, §10.2
  - 퇴근 미등록(당일 내) 보정 대상 → attd `11-attendance-correction.md` §11.1
- 영향 파일(예상): `com.prafta.app.attd.attd0X.*` (Controller/Service/Mapper) + `tb_user_work_plan`, `tb_sch_mgmt`, `tb_user_attd_mgmt`, `tb_attd_std_time_rule`, `tb_site` 조회 Mapper.xml
- IDOR 가드: JWT의 USER_CD/CMPNY_CD/SITE_CD로만 본인 데이터 조회. 파라미터로 USER_CD 받지 않음.

### APP002-02 — GET /api/app/attd/my/week  [backend]
- 요구사항: weekStartYmd 기준 7일 요약(스케줄/근태 1줄 요약 + 일별 상태 + 액션 4종 활성도) + 주 합계(예정/실 근로시간, 실=완료분만).
- 정책서 출처:
  - 합계 산정(완료된 근무만) → attd §10.1, §10.4 (자동 휴게 공제), 시안 §3.7
  - 바텀시트 4액션 활성/비활성 매트릭스 → §2 "§3.3 매트릭스 정책 출처 결론" 참조
  - 휴무/공휴일/연차 배지 → attd `08-leave.md` §8.2, `tb_holiday`, 시안 §3.4
- IDOR 가드: 동일.

### APP002-03 — GET /api/app/attd/my/month  [backend]
- 요구사항: yearMonth 기준 각 일자의 dayType(WORK/LEAVE/OFF/ACTION_REQUIRED) + hasIssue + 월 합계.
- 정책서 출처:
  - dayType 분류: 근무=스케줄 존재(`tb_user_work_plan` SCH_CD), 연차=`tb_user_leave_use`(LEAVE_STATUS='CONFIRMED'), 휴무=주말/`tb_holiday`/스케줄 없음, 처리필요=마감 차단 사유 → attd `13-attendance-close.md` §13.3
  - 마감 차단 사유 = 처리 필요 셀의 근거(미결 요청/GPS 미확인/미승인 초과근무/퇴근 미등록) → attd §13.3, §11.1
- IDOR 가드: 동일.

### APP002-04 — GET /api/app/attd/my/day-detail  [backend]
- 요구사항: workYmd 파라미터로 임의 일자 상세(응답 구조 = APP002-01과 동일).
- 정책서 출처: APP002-01과 동일.
- IDOR 가드: workYmd만 받고 USER_CD는 JWT. 과거/미래 일자도 본인 것만.

### APP002-05 — MyAttendanceView.vue  [frontend-screen]
- 요구사항: 헤더(뒤로/내 근태/알림) + 3등분 세그먼트(오늘/이번주/이번달) + 본문 분기 + 하단 탭바(근태 활성). 디자인 토큰을 루트(.my-attd-view)에 1회 선언.
- 정책서 출처: 시안 §4.1 공통 UI. (비즈니스 룰 없음 — 레이아웃 컨테이너)
- 연결 UI 명세: UI(prafta-app-002-ui-spec.md) 전체.
- 참조 패턴: `views/main/MainView.vue` (토큰 루트 선언 + 카드 조립 + 탭바 + dev case picker), `HomeTabBar.vue`.
- 진입: `MainView.vue` `onAttdSummaryDetail()` TODO + 하단 탭바 'attd' → router.push (developer가 라우트 신설; 본 라운드는 emit/TODO).

### APP002-06 — AttendanceTodayCard.vue  [frontend-component]
- 요구사항: 오늘 카드. 5변형(근무중/퇴근완료/사업장다름/2구간/퇴근미등록) 통합. 3행 정보 + 인라인 알림 + 푸터 2버튼.
- 정책서 출처: attd §7.1~7.3, §10.1~10.2, §11.1, 시안 §3.1·§3.5·§3.6·§4.2.
- 참조 패턴: `AttendanceCard.vue`(badge/btn/HHMM 포맷), 시안 `.cd/.tr/.tw/.al/.ft` 구조.

### APP002-07 — AttendanceWeekList.vue  [frontend-component]
- 요구사항: 주 네비 + 7일 카드(요일/날짜/배지/시간요약/chevron) + 주 합계 카드. 카드 탭 시 부모에 day 선택 emit(→ ActionSheet).
- 정책서 출처: 시안 §3.7·§4.3, attd §10.1.
- 참조 패턴: 시안 `.dc/.dp/.db/.ds/.ws` 구조.

### APP002-08 — AttendanceMonthCalendar.vue  [frontend-component]
- 요구사항: 월 네비 + 월 합계 + 범례 4종 + 7열 캘린더(셀 색상코딩 wk/lv/of/ac, out/td/sel). 셀 선택 emit.
- 정책서 출처: 시안 §2.3·§4.4, attd §13.3(처리 필요 근거), §8.2(연차).
- 참조 패턴: 시안 `.cal/.cal-d/.lgd/.mn` 구조.

### APP002-09 — AttendanceDayDetailCard.vue  [frontend-component]
- 요구사항: 선택일 상세 카드(오늘/이번달 공용). 06과 동일 3행 구조 + 처리필요 시 하단 빠른 액션 2버튼(근태 보정/초과근무 신청, transparent).
- 정책서 출처: APP002-06과 동일 + §3.3 매트릭스(빠른 액션 활성도).
- 참조 패턴: 06과 공용 — 06을 그대로 props 기반 재사용 가능하도록 09 설계(중복 최소화). 시안 화면 9·10 하단 카드.

### APP002-10 — AttendanceActionSheet.vue  [frontend-component]
- 요구사항: 바텀시트. 핸들+헤더(날짜+X)+메타 1줄 + 액션 4종(스케줄 수정/근태 보정/초과근무 신청/연차 신청) 항상 노출, 활성/비활성만 제어. 안내문구 없음.
- 정책서 출처: §3.3 매트릭스 정책 출처 결론(아래) + attd §9.2/§11/§9.3/§9.4.
- 참조 패턴: 시안 `.bs/.sh/.sa` 구조. 모달 오버레이는 `components/modal/` 패턴 참조하되 바텀시트는 신규.

---

## 3. 백엔드 API 계약 (실 스키마 기준 정밀화)

> 시간 포맷: HHMM(4자리 문자열) — `tb_user_attd_mgmt.CHECK_IN_TIME`, `tb_sch_mgmt.FST_SCH_STR_TIME` 등 실제 컬럼 타입과 일치(varchar(4)).
> 일자 포맷: YYYYMMDD(8자리 문자열) — `WORK_YMD` 등과 일치(varchar(8)). 단 `tb_holiday.HOLIDAY_YMD`만 `date` 타입 → 서버에서 YYYYMMDD 문자열로 변환.
> 분(minutes)은 서버 계산값(int). 클라이언트는 "Nh Nm" 포맷만 담당.

### 3.1 GET /api/app/attd/my/today  (APP002-01)

응답 필드 ↔ 스키마 매핑:

| 응답 필드 | 출처 컬럼 / 산출 | 비고 |
| --- | --- | --- |
| workDate | 서버 today (YYYYMMDD) | |
| siteName | `tb_site.SITE_NM` (JWT SITE_CD 조인) | |
| workPlanCode | `tb_user_work_plan.WORK_PLAN_CD` | SCH_CD 또는 LEAVE_CD |
| workPlanName | `tb_sch_mgmt.SCH_NO`+SCH 라벨 / `tb_leave_type_mgmt.LEAVE_NM` | ⚠️ "ST001 정규근무" 표기 출처 미확정 (§4) |
| workStatus | 산출: WORKING/BEFORE_WORK/TWO_SLOT_WORKING/CHECKED_OUT/CHECK_OUT_MISSING | 시안 §2.2 + attd §7 |
| isTwoSlot | `tb_sch_mgmt.SEC_SCH_STR_TIME IS NOT NULL` | |
| slots[].workSeq | `tb_user_attd_mgmt.WORK_SEQ` (없으면 스케줄 구간 순번) | 1=1구간, 2=2구간 |
| slots[].schedule.startTime | 1구간 `FST_SCH_STR_TIME` / 2구간 `SEC_SCH_STR_TIME` | HHMM |
| slots[].schedule.endTime | `FST_SCH_END_TIME` / `SEC_SCH_END_TIME` | HHMM |
| slots[].schedule.breakMinutes | `FST_SCH_BRK_MIN` / `SEC_SCH_BRK_MIN` (문자→int) | |
| slots[].schedule.workMinutes | 산출: (종료-시작-휴게) | attd §10.1, §10.4 |
| slots[].attendance.checkInDate/Time | `tb_user_attd_mgmt.CHECK_IN_DATE/CHECK_IN_TIME` | |
| slots[].attendance.checkInSiteName | ⚠️ `tb_user_attd_mgmt.SITE_CD` → SITE_NM (출근지=레코드 사업장) | §4 갭: 퇴근지 분리 불가 |
| slots[].attendance.checkInGpsValid | ⚠️ 스키마 미확인 (§4) — IS_MOCKED/isOutsideYn 컬럼 부재 | attd §7.3 |
| slots[].attendance.checkOutDate/Time | `CHECK_OUT_DATE/CHECK_OUT_TIME` (NULL 허용) | |
| slots[].attendance.checkOutSiteName | ⚠️ 스키마 미확인 (§4) — 퇴근지 분리 컬럼 없음 | |
| slots[].attendance.isMissingCheckOut | 산출: CHECK_IN 존재 && CHECK_OUT NULL && (스케줄 종료 경과) | 시안 §3.1, attd §11.1 |
| slots[].attendance.isDifferentSite | ⚠️ 스키마상 산출 불가 (§4) | 결정 필요 |
| slots[].standardized.startTime/endTime/settledMinutes | 산출: `tb_attd_std_time_rule` 적용 결과 | attd §10.2. 퇴근전=null |
| actions.canRequestModify | 산출: §3.1 매트릭스 | 아래 |
| actions.canCheckOut | 산출: §3.1 매트릭스 + attd §7 | |
| actions.canCheckIn | 산출: 2구간 재출근 가능 여부 — attd §5.1·§5.2 | |

§3.1 수정요청/퇴근 활성 판정 (정책 출처 결론):
- "근무중 → 수정요청 비활성, 퇴근 활성": 근태 미확정 상태 → 보정 요청 대상 아님(attd §11.1은 확정 후 보정). 퇴근 가능은 attd §7.1.
- "퇴근완료 → 수정요청 활성, 퇴근 비활성": 확정된 근태에 대한 보정 요청(attd §11.1·§11.2), 재퇴근 불가(attd §5.1).
- "2구간 진행 중 → 수정요청 비활성, 2구간 출근/퇴근 활성": attd §5.1·§5.2(재출근은 이전 퇴근 후), 보정은 전 구간 완료 후.
- "2구간 완료 → 수정요청 활성, 퇴근 비활성": 위와 동일 논리.
- "퇴근 미등록(당일 내) → 수정요청 비활성, 퇴근 활성": attd §11.1(보정 대상이나 당일은 자가 퇴근 우선). **단 "퇴근 가능 기한=당일 자정까지"는 정책서 미명시 → §5 정책 미확정.**

### 3.2 GET /api/app/attd/my/week  (APP002-02)

요청: `weekStartYmd` (YYYYMMDD). 응답 days[7] + summary.

| 응답 필드 | 출처 / 산출 | 비고 |
| --- | --- | --- |
| days[].workYmd | 반복 일자 | |
| days[].dayOfWeek | 산출(MON~SUN) | |
| days[].isToday | 서버 today 비교 | |
| days[].isHoliday / holidayName | `tb_holiday.HOLIDAY_YMD/HOLIDAY_NM` | 시안 §3.4 |
| days[].isLeaveUsed / leaveTypeName | `tb_user_leave_use`(CONFIRMED) → `tb_leave_type_mgmt.LEAVE_NM` | attd §8.2 |
| days[].workPlanCode/Name | work_plan + sch_mgmt | ⚠️ 라벨 표기 §4 |
| days[].isTwoSlot | sch_mgmt SEC_* | |
| days[].scheduleSummary | 산출 "0930~1800" or "0700~1300 / 1700~2100" | 시안 §4.3.2 |
| days[].attendanceSummary | 산출(미래/미생성=null) | |
| days[].attendanceStatus | NORMAL/LATE/EARLY_LEAVE/MISSING/WORKING/NOT_STARTED | attd §10.1 |
| days[].actions.canRequestScheduleModify | §3.3 매트릭스 | 아래 |
| days[].actions.canRequestAttendanceCorrection | §3.3 | |
| days[].actions.canRequestOvertime | §3.3 (마감 전까지) | 0-1 결론 |
| days[].actions.canRequestLeave | §3.3 | |
| summary.plannedWorkMinutes | 산출 합계 | 시안 §3.7 |
| summary.actualWorkMinutes | 산출(완료 근무만) | 시안 §3.7 |

§3.3 바텀시트 4액션 매트릭스 (정책 출처 결론):
- 스케줄 수정: attd §9.2 — 조건 "해당 일자 스케줄 존재 + 스케줄 마감 전". → 미래/미래휴무(스케줄 있음)만 활성. 과거/오늘 비활성.
- 근태 보정: attd §11 — 확정된 과거/오늘(완료) 근태 대상. 미래/마감후 비활성.
- 초과근무 신청: attd §9.3 + **재기획서 §3.2(근태 마감 전까지)** — 과거/오늘(완료) 활성, 단 출퇴근 누락 시 "보정 후 활성", 마감 후 비활성.
- 연차 신청: attd §9.4 — 조건 "해당 일자 스케줄 존재 + 잔여 연차 + 마감 전". 미래 활성. **휴무일 연차 가능 여부는 §5 정책 미확정**(시안은 미래휴무 비활성으로 그림 → 그 가정 채택하되 §5 표시).
- 모든 활성도는 **서버가 산출**해 내려준다(클라이언트는 표시만). 마감 여부는 attd §13.

### 3.3 GET /api/app/attd/my/month  (APP002-03)

요청: `yearMonth` (YYYYMM). 응답 monthlySummary + days[].

| 응답 필드 | 출처 / 산출 | 비고 |
| --- | --- | --- |
| monthlySummary.plannedWorkMinutes | 산출 | |
| monthlySummary.actualWorkMinutes | 산출(완료분) | 시안 "(완료분)" |
| days[].workYmd | 일자 | |
| days[].dayType | WORK/LEAVE/OFF/ACTION_REQUIRED | 0-1·§2.3 |
| days[].holidayName | `tb_holiday.HOLIDAY_NM` (있으면) | |
| days[].hasIssue | 산출: 마감 차단 사유 존재 | attd §13.3 |

dayType 우선순위(시안 §3.4 "연차 톤 우선"): ACTION_REQUIRED > LEAVE > WORK > OFF.
- ACTION_REQUIRED: 과거 근무일 중 마감 차단 사유 보유(퇴근 미등록/GPS 미확인/미승인 초과/미결 요청).
- LEAVE: `tb_user_leave_use` CONFIRMED(공휴일 겹쳐도 연차 우선 — 시안 §3.4).
- WORK: 스케줄(SCH_CD) 존재 평일.
- OFF: 주말/`tb_holiday`/스케줄 없음.

### 3.4 GET /api/app/attd/my/day-detail  (APP002-04)
- 요청: `workYmd` (YYYYMMDD). 응답 = 3.1과 동일 구조(오늘이 아닌 임의 일자).
- 과거/미래 모두 본인 데이터만. 미래 일자는 attendance 전체 null(스케줄만).

---

## 4. 스키마 미확인 / 갭 (developer/메인이 MCP로 보강·결정)

1. ⚠️ **퇴근지(checkOutSiteName) 분리 불가**: `tb_user_attd_mgmt`는 레코드당 단일 `SITE_CD`. 출근지≠퇴근지(시안 화면3)를 표현하려면 (a) 컬럼 추가(CHECK_OUT_SITE_CD) 또는 (b) 출근/퇴근을 별 WORK_SEQ 레코드로 분리하는 운영규칙 필요. **현재 스키마로는 isDifferentSite 항상 false.** → 백엔드 착수 전 결정 필요.
2. ⚠️ **GPS 유효/외부 플래그 컬럼 부재**: attd §7.2/§7.3는 "근무지 외"/"GPS 미확인" 태그를 말하나, `tb_user_attd_mgmt`에 isOutsideYn/IS_MOCKED 류 컬럼이 보이지 않음(`CHECK_IN_METHOD`[SYS031]만 존재). checkInGpsValid 산출 근거를 MCP로 확인 필요. (SYS031 코드값 의미도 확인 필요)
3. ⚠️ **근무코드 라벨 "ST001 정규근무" 표기 출처**: 시안의 "ST001"이 `tb_sch_mgmt.SCH_NO`인지 SCH_CD인지, "정규근무" 한글명이 어느 컬럼인지 미확정. `tb_sch_mgmt`에 SCH_NM 류 한글명 컬럼이 없음 → SYS019(SCH_TYPE) 코드라벨 + SCH_NO 조합인지 MCP 확인 필요.
4. ⚠️ **근태 마감 상태 조회 출처**: §3.2·§3.3의 "마감 전까지" 판정에 쓸 마감 테이블/플래그(prafta-028 "근태 월마감 부서확장"에서 도입된 마감 스코프) 컬럼명·조회 방식 미확인. prafta-028 마이그레이션 산출물 확인 필요.
5. ⚠️ **표준화 적용 결과 저장 위치**: `tb_attd_std_time_rule`은 회사 단위 룰(단위)만 보유. 일자별 표준화 시각/정산분이 어디 저장(또는 런타임 산출)되는지 미확인 — standardized.* 산출 경로 확인 필요.
6. ⚠️ **2구간 근태 레코드 구분**: 2구간은 `WORK_SEQ`로 구분(스키마 확인 완료)하나, 1구간 미퇴근 상태에서 2구간 레코드 생성 시점 운영규칙 확인 필요(attd §5.2).

---

## 5. 정책 미확정 (사용자 확인 필요 — 추측 금지)

1. **퇴근 가능 기한**: 시안 §3.1·화면5는 "퇴근은 오늘 안에만 가능"으로 가정. attd §7에 퇴근 마감 시각(당일 자정/D+1/다음 출근 직전) 명시 없음. → 사용자 확정 필요. (현재 골격은 시안 가정대로 "당일 내" 워딩 유지, 활성도는 서버 산출에 위임)
2. **휴무일 연차 신청 가능 여부**: 시안 §3.3은 "미래 휴무일=연차 비활성", 그러나 §3.4는 "공휴일에 연차 사용 가능(교대근무자)". 휴무(주말/평일휴무)와 공휴일을 구분해 연차 허용 범위가 갈림. attd §9.4 조건은 "스케줄 존재"인데 휴무는 스케줄 없음 → 원칙상 불가. **공휴일+교대(스케줄 있음)는 가능**으로 해석 가능하나 명시 없음. → 사용자 확정 필요.
3. **"스케줄 마감 전/후" 기준 시각**: §3.3 미래 스케줄 수정 활성도가 스케줄 마감(attd §12)에 묶임. 마감은 "조직+월 단위" 수동 실행 — 일자 단위 시각 기준 아님. 시안의 "미래(스케줄 마감 전/후)" 구분을 월 마감 상태로 매핑하는 게 맞는지 확정 필요.

---

## 6. 우선순위 근거

- attd는 법적 책임 영역(+1단계 격상). 그러나 본 작업은 **읽기 전용 조회 화면**(출퇴근 등록/결재는 별도 작업의 TODO) → 데이터 정합성 영향 낮음.
- 백엔드 API(01~04)가 프론트(05~10)의 선행. 단 계약 확정으로 mock 병행 가능.
- 영향 범위: 신규 화면/엔드포인트로 기존 코드 변경 최소(진입점 MainView 1곳).
