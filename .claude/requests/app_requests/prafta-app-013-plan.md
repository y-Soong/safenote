# prafta-app-013 작업 분해 (planner)

요청서: `prafta-app-013.md` (내 근태 화면 — 오늘/이번달 탭에 4액션 시트 추가 + 4액션 게이팅 규칙 변경)
단일 출처(확정): `prafta-app-013-decisions.md` — 모든 규칙은 이 문서 고정. 임의 규칙 추가 금지.
영역: app (모바일 webview Vue + 백엔드 `prafta-backend`). Flutter 셸 변경 없음.

정책 출처(이번 작업 정독 대상):
- `attd/09-requests-approval.md` §9.2(스케줄 수정) / §9.3(초과근무) / §9.4(연차) — 본 작업으로 갱신 완료.
- `attd/11-attendance-correction.md` §11.2(신청 시점) — 본 작업으로 신설 완료.
- `request-approval/03-policy-alignment.md` §3.2 — 초과근무 D+5 폐기 → 마감 전까지(정합 근거, 변경 없음).
- 마감 판정: `attd/13-attendance-close.md` + prafta-028 월마감(부서단위). 본 작업은 기존 `isMonthClosed` 결과(`closed`)를 그대로 사용.

---

## 0. 설계 합의 (developer 필독)

### 0-1. 게이팅 규칙 (3탭 공통, 결정 §1·§2 그대로)

일자별 프리미티브:
- `isPast / isToday / isFuture` = 대상일 vs 서버 today
- `closed` = 해당 월 근태마감 (기존 `isMonthClosed` 산출)
- `hasSchedule` = 근무 스케줄(SCH_CD) 존재 (연차/휴무-only 제외). 기존 `hasSchCd` 와 동일.
- `hasAttendance` = **출근 기록 1건 이상 존재** (`attdBySeq.get(1) != null || attdBySeq.get(2) != null`). 퇴근/완료 무관. (Q2=a)
- `completed` = 전 구간 출퇴근 완료 (기존 정의 유지: 1구간이면 s1Out, 2구간이면 s1Out && s2Out)
- `isWorking` = `hasAttendance && !completed` (출근했으나 전 구간 미완료)

4액션 enabled 조건:

| 액션 플래그 | 신규 조건 |
| --- | --- |
| `canRequestScheduleModify` | `hasSchedule && !closed` |
| `canRequestAttendanceCorrection` | `!isFuture && !closed && !isWorking` |
| `canRequestOvertime` | `!isFuture && !closed && hasAttendance && !isWorking` |
| `canRequestLeave` | `!closed && !hasAttendance` |

공통: `closed`면 4액션 전부 비활성(미래/현재/과거 무관). 미래 차단은 보정·초과근무에만.

추가 힌트 플래그:
- `leaveFullDayOnly = !hasSchedule` (연차 신청 폼이 full-day 강제에 소비; 본 작업은 계약만 내림 — Follow-up F1)

### 0-2. DTO 설계 (단일 산출 함수로 통일)

- `WeekDayActionsResponse` 에 필드 2개 추가: `leaveFullDayOnly`(boolean). (4액션 + 힌트가 한 객체)
  - Lombok+Jackson boolean is-접두 함정 주의(메모리): `leaveFullDayOnly` 는 is 접두가 아니므로 직렬화 명칭 안전. 단 추가 시 4플래그가 이미 `canXxx`(is 아님)라 영향 없음.
- 4액션 산출 로직을 **공용 private 메서드 1개**로 추출: `computeActionFlags(hasSchedule, isPast, isToday, hasAttendance, completed, closed)` → `WeekDayActionsResponse` 반환.
  - `computeWeekActions` 는 이 공용 메서드를 호출하도록 교체(기존 시그니처 유지 또는 내부 위임).
  - `today / day-detail`(buildDayResponse) 도 이 공용 메서드를 호출하여 동일 4액션 산출.
- `MyAttendanceDayResponse` 에 4액션 객체를 담을 신규 필드 추가: `sheetActions`(타입 `WeekDayActionsResponse`).
  - 기존 `actions`(타입 `DayActionsResponse`: `canRequestModify/canCheckOut/canCheckIn`)는 **유지**(오늘 카드 primary 버튼·하위호환). 손대지 않는다.
  - 프론트는 시트 구동에 `sheetActions` 를, primary/퇴근 버튼에 `actions` 를 쓴다.
- `MonthDayResponse` 는 이미 `actions`(WeekDayActionsResponse) 보유 여부 확인 필요. 이번달 셀 탭 시트는 day-detail 응답의 `sheetActions` 를 쓰므로 month 응답 자체는 변경 불필요(developer 확인).

### 0-3. 프론트 데이터 흐름

- 시트 컴포넌트 `AttendanceActionSheet` 는 이미 `day.actions`(4플래그)로 구동 — **컴포넌트 변경 없음**.
- 오늘 탭: `AttendanceTodayCard` "수정 요청" 버튼 → emit `requestModify` → `MyAttendanceView.onTodayAction` 에서 시트용 day 객체 `{ workYmd: todayDetail.workDate, actions: todayDetail.sheetActions, workPlanName, scheduleSummary, attendanceSummary, leaveTypeName }` 구성 후 `actionSheetDay/actionSheetOpen` 세팅.
- 이번달 탭: `AttendanceDayDetailCard` 하단 2버튼 제거. 본체 `AttendanceTodayCard` 의 "수정 요청" 버튼 → emit `requestModify` → `MyAttendanceView.onDayDetailAction` 에서 `dayDetail` 기준 시트용 day 객체 구성 후 시트 오픈.
- "수정 요청" 버튼은 **항상 enable**(서버 `canRequestModify` 의존 끊음). 시트 4행이 개별 게이팅.
- 시트 액션 라우팅은 기존 `onSheetAction` 재사용. `leave` 는 기존 stub("준비 중") 유지.

---

## 작업 분해 결과

### prafta-app-013-1 [backend] 4액션 산출 규칙 교체 + 공용화

- **유형**: backend
- **영역**: app
- **모듈**: app/attd/attd01
- **작업 유형**: 보완
- **요구사항 요약**: 이번주 4액션 규칙을 결정 §2로 교체하고, 산출 로직을 공용 메서드로 추출해 오늘/이번주/이번달이 동일 규칙을 쓰게 한다.
- **상세 설명**:
  - 정책 출처: `attd/09-requests-approval.md` §9.2·§9.3·§9.4, `attd/11-attendance-correction.md` §11.2, `request-approval/03-policy-alignment.md` §3.2.
  - 핵심 요구사항:
    1) 공용 산출 메서드 신설 `computeActionFlags(hasSchedule, isPast, isToday, hasAttendance, completed, closed)` → `WeekDayActionsResponse`. 규칙은 §0-1 표 그대로.
    2) `computeWeekActions` 를 ①로 위임(또는 내부 호출)하도록 교체. 이때 `hasAttendance`(s1 또는 s2 출근기록 존재)·`completed` 를 산출해 전달. `isWorking = hasAttendance && !completed`.
    3) `WeekDayActionsResponse` 에 `leaveFullDayOnly`(boolean) 필드 추가. 산출값 = `!hasSchedule`.
    4) `leaveFullDayOnly` 외 4플래그 명칭/JSON 계약 유지.
  - 영향 받는 파일:
    - `prafta-backend/.../app/attd/attd01/service/impl/AppAttd01ServiceImpl.java` (computeWeekActions ~L530, 신규 computeActionFlags)
    - `prafta-backend/.../app/attd/attd01/dto/response/WeekDayActionsResponse.java` (leaveFullDayOnly 추가)
  - 영향 받는 endpoint: GET /appApi/attd/my/week (응답 days[].actions 규칙 변경)
  - 예상 산출물: service(메서드 추출/교체), dto(필드 1개 추가)
- **선행 작업**: 없음
- **우선순위 근거**: 게이팅 규칙의 단일 출처. 다른 작업이 이 공용 메서드에 의존. 법적 책임 영역(attd) +1 격상.

### prafta-app-013-2 [backend] 오늘/일상세 4액션 산출 추가 (시트 연동용)

- **유형**: backend
- **영역**: app
- **모듈**: app/attd/attd01
- **작업 유형**: 보완
- **요구사항 요약**: today/day-detail 응답에도 동일 4액션(+leaveFullDayOnly)을 산출해 내려 시트가 오늘·이번달에서 동작하게 한다.
- **상세 설명**:
  - 정책 출처: prafta-app-013-1 과 동일.
  - 핵심 요구사항:
    1) `MyAttendanceDayResponse` 에 신규 필드 `sheetActions`(타입 `WeekDayActionsResponse`) 추가. 기존 `actions`(DayActionsResponse: canRequestModify/canCheckOut/canCheckIn)는 **그대로 유지**.
    2) `buildDayResponse`(~L142)에서 013-1의 공용 `computeActionFlags` 를 호출해 `sheetActions` 산출 후 빌더에 세팅. `hasAttendance`/`completed`/`hasSchedule`/`isPast`/`isToday`/`closed` 는 이미 해당 메서드에 존재(또는 attdBySeq 로 즉시 산출).
    3) `computeDayActions`(canRequestModify/canCheckOut/canCheckIn)는 변경하지 않는다(primary 버튼·하위호환).
  - 영향 받는 파일:
    - `prafta-backend/.../app/attd/attd01/service/impl/AppAttd01ServiceImpl.java` (buildDayResponse)
    - `prafta-backend/.../app/attd/attd01/dto/response/MyAttendanceDayResponse.java` (sheetActions 필드 추가)
  - 영향 받는 endpoint: GET /appApi/attd/my/today, GET /appApi/attd/my/day-detail (응답에 sheetActions 추가 — 가산적 변경, 하위호환)
  - 예상 산출물: service(빌더 보완), dto(필드 1개 추가)
  - 보안: 신규 노출 없음. my-* 조회는 본인 세션 USER_CD 기준(기존 IDOR 가드 유지). security 재검토 대상.
- **선행 작업**: prafta-app-013-1 (공용 메서드 의존)
- **우선순위 근거**: 프론트 시트 연동의 전제. 백엔드 API 우선. attd +1 격상.

### prafta-app-013-3 [frontend-screen] 오늘 탭 — "수정 요청" → 4액션 시트 연결

- **유형**: frontend-screen
- **영역**: app
- **모듈**: app/attd
- **작업 유형**: 보완
- **요구사항 요약**: 오늘 탭의 "수정 요청" 버튼을 누르면 이번주 날짜 클릭과 동일한 4액션 슬라이드 시트가 열리도록 연결한다.
- **상세 설명**:
  - 정책 출처: §9.2·§9.3·§9.4·§11.2 (게이팅은 BE 산출 표시만).
  - 핵심 요구사항:
    1) `AttendanceTodayCard` "수정 요청" 버튼: `canModify` 의존을 끊고 **항상 enable**(누르면 시트 오픈). 단 컴포넌트 자체는 emit `requestModify` 만 — 활성/라우팅은 컨테이너 책임. (골격: 버튼 disabled 바인딩 제거)
    2) `MyAttendanceView.onTodayAction`: `type === 'requestModify'` 분기에서 시트용 day 객체 구성(`workYmd = todayDetail.workDate`, `actions = todayDetail.sheetActions`, 메타: workPlanName/scheduleSummary/attendanceSummary/leaveTypeName) 후 `actionSheetDay`/`actionSheetOpen` 세팅. (developer: script 채움)
  - 영향 받는 파일:
    - `prafta-app-frontend/prafta-app-frontend/src/views/attd/components/AttendanceTodayCard.vue` (template: "수정 요청" 버튼 항상 활성 — 골격)
    - `prafta-app-frontend/prafta-app-frontend/src/views/attd/MyAttendanceView.vue` (script: onTodayAction 시트 라우팅 — developer)
  - 영향 받는 endpoint: GET /appApi/attd/my/today (sheetActions 소비)
  - 연결 UI 명세: UI-A006(기존 시트, 재사용) — 신규 화면 명세 없음(기존 컴포넌트 보완).
- **선행 작업**: prafta-app-013-2
- **우선순위 근거**: API 산출 후 화면. attd +1 격상.

### prafta-app-013-4 [frontend-screen] 이번달 일자상세 — 2버튼 제거 + "수정 요청" → 시트 통일

- **유형**: frontend-screen
- **영역**: app
- **모듈**: app/attd
- **작업 유형**: 보완
- **요구사항 요약**: 이번달 일자상세 하단의 "근태 보정/초과근무" 2버튼을 제거하고, 본체 "수정 요청" 버튼이 4액션 시트를 열도록 통일한다.
- **상세 설명**:
  - 정책 출처: §9.2·§9.3·§9.4·§11.2.
  - 핵심 요구사항:
    1) `AttendanceDayDetailCard`: 하단 `quick-ft` 2버튼 블록(근태 보정/초과근무)과 관련 script(`isActionRequired`/`canCorrect`/`canOvertime`/`onQuickAction`)·sprite·style(.quick-ft/.bt*) 제거. 본체 `AttendanceTodayCard` 의 emit `requestModify` 만 상위로 전달.
    2) `MyAttendanceView.onDayDetailAction`: `requestModify` 수신 시 `dayDetail` 기준 시트용 day 객체 구성(`workYmd = dayDetail.workDate`, `actions = dayDetail.sheetActions`, 메타) 후 시트 오픈. (기존 attendanceCorrection/overtime 직접 라우팅 분기는 시트 경유로 대체 — onSheetAction 재사용.)
  - 영향 받는 파일:
    - `prafta-app-frontend/prafta-app-frontend/src/views/attd/components/AttendanceDayDetailCard.vue` (template/style/script: 2버튼 제거 — 골격)
    - `prafta-app-frontend/prafta-app-frontend/src/views/attd/MyAttendanceView.vue` (script: onDayDetailAction 시트 라우팅 — developer)
  - 영향 받는 endpoint: GET /appApi/attd/my/day-detail (sheetActions 소비)
  - 연결 UI 명세: UI-A006(기존 시트, 재사용).
- **선행 작업**: prafta-app-013-2
- **우선순위 근거**: API 산출 후 화면. attd +1 격상.

---

## 우선순위 (실행 순서)

1. prafta-app-013-1 (백엔드 규칙 교체 + 공용화) — 선행 없음
2. prafta-app-013-2 (백엔드 today/day-detail 산출) — 1 선행
3. prafta-app-013-3 (오늘 탭 시트 연결) — 2 선행
4. prafta-app-013-4 (이번달 2버튼 제거 + 시트 통일) — 2 선행 (3·4 병행 가능)

## 수용 기준 (qa 체크포인트)

게이팅(3탭 동일):
- 스케줄 수정: 스케줄 있고 미마감이면 과거/오늘/미래 모두 활성. 스케줄 없으면 비활성. 마감월 비활성.
- 근태 보정: 미래 비활성. 과거/오늘 활성. 단 오늘 근무중(출근만, 미완료)이면 비활성. 마감월 비활성. 스케줄·근태기록 없는 과거/오늘도 요청 문구상 활성(엣지 — 결정 §6 허용, qa 확인).
- 초과근무: 미래 비활성. 과거/오늘 + 출근기록 존재면 활성. 출근기록 없으면 비활성. 오늘 근무중 비활성. 마감월 비활성.
- 연차: 출근기록 없으면 과거/오늘/미래 활성. 출근기록 있으면 비활성. 마감월 비활성. 스케줄 없는 날은 `leaveFullDayOnly=true` 동봉(폼 강제는 F1).
- 마감월(`closed`): 4액션 전부 비활성(시트는 열리되 4행 모두 disabled).

프론트:
- 오늘 탭 "수정 요청" 항상 눌림 → 시트 오픈, 4행이 sheetActions 로 개별 게이팅.
- 이번달 일자상세 하단 2버튼 없음. 본체 "수정 요청" → 시트 오픈.
- 시트 `leave` 선택 시 기존 stub("준비 중").
- 시트 액션(scheduleModify/attendanceCorrection/overtime) → 기존 AttdRequest 라우팅 정상.

하위호환:
- 오늘 카드 primary 버튼(퇴근하기/2구간 출근)·`canCheckOut`/`canCheckIn` 동작 불변.
- week 응답 4플래그 명칭 불변(leaveFullDayOnly 가산만).

## Follow-up (본 작업 범위 밖)

- F1: 연차 신청 폼 구현 시 `leaveFullDayOnly` 소비(스케줄 없는 날 full-day 강제). 현재 폼 stub(메모리 `project_prafta_app_005_my_leave`).
- F2(선택): 마감월 등 4행 전부 비활성일 때 "수정 요청" 버튼 자체 비활성/안내 UX. 결정 §6에서 "항상 오픈" 수용 — 선택적.
- F3: `MonthDayResponse.actions` 가 신규 규칙을 따르는지 developer 확인(이번달 셀 자체에 시트가 없다면 무영향; 셀 클릭이 day-detail 호출 경유면 무관).
