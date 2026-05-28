# PRAFTA-034 UI 명세 + 작업 분해 (Attd_11 월별 사용자 근태 판정)

> 단일 출처: `.claude/requests/prafta-034-decisions.md`
> 본 문서는 planner 산출물(UI 명세 + developer 핸드오프 작업 분해)이다.
> 비즈니스 룰/정의식은 decisions.md 를 그대로 따르며, 새로운 룰을 추가하지 않는다.

---

## 0. Notion 등록 안내 (메인 세션이 처리)

> subagent 는 Notion 접근을 하지 않는다. 메인 세션이 아래 내용으로 등록한다.

### "작업 로그" DB (작업 단위 — 아래 §3 작업 분해의 각 행을 그대로 등록)

- 작업ID: `PRAFTA-034-1` ~ `PRAFTA-034-4` (통합 채번은 메인 세션에서 최대 ID 확인 후 조정)
- 영역: web / 모듈: attd/attd11 / 담당 에이전트: planner / 상태: 분해완료

### "도메인 지식 베이스" DB (화면 명세)

| 컬럼 | 값 |
| --- | --- |
| 이름 | `UI-034 Attd_11 월별 사용자 근태 판정` |
| 영역 | web |
| 모듈 | attd |
| 현재 동작 | 신규 작성 |
| 의도된 동작 | 본 문서 §1~§2 (화면 명세) 전체 |
| 검증 상태 | Claude 분석 |
| 알려진 이슈 | (없음) |

> 산출물 경로: `prafta-web-frontend/prafta-web-frontend/src/views/attd/Attd_11.vue` (planner 가 골격 작성 완료, developer 가 script 채움)

---

## 1. 화면 명세 (UI-034)

- 화면 ID: `UI-034`
- 연결 작업: `PRAFTA-034-3` (프론트 골격/연동)
- 화면 위치: `prafta-web-frontend/prafta-web-frontend/src/views/attd/Attd_11.vue`
- 성격: **읽기 전용 조회 화면**. 한 달 기준, 사용자 1명당 1행, 월간 근태 종합 지표를 표(grid)로 표시.
- 참조 패턴:
  - 조회영역(사업장/소속부서/하위부서/사용자명 + focusKill + 자동조회 팝업) = **Attd_07 / Attd_08**.
  - 월 네비게이션(‹ › + CalendarSrchMonth, 단일 월) = **Attd_07 a07-nav**.
  - 결과 테이블(2단 헤더, sticky thead, 빈 결과 행) = **Attd_08 a08-table**.
  - 시간 포맷("N시간 M분") = **Attd_07 fmtMinutes**.

### 1-1. 화면 영역 구성

```
┌──────────────────────────────────────────────────────────────────────────┐
│ ViewHeader   "월별 사용자 근태 판정"                          [ 조회 ]      │  ← @search=fnSearch
├──────────────────────────────────────────────────────────────────────────┤
│ 조회영역 (viewSearch)                                                       │
│  기간    [‹] [ 2026-05 ▾ ] [›]   ← 단일 월 (decisions §3-5)                 │
│  사업장  [코드][🔍][사업장명]                                                │
│  소속부서 [코드][🔍][부서명]   ☐ 하위부서 조회                              │
│  사용자명 [        ]                                                        │
├──────────────────────────────────────────────────────────────────────────┤
│ 본문 (viewBody) — 사용자 1명 = 1행                                          │
│ ┌────┬────┬────┬────┬──────┬─────────┬─────────┬──────┬──────────┬──────┬──────────┐
│ │사번│이름│부서│직책│근무  │총 근무  │초과근무 │지각      │지각      │조퇴   │조퇴      │
│ │    │    │    │    │일수  │시간     │시간     │횟수      │시간 누계 │횟수   │시간 누계 │
│ ├────┼────┼────┼────┼──────┼─────────┼─────────┼──────┼──────────┼──────┼──────────┤
│ │... │홍길│개발│대리│ 21   │168시간0분│12시간30분│ 2    │ 0시간45분│ 1    │ 0시간20분│
│ └────┴────┴────┴────┴──────┴─────────┴─────────┴──────┴──────────┴──────┴──────────┘
└──────────────────────────────────────────────────────────────────────────┘
```

### 1-2. 컬럼 정의 (decisions §2 표 그대로)

| # | 컬럼 | 응답 필드 | 표기 | 정렬 |
| --- | --- | --- | --- | --- |
| 1 | 사번 | `userId` (USER_ID) | 텍스트 | 가운데 |
| 2 | 이름 | `userNm` | 텍스트 | 좌측 |
| 3 | 부서 | `deptNm` | 텍스트 | 좌측 |
| 4 | 직책 | `authNm` | 텍스트 | 가운데 |
| 5 | 근무일수 | `workDayCnt` | 숫자 | 우측 |
| 6 | 총 근무시간 | `workMinutes` | "N시간 M분" | 우측 |
| 7 | 초과근무시간 | `otMinutes` | "N시간 M분" | 우측 |
| 8 | 지각 횟수 | `lateCnt` | 숫자 | 우측 |
| 9 | 지각 시간 누계 | `lateMinutes` | "N시간 M분" | 우측 |
| 10 | 조퇴 횟수 | `earlyLeaveCnt` | 숫자 | 우측 |
| 11 | 조퇴 시간 누계 | `earlyLeaveMinutes` | "N시간 M분" | 우측 |

- 헤더는 2단: 1단(사번/이름/부서/직책/근무일수/총 근무시간/초과근무시간 = rowspan, 지각 colspan2, 조퇴 colspan2), 2단(지각·조퇴 각각 횟수/시간 누계).
- 시간 컬럼 표기 규칙: `fmtMinutes(분)` → `${h}시간 ${rm}분`. 0/빈값은 `0시간 0분`. (Attd_07 의 `fmtMinutes` 와 동일 표기, 단 Attd_07 은 0 일 때 빈문자를 반환하므로 본 화면 헬퍼는 0 표기로 별도 정의함.)
- 횟수/일수는 정수 표기, 빈값은 0.

### 1-3. 상태별 동작

| 상태 | UI |
| --- | --- |
| loading | (골격 단계 미구현) developer 가 필요 시 조회 버튼 비활성 또는 안내. 현재 골격은 별도 로딩 표시 없음. |
| empty (0건) | `<tr>` 1행 `colspan=11` "조회 결과가 없습니다." (Attd_08 a08-empty 패턴) |
| error | developer 가 `resolveApiErrorMessage` 로 `proxy.$alert` 노출 (Attd_07/08 패턴) |
| success | 사용자 수만큼 행 렌더링 |

### 1-4. 권한 동작 (PRAFTA-028 / decisions §7)

- `gv_authCd` 가 `master` / `hr` → 사업장만으로 조회 가능(부서 선택 불필요).
- 그 외 권한 → 사업장 + 소속부서 필수. 부서 미선택 시 조회 차단(alert "소속 부서를 선택해 주세요.").
- 사업장 미선택 시 조회 차단(alert + 사업장 입력란 focus).
- 본 화면은 읽기 전용 → 생성/저장/삭제/엑셀 버튼 모두 숨김(`fnButtonControll`). 근태 마감/쓰기 동작 없음.

### 1-5. 사용자 플로우

진입(onMounted → fnInit: 세션 사업장/부서 복원) → 월/사업장/부서/사용자명 입력 → [조회] 클릭(@search) → fnSearch(권한 가드 → GET /webApi/attd11/monthly-attd-summary) → rows 바인딩 → 사용자 1명=1행 표 렌더(또는 0건 행).

### 1-6. 반응형

- Attd_07/08 과 동일하게 데스크탑 관리자 화면 전제. 별도 모바일 break point 없음. 가로 넓은 표는 `.a11-table-wrap` overflow:auto 로 가로 스크롤 처리.

### 1-7. 백엔드 의존

- `GET /webApi/attd11/monthly-attd-summary` ← `PRAFTA-034-1`(백엔드) 산출.
- 사업장 자동조회: `GET /comApi/baseinfo/site-lists` (기존, 신규 아님).
- 부서 자동조회: `GET /comApi/baseinfo/site-node-lists` (기존, 신규 아님).

---

## 2. 데이터/정의 요약 (developer 구현 시 decisions §4·§5 정독 필수)

> 아래는 핸드오프 편의용 요약이며, **확정 정의식은 decisions.md §4 가 단일 출처**다.

- **근무일수** = `CHECK_IN_TIME IS NOT NULL` 인 `(USER_CD, WORK_YMD)` 의 **distinct WORK_YMD 개수**(차수 무관). `DEL_YN='N'`.
- **총 근무시간(분)** = 출근·퇴근 모두 존재하는 각 attendance 행에 대해 `(퇴근일시 − 출근일시) − 스케줄 휴게(BREAK_MIN)` 합. 음수 0 처리. 초과근무 제외.
- **초과근무시간(분)** = `TB_USER_OVERTIME_MGMT` 에서 `OT_STATUS='COMPLETED' AND DEL_YN='N'` 인 `WORK_MINUTES` 합(해당 월 WORK_YMD).
- **지각** = 출근(CHECK_IN) 있고 스케줄 시작(plan start) 있을 때 `실제출근일시 > 스케줄시작일시`. 횟수 +1, 분 += 차이.
- **조퇴** = 퇴근(CHECK_OUT) 있고 스케줄 종료(plan end) 있을 때 `실제퇴근일시 < 스케줄종료일시`. 횟수 +1, 분 += 차이.
- 스케줄 없는 날은 지각/조퇴 판정 제외.
- 모든 시각 비교/연산은 **날짜+시각(일시, YYYYMMDDHHmm)** 기준, 자정 넘김(야간) 보정. 스케줄 종료 < 시작이면 종료는 익일(Attd_08 `computeStatus` 패턴).

### 스키마 확인 결과 (schema-full.sql, 추측 아님)

- `tb_user_attd_mgmt`: PK=`ATTD_ID`. 컬럼 = `CMPNY_CD, SITE_CD, USER_CD, WORK_YMD(varchar8), NODE_CD, WORK_SEQ(int), CHECK_IN_DATE(varchar8), CHECK_IN_TIME(varchar4), CHECK_OUT_DATE, CHECK_OUT_TIME, DEL_YN`.
  - **한 행 = (USER_CD, WORK_YMD, WORK_SEQ) 단위**. 1구간/2구간이 각각 1행. decisions §4 "차수 단위 집계" 와 일치. 한 행에 plan1/plan2 가 동시에 들어있지 않으므로, 스케줄 조인(`TB_USER_WORK_PLAN`/`TB_SCH_MGMT`)에서 해당 차수의 시작/종료/휴게를 가져와 행별로 판정한다.
- `tb_user_overtime_mgmt`: PK=`OT_ID`. 사용 컬럼 = `USER_CD, WORK_YMD, WORK_MINUTES(int, 휴게 제외), OT_STATUS(IN_PROGRESS/COMPLETED/CANCELLED), DEL_YN`.
- `tb_syst_menu_d`: PK=(`MENU_D_ID`,`MENU_M_ID`). 컬럼 = `MENU_M_ID, MENU_VIEW, MENU_NM, MENU_IDX(int), MENU_DESC, USE_YN`.
- `tb_syst_auth_menu`: PK=(`CMPNY_CD`,`AUTH_CD`,`MENU_D_ID`). 컬럼 = `USE_YN, BTN_SRCH, BTN_NEW, BTN_DELT, BTN_SAVE, BTN_EXCL`.

> 주의: `TB_USER_WORK_PLAN`/`TB_SCH_MGMT(+HIST)` 의 effective-dating 조인 + plan start/end/break 산출은 **Attd07Mapper `selectMonthlyAttdList`** 의 패턴을 그대로 재사용한다(decisions §6). 본 planner 단계에서는 컬럼 추측을 피하기 위해 해당 매퍼를 단일 출처로 지정한다. developer 가 attd07 매퍼를 정독해 동일 조인을 가져온다.

---

## 3. developer 핸드오프 — 작업 분해

> 각 작업의 정책/근거 출처를 명시한다. 비즈니스 룰 출처는 모두 `decisions.md`(= 원본 요청 prafta-034.md + 사용자 승인) 와 PRAFTA-028(권한 게이팅)·Attd_07/08(판정/조회 패턴).

### PRAFTA-034-1 [backend] attd11 월별 근태 판정 조회 API

- 유형: backend / 영역: web / 모듈: attd/attd11 / 작업유형: 신규
- 요구사항 요약: 단일 월·사업장·(하위)부서·사용자명 기준으로 사용자 1명당 1행 월간 근태 종합(근무일수/총 근무시간/초과근무/지각·조퇴 횟수·시간)을 산출해 반환.
- 출처: decisions §2(컬럼), §4(정의식), §5(데이터 소스), §9(API/패키지). 판정 일시 비교는 Attd_08 `computeStatus`. 스케줄 조인은 Attd07Mapper `selectMonthlyAttdList`.
- 영향 받는 파일 (attd07 레이어 구조 동일 — decisions §9):
  - `com/prafta/web/attd/attd11/controller/Attd11Controller.java`
  - `com/prafta/web/attd/attd11/service/Attd11Service.java`
  - `com/prafta/web/attd/attd11/service/impl/Attd11ServiceImpl.java`
  - `com/prafta/web/attd/attd11/mapper/Attd11Mapper.java`
  - `src/main/resources/com/prafta/web/attd/attd11/mapper/Attd11Mapper.xml`
  - `com/prafta/web/attd/attd11/dto/request/MonthlyAttdSummaryRequest.java`
  - `com/prafta/web/attd/attd11/dto/response/MonthlyAttdSummaryResponse.java`
  - `com/prafta/web/attd/attd11/application/param/...Param.java` (조회 파라미터 — attd07 패턴 따름)
  - `com/prafta/web/attd/attd11/application/query/...Query.java` (조회 로직/판정 — attd07 패턴 따름)
  - `com/prafta/web/attd/attd11/application/result/...Result.java` (행 결과 객체)
- endpoint: `GET /webApi/attd11/monthly-attd-summary`
  - 파라미터: `workYm`(YYYY-MM), `siteCd`, `nodeCd`, `incSubNodeYn`(Y/N), `userNm`
  - 응답: `{ monthlyAttdSummaryResultList: [ { userCd, userId, userNm, deptNm, authCd, authNm, workDayCnt, workMinutes, otMinutes, lateCnt, lateMinutes, earlyLeaveCnt, earlyLeaveMinutes } ] }`
- 구현 주의:
  1. 부서 트리(하위부서)는 Attd07 `node_tree` RECURSIVE CTE 재사용 (`incSubNodeYn='Y'` 시 cascade). 출처: decisions §5·§6.
  2. attendance 는 `(USER_CD, WORK_YMD, WORK_SEQ)` 행 단위로 스케줄(plan start/end/break) 조인 후, 행별 지각/조퇴/근무시간 판정 → 사용자(USER_CD) 단위 집계. 근무일수는 distinct WORK_YMD. 출처: decisions §4.
  3. 지각/조퇴 시각 비교는 표준화 시각이 아니라 **원본 CHECK_IN/OUT_DATE+TIME** 기준. 야간 자정 보정(스케줄 종료<시작이면 익일). 출처: decisions §3-1, §4, Attd_08 `computeStatus`.
  4. 초과근무 합산 대상은 `OT_STATUS='COMPLETED' AND DEL_YN='N'` 만. 출처: decisions §3-4.
  5. 대상 사용자: `TB_USER` `USE_YN='Y' AND WITHDRAWAL_DATE IS NULL`, `USER_NM LIKE` 검색. 출처: decisions §5.
  6. 집계는 mapper.xml SQL 집계(GROUP BY USER_CD) 또는 행 조회 후 service 집계 중 선택 — 단, 야간 자정 보정·차수 판정이 SQL 로 복잡하면 행 조회 후 service 에서 일시 기반 판정/집계를 권장(Attd_08 이 화면단 재판정을 택한 것과 동일 이유).
- 선행 작업: 없음 (조회만).
- 우선순위 근거: 화면이 의존하는 API. 법적 책임 영역(attd) +1단계 격상. 최우선.

### PRAFTA-034-2 [backend] Attd_11 메뉴/권한 등록 마이그레이션

- 유형: backend(DDL/데이터) / 영역: web / 모듈: attd / 작업유형: 신규
- 요구사항 요약: 신규 화면 메뉴 등록 + 기존 Attd_07/Attd_08 과 동일 권한 집합 부여.
- 출처: decisions §8.
- 산출물: `.claude/migrations/prafta-034-001.sql` (운영 미적용, 사용자 확인 후 반영)
  - `tb_syst_menu_d` INSERT: MENU_D_ID=`Attd_11`, MENU_M_ID=`attd`, MENU_VIEW=`attd/Attd_11.vue`, MENU_NM=`월별 사용자 근태 판정`, MENU_IDX=11, USE_YN=`Y`.
  - `tb_syst_auth_menu` INSERT: 기존 Attd_07(또는 Attd_08) 의 (CMPNY_CD, AUTH_CD) 조합을 그대로 복제해 MENU_D_ID=`Attd_11` 로 INSERT. 읽기 전용이므로 `BTN_SRCH='Y'`, `BTN_NEW/BTN_DELT/BTN_SAVE/BTN_EXCL='N'` 권장.
- 구현 주의: `tb_syst_auth_menu` 의 실제 (CMPNY_CD, AUTH_CD) 행 목록은 schema-full.sql(DDL only)에 없으므로, developer 가 MCP(`prafta-mysql`)로 `SELECT CMPNY_CD, AUTH_CD, USE_YN, BTN_SRCH ... FROM tb_syst_auth_menu WHERE MENU_D_ID='Attd_07'` 를 조회해 동일 집합을 복제한다. 추측 금지.
- 선행 작업: 없음.
- 우선순위 근거: 화면 노출 전제. attd 영역 +1단계.

### PRAFTA-034-3 [frontend-screen] Attd_11 script 연동

- 유형: frontend-screen / 영역: web / 모듈: attd/attd11 / 작업유형: 신규
- 요구사항 요약: planner 작성 골격(template+style+상태/포맷 헬퍼 완성)에 API 호출/권한/자동조회 로직을 채운다.
- 출처: decisions §3-5(단일 월), §7(권한), §9(API). 골격 = `views/attd/Attd_11.vue` (planner 작성 완료).
- developer 가 채울 항목(골격 내 `// TODO(developer):` 위치):
  1. import: `axios`, `getMessage/MSG`, `resolveApiErrorMessage` 활성화.
  2. `fnSrchSiteInfo` / `fnSrchNodeInfo`: `/comApi/baseinfo/site-lists`, `/comApi/baseinfo/site-node-lists` GET + `fnCallback` 연결 (Attd_07 동일).
  3. `fnCallback`: site-lists / site-node-lists 응답 0/1/다건 분기 (Attd_07 동일).
  4. `fnSearch`: `GET /webApi/attd11/monthly-attd-summary` 호출 → `rows.value = response.data?.monthlyAttdSummaryResultList ?? []`. 에러 시 `resolveApiErrorMessage` alert. 사업장/부서 권한 가드(이미 골격에 분기 자리 있음, MSG 상수로 교체).
  5. alert 메시지 임시 문자열을 `MSG` 상수로 교체(`MSG.SITE_INPUT_REQUIRED`, `MSG.SITE_REQUIRED_FIRST` 등).
- 변경 금지(골격 유지): 컬럼 구조, 시간/횟수 포맷(`fmtMinutes`/`fmtCount`), 단일 월 네비, scoped style 토큰.
- 선행 작업: `PRAFTA-034-1`(API), `PRAFTA-034-2`(메뉴) — 선행되어야 화면 진입/조회 가능.
- 우선순위 근거: API/메뉴 다음.

### PRAFTA-034-4 [backend] (qa/security 핸드오프 메모 — 작업 아님, 참고)

- 본 화면은 PII(이름) 를 표시하므로 security/qa 가 응답 직렬화 시 평문 이름 로깅 금지를 점검(공통 정책). 단, 화면 표기는 정상.
- decisions §4 정의식 검증(지각/조퇴 분 합, 야간 자정 보정, 차수 단위)을 qa 가 테스트 케이스로 검증.
- (이 항목은 정식 작업 채번 대상 아님 — 메인 세션 판단에 위임.)

---

## 4. planner 자체 점검

- 골격은 template + scoped style 완성, script 는 상태/포맷/월네비/팝업 핸들러 자리 + `// TODO(developer):` 만 둠. API/store/router 본문 없음. (규칙 준수)
- 색상/폰트/간격: tokens.css 변수만 사용(`--color-*`, `--thead-bg` fallback 포함). 하드코딩 없음. `!important` 없음. `<style scoped>`. TypeScript 미사용. (규칙 준수)
- 공통 컴포넌트 사용: ViewHeader, CalendarSrchMonth, SiteSearchPop, SiteNodeSearchPop. 사업장/부서/사용자명 input 은 Attd_07/08 과 동일하게 native input 사용(해당 화면군의 확립된 조회영역 패턴 — 신규 컴포넌트화 대상 아님).
- decisions.md 밖의 비즈니스 룰 추가 없음.
