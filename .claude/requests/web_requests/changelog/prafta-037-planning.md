# prafta-037 — 사용자 앱 메인 홈 화면 (FE 한정 1차 분해)

> **작업 요청서**: `.claude/requests/prafta-037.md` + `.claude/requests/ref/prafta-037/prafta-request-home.md`
> **시안**: `.claude/requests/ref/prafta-037/prafta_home_variations.html` (단일 출처 — DOM/디자인 토큰/SVG 모두 그대로 차용)
> **대상 프로젝트**: `PRAFTA/prafta-app-frontend/prafta-app-frontend/` (Vue 3 + Vite, 모바일 webview)
> **본 라운드 스코프**: 정규직 사용자 화면 한정 FE 골격. 백엔드/사용자 유형 분기/모달/신규 라우트 외 모두 deferred.
> **분해일자**: 2026-05-28
> **작성자**: planner (prafta-037-1 분해)

---

## 1. 분해 원칙

1. 짧은 본문(`prafta-037.md`)이 "prafta-app-frontend 요청"임을 명시 → 상세 요청서 §1.2 의 `prafta-web-frontend/...` 경로는 **오기**로 처리. 작업 대상은 모바일 앱 프로젝트.
2. 시안 HTML 의 케이스 4종(출근 전 / 근무중 / 사업장 외 / TBM 없는 날)을 **단일 화면 + reactive state** 로 표현. 4개 화면으로 쪼개지 않음.
3. 5개 카드를 **각자 .vue 컴포넌트**로 분리 (상세 요청서 §9 N9~N15 대응).
4. 시안의 인라인 SVG 14개는 **단일 sprite 컴포넌트** `HomeIcons.vue` 로 묶음(CDN 의존 금지, 모든 카드에서 `<svg><use href="#i-..."/></svg>` 패턴 재사용).
5. CSS 변수는 시안 `:root` 토큰을 **그대로** 컴포넌트별 `<style scoped>` 상위 selector(`.home-view`)에 선언. Tailwind 와 무관하게 scoped CSS 만 사용.
6. 상태바(`.statusbar`)는 OS 가 제공하므로 **제외**.
7. `script setup` 영역은 refs 선언 + TODO 마커만. axios/Pinia/router 호출 코드 작성 금지.

---

## 2. 작업 ID 분해표 (PLNprafta-037NNN)

> ID 채번 규칙: `PLNprafta-037` + 3자리 순번. 백엔드/DB/사용자분기/모달 작업은 후속 라운드용 deferred(§5)에 정리.

| 작업 ID | 유형 | 제목 | 대상 파일 | 정책서 출처 | 우선순위 |
|---|---|---|---|---|---|
| PLNprafta-037001 | frontend-screen | MainView 신규 구조 교체 (5카드+헤더+탭바 조립, reactive state 선언) | `src/views/main/MainView.vue` | 상세 §1.3, §10 결정사항 1·2·4 / common §13 UI/UX | P0 (선행) |
| PLNprafta-037002 | frontend-component | HomeIcons SVG sprite (14개 symbol 통합) | `src/views/main/components/HomeIcons.vue` | 상세 §8.1 (CDN 의존 금지) | P0 (다른 카드의 선행) |
| PLNprafta-037003 | frontend-component | HomeHeader (사업장 정적 표시 + 알림벨 + 아바타) | `src/views/main/components/HomeHeader.vue` | 상세 §3.1 / common §6 사업장, §10 알림 | P1 |
| PLNprafta-037004 | frontend-component | AttendanceCard (출퇴근 — 케이스 1/2/3 통합) | `src/views/main/components/AttendanceCard.vue` | 상세 §3.2 / attd §5.1 §5.2 §7.1 §7.2 §7.3 / common §13.3 | P1 |
| PLNprafta-037005 | frontend-component | AttendanceSummaryCard (근태조회 — 잔여연차/승인요청 KPI) | `src/views/main/components/AttendanceSummaryCard.vue` | 상세 §3.3 / attd §8.1 §9.4 / 요청승인관리 재기획서 §1.2 | P1 |
| PLNprafta-037006 | frontend-component | SafetyActivityCard (안전점검 시작 + 위험성 발굴, 차단 배너) | `src/views/main/components/SafetyActivityCard.vue` | 상세 §3.4 / common §13.3 (차단 UI 일관성) | P1 |
| PLNprafta-037007 | frontend-component | TbmAttendCard (4 상태 — 차단/가능/완료/없음) | `src/views/main/components/TbmAttendCard.vue` | 상세 §3.5 / common §10 알림 | P1 |
| PLNprafta-037008 | frontend-component | NoticeListCard (공지 리스트 최대 3행) | `src/views/main/components/NoticeListCard.vue` | 상세 §3.6 / common §10 알림 | P1 |
| PLNprafta-037009 | frontend-component | HomeTabBar (하단 5탭 — 홈/근태/안전/TBM/마이) | `src/views/main/components/HomeTabBar.vue` | 상세 §1.3, §4.1 | P1 |

### 2.1 각 작업 상세 (정책서 출처 포함)

#### PLNprafta-037001 — MainView 신규 구조 교체

- **핵심 요구사항**:
  1. 기존 Tailwind 기반 `MainView.vue` 전체 폐기, 시안 HTML 의 `.page > .case > .phone > .main` 구조 차용.
  2. 5개 카드 컴포넌트를 자식으로 import 하여 조립. 헤더/인사말/탭바도 자식.
  3. reactive state(`attdStatus`, `isOffsite`, `tbmStatus`)를 선언만 하고 자식에 props 로 전달.
  4. 케이스 분기는 단일 화면 안에서 처리. 4개 페이지 분리 금지.
  5. `<script setup>` 은 import + ref 선언 + `// TODO(developer):` 마커만.
- **영향 받는 파일**: `src/views/main/MainView.vue` (전체 교체)
- **연결 UI 명세**: 상세 요청서 §1.3, §4.x
- **정책서 출처**:
  - 상세 §10 결정사항 1 (메인 카드 5종 확정) / 2 (사업장 셀렉터 제거) / 4 (출퇴근 5영역 고정) / 5 (케이스 3개로 통합)
  - common §13 UI/UX (디자인 토큰 사용)
  - common §13.3 공통 인터랙션 원칙
- **예상 산출물**: `.vue` 1개
- **선행**: 없음(다른 모든 골격이 본 화면에 합쳐짐)

#### PLNprafta-037002 — HomeIcons SVG sprite

- **핵심 요구사항**:
  1. 시안 HTML 의 14개 `<symbol id="i-...">` 를 단일 `.vue` 로 묶음.
  2. App.vue 또는 MainView.vue 가 마운트되는 시점에 1회 inline 렌더. `<svg width="0" height="0" style="position:absolute" aria-hidden="true"><defs>...</defs></svg>` 구조 그대로.
  3. 호출부는 `<svg class="icon"><use href="#i-mappin"/></svg>` 패턴.
  4. CDN/외부 라이브러리 의존 금지.
- **영향 받는 파일**: `src/views/main/components/HomeIcons.vue` (신규)
- **포함 symbol id (14개)**: `i-signal`, `i-wifi`, `i-battery`, `i-mappin`, `i-mappinoff`, `i-bell`, `i-chev`, `i-lock`, `i-clipboard`, `i-camera`, `i-clock`, `i-check`, `i-circlecheck`, `i-usercheck`, `i-caloff`, `i-home`, `i-cal`, `i-shield`, `i-monitor`, `i-user` (총 20개 — 시안 정독 시 통계상 20개. 작업 지시서의 14개는 가시 카드 기준이며 sprite 는 시안에 정의된 전체를 포함)
- **결정 사유**: 시안에 정의된 20개 symbol 을 모두 포함. 사용처가 없는 일부 symbol(`i-signal`/`i-wifi`/`i-battery` 등 상태바용)도 차후 모달/위젯에서 재사용 가능성이 있어 유지.
- **정책서 출처**: 상세 §8.1 (인라인 SVG, CDN 의존 금지)
- **예상 산출물**: `.vue` 1개
- **선행**: 없음

#### PLNprafta-037003 — HomeHeader

- **핵심 요구사항**:
  1. 좌측: `i-mappin` 아이콘(Primary 컬러) + 사업장명 + "소속" 배지(`.site .tag`).
  2. 우측: 알림 벨(`.h-bell`, 카운트 배지 포함) + 아바타 버튼(`.avatar`, 사용자 이니셜 2자).
  3. 드롭다운/셀렉터/바텀시트 호출 없음 (사업장 전환 기능 없음).
  4. props: `siteName`(string), `notificationCount`(number), `userInitial`(string).
  5. emits: `click:bell`, `click:avatar`.
- **영향 받는 파일**: `src/views/main/components/HomeHeader.vue` (신규)
- **정책서 출처**:
  - 상세 §3.1 (사업장 정적 표시) / §4.1 헤더 56px
  - common §6 (사업장 권한)
  - common §10 (알림)
- **예상 산출물**: `.vue` 1개
- **선행**: PLNprafta-037002 (sprite)

#### PLNprafta-037004 — AttendanceCard

- **핵심 요구사항**:
  1. 5영역 구조(상태배지 + 시간텍스트 / 출근버튼 + 퇴근버튼 / 위치). 시안 `.card.first` DOM 그대로.
  2. 케이스 분기는 props 로 받음:
     - `status`: `'BEFORE_WORK'` | `'WORKING'`
     - `isOffsite`: boolean
     - `scheduleStartTime`/`scheduleEndTime`: string (HHMM)
     - `checkInTime`: string|null (HHMM)
     - `siteName`: string
     - `canCheckIn`/`canCheckOut`: boolean
  3. 배지: `BEFORE_WORK` → `.badge.before` "출근 전" / `WORKING` → `.badge.ok` "근무중"
  4. 시간 텍스트: `BEFORE_WORK` → "예정 HH:MM ~ HH:MM" tertiary / `WORKING` → "출근 HH:MM" secondary
  5. 위치 메타: 미출근→`.loc-meta.neutral "-"` / 사업장내→`.loc-meta.neutral {사업장명}` Primary mappin / 사업장외→`.loc-meta.warn "사업장 외"` Warning mappinoff
  6. 버튼: 출근 활성/비활성 + 퇴근 활성/비활성 (`canCheckIn`/`canCheckOut` 기준)
  7. emits: `click:checkin`, `click:checkout`.
  8. **클릭 핸들러 안에 `// TODO(developer):` 마커**로 출퇴근 확인 모달 호출 위치 표시 (모달 컴포넌트는 본 라운드 outside scope).
- **영향 받는 파일**: `src/views/main/components/AttendanceCard.vue` (신규)
- **정책서 출처**:
  - 상세 §3.2 (5영역 + 상태 매트릭스)
  - attd §5.1 (출퇴근 횟수/구간 제한), §5.2 (재출근 조건)
  - attd §7.1 (출퇴근 기본 규칙), §7.2 (GPS 지오펜스 판정 — `isOffsite` 값의 출처)
  - attd §7.3 (GPS 미확인 처리 — **본 라운드 outside scope**, 정책 7.4 deferred)
  - common §13.3 (공통 인터랙션 — 확인 모달은 별도 작업)
- **예상 산출물**: `.vue` 1개
- **선행**: PLNprafta-037002

#### PLNprafta-037005 — AttendanceSummaryCard

- **핵심 요구사항**:
  1. 우상단 chevron only 진입 버튼(`.head-chev`, 18×18px, `--color-text-tertiary`, hit area 44×44px).
  2. 2분할 KPI: 좌 잔여연차 / 우 승인요청.
  3. 잔여연차 표시: `{remainingDays} / {grantedDays}일` (warn 컬러 없음, tabular-nums).
  4. 승인요청 표시: `{pendingCount}` warn 컬러 + `건 대기` tertiary.
  5. 각 KPI 카드(`.kpi`) 전체 클릭 가능.
  6. props: `remainingDays`, `grantedDays`, `pendingCount`.
  7. emits: `click:detail`, `click:leave`, `click:approval`.
- **영향 받는 파일**: `src/views/main/components/AttendanceSummaryCard.vue` (신규)
- **정책서 출처**:
  - 상세 §3.3 (잔여연차/승인요청 KPI 정의)
  - attd §8.1 (연차 타입), §9.4 (휴가 신청)
  - 요청승인관리 재기획서 §1.2 (요청 승인 화면 단일 출처)
  - 상세 §10 결정사항 9 (잔여 연차 통합 표시) / 10 (승인요청 대기건만)
- **예상 산출물**: `.vue` 1개
- **선행**: PLNprafta-037002

#### PLNprafta-037006 — SafetyActivityCard

- **핵심 요구사항**:
  1. 타이틀 "안전 활동" + 우상단 chevron.
  2. props.blocked = true 면 `.blocked-banner` ("출근 후에 이용할 수 있어요") 노출 + 두 버튼 모두 `.safety-btn.disabled`.
  3. 활성 상태에서는 두 버튼 모두 `.safety-btn.active` (좌: clipboard "안전점검 시작" / 우: camera "위험성 발굴").
  4. props: `blocked`(boolean).
  5. emits: `click:detail`, `click:safetyCheck`, `click:riskDiscovery`.
  6. **버튼 핸들러 주석**으로 매핑 명시:
     - "안전점검 시작" → 기존 `fnDayChkLst()` 라우팅(`/QrScanner`) 로직 보존 (작업 요청서 본문 명시)
     - "위험성 발굴" → 기존 `fnRisk_01()` 라우팅(`/Risk_01`) 로직 보존
     - developer 는 emit 받아서 부모(MainView)에서 라우팅 처리.
- **영향 받는 파일**: `src/views/main/components/SafetyActivityCard.vue` (신규)
- **정책서 출처**:
  - 상세 §3.4 (활성·차단 조건)
  - common §13.3 (차단 UI 일관성)
  - 산업안전 정책서 §3, §4 ([본 prafta 정책서 폴더에는 아직 없음] — 추후 매핑 필요)
- **예상 산출물**: `.vue` 1개
- **선행**: PLNprafta-037002

#### PLNprafta-037007 — TbmAttendCard

- **핵심 요구사항**:
  1. 타이틀 "TBM 참석" + 우상단 chevron.
  2. props.tbmStatus 4종에 따라 콜아웃과 버튼 활성 분기:
     - `'BEFORE_CHECK_IN'`: `tbm-meta` + `tbm-callout.neutral` + lock 아이콘 "출근 후 참석 가능" + 버튼 disabled
     - `'AVAILABLE'`: `tbm-meta` + `tbm-callout.warn` + clock 아이콘 "늦지 않게 참석해 주세요" + 버튼 active
     - `'ATTENDED'`: `tbm-meta` + `tbm-callout.success` + circlecheck 아이콘 "{attendedAt} 참석 완료" + 버튼 disabled
     - `'NONE'`: `tbm-meta.muted` "예정된 TBM이 없습니다" + `tbm-callout.neutral` + caloff 아이콘 "오늘은 TBM 일정이 없어요" + 버튼 disabled
  3. props: `tbmStatus`, `sessionTime`, `sessionLocation`, `sessionLeader`, `attendedAt`.
  4. emits: `click:detail`, `click:attend`.
  5. 동적 카운트다운 금지 (상세 §3.5.4 — 고정 문구 "늦지 않게 참석해 주세요").
- **영향 받는 파일**: `src/views/main/components/TbmAttendCard.vue` (신규)
- **정책서 출처**:
  - 상세 §3.5 (TBM 4 상태)
  - 상세 §10 결정사항 12 (4가지 상태 통일), 13 (카운트다운 제거)
  - common §10 알림 (TBM 참석 알림 — deferred)
- **예상 산출물**: `.vue` 1개
- **선행**: PLNprafta-037002

#### PLNprafta-037008 — NoticeListCard

- **핵심 요구사항**:
  1. 카드 패딩: 상단 16px / 좌우 0 / 하단 8px (`.notice-card`).
  2. 헤더 영역(`.notice-head`): 좌 "공지사항" + 미열람 카운트(`.notice-count`, 0이면 숨김) / 우 chevron(`.head-chev`).
  3. 행(`.notice-row`): 0.5px 상단 보더, 좌우 16px 패딩, 상하 12px 패딩.
  4. 행 구성: (선택) 중요 칩 `.imp` "중요" / 제목 `.title`(읽음=`.read` body·secondary, 미열람=body-strong) / 시간 메타 `.meta` / (선택) 미열람 점 `.unread-dot` (6×6px Danger).
  5. props: `items`(array of `{noticeId, isImportant, title, displayTime, isRead}`), `unreadCount`(number).
  6. emits: `click:more`, `click:row` (with noticeId).
  7. 최대 3행 표시 (props 로 받는 items 가 이미 3개로 잘려서 옴 — 잘라내기는 부모/백엔드 책임).
- **영향 받는 파일**: `src/views/main/components/NoticeListCard.vue` (신규)
- **정책서 출처**:
  - 상세 §3.6 (공지 리스트 정책)
  - 상세 §10 결정사항 11 (카드형 → 리스트형, 최대 3행)
  - common §10 알림 / 공지 정책
- **예상 산출물**: `.vue` 1개
- **선행**: PLNprafta-037002

#### PLNprafta-037009 — HomeTabBar

- **핵심 요구사항**:
  1. 5탭: 홈(활성) / 근태 / 안전 / TBM / 마이. 시안 `.tabbar` DOM 그대로.
  2. props: `activeTab`(`'home'`|`'attd'`|`'safety'`|`'tbm'`|`'my'`), `tbmBadgeCount`(number).
  3. TBM 탭에 미참석 카운트 배지(`.tab-badge`, 0이면 숨김).
  4. emits: `click:tab` (with key).
  5. **본 라운드는 활성 탭만 home, 나머지는 placeholder** — emit 만 발행하고 라우팅은 부모(MainView)에서 `// TODO(developer):` 마커.
- **영향 받는 파일**: `src/views/main/components/HomeTabBar.vue` (신규)
- **정책서 출처**:
  - 상세 §1.3 §8 (하단 탭바 5탭)
  - 상세 §4.1 (탭바 72px, TBM 미참석 카운트 배지)
- **예상 산출물**: `.vue` 1개
- **선행**: PLNprafta-037002

---

## 3. 본 라운드 outside scope (명시적 제외)

| 항목 | 사유 / 처리 방식 |
|---|---|
| 백엔드 API 호출 코드 (axios) | script setup 에 `// TODO(developer):` 마커만 |
| 사용자 유형 분기(관리자/정규직/일용직) | 정규직 시안 그대로 모두 노출. 식별 키 미정 (작업요청서 본문 명시) |
| 출퇴근 확인 모달 (사업장 내 단순 모달 / 사업장 외 지도 모달) | 별도 컴포넌트 분해 필요. 본 라운드 핸들러에 TODO 마커만 |
| 신규 라우트(근태/안전/TBM/마이 탭 진입 화면) | router/index.js 수정 없음. 핸들러 TODO 마커만 |
| 정책 미확정 10건 (7.1~7.10) | 시안 가정값 그대로 |
| 상태바(`.statusbar` 9:41 모형) | OS status bar 가 있으므로 제외 |
| Tailwind 마이그레이션 검토 | 본 화면만 scoped CSS + 변수로 새로 작성. 다른 화면은 영향 없음 |
| `App.vue` 의 sprite mount 위치 | MainView 가 마운트될 때 HomeIcons 가 함께 mount 되므로 별도 작업 불필요. 다른 화면에서 sprite 재사용 시 후속 라운드에서 App.vue 로 이동 검토 |
| 위험성 발굴 도메인의 SAFETY NOTE 정책서 매핑 | 정책서 폴더에 산업안전 정책서가 아직 없음 — deferred |

---

## 4. 우선순위 근거

1. **PLNprafta-037001 (MainView)** P0: 다른 카드 작업이 본 화면에 합쳐지므로 골격이 먼저 있어야 함.
2. **PLNprafta-037002 (HomeIcons sprite)** P0: 모든 카드가 `<use href="#i-..."/>` 패턴을 쓰므로 sprite 가 먼저 mount 되어야 함.
3. **PLNprafta-037003~009 (각 카드/탭바)** P1: 병렬 작업 가능. AttendanceCard 는 법적 책임 영역(attd) 으로 +1단계 격상이지만 본 라운드는 골격만이므로 P1 유지.

---

## 5. 후속 라운드 deferred 리스트

### 5.1 백엔드 신설 API 4종 (상세 §5 그대로)

| API | 메서드 | 경로 | 응답/요청 요약 |
|---|---|---|---|
| 홈 단일 조회 | GET | `/api/app/home/summary` | `{site, user, attendance, leave, approval, safety, tbm, notices}` 한 번에 응답 |
| 출근 등록 | POST | `/api/app/attd/check-in` | `{latitude, longitude, accuracy}` → GPS 지오펜스 판정 + `tb_user_attd_mgmt` INSERT |
| 퇴근 등록 | POST | `/api/app/attd/check-out` | 동일 body, UPDATE 처리 |
| TBM 참석 | POST | `/api/app/tbm/attend` | `{tbmSessionId}` → `tb_tbm_attendance` INSERT |

### 5.2 DB 변경 4종 (상세 §6 그대로)

| # | 항목 | 검토 사항 |
|---|---|---|
| 6.1 | `tb_user_attd_mgmt.IS_OFFSITE` 컬럼 | 사업장 외 출퇴근 구분 BOOLEAN 신설 검토 (현재 컬럼 유무 확인 필요) |
| 6.2 | TBM 세션·대상자·참석이력 테이블 | `tb_tbm_session` / `tb_tbm_target` / `tb_tbm_attendance` — 기존 테이블 확인 후 신설 |
| 6.3 | 공지사항 사용자별 읽음 처리 | `tb_notice_mgmt` / `tb_notice_read` 확인 후 보강 |
| 6.4 | 위험성 발굴 도메인 | 위험성평가(`tb_risk_assessment`)와 별도 도메인인지 통합인지 결정 필요 |

### 5.3 사용자 유형 분기 (관리자/정규직/일용직)

- 식별 키 미정 (JWT claim, USER_AUTH_LEVEL, USER_TYPE 등 후보).
- 각 유형별 화면 케이스가 다름(현재 사용자=정규직 시안만 확보).
- 사용자 결정 후 분기 구현. 본 라운드는 정규직 시안 그대로 모두에게 노출.

### 5.4 정책 미확정 10건 (상세 §7 표 그대로 인용)

| # | 항목 | 시안 가정 | 정책 확인 필요 |
|---|---|---|---|
| 7.1 | 사업장 외 출근 모달 | 단순 확인 모달 | 지도 모달 필요 여부 |
| 7.2 | 사업장 내 퇴근 모달 | 단순 확인 모달 또는 생략 | 모든 퇴근 액션에 모달 필요한지 |
| 7.3 | 2구간 스케줄 표시 | 1구간만 다룸 | 2구간 사용자 표시 방식 |
| 7.4 | GPS 미확인 케이스 | 본 시안에 없음 | GPS 수신 불가·권한 거부 시 카드 표시 (attd §7.3) |
| 7.5 | 퇴근 후 안전 활동 | 케이스 1과 동일 처리 | 퇴근 후 당일 안전점검 재진입 허용 여부 |
| 7.6 | TBM 사후 참석 | 세션 시간 무관하게 참석 가능 | 세션 종료 후 사후 참석 허용, 본인 체크 방식 |
| 7.7 | 인사말 가변 처리 | "오늘도 좋은 하루 되세요" 고정 | 시간대/요일별 가변 |
| 7.8 | 승인 요청 카운트 정의 | `REQ_STATUS = 'REQUESTED'` 본인 등록건 | 반려 후 재요청 대기 포함 여부 |
| 7.9 | 공지 정렬 | 중요 우선 → 최신순 | 정책 명시 여부 |
| 7.10 | 공지 메인 표시 개수 | 최대 3행 | 정책 또는 가이드 명시 여부 |

### 5.5 추가 컴포넌트 분해 (후속 라운드)

- `AttendanceConfirmDialog.vue` — 사업장 내 출근 + 현장 외 퇴근 모달 (상세 §3.2.3, 가이드 §4.9.1 / §5.1.2)
- `MapPickerDialog.vue` — 사업장 외 위치 지도 모달 (지도 SDK 의존성 검토 필요)
- 다른 탭(근태/안전/TBM/마이) 의 진입 화면 — 별도 분해
- 알림 센터 화면 — 별도 분해
- TBM 본인 체크 흐름 (QR/서명/단순 탭) — 정책 7.6 확정 후 분해

---

## 6. 산출 파일 목록 (본 라운드)

| # | 경로 | 신규/교체 |
|---|---|---|
| 1 | `PRAFTA/prafta-app-frontend/prafta-app-frontend/src/views/main/MainView.vue` | 전체 교체 |
| 2 | `PRAFTA/prafta-app-frontend/prafta-app-frontend/src/views/main/components/HomeIcons.vue` | 신규 |
| 3 | `PRAFTA/prafta-app-frontend/prafta-app-frontend/src/views/main/components/HomeHeader.vue` | 신규 |
| 4 | `PRAFTA/prafta-app-frontend/prafta-app-frontend/src/views/main/components/AttendanceCard.vue` | 신규 |
| 5 | `PRAFTA/prafta-app-frontend/prafta-app-frontend/src/views/main/components/AttendanceSummaryCard.vue` | 신규 |
| 6 | `PRAFTA/prafta-app-frontend/prafta-app-frontend/src/views/main/components/SafetyActivityCard.vue` | 신규 |
| 7 | `PRAFTA/prafta-app-frontend/prafta-app-frontend/src/views/main/components/TbmAttendCard.vue` | 신규 |
| 8 | `PRAFTA/prafta-app-frontend/prafta-app-frontend/src/views/main/components/NoticeListCard.vue` | 신규 |
| 9 | `PRAFTA/prafta-app-frontend/prafta-app-frontend/src/views/main/components/HomeTabBar.vue` | 신규 |

---

## 7. 결정 사유 로그

1. **시안 §1.2 경로 오기 처리**: 상세 요청서가 `prafta-web-frontend/src/views/home/HomeView.vue` 라고 적었으나, 짧은 본문 `prafta-037.md` 가 "prafta-app-frontend 요청" 임을 명시했고, 기존 라우터(`/MainView`)가 `src/views/main/MainView.vue` 를 가리키므로 짧은 본문이 우선. 라우터 경로 변경 없이 기존 `MainView.vue` 를 그대로 교체.
2. **시안 케이스를 단일 화면 + reactive state 로 처리**: 작업 지시서 §본 라운드 스코프 #2 명시 준수.
3. **HomeIcons sprite 단일 통합**: 작업 지시서 §본 라운드 스코프 #4 의 "권장: src/components/icons/HomeIcons.vue 같은 단일 SVG sprite 컴포넌트로 묶거나" 옵션을 채택. 위치는 `src/views/main/components/` (홈 전용으로 시작, 후속 라운드에서 전역 컴포넌트로 격상 검토).
4. **상태바 제외**: 작업 지시서 §본 라운드 스코프 #6 명시 준수.
5. **기존 라우트 보존**: `/QrScanner` (안전점검 시작), `/Risk_01` (위험성 발굴) 라우팅은 그대로 유지. SafetyActivityCard 의 emit 받아서 MainView 에서 router.push 호출 (TODO 마커).
6. **Tailwind vs scoped CSS**: 본 화면 한정으로 scoped CSS + 변수 사용. 다른 화면은 영향 없음. CLAUDE.md "화면 작업 시 절대 규칙" 준수.
7. **CSS 변수 선언 위치**: 각 카드별 `<style scoped>` 가 부모의 CSS 변수를 상속받을 수 있도록 MainView 의 `.home-view` selector 에 한 번만 선언. 자식 컴포넌트는 `var(--color-...)` 만 참조.
8. **SVG sprite 의 mount 위치**: MainView template 최상단에 `<HomeIcons />` 로 inline. App.vue 수정 없음. 홈 화면을 벗어나도 sprite 가 살아있을 필요는 없음(다른 화면은 자체 sprite 또는 lucide 사용).
9. **승인요청 KPI 라우팅**: 상세 §3.3.2 "본인 요청 목록 화면으로 이동(별도 화면, 본 작업 범위 외)" — emit 만 발행, 라우팅 TODO 마커.
10. **잔여연차 클릭**: 상세 §3.3.1 "연차 신청 바텀시트(`AttdLeaveForm.vue`)" — 본 라운드 outside scope, emit + TODO 마커.

---

## 8. 본 라운드 종료 후 메인 세션이 할 일 (Notion)

- 메인 세션이 `PLNprafta-037001~009` 9건을 Notion "작업 로그" DB 에 일괄 등록.
- 본 markdown 의 "각 작업 상세" 섹션을 그대로 "상세 설명" 컬럼에 복사.
- 산출 파일 경로(§6)를 "산출물" 컬럼에 기록.
- 상태=분해완료, 담당 에이전트=planner.
- 후속 라운드 deferred(§5) 항목들은 별도 작업 ID 로 채번하지 않음 (정책 확정 후 분해).
