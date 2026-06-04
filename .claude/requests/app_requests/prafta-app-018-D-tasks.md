# prafta-app-018-D — 작업 분해 (웹 FE+BE: 일자상세 팝업 연차 요청 카드 표시 정정)

> 분해자: planner / 출처: `prafta-app-018-D-web-leave-card.md` (+ 상위 `prafta-app-018-leave-apply-plan.md`)
> 영역: web (PRAFTA/prafta-backend + PRAFTA/prafta-web-frontend). 앱 작업 A/B/C 와 파일 비충돌 → 병렬 가능.
> 운영규칙: 재개불가 자율진행(Write). Notion 금지. 정책출처 attd §8/§8.5(사용단위·차감)·§9(결재표시), prafta-019(시간차/LEAVE_DAYS decimal), prafta-024(사용단위 SYS025).

---

## 0. 사실 확정 (코드 정독 결과 — ⚠️ 작업지시서 가정 1건 정정)

작업지시서 BE 항은 "`LEAVE_TYPE`('ANNUAL'/'HALF'/'HOUR' 계열)로 단위 유도 허용"이라 적었으나, **코드 정독 결과 LEAVE_TYPE 은 사용단위가 아니다**:

- `TB_USER_ATTD_REQ.LEAVE_TYPE` = "ANNUAL/HALF_AM/HALF_PM 등 **성격 코드**"
  (출처: `LeaveApplyRequest.java:27` 주석 — "요청 LEAVE_TYPE (ANNUAL/HALF_AM/HALF_PM 등 성격 코드)").
  → 종일/반차/시간차(=사용단위)와 1:1 매핑되지 않음. 시간차 N시간 구분 불가. **표시 출처로 부적합.**
- **사용단위(종일/반차/2시간/1시간/30분)의 진짜 출처는 `TB_USER_LEAVE_USE.USE_UNIT_TYPE` (SYS025)**.
  상수: `LeaveFlowServiceImpl.java:54~59` UNIT_FULL='00'(종일) / UNIT_HALF='01'(반차) / UNIT_HOUR2='02'(2시간) / UNIT_HOUR1='03'(1시간) / UNIT_MIN30='04'(30분).
  라벨은 `FNC_CMM_INFO_SRCH(cmpny,'SYST_VAL',USE_UNIT_TYPE,'SYS025')` 로 한글("종일/반차/2시간/1시간/30분") 산출.
- **차감일수**: `TB_USER_ATTD_REQ.LEAVE_DAYS` (decimal) — R 에 직접 존재(조인 불필요). `LeaveReqInsertCommand.leaveDays` 가 `BigDecimal`.
- **연차종류명**: `TB_LEAVE_TYPE_MGMT.LEAVE_NM` (U.LEAVE_CD 조인). (현 목표 포맷 "연차사용 ·…"에는 미사용이나, 확장 대비 함께 내림 — 선택. 본 분해는 **미포함**으로 단순화. §1-B 참조.)

### 이미 존재하는 정답 미러 레퍼런스
`LeaveFlowMapper.xml#selectMyPendingLeaveApprovals` (344~388행) 가 **정확히 본 작업이 필요한 필드를 이미 산출**한다:
`U.USE_UNIT_TYPE AS useUnitType`, `FNC...'SYS025' AS unitNm`, `R.LEAVE_DAYS AS leaveDays`, `R.START_TIME/END_TIME`.
그리고 05/06 구분 LEFT JOIN 조건도 완성형:
```
LEFT JOIN TB_USER_LEAVE_USE U
  ON U.CMPNY_CD = R.CMPNY_CD AND U.DEL_YN = 'N'
 AND ( (R.REQ_TYPE='05' AND U.REQ_ID=R.REQ_ID AND U.LEAVE_STATUS='CONFIRMED')
    OR (R.REQ_TYPE='06' AND U.LEAVE_ID=R.TARGET_ID) )
```
→ selectMonthlyAttdReq 에 **이 조인을 그대로 미러링**하면 된다. 타 타입(01~04/10)은 U 미매칭 → useUnitType/unitNm NULL.

### 출처 결정 (확정)
**USE_UNIT_TYPE 조인 채택**(LEAVE_TYPE/시간차이 유도 불채택). 이유: ① LEAVE_TYPE 은 사용단위가 아님 ② START/END 차이로 단위 역산은 야간·휴게 변수로 불안정 ③ 동일 모듈에 검증된 SYS025 라벨 산출 패턴 존재.

### ⚠️ 스키마 확인 한계 (보고)
MCP MySQL 도구가 본 세션 도구목록에 없고 `schema-full.sql` 스냅샷에 `TB_USER_ATTD_REQ` 가 없다(메모리 `project_prafta_app_001` 의 "schema 스냅샷 낡음"과 일치). 따라서 컬럼/타입은 **추측이 아닌 운영 매퍼(LeaveFlowMapper.xml)에서 실제 사용 중인 컬럼**으로만 확정했다:
`R.LEAVE_DAYS`, `R.START_TIME/END_TIME`, `U.USE_UNIT_TYPE`, `U.REQ_ID`, `U.LEAVE_ID`, `U.LEAVE_STATUS`, `U.DEL_YN`, `R.TARGET_ID`, `R.REQ_TYPE`, `R.CMPNY_CD` — 모두 운영 SELECT/INSERT 에서 검증된 실존 컬럼. developer 는 빌드 전 MCP 가용 시 `DESCRIBE TB_USER_LEAVE_USE` / `TB_USER_ATTD_REQ` 로 재확인 권장(특히 LEAVE_DAYS 정밀도 표기).

---

## prafta-app-018-D-1 [backend] selectMonthlyAttdReq + MonthlyAttdReqResult 연차 표시 컬럼 추가

- **유형**: backend (보완)
- **영역**: web
- **모듈**: attd/attd07
- **요구사항 요약**: 일자상세 요청 조회에 연차(05/06) 사용단위·차감일수를 실어, 프론트가 시각 모델 대신 연차 전용 카드를 그릴 수 있게 한다.

### 핵심 요구사항
1) `MonthlyAttdReqResult` record **끝**에 신규 필드 3개를 **순서대로** 추가 (위치기반 매핑):
   `String leaveDays` → `String useUnitType` → `String unitNm`.
   - `leaveDays` 는 String 으로 받는다(현 record 의 다른 수치성 시각도 모두 String). 포맷 정규화는 FE 담당. (BigDecimal 매핑 시 `1.00000` 등 trailing 0 가 그대로 와 FE 정규화 동일 처리 가능. String 권장 — 자릿수 보존+FE 단일 정규화.)
   - `unitNm` = SYS025 한글 라벨(종일/반차/2시간/1시간/30분). FE 가 그대로 `leaveTypeLabel` 에 사용(시간차 라벨 가공도 FE).
   - 각 필드에 한국어 주석 + "⚠️ record 끝 = SELECT 끝, 위치기반" 경고 1줄.
2) `Attd07Mapper.xml#selectMonthlyAttdReq` SELECT 절 **맨 끝**(현 마지막 컬럼 `CS.SEC_SCH_END_TIME AS curSecEndTime`, 1002행 뒤)에 동일 순서로 3컬럼 추가:
   ```
   , A.LEAVE_DAYS                                                  AS leaveDays
   , U.USE_UNIT_TYPE                                               AS useUnitType
   , FNC_CMM_INFO_SRCH(A.CMPNY_CD, 'SYST_VAL', U.USE_UNIT_TYPE, 'SYS025') AS unitNm
   ```
   (현 매퍼 alias 가 `A`(TB_USER_ATTD_REQ)이므로 R 대신 A 사용 — 일관성 유지.)
3) FROM 절에 `TB_USER_LEAVE_USE U` **LEFT JOIN** 추가 (selectMyPendingLeaveApprovals 조인 미러, alias A 기준):
   ```
   LEFT JOIN TB_USER_LEAVE_USE U
     ON U.CMPNY_CD = A.CMPNY_CD
    AND U.DEL_YN   = 'N'
    AND ( (A.REQ_TYPE='05' AND U.REQ_ID=A.REQ_ID AND U.LEAVE_STATUS='CONFIRMED')
       OR (A.REQ_TYPE='06' AND U.LEAVE_ID=A.TARGET_ID) )
   ```
   - 기존 LEFT JOIN(TS/WP/CS) 뒤에 둔다. WHERE 스코프(CMPNY/SITE/USER/WORK_YMD + REQ_STATUS='01' + 05/06 결재자 EXISTS 가드)는 **불변**.
   - LEFT JOIN 이므로 01~04/10 요청행은 U.* NULL → useUnitType/unitNm NULL, leaveDays 는 A.LEAVE_DAYS(연차 외엔 NULL). 회귀 없음.

### 영향 파일
- `prafta-backend/src/main/java/com/prafta/web/attd/attd07/result/MonthlyAttdReqResult.java` (record 끝 +3 필드)
- `prafta-backend/src/main/resources/com/prafta/web/attd/attd07/mapper/Attd07Mapper.xml` (`selectMonthlyAttdReq` 947~1033행: SELECT +3컬럼, FROM +1 LEFT JOIN)

### 타 statement 영향 점검 (회귀)
- `selectMonthlyAttdReqSummary`(396행~) : **resultType 이 `MonthlyAttdReqSummaryResult` (다른 record)** → MonthlyAttdReqResult 변경과 무관. 영향 없음.
- `selectDailyLeaveApprovalHistory`(1035행~) : resultType `DailyAttdDetailHistoryResult` (다른 record). 무관.
- `MonthlyAttdReqResult` 를 resultType 으로 쓰는 statement 는 `selectMonthlyAttdReq` **단 1건** (Grep 확인: record/매퍼 외 참조처는 FE 골격 주석뿐). → **record 끝 +3 필드가 영향 주는 SELECT 는 이 하나뿐**, 다른 호출자 회귀 없음.
- developer 확인 절차: 빌드 전 `Grep "MonthlyAttdReqResult"` 로 resultType 사용처가 selectMonthlyAttdReq 하나임을 재확인(신규 statement 가 추가됐을 가능성 차단).

### ⚠️ 위치매핑 경고 (이 record 기존 함정)
- record 는 이미 `schCd` → `tgt*5` → `cur*5` 가 SELECT 끝 순서와 1:1 위치바인딩(주석 79~83/988~992). 신규 3필드는 그 **뒤**(record 최종 `curSecEndTime` 다음, SELECT 최종 `curSecEndTime` 컬럼 다음)에 **정확히 동일 순서**로 붙인다. 중간 삽입 절대 금지(전 필드 밀림 → DATETIME→String 류 런타임 폭발).
- 필드 순서/컬럼 순서 동일성: `…curSecEndTime, leaveDays, useUnitType, unitNm` (record) ≡ `…AS curSecEndTime, …AS leaveDays, …AS useUnitType, …AS unitNm` (SELECT).

### 보안/스코프
- 신규 JOIN 은 CMPNY_CD 동일성으로 묶고 기존 WHERE 사업장/사용자/일자 스코프 + 05/06 결재자 EXISTS 가드를 그대로 둔다(IDOR 불변). U 조인이 타 사용자 사용기록을 끌어오지 않도록 `U.REQ_ID=A.REQ_ID`(05)/`U.LEAVE_ID=A.TARGET_ID`(06) 로 REQ 1:1 고정 — 미러 원본과 동일.

### 선행 작업
- 없음(병렬). 단 앱 B(`POST /appApi/leaveflow/apply`)의 LEAVE_DAYS/USE_UNIT_TYPE 저장 컨벤션과 정합해야 함 → 이미 web `insertLeaveReq`/`insertLeaveUse` 와 동일 컬럼·동일 SYS025 단위라 정합 보장됨(B 가 web submitLeave 미러이므로).

### 우선순위 근거
법적 책임영역(attd) +1 격상. 결재자 표시 정확성(무엇을 승인하는지) 직결.

---

## prafta-app-018-D-2 [frontend-screen] AttdDayDetailPop 연차 카드 분기 + 표시

- **유형**: frontend-screen (보완) — 기존 화면 보완이므로 신규 UI 명세 없이 reqCards 분기 + 템플릿 블록만.
- **영역**: web
- **모듈**: attd/attd07
- **화면 위치**: `prafta-web-frontend/src/views/attd/popup/AttdDayDetailPop.vue`
- **요구사항 요약**: 05/06 요청을 `mode:"leave"` 로 분리해 단위·(시간차 범위)·차감일수를 한 줄/그리드로 표시. 출근/퇴근 BEFORE-AFTER 모델 제거.

### 핵심 요구사항
1) `reqCards` computed(1573~1632행)에 **연차 분기 추가**. 분기 순서: `sched(10)` → **`leave(05/06)`(신규)** → `else(time, 01~04)`.
   - 조건: `req.reqType === '05' || req.reqType === '06'` (TARGET 유무 무관).
   - 정규근태(`r[act{n}*]`)를 **끌어오지 않음**(BEFORE 혼입 금지).
   - 산출 필드:
     - `unitCode` = `req.useUnitType`('00'~'04', null 가능)
     - `leaveTypeLabel`: `req.unitNm`(백엔드 SYS025 라벨)을 그대로 사용. (백엔드가 라벨을 내려주므로 FE 매핑 테이블 불필요. unitNm 없으면 '연차' fallback.)
       - 목표 포맷의 "시간차 N시간"은 SYS025 라벨이 이미 "2시간"/"1시간"/"30분" 이므로 그대로 사용 가능. (사용자 예시 "시간차 1시간"의 '시간차' 접두는 선택 — §아래 표시 규약 참조.)
     - `isTimed` = `['02','03','04'].includes(unitCode)` (시간차 여부)
     - `timeRange`: `isTimed` 면 `${fmtTime(req.startTime)}~${fmtTime(req.endTime)}`, 아니면 null
     - `leaveDaysLabel`: `${normalizeDays(req.leaveDays)}일 차감` (정규화 함수는 2 참조)
   - `base`(raw/reqId/reqType/reqStatus/insertDate/approvalStep/reqReason) 는 공통 유지 → 승인/반려 버튼·사유보기·결재라우팅 회귀 없음.
2) **차감일수 정규화 헬퍼** 추가(순수 표시 함수, 비즈니스 로직 아님 — UI 포맷):
   - 입력 `'1.00000'`/`'0.12500'`/`'0.5'`/null → 출력 `'1'`/`'0.125'`/`'0.5'`/`''`.
   - 규칙: `Number(v)` 변환 후 `parseFloat(n.toFixed(5))` 로 trailing 0 제거. (예: 1→'1', 0.125→'0.125', 0.5→'0.5'). NaN/null → '' (그 경우 카드에서 leaveDaysLabel 숨김).
   - 위치: 기존 `fmtTime`/`fmtInsertDate` 류 표시 헬퍼 곁(script setup 상단 유틸 영역). 골격에 함수 시그니처+본문(순수 포맷) 작성 허용.
3) **템플릿**(`req-diff` 영역, 209~270행)에 `card.mode === 'leave'` 분기 추가:
   - 위치: `v-if="card.mode==='sched'"` 와 기존 `v-else`(time) **사이**에 `v-else-if="card.mode==='leave'"` 블록.
   - 출근/퇴근 행 대신 연차 전용 1줄 그리드:
     ```
     [연차사용] · {leaveTypeLabel} · ({timeRange} 있으면) · {leaveDaysLabel}
     ```
     - 라벨 머리 "연차사용"은 `card.reqTypeNm`(SYS032 05='연차사용'/06='연차수정') 사용 → 06 은 "연차수정"으로 표기(정확). (목표 예시는 05 기준 "연차사용". reqTypeNm 사용이 더 정확.)
     - `timeRange` 는 `v-if="card.timeRange"` 로 종일/반차 시 미출력(시각 공란 문제 해결).
     - `leaveDaysLabel` 은 `v-if="card.leaveDaysLabel"` 로 0/빈값 시 미출력.
   - 클래스: 신규 `.req-leave-line`(또는 기존 `.req-diff-row` 재활용) — CSS 변수만, scoped 유지. 가운데점 `·` 구분.
4) **회귀 방지 지침**(템플릿/computed 양쪽):
   - 10(sched): `v-if="card.mode==='sched'"` 최우선 분기 불변.
   - 01/02(보정)·03/04(OT): `v-else`(time) 블록 불변 — 05/06 이 더 이상 이 분기로 안 떨어지므로 BEFORE/AFTER 출퇴근 표시 그대로.
   - 승인/반려 버튼(283~300행)·사유보기(272~281행)·`fnApproveReq`/`fnRejectReq` 라우팅: `base` 공통 유지로 무변경. 연차 결재 라우팅(approvalStep)도 base 에 남아 회귀 없음.

### 표시 규약 결정 (사용자 목표 포맷 vs SYS025 라벨)
- 사용자 목표: `연차사용 · 시간차 1시간 · 09:00~10:00 · 0.125일 차감`.
- SYS025 라벨은 "1시간"(접두 '시간차' 없음). 두 안:
  - **(채택) A안**: `{reqTypeNm} · {unitNm} · {timeRange?} · {days}일 차감` → `연차사용 · 1시간 · 09:00~10:00 · 0.125일 차감`. (백엔드 라벨 단일출처, 가공 최소. 시간차임은 timeRange 동반으로 자명.)
  - B안(목표 문구 100% 일치): 시간차(02/03/04)일 때만 FE 가 `시간차 ${unitNm}` 접두. → `시간차 1시간`. 종일/반차는 unitNm 그대로.
- **developer 가 B안(목표 문구 일치)으로 구현**하되, 접두는 FE 단순 분기(`isTimed ? '시간차 '+unitNm : unitNm`)로 처리. (사용자 확정 포맷 우선.) 종일='종일', 반차='반차', 시간차=`시간차 N시간/30분`.

### 영향 파일
- `prafta-web-frontend/src/views/attd/popup/AttdDayDetailPop.vue` (reqCards computed +leave 분기, 표시 헬퍼 normalizeDays, 템플릿 req-diff +leave 블록, scoped CSS +1 클래스)

### 백엔드 의존
- D-1 신규 필드 `useUnitType`/`unitNm`/`leaveDays` (POST 아님, 기존 일자상세 조회 응답 reqList 에 포함). endpoint 불변(`selectMonthlyAttdReq` 경유 GET 일자상세).

### 선행 작업
- prafta-app-018-D-1 (BE 필드 선행). 단 FE 분기 자체는 필드 없이도 NULL-safe 로 작성 가능 → 병렬 작성 후 통합.

### 우선순위 근거
attd +1 격상. D-1 후속.

---

## 3. 수용 기준 / 엣지

### 기능
- [시간차] 02/03/04 연차: `연차사용 · 시간차 N시간(또는 30분) · {시작}~{종료} · {N}일 차감` 정확 표시. timeRange 표시됨.
- [종일] 00: `연차사용 · 종일 · 1일 차감`. timeRange **미표시**(공란 아님). leaveDays 1.00000→'1'.
- [반차] 01: `연차사용 · 반차 · 0.5일 차감`. timeRange 미표시.
- [06 연차수정] reqTypeNm='연차수정'으로 머리라벨, 나머지 동일(U.LEAVE_ID=TARGET_ID 조인으로 단위/일수 표시).

### 회귀 (필수 무영향)
- 01/02 근태보정·03/04 OT: 기존 출근/퇴근 BEFORE-AFTER 카드 그대로(time 분기). 03(OT생성) BEFORE '-' 유지.
- 10 스케줄수정: sched 분기 그대로.
- 승인/반려 버튼·월마감 disabled·사유보기·연차 결재 라우팅(approvalStep) 그대로.
- `selectMonthlyAttdReqSummary`(월 목록 카운트)·`selectDailyLeaveApprovalHistory`(이력) 무영향(다른 resultType).

### 엣지
- [정규근태 미혼입] 05/06 카드에 act{n}In/Out 절대 미표시(time 분기 진입 안 함). 결재자가 무관한 출퇴근 시각 안 봄.
- [차감일수 0/null] leaveDays NULL 또는 0 → leaveDaysLabel '' → 라벨 숨김(빈 "·  · 일 차감" 방지). (정상 신청은 0 불가하나 방어).
- [단위 NULL] U 미매칭(사용기록 미생성 비정상 상태)으로 unitNm NULL → leaveTypeLabel '연차' fallback, timeRange 없음. 카드 깨지지 않음.
- [위치밀림 회귀] BE record +3 필드가 SELECT 끝과 순서 일치 안 하면 curSec*/tgt* 가 leaveDays 로 밀려 들어옴 → developer 빌드 후 일자상세 1건이라도 카드 렌더 확인(특히 10 sched 카드의 befSched/aftSched 가 정상인지 = 밀림 없음 증거).
- [정밀도] leaveDays decimal(예 0.12500) 문자열 그대로 와도 normalizeDays 가 0.125 로 정리. toFixed(5) 범위 내(차감일수 최대 1일대) 안전.

### 검증 메모 (developer/qa)
- BE: 빌드 통과 + selectMonthlyAttdReq resultType 사용처 단일 재확인. MCP 가용 시 DESCRIBE 로 LEAVE_DAYS/USE_UNIT_TYPE 실재·타입 재확인.
- FE: SFC/eslint 통과. 4상태(시간차/종일/반차/06) + 회귀 4종(01/02/03/04/10) 수동 1회.
