# PRAFTA-017-2 연차 현황 — UI 스펙 (planner 산출)

> 작성: planner / 검증 상태: Claude 분석 (사용자 검토 후 YJ 확정)
> 출처 정책서: `.claude/context/policies/attd/08-leave.md` §8.5 (법정 연차 부여 정책 / GRANT_TYPE prefix 분류 / §8.5.7 권한 / §8.5.8 멱등성)
> 요청서: `.claude/requests/prafta-017-2.md`, 참고(맹신 금지) CCI §6/§7/§8
> 실측 스키마(우선): `tb_user_leave_grant`, `tb_user`, `tb_user_service_credit`, `tb_site_node`, `tb_leave_type_mgmt`, `tb_leave_policy`

---

## 0. 화면 구성 (3종)

| ID | 이름 | 종류 | 위치 | 진입 |
| --- | --- | --- | --- | --- |
| UI-A | 연차현황 대시보드 (Baim_08) | LNB routed view | `src/views/baim/Baim_08.vue` | LNB 메뉴 |
| UI-B | 직원별 연차 상세 | 모달 (useModal) | `src/views/baim/popup/LeaveDetailPop.vue` | 대시보드 행 [>] 클릭 |
| UI-C | 연차 수동 부여 | 모달 (useModal, 중첩) | `src/views/baim/popup/ManualGrantPop.vue` | 대시보드 [일괄 수동 부여] / 상세 모달 [수동 부여] |

참조 패턴:
- 대시보드: `Baim_06`(ViewHeader + viewSearch + viewBody 테이블) + `LeavePolicyImpactPop`(테이블/요약카드/근사치 안내/CSV) 혼합.
- 상세/수동부여 모달: `LeavePolicyImpactPop`(풀스크린 wide 모달 골격, `@import modal-popup-guide.css`, `.prafta-modal-popup` overlay, `emit('close')`).
- 모달 중첩: `useModal()`은 컴포넌트 인스턴스마다 독립 closure(별도 app/container) → `LeaveDetailPop`이 자신의 `useModal()`로 `ManualGrantPop`을 열면 상세 모달은 그대로 유지된 채 그 위에 중첩된다. (단, 한 인스턴스가 같은 컴포넌트를 두 번 `open`하면 이전 것이 닫히는 점만 주의)

CSS: 신규 화면은 `tokens.css` 변수만 사용. HTML 시안의 `:root` 색상은 토큰으로 치환한다.

### HTML 시안 색상 → tokens.css 매핑표 (하드코딩 금지 변환 기준)

| 시안 변수 | 시안 값 | tokens.css 치환 |
| --- | --- | --- |
| `--color-primary` | #10B981 | `var(--color-primary)` (#16a34a) |
| `--color-primary-hover` | #059669 | `var(--color-primary-hover)` |
| `--bg-page`/`--bg-secondary` | #F5F6F7/#F3F4F6 | `var(--color-bg)` |
| `--bg-card`/`--bg-tertiary` | #FFF/#FAFBFC | `var(--color-surface)` / `var(--card-bg)` |
| `--border-default`/`--border-strong` | #E5E7EB/#D1D5DB | `var(--color-border)` / `var(--color-border-strong)` |
| `--text-primary`/`--text-secondary`/`--text-tertiary` | #1F2937/#6B7280/#9CA3AF | `var(--color-text-strong)` / `var(--color-text)` / `var(--color-text-muted)` |
| `--text-warning`/`--bg-warning` | #B45309/#FEF3C7 | `var(--color-warning-text)` / `var(--color-warning-bg)` |
| `--text-info`/`--bg-info` (법정외 강조) | #1D4ED8/#EFF6FF | `var(--color-primary-pressed)` + `rgba(22,163,74,0.06)` (info 토큰 없음 — primary 계열 사용; 법정/법정외 시각 구분은 배지 텍스트로) |
| `--text-danger` | #DC2626 | `var(--color-danger)` |
| `--radius-sm/md/lg` | 6/8/12px | `var(--btn-radius)` / `var(--input-radius)` / `var(--card-radius)` |

> 주의: tokens.css에는 별도 info(파랑) 토큰이 없다. 시안의 "법정 외" 파랑 강조는 정보 색이 아니라 분류 구분용이므로, 본 화면에서는 법정/법정외를 **배지 텍스트(법정 / 법정 외)** 와 2단 헤더 라벨로 구분하고 색상은 primary 계열(그린)·muted로 통일한다. 별도 파랑 토큰 신설은 하지 않는다(가드레일 4: 토큰만 사용). 진척 막대(progress)도 사용률 구간별로 `--color-primary` / `--color-warning-*` / `--color-danger`만 사용.

---

## UI-A. 연차현황 대시보드 (Baim_08.vue)

### A-1. 레이아웃 와이어프레임

```
┌ viewComm ─────────────────────────────────────────────────────────────┐
│ ViewHeader  연차 현황                                  [조회] [엑셀]    │  ← localButtons search=Y, excel=Y
├ viewBody .leave-dashboard ────────────────────────────────────────────┤
│ ┌ 메트릭 4카드 (grid 4) ───────────────────────────────────────────┐  │
│ │ 전체 직원 N명 │ 평균 사용률 N% │ 소멸임박(30일) N명 │ 이번달 신규부여 N│ │
│ └──────────────────────────────────────────────────────────────────┘  │
│ ┌ 필터바 ────────────────────────────────────────────────────────┐    │
│ │ [부서 ▾] [고용형태 ▾] [정렬 ▾] [직원명 검색.........]            │    │
│ └────────────────────────────────────────────────────────────────┘    │
│ ┌ 일괄 액션바 (선택 1명 이상일 때만 표시) ───────────────────────┐    │
│ │ N명 선택됨    [일괄 수동 부여] [연차사용계획서 조회] [선택 해제] │    │
│ └────────────────────────────────────────────────────────────────┘    │
│ ┌ 직원 테이블 (2단 헤더) ────────────────────────────────────────┐    │
│ │ [☐] 직원 │입사일│근속│  법정 휴가  │ 법정 휴가 외 │사용률│관리  │    │
│ │          │      │    │부여 사용 잔여│부여 사용 잔여│      │ [>]  │    │
│ └────────────────────────────────────────────────────────────────┘    │
│  전체 N명 중 a-b건                            [◀] 1 2 3 .. [▶]         │
└────────────────────────────────────────────────────────────────────────┘
```

### A-2. 컴포넌트 매핑

| 영역 | 컴포넌트 / 태그 | 비고 |
| --- | --- | --- |
| 헤더 | `ViewHeader` (`@search`, `@excel`) | localButtons: search=Y, save=N, create=N, delete=N, excel=Y |
| 부서/고용형태/정렬 셀렉트 | `BaseSelect` (v-model) | 옵션은 developer가 baseinfo/상수 연결 (부서는 site-node, 고용형태는 SYS041) |
| 직원명 검색 | native `<input type="text">` | 폼 input(공통 input 컴포넌트 부재 — Baim_06도 native 사용, 허용) |
| 행 체크박스 / 헤더 전체선택 | native `<input type="checkbox">` | 시안과 동일, 디자인시스템에 checkbox 컴포넌트 없음 |
| 사용률 progress | div.progress-bar > div.progress-fill | 색상 토큰만 |
| 관리 [>] | inline SVG 버튼 | 클릭 → `fnOpenDetail(row)` |
| 일괄/상세 → 수동부여 | `ManualGrantPop` (openPop) | |
| 상세 | `LeaveDetailPop` (openPop) | |
| alert/confirm | `proxy.$alert` / `proxy.$confirm` | |

### A-3. 상태별 동작

| 상태 | UI |
| --- | --- |
| loading | `isLoading=true` 동안 조회 버튼 비활성(또는 기존 패턴대로 무처리). 테이블 영역에 단순 텍스트 "조회 중..." 또는 기존 빈 상태 유지 |
| empty (직원 0건) | 테이블 tbody에 `<tr><td colspan>조회된 직원이 없습니다.</td></tr>` |
| error | `resolveApiErrorMessage` → `proxy.$alert` |
| success | 메트릭 카드 + 테이블 + 페이징 갱신 |
| 선택 0명 | 일괄 액션바 미표시 (`v-if="selectedUserCds.length > 0"`) |

### A-4. 사용자 플로우

1. 진입(LNB) → `onMounted` → `fnSearch()` 자동 조회 (CMPNY_CD 스코프, 1페이지).
2. 필터/정렬/검색어 변경 → [조회] 클릭 → `fnSearch()` 재호출(page=1 리셋).
3. 행 체크 → `selectedUserCds`에 누적 → 1명 이상 시 일괄 액션바 노출.
4. [일괄 수동 부여] → `ManualGrantPop` 열기(대상 N명) → 부여 성공 시 `onGranted` → 대시보드 재조회 + 선택 해제.
5. [연차사용계획서 조회] → **미확정 기능(요청서)**: 버튼만, 클릭 시 `proxy.$alert("준비 중인 기능입니다.")` 안내(D-3).
6. 행 [>] → `LeaveDetailPop` 열기(userCd 전달).
7. [엑셀] → 화면 데이터 클라이언트 CSV 다운로드(LeavePolicyImpactPop fnDownloadReport 패턴 재사용). developer 구현.
8. 페이지네이션 → `fnSearch(page)`.

### A-5. 백엔드 의존 (PRAFTA-017-2-1 backend)

```
GET /webApi/baim08/leave-dashboard/list
    query: deptCd, employmentType, searchKeyword, sortBy, page, size
    resp(예시): {
      metrics: { totalEmployees, avgUsageRate, expiringSoon30, newGrantThisMonth },
      list: [ {
        userCd, userNm, deptNm, hireDate(YYYYMMDD), employmentType,
        tenureText(예 "8년 2개월"), creditMonths,
        legal:    { granted, used, remaining },
        nonLegal: { granted, used, remaining },
        usageRate(0~100)
      } ],
      paging: { page, size, totalCount }
    }
```

### A-6. 검증 / 정렬 옵션

- 정렬(sortBy): `REMAIN_ASC`(잔여 적은순, 기본) / `REMAIN_DESC` / `USAGE_ASC`(사용률 낮은순) / `HIRE_ASC`(입사일순). 백엔드 ${} 화이트리스트 강제(가드레일/CLAUDE.md SQL 규칙).
- 고용형태(employmentType): REGULAR/CONTRACT/DAILY/EXECUTIVE(SYS041) 또는 전체.

### A-7. 반응형

- 데스크탑 우선. `@media (max-width: 1024px)` 에서 메트릭 4→2열, 필터바 wrap. 테이블은 `overflow-x:auto` 래퍼로 가로 스크롤.

---

## UI-B. 직원별 연차 상세 (LeaveDetailPop.vue)

### B-1. 레이아웃 와이어프레임 (풀스크린 wide 모달)

```
┌ .prafta-modal-popup .modal-content-wide (max 1080px) ───────────────────┐
│ modal-header   직원 연차 상세                              [X]          │
├ modal-body ─────────────────────────────────────────────────────────────┤
│ ‹ 연차 현황으로 돌아가기 (= 모달 닫기)                                   │
│ ┌ header-strip ─────────────────────────────────────────────────────┐  │
│ │ (아바타) 김도현                          [수동 부여] [새로고침]    │  │
│ │          생산팀 · 정규직                                            │  │  ← 직급 없음(스키마 미보유)
│ └────────────────────────────────────────────────────────────────────┘  │
│ ┌ info-strip (grid; 직급 컬럼 제외) ───────────────────────────────┐    │
│ │ 입사일 │ 근속(실제) │ 부여 정책 │ 다음 부여 예정일               │    │
│ └────────────────────────────────────────────────────────────────────┘  │
│ [법정 휴가]   부여 / 사용 / 잔여  (stat-card x3)                         │
│ [법정 휴가 외] 부여 / 사용 / 잔여 (stat-card x3)                         │
│ 부여 이력                                                                │
│ ┌ table ────────────────────────────────────────────────────────────┐  │
│ │ 부여일 │ 구분(법정/법정외 배지) │ 사유 │ 부여 │ 사용 │ 잔여 │ 만료일 │ 상태 │ │
│ └────────────────────────────────────────────────────────────────────┘  │
├ modal-footer ──────────────────────────────────────────────────────────┤
│                                                              [닫기]      │
└──────────────────────────────────────────────────────────────────────────┘
```

### B-2. 컴포넌트 매핑

| 영역 | 컴포넌트 | 비고 |
| --- | --- | --- |
| 모달 셸 | `.prafta-modal-popup` overlay + `.modal-content-wide` | LeavePolicyImpactPop 패턴 |
| [수동 부여] | 버튼 → `openPop(ManualGrantPop, { targetUsers:[현재 직원], onGranted })` | 중첩 모달 |
| [새로고침] | 버튼 → `fnReload()` 상세 재조회 (요청서: 기존 [이력 상세]→[새로고침] 명칭 변경 + 데이터 재조회) |
| 구분/상태 배지 | span.grant-tag / span.status-badge | 법정=primary톤, 법정외=muted톤, 상태별 |
| 닫기 | `emit('close')` | |

### B-3. 상태별 동작

| 상태 | UI |
| --- | --- |
| loading | 모달 본문 "불러오는 중..." (간단 텍스트) |
| empty (이력 0건) | 부여 이력 테이블 `<tr><td colspan>부여 이력이 없습니다.</td></tr>` |
| error | `proxy.$alert(resolveApiErrorMessage(...))` |
| success | info-strip + 2섹션 통계카드 + 이력 테이블 |

### B-4. 상태/구분 매핑

- 구분(natureBadge): GRANT_TYPE prefix → `STATUTORY_%`=「법정」, `MANUAL_%`=「법정 외」.
- 상태(status): ACTIVE=사용중 / EXHAUSTED=소진완료 / EXPIRED=만료 / CANCELED=취소됨.
- 부여 이력 정렬: GRANT_DATE 내림차순(백엔드).

### B-5. 백엔드 의존 (PRAFTA-017-2-1 backend)

```
GET /webApi/baim08/leave-dashboard/{userCd}/detail
    resp(예시): {
      user: { userCd, userNm, deptNm, employmentType, hireDate, grantPolicyText, nextGrantDateText },
      legalSummary:    { granted, used, remaining, expiresAt(가장 임박 ACTIVE 만료일, 가능 시) },
      nonLegalSummary: { granted, used, remaining },
      grantHistory: [ {
        grantDate(YYYYMMDD), natureBadge('LEGAL'|'NON_LEGAL'),
        reason, granted, used, remaining,
        expiresAt(=AVAIL_TO_DATE), status('ACTIVE'|'EXHAUSTED'|'EXPIRED'|'CANCELED')
      } ]
    }
```

> 근사 한계(D-4): TB_USER에 직급 컬럼 없음 → 직급 미표시(부서명만). "법적 근속"·"다음 부여 예정일"은 정책 계산 의존이므로 백엔드가 산출 못 하면 `'-'` 반환, 프론트는 그대로 표시.

---

## UI-C. 연차 수동 부여 (ManualGrantPop.vue)

### C-1. 레이아웃 와이어프레임 (narrow/normal 모달)

```
┌ .prafta-modal-popup .modal-content-normal (max ~520px) ─────────────────┐
│ modal-header   연차 수동 부여                              [X]          │
├ modal-body ─────────────────────────────────────────────────────────────┤
│ ┌ 대상 카드 ────────────────────────────────────────────────────────┐  │
│ │ (단일) (아바타) 김도현 · 생산팀                                    │  │
│ │ (일괄) 선택된 직원 N명에게 일괄 부여합니다.  [대상 보기 ▾]         │  │
│ └────────────────────────────────────────────────────────────────────┘  │
│ 부여 유형*           [BaseSelect: 휴가종류 ▾]                            │  ← manual-types
│ 부여 일수*  [number] 일      사용 가능일*  [date]                        │
│ 부여 사유            [textarea]                                          │
│ ┌ info-box: 수동 부여 연차는 부여 이력에 영구 기록되며 감사 추적 가능 ┐ │
│ └────────────────────────────────────────────────────────────────────┘  │
├ modal-footer ──────────────────────────────────────────────────────────┤
│                                                   [취소] [부여하기]      │
└──────────────────────────────────────────────────────────────────────────┘
```

### C-2. 컴포넌트 매핑

| 영역 | 컴포넌트 | 비고 |
| --- | --- | --- |
| 모달 셸 | `.prafta-modal-popup .modal-content-normal` | |
| 부여 유형 | `BaseSelect` v-model=`leaveCd` | 옵션 = manual-types(LEAVE_TYPE='02' AND GRANT_TYPE='02' AND USE_YN='Y') |
| 부여 일수 | native `<input type="number" min=0.5 step=0.5>` | suffix "일" |
| 사용 가능일 | native `<input type="date">` | YYYY-MM-DD → YYYYMMDD 변환은 developer |
| 부여 사유 | native `<textarea>` | 선택 |
| 부여하기 | 버튼 → `fnSubmit()` | @Transactional 단일/일괄 분기 |
| 취소/닫기 | `emit('close')` | |

### C-3. props / emits

```
props: {
  targetUsers: Array,   // [{ userCd, userNm, deptNm }] 단일=1건, 일괄=N건
}
emits: ['close', 'granted']   // granted → 부모(대시보드/상세) 재조회 트리거
```

> useModal이 onClose를 덮어쓰므로 부여 성공 재조회는 `onGranted` prop 콜백 또는 `emit('granted')` 로 전달(LeavePolicyImpactPop의 onSaved/emit('saved') 패턴 동일).

### C-4. 검증 (1차 게이트 — 최종 권위는 백엔드)

| 항목 | 규칙 | 위반 메시지 |
| --- | --- | --- |
| 부여 유형 | 필수 선택 | "부여 유형을 선택해 주세요." |
| 부여 일수 | > 0 | "부여 일수는 0보다 커야 합니다." |
| 부여 일수 | 0.5 단위 (값*2 정수) | "부여 일수는 0.5일 단위로 입력해 주세요." |
| 사용 가능일 | 필수, 8자리 형식 | "사용 가능일을 입력해 주세요." |
| (경고) 사용 가능일 | 오늘 이전이면 즉시 사용 가능 안내(차단 아님) | confirm "사용 가능일이 오늘 이전입니다. 진행하시겠습니까?" |

> 정책서 §8.5.8: 부여 레코드 GRANT_TYPE='MANUAL_OTHER'(법정외), GRANT_BY_TYPE='ADMIN', POLICY_SEQ=NULL, IDEMPOTENCY_KEY="{USER_CD}_{TIMESTAMP}_MANUAL" (백엔드). AVAIL_TO_DATE = AVAIL_FROM_DATE + 활성정책 AXIS6_VALIDITY_MONTHS(없으면 12).

### C-5. 상태별 동작

| 상태 | UI |
| --- | --- |
| loading | `isLoading` 동안 [부여하기] 비활성 |
| 유형 옵션 로딩 실패 | `proxy.$alert(...)`, 셀렉트 빈 상태 |
| error | `proxy.$alert(resolveApiErrorMessage(...))` |
| success | `proxy.$alert('부여되었습니다.')` → `emit('granted')` → `emit('close')` |

### C-6. 백엔드 의존 (PRAFTA-017-2-1 backend)

```
GET  /webApi/baim08/leave-grant/manual-types
     resp: { types: [ { leaveCd, leaveNm } ] }   // LEAVE_TYPE='02' AND GRANT_TYPE='02' AND USE_YN='Y' AND CMPNY_CD

POST /webApi/baim08/leave-grant/manual-grant        (단일)
     body: { userCd, leaveCd, grantDays, availFromDate(YYYYMMDD), reason }
POST /webApi/baim08/leave-grant/bulk-manual-grant   (일괄)
     body: { userCds:[...], leaveCd, grantDays, availFromDate, reason }
     → tb_user_leave_grant INSERT (각 직원 1건), @Transactional, 권한 MASTER/HR
```

---

## D. 결정 필요 / 합리적 기본값 (메인 세션 검토)

| # | 항목 | planner 기본값(진행) | 비고 |
| --- | --- | --- | --- |
| D-1 | 연차사용계획서 조회 | 버튼만, 클릭 시 "준비 중" alert | 요청서 명시(세부 미확정) |
| D-2 | [엑셀] 내보내기 구현 | 클라이언트 CSV(현 페이지 데이터) | 전체 export면 백엔드 별도. 일단 화면 데이터 CSV |
| D-3 | 직급 표시 | 미표시(부서명만) | TB_USER 직급 컬럼 없음(실측). 화면8 D-4 동일 |
| D-4 | 법적 근속/다음 부여 예정일 | 백엔드 미산출 시 '-' | 정책 계산 의존, 근사 한계 |
| D-5 | 부서 셀렉트 옵션 출처 | tb_site_node(현재 사업장 노드) | developer가 조회 연결. 사업장 스코프 정의 필요 |
| D-6 | 모달 너비 | 상세=wide(1080), 수동부여=normal(520) | modal-popup-guide 토큰 |
| D-7 | 일괄 부여 대상 표시 | "N명에게 일괄 부여" + 대상 보기 토글 | 시안엔 단일만 — 일괄은 카운트 표기 |
