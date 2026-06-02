# UI 명세 — UI-A005 MyLeaveSummaryView (연차 현황 / 모바일 앱)

- **화면 ID**: UI-A005
- **연결 작업**: prafta-app-005 슬롯 B(컨테이너) + C~G(컴포넌트), 데이터 슬롯 A(backend)
- **영역**: app (Flutter webview 내부 Vue, `prafta-app-frontend`)
- **모듈**: leave
- **화면 위치**: `src/views/leave/MyLeaveSummaryView.vue` (+ `src/views/leave/components/*.vue`)
- **시안**: `refs/prafta-app-005/prafta_my_leave_v1.html` (4 케이스)
- **참조 패턴**:
  - 컨테이너/헤더/세그먼트/탭바: `views/attd/MyAttendanceView.vue` (헤더+세그먼트+본문+sprite 구조)
  - 카드/배지/버튼 토큰: `views/attd/components/AttendanceTodayCard.vue`
  - KPI 표기: `views/main/components/AttendanceSummaryCard.vue` (trimDays 표기 규칙)
  - 디자인 토큰: `.my-attd-view` 루트 선언 세트를 `.my-leave-view`에 동일 선언 → 자식 scoped 상속

## 데이터 출처
- 단일 호출: `GET /appApi/leave01/my-leave-summary` (slot A). 진입 1회, 캐시 없음(§3.6).
- 응답: `{ user, groups{TOTAL,STATUTORY,NON_STATUTORY}, expiringSoon }` (plan §1-2).
- 그룹 토글 전환은 클라 상태만 변경(추가 API 없음).

## 레이아웃 와이어프레임

```
┌──────────────────────────────────────┐
│  ←        연차 현황            (빈)   │  헤더 56px (back / title / spacer)
├──────────────────────────────────────┤
│  ┌──────────────────────────────┐    │
│  │ [전체] [ 법정 ] [ 법정 외 ]  │    │  그룹 토글(세그먼트) C
│  └──────────────────────────────┘    │
│                                        │
│  ⚠ 3일 후 소멸되는 연차 5일      [×] │  소멸임박 콜아웃 D (조건부, 전체 토글만)
│                                        │
│  ┌──────────────────────────────┐    │
│  │ 잔여 일수                     │    │  메인 잔여 카드 E
│  │ 12 일      / 20일             │    │   - 큰 숫자(잔여) + 단위 + /부여
│  │ ▓▓▓░░░░░░░░░░░░░░░░░░░         │    │   - 진행바(사용/예정/잔여 3분할)
│  │ ● 사용 6일  ◌ 예정 2일  ○ 잔여12일│  │   - 범례 3항목
│  └──────────────────────────────┘    │
│  ┌──────────────────────────────┐    │
│  │  부여   │  사용   │ 사용예정  │    │  3분할 KPI F
│  │  20일   │  6일    │  2일      │    │
│  └──────────────────────────────┘    │
│  ┌──────────────────────────────┐    │
│  │ 입사일            2024-09-18  │    │  메타 카드 G
│  │  └ 경력 인정 N개월(조건부)    │    │   - 경력인정 0이면 숨김
│  │ 근속              1년 8개월   │    │
│  │ 사용률            40%         │    │
│  └──────────────────────────────┘    │
├──────────────────────────────────────┤
│  [ + 연차 신청하기 ]                  │  푸터 (잔여>0 활성 / ==0 비활성)
└──────────────────────────────────────┘
```

## 컴포넌트 매핑

| 영역 | 컴포넌트 | props | emit |
|---|---|---|---|
| 그룹 토글 | `LeaveGroupToggle.vue` (C) | `modelValue` (TOTAL/STATUTORY/NON_STATUTORY) | `update:modelValue` |
| 소멸임박 콜아웃 | `LeaveExpiryCallout.vue` (D) | `info`(expiringSoon) | `close` |
| 메인 잔여 카드 | `LeaveBalanceCard.vue` (E) | `label`(string), `group`(granted/used/planned/remaining) | — |
| 3분할 KPI | `LeaveSplitKpi.vue` (F) | `group` | — |
| 메타 카드 | `LeaveMetaCard.vue` (G) | `user`(hireDate/serviceMonths/serviceCreditMonths), `usageRate` | — |
| 컨테이너/헤더/푸터 | `MyLeaveSummaryView.vue` (B) | — | — |

- native `<input>`/`<button>` 직접 사용: 헤더 back·푸터 액션·토글 항목·콜아웃 닫기는 `<button>` 사용
  (app FE에 범용 BaimButton류 공통 컴포넌트 부재 — 기존 화면도 native button + scoped class 사용).
  공통 컴포넌트 신설은 본 작업 범위 외(별도 작업).

## 상태별 동작

| 상태 | UI |
|---|---|
| loading | 본문 "불러오는 중..." (MainView `home-loading` 패턴). 카드 미렌더 |
| empty(부여 전무, 신규입사) | 모든 수치 0, 진행바 회색 단일(`bar-zero`), "부여된 연차가 없습니다", 푸터 비활성 |
| error(네트워크) | $alert 폴백 안내, 본문 빈 상태. 401/403/500은 axios 인터셉터 처리 |
| success(정상 잔여) | 메인 숫자 primary green, 푸터 활성 |
| success(잔여 0) | 메인 숫자 text-primary(검정), 푸터 disabled (도움말 없음) |
| 소멸 임박 | "전체" 토글 + expiringSoon.exists=true 시 콜아웃 노출(닫기 가능, 세션한정) |

- 그룹 토글 전환: 메인카드 라벨("잔여 일수"/"법정 잔여 일수"/"법정 외 잔여 일수") + 모든 수치 갱신.
  콜아웃은 activeGroup==='TOTAL'일 때만 렌더(§3.4). 메타카드 입사/근속 고정, 사용률은 Q5 확정에 따름.

## 사용자 플로우
1. 메인 홈 "근태 조회" 카드 → "잔여 연차" KPI 탭 → 본 화면 진입(라우팅 developer).
2. 진입 시 GET 1회 → 기본 토글 "전체" 표시.
3. (선택) 토글 전환 → 클라 상태만 변경, 즉시 수치 갱신.
4. (조건) 소멸 임박 콜아웃 노출 → [×] 닫기(세션 한정, 재진입 시 재노출).
5. 푸터 [연차 신청하기] → (잔여>0) 연차 신청 폼(미구현 → "준비 중" 폴백, TODO) / (잔여0) 비활성.

## 반응형
- 모바일 first(360~414px). break point 분기 불필요(앱 webview 단일 폭). 기존 화면과 동일하게 미디어쿼리 없음.

## 백엔드 의존
- `GET /appApi/leave01/my-leave-summary` (slot A, plan §1).

## 접근성
- 진행바: 색 + 범례 텍스트 보조(색만으로 정보 전달 금지).
- 숫자 `font-variant-numeric: tabular-nums`(루트 선언).
- 콜아웃 닫기 `aria-label="닫기"`. 토글 `role="tablist"`/`role="tab"`/`aria-selected`(MyAttendanceView 패턴).
