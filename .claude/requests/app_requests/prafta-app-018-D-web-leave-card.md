# prafta-app-018-D — [웹 FE+BE] 일자상세 팝업 연차 요청 카드 표시 정정

상위: `prafta-app-018-leave-apply-plan.md`. **웹 영역**(app 작업이지만 app-018 자가 연차신청의 **웹 승인 측 짝**). app-018-B(앱이 05 요청 생성)와 짝 — **앱 자가 연차신청 활성화 전/함께 완료 권장**(결재자가 무엇을 승인하는지 제대로 보이게).

## 배경/문제
근무관리(Attd_07) 셀 더블클릭 → `AttdDayDetailPop.vue` 의 요청 카드가 연차(REQ_TYPE 05 연차사용/06 연차수정)를 **"출퇴근 시각 BEFORE/AFTER" 모델**(`mode:"time"`, reqCards 약 1617~1630행 / 템플릿 232~269행)로 표시한다. 결과:
- 시간차 연차: 연차 시작~종료가 "출근/퇴근"으로 **오라벨**, 단위/차감일수 표시 없음, BEFORE에 무관한 정규근태(act{n}*) 노출.
- 종일/반차 연차: START_TIME/END_TIME NULL → 출근 -/퇴근 - **완전 공란**(종일/반차·차감일수 안 보임).
원인 ① 백엔드 `MonthlyAttdReqResult`(+`selectMonthlyAttdReq`)에 `LEAVE_TYPE`(사용단위 계열)·`LEAVE_DAYS`(차감일수)가 **없음**(TB_USER_ATTD_REQ에는 존재). ② 프론트 `reqCards`/템플릿에 연차 전용 분기 없음.

## 목표
연차(05/06) 요청 카드를 **연차 전용 표시**로 정정. 예시 포맷(사용자 확정):
> `연차사용 · 시간차 1시간 · 09:00~10:00 · 0.125일 차감`
- 종일/반차: 시각 없이 `연차사용 · 종일 · 1.0일 차감` / `연차사용 · 반차 · 0.5일 차감`.
- 시간차: `… · 시간차 N시간(또는 30분) · {시작}~{종료} · {차감일수}일 차감`.

## 구현

### BE (web attd07)
- `MonthlyAttdReqResult` record + `Attd07Mapper.xml#selectMonthlyAttdReq` SELECT 에 연차 표시용 컬럼 추가:
  - `LEAVE_TYPE`(TB_USER_ATTD_REQ.LEAVE_TYPE — 'ANNUAL'/'HALF'/'HOUR' 계열), `LEAVE_DAYS`(decimal(8,5) 차감일수).
  - 정밀 단위 라벨("시간차 1시간"의 "1시간")·연차종류명이 필요하면 `TB_USER_LEAVE_USE`(REQ_ID 조인)의 `USE_UNIT_TYPE`(SYS025) 및 `TB_LEAVE_TYPE_MGMT.LEAVE_NM` 조인 검토. 단순화 가능하면 LEAVE_TYPE + (시간차 시작~종료 차이)로 단위 라벨 유도도 허용 — planner가 스키마 확인 후 결정.
  - ⚠️ **MyBatis record 위치매핑**: 신규 컬럼은 SELECT 끝·record 끝 순서를 정확히 일치(이 record는 tgt*/cur*에서 이미 같은 함정 경고 있음). 타 호출자 회귀 주의.
  - 식별값 JWT/스코프 가드 기존 유지. `SELECT *` 금지·leading 콤마·`#{}`.

### FE (`AttdDayDetailPop.vue`)
- `reqCards`: 05/06 을 신규 `mode:"leave"` 분기로 분리(현재 01~06 통합 분기에서 제외). 정규근태(act{n}*) 끌어오지 않음. 필드: `leaveTypeLabel`(종일/반차/시간차 N), `timeRange`(시간차면 `시작~종료`, 그 외 null), `leaveDaysLabel`(`{LEAVE_DAYS}일 차감`).
- 템플릿: `card.mode==='leave'` 전용 카드 블록 추가(출근/퇴근 행 대신 위 항목 1줄/그리드). 스케줄수정(10)·근태보정(01/02)·OT(03/04) 카드 **회귀 금지**.
- 차감일수 포맷: 불필요한 0 정리(예 0.12500→0.125), 1.00000→1.0 등 표시 정규화.

## 수용 기준
- 시간차/종일/반차 연차 카드가 단위·(시간차 범위)·차감일수를 정확히 표시. 결재자가 무엇을 승인하는지 명확.
- 정규근태가 연차 카드 BEFORE에 섞이지 않음. 타 요청타입 카드 회귀 없음.
- BE 신규 컬럼 위치매핑 정확(타 필드 밀림 없음), 빌드 통과. FE SFC/eslint 통과.
- 식별값 JWT 출처·스코프 가드 유지.

## 의존/병렬
- 파일 집합이 앱 작업(A/B/C: app-frontend·app BE req07/leaveflow)과 **겹치지 않음**(D는 web attd07 + web-frontend). 병렬 진행 가능.
- 단 LEAVE_TYPE/LEAVE_DAYS 의미는 B의 신청 INSERT 결과(TB_USER_ATTD_REQ 컬럼값)와 정합해야 하므로, B의 저장 컨벤션(LEAVE_TYPE 값 집합·LEAVE_DAYS 정밀도)을 참조해 맞춘다.

## 정책 출처
attd §8/§8.5(사용단위·차감), §9(결재 표시). prafta-019(시간차/LEAVE_DAYS decimal), prafta-024(사용단위). 화면 규약(planner UI 명세 → script).
