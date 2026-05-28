# 입사일 변경 처리방식 × 연차 부여 정책 연계 가이드

> 대상 화면: 사용자관리(User_01) → 사용자정보(UserInfoPop) → **입사일 수정(HireDateEditPop)** + 사용자 연차관리(**Attd_09**) "정책 기준 부여"
> 작성 근거: 정책서 `.claude/context/policies/attd/08-leave.md` §8.5.6 / §8.5.8 + 실제 구현(prafta-022/023/030) 코드
> 최종 갱신: 2026-05-27 (prafta-032: **처리방식(SYS039) 자동계산 폐기** → 관리자 수동 법정연차 조정으로 전환. 데드코드 물리 삭제 009/010 반영)

> ## ⚠️ prafta-032 변경 안내 (이 문서를 읽기 전에 반드시 확인)
>
> **입사일 변경 "처리 방식(SYS039: KEEP_AND_BACKFILL / KEEP_AND_APPLY_NEW / RESET_ALL)" 자동계산은 prafta-032로 폐기됐다.**
> 부여 엔진(`LeaveGrantEngineServiceImpl`)의 처리방식 분기·차액보전(`computeBackfillShortfall` 부여 경로)·RESET_ALL 취소+재발급 코드는
> prafta-032(009)에서 **물리 삭제**됐다. 따라서 아래 2~6장의 "처리 방식 3종"·"케이스별 매트릭스"·"처리 방식별 동작 상세" 설명은
> **역사적 기록(폐기됨)** 으로만 남긴다. 현재 동작은 다음과 같다.
>
> 1. **입사일 변경 화면(HireDateEditPop)**: 처리방식 라디오를 제거하고, 현재 연차 상태(법정/약정 부여·사용·잔여)를 표시한다.
>    관리자가 **목표 법정 부여량**을 직접 입력하면, (목표 − 현재 ACTIVE 법정 부여)의 **차액**을 추가/회수한다.
>    - 차액 > 0: 새 입사일 기준 미부여 발생일(오늘 이전)에 소급 부여 + 잔여는 오늘 폴백(GRANT_TYPE 자동판단, 멱등키 `_HD{histId}`).
>    - 차액 < 0: A안 직접 차감(소멸임박→최근부여→GRANT_ID큰순). 회수가능량(ACTIVE 법정 잔여) 초과 시 차단. 회수 사유 필수.
>    - 차액 = 0 또는 목표 미입력: 무처리.
>    - `TB_USER_HIRE_DATE_HISTORY.HANDLING_TYPE`는 신규분 `'MANUAL'` 고정(기존 KEEP_*/RESET_ALL 이력 값은 감사 보존).
> 2. **Attd_09 "정책 기준 부여"**: 단일 동작 = **기존 부여가 있으면 변경 없음(멱등 skip), 없으면 정책·입사일·경력인정 기준으로 신규 부여**
>    (월차 D2-B 만1년 일괄소멸 게이트 유지). 신규 입사자/일괄 부여 대상에만 부여가 들어가고, 기존 부여자는 영향 없음(멱등).
>    `PolicyGrantPreviewPop`은 "재발급" 컬럼/카운트와 "처리방식" 컬럼을 제거하고 **신규 부여 / 변경 없음**만 표시한다.
> 3. **영향분석(HireDateEditPop)**: "누락된 부여 / 다음 부여 예정 시점 / FISCAL 다음 회계연도 발생예정"은 유지된다
>    (`estimateBackfillDays` 차액 산식·`fiscalNextGrantText`는 살아있는 코드). 옵션별 재할당 시뮬 미리보기(`previewReallocationOptions`)는 폐기됐다.
> 4. **약정(법정 외, MANUAL_*) 휴가**: 본 경로의 자동 조정 대상이 아니다. 약정 변경은 기존 Attd_09 수동부여(`manualGrant`) 경로로만 대응한다.
>
> 단일 출처: `.claude/requests/prafta-032-decisions.md`(D1~D8). 아래 본문의 처리방식 설명은 폐기 전 동작 기록이다.

---

이 문서는 (역사적 기록) 입사일 수정 팝업의 **처리 방식(SYS039)** 선택이 연차 부여 정책(PRAFTA-018 7-axis)과 어떻게 맞물려 동작했는지를 케이스별로 정리한다. **처리방식 자동계산은 prafta-032로 폐기됐다**(위 안내 참조). 현재 동작이 아니라 폐기 전 동작의 기록이다.

---

## 0. 한눈에 요약 (TL;DR)

> ⚠️ **prafta-032 폐기**: 아래 처리방식(SYS039) 관련 요약은 폐기 전(prafta-030) 동작이다. 현재는 처리방식 자동계산 없이
> 관리자가 입사일 변경 화면에서 목표 법정 부여량을 직접 입력해 차액을 추가/회수한다(문서 상단 "prafta-032 변경 안내" 참조).

- 입사일 변경은 **2단계**다: ① 입사일 수정 팝업에서 `HIRE_DATE` 갱신 + 이력 저장(연차 미반영) → ② Attd_09 "정책 기준 부여" 버튼으로 실제 부여 반영.
- 버튼이 실행할 **처리 방식은 "그 직원의 최신 미적용(APPLIED_YN='N') 입사일 변경 이력"의 `HANDLING_TYPE`** 에서 가져온다. 미적용 이력이 없으면 기본값 `KEEP_AND_APPLY_NEW`.
- **`KEEP_AND_BACKFILL`(옵션1)은 prafta-030부터 "차액 보전 단건"으로 동작한다.** 기존 컴포넌트를 INSERT/교체하지 않고, **새 기준 본연차+가산 누적 − 기존 누적**(소멸 제외·사용 포함·월차 포함)을 계산해 그 차액(>0일 때만)을 `STATUTORY_ANNUAL` **단건**으로 추가 부여한다. 과거 변경으로 근속이 늘면(예: 3년차→5년차 가산 +1, 월차→본연차 경계 +8) 그만큼 반영된다. 차액 ≤ 0(미래 변경·근속 감소)이면 추가 없음(옵션2와 동일, 절대 줄이지 않음 §8.5.8).
- **`KEEP_AND_APPLY_NEW`(옵션2)는 당기·소급 추가가 없다.** 기존 부여를 유지하고 `HIRE_DATE`만 갱신하며, 다음 부여 시점부터 새 입사일 기준으로 계산한다.
- **체계 전환(월차↔본연차)·일수 하향 정정은 여전히 `RESET_ALL`(옵션3)** 을 써야 한다. 기존 법정 부여를 `STATUS='CANCELED'`로 소프트 취소하고, 새 입사일 기준으로 전체 재발급한다. (옵션1 차액 보전은 "추가만" 하므로 하향/체계 재정렬은 못 한다.)
- **월차는 더블딥 게이트로만 차단된다.** "경력인정 고용승계 더블딥"(실근속<12 AND 산정근속≥12 AND full 본연차15 발생)에서만 미발생하고, 정상 근로자·FISCAL 비례부여 등은 법정 월차(§8.5.4)를 보존한다.
- 어떤 처리 방식에서도 **`tb_user_leave_use` 사용 이력은 절대 삭제하지 않는다**(기부여 보호, §8.5.8).

---

## 1. 부여가 발생하는 경로 (현재 구현 기준)

| 경로 | 트리거 | 엔진/서비스 |
| --- | --- | --- |
| 정책 기준 부여 | Attd_09 "정책 기준 부여" 버튼(선택 직원) | `LeaveGrantEngineServiceImpl.hireDateGrant` |
| 수동 부여 | Attd_09 "일괄 수동 부여" / 상세팝업 수동부여 | `LeaveDashboardServiceImpl.manualGrant` |
| 만료 전이(부여 아님) | 매일 00:05 배치 | `LeaveStatusScheduler` → `ACTIVE`→`EXPIRED` |

> ⚠️ **자동 "부여" 일배치는 현재 구현되어 있지 않다.** 정책서 §8.5.4가 언급하는 "일배치 자동 부여"는 설계 의도이며, 실제 코드의 스케줄러(`LeaveStatusScheduler`)는 **만료 전이만** 수행한다. 즉 법정 연차가 직원에게 들어가려면 누군가 Attd_09에서 "정책 기준 부여"를 눌러야 한다.

---

## 2. 핵심 개념

### 2.1 처리 방식 3종 (SYS039) — ⚠️ prafta-032로 폐기됨 (역사적 기록)

> 이 절(2.1)과 이하 처리방식 관련 설명은 **prafta-032에서 폐기**됐다. 처리방식 라디오는 화면에서 제거됐고, 부여 엔진의 처리방식 분기는
> 물리 삭제됐다. 신규 입사일 변경 이력의 `HANDLING_TYPE`은 `'MANUAL'` 고정이다. 아래 내용은 폐기 전 동작 기록으로만 보존한다.

입사일 수정 팝업 "3. 처리 방식"에서 고르며, 선택값은 `TB_USER_HIRE_DATE_HISTORY.HANDLING_TYPE`에 저장된다.

| 코드 | 화면 라벨 | 정책서 의도(§8.5.6) |
| --- | --- | --- |
| `KEEP_AND_BACKFILL` | 기존 부여 유지 + 부족분 차액 보전 (권장) | 기존 GRANT 유지 + (새 기준 누적 − 기존 누적)의 차액만 단건 보전 부여 |
| `KEEP_AND_APPLY_NEW` | 기존 부여 유지 + 신규 부여만 변경된 입사일로 계산 | 기존 GRANT 유지 + 다음 부여 시점부터 새 입사일 기준 |
| `RESET_ALL` | 모든 부여 삭제 후 재계산 (위험) | 기존 `STATUTORY_*` GRANT를 `CANCELED` 소프트 처리 + 전체 재발급 |

> 🔎 **구현 주의 (prafta-030)**: 현재 엔진(`buildUserPlan`)은 처리 방식을 `isBackfill`(옵션1) / `isReset`(옵션3) / 그 외(옵션2)로 분기한다.
> - **`KEEP_AND_BACKFILL`(옵션1)**: 당기 ANNUAL/TENURE 컴포넌트 부여와 컴포넌트 백필(`computeBackfillPeriods`)을 **모두 끄고**(이중부여 차단), `computeBackfillShortfall`이 산정한 **차액 단건**(`STATUTORY_ANNUAL`)만 부여한다(`hireDateGrant` (c-1b)). 멱등키 접미사는 보전 전용 `_BF{histId}`.
> - **`KEEP_AND_APPLY_NEW`(옵션2)**: 당기·소급 추가 없음(기존 GRANT 유지, `HIRE_DATE`만 갱신).
> - **`RESET_ALL`(옵션3)**: 기존 STATUTORY 소프트 취소 + 새 입사일 기준 전체 재발급(회차키 `_R{histId}`).
> - **이전 안내("KEEP 두 종 동작 동일 — 누락 컴포넌트만 INSERT")는 prafta-030 이후 stale이다.** 옵션1은 차액 보전, 옵션2는 추가 없음으로 분기한다.
>
> `HIRE_DATE` 컬럼 갱신은 처리 방식과 무관하게 ① 단계에서 항상 일어난다.

### 2.2 멱등키 — "이미 줬는지" 판정 기준

부여 중복을 막는 키. **실제 엔진 구현**(`LeaveGrantEngineServiceImpl.buildIdempotencyKey`, `:588`):

```
멱등키 = {userCd}_{yearLabel}_{grantType}_HIRE{keySuffix}
```

- `yearLabel` = **부여 실행 시점**의 달력연도(AXIS1=HIRE_DATE) 또는 현재 회계연도(AXIS1=FISCAL_YEAR). **입사일 값이 키에 들어가지 않는다.**
- `grantType` = `STATUTORY_ANNUAL` / `STATUTORY_MONTHLY` / `STATUTORY_TENURE_BONUS`
- `keySuffix` = `KEEP_*` → `""`(빈 값), `RESET_ALL` → `_R{HIST_ID}`(회차마다 유니크)
- 부여 실행 조건: `days > 0 AND countByIdempotencyKey == 0` 일 때만 INSERT. 이미 같은 키가 있으면 **건너뜀** → 노트 "이미 부여됨(멱등 — 변경 없음)".

> 📌 정책서 §8.5.8의 설계 키(`{USER_CD}_{YYYY}_ANNUAL`, `{USER_CD}_{YYYYMM}_MONTHLY`, `{USER_CD}_{YYYY}_TENURE_BONUS`)와 **실제 엔진 키는 형식이 다르다**. 특히 월차도 엔진에서는 `YYYYMM`이 아니라 **연(`YYYY`) 단위 1건으로 합산** 부여되고, `_HIRE` 접미사가 붙는다. 운영상 적용되는 것은 **엔진 키**다.

### 2.3 부여 컴포넌트와 entitlement 산정

`resolveEntitlement`(`:674`)가 직원 1명의 부여 컴포넌트를 산정한다.

- `actualMonths` = 입사일~오늘 만(滿) 개월수(경력 인정 제외)
- `creditedMonths` = `actualMonths` + 경력 인정 개월(`tb_user_service_credit`)

| 컴포넌트 | LEAVE_CD | GRANT_TYPE | 조건 | 일수 |
| --- | --- | --- | --- | --- |
| 월차 | `SYS_MONTHLY` | `STATUTORY_MONTHLY` | `0 < actualMonths < 12` | `min(actualMonths, 11)` |
| 본연차 | `SYS_ANNUAL` | `STATUTORY_ANNUAL` | (HIRE_DATE) `creditedMonths ≥ 12` / (FISCAL) 회계연도 시작 1회+ 도래 | `15` |
| 근속가산 | `SYS_TENURE_BONUS` | `STATUTORY_TENURE_BONUS` | 본연차 조건 충족 시 | `tenureBonusDays(정책, 근속연차)` (AXIS5) |

- 월차는 **실제 근속(creditMonths 미포함)** 기준 — 법정 의무(§8.5.4).
- 본연차/근속가산은 **경력 인정 가산 근속** 기준.
- **1년 경계가 핵심**: 입사일을 과거로 충분히 당겨 만 12개월을 넘기면, 월차(최대 11일)가 사라지고 **본연차 15일 + 근속가산** 체계로 전환된다.

##### 월차 더블딥 게이트 (prafta-030 / D2)

월차는 **실근속 기준**으로 발생하되, **"경력인정 고용승계 더블딥"인 경우에만** 발생하지 않는다(`computeMonthlyPeriods`가 빈 목록 반환, `isCreditDoubleDip` 판정). 세 조건을 모두 만족할 때만 차단한다.

1. 실근속 `actualMonths < 12` (실제 재직 1년 미만)
2. 경력 인정 포함 산정근속 `creditedMonths ≥ 12`
3. 이번 부여 entitlement에 **full 본연차(`STATUTORY_ANNUAL`, days ≥ 15)** 발생

좁힌 결과(월차 **보존**되는 정상 케이스):

- **정상 근로자(경력인정 0)**: 산정=실근속 → 조건(2) 거짓 → 월차 보존.
- **FISCAL 비례부여(crossed==1 PRORATE, 비례<15)**: full 15가 아님 → 조건(3) 거짓 → 월차 보존.
- **FISCAL 첫 부분기(crossed==0)**: 본연차 미발생 → 조건(3) 거짓 → 월차 보존(공백 방지).
- **오직 경력인정으로 실근속<1년인데 full 15를 받는 더블딥만 차단** → 중복 월차 제거.

> full 15일 때만 차단이 정당하다(본연차 15 ≥ 월차 상한 11 + 즉시 사용 가능 → "더 유리한 처우"라 §60② 위반 아님). 비례 7 등 < 11이면 차단 시 법정 미달이므로 차단하지 않는다. 이미 INSERT된 월차는 건드리지 않는다(미래 부여 산정만 게이트, §8.5.8 기부여보호).

##### 월차 소멸 = 만 1년 도래일 일괄 (prafta-030 / D2-B)

1년 미만 월차의 소멸(`AVAIL_TO_DATE`)은 **발생일별 유효기간(AXIS6)이 아니라 "만 1년 도래일에 일괄 소멸"**한다(근로기준법 §60⑦, 정답표 §1.4). 첫해 월차는 발생 시점과 무관하게 모두 **`hire.plusYears(1).minusDays(1)`(입사일 + 1년 − 1일 = 입사 1주년 직전)**에 소멸한다.

- 예: 2023-08-15 입사 → 첫해 월차 전부 2024-08-14 소멸.
- 발생일(`AVAIL_FROM` = 입사 + m개월)은 그대로. 소멸일만 모든 첫해 월차에 동일하게 만 1년 도래일을 부여(`computeMonthlyPeriods` 월차 한정).
- 만 1년 도래일이 오늘 이전인(만1년 경과) 직원은 첫해 월차가 전부 소멸(0). 1년 미만 직원은 발생분 유지.
- **AXIS6(발생일 + 유효개월)은 본연차·근속가산 유효기간 전용**이며 월차에는 적용하지 않는다.
- 입사일을 과거로 당겨 만 1년을 넘긴 경우(체계 전환), 첫해 월차는 만 1년 도래로 자연 소멸 대상이 된다. 옵션별 미리보기의 "예상 보유"에도 만1년 경과 직원의 잔존 월차가 0으로 반영된다(과다 보유 교정).

### 2.4 AXIS1(부여 기준) 연계

| AXIS1 | yearLabel | 본연차/근속가산 산정 |
| --- | --- | --- |
| `HIRE_DATE` | 현재 달력연도 `YYYY` | `creditedMonths ≥ 12` 이면 본연차 15 + 근속가산(`creditedMonths/12`) |
| `FISCAL_YEAR` | 현재 회계연도 시작연도 | 입사 후 회계연도 시작 도래 횟수(`crossedFiscalStarts`)로 산정. 첫 부분기간(0회)엔 본연차 미부여(월차만) |

- AXIS3=`PRORATE`(비례부여)는 prafta-023로 분리되어 현재 **`NEXT_YEAR_BULK`로 폴백**(비례계산·AXIS4 반올림 미구현). Attd_09에 "비례부여 미적용 · 차년도 일괄" 배지로 안내.
- 유효기간(`AVAIL_TO_DATE`)은 정책 AXIS6(12/24개월)에서 산정.

### 2.5 기부여 보호 / 사용이력 불변 (§8.5.8 절대 규칙)

- 시스템은 **기 발생 연차를 사후 차감하지 않는다.**
- `RESET_ALL`도 물리 삭제가 아니라 `STATUS='CANCELED'` **소프트 취소**(감사 추적 보존).
- `tb_user_leave_use`(사용 이력)는 **어떤 처리 방식에서도 삭제·변경하지 않는다.** 취소된 GRANT를 가리키더라도 데이터는 보존.

---

## 3. 처리 방식별 동작 상세 — ⚠️ prafta-032로 폐기됨 (역사적 기록)

> 본 3장 전체(3.1 KEEP_AND_BACKFILL/KEEP_AND_APPLY_NEW, 3.2 RESET_ALL)는 **prafta-032에서 폐기**됐다.
> 부여 엔진의 차액보전 부여 분기·RESET_ALL 취소+재발급 코드는 prafta-032(009)에서 물리 삭제됐다.
> 현재는 관리자 수동 목표 부여량 입력에 따른 차액 추가/회수만 동작한다(문서 상단 안내 참조). 아래는 폐기 전 동작 기록이다.

### 3.1 `KEEP_AND_BACKFILL`(옵션1, 차액 보전) / `KEEP_AND_APPLY_NEW`(옵션2)

#### `KEEP_AND_BACKFILL` — 차액 보전 단건 (prafta-030)

1. 당기 ANNUAL/TENURE 컴포넌트 부여와 컴포넌트 백필을 **끈다**(이중부여 차단).
2. `computeBackfillShortfall` = **(새 기준 본연차+가산 누적) − (기존 부여 누적)**.
   - 새 기준 누적(`computeNewBasisAnnualCumulative`): 현행 entitlement 당기 ANNUAL/TENURE + 유효기간 내 백필 후보(ANNUAL/TENURE)의 합(월차 제외, 멱등 skip 여부 무관한 "목표 누적").
   - 기존 누적(`selectStatutoryGrantAccrual`): live(`STATUS!='CANCELED' AND DEL_YN='N'`) 전 `STATUTORY_*`(월차 포함)에 대해 `USED_DAYS + (AVAIL_TO_DATE >= today ? GRANT_DAYS-USED_DAYS : 0)` 합 = **소멸 제외 + 사용 포함**.
3. 차액 > 0이면 그만큼 `STATUTORY_ANNUAL` **단건** 부여. 멱등키 접미사 `_BF{histId}`(표준키/리셋키와 충돌 금지), `GRANT_REASON = 입사일 변경 보전(INSADAY_CHANGE_BACKFILL)`, 소멸일(AVAIL_TO_DATE) = **최신 발생일 + AXIS6**(`computeBackfillAvailToDate`).
4. 차액 ≤ 0(미래 변경·근속 감소)이면 보전 없음 → 옵션2와 동일(절대 줄이지 않음).

→ 과거 변경으로 근속이 늘어난 만큼(가산 +1, 경계 월차→본연차 +8 등) **반영된다**. 월차(c-3)는 더블딥 게이트를 통과하면 별도 부여된다.

#### `KEEP_AND_APPLY_NEW` — 추가 없음

1. 당기·소급 추가를 하지 않는다(기존 GRANT 유지).
2. `HIRE_DATE`만 ① 단계에서 갱신되고, 다음 부여 시점부터 새 입사일 기준으로 계산된다.

> 옵션1/옵션2 공통: 신규 부여 또는 취소가 1건이라도 있으면 그 직원의 미적용 이력 전부 `APPLIED_YN='Y'` 마킹.

### 3.2 `RESET_ALL`

1. `selectActiveStatutoryGrantIds`로 활성 `STATUTORY_*` 부여 GRANT_ID 조회
2. 각각 `cancelGrant`로 `STATUS='CANCELED'` 소프트 취소
3. `keySuffix = "_R{HIST_ID}"`(회차 유니크) → 멱등키가 새 값이라 `countByIdempotencyKey == 0` → **새 입사일 기준 entitlement 전체 재발급**
4. 미적용 이력 `APPLIED_YN='Y'` 마킹

→ 입사일 정정 결과가 **즉시 부여에 반영**된다. 입사일 입력 오류 정정·근속 경계 변동 시 사용.

---

## 4. 케이스별 매트릭스 — ⚠️ prafta-032로 폐기됨 (역사적 기록)

> 본 4장의 처리방식별 매트릭스는 **prafta-032에서 폐기**됐다. 현재는 처리방식 선택이 없으므로 매트릭스가 적용되지 않는다.
> 입사일 변경 시 관리자가 목표 법정 부여량을 직접 입력하면 차액만 추가/회수한다. 아래는 폐기 전 동작 기록이다.

### 4.1 정책서 §8.5.6 권장 매트릭스 (4 시나리오 × 처리 옵션)

| 시나리오 | 현재 근속 | 입사일 방향 | 권장 처리 |
| --- | --- | --- | --- |
| A | 1년 미만 | 과거로 | `KEEP_AND_BACKFILL` |
| B | 1년 미만 | 미래로 | `KEEP_AND_APPLY_NEW` (사용 이력 있을 때) |
| C | 1년 초과 | 과거로 | `RESET_ALL`(입사일 정책) / `KEEP_AND_BACKFILL`(회계연도 정책) |
| D | 1년 초과 | 미래로 | `KEEP_AND_APPLY_NEW` (사용 이력 있을 때) |

### 4.2 실제 동작 보강 (엔진 기준 "그래서 연차가 바뀌나?", prafta-030 반영)

| 케이스 | 상황 | `KEEP_AND_BACKFILL`(옵션1) | `KEEP_AND_APPLY_NEW`(옵션2) | `RESET_ALL`(옵션3) |
| --- | --- | --- | --- | --- |
| 신규 입사자 첫 부여 | 현재연도 부여 이력 없음 | ✅ 차액 = 새 기준 누적 − 0 → 본연차분 보전(월차는 게이트 통과 시 별도) | ✅ 당기 컴포넌트 신규 INSERT | ✅ 동일하게 부여(취소 대상 없음) |
| 입사일 과거로(여전히 1년 미만) | 월차 일수만 늘어야 함 | ⚠️ 본연차 미발생 → 차액 0(보전 없음). 월차는 더블딥 게이트 미해당이라 누락분 per-월 추가 | ❌ 변화 없음 | ✅ 기존 월차 취소 + 늘어난 월차 재발급 |
| 입사일 과거로(1년 경계 넘김) | 월차 → 본연차로 전환돼야 함 | ✅ 차액 = 새 기준 15(+가산) − 기존 월차 누적 → 부족분(예 +8) 단건 보전. **기존 월차는 잔존**(감소 없음 — 체계 재정렬 아님) | ❌ 변화 없음 | ✅ 월차 취소 + 본연차 15(+근속가산) 재발급 (정합) |
| 입사일 과거로(여러 해 근속 증가) | 근속가산 늘어야 함 | ✅ 차액 = 늘어난 가산분(예 +1/+2) 단건 보전 | ❌ 변화 없음 | ✅ 전체 재발급 |
| 입사일 미래로 | 부여가 줄거나 0이 돼야 함 | ❌ 차액 ≤ 0 → 보전 없음(줄이지 않음) | ❌ 변화 없음(기존 부여 보호) | ✅ 기존 취소 + 새 입사일 기준 재발급(미래면 0건) |
| 이미 RESET 후 재클릭 | 멱등 재확인 | — | — | ✅/멱등: 같은 `_R{HIST_ID}` 키라 재발급 안 함(중복 방지). 새 변경이면 새 HIST_ID로 다시 RESET |

> 핵심: **`KEEP_AND_BACKFILL`(옵션1)은 근속 증가에 따른 부족분을 "추가만" 한다(차액 보전, 감소 없음).** 따라서 기존 월차/본연차를 줄이거나 취소하는 **체계 재정렬·일수 하향**은 못 한다 — 그 정정이 필요하면 `RESET_ALL`이 정답이다. `KEEP_AND_APPLY_NEW`(옵션2)는 추가도 하지 않는다.

### 4.3 워크드 예시 (오늘 = 2026-05-26, AXIS1=HIRE_DATE 가정)

- 현재: 입사일 2025-07-xx → 만 약 10개월 → 월차 컴포넌트 보유(누적 N일).
- 변경: 입사일 **2025-05-01** → 만 12개월 도달 → 새 기준 entitlement = **본연차 15일(+근속가산)**.
  - **`KEEP_AND_BACKFILL`(옵션1)** → 차액 = 새 기준 15(+가산) − 기존 누적(소멸 제외·사용 포함·월차 포함). 부족분만큼 `STATUTORY_ANNUAL` 단건(멱등키 `_BF{histId}`, 사유 `입사일 변경 보전(INSADAY_CHANGE_BACKFILL)`)으로 보전. **기존 월차는 잔존(감소 없음)** — 체계 전환이 필요하면 옵션3.
  - **`KEEP_AND_APPLY_NEW`(옵션2)** → 추가 없음(`HIRE_DATE`만 갱신, 다음 부여부터 새 기준).
  - **`RESET_ALL`(옵션3)** → 기존 월차 취소 + 본연차 15일 재발급. **정합한 결과.**

(실제 일수는 회사 정책 AXIS1·AXIS5·경력 인정에 따라 달라지므로, 적용 전 **프리뷰 모달** 또는 영향분석 GET `options[]`(옵션1/2/3 add/reclaim/finalHold)에서 반드시 확인.)

---

## 5. "이미 부여됨(멱등 — 변경 없음)"이 뜨는 조건 체크리스트

`buildPlanNote`(`:638`)가 이 문구를 반환하는 조건 = 신규 INSERT 컴포넌트가 하나도 없고 RESET도 아님. 원인 후보:

1. 현재 연도/회계연도분이 **이미 같은 멱등키로 부여됨** (가장 흔함)
2. 최신 미적용 처리방식이 **KEEP_*** (또는 미적용 이력이 없어 기본 KEEP_AND_APPLY_NEW) → 기존 부여 재계산 안 함
3. 입사일이 **미래**거나 근속 **0개월** → entitlement 컴포넌트 자체가 없음(노트는 각각 "입사일 미래 — 부여 대상 아님" / "부여 대상 없음(근속 0개월)")
4. 그 직원을 **체크 안 함** → 애초에 대상에서 제외

해결(prafta-032 기준): 부여를 실제로 바꾸려면 입사일 수정 팝업에서 **목표 법정 부여량을 직접 입력**해 차액을 추가/회수한다.
("정책 기준 부여"는 신규 입사자 신규 부여 전용으로, 기존 부여자에게는 변경 없음(멱등 skip)이다. 종전의 RESET_ALL/BACKFILL 처리방식 경로는 폐기됨.)

---

## 6. 실무 의사결정 가이드 (prafta-032 기준)

> 처리방식(SYS039) 선택은 폐기됐다. 현재는 입사일 변경 화면에서 **목표 법정 부여량**을 직접 입력해 차액을 추가/회수한다.

| 목적 | 현재(prafta-032) 권장 동작 |
| --- | --- |
| 신규 입사자에게 첫 법정 연차 부여 | Attd_09 "정책 기준 부여"(기존 부여 없는 직원만 신규 부여) |
| 과거 입사일로 근속이 늘어 부족분만 보전(추가) | 입사일 변경 화면에서 **목표 법정 부여량 상향 입력 → 차액 소급 추가**(미부여 발생일 + 오늘 폴백) |
| 입사일 오타/오류 정정 — 일수 하향 | 입사일 변경 화면에서 **목표 법정 부여량 하향 입력 → 차액 회수(A안)**. 회수 사유 필수, 회수가능량(ACTIVE 법정 잔여) 초과 시 차단 |
| 단순 경력 인정만 반영(입사일은 그대로) | 입사일 변경 아님 → **사용자정보 [경력 인정]** 사용 |
| 약정(법정 외, MANUAL_*) 휴가 조정 | 본 경로 대상 아님 → 기존 **Attd_09 수동부여(manualGrant)** 경로 |
| 이미 사용한 연차가 있어 회수가 부담스러움 | 회수가능량 = ACTIVE 법정 잔여(USED_DAYS·신청 진행분 제외)로 제한됨 — 사용/사용예정 분은 회수 불가(차단) |

> (폐기 기록) 종전에는 KEEP_AND_BACKFILL(차액 보전)·KEEP_AND_APPLY_NEW(추가 없음)·RESET_ALL(전체 재발급) 중 선택했다. prafta-032로 폐기.

> 입사일 변경은 4대보험·임금·퇴직금에도 영향을 준다. 단순 경력 인정이 목적이면 입사일을 건드리지 말 것(팝업 하단 경고와 동일).

---

## 7. 권한 / 감사

- 입사일 변경·정책 기준 부여·이력 조회 모두 **`AUTH_MASTER` 또는 `AUTH_HR_MANAGER`** 전용(`AuthRoleUtils.isManager`, 위반 시 `ApiException`). (§8.5.7)
- 모든 입사일 변경은 `TB_USER_HIRE_DATE_HISTORY`에 영구 보관(변경 전/후, 사유 필수, 영향 스냅샷 JSON, 처리방식, 적용여부/적용일시/적용자).
- 변경 이력은 입사일 수정 팝업의 **"변경 이력 보기"** → `HireDateHistoryPop`에서 확인(사유는 길어질 수 있어 "보기" 버튼 → 별도 팝업).

---

## 8. 검증 SQL (운영 DB 직접 연결 금지 — 로컬/개발만)

```sql
-- 1) 직원의 법정 부여 현황 + 멱등키 (대상: USER_CD. USER_ID 아님)
SELECT GRANT_ID, GRANT_TYPE, GRANT_DAYS, USED_DAYS, STATUS,
       AVAIL_FROM_DATE, AVAIL_TO_DATE, IDEMPOTENCY_KEY, GRANT_DATE
  FROM TB_USER_LEAVE_GRANT
 WHERE CMPNY_CD = :cmpny AND USER_CD = :userCd
   AND GRANT_TYPE LIKE 'STATUTORY\_%'
 ORDER BY GRANT_DATE DESC;

-- 2) 입사일 변경 이력 + 최신 미적용 처리방식 (버튼이 무슨 처리방식을 쓸지 결정)
SELECT HIST_ID, PREV_HIRE_DATE, NEW_HIRE_DATE, HANDLING_TYPE,
       APPLIED_YN, APPLIED_DATE, INSERT_DATE
  FROM TB_USER_HIRE_DATE_HISTORY
 WHERE CMPNY_CD = :cmpny AND USER_CD = :userCd
 ORDER BY INSERT_DATE DESC;
```

- 1번에 `..._{현재연도}_..._HIRE` 키가 이미 있으면 그게 KEEP_*에서 "이미 부여됨"의 정체.
- 2번 최신 행이 `KEEP_*`거나 미적용 이력이 없으면 → 버튼이 KEEP 동작 → 재계산 안 함.

---

## 9. 코드/정책 출처

| 내용 | 위치 |
| --- | --- |
| 처리 매트릭스 / 처리방식 정의 (폐기됨 — 역사적 기록) | `policies/attd/08-leave.md` §8.5.6 |
| 멱등성·기부여 보호·STATUS 4종 | `policies/attd/08-leave.md` §8.5.8 |
| 권한 매핑 | `policies/attd/08-leave.md` §8.5.7 |
| 부여 엔진(현재: 단일 신규부여 + 멱등키) | `LeaveGrantEngineServiceImpl.java` (`hireDateGrant` / `buildUserPlan` / `resolveEntitlement` / `grantComponent` / `buildIdempotencyKey`) — 처리방식 분기·RESET 경로는 prafta-032(009)로 물리 삭제됨 |
| 입사일 변경 수동 법정연차 조정 (prafta-032) | `LeaveGrantEngineServiceImpl.adjustStatutoryGrantsByHireDateChange`(차액>0 추가/차액<0 회수) · `LeaveGrantEngineMapper.xml`(selectRecallableStatutoryTotal / selectActiveStatutoryGrantedTotal / selectActiveStatutoryGrantsForRecall / cancelStatutoryGrantForHireChange / reduceStatutoryGrantDaysForHireChange) |
| 영향분석 누락부여 추정 / 월차 게이트 (살아있음) | `LeaveGrantEngineServiceImpl.java` (`estimateBackfillDays` / `computeBackfillShortfall` / `computeNewBasisAnnualCumulative` / `computeBackfillPeriods` / `computeMonthlyPeriods` / `isCreditDoubleDip` / `fiscalNextGrantText`) · `LeaveDashboardMapper.xml selectStatutoryGrantAccrual` |
| 입사일 변경 영향분석 GET | `User01Controller` `GET /{userCd}/hire-date-impact` / `User01ServiceImpl.buildApproxImpact` / `HireDateImpactResponse(existingGrant/used/missingGrant/nextGrant/fiscalNextGrantText)` — prafta-032로 옵션별 시뮬(options[]·previewReallocationOptions) 폐기 |
| 미적용 처리방식/취소대상 조회 SQL (엔진 데드, 테스트 스텁·감사용 잔존) | `LeaveGrantEngineMapper.xml`(selectLatestUnappliedHandling / selectActiveStatutoryGrantIds) |
| 입사일 변경 저장 + 이력 INSERT | `User01ServiceImpl.updateUserHireDate` / `User01Mapper.xml insertUserHireDateHistory` |
| 입사일 변경 이력 조회 | `User01ServiceImpl.selectHireDateHistory` / `User01Mapper.xml selectUserHireDateHistory` |
| 만료 배치(부여 아님) | `LeaveStatusScheduler.java` |

---

## 10. 알려진 한계 / 보완 여지 (prafta-023 진행 반영)

1. ~~KEEP 두 종 동일 동작~~ → **해소(prafta-023 C)**: `KEEP_AND_BACKFILL`=유효기간 내 과거 본연차·근속 소급 / `KEEP_AND_APPLY_NEW`=당기분만 → 실제로 분기됨.
2. ~~KEEP_*는 일수 변화 자동 보정 안 함 → RESET_ALL 권장~~ → **정정(prafta-030 / D1)**: `KEEP_AND_BACKFILL`은 **차액 보전**으로 근속 증가(가산/경계)를 반영한다(추가만 하며 감소는 없음). 단 **일수 하향·체계 전환(월차↔본연차) 정정은 여전히 `RESET_ALL`**(옵션1은 차액을 더하기만 할 뿐 기존 행을 줄이거나 취소하지 못함). 옵션별 미리보기는 영향분석 GET(`/{userCd}/hire-date-impact`)이 `options[]`(옵션1/2/3 각 add/reclaim/finalHold)+`fiscalNextGrantText`로 확장(`previewReallocationOptions`, read-only — 부여 안 함).
3. ~~AXIS3=PRORATE 미구현~~ → **해소(prafta-023 D)**: FISCAL 첫 부분기간 비례부여 `(잔여일수÷365)×15` + AXIS4 CEIL/ROUND/FLOOR 구현. 단 **AXIS4=HALF_DAY(0.5일)는 부여 일수 BigDecimal 확장 필요 → 임시 CEIL(후속)**.
4. ~~자동 부여 일배치 미구현~~ → **추가(prafta-023 E, 게이트)**: `LeaveGrantScheduler`(기본 비활성, `prafta.leave.auto-grant.enabled=true` 시 동작). 단 **월차 per-월 누적은 미구현**(엔진 당기 집계, B 잔여).
5. ~~멱등키 형식이 정책서와 다름(_HIRE)~~ → **(부분) 해소(prafta-023 A)**: 정식 키(`{userCd}_{period}_{grantType}`) 전환 + 레거시 `_HIRE` dual-read(마이그레이션 없이 이중부여 차단). 단 **월차 per-월 키(`{YYYYMM}`)·FISCAL_YEAR 과거 백필은 후속**.

> 후속 잔여: 월차 per-월 누적(B), AXIS1=FISCAL_YEAR 과거 백필, AXIS4=HALF_DAY 0.5일, EXPIRE_YN deprecation(H).
