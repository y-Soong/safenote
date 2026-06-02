# PRAFTA-app-012 — 화면 명세 (앱 파트)

> 설계 출처: `.claude/context/near-miss-incident-design.md` §5-A/5-B. plan: `prafta-app-012-plan.md`.
> 참조 패턴(앱 디자인 시스템): `MainView.vue`/`MyRequestsView.vue`(디자인 토큰 .root 선언, BEM, scoped, 56px 헤더 + sticky), `Risk_01.vue`(단일 사진 multipart 첨부 input), `SafetyInspectSavedView.vue`(푸터 2버튼), `MyAttendanceView`(탭/카드).
> 환경: Vue 3 + Vite (JS only), 해시 라우터, `@/api/axios`(appApi 프리픽스·gv_* 자동주입·JWT), 전역 `$alert`/`$confirm`.
> 디자인 토큰: 앱은 공용 토큰 파일 대신 **각 화면 루트(.xxx-view)에 토큰 세트를 1회 선언**하는 관례(MainView L550~). 신규 화면도 동일 세트 복제.

---

## UI-app-012-1 NearMissReport (근로자 아차사고 보고)

- 연결 작업: PRAFTA-app-012-3 (BE 의존: A1 POST `/appApi/nearmiss/report`)
- 화면 위치: `prafta-app-frontend/prafta-app-frontend/src/views/nearmiss/NearMissReport.vue`
- 진입: 메인 안전 활동 카드 "아차사고 보고"(D-A5) → `router.push('/NearMissReport')`
- 참조 패턴: Risk_01(단일 사진 첨부 + 셀렉트), MyAttendance(헤더), SafetyInspectSaved(푸터 버튼)

### 레이아웃 와이어프레임
```
┌────────────────────────────┐
│ ←   아차사고 보고            │  56px 헤더(sticky)
├────────────────────────────┤
│ 유형 *                      │
│ (●)아차사고 ( )경미사고      │  라디오(SYS061)
│ ( )유해·위험요인 발견        │
│ 공정(선택)  [ 셀렉트 ▾ ]     │  COM002(선택)
│ 발생일시 *  [ datetime ]    │
│ 발생장소    [ 텍스트       ] │
│ 무슨 일이 있었나요? *        │
│ ┌────────────────────────┐ │  textarea(필수)
│ │                        │ │
│ └────────────────────────┘ │
│ 사고였다면 피해 정도는?      │
│ ( )경미 (●)중대 ( )치명      │  라디오(SYS062)
│ 사진(선택)  [ ＋ 사진 ] [▣] │  단일 첨부
│ 즉시 조치   [ 텍스트       ] │
├────────────────────────────┤
│      [    보고하기    ]     │  푸터 버튼(sticky)
└────────────────────────────┘
```

### 컴포넌트 매핑
| 영역 | 사용 | 비고 |
|---|---|---|
| 헤더 | native button + svg sprite(뒤로) | MyRequestsView 헤더 패턴(공통 헤더 컴포넌트 없음 — 앱은 화면별 헤더) |
| 유형/잠재중대성 | native radio + label | 앱에 공통 라디오 컴포넌트 없음(Risk_01 도 native) |
| 공정 셀렉트 | native select | Risk_01 select 패턴 |
| 발생일시 | native `<input type="datetime-local">` | 웹뷰 호환 |
| 경위/즉시조치 | native textarea/input | |
| 사진 | 오프스크린 `<input type="file" accept="image/*">` + 미리보기 | Risk_01 L73~82 동일 |
| 보고하기 | native button(푸터) | SafetyInspectSaved 푸터 버튼 |

> 앱은 공통 폼 컴포넌트 카탈로그가 빈약(Risk_01·login 등 모두 native + scoped). 따라서 native HTML + 토큰 스타일이 이 코드베이스의 정상 패턴(웹 prafta 디자인 시스템과 다름). SidePanel 외 공용 입력 컴포넌트 부재 확인.

### 상태별 동작
- loading: 보고하기 클릭 후 버튼 `disabled` + "보고 중..." (이중 제출 방지).
- empty: 해당 없음(입력 폼).
- error: A1 실패 시 `$alert(서버 message)`. 422(전이) 무관(보고는 INSERT).
- success: `$alert('보고했어요. 관리자에게 전달됩니다')` 후 `router.back()` 또는 메인 이동(developer).
- validation(골격 허용 최소): description 필수, occurDtime 필수, incidentTypeCd 필수. 미충족 시 `$alert` 후 중단.

### 사용자 플로우
진입 → 유형/발생일시/경위(필수) 입력 → (선택)장소·잠재중대성·사진·즉시조치 → [보고하기] → multipart A1 → 성공 알림 → 복귀.

### 백엔드 의존
- POST `/appApi/nearmiss/report` (multipart, PRAFTA-app-012-2 A1). REPORT_STATUS_CD='100', REPORTER_ID/SITE_CD=JWT, SRC_*=NULL.

---

## UI-app-012-2 NearMissManageList (관리자 사업장 사건 목록)

- 연결 작업: PRAFTA-app-012-4 (BE 의존: A3 `/site-incidents`, A4 `/status-counts`)
- 화면 위치: `prafta-app-frontend/prafta-app-frontend/src/views/nearmiss/NearMissManageList.vue`
- 진입: 메인 "사건 관리"(관리자 게이팅, D-A5) → `router.push('/NearMissManageList')`
- 참조 패턴: MyRequestsView(헤더+필터바+카드리스트), MyAttendance(상태탭/카운트)

### 레이아웃 와이어프레임
```
┌────────────────────────────┐
│ ←  사건 관리 (중곡사업장)    │  56px 헤더
├────────────────────────────┤
│ [접수 2][검토중 1][조치중 0] │  상태탭(카운트 배지, A4)
├────────────────────────────┤
│ ● 중대   NM20260530-003     │  카드(잠재중대성 배지)
│ 지게차 후진 충돌 위험        │  description 1줄 ellipsis
│ 3공정 · 김작업 · 05-30 14:20 │  공정/보고자/발생일시
│ [접수]                      │  상태 칩
├────────────────────────────┤
│ ○ 경미   NM20260530-002     │
│ ...                         │
└────────────────────────────┘
(빈 상태) "표시할 사건이 없어요"
```

### 컴포넌트 매핑
| 영역 | 사용 | 비고 |
|---|---|---|
| 헤더 | native button + svg(뒤로) + 사업장명 | gv_siteNm 세션 |
| 상태탭 | native button 그룹 + 카운트 배지 | A4 status-counts |
| 카드 | 화면 내 인라인(반복) | 별도 컴포넌트 미분리(1차 단순화). 필요 시 developer 분리 |
| 잠재중대성 배지 | span.badge--경미/중대/치명 | 색상=토큰(경미=primary, 중대=warning, 치명=danger) |
| 빈 상태 | div 텍스트 | MyRequests EmptyState 패턴 간이화 |

### 상태별 동작
- loading: "불러오는 중..." (MainView L35 패턴).
- empty: 사업장 사건 0건 → "표시할 사건이 없어요".
- error: A3 실패 → `$alert`. 403(사업장 권한 없음) → "사건 관리 권한이 없어요" 후 `router.back()`.
- success: 카드 리스트 렌더 + 탭 카운트.

### 사용자 플로우
진입(관리자) → 상태탭 선택(필터) → 카드 클릭 → 상세(UI-app-012-3) 이동(query nearMissId).

### 백엔드 의존
- GET `/appApi/nearmiss/site-incidents` (A3) — assertSiteAccess.
- GET `/appApi/nearmiss/status-counts` (A4).

---

## UI-app-012-3 NearMissManageDetail (관리자 1차 확인 상세)

- 연결 작업: PRAFTA-app-012-5 (BE 의존: A5 `/detail`, A6 `/change-status`)
- 화면 위치: `prafta-app-frontend/prafta-app-frontend/src/views/nearmiss/NearMissManageDetail.vue`
- 진입: 목록 카드 클릭 → `router.push({ path:'/NearMissManageDetail', query:{ nearMissId } })`
- 참조 패턴: SafetyInspectSaved(읽기 본문 + 푸터 2버튼), Risk_01(사진 미리보기 읽기)

### 레이아웃 와이어프레임
```
┌────────────────────────────┐
│ ← 사건 상세      ● 중대      │  헤더 + 잠재중대성 배지
├────────────────────────────┤
│ 유형     아차사고            │  읽기 블록
│ 발생     05-30 14:20         │
│ 장소     3공정 컨베이어 옆    │
│ 경위     지게차 후진 중...    │
│ 보고자   김작업 / 14:22      │
│ 즉시조치 후진경보 점검 요청   │  보고자 IMMEDIATE_ACTION_DESC
│ 사진     [▣]                │  단일(filePath)
│ 상태     접수                │
│ ─────────────────────────  │
│ 임시조치(관리자)             │
│ [ 경보 점검 지시           ] │  textarea(ADMIN_TEMP_ACTION_DESC)
├────────────────────────────┤
│ [   반려   ] [ 접수→검토중 ] │  푸터 2버튼
└────────────────────────────┘
(반려 시) 사유 입력 시트/프롬프트 → 사유 필수
```

### 컴포넌트 매핑
| 영역 | 사용 | 비고 |
|---|---|---|
| 헤더 + 배지 | native + span.badge | 목록과 동일 배지 |
| 읽기 본문 | dl/dt/dd 또는 row div | SafetyInspectSaved 본문 톤 |
| 사진 | img(filePath) | 없으면 미표시 |
| 임시조치 입력 | native textarea | ADMIN_TEMP_ACTION_DESC |
| 푸터 | native button x2(반려/검토중 전환) | SafetyInspectSaved 푸터(secondary/primary) |
| 반려 사유 | `$confirm`/간이 시트 또는 prompt | 사유 필수(900) |

### 상태별 동작
- loading: 상세 조회 중 "불러오는 중...".
- error: A5 실패 → `$alert` 후 `router.back()`. 404 → "사건을 찾을 수 없어요".
- success(전환): A6(200) 성공 → `$alert('검토중으로 변경했어요')` 후 목록 복귀(재조회).
- success(반려): A6(900, 사유) 성공 → `$alert('반려 처리했어요')` 후 복귀.
- 가드: 현재 상태가 '접수(100)' 아닐 때 "접수→검토중" 버튼 비활성(앱은 100→200 만 허용). 그 외 단계는 "웹에서 처리" 안내.
- validation(골격 허용): 반려 시 사유 미입력이면 `$alert` 후 중단.

### 사용자 플로우
진입 → 상세 읽기 → 임시조치 메모 입력(선택) → [접수→검토중] 또는 [반려(사유)] → A6 → 성공 알림 → 목록 복귀.

### 백엔드 의존
- GET `/appApi/nearmiss/detail` (A5) — assertSiteAccess.
- POST `/appApi/nearmiss/change-status` (A6) — 200(임시조치 동반) / 900(사유 필수).

---

## 공통 사항
- 반응형: 모바일 웹뷰 단일 폭(브레이크포인트 불요). MainView 와 동일.
- 접근성: 버튼 min 44px, aria-label(뒤로 등). MyRequestsView 동일.
- 색상/폰트/간격: CSS 변수만(토큰). 하드코딩·`!important` 금지. `<style scoped>` 필수.
- TypeScript 금지(JS only).

---

## Notion 등록 초안 (메인 세션 대행 — "도메인 지식 베이스" DB)

> 서브에이전트는 Notion 미접근. 아래를 메인 세션이 등록.

| 이름 | 영역 | 모듈 | 현재 동작 | 의도된 동작 | 검증 상태 |
|---|---|---|---|---|---|
| UI-app-012-1 NearMissReport | app | nearmiss | 신규 작성 | (본 문서 UI-app-012-1 전체) | Claude 분석 |
| UI-app-012-2 NearMissManageList | app | nearmiss | 신규 작성 | (본 문서 UI-app-012-2 전체) | Claude 분석 |
| UI-app-012-3 NearMissManageDetail | app | nearmiss | 신규 작성 | (본 문서 UI-app-012-3 전체) | Claude 분석 |

작업 로그(PRAFTA-app-012-3/4/5) 상세 설명에 `[UI 명세: UI-app-012-1/2/3]` 태그 연결. 산출물 컬럼에 각 .vue 경로 기록.
