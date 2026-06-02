# prafta-app-002 UI 명세 — 내 근태 조회 (모바일 앱)

> planner 작성. Notion "도메인 지식 베이스" 등록은 메인 세션 담당.
> 시안: `refs/prafta-app-002/prafta_my_attendance_v8.html` (10 케이스)
> 대상: `PRAFTA/prafta-app-frontend/prafta-app-frontend/src/views/attd/`
> 디자인 토큰: `MainView.vue`가 `.home-view`에 선언한 토큰 세트를 본 화면 루트 `.my-attd-view`에 동일 선언(시안 :root 토큰 1:1). 하드코딩 금지.

---

## UI-A001 MyAttendanceView (컨테이너)
- 연결 작업: APP002-05
- 화면 위치: `src/views/attd/MyAttendanceView.vue`
- 참조 패턴: `views/main/MainView.vue`(토큰 루트+조립+dev case picker), `HomeTabBar.vue`
- 레이아웃 와이어프레임:
```
┌──────────────────────────────┐
│ [<] 내 근태            [🔔2] │  헤더 56px
├──────────────────────────────┤
│  [ 오늘 │ 이번주 │ 이번달 ]  │  세그먼트 12px margin
├──────────────────────────────┤
│                              │
│   <활성 탭 본문 슬롯>        │  스크롤 영역
│   오늘  → AttendanceTodayCard│
│   이번주→ AttendanceWeekList │
│   이번달→ AttendanceMonthCalendar + AttendanceDayDetailCard
│                              │
├──────────────────────────────┤
│ 홈 │ [근태] │ 안전 │ TBM     │  탭바 72px (근태 활성)
└──────────────────────────────┘
   (이번주 카드 탭 시 AttendanceActionSheet 오버레이)
```
- 컴포넌트 매핑:

| 영역 | 컴포넌트 | 비고 |
| --- | --- | --- |
| 헤더 | (인라인) `.attd-hd` | 시안 `.hd` 구조. 뒤로/타이틀/알림아이콘+배지 |
| 세그먼트 | (인라인) `.attd-seg` | 시안 `.sg`. activeTab='today'/'week'/'month' |
| 오늘 본문 | AttendanceTodayCard | UI-A002 |
| 이번주 본문 | AttendanceWeekList | UI-A003 |
| 이번달 본문 | AttendanceMonthCalendar + AttendanceDayDetailCard | UI-A004 + UI-A005 |
| 바텀시트 | AttendanceActionSheet | UI-A006 |
| 탭바 | (인라인 또는 HomeTabBar 재사용) | activeTab='attd' |

- 상태별 동작:
  - loading: 본문 영역 스켈레톤/로더(loadingStore 전역 오버레이 사용 — axios 인터셉터가 처리).
  - empty: 스케줄/근태 없음 → 카드 대신 "표시할 근태가 없어요" 안내(시안에 케이스 없음 → developer가 표준 빈상태 적용).
  - error: 네트워크/401/403/500 → axios 인터셉터 표준 처리(useAuth/forceLogout). 화면 자체 에러배너 없음.
  - success: 탭별 데이터 렌더.
- 사용자 플로우: MainView "근태 조회" 또는 하단탭 '근태' → 진입(기본 '오늘') → 세그먼트 전환(주/월) → 카드/셀 탭 → 상세/바텀시트.
- 백엔드 의존: 오늘=GET /api/app/attd/my/today (APP002-01), 주=.../week(02), 월=.../month(03), 일상세=.../day-detail(04).
- 캐시: 조회한 주/월은 재호출 최소화(시안 §6.2) — developer가 Map 캐시 구현.

---

## UI-A002 AttendanceTodayCard (오늘 카드 / 일 상세 공용 본체)
- 연결 작업: APP002-06 (09와 본체 공용)
- 화면 위치: `src/views/attd/components/AttendanceTodayCard.vue`
- 참조 패턴: `AttendanceCard.vue`(badge/btn/HHMM), 시안 `.cd/.dr/.sl/.tr/.tw/.al/.ft`
- 5 변형(시안 화면 1~5):

| 변형 | workStatus | 근태행 클래스 | 상태배지 | 인라인알림 | 푸터 |
| --- | --- | --- | --- | --- | --- |
| 근무중 | WORKING | `.tw.a` | bd-p "근무중" | in "근무 중에는 근태 수정을 요청할 수 없어요." | [수정요청 비활성] [퇴근하기 활성] |
| 퇴근완료 | CHECKED_OUT | `.tw.a` | bd-n "퇴근" | in "출퇴근 기록과 근태가 다르면 수정 요청해 주세요." | [수정요청 활성] [퇴근하기 비활성] |
| 사업장다름 | CHECKED_OUT | `.tw.a.wr` | bd-n "퇴근" | 없음 | [수정요청 활성] [퇴근하기 비활성] |
| 2구간 | TWO_SLOT_WORKING | 구간별 분리(`.dv` 구분선) | bd-i "2구간 근무" | in "2구간 근무까지 모두 끝난 뒤에..." | [수정요청 비활성] [2구간 출근 활성] |
| 퇴근미등록 | CHECK_OUT_MISSING | `.tw.a.m` (danger) | bd-w "퇴근 미등록" | danger(`.al`) "퇴근은 오늘 안에만 가능해요." | [수정요청 비활성] [퇴근하기 활성] |

- 3행 정보 구조(각 구간): 스케줄(`.tw` calendar-event) / 근태(`.tw.a` fingerprint) / 표준화(`.tw.st`, 산정불가 시 `.tw.st.x` + "-").
- 색상 코딩:
  - 근태 정상 = `.tw.a`(옅은 그린), 사업장다름 = `.tw.a.wr`(앰버/warning, 퇴근지 텍스트 `.pw`), 미등록 = `.tw.a.m`(danger, 퇴근시각 `.dg` "미등록").
  - 표준화 산정 불가 = `.tw.st.x` + "-"만(부가텍스트 금지, 시안 §3.6).
- GPS 표시(시안 §3.5): 정상→"출근 {사업장명}", 미확인→"GPS 확인필요". 사업장다름→퇴근지 `.pw`(warning).
- 상태별: 모든 활성/비활성은 props(actions.*)로 서버 산출값 수신, 컴포넌트는 표시만.
- 백엔드 의존: APP002-01 / 04.

---

## UI-A003 AttendanceWeekList (이번주)
- 연결 작업: APP002-07
- 화면 위치: `src/views/attd/components/AttendanceWeekList.vue`
- 참조 패턴: 시안 `.cd(주네비)/.dc/.dp/.db/.dt2/.ds/.ws`
- 레이아웃:
```
[ < ]  2026.05.18 ~ 05.24  [ > ]    주 네비 카드
─ 월 18  ST001 정규근무  스케줄 0930~1800 · 근태 0925~1808  [>]
─ 화 19  ST001 정규근무  스케줄 0930~1800 · 근태 0942~1802 (지각)  [>]  ← 근태 warning(.wr)
─ 수 20  [오늘] ST001    스케줄 0930~1800 · 근태 0928~근무중  [>]
─ 목 21  [2구간] ST003 교대  스케줄 0700~1300 / 1700~2100  [>]
─ 금 22  ST002 조기근무  스케줄 0700~1500  [>]   ← 미래(근태 미표시)
─ 토 23  휴무  [>]                                ← .ds 없이 "휴무" 텍스트만
─ 일 24  [연차][부처님오신날]  [>]               ← .dc.lv, 배지 2개
┌ 이번주 합계  (완료된 근무만) ─────────────┐
│ 예정 근로시간 42h 00m │ 실 근로시간 14h 03m │ ← 실=primary색
└──────────────────────────────────────────┘
```
- 요일 색: 토=info(`.dw.sa`), 일=danger(`.dw.su`).
- 근태 요약 색: 정상=`.ac`(primary), 지각/이상=`.wr`(warning).
- 미래: 근태 부분 미표시("예정" 단어 금지, 시안 §4.3.2).
- 휴무: 시간요약 없이 "휴무"만(칩/배지 금지). 휴무+공휴일="휴무 · 부처님오신날". 연차+공휴일=`연차`+`부처님오신날` 배지 둘 다.
- 카드 탭: 어디든 탭 → emit('select-day', {day}) → 부모가 ActionSheet 오픈.
- 합계: summary.plannedWorkMinutes/actualWorkMinutes를 "Nh Nm" 포맷. note "완료된 근무만".
- 백엔드 의존: APP002-02.

---

## UI-A004 AttendanceMonthCalendar (이번달 캘린더)
- 연결 작업: APP002-08
- 화면 위치: `src/views/attd/components/AttendanceMonthCalendar.vue`
- 참조 패턴: 시안 `.cd/.mn/.mn-sum/.lgd/.cal/.cal-h/.cal-d/.cal-mk/.cal-alert`
- 레이아웃:
```
[ < ]  2026년 5월  [ > ]
예정 163h 30m · 실 72h 45m (완료분)
범례: ■근무 ■연차 ■휴무 ■처리 필요
일 월 화 수 목 금 토       ← 일=danger, 토=info 헤더색
[7열 × 6주 셀 그리드]
```
- 셀 색상 코딩(시안 §2.3·§4.4.2):

| 셀 클래스 | 의미 | 배경 | dayType |
| --- | --- | --- | --- |
| `.cal-d.wk` | 근무일 | 옅은 그린 + 그린 마커점 | WORK |
| `.cal-d.lv` | 연차일 | 옅은 앰버 | LEAVE |
| `.cal-d.of` | 휴무일 | 옅은 회색 | OFF |
| `.cal-d.ac` | 처리 필요 | 옅은 붉은색 + 우상단 경고점(.cal-alert) | ACTION_REQUIRED(hasIssue) |
| `.cal-d.out` | 이전/다음달 | 투명, 흐린 텍스트 | (인접월) |
| `.cal-d.td` | 오늘 | outline 강조(다른 색과 중첩 가능) | |
| `.cal-d.sel` | 선택 | 진한배경+흰글씨(.sel.lv 앰버 / .sel.of 회색 / .sel.ac danger) | |

- 접근성: 처리필요는 색+경고점+(상세카드 텍스트) 3중(시안 §6.3).
- 셀 탭: emit('select-date', {ymd}) → 부모가 day-detail 조회 → DayDetailCard 갱신.
- 월 네비: emit('prev-month')/('next-month').
- 백엔드 의존: APP002-03(셀 색상), 선택 시 APP002-04(상세).

---

## UI-A005 AttendanceDayDetailCard (선택일 상세 — 오늘/이번달 공용)
- 연결 작업: APP002-09
- 화면 위치: `src/views/attd/components/AttendanceDayDetailCard.vue`
- 참조 패턴: UI-A002와 본체 공용(시안 화면 9·10 하단 카드 = 화면 1~5 상단 카드). 06을 그대로 임베드 가능하도록 설계.
- 차이점(오늘 카드 대비):
  - 푸터 액션 배치가 다름: 처리 필요 일자 선택 시 하단 빠른 액션 2버튼([근태 보정] `.bt-s` + [초과근무 신청] `.bt-x` 비활성), `transparent` 배경·카드 아래 inline(시안 화면10 `.ft` border:none).
  - 일반 완료일: 푸터 없음(상세 표시만) 또는 상황별.
- 상태별: 처리필요(ACTION_REQUIRED) → 빠른 액션 노출. 그 외 → 액션 없음(시안).
- 백엔드 의존: APP002-04.

---

## UI-A006 AttendanceActionSheet (이번주 카드 탭 바텀시트)
- 연결 작업: APP002-10
- 화면 위치: `src/views/attd/components/AttendanceActionSheet.vue`
- 참조 패턴: 시안 `.bs(오버레이)/.sh/.shh(핸들)/.shr/.sht/.shc/.shm/.sa/.sai/.sab/.sat`. 모달 마운트는 `components/modal/` 패턴 참조.
- 레이아웃:
```
        (반투명 오버레이 .bs)
┌──────────────────────────────┐
│           ──── (핸들)        │
│ 5월 19일 (화)            [X] │
│ ST001 정규근무 · 0930~1800 · 근태 0942~1802
│ ┌─[📅] 스케줄 수정 요청   [>]│  ← 활성/비활성
│ ┌─[✏️] 근태 보정 요청     [>]│
│ ┌─[⏱] 초과근무 신청       [>]│
│ └─[☂] 연차 신청           [>]│
└──────────────────────────────┘
```
- 액션 4종 항상 노출(안내문구 없음, 시안 §3.3). 비활성=`.sa.x`(회색+opacity, chevron 숨김).
- 활성/비활성 매트릭스(서버 산출 actions.* 수신):

| 일자 상태 | 스케줄수정 | 근태보정 | 초과근무 | 연차 |
| --- | :-: | :-: | :-: | :-: |
| 과거(완료) | ✗ | ✓ | ✓ | ✗ |
| 과거(누락) | ✗ | ✓ | ✗(보정후) | ✗ |
| 과거(마감후) | ✗ | ✗ | ✗ | ✗ |
| 오늘(근무중) | ✗ | ✗ | ✗ | ✗ |
| 오늘(완료) | ✗ | ✓ | ✓ | ✗ |
| 미래(마감전) | ✓ | ✗ | ✗ | ✓ |
| 미래(마감후) | ✗ | ✗ | ✗ | ✓ |
| 미래 휴무일 | ✓ | ✗ | ✗ | ✗ |

> 초과근무 활성도 = "근태 마감 전까지"(재기획서 §3.2). D+5 폐기.
> 휴무일 연차/미래 연차 활성도 = 정책 미확정(plan §5) — 서버 산출값을 그대로 표시.

- 인터랙션: 핸들 드래그/오버레이 탭/X → close emit. 활성 액션 탭 → emit('action', {type}) (라우팅/폼은 developer).
- 상태별: 열림/닫힘 토글(부모가 v-model로 제어). 진입 day 메타는 props.
- 백엔드 의존: 데이터는 APP002-02 days[].actions. 실제 신청 폼은 후속 작업(본 범위 외).
