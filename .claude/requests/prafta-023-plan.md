# prafta-023 연차 부여엔진 정밀화·자동화 + 입사일 처리방식 분기 확립 · 작업 분해 계획서 (마스터)

> prafta-023(umbrella, `prafta-023.md`)의 4개 후속 항목 + 사용자 논의로 추가된 **"C" = 입사일 수정 처리방식(KEEP_AND_BACKFILL / KEEP_AND_APPLY_NEW) 동작 분기 확립**을 하나로 묶어 단위 작업으로 분해한다.
> **본 문서가 prafta-023 단일 출처(supersede).** `prafta-023.md`는 배경/분리근거 umbrella로 유지하고, 착수 시 본 문서의 결정 로그·작업표·필요 DDL을 따른다.
> **선행 의존**: prafta-022에서 추출한 공용 부여 엔진 `LeaveGrantEngineService`를 **그대로 재사용·확장**한다(신규 엔진 만들지 말 것 — prafta-022 결정 #9 계승).
> 작성 근거: 정책서 `.claude/context/policies/attd/08-leave.md` §8.5.2~§8.5.8 + `.claude/context/hire-date-change-handling-guide.md` + prafta-022 코드 실측.
> 작성일: 2026-05-24

---

## 0. 배경 — 왜 "C"가 prafta-023에 합쳐지나

사용자 테스트 중 "입사일을 바꿔도 정책 기준 부여 시 연차가 안 바뀐다(이미 부여됨/멱등)"가 확인됐고, 원인 분석 결과:

- 현재 엔진(`buildUserPlan`)은 처리방식 중 **`RESET_ALL` 여부(`isReset`)만 분기**한다 → **`KEEP_AND_BACKFILL` ≡ `KEEP_AND_APPLY_NEW`** (둘 다 "당해연도 누락 컴포넌트만 INSERT", 기존 부여 일수 재계산 안 함).
- 멱등키가 **당해연도 단일 집계키**(`{userCd}_{현재연도}_{grantType}_HIRE`)라 **월차가 매월 누적되지 않고**, 과거 여러 해 소급도 불가.
- 자동 정기부여 배치가 없어 부여는 버튼/수동에만 의존.

이 셋(처리방식 분기 / 멱등키 정식화 / 배치)은 prafta-023 §1~§3과 **동일 토대(기간 모델 + 정식 멱등키)** 를 요구하므로 분리 진행이 비효율적이다. 따라서 합본한다.

> **직전 논의 결정 변경**: "두 KEEP이 동일하니 입사일 수정 화면에서 처리방식을 통합하자"(임시 A안)는 **철회**한다. 본 작업으로 두 KEEP이 실제로 달라지므로 SYS039 3종을 모두 유지한다.

---

## 1. 결정 로그

| # | 항목 | 결정 |
|---|---|---|
| 1 | 단일 출처 | 본 계획서가 prafta-023 분해의 supersede. `prafta-023.md`는 umbrella로 유지. |
| 2 | "C"의 정체 | handlingType 두 KEEP의 **동작 분기 확립**. `KEEP_AND_BACKFILL`=입사 이후 누락 기간 **전체 백필(다년/다월)**, `KEEP_AND_APPLY_NEW`=**당기(현재 기간)분만** 부여 + 이후는 스케줄러가 처리. → SYS039 3종 유지(UI 통합 철회). |
| 3 | 멱등키 정식 전환 (§8.5.8 #1) | 월차 `{USER_CD}_{YYYYMM}_MONTHLY` / 본연차 `{USER_CD}_{YYYY}_ANNUAL` / 근속가산 `{USER_CD}_{YYYY}_TENURE_BONUS`. prafta-022의 `_HIRE` 접미사 키는 **수동버튼 한정 임시키였음** → 폐기·전환. RESET_ALL은 회차키 접미사(`_R{HIST_ID}`) 유지. |
| 4 | ⚠️ 기존 부여 재키잉 (최대 리스크) | 키 형식만 바꾸면 신규 키 `countByIdempotencyKey==0`이 되어 **기존 부여를 이중 부여**한다. 전환과 동시에 **기존 STATUTORY 부여행의 IDEMPOTENCY_KEY를 신규 형식으로 재키잉하는 일회성 마이그레이션**(또는 전환기 이중조회)을 **반드시** 함께 수행. 데이터 영향 → §5 승인 대상. |
| 5 | 기간(period) 모델 | `resolveEntitlement`를 "현재 1회분 산정"에서 **"입사일~오늘 기간별(연/월) 산정"** 으로 확장. 이 모델이 다년도 백필·월 누적·비례부여의 공통 토대. 엔진 내부 확장(신규 엔진 금지). |
| 6 | 기부여 보호 (§8.5.8 #2/#5 절대) | 백필/비례/배치 어떤 경로도 **기 발생 연차를 사후 차감하지 않는다.** 추가(INSERT)만 한다. 축소·정정이 필요한 경우는 **RESET_ALL**(cancel→재발급)만 사용. `tb_user_leave_use` 사용이력 불변. |
| 7 | PRORATE + AXIS4 (§1) | 첫해 비례부여 `(입사~기준일 잔여일수 ÷ 365) × 본연차일수` + AXIS4 반올림 4종(CEIL/ROUND/FLOOR/HALF_DAY). **AXIS3=PRORATE일 때만 AXIS4 유효**, 그 외 분기는 AXIS4=CEIL 강제(§8.5.3). 반올림 경계는 단위테스트로 고정. |
| 8 | 자동 정기부여 배치 (§2) | `LeaveStatusScheduler`(만료배치, @Scheduled) 패턴으로 일배치 신설. 엔진 재사용. 멱등키로 중복 차단. **월차 매월 누적 / 본연차·근속 연 1회**. 버튼 경로와 동일 멱등키라 충돌 없음(결정 #3). |
| 9 | 적용 트리거/handlingType 출처 | prafta-022 결정 #1·#3 계승: 입사일 수정은 **기록만**, 부여는 Attd_09 버튼/배치에서. 버튼은 그 직원 **최신 미적용(APPLIED_YN='N') 이력의 HANDLING_TYPE** 적용(없으면 KEEP_AND_APPLY_NEW). |
| 10 | EXPIRE_YN deprecation (§4) | 독립성 높음. 본 묶음에서 **분리 가능**(작업 H, 후순위/옵션). |
| 11 | 영향분석/프리뷰 정합 | 다년도 백필·비례부여 결과가 HireDateEditPop 영향분석(`buildApproxImpact`)과 Attd_09 프리뷰(`previewPolicyGrant`)에 동일 산식으로 반영되도록 엔진 산정 함수를 **단일 진실원**으로 공유. |

---

## 2. 근본원인 → 보완 지점 매핑

| 현상 | 근본원인 (코드 실측) | 보완 작업 |
|---|---|---|
| 두 KEEP 동일 동작 | `buildUserPlan`이 `isReset`만 분기, 기간 모델 부재 | 작업 B(기간모델) + C(분기) |
| 입사일 바꿔도 변화 없음 | 당해연도 단일 멱등키 존재 → skip, KEEP은 일수 재계산 안 함 | 작업 A(키)+B+C |
| 월차 매월 미누적 | `{userCd}_{YYYY}_..._MONTHLY` 연단위 1건 + 배치 부재 | 작업 A(월별키)+E(배치) |
| 다년도 소급 불가 | `yearLabel = now().getYear()` 현재연도만 산정 | 작업 B(기간모델)+C(BACKFILL) |
| PRORATE 미동작 | `NEXT_YEAR_BULK` 폴백(`:457` INFO 로그) | 작업 D(비례+반올림) |
| 부여 자동화 없음 | 스케줄러는 만료 전이만(`LeaveStatusScheduler`) | 작업 E(부여 배치) |

---

## 3. 작업 분해 · 의존성 · 순서

| 코드 | 작업 | 핵심 산출물(파일 수준) | 의존 |
|---|---|---|---|
| **A** | **멱등키 정식 전환 + 기존 부여 재키잉 마이그레이션.** 엔진 키 생성기를 §8.5.8 형식으로 교체(월별/연별 분기). 기존 `_HIRE` 키 부여행을 신규 키로 재키잉하는 일회성 스크립트(또는 전환기 이중조회 가드). | (수정) `LeaveGrantEngineServiceImpl.buildIdempotencyKey`/`grantComponent`<br>(신규) `sql/migration/prafta-023-rekey-grant-idempotency.sql` | 독립(우선), §5 승인 |
| **B** | **기간(period) 모델 도입.** `resolveEntitlement`를 입사일~기준일 기간별(연/월) entitlement 리스트 산정으로 확장. AXIS1(HIRE_DATE/FISCAL_YEAR)·경력인정 가산 유지. | (수정) `LeaveGrantEngineServiceImpl` entitlement 산정부(`:402~493`) | **A** |
| **C** | **handlingType 동작 분기 확립.** BACKFILL=누락 기간 전체 INSERT(다년/다월), APPLY_NEW=당기분만, RESET_ALL=취소→전체 재발급(회차키). 기부여 차감 금지 가드. | (수정) `buildUserPlan`/`hireDateGrant`(분기 로직) | **A·B** |
| **D** | **AXIS3=PRORATE 정밀 비례부여 + AXIS4 반올림 풀 매트릭스.** 비례식+반올림 4종 유틸, AXIS3≠PRORATE면 CEIL 강제. 경계 단위테스트. | (수정) 엔진 첫해 산정부 + (신규) 비례/반올림 유틸 + 테스트 | **B** |
| **E** | **자동 정기부여 스케줄러.** 일배치가 활성 정책 기준 대상 직원에게 월차(매월)/본연차·근속(연1회) 멱등 부여. 엔진 재사용. | (신규) `common/schedule/leave/scheduler/LeaveGrantScheduler.java`<br>(수정) 엔진에 배치 진입 메서드 | **A·B·C·D** |
| **F** | **프리뷰/영향분석 정합.** Attd_09 프리뷰(`previewPolicyGrant`)·HireDateEditPop 영향분석(`buildApproxImpact`)이 다년도 백필·비례 결과를 동일 산식으로 표시. | (수정) `LeaveGrantEngineServiceImpl.previewPolicyGrant`, `User01ServiceImpl.buildApproxImpact` | **C·D** |
| **G** | **프론트/문서 정합.** HireDateEditPop 처리방식 설명 갱신(이제 실제로 다름), Attd_09 프리뷰 모달이 "백필 N기간 / 비례 X일" 표기, 가이드 문서 §10 한계 항목 갱신. | (수정) `HireDateEditPop.vue`, `PolicyGrantPreviewPop.vue`, `.claude/context/hire-date-change-handling-guide.md` | **F** |
| **H** | (옵션·분리가능) **EXPIRE_YN deprecation 동기 로직/트리거** 검토. | (수정) 상태 동기 서비스/트리거 | 독립 |

**권장 순서**: `A`(키+마이그레이션) → `B`(기간모델) → `C`(분기) → `D`(비례) → `E`(배치) → `F`(프리뷰/영향) → `G`(프론트/문서). `H`는 독립·후순위.
- `A`가 모든 것의 토대이자 최대 리스크(이중부여) → 마이그레이션 검증 후 선행.
- `E`(배치)는 부여 정확도(B·C·D) 확정 후. 잘못된 산정을 자동화하면 대량 오부여 위험.

---

## 4. 재사용 블록 (신규 작성 금지, 그대로 호출/확장)

| 블록 | 위치 | 용도 |
|---|---|---|
| `LeaveGrantEngineService`(전체) | `common/cmm/leave/service` | 부여 단일 엔진 — 버튼/배치/프리뷰 공유(확장만, 신설 금지) |
| `cancelGrant(...)` | `LeaveGrantStatusServiceImpl` | RESET_ALL 소프트 취소(CANCELED, 사용이력 보존) |
| `tenureBonusDays(policy, year)` | `LeaveGrantEngineServiceImpl` | AXIS5 근속가산 |
| `findActivePolicy(cmpnyCd)` | `LeavePolicyService` | 활성 7-axis 정책 VO |
| `markHireDateHistoryApplied` / `selectLatestUnappliedHandling` / `selectActiveStatutoryGrantIds` | `LeaveGrantEngineMapper` | 적용 마킹 / 최신 미적용 처리방식 / 취소대상 |
| `@Scheduled` 배치 패턴 | `common/schedule/leave/scheduler/LeaveStatusScheduler` | 작업 E 스케줄러 골격 참조 |
| `countByIdempotencyKey` | `LeaveDashboardMapper` | 멱등 중복 차단 |

---

## 5. 필요 DDL / 마이그레이션 (사용자 승인 대상 — 임의 적용 금지)

> prafta-022에서 `TB_USER_HIRE_DATE_HISTORY`에 APPLIED_* 컬럼은 이미 추가됨. 본 작업의 핵심은 **스키마 변경이 아니라 데이터(멱등키) 마이그레이션**이다.

### M1. 기존 STATUTORY 부여 IDEMPOTENCY_KEY 재키잉 (작업 A 필수, 최대 리스크)
- 목적: 키 형식 전환 시 기존 부여를 신규 키로 인식시켜 **이중부여 차단**.
- 대상: `tb_user_leave_grant` 중 `GRANT_TYPE LIKE 'STATUTORY\_%' AND DEL_YN='N'` 의 `IDEMPOTENCY_KEY`.
- 매핑(안): `{userCd}_{YYYY}_{grantType}_HIRE` → `{USER_CD}_{YYYY}_ANNUAL` / `_TENURE_BONUS`, 월차 기존 단일키는 **기존분 보존 + 신규 월차부터 `{YYYYMM}_MONTHLY`** 로 운영(과거 월차 재분할 여부는 오픈이슈 #1).
- 안전장치: (a) 백업 후 실행, (b) `UNIQUE(CMPNY_CD, IDEMPOTENCY_KEY)` 충돌 사전 점검 쿼리, (c) 롤백 스크립트 동봉, (d) 우선 dev에서 건수 검증.
- 대안(전환기 무중단): 엔진이 **신규키 + 레거시 `_HIRE`키를 둘 다 조회**해 존재하면 skip → 재키잉 없이 점진 전환. 운영 리스크 낮으나 코드 복잡. → A 착수 시 둘 중 택1 결정.

> 신규 테이블/컬럼은 원칙적으로 없음. (감사 편의를 위해 `tb_user_leave_grant`에 부여 대상기간 `PERIOD_YM` 컬럼 추가는 **선택**, 오픈이슈 #2)

---

## 6. 프론트 / 문서 수정 지점

| 지점 | 변경 |
|---|---|
| `HireDateEditPop.vue` 처리방식 옵션 설명 | "BACKFILL=누락분 소급" / "APPLY_NEW=당기만, 이후 자동" 문구를 **실제 동작에 맞게** 갱신(이제 다름). UI 통합 안 함. |
| `PolicyGrantPreviewPop.vue` 프리뷰 | 직원별 "백필 N기간(연/월) · 비례 X일 · 취소 Y건 · 추가 Z일" 집계 표기 추가. |
| `Attd_09.vue` | 정책 기준 부여 결과 alert에 백필/비례 요약 반영(엔드포인트 응답 확장 시). |
| `.claude/context/hire-date-change-handling-guide.md` | §10 한계 항목(① KEEP 동일 ③ PRORATE 미구현 ④ 배치 없음 ⑤ 키 불일치) **해소 반영**, 케이스 매트릭스 §4.2 갱신. |

---

## 7. 테스트 매트릭스 (경계 고정 — 단위테스트 필수)

| 그룹 | 케이스 |
|---|---|
| 비례+반올림 | AXIS3=PRORATE × AXIS4 {CEIL/ROUND/FLOOR/HALF_DAY} × 잔여일수 경계(예: 0.4/0.5/0.6일분), AXIS3≠PRORATE면 CEIL 강제 확인 |
| 다년도 백필 | 입사일 3년 전으로 정정 → 누락 연도별 본연차 15+근속가산 백필(연도별 멱등키), 재실행 시 멱등 |
| 월 누적 | 1년 미만 직원, 배치 N개월 반복 실행 → `{YYYYMM}` 키로 월별 1일씩 누적(최대 11), 같은 달 재실행 멱등 |
| 회계연도 | AXIS1=FISCAL_YEAR × AXIS2(시작 MM/DD) × 입사 후 회계연도 경계 도래 횟수 |
| 1년 경계 전환 | 입사일 정정으로 월차→본연차 전환: BACKFILL/RESET_ALL 각각 결과 정합(월차 잔존 중복 없을 것) |
| 멱등/동시성 | 버튼+배치 동시, UNIQUE(CMPNY_CD,IDEMPOTENCY_KEY) 최종 차단, TOCTOU |
| 기부여 보호 | 입사일 미래로 정정 → 차감 금지(BACKFILL/APPLY_NEW 변화없음), RESET_ALL만 cancel. `tb_user_leave_use` 불변 |
| 경계 입력 | 입사일 미래/근속 0개월/입사일 미입력(전건 거부) |
| 마이그레이션 | 재키잉 후 첫 부여 실행이 이중부여 없는지(M1 검증) |

---

## 8. 기부여 보호 절대 가드 체크리스트 (§8.5.8 — 어떤 작업에서도 위반 금지)

- [ ] 기 발생 연차 **사후 차감 없음** (백필/비례/배치는 INSERT만)
- [ ] 축소·정정은 **RESET_ALL의 CANCELED 소프트 취소**로만
- [ ] `tb_user_leave_use` 사용이력 **삭제·변경 없음**
- [ ] 모든 부여 `IDEMPOTENCY_KEY` UNIQUE 중복 차단
- [ ] STATUS 4종(ACTIVE/EXHAUSTED/EXPIRED/CANCELED) 정합 + EXPIRE_YN/DEL_YN 동기

---

## 9. 범위 밖 / 리스크 / 오픈 이슈

**범위 밖**: prafta-024(사용 단위 단일화)와 무관. 사용 신청/차감 로직 변경 없음(부여 측만).

**리스크**:
- (최대) M1 재키잉 — 운영 데이터 직접 변경. 백업·충돌점검·롤백·dev 선검증 필수.
- 배치 대량 부여 성능(MAX_GRANT_USER_COUNT=500 등 청크/스케줄 시간 고려).
- 잘못된 산정의 자동화 → E는 B·C·D 검증 완료 후.

**오픈 이슈 (착수 시 사용자/planner 결정)**:
1. 과거 월차를 `{YYYYMM}` 월별로 **소급 재분할**할지, 기존 연단위분은 보존하고 신규부터 월별로 갈지.
2. `tb_user_leave_grant.PERIOD_YM`(부여 대상기간) 컬럼 추가 여부(감사/백필 정확도 ↑ vs 스키마 변경).
3. M1을 **일회성 재키잉** vs **엔진 이중조회 전환기** 중 무엇으로?
4. EXPIRE_YN deprecation(작업 H)을 본 묶음에 포함할지 분리할지.
5. 자동부여 배치 주기(일/월 1회)와 실행 시각, 회사별 정책 적용 범위.

---

## 10. 착수 가이드

1. 본 계획서 + 정책서 §8.5.2~§8.5.8 + `hire-date-change-handling-guide.md` 정독.
2. **A 먼저**: M1 마이그레이션 방식(일회성 vs 이중조회) 사용자 승인 → dev 검증.
3. B→C→D 엔진 확장(단위테스트 동반) → F 프리뷰/영향 정합 → E 배치 → G 프론트/문서.
4. 모든 단계에서 §8 가드 체크리스트 통과 확인.
5. (개발 착수 시) 작업 로그/보안 로그는 메인 세션이 Notion에 기록(서브에이전트는 Notion 미사용).

---

## 11. 확정 결정 + 구현 현황 (2026-05-24)

### 확정된 결정 (사용자)
- **백필 범위 = (i) + 유효기간 제한**: 입사일을 과거로 정정 시 누락 본연차/근속가산을 **소급 부여하되**, 각 컴포넌트의 발생일 기준 **사용기간(AXIS6 유효개월)이 아직 유효(availTo ≥ today)한 과거분만** 부여. 유효기간 지난(소멸) 과거분은 휴가로 되살리지 않음(→ 필요 시 HR이 연차수당/수동부여로 별도 처리).
- **멱등키 전환 = (M1 안 함) dual-read**: 운영 데이터 재키잉 마이그레이션 미수행. 엔진이 정식 키 + 레거시 `_HIRE` 키를 dual-read하여 이중부여 차단(되돌리기 쉬운 코드 변경).
- **경력인정(creditMonths) 규칙(가정)**: 본연차/월차 **발생 연차 수에는 미반영**, **근속가산 tier에만 가산**. (기존 단발 산정과 미세 차이 가능 — 로컬 테스트로 검증)

### 구현 완료 (코드, 로컬 빌드·테스트 필요)
- **작업 A 완료**: `buildIdempotencyKey` 정식 키(`{userCd}_{periodLabel}_{grantType}{keySuffix}`) + `legacyHireIdempotencyKey`/`alreadyGranted` dual-read. 부여 일수 변화 없음(키 plumbing).
- **작업 C 핵심 완료(본연차/근속, HIRE_DATE)**: `computeBackfillPeriods`(read-only, 유효기간 제한 + 당해분 제외) 추가. `hireDateGrant`에서 BACKFILL/RESET_ALL일 때 과거 누락 본연차+근속가산 소급 부여(APPLY_NEW은 당기분만 → 이제 두 KEEP이 실제로 다름). `previewPolicyGrant`에 소급분 addDays 합산 + 노트 보정(F 일부).
- **작업 D 완료(PRORATE + CEIL/ROUND/FLOOR)**: `resolveFiscalEntitlement` 첫 부분기간(crossedFiscalStarts==0) + AXIS3=PRORATE 시 비례 본연차 `(입사~다음 회계연도 시작 일수÷365)×15` 부여. `computeProratedAnnualDays`/`applyAxis4Rounding` 추가. AXIS4 CEIL/ROUND/FLOOR 정수 반올림. **단위테스트 3건 통과(`LeaveGrantEngineProrationTest`)** + 빌드 검증 완료. (`build.gradle`에 `useJUnitPlatform()` 누락분 추가 — 없으면 JUnit5 테스트 미실행)
- **작업 F 완료(영향분석 정합)**: 엔진에 read-only `estimateBackfillDays(cmpnyCd,userCd,hireDate)` 추가, `User01ServiceImpl.buildApproxImpact`의 "누락된 부여(변경 후 기준)"를 유효기간 내 소급 추정과 정합(엔진 주입, 순환의존 없음). 빌드 검증 완료. (FISCAL_YEAR/미래 입사일은 0)
- **작업 E 완료(자동 정기부여 배치 — 게이트)**: `LeaveGrantScheduler`(@Scheduled, **기본 비활성** `prafta.leave.auto-grant.enabled:false`) + 엔진 `runScheduledAutoGrant()`(활성정책 회사별 → 입사일 보유 활성직원 청크≤500 → `hireDateGrant` 시스템 컨텍스트 위임, 청크별 트랜잭션) + 열거 쿼리 2개. 빌드 검증 완료. **월차 per-월 누적은 미구현(엔진 당기 집계, B 잔여)**.
- **작업 G 완료(프론트/문서 정합)**: HireDateEditPop 처리방식 3종 설명·경고 문구를 실제 동작에 맞게 갱신. `Attd09ServiceImpl.getPolicyInfo` PRORATE 폴백 안내 비표시(D로 구현됨). 본 가이드 문서 §10 한계 갱신. (PolicyGrantPreviewPop은 addDays/note 이미 렌더링 → 백필/소급 자동 반영). 빌드 검증 완료.
- **follow-up #2 완료(FISCAL_YEAR 과거 백필)**: `computeBackfillPeriods` 재구조화 + `addFiscalBackfillPeriods` — 입사 후 도래한 과거 회계연도(당해 제외)마다 본연차+근속 유효기간 내 소급. 빌드 검증 완료.
- **follow-up #1 완료(월차 per-월 누적)**: 집계 월차 산정 제거 → `computeMonthlyPeriods`(YYYYMM 키, 월 1일, 유효기간 내, 처리방식·AXIS 무관)로 분리 부여. **레거시 ACTIVE 집계 월차 보유 연도는 상호배타 제외**(`countActiveByIdempotencyKey` 신규). hireDateGrant/preview 배선. 빌드+기존 단위테스트 통과.

### 본 증분에서 의도적으로 제외(후속)
- **월차 per-월 누적**: 현행 당기 집계 유지. 매월 누적은 **작업 E(스케줄러)** 에서.
- **AXIS1=FISCAL_YEAR 과거 백필**: 발생일 산정 상이 → `computeBackfillPeriods`가 FISCAL이면 빈 목록 반환(당기 부여는 buildUserPlan 유지). 후속.
- **AXIS4=HALF_DAY(0.5일)**: 부여 일수를 0.5 단위로 표현하려면 GrantComponent/grantComponent 일수 타입 BigDecimal 전면 확장 필요 → 본 단계 미구현(임시 CEIL + 경고). 후속.
- **follow-up #3 완료(AXIS4=HALF_DAY 0.5일)**: 부여 일수 `int→BigDecimal` 전면 확장(records/grantComponent/Plan/Period/entitlement/backfill/monthly/grant루프/preview/estimate). `applyAxis4Rounding`/`computeProratedAnnualDays` BigDecimal 반환, HALF_DAY=0.5 절사. 프리뷰 addDays는 표시용 정수 반올림(DTO/프론트 무변경). 단위테스트(HALF_DAY 0.5 케이스) 통과 + 빌드 검증.
- **follow-up H Phase1 완료(EXPIRE_YN 읽기 의존 제거)**: `selectDeductibleGrant`의 redundant `EXPIRE_YN='N'` 제거(STATUS='ACTIVE'로 충분, 동작 불변). EXPIRE_YN은 쓰기 전용(STATUS 동기화)로 강등. **컬럼 DROP(Phase2)은 외부 읽기 점검+승인 후 별도** — `prafta-023-H-plan.md`.
- **#1 월차 per-월 한계(레거시 전환)**: 레거시 ACTIVE 연 집계 월차 보유 연도는 만료/RESET 전까지 집계 유지(상호배타). 완전 per-월 전환은 일회성 마이그레이션 필요(후속). RESET/레거시 케이스는 런타임 검증 권장.
- **런타임 검증 체크리스트**: `.claude/context/prafta-023-verification-checklist.md` (사용자 테스트 → 일괄 피드백용).

### 검증 권장
- 로컬 `gradlew.bat compileJava` 빌드 확인.
- §7 테스트 매트릭스 중 본 증분 해당: 다년도 백필×유효기간 경계, 1년 경계 전환(월차→본연차), 멱등 재실행, RESET_ALL 재발급, dual-read 이중부여 없음.
