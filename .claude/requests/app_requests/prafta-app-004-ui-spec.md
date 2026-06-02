# prafta-app-004 화면 명세 (TBM 입실/종료)

> planner 작성. Notion "도메인 지식 베이스" 등록은 메인 세션 담당.

## UI-A0xx TbmEntryView (TBM 입실/종료)
- **연결 작업**: APP004-C4 (백엔드 C1/C2/C3 의존)
- **화면 위치**: `prafta-app-frontend/prafta-app-frontend/src/views/tbm/TbmEntryView.vue`
- **라우트**: `/TbmEntry?sessionCd=...` (router/index.js 정적 등록, 보호 — beforeEach 토큰 게이트)
- **참조 패턴**: `MyAttendanceView.vue`(루트 헤더+토큰 1회 선언+본문 분기), `AttendanceActionSheet.vue`(GPS/액션 게이팅), `TbmAttendCard.vue`(콜아웃 톤 클래스)

### 레이아웃 와이어프레임
```
┌──────────────────────────────┐
│ ←  TBM 입실            (헤더)  │
├──────────────────────────────┤
│  세션 카드                     │
│   제목 / 진행자 / 장소         │
│   상태 배지(개설/진행중)        │
├──────────────────────────────┤
│  [GPS 콜아웃]                  │
│   ✓ 근무지 안(12m) / ⚠ 확인중  │
├──────────────────────────────┤
│  입실 단계 (NOT_ENTERED)       │
│   ○ 비밀번호 입력  [______]    │
│   ─── 또는 ───                 │
│   [ QR 스캔으로 입실 ]          │
│   (서명영역: C-D1 확정시)       │
│   [    입실하기   (CTA)   ]     │
├──────────────────────────────┤
│  종료 단계 (ENTERED)           │
│   입실 12:03 · 근무지 안        │
│   ○ 종료 비밀번호  [______]    │
│   (서명영역)                    │
│   [    종료하기   (CTA)   ]     │
└──────────────────────────────┘
```

### 컴포넌트 매핑
| 영역 | 구현 | 비고 |
|---|---|---|
| 헤더(뒤로/제목) | native button+svg(앱 공통 sprite 패턴) | MyAttendanceView 헤더 동일 |
| 세션 정보 카드 | scoped div(.card) | TbmAttendCard .card 패턴 |
| 상태 배지 | scoped span(톤 클래스) | --color-primary-tint 등 |
| GPS 콜아웃 | scoped div(.callout, 톤 분기) | TbmAttendCard .tbm-callout 톤 재사용 |
| 비번 입력 | native input(앱에 공통 input 컴포넌트 부재 — 기존 화면도 native 사용) | inputmode numeric |
| QR 입실 | 라우팅 `/QrScanner` 또는 Flutter qr_scan_page | developer 연결 |
| 서명 영역 | placeholder(C-D1 확정 시 C6에서 캡처) | 조건부 |
| CTA(입실/종료) | native button(.cta) | --color-primary |

### 상태별 동작
- **loading**: 컨텍스트 조회 중 — 스켈레톤/스피너(.loading).
- **NOT_ENTERED**: 입실 단계 노출(비번/QR/GPS/서명/입실 CTA).
- **GPS 분기**: AUTO=거리 콜아웃(안/밖), MANUAL=확인안내, DISABLED=콜아웃 숨김(C-D5).
- **ENTERED**: 입실 정보 + 종료 단계.
- **COMPLETED**: "이수 완료" 안내, 액션 비활성.
- **error**: 비번 불일치/세션 마감/거리 초과 — 인라인 에러 메시지(alertUtil 가능).
- **empty/unavailable**: 입실 불가 세션(DRAFT/CANCELLED/COMPLETED) — 안내 + 입실 CTA 비활성.

### 사용자 플로우
진입(메인 카드 참석하기 → /TbmEntry) → 컨텍스트 조회 → GPS 요청(requestGps) → 비번 입력 or QR 스캔 → 입실하기 → (서명?) → 입실 완료 → (교육) → 종료 비번/QR → 종료하기 → 이수 완료.

### 백엔드 의존
- GET `/appApi/tbm/entry-context` (C3)
- POST `/appApi/tbm/enter` (C1)
- POST `/appApi/tbm/exit` (C2)

### 미확정 (UI 영향)
- 서명 영역 노출(C-D1), GPS 좌표 노출 방식(C-D5), 일용직 화면(C-D2 — 본 화면 정규직 전용).
