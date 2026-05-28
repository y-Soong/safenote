## 8. 휴가 정책

### 8.1 연차 타입 관리

연차 타입은 "연차 타입 관리" 화면에서 정의한다. 연차/반차/반반차/시간차/하계휴가 등 다양한 유형을 운영한다.

#### 8.1.1 구성 속성

| 속성 | 설명 |
| --- | --- |
| 연차코드 / 연차명 | 시스템 식별 코드 및 표시명 |
| 연차타입 | 사용자 신청 / 관리자 부여 |
| 유급구분 | 유급 / 무급 |
| 휴가성격 | 법정 / 약정 |
| 기본일수 | 연간 기본 부여 일수(사용자 신청 타입 한정) |
| 사용단위 | 일 / 반일 / 시간 |
| 사용 가능 기간 | 해당 연도 내 / 설정 안 함 등 |
| 사용여부 | 사용 / 미사용 |
| 자동부여 기준일 (SYS027) | `01` 입사일 / `02` 생일 / `03` 부여일지정 |
| 실행시점 | 자동부여 시점. 기준일=`01`/`02`이면 "기준일 N개월 전 1일" 오프셋(`GRANT_OFFSET_MONTH`, 1-11). 기준일=`03`이면 매년 지정 MMDD(`GRANT_ASSIGN_MMDD`). |
| 결재여부 / 결재단계 | 승인 프로세스 필요 여부 및 단계 수 |
| 인사팀승인 | HR 최종 승인 필요 여부 |
| 증빙여부 | 증빙 파일 첨부 필수 여부 |

> `TB_LEAVE_TYPE_MGMT.GRANT_DAYS` 컬럼은 PRAFTA-017로 제거되었다. 관리자 부여 타입의 부여 일수는 별도 "수동 부여" 행위 시점에 결정한다.

#### 8.1.2 자동부여 규칙 (PRAFTA-017)

관리자 부여 타입(`LEAVE_TYPE='02'`) + 자동 부여(`GRANT_TYPE='01'`) 조합에서만 자동부여 규칙이 의미를 가진다. 기준일 코드(SYS027)별 동작은 다음과 같다.

| 기준일 (SYS027) | 의미 | 필수 컬럼 | NULL이어야 하는 컬럼 |
| --- | --- | --- | --- |
| `01` 입사일 | 각 직원의 입사일 기준으로 자동 부여 | `GRANT_OFFSET_MONTH` (1-11) | `GRANT_ASSIGN_MMDD` |
| `02` 생일 | 각 직원의 생일 기준으로 자동 부여 | `GRANT_OFFSET_MONTH` (1-11) | `GRANT_ASSIGN_MMDD` |
| `03` 부여일지정 | 매년 동일한 월-일에 자동 부여 | `GRANT_ASSIGN_MMDD` (MMDD 4자리) | `GRANT_OFFSET_MONTH` |

##### Cross-field 검증

- 백엔드(`LeaveTypeRequest`)는 위 표에 따른 cross-field validation을 강제한다. 위반 시 `ATTD_400_015` 반환.
- 프론트(`LeaveTypeCreatePop.vue`)도 동일 규칙을 `canSave` 컴퓨티드에 반영한다.

##### `GRANT_ASSIGN_MMDD` 입력값 검증

- 형식: `^(0[1-9]|1[0-2])(0[1-9]|[12][0-9]|3[01])$`
- 월별 일수: 02월=28/29일, 04/06/09/11월=30일, 나머지=31일
- **02/29 입력은 허용한다.** 평년에 자동부여가 실행되는 시점에는 02/28로 fallback한다(부여 실행 스케줄러 책임 — 별도 후속 작업).

##### 안내 문구 권장 워딩 (UI)

- 기준일=`01` 또는 `02`: "(기준일) N개월 전 1일 00시에 자동 부여"
- 기준일=`03`: "매년 MM월 DD일 00시 자동 부여"

### 8.2 휴가 등록·신청

- 관리자는 스케줄관리 화면에서 휴가를 등록(연차 타입 적용 버튼)

- 근로자는 모바일 앱에서 스케줄이 존재하는 날에 한해 휴가 신청

- 승인권자: 해당 노드 담당 정/부 (공통 정책서 7.2 참조)

### 8.3 출근 차단 (노무 수령 거부)

#### 8.3.1 일 단위 휴가 승인일

- 출근 등록 자체를 차단

- 시도 시 얼럿 표시, 처리 불가

#### 8.3.2 시간 단위 휴가 승인 구간

- 해당 구간 내 출근·재출근 차단

- 시도 시 얼럿 표시, 처리 불가

### 8.4 시간 단위 휴가일 예외 처리

시간 단위 휴가 구간 시작 전 퇴근을 제때 등록하지 못하는 등 구간이 깨지는 케이스는 근태 보정 요청-승인으로 확정한다.

### 8.5 법정 연차 부여 정책 (PRAFTA-018)

회사 단위 법정 연차 부여 정책을 7개 axis 조합으로 정의하고, 일배치로 자동 부여한다. PRAFTA-017 §8.1(연차 타입 관리)와는 **분리된 도메인**으로 운영한다.

#### 8.5.1 도메인 모델 분리

| 영역 | 출처 | 대상 테이블 | 역할 |
| --- | --- | --- | --- |
| PRAFTA-017 attd03 | 사용자/관리자 정의 휴가 타입 | `tb_leave_type_mgmt` | 사용자 신청 휴가(병가/출산/교육 등) + 약정/포상휴가 등 관리자 부여 휴가 **타입** 정의 |
| PRAFTA-018 baim07 | 회사 단일 법정 연차 정책 | `TB_LEAVE_POLICY` (7개 axis) | 법정 연차의 **부여 규칙** (입사일/회계연도 기준, 비례부여, 근속 가산 등) 정의 |

두 모델은 `tb_user_leave_grant.GRANT_TYPE` 컬럼([SYS035]) prefix로 구분한다:

- `STATUTORY_*` — 법정 (시스템 자동 부여, PRAFTA-018 정책 기반)
- `MANUAL_*` — 약정/포상 (관리자 수동 부여, PRAFTA-017 타입 기반)

`tb_user_leave_grant.LEAVE_CD`는 두 영역 모두에서 사용한다. 법정 연차는 시스템 시드 LEAVE_CD(`SYS_ANNUAL`, `SYS_MONTHLY` 등)에 매핑되며, 해당 시드는 `tb_leave_type_mgmt.SYSTEM_YN='Y'` 행에 저장된다. SYSTEM_YN='Y' 행은 attd03 화면에서 편집/삭제 불가하다.

#### 8.5.2 7개 axis 정의

작업 지시서 §5 통합본을 따른다(§1.3.3의 9개 axis DDL은 폐기).

| # | axis 컬럼 | 옵션 / 입력 | 코드 |
| --- | --- | --- | --- |
| 1 | `AXIS1_GRANT_BASE` | `HIRE_DATE` 입사일 / `FISCAL_YEAR` 회계연도 | SYS036 |
| 2 | `AXIS2_FISCAL_START_MM` / `_DD` | 회계연도 시작월(01~12) / 시작일(01~31). AXIS1=`FISCAL_YEAR`일 때만 활성 | — |
| 3 | `AXIS3_FIRST_YEAR_METHOD` | `MONTHLY_ONLY` 월차만 / `PRORATE` 비례 / `NEXT_YEAR_BULK` 차년도 일괄 | SYS037 |
| 3-보조 | `AXIS3_PREGRANT_YN` | `Y` 입사일 일괄선부여 / `N` (프리셋 4번 표현) | — |
| 4 | `AXIS4_PRORATE_ROUNDING` | `CEIL` 올림 / `ROUND` 반올림 / `FLOOR` 내림 / `HALF_DAY` 0.5일 절사. AXIS3=`PRORATE`일 때만 활성 | SYS038 |
| 5 | `AXIS5_TENURE_MODE` / `_START_YEAR` / `_INTERVAL` / `_MAX_DAYS` | 근속 가산 정책. `LEGAL` 시 n=3, m=2, max≥25 강제. `CUSTOM` 시 n∈1~3, m∈1~2, max≥25 | — |
| 6 | `AXIS6_VALIDITY_MONTHS` | 12개월(법정) 고정 — prafta-028에서 24개월 연장옵션 폐지 | — |
| 7 | `AXIS7_USE_PROMOTION` | `Y` 사용촉진 운영 / `N` | — |

회사당 활성 정책은 1개만 유지한다(`TB_LEAVE_POLICY.USE_YN='Y'`). 정책 변경 시 이전 정책을 `USE_YN='N'`으로 UPDATE한 뒤 새 정책을 INSERT한다(트랜잭션 단위, 서비스 레이어 책임).

#### 8.5.3 Cross-axis 활성 매트릭스

| 조건 | 강제 규칙 | 위반 시 |
| --- | --- | --- |
| `AXIS1=HIRE_DATE` | `AXIS3 ∈ {MONTHLY_ONLY}`만 허용 (PRORATE/NEXT_YEAR_BULK 차단) | `ApiException` |
| `AXIS1=FISCAL_YEAR` | `AXIS3 ∈ {PRORATE, NEXT_YEAR_BULK}`만 허용 (**MONTHLY_ONLY 차단** — prafta-029) | `ApiException` (`ATTD_400_020`) |
| `AXIS3=PRORATE` | `AXIS4` 입력 활성 (그 외 분기에서는 `AXIS4='CEIL'` 기본값 강제 저장) | 사용자 입력 무시 |
| `AXIS4=HALF_DAY` | `TB_LEAVE_USAGE_POLICY.ALLOW_HALF_DAY='Y'` 강제, 해제 불가 | `ApiException` |
| `AXIS5_TENURE_MODE=LEGAL` | `AXIS5_START_YEAR=3`, `AXIS5_INTERVAL=2` 강제 | — |
| `AXIS5_TENURE_MODE=CUSTOM` | `1 ≤ AXIS5_START_YEAR ≤ 3`, `1 ≤ AXIS5_INTERVAL ≤ 2`, `AXIS5_MAX_DAYS ≥ 25` | `ApiException` (법정 위반) |
| `AXIS6_VALIDITY_MONTHS` | `= 12` (법정 고정 — prafta-028, 24 연장옵션 폐지) | `ApiException` |

> **prafta-029**: `MONTHLY_ONLY`는 입사일 기준(`HIRE_DATE`) 전용이다. 회계연도 기준에서는 월차가 1년 미만 법정 의무로 항상 발생하므로(§8.5.4), 첫해 **본연차** 처리 방식만 고르면 되며 그 선택지는 `PRORATE`(비례) / `NEXT_YEAR_BULK`(차년도 일괄) 둘뿐이다. 정책 저장·영향분석 검증(`LeavePolicyServiceImpl.validateAxisMatrix`)과 화면(`Baim_07.vue` — 회계연도 선택 시 "월차만 부여" 비활성화)이 함께 강제한다.

#### 8.5.3.1 회계연도 기준 첫해 본연차 부여 표준 모델 (고용노동부, prafta-029)

`crossed` = 입사일 이후 오늘까지 도래한 회계연도 시작(AXIS2) 횟수(`countFiscalStartsCrossed`). 본연차(`STATUTORY_ANNUAL`) 부여는 다음을 따른다. 월차(`STATUTORY_MONTHLY`)는 AXIS3과 무관하게 항상 별도 부여(§8.5.4).

| AXIS3 | `crossed==0` (첫 회계연도 시작 전) | `crossed==1` (첫 회계연도 시작 통과) | `crossed>=2` |
| --- | --- | --- | --- |
| `PRORATE` 비례 | 본연차 0 (월차만) | **전년 부분기 비례** = `15 × (현재 회계연도 시작 − 입사일)일수 ÷ 365`, AXIS4 반올림 | 본연차 15 + 근속가산 |
| `NEXT_YEAR_BULK` 차년도 일괄 | 본연차 0 (월차만) | **본연차 15 일괄** | 본연차 15 + 근속가산 |

- 두 방식의 차이는 **오직 `crossed==1`**에서만 난다(비례 vs 만연차 15). `crossed==0`은 둘 다 월차만, `crossed>=2`는 둘 다 만연차 15+근속가산으로 동일하다.
- 비례 base는 "입사일이 속한 첫 부분 회계연도의 재직일수"다(= 입사일 ~ 입사 직후 도래한 회계연도 시작). 만 15일은 그 다음 회계연도 시작(`crossed>=2`)부터 부여된다 — 즉 **회계연도 시작을 1회 넘겼다는 사실만으로 만 15일을 주지 않는다**(prafta-029 이전 버그 교정).
- 구현: `LeaveGrantEngineServiceImpl.resolveFiscalEntitlement` / `computeProratedAnnualDays`. DB에 잔존하는 비표준 조합(`FISCAL_YEAR + MONTHLY_ONLY`)은 엔진에서 `NEXT_YEAR_BULK`로 안전 폴백(INFO 로그)하며, 별도 마이그레이션은 두지 않는다(화면 정규화 + 신규 저장 차단으로 자연 교정).

#### 8.5.4 1년 미만 월차 (법정 의무, axis 아님)

근로기준법 §60②에 따라 1년 미만 근로자는 매월 만근 시 1일씩 최대 11일 부여된다. 이는 회사 정책과 무관한 법정 의무로, axis로 두지 않고 화면(직접 설정)에서 AXIS3 카드 내 안내문으로 표시한다.

- 자동 부여 시: `GRANT_TYPE=STATUTORY_MONTHLY`, `IDEMPOTENCY_KEY="{USER_CD}_{YYYYMM}_MONTHLY"`
- 3번 axis의 `MONTHLY_ONLY`는 "본연차 추가 부여 없이 법정 월차로만 운영"한다는 선언이며(**입사일 기준 전용 — prafta-029, §8.5.3 참조**), 법정 월차 자체는 어떤 axis 조합에서도 동일하게 발생한다.

##### 월차 더블딥 게이트 (prafta-030)

월차는 **실근속 기준**으로 발생함을 원칙으로 하되, **"경력인정 고용승계 더블딥"인 경우에만** 발생시키지 않는다. 차단은 다음 세 조건을 모두 만족할 때로 한정한다(`computeMonthlyPeriods` / `isCreditDoubleDip`).

1. 실근속 `actualMonths < 12` (실제 재직 1년 미만)
2. 경력 인정 포함 산정근속 `creditedMonths ≥ 12`
3. 이번 부여 entitlement에 **full 본연차(`STATUTORY_ANNUAL`, days ≥ 15)** 발생

좁힌 결과(월차 **보존**):

- 정상 근로자(경력인정 0): 산정=실근속 → 조건(2) 거짓 → 월차 보존.
- FISCAL 비례부여(crossed==1 PRORATE, 비례 < 15): full 15 아님 → 조건(3) 거짓 → 월차 보존.
- FISCAL 첫 부분기(crossed==0): 본연차 미발생 → 조건(3) 거짓 → 월차 보존(공백 방지).
- 오직 경력인정으로 실근속 < 1년인데 full 15를 받는 더블딥만 차단(중복 월차 제거).

> **full 15일 때만 차단이 정당**하다(본연차 15 ≥ 월차 상한 11 + 즉시 사용 가능 → "더 유리한 처우"라 §60② 위반 아님). **비례 < 15(예: 7)이면 차단 시 법정 미달이므로 차단하지 않는다.** 이미 INSERT된 월차는 차감하지 않는다(미래 부여 산정만 게이트, §8.5.8 기부여보호).

##### 월차 소멸 = 만 1년 도래일 일괄 (prafta-030 D2-B)

1년 미만 월차의 소멸(AVAIL_TO_DATE)은 **발생일별 유효기간(AXIS6)이 아니라 "만 1년 도래일에 일괄 소멸"**한다(근로기준법 §60⑦). 즉 첫해에 발생한 월차는 **발생 시점과 무관하게 모두 "입사일 + 1년 − 1일"(입사 1주년 직전)에 소멸**한다.

- 산식(`computeMonthlyPeriods`, 월차 한정): `AVAIL_TO_DATE = hire.plusYears(1).minusDays(1)` (BASIC_ISO_DATE, YYYYMMDD).
- 예: 2023-08-15 입사 → 첫해 월차 전부 2024-08-14 소멸.
- 발생일(AVAIL_FROM = 입사 + m개월)은 그대로 둔다. 소멸일만 모든 첫해 월차에 동일하게 만 1년 도래일을 부여한다.
- 효과: 만 1년 도래일이 오늘 이전인(만1년 경과) 직원은 첫해 월차가 전부 소멸(0)되고, 1년 미만 직원은 발생분이 유지된다.
- **AXIS6(발생일 + 유효개월)은 본연차(`STATUTORY_ANNUAL`)·근속가산(`STATUTORY_TENURE_BONUS`) 유효기간 전용**이며 월차에는 적용하지 않는다.
- 정책 기준 부여(Attd_09 "정책 기준 부여" = `hireDateGrant`)와 옵션별 미리보기(`previewReallocationOptions`/`previewPolicyGrant`)가 동일한 `computeMonthlyPeriods`를 공유하므로, 실제 부여되는 월차 행의 AVAIL_TO_DATE도 만 1년 도래일로 INSERT된다. 만료 스케줄러는 AVAIL_TO_DATE 기준으로 동작하므로 신규 부여분은 자동 정합한다.

#### 8.5.5 시스템 LEAVE_CD 시드

`tb_leave_type_mgmt`에 시스템 자동 부여용 시드 6종을 `SYSTEM_YN='Y'`로 등록한다.

| LEAVE_CD | LEAVE_NM | 용도 | LEAVE_NATURE_TYPE (SYS024) |
| --- | --- | --- | --- |
| `SYS_ANNUAL` | 연차 | 본연차 자동 부여 (1년 이상) | `01` 법정 |
| `SYS_MONTHLY` | 월차 | 1년 미만 법정 월차 | `01` 법정 |
| `SYS_TENURE_BONUS` | 근속가산 연차 | AXIS5 가산분 | `01` 법정 |
| `SYS_PROMOTION` | 사용촉진 연차 | AXIS7=Y 시 사용촉진 잔여 처리 | `01` 법정 |
| `SYS_PREGRANT` | 일괄선부여 연차 | AXIS3_PREGRANT_YN=Y 시 입사일 일괄 | `01` 법정 |
| `SYS_BIRTHDAY` | 생일 안식휴가 | PRAFTA-017 SYS027=`02` 자동부여와 별개, 예약용 | `02` 특별 |

공통 코드값(시드 INSERT 시): `LEAVE_TYPE='02'`(관리자 부여), `GRANT_TYPE='01'`(자동), `PAID_TYPE='01'`(유급).

`SYSTEM_YN='Y'` 행은 attd03 연차 타입 관리 화면에서 readonly + 삭제 불가로 표시한다. 회사별 시드 INSERT는 회사 생성 트리거/스크립트 또는 일회성 백필로 단계 2에서 처리한다.

#### 8.5.6 입사일 변경 처리 매트릭스

입사일 변경은 4가지 시나리오 × 3가지 처리 옵션 조합으로 운영한다([SYS039]).

| 시나리오 | 현재 근속 | 입사일 방향 | 권장 처리 |
| --- | --- | --- | --- |
| A | 1년 미만 | 과거로 | `KEEP_AND_BACKFILL` |
| B | 1년 미만 | 미래로 | `KEEP_AND_APPLY_NEW` (사용 이력 있을 때) |
| C | 1년 초과 | 과거로 | `RESET_ALL` (입사일 정책 시) / `KEEP_AND_BACKFILL` (회계연도 정책 시) |
| D | 1년 초과 | 미래로 | `KEEP_AND_APPLY_NEW` (사용 이력 있을 때) |

처리 방식 상세:

- `KEEP_AND_BACKFILL` — 기존 GRANT 유지 + **부족분 차액 보전 단건**(prafta-030, 권장). 컴포넌트별 INSERT가 아니라, **(새 기준 본연차+가산 누적) − (기존 부여 누적)**의 차액(>0일 때만)을 `STATUTORY_ANNUAL` **1건**으로 추가 부여한다. 절대 줄이지 않는다(차액 ≤ 0이면 추가 없음 = `KEEP_AND_APPLY_NEW`와 동치).
  - **기존 부여 누적**: live(`STATUS!='CANCELED' AND DEL_YN='N'`) 전 `STATUTORY_*`(월차 포함)에 대해 `USED_DAYS + (AVAIL_TO_DATE >= today ? GRANT_DAYS-USED_DAYS : 0)` 합 = **소멸 제외 + 사용 포함**. (월차→본연차 경계에서 기존 월차 누적이 차감되어 부족분만 보전됨.)
  - **보전 GRANT_REASON** = `입사일 변경 보전(INSADAY_CHANGE_BACKFILL)`, **멱등키 접미사** = `_BF{histId}`(표준키/리셋키와 충돌하지 않는 보전 전용 네임스페이스), 소멸일(AVAIL_TO_DATE) = 최신 발생일 + AXIS6.
  - **기부여 보호(§8.5.8) 유지**: 추가만 하며 기존 행을 줄이거나 취소하지 않는다. 일수 하향·체계 전환(월차↔본연차) 정정은 `RESET_ALL`을 사용한다.
- `KEEP_AND_APPLY_NEW` — 기존 GRANT 유지 + 신규 부여만 새 입사일 기준 계산(당기·소급 추가 없음). HIRE_DATE만 UPDATE.
- `RESET_ALL` — 기존 `STATUTORY_*` GRANT를 `STATUS='CANCELED'`로 소프트 처리 + 새 입사일 기준 전체 재발급. **`tb_user_leave_use` 사용 이력은 어떤 옵션에서도 절대 삭제하지 않는다.**

모든 입사일 변경은 `TB_USER_HIRE_DATE_HISTORY`에 영향 스냅샷(`AFFECTED_GRANT_SNAPSHOT JSON`) 포함하여 영구 보관한다. 변경 사유는 자유 텍스트 필수.

#### 8.5.7 권한 매핑

| 작업 | 필요 권한 | 출처 |
| --- | --- | --- |
| 입사일 변경 (POST) | `AUTH_MASTER` OR `AUTH_HR_MANAGER` | common §8.2.1 |
| 정책 변경/저장 (POST) | `AUTH_MASTER` OR `AUTH_HR_MANAGER` | common §8.2.1 |
| 수동 부여 (POST) | `AUTH_MASTER` OR `AUTH_HR_MANAGER` | common §8.2.1 |
| 정책 조회 (GET) | 인증 사용자 + 사업장 스코프 | common §8.2 |
| 대시보드 조회 (GET) | `AUTH_MASTER` OR `AUTH_HR_MANAGER` + 사업장 스코프 | common §8.2 |

서버 코드 진입부에서 `AuthRoleUtils.isManager(authCd)` 또는 동등한 검증을 강제한다. 위반 시 `ApiException(ATTD_403_*)`. PRAFTA-017 보안 검토에서 발견된 "관리자 전용 endpoint 권한 누락" 이슈(prafta-017-001)와 동일한 패턴으로 본 작업에서 새로 추가되는 모든 POST endpoint에 적용한다.

#### 8.5.8 멱등성 + 기 부여 보호 (절대 규칙)

다음 규칙은 어떤 경우에도 어기지 않는다.

1. **모든 자동 부여는 `IDEMPOTENCY_KEY`로 중복 차단** (UNIQUE: `CMPNY_CD + IDEMPOTENCY_KEY`):
   - 본연차: `{USER_CD}_{YYYY}_ANNUAL`
   - 월차: `{USER_CD}_{YYYYMM}_MONTHLY`
   - 근속 가산: `{USER_CD}_{YYYY}_TENURE_BONUS`
   - 수동 부여: `{USER_CD}_{TIMESTAMP}_{GRANT_TYPE}`
2. **시스템은 기 발생 연차를 사후 차감하지 않는다.** 정책 변경 / 입사일 변경에서도 동일. `RESET_ALL`은 `STATUS='CANCELED'` 소프트 처리(데이터 보존, 감사 추적 가능).
3. **STATUS([SYS040]) 4종**: `ACTIVE` / `EXHAUSTED` / `EXPIRED` / `CANCELED`. 기존 `EXPIRE_YN` / `DEL_YN` 컬럼과는 다음과 같이 동기화한다(단계 1에서는 보정 UPDATE만, 트리거/서비스 동기 로직은 단계 2):

   | STATUS | EXPIRE_YN | DEL_YN | 의미 |
   | --- | --- | --- | --- |
   | `ACTIVE` | `N` | `N` | 사용중 |
   | `EXHAUSTED` | `N` | `N` | 잔여 0 도달 |
   | `EXPIRED` | `Y` | `N` | AVAIL_TO_DATE 경과 |
   | `CANCELED` | `N` | `N` | RESET_ALL 등에서 소프트 취소 |

4. **EXPIRE_YN deprecation은 단계 2 이후 별도 작업.** 단계 1에서는 STATUS 신규 컬럼 추가 + 보정 UPDATE만 수행.
5. **`tb_user_leave_use` 사용 이력은 어떤 옵션에서도 삭제하지 않는다.** RESET_ALL에서도 `GRANT_ID`가 `STATUS='CANCELED'`인 GRANT를 가리키지만 데이터 무결성은 유지된다.

#### 8.5.9 사용 단위 정책 (TB_LEAVE_USAGE_POLICY)

회사 정책(TB_LEAVE_POLICY)과 1:1로 매핑되는 사용 단위 정책. 컬럼:

- `ALLOW_FULL_DAY` — 항상 `Y`, 변경 불가 (SYS025-00 1일)
- `ALLOW_HALF_DAY` — 반차(0.5일) 단위 (SYS025-01). `AXIS4=HALF_DAY`일 때 `Y` 강제
- `ALLOW_HOUR_2` — 시간차 2시간 단위 (SYS025-02) — prafta-019-A
- `ALLOW_HOUR_1` — 시간차 1시간 단위 (SYS025-03) — prafta-019-A
- `ALLOW_MIN_30` — 시간차 30분 단위 (SYS025-04) — prafta-019-A
- `MAX_DAILY_REQUEST` — 같은 날 최대 신청 건수 (0=불허)

> ⚠️ prafta-019-A 에서 `ALLOW_QUARTER_DAY`(0.25일) / `ALLOW_HOURLY`(0.125일) 고정분수 컬럼은 폐기되고,
> 위 시간차 토글 3종 + "그날 소정근로분 기준 동적 환산"으로 대체되었다. SYS025 도 `00~04`로 재정렬됨.

휴게시간 처리, 시간 단위 시작 시각 제약, 1일 환산 시간은 정책 컬럼으로 두지 않고 시스템에서 일률 강제한다:

- 시간 단위 휴가는 휴게시간을 가로지르는 시간대 선택을 화면에서 차단
- 시작 시각 제약은 "근무 시간 내에서만" 고정
- 1일 환산 시간은 사용자별 근무 스케줄에 비례하여 자동 계산

