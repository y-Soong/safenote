# 연차(휴가) 도메인 — 테이블 & 비즈니스 로직 정리 (prafta-017/018)

> 작성일: 2026-05-21. prafta-017/018 작업 기준. 실 DB(MySQL, cmpnyCd='001') 실측 반영.
> 모든 데이터는 **회사(CMPNY_CD) 단위로 격리**된다.

---

## 1. 테이블 목록 (신규 / 확장 / 기존)

### 1.1 신규 생성 (prafta-018 단계1)
| 테이블 | 설명 |
| --- | --- |
| `TB_LEAVE_POLICY` | 회사 법정 연차 부여 정책 (7개 axis). 회사당 활성(USE_YN='Y') 1건. |
| `TB_LEAVE_USAGE_POLICY` | 연차 사용 단위 정책 (1일/0.5/0.25/0.125, 같은날 다중신청). 정책과 1:1. |
| `TB_LEAVE_POLICY_HISTORY` | 정책 변경 이력 (변경 전/후 스냅샷 JSON + 영향분석). |
| `TB_USER_SERVICE_CREDIT` | 사용자 경력 인정 (개월 단위, 점진 부여 전용). |
| `TB_USER_HIRE_DATE_HISTORY` | 입사일 변경 이력 (노무 감사용). |

### 1.2 기존 테이블에 컬럼 추가
| 테이블 | 추가 컬럼 |
| --- | --- |
| `TB_USER` | `HIRE_DATE`(입사일, 연차 기준), `EMPLOYMENT_TYPE`[SYS041], `CONTRACT_END_DATE` |
| `TB_USER_LEAVE_GRANT` | `GRANT_TYPE`[SYS035], `GRANT_BY_TYPE`[SYS043], `POLICY_SEQ`, `IDEMPOTENCY_KEY`, `STATUS`[SYS040] 등 |
| `TB_LEAVE_TYPE_MGMT` | `SYSTEM_YN`(시스템 시드 여부), `GRANT_ASSIGN_MMDD`(부여일지정 MMDD, prafta-018-006) |

### 1.3 기존 (이전 근태 작업 — 활용)
| 테이블 | 설명 |
| --- | --- |
| `TB_LEAVE_TYPE_MGMT` | 회사별 휴가 종류(카탈로그). LEAVE_CD가 키. |
| `TB_USER_LEAVE_GRANT` | 사용자 연차 부여 이력 (법정/수동 전부 적재). |
| `TB_USER_LEAVE_USE` | 사용자 연차 사용 실적 (부여에서 차감). |

### 1.4 공통코드 (TB_SYST_VAL_M / _D)
신규: **SYS035~043** (아래 §5). 기존: SYS021~027.

---

## 2. 테이블 관계 (한눈에)

```
회사(CMPNY_CD)
 ├─ TB_LEAVE_POLICY ──1:1── TB_LEAVE_USAGE_POLICY      (회사 연차 정책 + 사용단위)
 │        └── 변경 시 ──> TB_LEAVE_POLICY_HISTORY       (스냅샷 보존)
 ├─ TB_LEAVE_TYPE_MGMT                                  (휴가 종류 카탈로그, LEAVE_CD)
 └─ 직원(TB_USER, HIRE_DATE)
        ├─ TB_USER_SERVICE_CREDIT       (경력 인정 N개월)
        ├─ TB_USER_HIRE_DATE_HISTORY    (입사일 변경 이력)
        └─ TB_USER_LEAVE_GRANT          (부여 1건 = 휴가종류 LEAVE_CD + 정책 POLICY_SEQ)
                 └── 차감 ──> TB_USER_LEAVE_USE         (사용 실적, GRANT_ID 참조)
```

- `TB_USER_LEAVE_GRANT.LEAVE_CD` → `TB_LEAVE_TYPE_MGMT.LEAVE_CD`
- `TB_USER_LEAVE_GRANT.POLICY_SEQ` → `TB_LEAVE_POLICY.POLICY_SEQ` (수동부여는 NULL)
- `TB_USER_LEAVE_USE.GRANT_ID` → `TB_USER_LEAVE_GRANT.GRANT_ID`

---

## 3. 비즈니스 로직 흐름 (5단계)

**① 정책 설정** — 화면: 연차 부여 정책(`Baim_07.vue`)
- `TB_LEAVE_POLICY`(7-axis) + `TB_LEAVE_USAGE_POLICY`(사용단위) 저장. 회사당 활성 1건.
- 변경 시 기존 USE_YN='N' 처리 후 신규 INSERT + `TB_LEAVE_POLICY_HISTORY`에 스냅샷.
- 영향분석(화면8 모달)로 변경 전 시뮬레이션.

**② 휴가 종류 관리** — `TB_LEAVE_TYPE_MGMT`
- 각 휴가 종류를 LEAVE_TYPE(누가 신청)/GRANT_TYPE(자동·수동)/NATURE(법정·특별)/SYSTEM_YN으로 분류.

**③ 부여** — `TB_USER_LEAVE_GRANT`
- **자동 부여**: 스케줄러가 정책 기준 법정연차 부여 (GRANT_BY_TYPE='01'/AUTO). **※ 부여엔진 미구현 — 후속(prafta-018).**
- **수동 부여**: 관리자가 직원에게 부여 (GRANT_BY_TYPE='02'/ADMIN). 화면: 수동부여 모달.
- 부여 1건마다 STATUS(ACTIVE/EXHAUSTED/EXPIRED/CANCELED) + 유효기간(AVAIL_FROM~AVAIL_TO).

**④ 사용** — `TB_USER_LEAVE_USE`
- 휴가 사용 시 부여에서 차감. `TB_USER_LEAVE_GRANT.USED_DAYS`는 사용 합계 캐시.

**⑤ 현황 조회** — 화면: 연차현황(`Attd_09.vue`) + 직원상세 모달 + 영향분석
- 직원별 **법정 / 법정외**로 부여·사용·잔여 집계.

---

## 4. 핵심 규칙 (예시)

- **수동 부여 가능한 휴가 종류**: `TB_LEAVE_TYPE_MGMT`에서 `LEAVE_TYPE='02'(관리자 부여) AND GRANT_TYPE='02'(수동 부여) AND USE_YN='Y'` 인 것만 선택 가능. (현재 회사001엔 `00018 LEAVE_ADMIN_MANUAL` 1건)
- **법정 / 법정외 구분**: `TB_USER_LEAVE_GRANT.GRANT_TYPE LIKE 'STATUTORY_%'` → 법정 / `'MANUAL_%'` → 법정외. (값 카탈로그 = SYS035)
- **부여 방식**: `GRANT_BY_TYPE` = `'01'`(자동·스케줄러) / `'02'`(관리자 수동) — [SYS043].
- **활성 부여만 집계**: `STATUS='ACTIVE' AND DEL_YN='N'`. 잔여 = `GRANT_DAYS - USED_DAYS`.
- **회사 활성 정책 1건**: `TB_LEAVE_POLICY`는 회사당 `USE_YN='Y'` 1건만 (DB UNIQUE 제약 `UX_TB_LEAVE_POLICY_ACTIVE`).
- **7-axis 정책 규칙**:
  - AXIS1=`HIRE_DATE`(입사일 기준)면 → 첫해 처리(AXIS3)는 `MONTHLY_ONLY`만 가능 (비례/차년도일괄은 회계연도 기준에서만).
  - AXIS4(반올림)=`HALF_DAY` → 사용단위 0.5일(ALLOW_HALF_DAY) 강제 'Y'.
  - AXIS4(반올림)는 AXIS3=`PRORATE`일 때만 의미 / AXIS2(회계연도 시작일)는 AXIS1=`FISCAL_YEAR`일 때만 의미.
  - AXIS5(근속가산) `LEGAL`이면 시작 3년차·2년주기 고정, `CUSTOM`이면 n(1~3)·m(1~2)·최대일수(≥25) 입력.
- **수동 부여 INSERT 시 고정값**: `GRANT_TYPE='MANUAL_OTHER'`(법정외), `GRANT_BY_TYPE='02'`, `POLICY_SEQ=NULL`, `AVAIL_TO_DATE = AVAIL_FROM_DATE + 활성정책 AXIS6_VALIDITY_MONTHS`(없으면 12개월), `IDEMPOTENCY_KEY="{USER_CD}_{시각}_MANUAL"`.
- **경력 인정**: 직원 합산 = `SUM(CREDIT_MONTHS) WHERE USE_YN='Y'`. 법적 근속 기준일 = `HIRE_DATE − 합산개월`. (※ 적용은 항상 점진 부여 — 과거 소급 안 함)
- **입사일 변경**: `TB_USER.HIRE_DATE` UPDATE + `TB_USER_HIRE_DATE_HISTORY` INSERT(처리방식 HANDLING_TYPE[SYS039] + 사유). **기 부여 연차는 사후 차감하지 않음**(보호). 처리방식별 grant 백필/취소/재발급은 부여엔진 완성 후 후속.
- **권한**: 정책 저장 / 수동 부여 / 경력인정·입사일변경 = `AUTH_MASTER` 또는 `AUTH_HR_MANAGER`(=master/hr)만. (서비스 진입부 `AuthRoleUtils.isManager` 강제)

---

## 5. 연차 도메인 공통코드 (TB_SYST_VAL_M/D)

| 코드 | 이름 | 쓰이는 곳 / 값(예) |
| --- | --- | --- |
| SYS021 | 연차타입 | `TB_LEAVE_TYPE_MGMT.LEAVE_TYPE` — 01 사용자신청 / 02 관리자부여 |
| SYS022 | 연차부여타입 | `TB_LEAVE_TYPE_MGMT.GRANT_TYPE` — 01 자동부여 / 02 수동부여 |
| SYS023 | 연차유급구분 | `TB_LEAVE_TYPE_MGMT.PAID_TYPE` |
| SYS024 | 연차성격타입 | `TB_LEAVE_TYPE_MGMT.LEAVE_NATURE_TYPE` — 01 법정 / 02 특별 |
| SYS025 | 연차사용단위 | 01 1일 / 02 0.5일 / 03 0.25일 / 04 0.125일 |
| SYS026 | 연차사용가능기간타입 | — |
| SYS027 | 연차자동부여타입 | `TB_LEAVE_TYPE_MGMT.GRANT_BASE_TYPE` — 01 입사일 / 02 생일 / 03 부여일지정 |
| SYS035 | 연차 부여 분류 | `TB_USER_LEAVE_GRANT.GRANT_TYPE` — STATUTORY_ANNUAL/MONTHLY/TENURE_BONUS(법정), MANUAL_BONUS/CONDOLENCE/LONG_SERVICE/OTHER(법정외) |
| SYS036 | 연차 정책 AXIS1 | HIRE_DATE / FISCAL_YEAR |
| SYS037 | 연차 정책 AXIS3 | MONTHLY_ONLY / PRORATE / NEXT_YEAR_BULK |
| SYS038 | 연차 정책 AXIS4 | CEIL / ROUND / FLOOR / HALF_DAY |
| SYS039 | 입사일 변경 처리 방식 | KEEP_AND_BACKFILL / KEEP_AND_APPLY_NEW / RESET_ALL |
| SYS040 | 연차 부여 상태 | ACTIVE / EXHAUSTED / EXPIRED / CANCELED |
| SYS041 | 고용 형태 | REGULAR / CONTRACT / DAILY / EXECUTIVE |
| SYS042 | 경력 인정 사유 | MA_TRANSFER / EXPERIENCE_* / GROUP_MOVE / OTHER |
| SYS043 | 연차 부여 방식 | 01 자동 부여(AUTO) / 02 관리자 수동 부여(ADMIN) — `TB_USER_LEAVE_GRANT.GRANT_BY_TYPE` |

---

## 6. 화면 ↔ 테이블 매핑

| 화면 | 파일 | 주요 테이블 |
| --- | --- | --- |
| 연차 부여 정책 | `Baim_07.vue` (백엔드 `baim07`) | TB_LEAVE_POLICY, TB_LEAVE_USAGE_POLICY, TB_LEAVE_POLICY_HISTORY |
| 정책 변경 영향분석(모달) | `LeavePolicyImpactPop.vue` | TB_LEAVE_POLICY + TB_USER (근사 시뮬) |
| 연차 현황 대시보드 | `Attd_09.vue` (백엔드 `attd09`) | TB_USER_LEAVE_GRANT(법정/법정외 집계), TB_USER, TB_USER_SERVICE_CREDIT |
| 직원 연차 상세(모달) | `LeaveDetailPop.vue` | TB_USER_LEAVE_GRANT(부여이력) |
| 수동 부여(모달) | `ManualGrantPop.vue` | TB_LEAVE_TYPE_MGMT(LEAVE_TYPE=02·GRANT_TYPE=02 필터) → TB_USER_LEAVE_GRANT INSERT |
| 사용자 정보(팝업) | `UserInfoPop.vue` (백엔드 `user01`) | TB_USER, TB_USER_SERVICE_CREDIT |
| 입사일 수정(모달) | `HireDateEditPop.vue` | TB_USER.HIRE_DATE, TB_USER_HIRE_DATE_HISTORY |
| 연차 타입 관리 | `LeaveTypeCreatePop` (백엔드 `attd03`) | TB_LEAVE_TYPE_MGMT |

---

## 7. 현재 한계 (정직)

- **정밀 부여 엔진(스케줄러) 미구현** → 자동 법정연차 부여가 아직 없음. 그래서:
  - 영향분석(화면8)·입사일변경 영향분석은 **근사**(현재 부여 데이터 기준, 대부분 0).
  - 입사일 변경의 처리방식(BACKFILL/RESET 등)은 **이력에 기록만** 되고 실제 grant 조작은 미수행.
- **dev DB에 부여 데이터가 거의 없음** → 대시보드 수치는 수동 부여 전까지 0. 수동 부여(`ManualGrantPop`)로 데이터를 넣으면 의미 있는 값이 보임.
</content>
