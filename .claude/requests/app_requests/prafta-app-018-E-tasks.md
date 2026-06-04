# prafta-app-018-E — 작업 분해 (planner)

> 상위: `prafta-app-018-leave-apply-plan.md` / 원 작업지시서: `prafta-app-018-E-app-partial-leave.md`
> 영역: **앱(BE + FE)**. BE = `com.prafta.app.attd.attd01`, FE = `prafta-app-frontend`(이중중첩) `src/views/attd/*`.
> 확정(사용자 (a)안): 연차 사용 마커를 부분휴가 상세로 표기.
>   예 `월차 · 시간차 · 03:00~04:30 · 0.19일` / 반차 `월차 · 반차 · 0.5일` / 종일=현행 유지(라벨만).
>   시간차/반차일은 그날 **근무일로 유지**(스케줄/출퇴근/표준화 로직 무영향).

---

## 0. planner 조사 결론 (정독 결과 — developer 필독)

### 0-1. 스키마(컬럼 확인 완료)
`TB_USER_LEAVE_USE` 에 다음 컬럼 존재(웹 `LeaveFlowMapper.xml` 에서 동일 컬럼 SELECT/UPDATE 확인):
`USE_UNIT_TYPE`(SYS025), `START_TIME`, `END_TIME`, `LEAVE_DAYS`(decimal(8,5)), `LEAVE_MINUTES`.
SYS025 코드: `00`=종일 / `01`=반차 / `02`=2시간 / `03`=1시간 / `04`=30분.
- ⚠️ schema-full.sql 스냅샷에는 이 테이블이 없음(낡음). 컬럼은 웹 매퍼 실사용으로 교차확인했고,
  developer 는 착수 전 MCP `prafta-mysql` 로 `DESCRIBE TB_USER_LEAVE_USE` 재확인할 것
  (특히 `START_TIME`/`END_TIME`/`LEAVE_MINUTES` 의 타입·NULL 여부). 본 분해는 웹 매퍼 기준 가정.

### 0-2. 주간/월간 경로 (이미 leaveByYmd 사용)
`selectWeek`(서비스 483행 `expandLeave`) / `selectMonth`(689행) 는 `selectLeaveUseByRange`→`expandLeave`
→`leaveByYmd` 맵을 만들어 일자별 연차를 매핑한다.
- 주간: `WeekDayResponse.isLeaveUsed/leaveTypeName` 채움(서비스 538~539행). **여기에 상세필드 추가**.
- 월간: `MonthDayResponse` 는 캘린더 셀(dayType=LEAVE)만 — 상세 텍스트 표시 자리 없음(점/색만).
  → **월간은 상세필드 추가 불필요**(캘린더 셀에는 텍스트 안 들어감). 상세는 셀 클릭 후 day-detail 카드에서 본다.

### 0-3. ⚠️ 오늘/일자상세 경로의 핵심 갭 (가장 중요)
`buildDayResponse`(today·day-detail, 서비스 147~269행)는 **`selectLeaveUseByRange` 를 호출하지 않는다.**
연차 판정을 오로지 `selectScheduleByRange`(TB_USER_WORK_PLAN→TB_LEAVE_TYPE_MGMT 조인)의 `leaveCd/leaveNm`
존재로만 한다(`isLeaveDay`, 166행). 그런데 **부분연차(시간차/반차)는 `upsertWorkPlanLeave` 가 종일만**
work_plan 에 LEAVE 로 심으므로(상위 요청서 §배경), **시간차/반차일은 work_plan 에 SCH_CD(근무)로 남아
`isLeaveDay=false`** → today/day-detail 응답에 연차 흔적이 전혀 없다(현행).
또한 `MyAttendanceDayResponse` 에는 `leaveTypeName` 필드조차 없다(현행 today/day-detail 는 연차를 `workPlanName`
=종일연차의 leaveNm 으로만 표시).
- ⇒ **today/day-detail 에서 부분연차 상세를 보이려면, buildDayResponse 가 추가로 `selectLeaveUseByRange`
  (단일일 범위)를 호출**하여 그날 연차 사용 내역을 가져와 새 표시필드에 실어야 한다(근무일 유지 = 스케줄/슬롯/
  출퇴근 로직은 그대로 두고, **연차 사용 정보를 부가 표시로만 덧댄다**).
- 종일연차일(work_plan=LEAVE, isLeaveDay=true)도 동일 selectLeaveUseByRange 결과로 단위 라벨(`종일`)을
  채워 일관 표기한다(없으면 현행 leaveNm 라벨 유지).

### 0-4. 단위 라벨 방식 — **FE 매핑으로 결정**
앱 관례가 FE 상수 매핑이다(`LeaveApplyForm.vue` 271~277행 `UNIT_LABELS`, 웹은 FNC SYS025).
→ **BE 는 라벨 문자열을 만들지 않고 `useUnitType` 코드 + 숫자필드만 내려준다. 라벨 산출은 FE(attdFormat.js)에서.**
이유: (1) 앱 일관성, (2) BE에 한글 문자열 누수 방지(인코딩 함정 회피), (3) "시간차" 그룹라벨 정책이 FE 표시규칙.

### 0-5. 차감일수 표시 자릿수 — **소수 2자리 반올림으로 결정** (요청서 (a)안 예시 `0.19일` 정합)
원 작업지시서 (a)안 예시가 `0.1875 → 0.19일`(2자리 반올림)이다. **앱 E 는 2자리 반올림 + 불필요한 0 정리**.
- 규칙: `round(LEAVE_DAYS, 2)` 후 trailing zero 정리. 예 `0.18750→0.19`, `0.50000→0.5`, `1.00000→1`.
- ⚠️ 웹 D(`prafta-app-018-D`)는 trailing-zero trim만(`0.12500→0.125`, 무반올림)으로 다르다.
  **이는 의도된 영역차**(웹 D=결재자 정밀표시, 앱 E=내근태 요약표시). developer 는 두 정책을 혼용하지 말 것.
  앱 E 의 표시 자릿수 단일 출처는 attdFormat.js 신규 헬퍼(`formatLeaveDays`)다.

---

## 1. 작업 분해

### prafta-app-018-E-1 [backend] LeaveUseResult/매퍼 확장 + today·day-detail 연차 상세 주입

- **유형**: backend
- **영역**: app
- **모듈**: attd/attd01
- **작업 유형**: 보완
- **요구사항 요약**: 연차 사용 조회에 단위/시각/차감분 컬럼을 더하고, 주간 응답과 오늘/일자상세 응답에 부분연차 상세 표시필드를 노출한다.

- **상세 설명**:
  - **핵심 요구사항**:
    1) `result/LeaveUseResult` record 에 컬럼 추가: `useUnitType`(U.USE_UNIT_TYPE), `startTime`(U.START_TIME),
       `endTime`(U.END_TIME), `leaveMinutes`(U.LEAVE_MINUTES). **record 끝에 이 순서로 append**.
       기존 필드(startDate/endDate/leaveCd/leaveNm/leaveDays) 위치 불변 → 그 뒤에 4개 추가.
    2) `mapper AppAttd01Mapper.xml#selectLeaveUseByRange` SELECT 절 끝(현재 `U.LEAVE_DAYS AS leaveDays` 다음)에
       `, U.USE_UNIT_TYPE AS useUnitType` `, U.START_TIME AS startTime` `, U.END_TIME AS endTime`
       `, U.LEAVE_MINUTES AS leaveMinutes` 를 **record append 와 동일 순서로** 추가(⚠️ MyBatis 위치매핑:
       SELECT 컬럼순서 = record 생성자 인자순서. 순서 어긋나면 타입밀림). WHERE/JOIN/스코프 가드 불변.
    3) **주간(`WeekDayResponse`) 표시필드 추가**: 기존 `isLeaveUsed/leaveTypeName` 옆에
       - `leaveUnitType`(String, SYS025 코드 원값 — FE 라벨산출용)
       - `leaveTimeRange`(String, 시간차일 때 `"HHMM~HHMM"` 형태 또는 start/end 각각? → **아래 4) 키 계약** 참조)
       - `leaveDays`(BigDecimal, 차감일수 원값)
       을 노출. `expandLeave` 결과 `LeaveUseResult` 에서 그대로 옮긴다(서비스 538~539행 빌더에 추가).
    4) **today/day-detail(`MyAttendanceDayResponse`) 표시필드 추가 + selectLeaveUseByRange 단일일 호출**:
       - `buildDayResponse` 안에서 `appAttd01Mapper.selectLeaveUseByRange(q)`(q 는 이미 targetYmd~targetYmd 단일범위)
         를 호출하여 그날 연차 사용 1건(다건이면 §3 엣지대로)을 가져온다.
       - `MyAttendanceDayResponse` 에 필드 추가: `isLeaveUsed`(boolean, @JsonProperty 고정), `leaveTypeName`,
         `leaveUnitType`, `leaveTimeRange`, `leaveDays`.
       - **근무일 유지**: 이 호출/필드주입은 표시 전용. `isLeaveDay`(work_plan 기반)·`slotCount`·`slots`·
         `workStatus`·`dayType`·합계 산출 로직을 **일절 건드리지 않는다**. 즉 시간차/반차일은 종전처럼
         스케줄/슬롯/출퇴근이 그대로 나오고, 연차 상세는 부가 표시로만 더해진다.
       - 종일연차일(isLeaveDay=true)도 selectLeaveUseByRange 결과가 있으면 동일 필드를 채운다(단위=종일).
         결과가 없으면(과거 종일연차가 leave_use 없이 work_plan 만 있는 레거시) 기존 leaveNm 표시 유지(필드 null 허용).
    5) **⚠️ Lombok boolean is-접두 함정**: 신규 `isLeaveUsed`(today/day-detail)는 `@JsonProperty("isLeaveUsed")`
       로 고정(WeekDayResponse 의 기존 isLeaveUsed 와 동일 패턴, 14~33행 참조). 다른 신규 필드는 String/BigDecimal
       이라 해당없음.

  - **키 계약(FE 소비) — 확정**:
    - `leaveUnitType`: SYS025 코드 String(`"00"~"04"`). 라벨은 FE 매핑.
    - `leaveTimeRange`: **`"HHMM~HHMM"` 단일 문자열**로 결정(예 `"0300~0430"`). start/end 둘 다 있을 때만 non-null;
      종일/반차는 START_TIME/END_TIME 이 있을 수도(상위 §D2-a: 시간단위 휴가의 종일/반차는 시각이 채워짐) 없을 수도
      있으므로, **시각 표시 여부는 BE 가 단위로 판단하지 말고** START_TIME·END_TIME 이 둘 다 존재하면 range 를 만들고
      없으면 null 로 둔다. (FE 가 단위코드로 "시간차일 때만 range 노출" 결정 — §2 참조. BE 는 데이터만.)
      → 포맷 헬퍼는 BE 에 이미 있는 패턴 없음. **BE 는 `START_TIME~END_TIME` 을 `start + "~" + end` 로 단순 조합**
      (둘 다 hasText 일 때만; 아니면 null). 콜론(:) 삽입은 FE 가 한다(HHMM→HH:MM).
    - `leaveDays`: BigDecimal 원값(정규화는 FE). 직렬화 시 trailing zero 가 남을 수 있으나 FE 가 round 처리.
    - `leaveTypeName`: 연차종류명(leaveNm). today/day-detail 신규.
    - `leaveUnitType/leaveTimeRange/leaveDays/leaveTypeName` 은 연차 미사용일이면 전부 null(+ isLeaveUsed=false).

  - **영향 받는 파일**:
    - `prafta-backend/src/main/java/com/prafta/app/attd/attd01/result/LeaveUseResult.java` (record append)
    - `prafta-backend/src/main/resources/com/prafta/app/attd/attd01/mapper/AppAttd01Mapper.xml` (selectLeaveUseByRange SELECT append)
    - `prafta-backend/src/main/java/com/prafta/app/attd/attd01/dto/response/WeekDayResponse.java` (필드 3종 추가)
    - `prafta-backend/src/main/java/com/prafta/app/attd/attd01/dto/response/MyAttendanceDayResponse.java` (필드 5종 추가, isLeaveUsed @JsonProperty)
    - `prafta-backend/src/main/java/com/prafta/app/attd/attd01/service/impl/AppAttd01ServiceImpl.java`
      (buildDayResponse 에 selectLeaveUseByRange 호출+필드주입; selectWeek 빌더에 필드주입; leaveTimeRange 조합 헬퍼)
  - **영향 받는 endpoint**(계약 변경=필드 추가, 하위호환):
    - `GET /appApi/attd/my/week` (WeekDayResponse.days[])
    - `GET /appApi/attd/my/today` (MyAttendanceDayResponse)
    - `GET /appApi/attd/my/day-detail` (MyAttendanceDayResponse)
    - 월간 `GET /appApi/attd/my/month` 는 **변경 없음**(캘린더 셀은 텍스트 미표시, §0-2).
  - **예상 산출물**: result record / mapper / 2 response DTO / service.
  - **연결 UI 명세**: UI-A003(주간 리스트)·UI-A002/UI-A005(오늘/일자상세 카드) **수정**(신규 화면 아님 → 골격 신규 작성 없음, §2 diff 지침).

- **선행 작업**: 없음(조회 전용 확장). (상위 B 의 leave_use INSERT 컨벤션과 데이터 정합하나, E 는 읽기라 코드 의존 없음.)
- **우선순위 근거**: 법적 책임영역(attd) +1단계. 단 읽기전용 표시 보완이라 데이터정합/보안 위험 낮음 → 중간.

---

### prafta-app-018-E-2 [frontend-screen] 내 근태 4뷰 연차 마커 상세표기

- **유형**: frontend-screen (기존 컴포넌트 수정 — 신규 .vue 없음)
- **영역**: app
- **모듈**: attd
- **작업 유형**: 보완
- **요구사항 요약**: 주간 리스트/오늘 카드/일자상세 카드의 연차 마커를 `종류 · 단위(· 시각) · 차감일수`로 표기. 종일은 현행 유지, 시간차/반차는 상세+근무일 유지.

- **상세 설명**:
  - **핵심 요구사항**:
    1) **포맷 헬퍼 신설**(`src/views/attd/attdFormat.js`):
       - `LEAVE_UNIT_LABELS = { '00':'종일','01':'반차','02':'시간차','03':'시간차','04':'시간차' }`
         (02·03·04 모두 "시간차" 그룹라벨 — (a)안 예시 정합. 정밀 "2시간/1시간/30분"은 표기 안 함, 시각 range 로 대체).
       - `leaveUnitLabel(useUnitType)` → 위 맵 lookup, 미상 시 ''.
       - `isLeaveTimeUnit(useUnitType)` → `['02','03','04'].includes(code)`.
       - `formatLeaveTimeRange(rangeStr)` → BE 가 준 `"HHMM~HHMM"` 를 `"HH:MM~HH:MM"` 로 변환
         (내부에서 formatHHMM 재사용; 입력 null/형식불충분이면 null).
       - `formatLeaveDays(days)` → **소수 2자리 반올림 + trailing zero 정리**(§0-5). 예 0.1875→"0.19",
         0.5→"0.5", 1→"1". 구현: `Number(parseFloat(days).toFixed(2))` 후 String. 비유효 입력은 null.
       - `formatLeaveMarker({ leaveTypeName, leaveUnitType, leaveTimeRange, leaveDays })`
         → 부분/종일 공통 마커 문자열 1줄 생성:
         - 토큰 배열 = [leaveTypeName, leaveUnitLabel(code)] + (isLeaveTimeUnit && range 있으면 [formatLeaveTimeRange(range)])
           + (formatLeaveDays(days) 있으면 [`${days}일`]).
         - `.filter(Boolean).join(' · ')`. 예 종일 `월차 · 종일 · 1일`(또는 leaveDays 노출 정책 — 아래 표기규칙).
       - **종일 표기규칙(현행 유지 원칙)**: (a)안 "종일=현행 유지". 종일은 **`월차 · 종일`까지만**(차감일수 생략 가능)
         으로 두되, 현행이 종류명만이면 그대로. → developer 는 종일 분기에서 일수 토큰을 생략(반차/시간차만 일수 표기).
         반차 = `월차 · 반차 · 0.5일`, 시간차 = `월차 · 시간차 · 03:00~04:30 · 0.19일`. **이 3분기를 formatLeaveMarker 가 단위코드로 처리**:
         `'00'`(종일)→일수·시각 토큰 모두 생략, `'01'`(반차)→일수만, `'02'~'04'`(시간차)→시각+일수.
    2) **AttendanceWeekList.vue 수정**(주간 카드):
       - 현재 연차는 `bd bd-w "연차"` 배지 + `planTitleText`=leaveTypeName 만 표시(165행), `summaryHtml` 는 연차면 ''(203행).
       - **수정**: `isLeave(day)` 일 때 `db__summary` 자리(또는 제목 보조)에 `formatLeaveMarker(day)` 결과를 1줄 노출.
         가장 간섭 적은 방법 = `summaryHtml(day)` 의 연차 분기를 '' 대신 마커 문자열로 변경(plain text, v-html 불필요 →
         새 `leaveMarkerText(day)` computed/함수로 분리하고 템플릿에서 `isLeave(day)` 면 `<span class="db__summary">{{ leaveMarkerText(day) }}</span>`).
         배지("연차")는 유지. 근무/휴무 분기 회귀 금지.
    3) **AttendanceTodayCard.vue 수정**(오늘/일자상세 본체):
       - 현행은 연차일을 `detail.workPlanName`(종일 leaveNm)로만 보여주고, 시간차/반차일은 근무카드로 나온다(연차 흔적 0).
       - **수정**: 상태배지 영역(`.sl`, 23~27행) 또는 그 아래에, `detail.isLeaveUsed` 이면 연차 마커 1줄을 추가 노출.
         시간차/반차는 근무 3행(스케줄/근태/표준화)을 **그대로 두고**(근무일 유지), 그 위/아래에 연차 마커 칩/라인을 덧댄다.
         권장: `.sl` 아래에 `<p v-if="isLeaveUsed" class="lv-marker">{{ leaveMarkerText }}</p>` 한 줄 추가.
         종일연차일(slots 비어있음)도 동일 마커로 표기(workPlanName 의존 축소 또는 병기).
       - `leaveMarkerText` = `formatLeaveMarker(props.detail)`. `isLeaveUsed` = `!!props.detail?.isLeaveUsed`.
       - 신규 스타일 `.lv-marker`: CSS 변수만(예 `color: var(--color-warning-text)`, `font-size:12px`, scoped). !important 금지.
    4) **AttendanceMonthCalendar.vue**: **변경 없음**(셀 텍스트 미표시). 셀 클릭→day-detail 카드에서 상세 표기됨.
    5) **AttendanceDayDetailCard.vue**: AttendanceTodayCard 재사용 wrapper라 **추가 변경 없음**(3 자동 반영).

  - **영향 받는 파일**:
    - `prafta-app-frontend/prafta-app-frontend/src/views/attd/attdFormat.js` (헬퍼 5종 추가)
    - `prafta-app-frontend/prafta-app-frontend/src/views/attd/components/AttendanceWeekList.vue` (연차 마커 라인)
    - `prafta-app-frontend/prafta-app-frontend/src/views/attd/components/AttendanceTodayCard.vue` (연차 마커 라인 + .lv-marker 스타일)
    - (무변경 확인용) AttendanceMonthCalendar.vue / AttendanceDayDetailCard.vue
  - **백엔드 의존**: E-1 (`leaveUnitType/leaveTimeRange/leaveDays/leaveTypeName/isLeaveUsed` 키).
  - **예상 산출물**: attdFormat.js 헬퍼 + 2 컴포넌트 수정. **신규 .vue 없음 → planner Vue 골격 작성 대상 아님**(기존 컴포넌트 diff 지침으로 충분, §2).
  - **연결 UI 명세**: UI-A003 / UI-A002 / UI-A005 (수정).

- **선행 작업**: prafta-app-018-E-1.
- **우선순위 근거**: E-1 의 키 계약 의존. 표시 보완.

---

## 2. FE 수정 지침 (diff 가이드 — 신규 .vue 없으므로 골격 대신 명세)

> 본 작업은 **기존 컴포넌트 수정**이라 planner Vue 골격(신규 template/style 전량)을 작성하지 않는다.
> 아래는 developer 가 따라야 할 정확한 수정 위치/규칙. script 로직(헬퍼 구현)은 developer 영역이나,
> 표시 규칙·CSS 토큰·간섭 금지 범위는 planner 가 못박는다.

### 2-1. attdFormat.js (신규 export — 표시 전용, 순수함수)
- 위 E-2 핵심요구 1) 의 5 헬퍼. 모두 입력 검증 후 안전 폴백(null/''). 외부 데이터 가공·API·store 금지.
- `formatLeaveMarker` 단위코드 분기표:
  | useUnitType | 토큰 구성 | 예시 |
  |---|---|---|
  | `00` 종일 | 종류 · 종일 | `월차 · 종일` |
  | `01` 반차 | 종류 · 반차 · {일수}일 | `월차 · 반차 · 0.5일` |
  | `02/03/04` 시간차 | 종류 · 시간차 · {HH:MM~HH:MM} · {일수}일 | `월차 · 시간차 · 03:00~04:30 · 0.19일` |
  | (코드 null·미상) | 종류만 | `월차` |
- range 가 비어도(시간차인데 시각 결측) range 토큰만 생략하고 나머지는 유지(방어).

### 2-2. AttendanceWeekList.vue
- 템플릿 53행 `summaryHtml(day)` 라인 유지(근무/휴무용). 그 **앞**에 연차 분기 추가:
  `<span v-if="isLeave(day)" class="db__summary">{{ leaveMarkerText(day) }}</span>`
  그리고 기존 `summaryHtml` 의 v-html 라인은 `v-else-if="summaryHtml(day)"` 로 가드(연차일 중복 출력 방지).
- script: `import { ..., leaveUnitLabel, isLeaveTimeUnit, formatLeaveTimeRange, formatLeaveDays, formatLeaveMarker } from '../attdFormat'`
  후 `const leaveMarkerText = (day) => formatLeaveMarker(day)` 추가. day 객체는 이미 `leaveTypeName/leaveUnitType/leaveTimeRange/leaveDays` 보유(E-1).
- 배지(`bd-w "연차"`, 46행) 유지. `dc--leave` 배경 유지. 근무/휴무 summaryHtml 회귀 금지.

### 2-3. AttendanceTodayCard.vue
- 템플릿 `.sl`(23~27행) 블록 **직후**에 추가:
  ```
  <p v-if="isLeaveUsed" class="lv-marker">{{ leaveMarkerText }}</p>
  ```
- script: attdFormat 에서 `formatLeaveMarker` import.
  `const isLeaveUsed = computed(() => !!(props.detail && props.detail.isLeaveUsed))`
  `const leaveMarkerText = computed(() => formatLeaveMarker(props.detail || {}))`
- 슬롯/상태/버튼/표준화 로직 **일절 무수정**(근무일 유지). 연차 마커는 부가 라인 1개만.
- style scoped 추가(CSS 변수만):
  ```
  .lv-marker { margin: 2px 0 6px; font-size: 12px; font-weight: 600; color: var(--color-warning-text); }
  ```
  (warning 톤 = 주간 연차 배지/캘린더 lv 셀과 톤 일관). !important 금지, 하드코딩 색/px 금지(px 단위는 기존 카드 관례를 따르되
  색은 토큰만).

### 2-4. 무변경 컴포넌트(회귀 확인 대상)
- AttendanceMonthCalendar.vue: 셀 색/점만(LEAVE dayType). 텍스트 미표시 → 수정 불필요.
- AttendanceDayDetailCard.vue: TodayCard 재사용 → 자동 반영.
- MyAttendanceView.vue: `toSheetDay` 가 `leaveTypeName` 을 시트로 넘기는데(624행), detail 에 이제 leaveTypeName 이
  실제로 채워지므로(E-1) 시트 메타가 개선됨 — **추가 수정 불필요**(키 이미 존재). 단 회귀 확인.

---

## 3. 수용 기준 / 엣지

### 3-1. 표기 정확성
- **시간차**(02/03/04): 주간/오늘/일자상세에서 `종류 · 시간차 · {HH:MM~HH:MM} · {일수}일`. 시각·일수 모두 표기.
- **반차**(01): `종류 · 반차 · 0.5일`. 시각 없음(또는 있어도 미표기 — 단위 기준 FE 판단).
- **종일**(00): `종류 · 종일`(현행 유지, 일수·시각 미표기). 종전 종일연차 표시 회귀 없음.
- 차감일수: 2자리 반올림 + trailing zero 정리(0.1875→0.19, 0.5→0.5, 1→1). 웹 D(0.125 무반올림)와 혼동 금지.

### 3-2. 근무일 유지 (최우선 불변식)
- 시간차/반차일은 그날 스케줄/슬롯/출퇴근/표준화/지각·조퇴/합계가 **종전과 100% 동일**하게 산출·표시된다.
  연차 마커는 **부가 표시 1줄**만 추가될 뿐, work_plan/slot/dayType/workStatus 로직 무영향.
- 회귀 테스트: 동일 사용자·동일 날짜에서 E 적용 전/후 slots·workStatus·plannedSum·actualSum·attendanceStatus 불변.

### 3-3. 종일연차일
- work_plan=LEAVE(isLeaveDay=true)인 종일연차일: today/day-detail 에서 `월차 · 종일` 마커 + (기존 workPlanName 병기/대체).
  leave_use 미존재 레거시 종일연차는 필드 null → 현행 leaveNm 표시로 폴백(깨지지 않음).

### 3-4. 다건 / 같은 날 복수 연차
- `selectLeaveUseByRange`(단일일)가 같은 날 2건 이상 반환 가능(예 오전 시간차 + 오후 시간차).
  - 주간 `expandLeave` 는 `putIfAbsent`로 **첫 1건만** 매핑(현행 동작 유지) → 마커도 첫 1건 기준.
  - today/day-detail 신규 호출도 **첫 1건 기준 마커**(현행 주간과 일관). 다건 합산/리스트 표기는 **본 작업 범위 외**
    (요청서 §수용기준 "다건 처리"는 "깨지지 않게 첫 건 표기 + 나머지 무시"로 해석. 다건 풀표기는 follow-up).
  - ⚠️ developer 는 다건일 때 NPE/리스트인덱스 오류 없이 첫 건만 안전 채택할 것(list.isEmpty 가드).

### 3-5. record 위치매핑 / 빌드
- `LeaveUseResult` record append 순서 = mapper SELECT append 순서 **정확히 일치**(useUnitType→startTime→endTime→leaveMinutes).
  타입: useUnitType=String, startTime=String, endTime=String, leaveMinutes=Integer(또는 DB가 char면 String — DESCRIBE 확인).
  ⚠️ leaveMinutes 가 DATETIME 아님 확인(위치밀림 시 record 인접필드로 잘못 매핑되는 함정 — 메모리 record 함정).
- BE 빌드(gradlew, 비대화형) 통과. FE SFC/eslint 통과(TS 금지, scoped 유지).

### 3-6. 직렬화 계약
- today/day-detail 신규 `isLeaveUsed` 는 `@JsonProperty("isLeaveUsed")` 로 키 고정(is 탈락 방지). FE 가 `detail.isLeaveUsed` 소비.
- 연차 미사용일: 모든 신규 연차필드 null + isLeaveUsed=false. FE 마커는 미렌더(v-if).

---

## 4. 정책 출처
- attd §8(연차)·§8.5(사용단위·차감)·§10(근태표시). prafta-019(시간차·LEAVE_DAYS decimal). prafta-024(사용단위).
- 상위 `prafta-app-018-leave-apply-plan.md` §D2-a(시간단위 휴가의 종일/반차 시각표현), [[project_prafta_app_002_my_attendance]].
- 화면 규약: CSS변수/공통컴포넌트/비-TS/scoped(CLAUDE.md 프론트엔드 환경).

## 5. 후속 에이전트 가이드
- **developer(BE)**: §0-1 MCP DESCRIBE 재확인 → E-1. record/mapper 위치매핑 최우선. buildDayResponse 의 연차 호출은
  **표시 전용·근무일 무영향** 원칙 엄수.
- **developer(FE)**: E-2. attdFormat 순수헬퍼 + 2 컴포넌트 부가 라인. 근무 분기 회귀 금지.
- **qa**: §3 전부. 특히 3-2(근무일 유지 불변식: 적용 전/후 slots·합계·status 동일), 3-4(다건 첫건 안전채택), 3-1(자릿수 0.19).
  ⚠️ planner 골격이 아닌 diff 지침이므로 "스펙 가정 도전 + 엣지 탐색"으로 검증(종일/반차/시간차/레거시 종일/다건/미사용일 6케이스).
- **security**: 조회 확장만(쓰기 없음). selectLeaveUseByRange 는 기존 q 스코프(cmpny/site/user JWT) 그대로 → IDOR 신규노출 없음.
  확인 포인트: today/day-detail 신규 호출이 동일 q(자기 스코프)만 쓰는지(다른 식별자 주입 없음).
