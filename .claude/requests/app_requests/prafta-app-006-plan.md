# prafta-app-006 — 모바일 앱 "내 승인 요청" 화면 분해 plan

> **작업 ID prefix**: `PRAFTA-APP-006`
> **단일 출처 (SSOT) 선언**: 본 plan 이 prafta-app-006 의 단일 출처이다. 후속 developer / qa / security 는 본 plan 만 정독하면 된다.
>
> **폐기 대상 (절대 다시 정독 금지)**:
> - `.claude/requests/app_requests/refs/prafta-app-006/prafta-request-my-requests.md` — 4종 통합 표시·SCHED_MODIFY·REJECT_REASON 컬럼·CANCELED 추가·`/api/app/req/...` 경로·취소 endpoint·요청 상세 화면 가정이 prafta 실제 시스템과 어긋남.
> - 동 디렉토리 `prafta_my_requests.html` 시안 — 시각 톤(여백·라운드·배지 색감·empty 일러스트)만 참조용. 7종 케이스 중 "스케줄 수정 / 취소 액션" 케이스는 폐기.
>
> 본 plan 과 원본 요청서가 충돌하면 **본 plan 이 무조건 우선**한다.

---

## 0. 개요

### 0.1 배경

PRAFTA 모바일 앱 메인 홈 `MainView.vue` 의 "근태 조회 카드 > 승인 요청 N건 대기" KPI 를 클릭했을 때 진입할 신규 화면. 현재 `onApprovalClick` 핸들러는 `showAlert('준비 중입니다')` 스텁이다 (`MainView.vue` L462).

본 화면은 **본인이 등록한 6종 근태 요청 (`tb_user_attd_req`) 의 전체 이력**을 단일 리스트로 조회한다.

### 0.2 본 작업의 실제 범위

- **백엔드 1개 endpoint 신규**: `GET /appApi/req06/my` (패키지 `com.prafta.app.req.req06`, 매핑 `/req06/my`). 본인 요청 목록 페이지 조회.
- **프론트엔드 1개 화면 + 8개 컴포넌트 신규** (`prafta-app-frontend/src/views/req/`).
- **라우트 1개 신설** (`/MyRequests`, PascalCase, 비-public).
- **메인 홈 진입 동선 연결** (`MainView.vue` 의 `onApprovalClick` 핸들러 한 줄 교체).

### 0.3 본 작업의 실제 비범위 (폐기·미구현)

- **DB 스키마 변경 0건** (REJECT_REASON·PROCESSED_AT·CANCELED 모두 이미 존재).
- **사용자 직접 취소 endpoint 미신설** (취소 상태값만 표시).
- **카운트 endpoint 미신설** (`MainView` 가 이미 `home-summary.approval.pendingCount` 사용 — 중복 제거).
- **요청 상세 화면 미작성** (목록 카드의 "자세히 >" 진입은 §7 follow-up).
- **SCHED_MODIFY 미노출** (REQ_TYPE 부재 / 등록 endpoint 부재).
- **알림센터 / 푸시 진입 동선 미작성** (`prafta-031` outbox consumer 미구현 상태).

### 0.4 사용자 1차 컨펌 (자율 진행 모드)

본 plan 은 `prafta-037-F5-plan.md` 와 동일하게 사용자 컨펌 없이 한 번에 작성한다. 결정 포인트는 §1.B planner 결정으로 명시했다. 메인 세션이 본 plan 본문을 디스크에 옮긴 뒤 그대로 Notion "작업 로그" 에 일괄 등록한다 (planner 는 Notion 직접 접근 불가 — `feedback_subagent_no_resume.md`).

---

## 1. 결정 사항

### 1.A 사용자 확정 결정 (2026-05-29 채팅)

| # | 항목 | 결정 |
|---|---|---|
| **Q1** | REQ_TYPE 표시 단위 | **SYS032 6종 그대로** (`근태 생성/근태 수정/초과근무 생성/초과근무 수정/연차 사용/연차 수정`). 4종 통합(스케줄/근태/OT/연차) 불채택. 카드 상단 유형 라벨도 6종. 필터도 6종 다중 선택. |
| **Q2** | SCHED_MODIFY | **본 화면에서 완전 제외**. 등록 endpoint·REQ_TYPE 부재 (`ReqInboxServiceImpl.java` 명시). 필터·표시 모두 제외. 시안의 "스케줄 수정" 케이스는 폐기. |
| **Q3** | 사용자 직접 취소 | **본 작업 범위 밖**. `REQ_STATUS='04'` (CANCELLED) 상태값은 이미 존재하므로 다른 경로로 들어온 취소 건이 있다면 메타에 라벨만 노출. 본 화면에서 cancel 버튼·endpoint 미구현. |
| **Q4** | 기간 필터 기준 일자 | **대상 일자 `WORK_YMD` 기준**. 요청일자(`INSERT_DATE`) 기준 불채택. 트리거 라벨 `M/D ~ M/D` 도 WORK_YMD 기준. |
| **Q5** | 페이지네이션 | **무한 스크롤 + `limit=20` 고정**. 스크롤 하단 도달 시 다음 페이지 자동 로드. 응답에 `hasMore` 플래그 포함. |
| **Q6** | 요청 상세 화면 | **본 작업 범위 밖** (follow-up). 본 작업은 목록만. "자세히 >" 클릭 시 `showAlert('준비 중입니다')` 스텁. |

### 1.B planner 결정 포인트 (가이드라인·기존 패턴 근거 자율 결정)

| # | 결정 포인트 | planner 결정 | 근거 |
|---|---|---|---|
| **P1** | 백엔드 패키지명 | `com.prafta.app.req.req06` (REST 매핑 prefix `/req06`, 풀 경로 `/appApi/req06/my`). | `com.prafta.app.{module}.{moduleNN}` 컨벤션 (home01/attd01/chkLst01/risk01/tbm01 동일). req 모듈의 첫 번째 화면 → 06 채번은 prafta-app-006 작업 ID 와 자연 매칭. |
| **P2** | endpoint 명 | `GET /appApi/req06/my`. 원본 요청서의 `/api/app/req/my` 불채택 (실제 prefix `/appApi`). | `MainView` 가 `/appApi/home01/home-summary` 호출. axios baseURL=`/prafta`, 컨트롤러 `@RequestMapping("/req06")` + 자동 prefix `/prafta/appApi`. |
| **P3** | `summary.lines[]` 가공 비용 | **1차는 단순 가공**으로 한정. 1) 01/02 근태: `{출근시각} ~ {퇴근시각}` 한 줄(START_TIME/END_TIME 컬럼만 사용, 원본 시각 join 미수행). 2) 03/04 초과: `{startTime} ~ {endTime} ({합계분}분)`. 3) 05/06 연차: `LEAVE_TYPE 라벨 · {일수}일` (START_DATE != END_DATE 면 `시작 ~ 종료` 추가). **정밀 가공(원본↔변경 diff·구간별 분리·"변경 없음" 회색)은 §7 follow-up**. | 시안 §2.3 은 "원본 출근 09:00 → 요청 08:30 / 퇴근 (변경 없음)" 형식을 요구하지만 `tb_user_attd_req` 가 원본 시각을 별도로 저장하지 않음 (START_TIME/END_TIME 한 쌍만). 정밀 diff 는 (a) WORK_YMD/WORK_SEQ 로 `tb_user_attd_mgmt` 조회 (b) WORK_SEQ null 처리 (c) 1+2구간 분기까지 필요 — 본 라운드의 1차 가공 범위를 초과. 단순 가공이라도 시안 §3.2 의 "신청 정보 확인" 목적은 충족(요청한 시각이 보임). |
| **P4** | 정렬 옵션 라벨 | `대기 우선 (기본) / 최근 요청순 / 대상일자 가까운순`. 원본 요청서 §3.1.1 그대로. | 원본 요청서 §3.1.1, 시안 §6 정렬 바텀시트 동일. |
| **P5** | "대기 외 건" 정렬 (PENDING_FIRST 내부) | **요청 일시(INSERT_DATE) DESC**. 처리 일시(PROCESS_DATE) DESC 불채택. | 원본 요청서 §7.3 "[정책 확정 필요]" 항목 — 정책서에 명시 없음. 요청자(=본인) 관점에서 최근에 신청한 건이 위로 오는 것이 자연. 처리 일시는 노출 컬럼이지만 정렬 기준으로 노출하지 않음. |
| **P6** | TARGET_DATE 정렬 동작 | **MySQL 기준 `ABS(DATEDIFF(WORK_YMD, TODAY))` ASC**. WORK_YMD null 행은 가장 뒤(`COALESCE(WORK_YMD, '99991231')` 트릭). | 시안 §6 "대상일자 가까운순" = "오늘 가까운 순". 미래·과거 모두 오늘 가까운 순서가 직관적. |
| **P7** | 기간 필터 미지정 시 | **전체 기간 (필터 없음)**. 디폴트 30일·올해 불채택. | Q5 의 무한 스크롤이 기간 제한 없이도 동작하도록 설계. 사용자가 기간 필터를 적용해야만 WHERE 절 추가. |
| **P8** | 4종 바텀시트 공통화 | **`BaseBottomSheet.vue` 1개 베이스 + 시트 4종 분리**. 베이스는 핸들·헤더·dimmer·푸터 슬롯만 노출. 옵션 영역은 시트별 슬롯. | 시안 §4.4 공통 규격이 일관되고, 푸터 패턴이 (1) 다중선택 [초기화 / N개 선택 적용] (2) 라디오 단일선택 (푸터 없음) 두 패턴. 베이스의 푸터 슬롯 노출 여부만 시트별로 결정. 컴포넌트 4개 각각 별도 컴포넌트로 분리하되 베이스를 공유. |
| **P9** | 빈 상태 3종 구분 | **2종만 채택** — (a) "아직 등록한 요청이 없어요" (전체 0건: `totalCount===0`) (b) "선택한 조건의 요청이 없어요" (`filteredCount===0 && totalCount>0`). 검색 케이스(c)는 본 작업 범위 밖. | 시안 §3.3 의 3종 중 검색은 미구현. 보조 텍스트도 시안 그대로. |
| **P10** | 메인 홈 KPI 일치 | 본 화면 진입 → 백 → 메인 홈 재진입 시 `home-summary` 재호출 (`MainView` 의 `onMounted` 가 이미 호출함). **`onActivated` 추가 권고는 §7 follow-up**. KeepAlive 미사용이면 `onMounted` 가 매번 호출되므로 본 라운드는 OK. | `MainView` 코드 정독: `onMounted(() => { applySessionHeader(); loadHomeSummary(); })`. KeepAlive 가 router 에 없음 → 화면 재진입 시 onMounted 재실행됨. 자연 일치. |
| **P11** | 시스템 코드 07/08/09 노출 | **응답·필터 모두 미노출**. 백엔드 SELECT 의 `WHERE REQ_TYPE IN ('01','02','03','04','05','06')` 강제. | SYS032 의 07/08/09 는 시스템용 HIST_TYPE (관리자 반려/OT 승인/OT 반려) 으로 `tb_user_attd_req_hist` 전용. `tb_user_attd_req.REQ_TYPE` 에 07/08/09 가 들어갈 가능성은 0이지만 SELECT 에서도 명시 차단해 fail-closed. |
| **P12** | CSS 변수 / 디자인 토큰 | `MainView/MyAttendanceView` 의 팔레트 (`--color-primary: #16a34a` 등) 그대로 사용. 화면 루트 `.my-requests-view` 에 1회 선언. 신규 색상 도입 금지. | `MyAttendanceView.vue` 의 토큰 선언 패턴 동일. |
| **P13** | 라우트 prefix | `/MyRequests` (PascalCase, 비-public). | `MyAttendance` 와 동일 패턴 (`router/index.js` L11-15). publicPaths 에 추가하지 않음. |
| **P14** | `RequestTypeFilterSheet` 옵션 순서 | SYS032 코드 오름차순: 근태생성(01)/근태수정(02)/초과근무생성(03)/초과근무수정(04)/연차사용(05)/연차수정(06). | 기본 결재함 (`Attd_10.vue`) 의 표시 순서와 동일. |
| **P15** | 상태 다중 선택 0개 처리 | **전체 해제 = 필터 미적용**. 시트 푸터 `[초기화] / [N개 선택 적용]` 의 적용 버튼은 N>=1 일 때만 활성. 트리거 칩은 비활성 상태로 복귀. | 원본 요청서 §3.2.5 일치. |
| **P16** | 정렬 시트 동작 | 라디오 선택 즉시 적용 + 시트 자동 닫힘 (시안 §4.4.4). | 원본 요청서 §3.1.3 일치. |
| **P17** | 카드 행 클릭 영역 | 카드 전체 영역이 "자세히 >" 와 동등하게 동작 (탭 시 §7 stub `showAlert`). | 시안 §3.4 — 행 전체가 진입 타겟. |

### 1.C 정책서 출처

| 결정 | 정책서 섹션 |
|---|---|
| 단순 조회(본인 요청 목록)는 감사 로그 미대상 | `common/11-security-privacy.md` §11.3 (감사 대상 열거 — 단순 조회 미포함) |
| 본인 한정 조회 (cross-user 차단) | `common/08-permissions.md` §8.4 조직 스코프 (본인 데이터는 본인만) |
| 토큰 기반 식별 (gv_userCd) | `common/03-account-auth.md` §3.4 (액세스 토큰 1시간, JWT 클레임) |
| 상태 표현 3중 (색상 + dot + 라벨) | `common/13-ui-ux.md` §13.2 (피드백 / 접근성) |
| 차단 상황 명확 표시 (대기·반려) | `common/13-ui-ux.md` §13.3 (배지·배너) |
| 4탭 통합 결정 = 본 화면은 일치 (6종 SYS032 그대로) | `request-approval/03-policy-alignment.md` §3.1 — 관리자 화면은 4탭, 본 화면은 SYS032 6종 그대로 (관리자 탭 분리와 사용자 목록 표시는 독립) |

---

## 2. 원본 요청서에서 잘라낸·보류한 항목

### 2.1 폐기 (다시 재고하지 않음)

| 원본 항목 | 폐기 사유 |
|---|---|
| §6.1 `REJECT_REASON varchar(500)` 컬럼 추가 | `tb_user_attd_req.PROCESS_COMMENT varchar(500)` 가 이미 존재. 동일 의미. |
| §6.2 `CANCELED` 상태값 enum 추가 | `SYS033='04'` (CANCELLED) 이미 존재 (`AttdReqTypeUtils.REQ_STATUS_CANCELLED`). |
| §6.3 `PROCESSED_AT datetime` 컬럼 추가 | `tb_user_attd_req.PROCESS_DATE` 가 이미 존재. 동일 의미. |
| §6.4 `SCHED_MODIFY` REQ_TYPE 추가 | Q2 결정으로 제외. `ReqInboxServiceImpl.java` 가 "스케줄 수정 별도 REQ_TYPE 부재 — 미지원" 명시. |
| §5.2 `GET /api/app/req/my/count-by-status` | `home-summary.approval.pendingCount` 가 동일 값 제공. 중복 endpoint 신설 금지. |
| §5.3 `POST /api/app/req/{reqId}/cancel` | Q3 결정으로 미구현. |
| §1.2 "파일 위치: prafta-web-frontend" | 오타 — 실제 `prafta-app-frontend/src/views/req/MyRequestsView.vue`. 본 plan 정정. |
| §9 분해표 N1/N2/N3/N5/N6/N15 | DB 변경/카운트/취소/상세 모두 본 작업 범위 밖. |
| §2.1 표 "유형 4종" (SCHED_MODIFY 포함) | 6종으로 교체 (Q1). |
| §2.4 상태 4종 라벨 영문 코드 (REQUESTED/APPROVED 등) | 코드값은 SYS032/SYS033 (01/02/03/04). 응답·UI 라벨은 한국어. |

### 2.2 보류 (§7 follow-up 후보)

| 항목 | 보류 사유 |
|---|---|
| `summary.lines[]` 정밀 가공 (원본↔변경 diff·"변경 없음") | P3 — 원본 시각 join 필요. 별도 라운드. |
| 요청 상세 화면 (`RequestDetailView.vue`) | Q6 — 별도 화면 작업. |
| 사용자 직접 취소 흐름 | Q3 — 정책 확정·노티 처리 필요. |
| 메인 홈 KPI `onActivated` 강제 재호출 | P10 — KeepAlive 미사용으로 현재는 자연 일치. KeepAlive 도입 시 필요. |
| 알림 진입 동선 (push → 본 화면) | `prafta-031` outbox consumer 미구현. |
| 검색 (키워드) 빈 상태 | P9 — 검색 기능 자체가 미구현. |
| 보존 기간 / 보관함 분리 | 원본 요청서 §7.6 — 정책 미정. |

---

## 3. 영향 범위 스캔

### 3.1 DB 스키마

- **변경 0건**. 사용 컬럼:
  - `tb_user_attd_req`: `REQ_ID, CMPNY_CD, SITE_CD, USER_CD, REQ_TYPE, REQ_STATUS, WORK_YMD, NODE_CD, WORK_SEQ, START_DATE, START_TIME, END_DATE, END_TIME, OT_TYPE, LEAVE_TYPE, LEAVE_DAYS, PROCESS_USER_CD, PROCESS_COMMENT, PROCESS_DATE, INSERT_DATE, DEL_YN`.
  - 인덱스 활용: `IDX_ATTD_REQ_USER (CMPNY_CD, SITE_CD, USER_CD, REQ_STATUS)`. 본인 + 상태 다중 필터에 정확히 매칭.
- 공통 코드 (`tb_syst_val_d`):
  - SYS032 (01~06) — 라벨 조회.
  - SYS033 (01~04) — 라벨 조회.
  - SYS006 (OT_TYPE) — 라벨 조회 (있다면).
  - SYS010 (LEAVE_TYPE, half_am/half_pm/hourly 등) — 라벨 조회.
  - 백엔드에서 캐시된 syst_val 조회 패턴 그대로 사용 (별도 join 또는 in-memory map).

### 3.2 백엔드 신규 (1 endpoint + 패키지 한 벌)

```
com.prafta.app.req.req06
├── controller/AppReq06Controller.java          (신규 — GET /req06/my)
├── service/AppReq06Service.java                (신규 인터페이스)
├── service/impl/AppReq06ServiceImpl.java       (신규)
├── mapper/AppReq06Mapper.java                  (신규)
├── application/
│   ├── param/MyReqListParam.java               (record, JWT 기반)
│   └── query/MyReqListQuery.java               (record, mapper 직결)
├── dto/
│   ├── request/MyReqListRequest.java
│   └── response/
│       ├── MyReqListResponse.java
│       └── MyReqItemResponse.java
└── result/
    └── MyReqItemResult.java                    (mapper resultType)

src/main/resources/com/prafta/app/req/req06/mapper/
└── AppReq06Mapper.xml                          (신규)
```

### 3.3 프론트엔드 신규 (앱)

```
prafta-app-frontend/src/views/req/
├── MyRequestsView.vue                          (상위 화면)
└── components/
    ├── RequestFilterBar.vue                    (상단 행 + 트리거 행)
    ├── RequestCard.vue                         (6×4 = 24 케이스 카드)
    ├── RequestEmptyState.vue                   (빈 상태 2종)
    ├── BaseBottomSheet.vue                     (4종 시트 공통 베이스)
    ├── RequestStatusFilterSheet.vue            (상태 다중 선택)
    ├── RequestDateRangeFilterSheet.vue         (기간 from-to)
    ├── RequestTypeFilterSheet.vue              (유형 다중 선택)
    └── RequestSortSheet.vue                    (정렬 라디오)
```

### 3.4 라우트 / 진입 동선

| 파일 | 변경 |
|---|---|
| `prafta-app-frontend/src/router/index.js` | `routes[]` 에 `{ path: '/MyRequests', name: 'MyRequests', component: () => import('@/views/req/MyRequestsView.vue') }` 추가. publicPaths 미추가. |
| `prafta-app-frontend/src/views/main/MainView.vue` | `onApprovalClick` 핸들러 본문 교체: `showAlert('준비 중입니다')` → `router.push('/MyRequests')`. |

---

## 4. 작업 단위 분해

### 4.1 작업표

| 작업 ID | 유형 | 영역 | 모듈 | 작업유형 | 요구사항 요약 |
|---|---|---|---|---|---|
| **PRAFTA-APP-006-1** | backend | app | req/req06 | 신규 | `GET /appApi/req06/my` endpoint — 본인 요청 목록 페이지 조회. 컨트롤러/서비스/매퍼/DTO 한 벌. JWT 기반 IDOR 가드. |
| **PRAFTA-APP-006-2** | frontend-component | app | req | 신규 | `BaseBottomSheet.vue` — 4종 시트 공통 베이스 (핸들·헤더·dimmer·푸터 슬롯). |
| **PRAFTA-APP-006-3** | frontend-component | app | req | 신규 | `RequestStatusFilterSheet.vue` — 상태 다중 선택 시트 (SYS033 4종, dot+라벨). |
| **PRAFTA-APP-006-4** | frontend-component | app | req | 신규 | `RequestDateRangeFilterSheet.vue` — 기간 from-to 시트 (WORK_YMD 기준, 프리셋 4종). |
| **PRAFTA-APP-006-5** | frontend-component | app | req | 신규 | `RequestTypeFilterSheet.vue` — 유형 다중 선택 시트 (SYS032 6종). |
| **PRAFTA-APP-006-6** | frontend-component | app | req | 신규 | `RequestSortSheet.vue` — 정렬 라디오 시트 (3옵션, 선택 즉시 적용). |
| **PRAFTA-APP-006-7** | frontend-component | app | req | 신규 | `RequestCard.vue` — 요청 카드 (24 케이스: 6×4). |
| **PRAFTA-APP-006-8** | frontend-component | app | req | 신규 | `RequestFilterBar.vue` — 필터 영역 (상단 총건수+정렬, 트리거 행+초기화). |
| **PRAFTA-APP-006-9** | frontend-component | app | req | 신규 | `RequestEmptyState.vue` — 빈 상태 2종. |
| **PRAFTA-APP-006-10** | frontend-screen | app | req | 신규 | `MyRequestsView.vue` — 상위 컨테이너 + 무한 스크롤 + API 호출 + 시트 상태 관리. |
| **PRAFTA-APP-006-11** | frontend-screen | app | router/main | 보완 | 라우트 `/MyRequests` 등록 + `MainView.onApprovalClick` 핸들러 교체. |

### 4.2 권장 착수 순서

1. **백엔드 먼저**: 1 (단독). 응답 스키마가 프론트 골격의 props·state 형태를 결정.
2. **프론트 leaf 컴포넌트**: 2 (베이스) → 3, 4, 5, 6 (4종 시트, 순서 무관).
3. **프론트 카드·빈 상태**: 7, 9.
4. **프론트 필터 바**: 8.
5. **프론트 화면 통합**: 10.
6. **진입 동선 연결**: 11.

### 4.3 상세 설명 (Notion "작업 로그" 의 "상세 설명" 칸 그대로)

#### PRAFTA-APP-006-1 (백엔드 endpoint)

```
[backend]

[정책 근거]
- common/03-account-auth.md §3.4 (JWT 토큰 식별)
- common/08-permissions.md §8.4 (본인 데이터 본인만 = 조직 스코프)
- common/11-security-privacy.md §11.3 (단순 목록 조회는 감사 미대상)

[핵심 요구사항]
1) GET /appApi/req06/my — 본인이 등록한 tb_user_attd_req 행 페이지 조회.
2) 인증: JWT 의 gv_cmpnyCd / gv_siteCd / gv_userCd 만 사용. 쿼리/바디의 식별값 무시 (IDOR 가드).
   AppHome01Controller 패턴 동일 (HomeSummaryParam.from(tokenInfo)).
3) 쿼리 파라미터:
   - reqTypes (쉼표 다중, 기본 "01,02,03,04,05,06", 07/08/09 시스템 코드는 거부)
   - reqStatuses (쉼표 다중, 기본 "01,02,03,04")
   - targetYmdFrom, targetYmdTo (YYYYMMDD, WORK_YMD 기준, 둘 다 있을 때만 필터)
   - sort (PENDING_FIRST | RECENT | TARGET_DATE, 기본 PENDING_FIRST)
   - offset (기본 0), limit (고정 20, 클라 값 무시)
4) 응답 필드:
   - totalCount (필터 무관 본인 전체 건수, REQ_TYPE IN ('01'~'06') AND DEL_YN='N')
   - filteredCount (필터 적용 후 총합)
   - items[] (현 페이지 — 최대 20개)
     - reqId, reqType, reqTypeDisplay, reqStatus, reqStatusDisplay
     - targetYmd, targetYmdDisplay ("YYYY-MM-DD (요일)" — 한국어 요일)
     - summary { lines[] }
     - reqDatetime (ISO), reqDateDisplay ("YYYY년 M월 D일 요청")
     - processedAt (ISO or null), processedDateDisplay ("YYYY년 M월 D일 {승인|반려|취소}" or null)
     - rejectReason (REQ_STATUS='03' 일 때만 PROCESS_COMMENT, 그 외 null)
   - hasMore (다음 페이지 존재 여부)
5) 정렬:
   - PENDING_FIRST: ORDER BY (CASE REQ_STATUS WHEN '01' THEN 0 ELSE 1 END), INSERT_DATE DESC
   - RECENT: ORDER BY INSERT_DATE DESC
   - TARGET_DATE: ORDER BY ABS(DATEDIFF(STR_TO_DATE(WORK_YMD,'%Y%m%d'),CURDATE())) ASC, INSERT_DATE DESC.
     WORK_YMD NULL 행은 COALESCE 로 가장 뒤로 (예: COALESCE(WORK_YMD,'99991231')).
6) summary.lines 가공 (1차 단순 가공 — 정밀 가공은 §7 follow-up):
   - 01/02 근태: "{START_TIME HH:mm} ~ {END_TIME HH:mm}" 1줄. START_TIME null → "출근 미지정", END_TIME null → "퇴근 미지정".
   - 03/04 OT: "{START_TIME} ~ {END_TIME} ({minutes}분)" + OT_TYPE 라벨이 있으면 줄 끝에 " · {OT_TYPE 라벨}" 추가.
   - 05/06 연차: "{LEAVE_TYPE 라벨} · {LEAVE_DAYS}일" + START_DATE != END_DATE 면 1줄 추가로 "{startYmd} ~ {endYmd}".
   - 모든 시각은 HH:mm 포맷 (DB 의 HHmm 또는 HH:mm:ss → HH:mm).
7) 라벨 매핑: SYS032 / SYS033 / SYS010 (LEAVE_TYPE) / SYS006 (OT_TYPE) 의 SYST_VAL_NM 조회.
   기존 syst_val 조회 패턴 재사용. 별도 mapper 추가 또는 in-memory map.
8) WHERE 조건 (모든 SELECT 공통):
   - CMPNY_CD = #{param.cmpnyCd} AND SITE_CD = #{param.siteCd} AND USER_CD = #{param.userCd}
   - DEL_YN = 'N'
   - REQ_TYPE IN ('01','02','03','04','05','06')  ← 07/08/09 시스템 코드 차단
   - reqTypes 파라미터가 있으면 추가 IN
   - reqStatuses 파라미터가 있으면 추가 IN
   - targetYmdFrom/To 가 있으면 WORK_YMD BETWEEN

[영향 받는 파일]
- (신규) com.prafta.app.req.req06.controller.AppReq06Controller
- (신규) com.prafta.app.req.req06.service.AppReq06Service + Impl
- (신규) com.prafta.app.req.req06.mapper.AppReq06Mapper
- (신규) com.prafta.app.req.req06.application.param.MyReqListParam
- (신규) com.prafta.app.req.req06.application.query.MyReqListQuery
- (신규) com.prafta.app.req.req06.dto.request.MyReqListRequest
- (신규) com.prafta.app.req.req06.dto.response.MyReqListResponse / MyReqItemResponse
- (신규) com.prafta.app.req.req06.result.MyReqItemResult
- (신규) resources/com/prafta/app/req/req06/mapper/AppReq06Mapper.xml

[재사용]
- com.prafta.web.attd.attd07.util.AttdReqTypeUtils (REQ_TYPE / REQ_STATUS 상수)
- com.prafta.common.security.JwtUtil#getAllClaimsAsMap
- com.prafta.common.dto.TokenInfo (gv_cmpnyCd/gv_siteCd/gv_userCd)
- com.prafta.common.error.common.CommonErrorCode.COMMON_400_003 (식별값 없음)

[Endpoint]
GET /appApi/req06/my?reqTypes=&reqStatuses=&targetYmdFrom=&targetYmdTo=&sort=&offset=&limit=20

[예상 산출물]
controller / service+impl / mapper(.java+.xml) / DTO 5종 / Param·Query / Result 1종.

[비범위]
- DB 스키마 변경 없음
- 카운트 endpoint 미신설 (home-summary 재사용)
- cancel endpoint 미신설
- summary.lines 정밀 가공 (§7 follow-up)
- 단순 목록 조회는 감사 로그 미대상 (common §11.3)
```

#### PRAFTA-APP-006-2 (BaseBottomSheet)

```
[frontend-component]

[정책 근거]
- common/13-ui-ux.md §13.2 (모바일 최소 44px 터치 영역)
- common/13-ui-ux.md §13.3 (커스텀 dialog — role="dialog" aria-modal="true")

[핵심 요구사항]
1) 4종 시트 공통 베이스. props: modelValue(boolean v-model), title, showFooter(boolean).
2) 구조: dimmer (rgba(0,0,0,0.45)) + sheet (하단 슬라이드 업, 상단 라운드 20px, 하단 0).
3) 헤더: 36×4 핸들 + 타이틀 18/500 + 닫기(X) 32×32.
4) 옵션 영역 슬롯 (default). 푸터 슬롯 (showFooter 일 때만 노출).
5) ESC 키 / dimmer 탭으로 닫힘. v-model false 로 emit.
6) role="dialog" aria-modal="true" + 포커스 트랩 (1차는 단순 — 시트 열림 시 닫기 버튼에 focus).
7) 인라인 SVG 핸들·X 아이콘 (CDN 의존 금지 — MyAttendanceView 패턴 동일).

[영향 받는 파일]
- (신규) prafta-app-frontend/src/views/req/components/BaseBottomSheet.vue

[연결 UI 명세]
UI 명세는 §8 골격 참조.
```

#### PRAFTA-APP-006-3 (RequestStatusFilterSheet)

```
[frontend-component]

[정책 근거]
- common/13-ui-ux.md §13.2 (상태 표현 3중: 색상+dot+라벨)

[핵심 요구사항]
1) BaseBottomSheet 사용. title="상태", showFooter=true.
2) props: modelValue (boolean), selected (string[]). emits: update:modelValue, apply (string[]).
3) 옵션 4종 (SYS033 코드): 01 대기 (warning), 02 승인 (primary), 03 반려 (danger), 04 취소 (gray).
   각 옵션: dot 6px + 라벨 + 체크박스 (right).
4) 푸터: [초기화] (회색 보더) / [N개 선택 적용] (primary). N=0 이면 적용 비활성.
5) 적용 클릭 → emit('apply', selected[]) + emit('update:modelValue', false).
6) 초기화 → 시트 내부 selected 만 [] 비움 (apply 누르기 전까지는 상위에 반영 안 함).

[영향 받는 파일]
- (신규) prafta-app-frontend/src/views/req/components/RequestStatusFilterSheet.vue
```

#### PRAFTA-APP-006-4 (RequestDateRangeFilterSheet)

```
[frontend-component]

[정책 근거]
- common/13-ui-ux.md §13.3 (차단 상황 명확 표시)

[핵심 요구사항]
1) BaseBottomSheet 사용. title="대상 기간", showFooter=true.
2) props: modelValue, from (YYYYMMDD), to (YYYYMMDD). emits: update:modelValue, apply { from, to }.
3) 프리셋 4종 (오늘 기준 WORK_YMD 범위):
   - 최근 7일 (today-7 ~ today)
   - 최근 30일 (today-30 ~ today)
   - 최근 3개월 (today-90 ~ today)
   - 올해 (yyyy0101 ~ yyyy1231)
4) 프리셋 칩: 활성 시 primary-tint 배경. 미선택 시 white+border.
5) from / to 직접 입력: native <input type="date"> 2개 (1차는 단순 — 별도 일자 셀렉터 도입은 follow-up).
6) 적용 → emit('apply', { from, to }). 둘 다 비어 있으면 적용 시 { from: null, to: null } (필터 미적용 동등).
7) 초기화 → 시트 내부 from/to 만 비움.

[영향 받는 파일]
- (신규) prafta-app-frontend/src/views/req/components/RequestDateRangeFilterSheet.vue
```

#### PRAFTA-APP-006-5 (RequestTypeFilterSheet)

```
[frontend-component]

[핵심 요구사항]
1) BaseBottomSheet 사용. title="요청 유형", showFooter=true.
2) props: modelValue, selected (string[]). emits: update:modelValue, apply.
3) 옵션 6종 (SYS032 코드 오름차순):
   01 근태 생성 / 02 근태 수정 / 03 초과근무 생성 / 04 초과근무 수정 / 05 연차 사용 / 06 연차 수정.
4) 각 옵션: 라벨만 (컬러 칩 없음) + 체크박스.
5) 푸터 패턴은 RequestStatusFilterSheet 동일.

[영향 받는 파일]
- (신규) prafta-app-frontend/src/views/req/components/RequestTypeFilterSheet.vue
```

#### PRAFTA-APP-006-6 (RequestSortSheet)

```
[frontend-component]

[핵심 요구사항]
1) BaseBottomSheet 사용. title="정렬", showFooter=false.
2) props: modelValue, selected (string). emits: update:modelValue, apply (string).
3) 옵션 3종 (라디오 단일 선택):
   - PENDING_FIRST: "대기 우선 (기본)"
   - RECENT: "최근 요청순"
   - TARGET_DATE: "대상일자 가까운순"
4) 라디오 클릭 즉시 apply emit + modelValue false (자동 닫힘) — 시안 §4.4.4.

[영향 받는 파일]
- (신규) prafta-app-frontend/src/views/req/components/RequestSortSheet.vue
```

#### PRAFTA-APP-006-7 (RequestCard)

```
[frontend-component]

[정책 근거]
- common/13-ui-ux.md §13.2 (상태 표현 3중)
- common/13-ui-ux.md §13.3 (피드백·배지)

[핵심 요구사항]
1) props: item (MyReqItemResponse 1건).
2) 구조 (시안 §4.3):
   - 상단: 좌측 reqTypeDisplay (13/500, secondary) / 우측 상태 배지 (dot+라벨, 22px 높이, 6px 라운드).
   - 본문 1행: targetYmdDisplay (15/500, primary, tabular-nums).
   - 본문 2행~: summary.lines[] 줄바꿈 표시 (13/400, secondary).
   - (REQ_STATUS='03') rejectReason 인라인 박스 (회색 배경 #F9FAFB, 라운드 8px, 패딩 8/10).
   - 하단 디바이더(border-light 0.5px) + 메타 (좌: reqDateDisplay [+ ' / ' + processedDateDisplay], 우: "자세히 >" chevron-right).
3) 상태 배지 색상 매핑:
   - 01 대기: --color-warning / --color-warning-tint
   - 02 승인: --color-primary / --color-primary-tint
   - 03 반려: --color-danger / #fef2f2
   - 04 취소: --color-text-tertiary / --color-border-light
4) emits: click (전체 카드 영역 탭).
5) tabular-nums 적용 (시각 정렬).
6) 인라인 SVG chevron-right.

[영향 받는 파일]
- (신규) prafta-app-frontend/src/views/req/components/RequestCard.vue
```

#### PRAFTA-APP-006-8 (RequestFilterBar)

```
[frontend-component]

[핵심 요구사항]
1) props: totalCount, filteredCount, activeFilters { statuses[], dateFrom, dateTo, types[] }, sortLabel.
2) emits: openStatusSheet, openDateSheet, openTypeSheet, openSortSheet, reset.
3) 구조 (시안 §4.2):
   - 상단 행: 좌측 "전체 {filteredCount}건" + (활성 필터 1개 이상이면) "필터 N" 태그 / 우측 정렬 버튼 (캡슐, sortLabel + chevron-down).
   - 트리거 행: 캡슐 칩 3개 (상태 / 기간 / 유형) + (활성 1개 이상이면) "X 초기화" 버튼.
4) 트리거 칩 활성 표현: primary-tint 배경 + primary-tint 보더 + primary 텍스트.
   다중 선택(상태·유형): 활성 시 좌측에 선택 개수 배지 (primary 채움, 흰 글씨).
   기간: 활성 시 "M/D ~ M/D" (연도 생략, WORK_YMD 기준).
5) 가로 스크롤 컨테이너 (overflow-x: auto, 스크롤바 hidden).

[영향 받는 파일]
- (신규) prafta-app-frontend/src/views/req/components/RequestFilterBar.vue
```

#### PRAFTA-APP-006-9 (RequestEmptyState)

```
[frontend-component]

[핵심 요구사항]
1) props: kind ('total' | 'filtered').
2) 메시지 매핑:
   - total: "아직 등록한 요청이 없어요" / 보조 "근태 화면에서 요청을 등록할 수 있어요"
   - filtered: "선택한 조건의 요청이 없어요" / 보조 "필터를 변경해 보세요"
3) 일러스트: 회색 원형 (--color-border-light, 56×56) + inbox 아이콘 (28×28, tertiary).
4) 액션 버튼 없음.

[영향 받는 파일]
- (신규) prafta-app-frontend/src/views/req/components/RequestEmptyState.vue
```

#### PRAFTA-APP-006-10 (MyRequestsView)

```
[frontend-screen]

[정책 근거]
- common/13-ui-ux.md §13.2 (피드백·터치 영역)
- common/13-ui-ux.md §13.3 (확인 모달)

[핵심 요구사항]
1) 라우트: /MyRequests (PascalCase, 비-public).
2) 헤더 (56px): 백 버튼 + "내 승인 요청" 타이틀. (우측 액션 없음 — 좌우 균형용 빈 공간)
3) RequestFilterBar + 리스트 + 4종 시트.
4) state:
   - selectedStatuses (string[], 기본 [])
   - dateFrom / dateTo (string | null)
   - selectedTypes (string[], 기본 [])
   - sort (string, 기본 'PENDING_FIRST')
   - offset, hasMore, isLoading, isLoadingMore, items[], totalCount, filteredCount
   - sheet 4종 open boolean
5) API 호출:
   - 초기/필터 변경/정렬 변경 시: offset=0, items=[], loadPage().
   - 무한 스크롤: IntersectionObserver 또는 scroll 이벤트로 하단 도달 시 loadPage(append=true).
   - URL: GET /appApi/req06/my?reqTypes=&reqStatuses=&targetYmdFrom=&targetYmdTo=&sort=&offset=&limit=20
6) 빈 상태 분기:
   - items.length===0 && totalCount===0 → <RequestEmptyState kind="total" />
   - items.length===0 && totalCount>0 → <RequestEmptyState kind="filtered" />
   - items.length>0 → 카드 리스트 + (hasMore && isLoadingMore) "불러오는 중..." 인디케이터.
7) 카드 탭: 1차는 showAlert('준비 중입니다') stub (§7 follow-up).
8) 시트 상태: 트리거 칩 클릭 → 해당 시트 open=true. apply emit 수신 → state 업데이트 + 재조회.
9) 메인 홈 KPI 일치: 본 화면 진입 시 별도 카운트 호출 없음. 백 시 home-summary onMounted 재호출에 의존 (P10).

[영향 받는 파일]
- (신규) prafta-app-frontend/src/views/req/MyRequestsView.vue

[디자인 토큰]
.my-requests-view 루트에 MainView 와 동일 팔레트 1회 선언 (P12).
```

#### PRAFTA-APP-006-11 (라우트 + 진입 동선)

```
[frontend-screen / 보완]

[핵심 요구사항]
1) router/index.js routes[] 에 {
     path: '/MyRequests',
     name: 'MyRequests',
     component: () => import('@/views/req/MyRequestsView.vue')
   } 추가.
2) publicPaths 에 추가 안 함 (로그인 필요).
3) MainView.vue 의 onApprovalClick 본문 교체:
   - before: showAlert('준비 중입니다')
   - after:  router.push('/MyRequests')
4) AttendanceSummaryCard 의 @click:approval 이벤트는 그대로 (MainView 가 핸들러만 교체).

[영향 받는 파일]
- prafta-app-frontend/src/router/index.js
- prafta-app-frontend/src/views/main/MainView.vue
```

---

## 5. 의존성 그래프

```
PRAFTA-APP-006-1 (BE endpoint)
    └─ (응답 스키마 확정)
         ↓
PRAFTA-APP-006-2 (BaseBottomSheet) ───┐
                                      ├─ PRAFTA-APP-006-3 (StatusFilterSheet)
                                      ├─ PRAFTA-APP-006-4 (DateRangeFilterSheet)
                                      ├─ PRAFTA-APP-006-5 (TypeFilterSheet)
                                      └─ PRAFTA-APP-006-6 (SortSheet)

PRAFTA-APP-006-7 (RequestCard) ─────── (독립)
PRAFTA-APP-006-9 (EmptyState) ──────── (독립)
PRAFTA-APP-006-8 (FilterBar) ───────── (독립)

         ↓ (전부 완료 후)
PRAFTA-APP-006-10 (MyRequestsView) ── 통합 (전 컴포넌트 + 1번 BE 호출)
         ↓
PRAFTA-APP-006-11 (라우트 + 진입 동선)
```

병렬 진행 가능: BE (1) 와 FE leaf (2~9) 는 응답 스키마가 §4.3 PRAFTA-APP-006-1 의 핵심 요구사항 4) 로 고정되어 있으므로 동시 진행 가능.

---

## 6. 비기능 요구사항

### 6.1 보안

- **JWT 기반 식별 강제**: `HomeSummaryParam.from(TokenInfo)` 패턴 동일. 쿼리/바디의 cmpnyCd·siteCd·userCd 절대 사용 금지 (IDOR 가드).
- **REQ_TYPE allow-list**: SELECT 단계에서 `IN ('01','02','03','04','05','06')` 강제. 07/08/09 시스템 코드 fail-closed.
- **DEL_YN='N' 필수**.
- **감사 로그**: 단순 조회는 미대상 (`common/11-security-privacy.md` §11.3 — 다운로드/상세 위치 조회/중요 데이터 삭제만 감사).
- **PII**: `tb_user_attd_req` 에 본인 데이터만 조회되므로 PII 평문 노출 우려 없음. `PROCESS_USER_CD` 는 USER_CD 형식 (PII 아님).

### 6.2 SQL

- 명시 컬럼 나열, `SELECT *` 금지.
- `#{}` 바인딩. ORDER BY 의 sort 분기는 `<choose>` MyBatis 분기 (`${}` 금지).
- Leading comma.
- `IDX_ATTD_REQ_USER (CMPNY_CD, SITE_CD, USER_CD, REQ_STATUS)` 활용 — WHERE 절 컬럼 순서 일치.

### 6.3 프론트엔드

- **TypeScript 금지** (`<script setup>` JS).
- **scoped style 필수**, `<style>` 단독 사용 금지.
- **CSS 변수만** 사용. 신규 색상 도입 금지 (P12).
- **!important 금지**.
- **인라인 SVG**: CDN 의존 금지 (`MyAttendanceView` 패턴 동일).
- **터치 영역 최소 44×44** (`common/13-ui-ux.md` §13.2).
- **반응형**: 360~414px 기준 폭 (앱 webview).
- **한국어 텍스트** (사용자 노출 문구 모두).

### 6.4 성능

- 무한 스크롤 limit=20 고정.
- 필터·정렬 변경 시 offset=0 부터 재로드.
- 클라이언트 측 필터링/정렬 없음 — 모두 BE 위임.
- IntersectionObserver 사용 (scroll 이벤트보다 성능 우수).

### 6.5 에러 처리

- 401/403: axios 인터셉터가 강제 로그아웃 (기존 MainView 패턴).
- 그 외 5xx / 네트워크 오류: `showAlert` 폴백 (한국어 메시지).

---

## 7. Follow-up 후보

| # | 항목 | 이유 |
|---|---|---|
| F1 | 요청 상세 화면 (`RequestDetailView.vue` + `GET /appApi/req06/my/{reqId}`) | Q6 — 별도 화면. 카드 "자세히 >" 진입 타겟. |
| F2 | `summary.lines[]` 정밀 가공 (원본↔변경 diff·"변경 없음" 회색 라벨·1+2구간 분리) | P3 — 원본 시각 join (`tb_user_attd_mgmt`·`tb_user_attd_schedule`) 필요. |
| F3 | 사용자 직접 취소 흐름 (`POST /appApi/req06/{reqId}/cancel`) | Q3 — 정책 확정 후. 본인+REQUESTED 만 가능, 다른 상태 422. |
| F4 | 메인 홈 KPI 강제 일치 (`MainView` 의 `onActivated` 추가) | P10 — KeepAlive 도입 시 필요. 현재는 자연 일치. |
| F5 | 알림 진입 동선 (push → 본 화면 또는 상세) | `prafta-031` outbox consumer 구현 후 연결. |
| F6 | 키워드 검색 + 검색 빈 상태 | P9 — 검색 자체가 미구현. |
| F7 | 보관함 / 보존 기간 분리 | 원본 요청서 §7.6 — 정책 미정. |
| F8 | 일자 셀렉터 컴포넌트 (`RequestDateRangeFilterSheet` 의 native `<input type="date">` 대체) | iOS/안드로이드 native picker 통일 위해. |
| F9 | "재요청" 액션 (반려 건의 한 번 탭으로 새 등록 폼 진입) | 원본 요청서 §7.10. |
| F10 | 정밀 가공이 끝나면 카드의 `summary.lines` line-clamp 정책 (다중 줄 시 카드 높이 제어) | F2 완료 후 평가. |

---

## 8. Vue 컴포넌트 골격 (작성·디스크 등록은 사용자 승인 후)

> **모든 골격은 template + style 만 완성. script 영역은 `// TODO(developer):` 마커 + 반응형 변수 선언만.**
> **CSS 변수만 사용. native HTML 태그 사용은 필요 최소만 (앱 프론트는 공통 폼 컴포넌트가 부분적이므로 `<button>`, `<input>` 직접 사용 허용).**

### 8.1 `prafta-app-frontend/src/views/req/MyRequestsView.vue`

```vue
<!--
  MyRequestsView.vue — 내 승인 요청 목록 화면 (모바일 앱)
  - 작업 ID: PRAFTA-APP-006-10 (분해: .claude/requests/app_requests/prafta-app-006-plan.md)
  - planner 라운드 스코프: 헤더 + 필터바 + 리스트 + 4종 시트 조립 (template/style 완성)
  - developer 라운드 스코프(아래 TODO): API 호출, 무한 스크롤, 시트 상태 전파, 라우팅
  - 디자인 토큰: MainView(.home-view) 와 동일 세트를 .my-requests-view 루트에 1회 선언.
-->
<template>
  <div class="my-requests-view">
    <!-- 헤더 -->
    <header class="req-hd">
      <button type="button" class="req-hd__back" aria-label="뒤로" @click="onBack">
        <svg class="icon" width="22" height="22" aria-hidden="true">
          <use href="#i-req-chev-left" />
        </svg>
      </button>
      <h1 class="req-hd__title">내 승인 요청</h1>
      <span class="req-hd__spacer" aria-hidden="true"></span>
    </header>

    <!-- 필터 영역 -->
    <RequestFilterBar
      :total-count="totalCount"
      :filtered-count="filteredCount"
      :active-filters="activeFilters"
      :sort-label="currentSortLabel"
      @open-status-sheet="statusSheetOpen = true"
      @open-date-sheet="dateSheetOpen = true"
      @open-type-sheet="typeSheetOpen = true"
      @open-sort-sheet="sortSheetOpen = true"
      @reset="onResetFilters"
    />

    <!-- 본문 (스크롤 영역) -->
    <main class="req-body" ref="bodyRef">
      <!-- 빈 상태 -->
      <RequestEmptyState
        v-if="items.length === 0 && !isLoading && totalCount === 0"
        kind="total"
      />
      <RequestEmptyState
        v-else-if="items.length === 0 && !isLoading && totalCount > 0"
        kind="filtered"
      />

      <!-- 카드 리스트 -->
      <template v-else>
        <RequestCard
          v-for="item in items"
          :key="item.reqId"
          :item="item"
          @click="onCardClick(item)"
        />
        <!-- 무한 스크롤 sentinel -->
        <div ref="sentinelRef" class="req-sentinel" aria-hidden="true"></div>
        <p v-if="isLoadingMore" class="req-load-more">불러오는 중...</p>
      </template>
    </main>

    <!-- 시트 4종 -->
    <RequestStatusFilterSheet
      v-model="statusSheetOpen"
      :selected="selectedStatuses"
      @apply="onApplyStatus"
    />
    <RequestDateRangeFilterSheet
      v-model="dateSheetOpen"
      :from="dateFrom"
      :to="dateTo"
      @apply="onApplyDate"
    />
    <RequestTypeFilterSheet
      v-model="typeSheetOpen"
      :selected="selectedTypes"
      @apply="onApplyType"
    />
    <RequestSortSheet
      v-model="sortSheetOpen"
      :selected="sort"
      @apply="onApplySort"
    />

    <!-- 인라인 SVG sprite (본 화면 전용) -->
    <svg width="0" height="0" class="req-sprite" aria-hidden="true" focusable="false">
      <defs>
        <symbol id="i-req-chev-left" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <polyline points="15 18 9 12 15 6" />
        </symbol>
      </defs>
    </svg>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, getCurrentInstance } from 'vue'
import { useRouter } from 'vue-router'

import RequestFilterBar from './components/RequestFilterBar.vue'
import RequestCard from './components/RequestCard.vue'
import RequestEmptyState from './components/RequestEmptyState.vue'
import RequestStatusFilterSheet from './components/RequestStatusFilterSheet.vue'
import RequestDateRangeFilterSheet from './components/RequestDateRangeFilterSheet.vue'
import RequestTypeFilterSheet from './components/RequestTypeFilterSheet.vue'
import RequestSortSheet from './components/RequestSortSheet.vue'

const router = useRouter()
const { proxy } = getCurrentInstance() || { proxy: null }

// 공통: alert 폴백 (MainView 패턴 동일)
const showAlert = (message) => {
  if (proxy?.$alert) return proxy.$alert(message)
  window.alert(message)
  return Promise.resolve()
}

// ───────────────────────────────────────────────────────────
// 필터·정렬 상태
// ───────────────────────────────────────────────────────────
const selectedStatuses = ref([]) // string[] (SYS033 코드)
const dateFrom = ref('') // YYYYMMDD or ''
const dateTo = ref('')
const selectedTypes = ref([]) // string[] (SYS032 코드 01~06)
const sort = ref('PENDING_FIRST') // PENDING_FIRST | RECENT | TARGET_DATE

const SORT_LABELS = {
  PENDING_FIRST: '대기 우선',
  RECENT: '최근 요청순',
  TARGET_DATE: '대상일자 가까운순',
}
const currentSortLabel = computed(() => SORT_LABELS[sort.value] || SORT_LABELS.PENDING_FIRST)

const activeFilters = computed(() => ({
  statuses: selectedStatuses.value,
  dateFrom: dateFrom.value,
  dateTo: dateTo.value,
  types: selectedTypes.value,
}))

// ───────────────────────────────────────────────────────────
// 시트 open 상태
// ───────────────────────────────────────────────────────────
const statusSheetOpen = ref(false)
const dateSheetOpen = ref(false)
const typeSheetOpen = ref(false)
const sortSheetOpen = ref(false)

// ───────────────────────────────────────────────────────────
// 리스트 / 페이지네이션 상태
// ───────────────────────────────────────────────────────────
const items = ref([]) // MyReqItemResponse[]
const totalCount = ref(0)
const filteredCount = ref(0)
const hasMore = ref(false)
const offset = ref(0)
const isLoading = ref(false)
const isLoadingMore = ref(false)

const bodyRef = ref(null)
const sentinelRef = ref(null)

// ───────────────────────────────────────────────────────────
// 이벤트 핸들러 (developer 라운드: 실API + 무한스크롤 결선)
// ───────────────────────────────────────────────────────────
const onBack = () => {
  router.back()
}

const onApplyStatus = (selected) => {
  selectedStatuses.value = selected
  // TODO(developer): offset=0 으로 재조회 트리거
}

const onApplyDate = ({ from, to }) => {
  dateFrom.value = from || ''
  dateTo.value = to || ''
  // TODO(developer): offset=0 으로 재조회 트리거
}

const onApplyType = (selected) => {
  selectedTypes.value = selected
  // TODO(developer): offset=0 으로 재조회 트리거
}

const onApplySort = (newSort) => {
  sort.value = newSort
  // TODO(developer): offset=0 으로 재조회 트리거
}

const onResetFilters = () => {
  selectedStatuses.value = []
  dateFrom.value = ''
  dateTo.value = ''
  selectedTypes.value = []
  // TODO(developer): offset=0 으로 재조회 트리거 (정렬은 영향받지 않음)
}

const onCardClick = (item) => {
  // TODO(developer): 요청 상세 화면 진입 (§7 follow-up F1).
  // 1차는 stub.
  showAlert('준비 중입니다')
}

// TODO(developer): 아래 함수들을 채워야 함
// const loadPage = async (append = false) => { ... }
//   GET /appApi/req06/my?reqTypes=&reqStatuses=&targetYmdFrom=&targetYmdTo=&sort=&offset=&limit=20
//   - append=false 시 offset=0, items=[], isLoading=true
//   - append=true 시 isLoadingMore=true
//   - 응답의 totalCount/filteredCount/items/hasMore 적용
// const setupInfiniteScroll = () => { ... }
//   IntersectionObserver(sentinelRef) → entries[0].isIntersecting && hasMore && !isLoadingMore
//   → offset += 20; loadPage(true)
// onMounted(() => { loadPage(false); setupInfiniteScroll() })
</script>

<style scoped>
/*
 * 디자인 토큰 — MainView 와 동일 세트를 .my-requests-view 에 1회 선언.
 * 자식 컴포넌트(scoped)는 var(--color-...) 를 상속받아 사용 가능.
 */
.my-requests-view {
  --color-primary: #16a34a;
  --color-primary-tint: #f0fdf4;
  --color-primary-tint-border: #dcfce7;
  --color-danger: #ef4444;
  --color-danger-tint: #fef2f2;
  --color-warning: #f59e0b;
  --color-warning-tint: #fffbeb;
  --color-text-primary: #111827;
  --color-text-secondary: #6b7280;
  --color-text-tertiary: #9ca3af;
  --color-border: #e5e7eb;
  --color-border-light: #f3f4f6;
  --color-surface: #ffffff;
  --color-bg: #f9fafb;
  --radius-sm: 6px;
  --radius-md: 10px;
  --radius-lg: 14px;
  --radius-full: 9999px;
  --shadow-sm: 0 1px 2px rgba(0, 0, 0, 0.04);

  min-height: 100vh;
  background: var(--color-bg);
  color: var(--color-text-primary);
  display: flex;
  flex-direction: column;
  font-family:
    -apple-system, BlinkMacSystemFont, 'Apple SD Gothic Neo', 'Pretendard', 'Noto Sans KR',
    sans-serif;
}

/* 헤더 */
.req-hd {
  height: 56px;
  background: var(--color-surface);
  border-bottom: 0.5px solid var(--color-border);
  display: grid;
  grid-template-columns: 44px 1fr 44px;
  align-items: center;
  position: sticky;
  top: 0;
  z-index: 10;
}
.req-hd__back {
  width: 44px;
  height: 44px;
  background: transparent;
  border: 0;
  color: var(--color-text-primary);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}
.req-hd__title {
  margin: 0;
  text-align: center;
  font-size: 18px;
  font-weight: 500;
  color: var(--color-text-primary);
}
.req-hd__spacer {
  width: 44px;
  height: 44px;
}

/* 본문 */
.req-body {
  flex: 1;
  padding: 8px 16px 24px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.req-sentinel {
  height: 1px;
}

.req-load-more {
  margin: 8px 0;
  text-align: center;
  font-size: 13px;
  color: var(--color-text-secondary);
}

.icon {
  display: block;
}
</style>
```

### 8.2 `prafta-app-frontend/src/views/req/components/RequestFilterBar.vue`

```vue
<!--
  RequestFilterBar.vue — 내 승인 요청 필터 영역 (상단 행 + 트리거 행)
  - 작업 ID: PRAFTA-APP-006-8
-->
<template>
  <section class="req-filter-bar" aria-label="요청 필터">
    <!-- 상단 행: 총 건수 + 정렬 -->
    <div class="req-filter-bar__top">
      <div class="req-filter-bar__count-wrap">
        <span class="req-filter-bar__count">전체 {{ filteredCount }}건</span>
        <span v-if="activeFilterCount > 0" class="req-filter-bar__filter-tag"
          >필터 {{ activeFilterCount }}</span
        >
      </div>
      <button
        type="button"
        class="req-filter-bar__sort"
        aria-label="정렬 옵션 열기"
        @click="$emit('openSortSheet')"
      >
        <span>{{ sortLabel }}</span>
        <svg width="14" height="14" aria-hidden="true">
          <use href="#i-req-chev-down" />
        </svg>
      </button>
    </div>

    <!-- 트리거 행: 3종 필터 + 초기화 -->
    <div class="req-filter-bar__triggers" role="group" aria-label="필터 트리거">
      <button
        type="button"
        class="req-trigger"
        :class="{ 'req-trigger--on': activeFilters.statuses.length > 0 }"
        @click="$emit('openStatusSheet')"
      >
        <span v-if="activeFilters.statuses.length > 0" class="req-trigger__badge">{{
          activeFilters.statuses.length
        }}</span>
        <span class="req-trigger__label">상태</span>
        <svg width="14" height="14" aria-hidden="true">
          <use href="#i-req-chev-down" />
        </svg>
      </button>

      <button
        type="button"
        class="req-trigger"
        :class="{ 'req-trigger--on': hasDateRange }"
        @click="$emit('openDateSheet')"
      >
        <span class="req-trigger__label">{{ dateRangeLabel }}</span>
        <svg width="14" height="14" aria-hidden="true">
          <use href="#i-req-chev-down" />
        </svg>
      </button>

      <button
        type="button"
        class="req-trigger"
        :class="{ 'req-trigger--on': activeFilters.types.length > 0 }"
        @click="$emit('openTypeSheet')"
      >
        <span v-if="activeFilters.types.length > 0" class="req-trigger__badge">{{
          activeFilters.types.length
        }}</span>
        <span class="req-trigger__label">유형</span>
        <svg width="14" height="14" aria-hidden="true">
          <use href="#i-req-chev-down" />
        </svg>
      </button>

      <button
        v-if="activeFilterCount > 0"
        type="button"
        class="req-reset"
        aria-label="필터 전체 초기화"
        @click="$emit('reset')"
      >
        <svg width="12" height="12" aria-hidden="true">
          <use href="#i-req-x" />
        </svg>
        <span>초기화</span>
      </button>
    </div>

    <!-- 인라인 SVG sprite (본 컴포넌트 전용) -->
    <svg width="0" height="0" class="req-filter-bar__sprite" aria-hidden="true" focusable="false">
      <defs>
        <symbol id="i-req-chev-down" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <polyline points="6 9 12 15 18 9" />
        </symbol>
        <symbol id="i-req-x" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <line x1="18" y1="6" x2="6" y2="18" />
          <line x1="6" y1="6" x2="18" y2="18" />
        </symbol>
      </defs>
    </svg>
  </section>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  totalCount: { type: Number, default: 0 },
  filteredCount: { type: Number, default: 0 },
  activeFilters: {
    type: Object,
    default: () => ({ statuses: [], dateFrom: '', dateTo: '', types: [] }),
  },
  sortLabel: { type: String, default: '대기 우선' },
})

defineEmits(['openStatusSheet', 'openDateSheet', 'openTypeSheet', 'openSortSheet', 'reset'])

const hasDateRange = computed(
  () => !!props.activeFilters.dateFrom || !!props.activeFilters.dateTo,
)

const activeFilterCount = computed(() => {
  let n = 0
  if (props.activeFilters.statuses?.length > 0) n += 1
  if (hasDateRange.value) n += 1
  if (props.activeFilters.types?.length > 0) n += 1
  return n
})

// YYYYMMDD → M/D
const fmtMD = (ymd) => {
  if (!ymd || ymd.length !== 8) return ''
  const m = Number(ymd.slice(4, 6))
  const d = Number(ymd.slice(6, 8))
  return `${m}/${d}`
}

const dateRangeLabel = computed(() => {
  if (!hasDateRange.value) return '기간'
  const f = fmtMD(props.activeFilters.dateFrom)
  const t = fmtMD(props.activeFilters.dateTo)
  if (f && t) return `${f} ~ ${t}`
  if (f) return `${f} ~`
  return `~ ${t}`
})
</script>

<style scoped>
.req-filter-bar {
  padding: 8px 16px 12px;
  background: var(--color-bg);
  border-bottom: 0.5px solid var(--color-border);
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.req-filter-bar__top {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.req-filter-bar__count-wrap {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.req-filter-bar__count {
  font-size: 13px;
  font-weight: 500;
  color: var(--color-text-primary);
}

.req-filter-bar__filter-tag {
  background: var(--color-primary-tint);
  color: var(--color-primary);
  border-radius: 4px;
  padding: 0 6px;
  height: 18px;
  font-size: 11px;
  font-weight: 500;
  line-height: 18px;
}

.req-filter-bar__sort {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  height: 32px;
  padding: 0 10px;
  background: var(--color-surface);
  border: 0.5px solid var(--color-border);
  border-radius: var(--radius-full);
  color: var(--color-text-secondary);
  font-size: 12px;
  cursor: pointer;
}

.req-filter-bar__triggers {
  display: flex;
  flex-direction: row;
  gap: 6px;
  overflow-x: auto;
  scrollbar-width: none;
}
.req-filter-bar__triggers::-webkit-scrollbar {
  display: none;
}

.req-trigger {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  height: 32px;
  padding: 0 8px 0 12px;
  background: var(--color-surface);
  border: 0.5px solid var(--color-border);
  border-radius: var(--radius-full);
  color: var(--color-text-secondary);
  font-size: 12px;
  white-space: nowrap;
  cursor: pointer;
}

.req-trigger--on {
  background: var(--color-primary-tint);
  border-color: var(--color-primary-tint-border);
  color: var(--color-primary);
}

.req-trigger__badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 16px;
  height: 16px;
  padding: 0 4px;
  background: var(--color-primary);
  color: var(--color-surface);
  border-radius: var(--radius-full);
  font-size: 10px;
  font-weight: 600;
}

.req-reset {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  height: 32px;
  padding: 0 10px;
  background: transparent;
  border: 0;
  color: var(--color-text-tertiary);
  font-size: 12px;
  cursor: pointer;
  white-space: nowrap;
}
</style>
```

### 8.3 `prafta-app-frontend/src/views/req/components/RequestCard.vue`

```vue
<!--
  RequestCard.vue — 요청 카드 (SYS032 6종 × SYS033 4종 = 24 케이스)
  - 작업 ID: PRAFTA-APP-006-7
  - props.item: MyReqItemResponse (백엔드 PRAFTA-APP-006-1 응답 1건)
-->
<template>
  <article class="req-card" role="button" tabindex="0" @click="$emit('click')">
    <!-- 상단: 유형 + 상태 배지 -->
    <header class="req-card__top">
      <span class="req-card__type">{{ item.reqTypeDisplay }}</span>
      <span class="req-card__badge" :class="badgeClass">
        <span class="req-card__badge-dot" aria-hidden="true"></span>
        <span class="req-card__badge-label">{{ item.reqStatusDisplay }}</span>
      </span>
    </header>

    <!-- 본문: 대상일자 + 요약 -->
    <div class="req-card__body">
      <p class="req-card__target-ymd">{{ item.targetYmdDisplay }}</p>
      <p
        v-for="(line, idx) in (item.summary && item.summary.lines) || []"
        :key="idx"
        class="req-card__summary-line"
      >
        {{ line }}
      </p>
    </div>

    <!-- 반려 사유 (REQ_STATUS='03' 일 때만) -->
    <div v-if="item.reqStatus === '03' && item.rejectReason" class="req-card__reject">
      <span class="req-card__reject-label">반려 사유</span>
      <span class="req-card__reject-dot" aria-hidden="true"> · </span>
      <span class="req-card__reject-body">{{ item.rejectReason }}</span>
    </div>

    <!-- 하단 메타 -->
    <footer class="req-card__meta">
      <span class="req-card__meta-dates">
        {{ item.reqDateDisplay
        }}<template v-if="item.processedDateDisplay"> / {{ item.processedDateDisplay }}</template>
      </span>
      <span class="req-card__meta-more">
        자세히
        <svg width="12" height="12" aria-hidden="true">
          <use href="#i-req-chev-right" />
        </svg>
      </span>
    </footer>

    <!-- 인라인 SVG sprite (본 컴포넌트 전용) -->
    <svg width="0" height="0" class="req-card__sprite" aria-hidden="true" focusable="false">
      <defs>
        <symbol id="i-req-chev-right" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <polyline points="9 18 15 12 9 6" />
        </symbol>
      </defs>
    </svg>
  </article>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  item: { type: Object, required: true },
})

defineEmits(['click'])

const badgeClass = computed(() => {
  switch (props.item?.reqStatus) {
    case '01':
      return 'req-card__badge--warning' // 대기
    case '02':
      return 'req-card__badge--primary' // 승인
    case '03':
      return 'req-card__badge--danger' // 반려
    case '04':
      return 'req-card__badge--neutral' // 취소
    default:
      return 'req-card__badge--neutral'
  }
})
</script>

<style scoped>
.req-card {
  background: var(--color-surface);
  border: 0.5px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 14px 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  cursor: pointer;
  text-align: left;
  font-family: inherit;
}

.req-card__top {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.req-card__type {
  font-size: 13px;
  font-weight: 500;
  color: var(--color-text-secondary);
}

.req-card__badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 22px;
  padding: 0 8px;
  border-radius: var(--radius-sm);
  font-size: 12px;
  font-weight: 500;
}
.req-card__badge-dot {
  width: 6px;
  height: 6px;
  border-radius: var(--radius-full);
  background: currentColor;
}

.req-card__badge--warning {
  background: var(--color-warning-tint);
  color: var(--color-warning);
}
.req-card__badge--primary {
  background: var(--color-primary-tint);
  color: var(--color-primary);
}
.req-card__badge--danger {
  background: var(--color-danger-tint);
  color: var(--color-danger);
}
.req-card__badge--neutral {
  background: var(--color-border-light);
  color: var(--color-text-tertiary);
}

.req-card__body {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.req-card__target-ymd {
  margin: 0;
  font-size: 15px;
  font-weight: 500;
  color: var(--color-text-primary);
  font-variant-numeric: tabular-nums;
}

.req-card__summary-line {
  margin: 0;
  font-size: 13px;
  font-weight: 400;
  color: var(--color-text-secondary);
  line-height: 18px;
  font-variant-numeric: tabular-nums;
}

.req-card__reject {
  background: var(--color-bg);
  border-radius: 8px;
  padding: 8px 10px;
  font-size: 12px;
  font-weight: 400;
  color: var(--color-text-secondary);
  line-height: 17px;
}
.req-card__reject-label {
  color: var(--color-text-primary);
  font-weight: 500;
}
.req-card__reject-dot {
  color: var(--color-text-secondary);
}

.req-card__meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 8px;
  border-top: 0.5px solid var(--color-border-light);
  font-size: 12px;
  font-weight: 400;
  color: var(--color-text-tertiary);
  font-variant-numeric: tabular-nums;
}

.req-card__meta-more {
  display: inline-flex;
  align-items: center;
  gap: 2px;
}
</style>
```

### 8.4 `prafta-app-frontend/src/views/req/components/RequestEmptyState.vue`

```vue
<!--
  RequestEmptyState.vue — 빈 상태 (2종)
  - 작업 ID: PRAFTA-APP-006-9
  - kind: 'total' | 'filtered'
-->
<template>
  <div class="req-empty">
    <div class="req-empty__icon-wrap" aria-hidden="true">
      <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
        <polyline points="22 12 16 12 14 15 10 15 8 12 2 12" />
        <path d="M5.45 5.11 2 12v6a2 2 0 0 0 2 2h16a2 2 0 0 0 2 -2v-6l-3.45 -6.89A2 2 0 0 0 16.76 4H7.24a2 2 0 0 0 -1.79 1.11Z" />
      </svg>
    </div>
    <p class="req-empty__msg">{{ msg }}</p>
    <p class="req-empty__sub">{{ sub }}</p>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  kind: { type: String, default: 'total', validator: (v) => ['total', 'filtered'].includes(v) },
})

const msg = computed(() =>
  props.kind === 'total' ? '아직 등록한 요청이 없어요' : '선택한 조건의 요청이 없어요',
)
const sub = computed(() =>
  props.kind === 'total' ? '근태 화면에서 요청을 등록할 수 있어요' : '필터를 변경해 보세요',
)
</script>

<style scoped>
.req-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 60px 16px;
  gap: 12px;
}

.req-empty__icon-wrap {
  width: 56px;
  height: 56px;
  border-radius: var(--radius-full);
  background: var(--color-border-light);
  color: var(--color-text-tertiary);
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.req-empty__msg {
  margin: 0;
  font-size: 14px;
  color: var(--color-text-secondary);
}

.req-empty__sub {
  margin: 0;
  font-size: 12px;
  color: var(--color-text-tertiary);
}
</style>
```

### 8.5 `prafta-app-frontend/src/views/req/components/BaseBottomSheet.vue`

```vue
<!--
  BaseBottomSheet.vue — 바텀시트 공통 베이스 (4종 시트 공통)
  - 작업 ID: PRAFTA-APP-006-2
  - props: modelValue (v-model), title, showFooter
  - slots: default(옵션 영역), footer (showFooter 일 때만 노출)
-->
<template>
  <transition name="req-sheet-fade">
    <div
      v-if="modelValue"
      class="req-sheet__dimmer"
      role="dialog"
      aria-modal="true"
      @click.self="onClose"
    >
      <div class="req-sheet" :aria-label="title">
        <div class="req-sheet__handle" aria-hidden="true"></div>
        <header class="req-sheet__header">
          <h2 class="req-sheet__title">{{ title }}</h2>
          <button
            type="button"
            class="req-sheet__close"
            aria-label="닫기"
            @click="onClose"
          >
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
              <line x1="18" y1="6" x2="6" y2="18" />
              <line x1="6" y1="6" x2="18" y2="18" />
            </svg>
          </button>
        </header>

        <div class="req-sheet__body">
          <slot />
        </div>

        <footer v-if="showFooter" class="req-sheet__footer">
          <slot name="footer" />
        </footer>
      </div>
    </div>
  </transition>
</template>

<script setup>
import { onMounted, onBeforeUnmount, watch } from 'vue'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  title: { type: String, default: '' },
  showFooter: { type: Boolean, default: true },
})

const emit = defineEmits(['update:modelValue'])

const onClose = () => {
  emit('update:modelValue', false)
}

const onKeyDown = (e) => {
  if (e.key === 'Escape' && props.modelValue) {
    onClose()
  }
}

onMounted(() => {
  window.addEventListener('keydown', onKeyDown)
})
onBeforeUnmount(() => {
  window.removeEventListener('keydown', onKeyDown)
})

// TODO(developer): 시트 열림 시 닫기 버튼에 focus 이동 (포커스 트랩)
watch(
  () => props.modelValue,
  (open) => {
    // 1차는 단순 — 필요 시 포커스 트랩 보강
  },
)
</script>

<style scoped>
.req-sheet__dimmer {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  display: flex;
  align-items: flex-end;
  justify-content: center;
  z-index: 100;
}

.req-sheet {
  width: 100%;
  max-width: 414px;
  background: var(--color-surface);
  border-top-left-radius: 20px;
  border-top-right-radius: 20px;
  padding: 8px 0 calc(16px + env(safe-area-inset-bottom, 0px));
  display: flex;
  flex-direction: column;
  max-height: 80vh;
}

.req-sheet__handle {
  width: 36px;
  height: 4px;
  background: var(--color-border);
  border-radius: var(--radius-full);
  margin: 4px auto 8px;
}

.req-sheet__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 4px 16px 8px;
}

.req-sheet__title {
  margin: 0;
  font-size: 18px;
  font-weight: 500;
  color: var(--color-text-primary);
}

.req-sheet__close {
  width: 32px;
  height: 32px;
  background: transparent;
  border: 0;
  color: var(--color-text-secondary);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}

.req-sheet__body {
  flex: 1;
  overflow-y: auto;
  padding: 4px 16px 12px;
}

.req-sheet__footer {
  padding: 8px 16px 0;
  border-top: 0.5px solid var(--color-border-light);
}

.req-sheet-fade-enter-active,
.req-sheet-fade-leave-active {
  transition: opacity 0.18s ease;
}
.req-sheet-fade-enter-from,
.req-sheet-fade-leave-to {
  opacity: 0;
}
</style>
```

### 8.6 `prafta-app-frontend/src/views/req/components/RequestStatusFilterSheet.vue`

```vue
<!--
  RequestStatusFilterSheet.vue — 상태 다중 선택 시트
  - 작업 ID: PRAFTA-APP-006-3
-->
<template>
  <BaseBottomSheet
    :model-value="modelValue"
    title="상태"
    :show-footer="true"
    @update:model-value="$emit('update:modelValue', $event)"
  >
    <ul class="req-status-list">
      <li
        v-for="opt in OPTIONS"
        :key="opt.code"
        class="req-status-list__item"
        :class="{ 'req-status-list__item--on': localSelected.includes(opt.code) }"
        @click="toggle(opt.code)"
      >
        <span class="req-status-list__dot" :class="opt.dotClass" aria-hidden="true"></span>
        <span class="req-status-list__label">{{ opt.label }}</span>
        <span class="req-status-list__check" aria-hidden="true">
          <svg
            v-if="localSelected.includes(opt.code)"
            width="18"
            height="18"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
            stroke-linecap="round"
            stroke-linejoin="round"
          >
            <polyline points="20 6 9 17 4 12" />
          </svg>
        </span>
      </li>
    </ul>

    <template #footer>
      <div class="req-status-footer">
        <button type="button" class="req-status-footer__reset" @click="onResetInternal">
          초기화
        </button>
        <button
          type="button"
          class="req-status-footer__apply"
          :disabled="localSelected.length === 0"
          @click="onApply"
        >
          {{ localSelected.length }}개 선택 적용
        </button>
      </div>
    </template>
  </BaseBottomSheet>
</template>

<script setup>
import { ref, watch } from 'vue'
import BaseBottomSheet from './BaseBottomSheet.vue'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  selected: { type: Array, default: () => [] },
})
const emit = defineEmits(['update:modelValue', 'apply'])

const OPTIONS = [
  { code: '01', label: '대기', dotClass: 'req-status-list__dot--warning' },
  { code: '02', label: '승인', dotClass: 'req-status-list__dot--primary' },
  { code: '03', label: '반려', dotClass: 'req-status-list__dot--danger' },
  { code: '04', label: '취소', dotClass: 'req-status-list__dot--neutral' },
]

const localSelected = ref([...props.selected])

watch(
  () => props.modelValue,
  (open) => {
    if (open) {
      // 열릴 때 상위 selected 와 동기화
      localSelected.value = [...props.selected]
    }
  },
)

const toggle = (code) => {
  const idx = localSelected.value.indexOf(code)
  if (idx >= 0) {
    localSelected.value.splice(idx, 1)
  } else {
    localSelected.value.push(code)
  }
}

const onResetInternal = () => {
  localSelected.value = []
}

const onApply = () => {
  emit('apply', [...localSelected.value])
  emit('update:modelValue', false)
}
</script>

<style scoped>
.req-status-list {
  list-style: none;
  margin: 0;
  padding: 0;
}

.req-status-list__item {
  display: grid;
  grid-template-columns: auto 1fr auto;
  align-items: center;
  gap: 8px;
  padding: 14px 0;
  cursor: pointer;
  border-bottom: 0.5px solid var(--color-border-light);
  min-height: 44px;
}

.req-status-list__dot {
  width: 8px;
  height: 8px;
  border-radius: var(--radius-full);
  background: currentColor;
}
.req-status-list__dot--warning {
  color: var(--color-warning);
}
.req-status-list__dot--primary {
  color: var(--color-primary);
}
.req-status-list__dot--danger {
  color: var(--color-danger);
}
.req-status-list__dot--neutral {
  color: var(--color-text-tertiary);
}

.req-status-list__label {
  font-size: 14px;
  color: var(--color-text-primary);
}

.req-status-list__check {
  color: var(--color-primary);
  width: 20px;
  height: 20px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.req-status-footer {
  display: grid;
  grid-template-columns: 1fr 2fr;
  gap: 8px;
  padding: 4px 0;
}

.req-status-footer__reset {
  height: 48px;
  background: var(--color-surface);
  border: 0.5px solid var(--color-border);
  border-radius: var(--radius-md);
  color: var(--color-text-secondary);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
}

.req-status-footer__apply {
  height: 48px;
  background: var(--color-primary);
  border: 0;
  border-radius: var(--radius-md);
  color: var(--color-surface);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
}
.req-status-footer__apply:disabled {
  background: var(--color-border);
  color: var(--color-text-tertiary);
  cursor: not-allowed;
}
</style>
```

### 8.7 `prafta-app-frontend/src/views/req/components/RequestDateRangeFilterSheet.vue`

```vue
<!--
  RequestDateRangeFilterSheet.vue — 기간 from-to (WORK_YMD 기준)
  - 작업 ID: PRAFTA-APP-006-4
  - 1차는 native <input type="date"> 사용. 일자 셀렉터 도입은 §7 follow-up F8.
-->
<template>
  <BaseBottomSheet
    :model-value="modelValue"
    title="대상 기간"
    :show-footer="true"
    @update:model-value="$emit('update:modelValue', $event)"
  >
    <!-- 프리셋 칩 -->
    <div class="req-date-presets" role="group" aria-label="프리셋">
      <button
        v-for="p in PRESETS"
        :key="p.key"
        type="button"
        class="req-date-presets__chip"
        :class="{ 'req-date-presets__chip--on': activePreset === p.key }"
        @click="applyPreset(p.key)"
      >
        {{ p.label }}
      </button>
    </div>

    <!-- from-to 직접 입력 -->
    <div class="req-date-inputs">
      <label class="req-date-inputs__field">
        <span class="req-date-inputs__lbl">시작일</span>
        <input
          v-model="localFromInput"
          type="date"
          class="req-date-inputs__input"
          @change="onInputChange"
        />
      </label>
      <span class="req-date-inputs__tilde" aria-hidden="true">~</span>
      <label class="req-date-inputs__field">
        <span class="req-date-inputs__lbl">종료일</span>
        <input
          v-model="localToInput"
          type="date"
          class="req-date-inputs__input"
          @change="onInputChange"
        />
      </label>
    </div>

    <template #footer>
      <div class="req-date-footer">
        <button type="button" class="req-date-footer__reset" @click="onResetInternal">
          초기화
        </button>
        <button type="button" class="req-date-footer__apply" @click="onApply">
          적용하기
        </button>
      </div>
    </template>
  </BaseBottomSheet>
</template>

<script setup>
import { ref, watch } from 'vue'
import BaseBottomSheet from './BaseBottomSheet.vue'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  from: { type: String, default: '' }, // YYYYMMDD
  to: { type: String, default: '' },
})
const emit = defineEmits(['update:modelValue', 'apply'])

const PRESETS = [
  { key: 'last7', label: '최근 7일' },
  { key: 'last30', label: '최근 30일' },
  { key: 'last90', label: '최근 3개월' },
  { key: 'thisYear', label: '올해' },
]

const localFromInput = ref('') // YYYY-MM-DD (native input)
const localToInput = ref('')
const activePreset = ref('')

// YYYYMMDD ↔ YYYY-MM-DD 변환
const ymdToInput = (ymd) =>
  ymd && ymd.length === 8 ? `${ymd.slice(0, 4)}-${ymd.slice(4, 6)}-${ymd.slice(6, 8)}` : ''
const inputToYmd = (s) => (s ? s.replace(/-/g, '') : '')

watch(
  () => props.modelValue,
  (open) => {
    if (open) {
      localFromInput.value = ymdToInput(props.from)
      localToInput.value = ymdToInput(props.to)
      activePreset.value = ''
    }
  },
)

// 오늘 기준 프리셋 계산 (developer 가 dayjs 등 도입 시 교체 가능)
const todayDate = () => new Date()
const fmtInput = (d) => {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const dd = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${dd}`
}

const applyPreset = (key) => {
  activePreset.value = key
  const today = todayDate()
  const toStr = fmtInput(today)
  let fromStr = ''
  if (key === 'last7') {
    const d = new Date(today)
    d.setDate(d.getDate() - 7)
    fromStr = fmtInput(d)
  } else if (key === 'last30') {
    const d = new Date(today)
    d.setDate(d.getDate() - 30)
    fromStr = fmtInput(d)
  } else if (key === 'last90') {
    const d = new Date(today)
    d.setDate(d.getDate() - 90)
    fromStr = fmtInput(d)
  } else if (key === 'thisYear') {
    fromStr = `${today.getFullYear()}-01-01`
    localFromInput.value = fromStr
    localToInput.value = `${today.getFullYear()}-12-31`
    return
  }
  localFromInput.value = fromStr
  localToInput.value = toStr
}

const onInputChange = () => {
  // 사용자가 직접 수정하면 프리셋 활성 해제
  activePreset.value = ''
}

const onResetInternal = () => {
  localFromInput.value = ''
  localToInput.value = ''
  activePreset.value = ''
}

const onApply = () => {
  emit('apply', {
    from: inputToYmd(localFromInput.value),
    to: inputToYmd(localToInput.value),
  })
  emit('update:modelValue', false)
}
</script>

<style scoped>
.req-date-presets {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
  padding: 4px 0 12px;
}

.req-date-presets__chip {
  height: 32px;
  padding: 0 12px;
  background: var(--color-surface);
  border: 0.5px solid var(--color-border);
  border-radius: var(--radius-full);
  color: var(--color-text-secondary);
  font-size: 12px;
  cursor: pointer;
}
.req-date-presets__chip--on {
  background: var(--color-primary-tint);
  border-color: var(--color-primary-tint-border);
  color: var(--color-primary);
}

.req-date-inputs {
  display: grid;
  grid-template-columns: 1fr auto 1fr;
  gap: 8px;
  align-items: end;
  padding: 8px 0 16px;
}

.req-date-inputs__field {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.req-date-inputs__lbl {
  font-size: 12px;
  color: var(--color-text-secondary);
}

.req-date-inputs__input {
  height: 40px;
  padding: 0 10px;
  background: var(--color-surface);
  border: 0.5px solid var(--color-border);
  border-radius: var(--radius-sm);
  color: var(--color-text-primary);
  font-size: 14px;
  font-family: inherit;
}
.req-date-inputs__input:focus {
  outline: none;
  border-color: var(--color-primary);
}

.req-date-inputs__tilde {
  padding-bottom: 10px;
  color: var(--color-text-tertiary);
}

.req-date-footer {
  display: grid;
  grid-template-columns: 1fr 2fr;
  gap: 8px;
  padding: 4px 0;
}

.req-date-footer__reset {
  height: 48px;
  background: var(--color-surface);
  border: 0.5px solid var(--color-border);
  border-radius: var(--radius-md);
  color: var(--color-text-secondary);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
}

.req-date-footer__apply {
  height: 48px;
  background: var(--color-primary);
  border: 0;
  border-radius: var(--radius-md);
  color: var(--color-surface);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
}
</style>
```

### 8.8 `prafta-app-frontend/src/views/req/components/RequestTypeFilterSheet.vue`

```vue
<!--
  RequestTypeFilterSheet.vue — 요청 유형 다중 선택 (SYS032 6종)
  - 작업 ID: PRAFTA-APP-006-5
-->
<template>
  <BaseBottomSheet
    :model-value="modelValue"
    title="요청 유형"
    :show-footer="true"
    @update:model-value="$emit('update:modelValue', $event)"
  >
    <ul class="req-type-list">
      <li
        v-for="opt in OPTIONS"
        :key="opt.code"
        class="req-type-list__item"
        @click="toggle(opt.code)"
      >
        <span class="req-type-list__label">{{ opt.label }}</span>
        <span class="req-type-list__check" aria-hidden="true">
          <svg
            v-if="localSelected.includes(opt.code)"
            width="18"
            height="18"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
            stroke-linecap="round"
            stroke-linejoin="round"
          >
            <polyline points="20 6 9 17 4 12" />
          </svg>
        </span>
      </li>
    </ul>

    <template #footer>
      <div class="req-type-footer">
        <button type="button" class="req-type-footer__reset" @click="onResetInternal">
          초기화
        </button>
        <button
          type="button"
          class="req-type-footer__apply"
          :disabled="localSelected.length === 0"
          @click="onApply"
        >
          {{ localSelected.length }}개 선택 적용
        </button>
      </div>
    </template>
  </BaseBottomSheet>
</template>

<script setup>
import { ref, watch } from 'vue'
import BaseBottomSheet from './BaseBottomSheet.vue'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  selected: { type: Array, default: () => [] },
})
const emit = defineEmits(['update:modelValue', 'apply'])

const OPTIONS = [
  { code: '01', label: '근태 생성' },
  { code: '02', label: '근태 수정' },
  { code: '03', label: '초과근무 생성' },
  { code: '04', label: '초과근무 수정' },
  { code: '05', label: '연차 사용' },
  { code: '06', label: '연차 수정' },
]

const localSelected = ref([...props.selected])

watch(
  () => props.modelValue,
  (open) => {
    if (open) localSelected.value = [...props.selected]
  },
)

const toggle = (code) => {
  const idx = localSelected.value.indexOf(code)
  if (idx >= 0) localSelected.value.splice(idx, 1)
  else localSelected.value.push(code)
}

const onResetInternal = () => {
  localSelected.value = []
}

const onApply = () => {
  emit('apply', [...localSelected.value])
  emit('update:modelValue', false)
}
</script>

<style scoped>
.req-type-list {
  list-style: none;
  margin: 0;
  padding: 0;
}

.req-type-list__item {
  display: grid;
  grid-template-columns: 1fr auto;
  align-items: center;
  padding: 14px 0;
  cursor: pointer;
  border-bottom: 0.5px solid var(--color-border-light);
  min-height: 44px;
}

.req-type-list__label {
  font-size: 14px;
  color: var(--color-text-primary);
}

.req-type-list__check {
  color: var(--color-primary);
  width: 20px;
  height: 20px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.req-type-footer {
  display: grid;
  grid-template-columns: 1fr 2fr;
  gap: 8px;
  padding: 4px 0;
}

.req-type-footer__reset {
  height: 48px;
  background: var(--color-surface);
  border: 0.5px solid var(--color-border);
  border-radius: var(--radius-md);
  color: var(--color-text-secondary);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
}

.req-type-footer__apply {
  height: 48px;
  background: var(--color-primary);
  border: 0;
  border-radius: var(--radius-md);
  color: var(--color-surface);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
}
.req-type-footer__apply:disabled {
  background: var(--color-border);
  color: var(--color-text-tertiary);
  cursor: not-allowed;
}
</style>
```

### 8.9 `prafta-app-frontend/src/views/req/components/RequestSortSheet.vue`

```vue
<!--
  RequestSortSheet.vue — 정렬 라디오 단일 선택 (선택 즉시 적용)
  - 작업 ID: PRAFTA-APP-006-6
-->
<template>
  <BaseBottomSheet
    :model-value="modelValue"
    title="정렬"
    :show-footer="false"
    @update:model-value="$emit('update:modelValue', $event)"
  >
    <ul class="req-sort-list">
      <li
        v-for="opt in OPTIONS"
        :key="opt.code"
        class="req-sort-list__item"
        @click="onPick(opt.code)"
      >
        <span class="req-sort-list__label">{{ opt.label }}</span>
        <span class="req-sort-list__radio" aria-hidden="true">
          <span
            class="req-sort-list__radio-dot"
            :class="{ 'req-sort-list__radio-dot--on': selected === opt.code }"
          ></span>
        </span>
      </li>
    </ul>
  </BaseBottomSheet>
</template>

<script setup>
import BaseBottomSheet from './BaseBottomSheet.vue'

defineProps({
  modelValue: { type: Boolean, default: false },
  selected: { type: String, default: 'PENDING_FIRST' },
})
const emit = defineEmits(['update:modelValue', 'apply'])

const OPTIONS = [
  { code: 'PENDING_FIRST', label: '대기 우선 (기본)' },
  { code: 'RECENT', label: '최근 요청순' },
  { code: 'TARGET_DATE', label: '대상일자 가까운순' },
]

const onPick = (code) => {
  // 선택 즉시 적용 + 자동 닫힘 (시안 §4.4.4)
  emit('apply', code)
  emit('update:modelValue', false)
}
</script>

<style scoped>
.req-sort-list {
  list-style: none;
  margin: 0;
  padding: 0;
}

.req-sort-list__item {
  display: grid;
  grid-template-columns: 1fr auto;
  align-items: center;
  padding: 14px 0;
  cursor: pointer;
  border-bottom: 0.5px solid var(--color-border-light);
  min-height: 44px;
}

.req-sort-list__label {
  font-size: 14px;
  color: var(--color-text-primary);
}

.req-sort-list__radio {
  width: 20px;
  height: 20px;
  border-radius: var(--radius-full);
  border: 1.5px solid var(--color-border);
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.req-sort-list__radio-dot {
  width: 10px;
  height: 10px;
  border-radius: var(--radius-full);
  background: transparent;
}
.req-sort-list__radio-dot--on {
  background: var(--color-primary);
}
.req-sort-list__radio:has(.req-sort-list__radio-dot--on) {
  border-color: var(--color-primary);
}
</style>
```

---

## 9. 라우트 패치 골격

> **PRAFTA-APP-006-11.** `prafta-app-frontend/src/router/index.js` 의 `routes[]` 배열에 다음 1줄 추가 (MyAttendance 등록 위치 바로 아래 권장).

```js
// PRAFTA-APP-006: 내 승인 요청 목록 화면
{
  path: '/MyRequests',
  name: 'MyRequests',
  component: () => import('@/views/req/MyRequestsView.vue'),
},
```

`publicPaths` 에 추가하지 않음 (로그인 필요 라우트).

---

## 10. 메인 홈 진입 동선 골격

> **PRAFTA-APP-006-11.** `prafta-app-frontend/src/views/main/MainView.vue` 의 `onApprovalClick` 핸들러 본문 교체.

```js
// before (현재)
const onApprovalClick = () => {
  // TODO(developer): 본인 요청 목록 화면 진입 (별도 라우트 신설)
  showAlert('준비 중입니다')
}

// after (PRAFTA-APP-006-11)
const onApprovalClick = () => {
  // PRAFTA-APP-006: 내 승인 요청 목록 화면 진입.
  // 메인 홈 KPI(approval.pendingCount)와 본 화면의 대기 건수는
  // home-summary 의 onMounted 재호출로 자연 동기화된다 (P10).
  router.push('/MyRequests')
}
```

---

## 11. 차후 분해 시 메모

- F1 (요청 상세 화면) 분해 시 `tb_user_attd_req_hist` 의 처리 이력(HIST_TYPE 07/08/09) 을 타임라인 형태로 노출하도록 설계 (`common/13-ui-ux.md` §13.3 "처리 이력은 타임라인 형태로 시각화").
- F2 (정밀 가공) 분해 시 매퍼에 `tb_user_attd_mgmt` (1구간/2구간 원본 시각) join 필요. WORK_SEQ 가 null 인 케이스 (스케줄 미정 일자 보정) 분기 명시.
- F3 (취소 흐름) 분해 시 `common/09-locking.md` 선점 정책 검토 (관리자가 처리 중이면 사용자 취소 차단).
- 본 plan 의 `req06` 패키지명은 prafta-app-006 작업 ID 와 자연 매칭한 것이며, 향후 `web.req.req06` (관리자 본인 요청 조회) 또는 다른 req 모듈과 충돌하지 않는다 (app/web 패키지 분리됨).
- BE 1번 endpoint 의 `summary.lines` 가공이 응답 페이로드 크기를 좌우한다. items 20개 기준 평균 lines 1.5개 → 응답 30~50 lines 예상. 무한 스크롤 성능 OK.
- 본 plan 작성 시점에 `prafta-app-frontend/src/views/req/` 디렉토리는 미존재. 작업 1번 후 컴포넌트 작성 시 디렉토리 신규 생성.
