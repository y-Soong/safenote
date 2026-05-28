# 정책서 변경 이력

본 디렉토리(`.claude/context/policies/`)의 분할된 정책서가 docx 원본과 동기화된 이력을 기록한다.

## 2026-05-27 — prafta-032 (입사일 변경 "처리방식" 폐기 → 관리자 수동 연차 조정 전환)

근태관리 §8.5.6 갱신. 단일 출처 `.claude/requests/prafta-032-decisions.md`(D1·D3·D4·D5·D8). prafta-030(차액 보전)·prafta-031(수동부여 회수·noti outbox)을 전제로, 입사일 변경 시 동작하던 **자동 처리방식(SYS039: `KEEP_AND_BACKFILL`/`KEEP_AND_APPLY_NEW`/`RESET_ALL`)을 폐기**하고 **관리자 수동 목표 부여량 입력**으로 전환. 사유: 처리방식 자동계산은 회사/근로자 유불리가 일률적이지 않아 고정 자동계산 리스크가 큼 — 시스템은 검증·이력만, 부여량은 관리자가 직접 결정.

- **§8.5.6 — 처리방식 자동계산 폐기(D1, 비활성화 후 제거)**: `LeaveGrantEngineServiceImpl.buildUserPlan`의 `HANDLING_TYPE` 분기·`isReset`(RESET_ALL 취소+재발급)·`isBackfill`(차액 보전) **부여 경로를 데드코드로 비활성화**(항상 `KEEP_AND_APPLY_NEW` 동치 = 신규 부여만). 물리 삭제는 다음 단계(롤백 안전성 확보 후, 032009). `TB_USER_HIRE_DATE_HISTORY.HANDLING_TYPE`은 신규분 `'MANUAL'` 고정, 기존 이력 값(KEEP_*/RESET_ALL)은 감사 추적용 보존. **prafta-029/030 가드(RESET 회차키 누수·차액보전 멱등키 `_BF`)와 멱등키 체계는 잔존**(경로만 끊음).
- **§8.5.6 — 수동 연차 조정(D2~D5)**: 차액 = (관리자 입력 목표 법정 부여량) − (현재 ACTIVE 법정 부여량). `>0` 추가부여(D4), `<0` 회수(D5), `=0` 무처리. 법정(STATUTORY_*)만 대상, 약정(MANUAL_*)은 표시만(Attd_09 manualGrant로 별도 대응).
  - **추가(D4)**: 새 입사일 기준 미부여 발생일(오늘 이전·소멸 전) 빠른순 소급 부여 + 오늘 폴백. 소급 GRANT_REASON=`입사일 변경 소급(INSADAY_CHANGE_BACKFILL)`, 폴백=`입사일 변경 초과 부여(MANUAL_OVERAGE)`. GRANT_TYPE 자동판단(creditedMonths <12 MONTHLY / 12~36 ANNUAL / ≥36 TENURE_BONUS), GRANT_BY_TYPE=`'01'`, 멱등키 접미사 `_HD{histId}`(_BF/_R와 구분).
  - **회수(D5, A안 직접 차감)**: ACTIVE 법정만, 우선순위 ①소멸임박 ②부여최근 ③GRANT_ID큰순. 행 잔여 전체+USED_DAYS=0 → `CANCELED`(prafta-031 패턴), 부분/USED_DAYS>0 → GRANT_DAYS 직접 차감(USED_DAYS·`tb_user_leave_use` FK 불변). 별도 회수 테이블 미신설(잔액/차감 SQL 무수정이 A안 핵심).
  - **검증(D3)**: 회수 시도량 > 회수가능량(ACTIVE 법정 잔여=GRANT_DAYS−USED_DAYS 합, 신청 진행분은 USED_DAYS 예약으로 자연 반영) → 차단(`USER_400_030`). 회수 사유 필수(`USER_400_031`), 목표 음수 불가(`USER_400_032`). 입사일 UPDATE+조정+이력 INSERT는 단일 `@Transactional(rollbackFor=Exception.class)`(D8).
- **GRANT_DATE 정합 정정(#3)**: 소급 부여행의 `GRANT_DATE`는 **부여 실행일(오늘)**, 발생일은 `AVAIL_FROM_DATE`에 기록(기존 `grantComponent`/`computeBackfillPeriods` 컨벤션과 일관). 결정문서 D4의 "GRANT_DATE=발생일" 표기 폐기. 코드는 변경 없이 Javadoc·결정문서만 정정.
- **테스트 정리**: 폐기된 처리방식(backfill/reset)을 단정하던 단위테스트 8건 `@Disabled`(030Test 6: case1/7/13·backfill_idempotentReclick·preview_backfillShortfallReflected·boundaryB / ScenarioTest 2: resetAll_cancelsThenReissues·c_backfill_vs_applyNew). 라벨버그 테스트(`c_labelBug_novemberHire_noDuplicate`)는 단일 KEEP(소급 없음) 현행 동작에 맞춰 단정 재작성(31→16, 당기 본연차15+근속가산1). 신규 `LeaveGrantEnginePrafta032Test`(10건: target=null/음수, 추가 소급/폴백, GRANT_TYPE 경계, 회수 차단/사유/전체취소/부분차감/used보존) 추가. 전 LeaveGrantEngine 테스트 GREEN(@Disabled 8 제외).
- 마이그레이션: `prafta-backend/src/main/resources/sql/migration/prafta-032-*.sql`(운영 미적용, 사용자 수동). **D6(Attd_09/PolicyGrantPreviewPop reissueCount 제거)는 이번 범위 밖 — 후속 처리.**

## 2026-05-26 — prafta-030 (입사일 변경 처리방식 정합: 옵션1 차액 보전 · 월차 더블딥 게이트 · 월차 소멸 만1년 일괄)

근태관리 §8.5.4/§8.5.6 갱신 + 가이드(`hire-date-change-handling-guide.md`) 정정. 단일 출처 `.claude/requests/prafta-030-decisions.md`(D1~D5). 현행 엔진을 18케이스 정답표에 맞춰 확인하고 어긋난 부분만 수정. **신규 테이블 없음**(기존 `TB_USER_LEAVE_GRANT` / `TB_USER_HIRE_DATE_HISTORY` 재사용).

- **§8.5.6 — 옵션1(`KEEP_AND_BACKFILL`) 차액 보전 전환(D1)**: "누락 컴포넌트 INSERT"에서 **"부족분 차액 단건 보전"**으로. 차액 = (새 기준 본연차+가산 누적) − (기존 누적, 소멸 제외·사용 포함·월차 포함). >0이면 `STATUTORY_ANNUAL` 1건. 보전 GRANT_REASON = `입사일 변경 보전(INSADAY_CHANGE_BACKFILL)`, 멱등키 접미사 `_BF{histId}`. 추가만 하며 감소 없음(기부여 보호 §8.5.8 유지). 일수 하향·체계 전환은 여전히 `RESET_ALL`.
- **§8.5.4 — 월차 더블딥 게이트(D2)**: 월차는 실근속 기준이되, (실근속<12 AND 산정근속≥12 AND **full 본연차 15** 발생)인 "경력인정 고용승계 더블딥"에서만 미발생. 정상 근로자·FISCAL 비례(crossed==1 PRORATE, <15)·FISCAL crossed==0은 법정 월차(§8.5.4) 보존(full 15일 때만 차단 — 비례 < 15는 차단 안 함).
- **§8.5.4 — 월차 소멸 만1년 일괄 교정(D2-B, 2026-05-26 추가)**: 1년 미만 월차 소멸을 "발생일+AXIS6(12개월) per-건"에서 **"만 1년 도래일 일괄 소멸"**(`hire.plusYears(1).minusDays(1)`, 근기법 §60⑦/정답표 §1.4)로 교정. `computeMonthlyPeriods`의 월차 `AVAIL_TO_DATE`만 변경(본연차/가산 AXIS6 유효기간 불변). apply(`hireDateGrant`=Attd_09 '정책 기준 부여')와 preview가 `computeMonthlyPeriods`를 공유하므로 실제 부여 월차 행도 만1년 도래일로 INSERT. 만1년 경과 직원의 잔존 첫해 월차가 0으로 정합(과다 보유 교정). 테스트: 030Preview #6(22→16/16/15), 029 standardModelFiscalRegression 2025-02-22(21→13). 기존 DB 월차(구 availTo)는 점진 만료/RESET로 정리(마이그레이션 별도).
- **옵션별 미리보기 API + UI(D4)**: 입사일 변경 영향분석 GET(`/{userCd}/hire-date-impact`)를 `options[]`(옵션1/2/3 각 add/reclaim/finalHold/reclaimNote)+`fiscalNextGrantText`로 확장(read-only — 부여 안 함). HireDateEditPop 옵션별 시뮬·회수표시·FISCAL 다음 회계연도 발생시점 노출. UserInfoPop 경력인정 안내.
- 구현/검증: `LeaveGrantEngineServiceImpl`(`computeBackfillShortfall`·`computeNewBasisAnnualCumulative`·`computeBackfillAvailToDate`·`isCreditDoubleDip`·`computeMonthlyPeriods`·`previewReallocationOptions`·`buildFiscalNextGrantText`), `LeaveDashboardMapper.xml`(`selectStatutoryGrantAccrual`), `User01ServiceImpl`·`HireDateImpactResponse`, FE `HireDateEditPop.vue`/`UserInfoPop`. DB 마이그레이션 없음.

## 2026-05-25 — prafta-029 (회계연도 기준 첫해 본연차 부여 표준 모델화)

근태관리 §8.5.3/§8.5.4 갱신. 연차 부여 엔진 검증(prafta-029) 중 발견된 회계연도 기준 첫해 본연차 조기 부여 버그를 고용노동부 표준 모델로 교정.

- **§8.5.3 교차 매트릭스**: `AXIS1=FISCAL_YEAR`의 AXIS3 허용 집합을 `{MONTHLY_ONLY, PRORATE, NEXT_YEAR_BULK}` → **`{PRORATE, NEXT_YEAR_BULK}`**로 축소(MONTHLY_ONLY 차단, `ATTD_400_020`). `MONTHLY_ONLY`는 입사일 기준 전용으로 명시.
- **§8.5.3.1 신규**: 회계연도 첫해 본연차 표준 모델 표(crossed==0 월차만 / crossed==1 PRORATE=전년부분 비례·NEXT_YEAR_BULK=만15 / crossed>=2 만15+근속). "회계연도 시작 1회 통과만으로 만15 부여" 버그 교정 명시. 비례 base = 입사일~도래 회계연도 시작 재직일수/365×15.
- **§8.5.4**: `MONTHLY_ONLY` 설명에 "입사일 기준 전용" 단서 추가.
- 구현/검증: `LeaveGrantEngineServiceImpl.resolveFiscalEntitlement`·`computeProratedAnnualDays`, `LeavePolicyServiceImpl.validateAxisMatrix`, `Baim_07.vue`(회계연도 선택 시 월차만 비활성화), 회귀 테스트 `LeaveGrantEnginePrafta029Test.standardModelFiscalRegression`. 잔존 `FISCAL+MONTHLY_ONLY`는 엔진에서 NEXT_YEAR_BULK 폴백(마이그레이션 없음).

## 2026-05-20 — PRAFTA-018 단계 1 (법정 연차 부여 정책 도메인 설계)

근태관리 §8.5 신규 신설. 사용자 요청서 `.claude/requests/prafta-018.md` 기반. 단계 1 범위는 DDL + 정책서까지, 코드 구현은 단계 2 이후.

- **§8.5 "법정 연차 부여 정책" 신규 절** (9개 하위 절 신설)
  - §8.5.1 PRAFTA-017 attd03(연차 타입) / PRAFTA-018 baim07(법정 연차 axis 정책) 도메인 모델 분리. `GRANT_TYPE` prefix(`STATUTORY_*` / `MANUAL_*`)로 구분
  - §8.5.2 7개 axis 정의 (작업 지시서 §5 통합본 채택, §1.3.3 9개 axis는 폐기)
  - §8.5.3 Cross-axis 활성 매트릭스 (AXIS1↔AXIS3, AXIS3=PRORATE↔AXIS4 활성, AXIS4=HALF_DAY↔ALLOW_HALF_DAY 강제)
  - §8.5.4 1년 미만 월차 (법정 의무, axis 아님)
  - §8.5.5 시스템 LEAVE_CD 시드 6종 정의 + `SYSTEM_YN` 컬럼 추가
  - §8.5.6 입사일 변경 처리 매트릭스 (KEEP_AND_BACKFILL / KEEP_AND_APPLY_NEW / RESET_ALL)
  - §8.5.7 권한 매핑 (`AUTH_MASTER` + `AUTH_HR_MANAGER`)
  - §8.5.8 멱등성(IDEMPOTENCY_KEY) + 기 부여 자동 차감 금지 + STATUS↔EXPIRE_YN 동기화
  - §8.5.9 사용 단위 정책 (휴게시간/시간단위 시작시각/1일환산은 시스템 강제로 정책 컬럼 없음)
- **DDL** (`.claude/requests/prafta-018.sql`)
  - TB_USER ALTER (HIRE_DATE/EMPLOYMENT_TYPE/CONTRACT_END_DATE 추가)
  - tb_leave_type_mgmt ALTER (SYSTEM_YN 추가)
  - tb_user_leave_grant ALTER (GRANT_TYPE/USED_DAYS/POLICY_SEQ/IDEMPOTENCY_KEY/STATUS 추가) + STATUS 보정 UPDATE
  - 신규 테이블 5종: `TB_USER_SERVICE_CREDIT` / `TB_USER_HIRE_DATE_HISTORY` / `TB_LEAVE_POLICY` / `TB_LEAVE_POLICY_HISTORY` / `TB_LEAVE_USAGE_POLICY`
  - 신규 SYS 코드 8종: **SYS035~SYS042** (현재 최대 SYS034 다음부터 할당)

### 점검 필요 (후속 단계)

- **SYS024 정책서 표기 정합성**: 실제 DB는 `01=법정 / 02=특별`. 일부 정책서/코드/외부 문서에 "약정"으로 표기된 부분이 있는지 단계 2에서 일괄 점검 후 통일.
- **EXPIRE_YN ↔ STATUS 동기화 트리거/서비스 책임 분담** 단계 2 결정.
- **시스템 LEAVE_CD 시드 회사별 INSERT** — 회사 생성 트리거 또는 일회성 백필로 단계 2에서 처리.
- **활성 정책 1개 보장 트랜잭션 로직** — 단계 2 서비스 레이어.

## 2026-05-20 — PRAFTA-017 자동부여 부여일지정

근태관리 §8.1.1 / §8.1.2 갱신. 사용자 요청서 `.claude/requests/prafta-017.md` 기반.

- §8.1.1 "구성 속성" 표 갱신
  - "기본일수 / 부여일수" → "기본일수" 단독으로 단순화. 부여일수는 별도 부여 행위 시점에 결정한다는 노트 추가.
  - "자동부여 기준일" 값 보정: `01` 입사일 / `02` 생일 / `03` 부여일지정 (SYS027 코드)
  - "실행시점" 설명을 기준일별 분기로 명세 (`GRANT_OFFSET_MONTH` vs `GRANT_ASSIGN_MMDD`)
  - `TB_LEAVE_TYPE_MGMT.GRANT_DAYS` 컬럼이 PRAFTA-017로 DROP된 사실 명시
- §8.1.2 "자동부여 규칙" 신규 절 추가
  - SYS027 기준일별 cross-field validation 규칙
  - `GRANT_ASSIGN_MMDD` 입력값 형식 + 월별 일수 검증
  - **02/29 평년 02/28 fallback 정책 명문화** (저장은 허용, 부여 실행 스케줄러 책임)
  - UI 안내 문구 권장 워딩

### 점검 필요(qa 위임)

- SYS027=`02`의 의미: 본 갱신본은 "생일"로 기재했으나 기존 표는 "회계연도"였다. 화면 코드 주석/마스터 데이터/정책서 3자에서 일치 여부를 PRAFTA-017 QA 단계에서 확인하고, 불일치 시 정책서 재정정.

## 2026-05-14 — 초기 분할

원본 docx 3종을 마크다운으로 추출하여 섹션별 분할 저장.

| 영역 | 원본 파일 | 버전 |
| --- | --- | --- |
| 공통 | `PRAFTA_공통_정책서_v1_1.docx` | v1.1 |
| 근태관리 | `PRAFTA_근태관리_정책서_v1_0.docx` | v1.0 |
| 요청승인관리 재기획 | `PRAFTA_요청승인관리_재기획_v0_1.docx` | v0.1 |

### 발견된 정책서 간 불일치 (별도 점검 보고서로 정리 예정)

1. **요청승인관리 §9.6 화면 구성**: 근태관리 §9.6 = 3탭, 재기획서 §3.1 = 4탭 → 재기획서 우선 (단일 출처 선언).
2. **사후 상신 기한**: 근태관리 §9.3.1 = D+5일, 재기획서 §3.2 = 사업장별 근태 마감 전까지 → 재기획서 우선.

### 기술 정책서와의 불일치 (별도 정리 필요)

3. **JWT 토큰 만료**:
   - 공통 정책서 §3.4 = 액세스 1시간 / 리프레쉬 48시간
   - `.claude/agents/security.md` §8 = 액세스 30분 / 리프레쉬 14일
   - → 사용자 확인 필요.
