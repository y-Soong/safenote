# UI-{순번} Baim_08 정책 변경 영향 분석 (신규)

> 메인 세션(orchestrator)이 Notion "도메인 지식 베이스"에 `UI-{순번} Baim_08` 행으로 등록.
> 검증 상태: `Claude 분석`. 영역: web. 모듈: baim. 현재 동작: 신규 작성.
> 연결: Baim_07(통합 연차 부여 정책)의 [분석 실행] 진입 화면. prafta-018 일환.

---

## 1. 개요

| 항목 | 값 |
| --- | --- |
| 화면 ID | UI-{순번} |
| 화면명 | 정책 변경 영향 분석 |
| 연결 작업 | PRAFTA-018-{n}-FE-01 (Baim_08 script), PRAFTA-018-{n}-FE-02 (Baim_07 네비게이션+store), PRAFTA-018-{n}-BE-01~03 (엔진/엔드포인트/매퍼) |
| 화면 위치 | `src/views/baim/Baim_08.vue` (신규) |
| 보조 store | `src/stores/leavePolicyDraftStore.js` (신규, 타깃 axis 전달용) |
| 정책서 출처 | `.claude/requests/ref/prafta-017/CLAUDE_CODE_INSTRUCTIONS.md` §9 (전체: §9.2 구성 / §9.4 진입흐름 / §9.5 diff규칙 / §9.6 주요영향 / §9.8 알고리즘 / §9.10 검증 / §9.11 TC) + `.claude/context/policies/attd/08-leave.md` §8.5.2(7 axis)·§8.5.7(권한)·§8.5.8(과거 소급 금지) |
| 디자인 시안 | `.claude/requests/ref/prafta-017/08-policy-change-impact.html` (카드/레이아웃 참고용. diff axis 순서는 본 화면이 Baim_07 UI 순서를 따름 — 가드레일 2) |
| 참조 패턴 | `src/views/baim/Baim_07.vue` (axis 카드/배지/노트 스타일, useModal, proxy.$alert/$confirm, axios, getMessage/MSG, resolveApiErrorMessage), `src/views/baim/Baim_06.vue`(viewComm/viewBody, /webApi/baim06) |
| 접근 권한 | 진입: 인증 사용자(화면은 열림) / [분석 실행]·[정책 변경 진행] 호출: AUTH_MASTER OR AUTH_HR_MANAGER (서버 `ensureManager` 강제, 화면은 진입 가능하나 호출 시 403 처리) — 정책서 §8.5.7 |
| 진입 경로 | Baim_07 [분석 실행] 클릭 → `leavePolicyDraftStore`에 현재 폼 타깃 axis 적재 → `router.push("/main/{baim08 메뉴 route}")`. 타깃 store가 비어 있으면(직접 URL 진입) Baim_07로 안내 후 복귀. → **결정 필요 D-1** |

---

## 2. 구성 영역 (§9.2 / HTML 시안 기준, 풀페이지 back-link형)

```
┌────────────────────────────────────────────────────────────────┐
│ ‹ 연차 정책으로 돌아가기                                          │ ← back-link
│ ▍정책 변경 영향 분석                                             │ ← page-title (좌측 바)
│   [연차 부여 정책] 화면에서 설정한 변경이 영향받는 직원과 ...     │ ← page-desc
│ ┌── 정책 요약 카드 (input-card) ──────────────────────────────┐  │
│ │ 현재 정책        →   변경할 정책        변경적용일[____] [분석실행]│ │
│ │ 회계연도 기준(비례)   입사일 기준                                │ │
│ │ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─    │ │
│ │ 변경 사항 상세 보기                                       ▾   │ │ ← diff-toggle(기본 펼침)
│ │ ┌ 1 부여 기준 ──────┐ ┌ 2 입사 첫해 처리 ─┐                   │ │ ← diff-panel 2열, Baim_07 UI 순서
│ │ │ 회계연도→입사일   │ │ 비례→월차만       │                   │ │
│ │ ├ 3 비례 반올림 ────┤ ├ 4 회계연도 시작일 ┤                   │ │
│ │ │ 올림→(조건부 비활)│ │ 1/1→(비활성)      │                   │ │
│ │ ├ 5 근속 가산 ──────┤ ├ 6 유효기간 ───────┤                   │ │
│ │ │ 변경 없음·법정    │ │ 변경 없음·12개월  │                   │ │
│ │ ├ 7 사용촉진 ───────┤                                          │ │
│ │ │ 변경 없음·사용안함│                                          │ │
│ │ └───────────────────┘                                          │ │
│ └────────────────────────────────────────────────────────────┘  │
│ ⓘ 직원별 표·요약은 1년치 부여 시뮬레이션 기반 근사치입니다 ...   │ ← 근사치 안내 (warning note)
│ ┌ 전체 직원 ┐ ┌ 정상 적용 ┐ ┌ 주의 필요 ┐ ┌ 추가 부담 합계 ┐    │ ← 요약카드 4 (warning/danger)
│ │ 47명      │ │ 35명      │ │ 12명      │ │ 180일          │    │
│ └───────────┘ └───────────┘ └───────────┘ └────────────────┘    │
│ 영향받는 직원 (주의 필요 12명)                                   │ ← section-title
│ ┌──────────────────────────────────────────────────────────┐    │
│ │ 직원 | 입사일 | 기존 부여 | 기존 사용 | 예상 추가 | 주요 영향 │    │ ← 직원표
│ │ 박서연                                                       │    │
│ │ 물류 / 사원   2025-12-25  15일  5일  +26일  1년 미만 월차 ... │    │
│ └──────────────────────────────────────────────────────────┘    │
│                                 [상세 리포트 다운로드] [정책 변경 진행]│ ← 하단 버튼
└────────────────────────────────────────────────────────────────┘
```

- HTML 시안의 `.container`(max-width 1080px 중앙정렬)를 따르되 토큰 기반으로 변환. Baim_07이 `max-width: 1280px`를 쓰므로 본 화면도 동일 컨테이너 폭 관례를 우선하고 시안 카드 스타일을 차용.
- 본 화면은 ViewHeader를 쓰지 않고 시안처럼 자체 헤더(back-link + page-title)를 사용한다(풀페이지형). → **결정 필요 D-2**

---

## 3. 상태 (reactive) 목록

> 모두 `ref`. 초기값은 빈/기본. 실제 채움은 developer 몫.

| 변수 | 타입 | 초기값 | 설명 |
| --- | --- | --- | --- |
| `targetPolicy` | Object\|null | `null` | Baim_07에서 store로 전달받은 타깃 axis 조합(LeavePolicySaveRequest 형태). 없으면 진입 불가 안내. |
| `currentPolicySummary` | String | `''` | 현재 정책 한 줄 요약(서버 응답 또는 store) |
| `targetPolicySummary` | String | `''` | 변경할 정책 한 줄 요약 |
| `applyDate` | String | `''` | 변경 적용일 (YYYY-MM-DD input). 분석/진행 호출 직전 YYYYMMDD 변환. |
| `diffOpen` | Boolean | `true` | diff 패널 펼침/접힘 (기본 펼침, §9.5.3) |
| `diffList` | Array | `[]` | `{ axisNum, axisName, fromValue, toValue, changeType, note }` 7행 (Baim_07 UI 순서) |
| `summary` | Object | `{ totalEmployees: 0, normalCount: 0, affectedCount: 0, additionalDaysTotal: 0 }` | 요약 카드 4개 값 |
| `affectedEmployees` | Array | `[]` | `{ userCd, userNm, deptNm, positionNm, hireDate, currentGrant, currentUsed, expectedAdditional, mainImpact }` |
| `analyzed` | Boolean | `false` | 분석 실행 여부(실행 전엔 요약/표 영역 안내 노출) |
| `isLoading` | Boolean | `false` | 분석/진행 로딩 |

### 3.1 computed (시그니처만 — 본문 developer)

| computed | 반환 | 설명 |
| --- | --- | --- |
| `hasTarget` | Boolean | `targetPolicy != null` (없으면 본문 비활성 + 복귀 안내) |
| `affectedCountLabel` | String | `영향받는 직원 (주의 필요 N명)` 섹션 타이틀 |
| `canProceed` | Boolean | 분석 완료 + 변경사항 존재 시 [정책 변경 진행] 활성 |

---

## 4. diff 패널 규칙표 (§9.5, Baim_07 UI 순서 — 가드레일 2)

> 표시 순서는 **Baim_07 UI 7-axis 순서**를 따른다(HTML 시안의 옛 순서 아님).
> axis↔백엔드 필드 매핑은 prafta-017-1-ui-spec §4와 동일. DB 컬럼 rename 금지.

| diff 표시 순서 | axisNum | axisName(라벨) | 백엔드 필드 | DB 컬럼 | 비활성 판단(§9.5.2) |
| :---: | :---: | --- | --- | --- | --- |
| 1 | 1 | 연차 부여 기준 | `axis1GrantBase` | `AXIS1_GRANT_BASE` | 단순 변경 비교 |
| 2 | 3 | 입사 첫해 처리 방식 | `axis3FirstYearMethod` | `AXIS3_FIRST_YEAR_METHOD` | 단순 변경 비교 |
| 3 | 4 | 비례 부여 시 반올림 | `axis4ProrateRounding` | `AXIS4_PRORATE_ROUNDING` | **조건부 비활성**: PRORATE→그외 시 `(조건부 비활성)`, 그외→PRORATE 시 `(조건부 비활성)→새값` |
| 4 | 2 | 회계연도 시작일 | `axis2FiscalStartMm/Dd` | `AXIS2_FISCAL_START_MM/DD` | **비활성**: FISCAL_YEAR→HIRE_DATE 시 `(비활성)`, HIRE_DATE→FISCAL_YEAR 시 `(비활성)→새값` |
| 5 | 5 | 근속 가산 정책 | `axis5TenureMode/StartYear/Interval/MaxDays` | `AXIS5_*` | 모드/n/m/max 중 하나라도 다르면 변경 |
| 6 | 6 | 연차 유효기간 | `axis6ValidityMonths` | `AXIS6_VALIDITY_MONTHS` | 단순 변경 비교 |
| 7 | 7 | 연차 사용촉진 제도 | `axis7UsePromotion` | `AXIS7_USE_PROMOTION` | 단순 변경 비교 |

### 4.1 changeType → 표시 매핑 (§9.5.1)

| changeType | 표시 | 카드 스타일 |
| --- | --- | --- |
| `CHANGED` | `이전값(취소선) → 새값(강조)` | 기본(흰 카드) + diff-from(취소선)/diff-to(primary 강조) |
| `DEACTIVATED` | `이전값 → (비활성)` 또는 `(조건부 비활성)` | diff-to에 disabled 스타일(흐림/italic) |
| `ACTIVATED` | `(비활성) → 새값` | diff-to 강조 |
| `UNCHANGED` | `변경 없음 · 현재값` | unchanged 카드(흐린 배경, 회색 텍스트), note 활용 |

> diff 라벨 텍스트(예: "회계연도 기준", "1월 1일", "법정 기준 유지(n=3,m=2,max=25)")는 백엔드 `diff[].fromValue/toValue/note`가 제공. 프론트는 표시만.

---

## 5. 요약 카드 4개 (§9.2-3)

| 카드 | 값 키 | 단위 | 스타일 |
| --- | --- | --- | --- |
| 전체 직원 | `summary.totalEmployees` | 명 | 기본 |
| 정상 적용 | `summary.normalCount` | 명 | 기본 |
| 주의 필요 | `summary.affectedCount` | 명 | warning(`--color-warning-bg`/`--color-warning-text`) |
| 추가 부담 합계 | `summary.additionalDaysTotal` | 일 | danger(`--color-danger` 기반 rgba 배경) |

---

## 6. 직원표 필드 (§9.2-4 / §9.6)

| 컬럼 | 키 | 정렬 | 비고 |
| --- | --- | --- | --- |
| 직원 | `userNm` + `deptNm / positionNm` | 좌 | 2줄(이름 / 부서·직급). **PII(이름/부서)** — 관리자 화면 한정 노출 |
| 입사일 | `hireDate` | 좌 | YYYY-MM-DD 표기(서버 YYYYMMDD → 프론트 포맷) |
| 기존 부여 | `currentGrant` | 우 | `N일` |
| 기존 사용 | `currentUsed` | 우 | `N일` |
| 예상 추가 | `expectedAdditional` | 우 | `+N일` (warning 색 강조) — **근사치** |
| 주요 영향 | `mainImpact` | 좌 | 서버가 §9.6 우선순위로 생성한 단일 메시지 |

> positionNm(직급): TB_USER에 직급 컬럼이 없다 → **결정 필요 D-4**. deptNm: TB_USER.NODE_CD는 코드이므로 TB_NODE 조인으로 명칭 조회.

---

## 7. 네비게이션 설계

### 7.1 라우트
- 앱 라우팅은 DB 메뉴 기반(`/main/{menuRoute}` children, `viewResolver.js`의 `import.meta.glob`이 `Baim_08.vue`를 자동 로드). 별도 정적 router 등록 불필요.
- **전제**: Baim_08이 메뉴(TB_MENU)로 등록되어 `view='baim/Baim_08'`, `route` 키가 있어야 `router.push("/main/{route}")`로 이동 가능. → **결정 필요 D-1**(메뉴 등록 또는 Baim_07이 보유한 형제 메뉴 route 키 활용).
- `defineOptions({ name: "Baim_08" })` 필수(viewResolver/keep-alive 키 매칭).

### 7.2 타깃 전달 store (`leavePolicyDraftStore.js`, 신규 — 권장)
```
state: { target: null }   // LeavePolicySaveRequest 형태(타깃 axis + applyFromDate 후보)
actions:
  setTarget(payload)      // Baim_07 [분석 실행]에서 적재
  clearTarget()           // Baim_08 진입 후 소비 또는 이탈 시 정리
```
- Baim_07 `fnGoImpactAnalysis`: 현재 폼으로 `fnBuildSaveRequest`(applyFromDate 제외/임시) 구성 → `setTarget(payload)` → `router.push("/main/{baim08 route}")`.
- Baim_08 `onMounted`: store.target 읽어 `targetPolicy`에 매핑. **null이면** `proxy.$alert("연차 정책 화면에서 [분석 실행]으로 진입해 주세요.")` 후 Baim_07로 복귀(또는 back-link만 노출하고 본문 비활성). 직접 URL 진입 방어.
- back-link [연차 정책으로 돌아가기]: `router.push("/main/{baim07 route}")` 또는 `router.back()`. → developer가 메뉴 route 확정 후 결정.

---

## 8. API 연동 표

> ⭐ 가드레일 1: `/webApi/baim07/...` 프리픽스 필수(axios baseURL `/prafta` 제공). 형제 Baim_06이 `/webApi/baim06/...` 호출하는 패턴 동일.
> 신규 저장 엔드포인트 만들지 않는다 — [정책 변경 진행]은 기존 저장 재사용(가드레일).

| 동작 | 메서드/엔드포인트 | 요청 | 응답 | 권한 |
| --- | --- | --- | --- | --- |
| 영향 분석 | `POST /webApi/baim07/policy/analyze-impact` | `LeavePolicySaveRequest`(타깃 axis + applyFromDate) | `AnalyzeImpactResponse` | MASTER/HR |
| 정책 변경 진행(신규) | `POST /webApi/baim07/policy` | `LeavePolicySaveRequest`(타깃+applyFromDate+changeReason) | `LeavePolicySaveResponse` | MASTER/HR |
| 정책 변경 진행(활성 존재) | `PUT /webApi/baim07/policy/{policySeq}` | 〃 | 〃 | MASTER/HR |

### 8.1 호출 흐름(developer)
- [분석 실행]: applyDate(YYYY-MM-DD)→YYYYMMDD 변환, targetPolicy에 applyFromDate 주입 → `POST analyze-impact` → `summary/diff/affectedEmployees` 매핑, `analyzed=true`.
- [정책 변경 진행]: `ReasonInputModal`로 changeReason 입력 → body에 applyFromDate+changeReason 포함 → policySeq 유무로 POST/PUT 분기(Baim_07 §7.1과 동일). 성공 시 `proxy.$alert(MSG.SAVE_SUCCESS)` → Baim_07 복귀.
- [상세 리포트 다운로드]: 1차 범위에서는 화면 데이터 클라이언트 CSV/엑셀 또는 "준비중". → **결정 필요 D-3**.

### 8.2 AnalyzeImpactResponse (백엔드 신규 — §11 참조)
```
{
  summary: { totalEmployees, normalCount, affectedCount, additionalDaysTotal },
  diff: [ { axisNum, axisName, fromValue, toValue, changeType, note } ],   // 백엔드가 Baim_07 UI 순서로 정렬해 반환 권장
  affectedEmployees: [ { userCd, userNm, deptNm, positionNm, hireDate, currentGrant, currentUsed, expectedAdditional, mainImpact } ]
}
```

---

## 9. 검증 규칙 (§9.10, 프론트 1차 — 최종 권위는 백엔드)

| # | 항목 | 프론트 1차 | 백엔드 |
| --- | --- | --- | --- |
| 1 | applyDate 미래 | 오늘 이전이면 차단 + alert | `validateAxisMatrix(enforceFutureApply)` ATTD_400_020 |
| 2 | applyDate 12개월 이내 | 12개월 초과 시 경고(허용은 가능) | 백엔드 정책 위임 |
| 3 | axis 매트릭스 | targetPolicy는 Baim_07에서 이미 매트릭스 통과분 | `validateAxisMatrix` 재검증 |
| 4 | 변경 없음 | 분석 결과 diff 전부 UNCHANGED면 [진행] 비활성 + 안내 | analyze-impact가 400 거부 가능("변경 사항이 없습니다") |
| 5 | AXIS5 변경 | — | 법정 위반 방지 추가 검증(기존 로직) |

---

## 10. 상태별 동작

| 상태 | UI |
| --- | --- |
| 진입 직후(타깃 없음) | 본문 비활성 + "연차 정책에서 [분석 실행]으로 진입" 안내, back-link만 활성 |
| 진입 직후(타깃 있음, 미분석) | 정책 요약/diff 노출, 요약카드·직원표 자리에 "변경 적용일 입력 후 [분석 실행]" placeholder |
| loading | [분석 실행]/[정책 변경 진행] disabled, (선택) 스피너 |
| empty(영향 직원 0) | 요약카드는 노출(affectedCount=0), 직원표 자리에 "영향받는 직원이 없습니다" |
| error | `resolveApiErrorMessage(err, fallback)` → `proxy.$alert(msg)` |
| success(분석) | 요약카드 4 + 직원표 렌더, [정책 변경 진행] 활성 |
| success(진행) | `proxy.$alert(MSG.SAVE_SUCCESS)` 후 Baim_07 복귀 |

---

## 11. 백엔드 계약 + 알고리즘 명세 (developer 구현용)

### 11.1 엔드포인트
- `POST /baim07/policy/analyze-impact` → 실제 `/prafta/webApi/baim07/policy/analyze-impact`.
- 컨트롤러: `Baim07Controller`에 `@PostMapping("/policy/analyze-impact")` 추가.
- 요청: 기존 `LeavePolicySaveRequest` 재사용(타깃 axis + applyFromDate). currentPolicy는 서버가 `TB_LEAVE_POLICY WHERE USE_YN='Y'`로 직접 조회.
- 권한: `previewImpact`와 동일하게 서비스 진입부 `ensureManager(authCd, ...)` (§8.5.7).
- 저장 없음(읽기 전용 시뮬레이션, §9.9).

### 11.2 응답 DTO (신규)
- `AnalyzeImpactResponse`(web.baim.baim07.dto.response): `summary`, `diff[]`, `affectedEmployees[]`.
- 권장 위치: 공통 재사용 가능성이 있으면 `common.cmm.leave.vo`에 `AnalyzeImpactVO`/`ImpactDiffVO`/`AffectedEmployeeVO`를 두고 response가 래핑. (기존 `ImpactSummaryVO`는 그대로 두고 신규 추가 — rename 금지.)
- 필드:
  - `summary { int totalEmployees; int normalCount; int affectedCount; BigDecimal additionalDaysTotal; }`
  - `diff[] { int axisNum; String axisName; String fromValue; String toValue; String changeType; String note; }`
  - `affectedEmployees[] { String userCd; String userNm; String deptNm; String positionNm; String hireDate; BigDecimal currentGrant; BigDecimal currentUsed; BigDecimal expectedAdditional; String mainImpact; }`

### 11.3 알고리즘 (§9.8 + §9.6, 근사 best-effort)
1. 현재 정책 조회: `selectActivePolicy(cmpnyCd)`(기존). null이면 "현재 활성 정책 없음" → 모든 axis "신규"로 diff, 직원 시뮬은 현재=0 기준.
2. axis 매트릭스 검증: `validateAxisMatrix(cmd, enforceFutureApply=false)`(기존 재사용). 현재==타깃이면 400 거부(변경 없음, §9.10-4).
3. diff 계산: §9.5.2 비활성 판단 포함, **Baim_07 UI 순서**(1,3,4,2,5,6,7)로 정렬해 반환. fromValue/toValue/note는 코드값→한글 라벨 변환(서비스 내 라벨 맵).
4. 활성 직원 조회: 신규 매퍼 `selectActiveUsersForImpact(cmpnyCd)` — userCd/userNm/NODE_CD(→deptNm)/HIRE_DATE. (countActiveUsers와 동일 필터: USE_YN='Y' AND WITHDRAWAL_DATE IS NULL AND ACCOUNT_STATUS='01'.) LIMIT/페이징 가드 적용(대량 사업장 대비).
5. 직원별 1년치 부여 근사 시뮬:
   - `simulateAnnualGrant(emp, policy, applyDate)` = 본연차(15) + 1년 미만 월차(최대 11, 입사 12개월 미만 시 경과월수 근사) + 근속가산(`year>=start ? floor((year-start)/interval)+1 : 0`, max=axis5MaxDays 상한). 비례/회계연도/입사일 분기는 §4.8 부여 시점 규칙 근사.
   - `expectedAdditional = simulate(target) - simulate(current)`. >0 이면 affected.
6. mainImpact: §9.6 우선순위(1 1년미만+회계부여이력 / 2 11~12개월 도래 / 3 회계→입사 월차누락 / 4 axis5 변경 차이 / 5 기존 유지)로 단일 메시지. **정밀 부여엔진이 없으므로 근사** — 한계 문구를 화면에 노출.
7. summary 합산: totalEmployees=활성직원수, affectedCount=expectedAdditional>0 인원, normalCount=total-affected, additionalDaysTotal=ΣexpectedAdditional.

### 11.4 활성 직원 매퍼 (신규)
- `LeavePolicyMapper.selectActiveUsersForImpact(cmpnyCd)` → `List<AffectedEmployeeBaseVO>` (userCd/userNm/deptNm/hireDate). currentGrant/currentUsed는 시뮬레이션 단계에서 채움(또는 TB_USER_LEAVE_BALANCE류 테이블이 있으면 조회 — **DESCRIBE로 확인, 결정 필요 D-5**).
- TB_USER 실측: `USER_CD`, `USER_NM`(PII 평문), `NODE_CD`(부서코드), `HIRE_DATE`(varchar8). **직급 컬럼 없음**(D-4). 부서명은 `TB_NODE` 조인(`CMPNY_CD`,`SITE_CD`,`NODE_CD`).
- PII: userNm/deptNm은 관리자(MASTER/HR) 화면 한정 노출. 로그에는 마스킹(userCd만 기록).

---

## 12. 테스트 케이스 (§9.11 — 화면+계약 관점)

| TC | 시나리오 | 기대 |
| --- | --- | --- |
| TC-01 | Baim_07 변경 없이 [분석 실행] | analyze-impact 400("변경 사항 없음") → alert |
| TC-02 | Baim_07 → Baim_08 진입 | 현재/변경 정책 readonly 자동 표시(store target 매핑) |
| TC-03 | diff 패널 | 변경 axis 강조, 미변경 axis 회색(unchanged), Baim_07 UI 순서 |
| TC-04 | diff 토글 | 접기/펼치기 정상(기본 펼침) |
| TC-05 | 회계연도 비례→입사일+1년미만 직원 | mainImpact="1년 미만 월차 N일 + 1년차 15일 추가 발생" |
| TC-06 | 입사 11개월 직원 | mainImpact="1년차 도래 시 15일 추가 발생" |
| TC-07 | AXIS5만 변경 | mainImpact="근속 가산 정책 변경으로 N일 차이" |
| TC-08 | AXIS1 변경(한쪽 HIRE_DATE) | diff axis2(회계연도 시작일) 비활성 표시 |
| TC-09 | AXIS3(UI2) 변경 | diff axis4(비례 반올림) 조건부 비활성 표시 |
| TC-10 | applyDate 어제 입력 | 프론트 차단 + 백엔드 ATTD_400_020 |
| TC-11 | [정책 변경 진행] | 기존 POST/PUT 재사용 → TB_LEAVE_POLICY UPDATE + HISTORY INSERT(IMPACT_SUMMARY 포함) |
| TC-12 | 직접 URL 진입(store 비어 있음) | 본문 비활성 + Baim_07 진입 안내 |

---

## 13. 근사치 한계 (화면 명시 필수)

> §9.6/§9.8 규칙을 구현하되, 1년치 부여 시뮬레이션은 정밀 부여엔진(법정 연차 자동 부여 배치, §10)이 미구현이므로 **근사치**다.
> - 1년 미만 월차는 경과 개월수 근사(만근/결근 미반영).
> - 회계연도/입사일 시점 비례부여는 §4.8 시간축 근사.
> - 기존 부여/사용(currentGrant/currentUsed)은 잔액 테이블 확정 전까지 근사(D-5).
> 화면 상단 직원표 위에 "직원별 표·요약은 1년치 부여 시뮬레이션 기반 근사치이며 실제 부여 결과와 차이가 있을 수 있습니다." warning note를 항상 노출.

---

## 14. 반응형

- 데스크탑(관리자 백오피스) 우선. 컨테이너 max-width(1280px) 중앙정렬.
- `@media (max-width: 768px)`: diff-panel 2열→1열, summary-grid 4열→2열(또는 1열), 정책 요약 row 세로 스택, 직원표 가로 스크롤(`overflow-x:auto`).

---

## 15. developer 인계 노트 (script 영역)

- `getCurrentInstance().proxy`로 `$alert`/`$confirm`, `useModal().open`으로 `ReasonInputModal`, `axios`(`@/api/axios`), `getMessage(MSG.*)`, `resolveApiErrorMessage`, `useRouter`/`useUserStore`/`useLeavePolicyDraftStore`.
- `onMounted`: store.target 매핑 → 없으면 복귀 안내. (analyze-impact 자동 호출은 하지 않고 [분석 실행] 버튼으로 트리거 — 사용자가 applyDate 입력 후 실행.)
- `defineOptions({ name: "Baim_08" })` 필수.
- diff 라벨 텍스트는 백엔드 응답(fromValue/toValue/note)을 그대로 표시(프론트 라벨 매핑 최소화).
- 골격에 모든 마크업/스타일/상태선언/computed·메서드 시그니처 + `// TODO(developer):` 작성 완료. 실제 API/store/router 연결은 developer.

---

## 16. 결정 필요 항목

- **D-1 (Baim_08 메뉴 route 키)**: Baim_08을 TB_MENU에 등록해 `/main/{route}` 이동을 쓸지, 아니면 Baim_07이 보유한 형제 route 키로 우회할지 미확정. 합리적 기본값: Baim_08을 메뉴로 등록(또는 Baim_07과 같은 baim 그룹 하위 숨김 메뉴)하고 `router.push("/main/baim08")` 가정. developer/메인이 실제 메뉴 route 확정 필요.
- **D-2 (헤더 형식)**: ViewHeader 미사용·자체 back-link 헤더로 결정(시안 풀페이지형). baim 표준과 다르므로 메인 검토 권장.
- **D-3 (상세 리포트 다운로드)**: 1차 범위는 화면 데이터 클라이언트 CSV 또는 "준비중" 처리. 서버 리포트 엔드포인트는 신규 범위 밖.
- **D-4 (직급 positionNm)**: TB_USER에 직급 컬럼 없음. 합리적 기본값: positionNm은 빈 값/미표시 또는 AUTH_CD 라벨 대체. 시안의 "사원/대리/주임"에 해당하는 직급 마스터 미존재 → deptNm(부서)만 표기 권장.
- **D-5 (기존 부여/사용 데이터 출처)**: currentGrant/currentUsed의 실제 출처(잔액/부여 이력 테이블) 미확정. 정밀 부여엔진(§10) 미구현 → 근사. developer가 DESCRIBE로 잔액성 테이블 존재 확인 후 결정.
