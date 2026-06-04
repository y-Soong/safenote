# PRAFTA-034 결정 문서 (단일 출처)

> Attd_11.vue 신규 화면 — **월별 사용자 근태 판정**.
> 모든 에이전트(planner / developer / security / qa)는 본 문서를 단일 출처로 참조한다.
> 원본 요청서: `.claude/requests/prafta-034.md`

## 1. 화면 개요

- 화면 ID: `Attd_11` (MENU_M_ID = `attd`)
- 화면명: **월별 사용자 근태 판정**
- 성격: **읽기 전용 조회 화면**. 한 달 기준으로 **사용자 1명당 1행**, 월간 근태 종합 지표를 표(grid)로 표시.
- 조회 조건: 기간(**단일 월**), 사업장, 소속부서(+하위부서 조회 체크), 사용자명

## 2. 표시 컬럼 (사용자 1명 = 1행)

| 구분 | 컬럼 | 설명 |
| --- | --- | --- |
| 사용자정보 | 사번(USER_ID) / 이름 / 부서 / 직책 | Attd_07 패턴 |
| 근무일수 | workDayCnt | 해당 월 출근 실적이 1건이라도 있는 **날 수** |
| 총 근무시간 | workMinutes → "N시간 M분" | 정규 근무시간 합(휴게 공제, **초과근무 제외**) |
| 초과근무시간 | otMinutes → "N시간 M분" | COMPLETED 초과근무 분 합 |
| 지각 횟수 | lateCnt | 지각 판정된 출근 건수 |
| 지각 시간 누계 | lateMinutes → "N시간 M분" | 지각 분 합 |
| 조퇴 횟수 | earlyLeaveCnt | 조기퇴근 판정된 퇴근 건수 |
| 조퇴 시간 누계 | earlyLeaveMinutes → "N시간 M분" | 조퇴 분 합 |

## 3. 확정 결정 (사용자 승인 2026-05-27)

1. **지각/조퇴 판정 시각 = 실제(원본) 출퇴근 시각 기준.** 표준화 시각이 아니라 `TB_USER_ATTD_MGMT.CHECK_IN_TIME / CHECK_OUT_TIME`(+DATE) 원본을 사용한다. (기존 Attd_08 `computeStatus` 로직과 동일 기준)
2. **총 근무시간 = 정규 근무시간만** (출근~퇴근 − 휴게시간, **초과근무 제외**). 시각 기준은 1번과 동일하게 실제 시각.
3. **근무일수 = 해당 월 출근 기록(CHECK_IN_TIME)이 존재하는 날의 수.** 휴가일/휴무일 미포함. (출근 기록 존재 기준 — 출+퇴 모두여야 하는 "정상 완료일" 아님)
4. **초과근무 집계 대상 = `OT_STATUS='COMPLETED'` 만** 합산. IN_PROGRESS·CANCELLED 제외, `DEL_YN='N'`.
5. **조회 기간 = 단일 월.** Attd_07 처럼 월 선택 1개(workYm `YYYY-MM`). from~to 월 범위 아님.

## 4. 산출 정의식 (developer 구현 기준)

> 모든 시각 비교/연산은 **날짜+시각(일시, YYYYMMDDHHmm)** 기준으로 자정 넘김(야간근무)을 보정한다. `CHECK_IN_DATE/CHECK_OUT_DATE` 컬럼을 사용. 스케줄 종료시각이 시작시각보다 이르면 종료는 익일로 본다(Attd_08 패턴).

- **차수(WORK_SEQ) 단위 집계**: 한 날에 1구간/2구간이 모두 있으면 각 구간(차수)을 독립 건으로 카운트한다. 단일 구간 사용자는 = 일 단위와 동일.
- **근무일수**: `CHECK_IN_TIME IS NOT NULL` 인 `(USER_CD, WORK_YMD)` 의 **distinct WORK_YMD 개수** (차수 무관, 날 단위).
- **총 근무시간(분)**: 출근·퇴근이 **모두 존재**하는 각 attendance 행에 대해 `(퇴근일시 − 출근일시) − BREAK_MIN`(스케줄 휴게, `plan{1,2}BreakMin`) 의 합. 음수는 0 처리.
- **초과근무시간(분)**: `TB_USER_OVERTIME_MGMT` 에서 `OT_STATUS='COMPLETED' AND DEL_YN='N'` 인 `WORK_MINUTES` 합 (해당 월 WORK_YMD).
- **지각**: 출근 기록(CHECK_IN)이 있고 스케줄 시작(plan start)이 있을 때, `실제출근일시 > 스케줄시작일시` 면 지각.
  - 지각 횟수 += 1, 지각 분 += (실제출근일시 − 스케줄시작일시) 분.
- **조퇴**: 퇴근 기록(CHECK_OUT)이 있고 스케줄 종료(plan end)가 있을 때, `실제퇴근일시 < 스케줄종료일시` 면 조퇴.
  - 조퇴 횟수 += 1, 조퇴 분 += (스케줄종료일시 − 실제퇴근일시) 분.
- 스케줄(계획시각)이 없는 날(스케줄 없는 자율 출근)은 지각/조퇴 판정 제외.

## 5. 데이터 소스 (스키마 확인 완료)

- `TB_USER_ATTD_MGMT` — 출퇴근 실적. WORK_SEQ(1·2차), CHECK_IN_DATE/TIME, CHECK_OUT_DATE/TIME, DEL_YN. `A.WORK_SEQ=1/2`, `DEL_YN='N'`.
- `TB_USER_WORK_PLAN` + `TB_SCH_MGMT`(+`TB_SCH_MGMT_HIST`) — 근무계획→스케줄 시간(plan start/end, break). Attd07Mapper `selectMonthlyAttdList` 의 effective-dating 조인 패턴 재사용.
- `TB_USER_OVERTIME_MGMT` — 초과근무. `WORK_MINUTES`(분, 휴게 제외), `OT_STATUS`(IN_PROGRESS/COMPLETED/CANCELLED), `OT_TYPE`, DEL_YN.
- `TB_SITE_NODE` — 부서 트리(하위부서 RECURSIVE), Attd07 `node_tree` CTE 재사용.
- `TB_USER` — 대상 사용자(USE_YN='Y', WITHDRAWAL_DATE IS NULL), USER_NM LIKE 검색.

## 6. 참조 화면 / 패턴

- **Attd_07 (`views/attd/Attd_07.vue`, 백엔드 `attd07` 패키지)** — 조회영역(사업장/소속부서/하위부서/사용자명), 월 네비, node_tree CTE, effective-dating 스케줄 조인, PRAFTA-028 권한 게이팅을 차용.
- **Attd_08 (`views/attd/Attd_08.vue`)** — `computeStatus()` 의 지각/조퇴 일시 비교 로직(실제 시각 기준, 자정 보정) 차용.

## 7. 권한 / 게이팅 (PRAFTA-028 동일)

- `gv_authCd` 가 `master` / `hr` 이면 사업장만으로 조회 가능(부서 선택 불필요).
- 그 외 권한은 **사업장 + 소속부서 필수**. 부서 미선택 시 조회 차단(alert).
- 본 화면은 읽기 전용 → 근태 마감 버튼/쓰기 없음.

## 8. 신규 화면 등록 (마이그레이션 필요)

- `tb_syst_menu_d`: MENU_D_ID=`Attd_11`, MENU_M_ID=`attd`, MENU_VIEW=`attd/Attd_11.vue`, MENU_NM=`월별 사용자 근태 판정`, MENU_IDX=11, USE_YN=`Y`.
- `tb_syst_auth_menu`: 기존 Attd_07/Attd_08 권한 매핑과 동일 권한 집합으로 부여.
- 마이그레이션 파일: `.claude/migrations/prafta-034-001.sql` (운영 미적용, 사용자 확인 후 반영).
- 라우팅: viewResolver 가 컴포넌트명으로 자동 로드(별도 router 등록 불필요).

## 9. API (제안)

- `GET /attd11/monthly-attd-summary` (프론트: `/webApi/attd11/monthly-attd-summary`)
- 파라미터: `workYm`(YYYY-MM), `siteCd`, `nodeCd`, `incSubNodeYn`(Y/N), `userNm`
- 응답: `{ monthlyAttdSummaryResultList: [ { userCd, userId, userNm, deptNm, authCd, authNm, workDayCnt, workMinutes, otMinutes, lateCnt, lateMinutes, earlyLeaveCnt, earlyLeaveMinutes } ... ] }`
- 백엔드 패키지: `com.prafta.web.attd.attd11.*` (attd07 레이어 구조 동일: controller/service/service.impl/mapper/dto.request/dto.response/application.param/application.query/result)
