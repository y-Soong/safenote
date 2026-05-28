# UI-{순번} Baim_07 연차 부여 정책 (통합 화면)

> 메인 세션(orchestrator)이 Notion "도메인 지식 베이스"에 `UI-{순번} Baim_07` 행으로 등록.
> 검증 상태: `Claude 분석`. 영역: web. 모듈: baim. 현재 동작: 신규 작성.

---

## 1. 개요

| 항목 | 값 |
| --- | --- |
| 화면 ID | UI-{순번} |
| 화면명 | 연차 부여 정책 |
| 연결 작업 | PRAFTA-017-1-02 (프론트 script 구현), PRAFTA-017-1-01 (백엔드 정리) |
| 화면 위치 | `src/views/baim/Baim_07.vue` (신규) |
| 보조 컴포넌트 | `src/views/baim/popup/LeavePolicyPreviewPop.vue` (기존 골격 존재, script 미완) |
| 정책서 출처 | `.claude/context/policies/attd/08-leave.md` §8.5 (전체), 작업 요청서 `prafta-017-1.md` §4 |
| 디자인 시안 | `.claude/requests/ref/prafta-017-1/03-leave-policy.html` |
| 참조 패턴 | `src/views/baim/Baim_06.vue` (ViewHeader + viewComm/viewBody 구조, useModal 팝업 호출, proxy.$alert/$confirm, axios, getMessage) |
| 접근 권한 | 조회: 인증 사용자 + 사업장 스코프 / 저장(POST·PUT): AUTH_MASTER OR AUTH_HR_MANAGER (정책서 §8.5.7, 서버에서 강제. 화면은 진입 가능하나 저장 시 403 처리) |
| 진입 경로 | 좌측 메뉴(LNB)의 baim 모듈 메뉴 코드로 라우팅. `viewResolver.js`의 `import.meta.glob("/src/views/**/*.vue")`가 `Baim_07.vue`를 자동 로드하므로 별도 router 등록 불필요. |

---

## 2. 구성 영역 (시안 §4.2 / HTML 기준)

```
┌──────────────────────────────────────────────────────────────┐
│ ViewHeader: "연차 부여 정책"          [변경 이력] [조회] [저장] │  ← 헤더 우측 액션
├──────────────────────────────────────────────────────────────┤
│ (viewBody)                                                     │
│ ┌──────────────────────────────────────────────────────────┐ │
│ │ ⓘ 부여 시점 미리보기 안내 카드      [부여 시점 미리보기] →│ │  ← preview-help-card
│ └──────────────────────────────────────────────────────────┘ │
│ ┌── axis 1 ─ 연차 부여 기준 ───────────────────────────────┐  │
│ │  ◉ 입사일 기준        ○ 회계연도 기준                     │  │
│ └──────────────────────────────────────────────────────────┘ │
│ ┌── axis 2 ─ 입사 첫해 처리 방식 ──────────────────────────┐  │
│ │  ◉ 월차만 부여   ○ 비례 부여(disabled)  ○ 차년도 일괄(dis)│  │
│ │  ⓘ 매트릭스 안내  / ⚠ 법정 월차 안내                      │  │
│ └──────────────────────────────────────────────────────────┘ │
│ ┌── axis 3 ─ 비례 부여 시 반올림  [조건부 활성] ───────────┐  │  ← AXIS2=PRORATE 시만 활성
│ │  ◉ 올림  ○ 반올림  ○ 내림  ○ 0.5일 절사  (전체 disabled) │  │
│ └──────────────────────────────────────────────────────────┘ │
│ ┌── axis 4 ─ 회계연도 시작일  [조건부 활성] ───────────────┐  │  ← AXIS1=FISCAL_YEAR 시만 활성
│ │  [ 1 ] 월  [ 1 ] 일   (disabled)                          │  │
│ └──────────────────────────────────────────────────────────┘ │
│ ┌── axis 5 ─ 근속 가산 정책 ───────────────────────────────┐  │
│ │  ◉ 법정 기준[법정]   ○ 회사 정책(직접 입력)               │  │
│ │   [3] 년차부터 [2] 년마다 +1일   (CUSTOM 시 활성)         │  │
│ │   최대 연차일수 [25] 일 [법정 25일]                       │  │
│ │   ✅ 부여 미리보기(실시간): 1~2년차 15일 / 3년차 16일 ... │  │
│ └──────────────────────────────────────────────────────────┘ │
│ ┌── axis 6 ─ 연차 유효기간 ────────────────────────────────┐  │
│ │  ◉ 12개월[법정]   ○ 24개월                                │  │
│ └──────────────────────────────────────────────────────────┘ │
│ ┌── axis 7 ─ 연차 사용촉진 제도 ───────────────────────────┐  │
│ │  ◉ 사용 안 함   ○ 사용(자동 통지)                         │  │
│ └──────────────────────────────────────────────────────────┘ │
│ ──── 휴가 사용 단위 정책 ────                                  │  ← section-divider
│ ┌──────────────────────────────────────────────────────────┐ │
│ │ 사용 단위(다중)         │ 같은 날 다중 신청                │ │
│ │ ☑1일(필수,disabled)     │ [허용(3건)/허용(2건)/불허] select│ │
│ │ ☑0.5일 ☑0.25일 ☐0.125일 │                                  │ │
│ └──────────────────────────────────────────────────────────┘ │
│ ──── 고급 기능 ────                                            │
│ ┌ 정책 변경 영향 분석                          [분석 실행] ┐  │  ← 화면8 라우팅 자리만
│ └──────────────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────────────┘
```

헤더 액션은 HTML 시안에서 [변경 이력] [조회] 2개지만, baim 표준 `ViewHeader`는 `save` 버튼을 노출하므로 [저장]도 헤더에 둔다. 본문 하단의 [취소][저장] 버튼군은 시안과 동일하게 유지(헤더 저장과 동일 동작). → **결정 필요 항목 D-3 참조.**

---

## 3. 상태 (reactive) 목록

> 모두 `ref`. 초기값은 정책서/시안 기본값 기준. 실제 채움(조회 응답 매핑)은 developer 몫.

| 변수 | 타입 | 초기값 | 설명 |
| --- | --- | --- | --- |
| `policySeq` | Number\|null | `null` | 활성 정책 식별자(PUT 시 path). null이면 신규(POST). |
| `axis1GrantBase` | String | `'HIRE_DATE'` | 1번 부여 기준 |
| `axis3FirstYearMethod` | String | `'MONTHLY_ONLY'` | **UI 2번** 첫해 처리 (백엔드 axis3) |
| `axis4ProrateRounding` | String | `'CEIL'` | **UI 3번** 비례 반올림 (백엔드 axis4) |
| `axis2FiscalStartMm` | String | `'01'` | **UI 4번** 회계연도 시작월 (백엔드 axis2) |
| `axis2FiscalStartDd` | String | `'01'` | **UI 4번** 회계연도 시작일 (백엔드 axis2) |
| `axis5TenureMode` | String | `'LEGAL'` | 5번 근속 모드 |
| `axis5StartYear` | Number | `3` | 5번 가산 시작 연차 (LEGAL 시 3 고정) |
| `axis5Interval` | Number | `2` | 5번 가산 주기 (LEGAL 시 2 고정) |
| `axis5MaxDays` | Number | `25` | 5번 최대 연차일수 (≥25) |
| `axis6ValidityMonths` | Number | `12` | 6번 유효기간 (12/24) |
| `axis7UsePromotion` | String | `'N'` | 7번 사용촉진 |
| `applyFromDate` | String | `''` | 정책 적용 시작일 (YYYYMMDD). developer가 기본값/입력 UI 결정 → **결정 필요 D-1** |
| `allowFullDay` | Boolean | `true` | 1일 단위 (항상 true, disabled) |
| `allowHalfDay` | Boolean | `true` | 0.5일 단위 |
| `allowQuarterDay` | Boolean | `true` | 0.25일 단위 |
| `allowHourly` | Boolean | `false` | 0.125일(1시간) 단위 |
| `maxDailyRequest` | Number | `3` | 같은 날 최대 신청 건수 (0=불허) |
| `changeReason` | String | `''` | 변경 사유 (저장 시 입력받음 → ReasonInputModal 활용 권장) |
| `isLoading` | Boolean | `false` | 조회/저장 로딩 |

### 3.1 computed (시그니처만 — 본문은 developer)

| computed | 반환 | 설명 |
| --- | --- | --- |
| `axis2Disabled` | `{ PRORATE: bool, NEXT_YEAR_BULK: bool }` | 1번 매트릭스(§4.3). axis1=HIRE_DATE면 PRORATE/NEXT_YEAR_BULK 비활성 |
| `axis3Active` | Boolean | UI 3번 활성 여부 = `axis3FirstYearMethod === 'PRORATE'` |
| `axis4Active` | Boolean | UI 4번 활성 여부 = `axis1GrantBase === 'FISCAL_YEAR'` |
| `halfDayLocked` | Boolean | UI 3번 = HALF_DAY → 0.5일 강제 체크+잠금 (§4.6) |
| `tenureCustom` | Boolean | `axis5TenureMode === 'CUSTOM'` |
| `tenurePreview` | `Array<{label, days, isMax}>` | 5번 실시간 부여 미리보기 (§4.7.4 계산식) |

---

## 4. axis ↔ 백엔드 필드 매핑표 (확정 — 가드레일 1)

> UI 표시 순서(시안 v2 재배치)와 DB/백엔드 필드명(OLD axis 번호)이 다르다.
> **DB 컬럼 rename/마이그레이션 금지.** UI 번호 → 기존 필드명 매핑으로만 처리.

| UI 번호(시안) | UI 라벨 | 백엔드 요청 필드(camelCase) | DB 컬럼 | 옵션/입력 |
| :---: | --- | --- | --- | --- |
| 1 | 연차 부여 기준 | `axis1GrantBase` | `AXIS1_GRANT_BASE` | HIRE_DATE / FISCAL_YEAR |
| 2 | 입사 첫해 처리 방식 | `axis3FirstYearMethod` | `AXIS3_FIRST_YEAR_METHOD` | MONTHLY_ONLY / PRORATE / NEXT_YEAR_BULK |
| 3 | 비례 부여 시 반올림 | `axis4ProrateRounding` | `AXIS4_PRORATE_ROUNDING` | CEIL / ROUND / FLOOR / HALF_DAY |
| 4 | 회계연도 시작일(월) | `axis2FiscalStartMm` | `AXIS2_FISCAL_START_MM` | 01~12 |
| 4 | 회계연도 시작일(일) | `axis2FiscalStartDd` | `AXIS2_FISCAL_START_DD` | 01~31 |
| 5 | 근속 가산 모드 | `axis5TenureMode` | `AXIS5_TENURE_MODE` | LEGAL / CUSTOM |
| 5 | 가산 시작 연차(n) | `axis5StartYear` | `AXIS5_START_YEAR` | LEGAL=3 / CUSTOM 1~3 |
| 5 | 가산 주기(m) | `axis5Interval` | `AXIS5_INTERVAL` | LEGAL=2 / CUSTOM 1~2 |
| 5 | 최대 연차일수 | `axis5MaxDays` | `AXIS5_MAX_DAYS` | ≥25 |
| 6 | 연차 유효기간 | `axis6ValidityMonths` | `AXIS6_VALIDITY_MONTHS` | 12 / 24 |
| 7 | 사용촉진 | `axis7UsePromotion` | `AXIS7_USE_PROMOTION` | Y / N |
| (고정) | 프리셋 | `policyPreset` | `POLICY_PRESET` | 항상 `"CUSTOM"` 전송 (프리셋 제거) |
| (고정) | 일괄선부여 | `axis3PregrantYn` | `AXIS3_PREGRANT_YN` | 항상 `"N"` 전송 (통합 스펙 2번 옵션에 PREGRANT 없음) |
| 사용단위 | 1일 | `allowFullDay` (없으면 미전송, DB Y 고정) | `ALLOW_FULL_DAY` | 항상 Y |
| 사용단위 | 0.5일 | `allowHalfDay` | `ALLOW_HALF_DAY` | Y/N (HALF_DAY 시 Y 강제) |
| 사용단위 | 0.25일 | `allowQuarterDay` | `ALLOW_QUARTER_DAY` | Y/N |
| 사용단위 | 0.125일 | `allowHourly` | `ALLOW_HOURLY` | Y/N |
| 다중신청 | 같은날 건수 | `maxDailyRequest` | `MAX_DAILY_REQUEST` | 0(불허)/2/3 |
| 메타 | 적용 시작일 | `applyFromDate` | `APPLY_FROM_DATE` | YYYYMMDD |
| 메타 | 변경 사유 | `changeReason` | (HISTORY) | 자유 텍스트 |

> Boolean ↔ 'Y'/'N' 변환은 저장 직전 developer가 수행 (예: `allowHalfDay ? 'Y' : 'N'`).

---

## 5. 조건부 활성 규칙표 (§4.3 / §4.4 / §4.6)

| # | 조건 | 동작 | TC |
| --- | --- | --- | --- |
| R1 | `axis1GrantBase === 'HIRE_DATE'` | UI 2번 옵션 중 PRORATE·NEXT_YEAR_BULK `disabled`. 선택값이 비활성 옵션이면 MONTHLY_ONLY로 자동 보정 | TC-01, TC-03 |
| R2 | `axis1GrantBase === 'FISCAL_YEAR'` | UI 2번 3개 옵션 모두 활성 | TC-02 |
| R3 | `axis3FirstYearMethod !== 'PRORATE'` | UI 3번(반올림) 카드에 "조건부 활성" 배지 + 전체 옵션 disabled. 저장 시 `axis4ProrateRounding='CEIL'`로 정규화(백엔드도 강제) | TC-04 |
| R4 | `axis1GrantBase !== 'FISCAL_YEAR'` | UI 4번(회계연도 시작일) 입력 disabled + "조건부 활성" 배지. 저장 시 axis2 필드 미전송(백엔드 NULL 정규화) | TC-05 |
| R5 | UI 3번 = `HALF_DAY` 선택 | 사용단위 0.5일(`allowHalfDay`) 자동 체크 + disabled(해제 불가). 저장 시 'Y' 강제(백엔드도 강제) | TC-06 |
| R6 | `axis5TenureMode === 'LEGAL'` | n=3, m=2 고정 + 입력 disabled, max만 입력 가능 | TC-07 |
| R7 | `axis5TenureMode === 'CUSTOM'` | n(1~3), m(1~2) 입력 활성, max(25~40) | TC-08, TC-09 |

> 비활성(disabled) 스타일: `form.css`의 `input:disabled`/`select:disabled` 토큰 활용 + 옵션 카드는 `.option.disabled` 로컬 클래스(토큰 색).

---

## 6. 검증 규칙 (프론트 1차 — §4.12 대응, 최종 권위는 백엔드)

| 항목 | 프론트 체크 | 위반 시 |
| --- | --- | --- |
| 매트릭스 | axis1=HIRE_DATE면 axis3는 MONTHLY_ONLY만 (UI에서 비활성으로 강제) | 저장 차단 + alert |
| max_days | `axis5MaxDays >= 25` | 저장 차단 + alert |
| CUSTOM 범위 | `1<=startYear<=3`, `1<=interval<=2` | 저장 차단 + alert |
| 유효기간 | `axis6ValidityMonths >= 12` (12/24만 선택 가능하므로 UI상 자동 충족) | — |
| 회계연도 일 | axis1=FISCAL_YEAR면 mm 1~12, dd 1~31 (월별 최대일은 백엔드 위임) | 저장 차단 + alert |
| applyFromDate | 필수, YYYYMMDD 8자리 | 저장 차단 + alert |

> 프론트 검증은 UX용 1차 게이트. 매트릭스/법정 한도 최종 거부는 백엔드 `validateAxisMatrix`가 `ATTD_400_020` / `ATTD_403_011`로 처리. 백엔드 에러는 `resolveApiErrorMessage`로 표시.

---

## 7. API 연동 표

> ⭐ 통합 스펙 §4.11의 `/api/leave-policy/*`는 예시. **실제 엔드포인트는 `/baim07/policy/*`** (가드레일 2).
> 부여 시점 미리보기는 **클라이언트 계산** — 신규 엔드포인트 호출 금지(가드레일 3).

| 동작 | 메서드/엔드포인트 | 요청 | 응답 | 권한 |
| --- | --- | --- | --- | --- |
| 활성 정책 조회 | `GET /baim07/policy/active` | (헤더 인증) | `{ policy: LeavePolicyVO }` (없으면 policy=null → 기본값 화면) | 인증+사업장 |
| 정책 생성 | `POST /baim07/policy` | `LeavePolicySaveRequest` | `LeavePolicySaveResponse` | MASTER/HR_MANAGER |
| 정책 변경 | `PUT /baim07/policy/{policySeq}` | `LeavePolicySaveRequest` | `LeavePolicySaveResponse` | MASTER/HR_MANAGER |
| 변경 이력 | `GET /baim07/policy/history` | 페이징 파라미터 | `LeavePolicyHistoryListResponse` | 인증+사업장 |
| 영향 미리보기 | `POST /baim07/policy/impact-preview` | `LeavePolicySaveRequest` | `ImpactPreviewResponse` | MASTER/HR_MANAGER |

### 7.1 저장 분기 (developer)

- `policySeq`가 있으면(조회로 활성 정책 로드됨) → `PUT /baim07/policy/{policySeq}`
- 없으면(신규) → `POST /baim07/policy`
- 두 경우 모두 동일한 `LeavePolicySaveRequest` body 사용.

### 7.2 요청 body 구성 시 정규화 (developer, §5 규칙 반영)

- `policyPreset: "CUSTOM"` 고정 전송
- `axis3PregrantYn: "N"` 고정 전송
- axis1≠FISCAL_YEAR → `axis2FiscalStartMm/Dd` 미전송(또는 빈값, 백엔드 NULL 정규화)
- axis3(UI2)≠PRORATE → `axis4ProrateRounding` 값 무시(백엔드 'CEIL' 강제)이지만 프론트는 'CEIL' 전송 권장
- UI3=HALF_DAY → `allowHalfDay: 'Y'` 강제
- Boolean → 'Y'/'N' 변환

---

## 8. 부여 시점 미리보기 팝업 계산 로직 요약 (§4.8 / HTML 기준)

> 컴포넌트: `src/views/baim/popup/LeavePolicyPreviewPop.vue` (골격 존재, script 미완 — developer가 채움).
> useModal `open(LeavePolicyPreviewPop, { axis1GrantBase, fiscalStartMm, fiscalStartDd, prorateRounding, hireDate })`로 호출.

- 기준 입사일: 기본 `2025-07-15` (props로 주입, 시안 §4.8.2 고정값).
- 4개 정책 행을 시간순 컬럼(입사일 → 첫해 월별 → 차년/회계연도 시점)으로 표시:
  1. 입사일 기준 - 월차만: 입사 다음달부터 매월 월차 1, 입사 1주년(다음해 동월)에 본연차 15
  2. 회계연도 기준 - 월차만: 매월 월차 1, 차년도 회계연도 시작일에 본연차 15
  3. 회계연도 기준 - 비례: 회계연도 시작일에 비례 본연차(잔여기간/12 × 15, `prorateRounding` 적용 ≈ 7), 이후 회계연도마다 15
  4. 회계연도 기준 - 차년도 일괄: 차년도 회계연도 시작일에 본연차 15 일괄
- 셀 클래스: `hire`(입사일) / `event`(본연차·비례, 노란 강조) / `monthly`(월차, 흐림) / `''`(빈칸)
- 표 `min-width: 900px`, 가로 스크롤(`.preview-table-wrap { overflow-x:auto }`)
- 하단 요약 2줄: 회사 부담 비교(1년차 누적 순위), 첫 본연차 부여 시점 정책별 비교
- 닫기: X 버튼 / 배경 클릭 / [닫기] 버튼 → `$emit('close')` (useModal closeCallback)

> developer 작업: HTML 시안 `<script>`의 `updatePreview` 로직(및 4정책 시간축 표 구성)을 Vue computed로 이식. 회계연도 시작일이 1/1이 아닐 때 헤더 시간축 동적 재계산은 TODO로 표기되어 있음.

---

## 9. 상태별 동작

| 상태 | UI |
| --- | --- |
| loading | 조회/저장 중 헤더 버튼 disabled, (선택) `LoadingSpinner` 노출 |
| empty (활성 정책 없음) | `GET active`가 policy=null → 모든 axis 기본값으로 표시(신규 작성 모드, policySeq=null) |
| error | `resolveApiErrorMessage(err, fallback)` → `proxy.$alert(msg)` |
| success | 저장 성공 시 `proxy.$alert(getMessage(MSG.SAVE_SUCCESS))` 후 재조회 |

---

## 10. 테스트 케이스 (§4.13 대응 — 화면 관점)

| TC | 시나리오 | 화면 기대 |
| --- | --- | --- |
| TC-01 | axis1=HIRE_DATE 선택 | UI2의 PRORATE·NEXT_YEAR_BULK disabled, MONTHLY_ONLY만 선택 가능 |
| TC-02 | axis1=FISCAL_YEAR 선택 | UI2 3개 옵션 모두 활성 |
| TC-03 | HIRE_DATE+PRORATE 강제 후 저장 | UI에서 선택 불가(차단). 강제 전송 시 백엔드 400 → alert |
| TC-04 | UI2≠PRORATE | UI3 카드 "조건부 활성" 배지 + 옵션 disabled |
| TC-05 | axis1≠FISCAL_YEAR | UI4(회계연도 시작일) 입력 disabled + 배지 |
| TC-06 | UI3=HALF_DAY 선택 | 사용단위 0.5일 자동 체크 + disabled |
| TC-07 | 5번 LEGAL | n=3·m=2 고정 disabled, max만 입력 가능, 미리보기 "법정 기준" |
| TC-08 | 5번 CUSTOM n=5 저장 | 프론트 입력 max=3 제한(input max). 강제 시 백엔드 400 |
| TC-09 | 5번 CUSTOM m=3 저장 | 프론트 input max=2 제한. 강제 시 백엔드 400 |
| TC-10 | max_days=20 저장 | 프론트 input min=25 + 검증 차단 |
| TC-11 | 5번 미리보기 n=1,m=1,max=25 | 실시간 미리보기 1년차 16일, 11년차 25일(최대) |
| TC-12 | [부여 시점 미리보기] 클릭 | 팝업 오픈 + 4정책 시간순 표 |
| TC-16 | 활성 정책 존재 시 저장 | PUT으로 분기, 성공 후 재조회 시 갱신 반영 |

---

## 11. 반응형

- 데스크탑(관리자 백오피스) 우선. axis 카드 1열 세로 스택(시안과 동일).
- 사용단위 정책 좌우 2단(`usage-form-grid`)은 1024px 이하에서 1열로(모달가이드 break point 관례 따름). 단, 본 화면은 모달이 아닌 본문이므로 로컬 `@media (max-width: 768px)`로 1열 처리.
- 미리보기 팝업 표는 가로 스크롤로 좁은 화면 대응.

---

## 12. developer 인계 노트 (script 영역)

- `Baim_06.vue` 패턴: `getCurrentInstance().proxy`로 `$alert`/`$confirm`/`$util`, `useModal().open`으로 팝업, `axios`(`@/api/axios`), `getMessage(MSG.*)`, `resolveApiErrorMessage`.
- `onMounted`에서 `fnSearch()`(GET active) 호출 → 응답을 reactive 상태에 매핑.
- `defineOptions({ name: "Baim_07" })` 필수(viewResolver 키 매칭).
- 헤더 버튼 활성화: `localButtons`에서 `search:'Y'`, `save:'Y'` 노출, `create/delete/excel:'N'`.
- 변경 사유 입력: 저장 직전 `ReasonInputModal` 활용 권장(기존 공통 모달).
- 골격에 모든 마크업/스타일/상태선언/메서드 시그니처 + `// TODO(developer):` 작성 완료.
