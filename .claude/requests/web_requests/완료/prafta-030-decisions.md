# prafta-030 — 입사일 변경 처리방식별 동작 확인·수정 : 분석 및 확정 결정

> 작성: main 세션(분석·사용자 합의 결과). 다운스트림(planner/developer/qa/security)의 단일 출처.
> 합의일: 2026-05-26.

## 0. 작업 성격 (중요)
- 요청서: `.claude/requests/prafta-030.md`
- 참고: `.claude/requests/ref/prafta-030/` — `연차_재할당_프로세스_개발_작업요청서.md`(이상 설계), `법정연차_부여규칙_정확도_명세서.md`, `회계연도_기준_형평성_보전_의무.md`, `연차_재할당_18개_케이스_정리.xlsx`(→ `_xlsx_dump.txt` 텍스트 추출본).
- **참고 작업요청서는 prafta를 모르는 AI가 쓴 "이상적 설계"이고, 18케이스 xlsx는 사실상 "채점 정답표"다.**
- prafta는 이 문서가 제안하는 대부분(옵션1/2/3, 규칙 A/B/C, 발생/변경이력 테이블, 멱등·기부여보호)을 **이미 구현·검증**해 둔 상태(prafta-022/023/026/029).
- 따라서 본 작업 = **신규 구축이 아니라, 현행 엔진을 정답표에 맞춰 확인하고 어긋나는 부분만 수정**하는 것.

## 1. 요청서 ↔ prafta 매핑 (신규 테이블/Enum 만들지 않음)
| 요청서 개념 | prafta 현행 |
|---|---|
| 옵션 1 (기존유지+부족분 소급) | 처리방식 `KEEP_AND_BACKFILL` (SYS039) |
| 옵션 2 (기존유지+다음부터 새기준) | `KEEP_AND_APPLY_NEW` |
| 옵션 3 (회수+재부여) | `RESET_ALL` |
| 규칙 A 입사일 기준 | AXIS1=HIRE_DATE |
| 규칙 B 회계연도 비례 | AXIS1=FISCAL_YEAR + AXIS3=PRORATE |
| 규칙 C 회계연도 차년도 일괄 | AXIS1=FISCAL_YEAR + AXIS3=NEXT_YEAR_BULK |
| `TB_LEAVE_GRANT_HIST`(신규 제안) | **기존 `TB_USER_LEAVE_GRANT`** (GRANT_REASON varchar500·GRANT_TYPE·STATUS·IDEMPOTENCY_KEY·AVAIL_TO_DATE 모두 존재) |
| `TB_HIREDATE_CHANGE_LOG`(신규 제안) | **기존 `TB_USER_HIRE_DATE_HISTORY`** (HANDLING_TYPE·AFFECTED_GRANT_SNAPSHOT json·APPLIED_YN 모두 존재) |
| LEAVE_LAW_TYPE / GRANT_REASON enum | GRANT_TYPE(`STATUTORY_ANNUAL`/`STATUTORY_MONTHLY`/`STATUTORY_TENURE_BONUS`) + `GRANT_REASON`(varchar500 자유텍스트, 존재) |

- 명칭 주의: 요청서 STATUTORY_BONUS ≡ prafta `STATUTORY_TENURE_BONUS`. 요청서 CANCELLED ≡ prafta `CANCELED`(L 하나).

## 2. 이미 정답표와 일치 — 수정 불필요 (코드로 검증)
- 가산 패턴 1,1,2,2,…/최대25 (`tenureBonusDays` line 1045~1063).
- **옵션 2**: 기존 보유 유지·당기 추가 없음 → 정답표 #3·#4·#9·#10·#15·#16 일치.
- **옵션 3(RESET_ALL)**: 기존 STATUTORY 소프트취소(CANCELED) + 새 입사일 재발급 → #5(17)·#6(15) 등 일치. 사용이력(tb_user_leave_use) 불변.
- 미래 변경 무회수: KEEP 계열이 줄이지 않음 → #2·#8·#14 "감소 없음" 일치.

## 3. 확정 결정 (사용자 합의)
### D1 — 옵션1(`KEEP_AND_BACKFILL`) 차액 보전으로 전환  ★핵심
- 현행 문제(코드 확인): KEEP은 멱등키 미존재 컴포넌트만 INSERT하고 **기존 컴포넌트 일수를 재계산/교체하지 않음**(line 153~197, 482~488). 과거연도 백필은 유효기간(AXIS6 12개월) 경과로 제외 → 과거변경·근속증가(예: 3년차→5년차 가산 +1)가 **반영 안 됨**. 정답표 #1은 +1, #7/#13은 +2, 경계B는 +8을 기대.
- 결정: 과거변경(근속 증가)으로 **새 기준 부여누적(소멸 제외, 사용 포함) > 기존 부여누적**이면 그 **차액을 단건 추가 부여**한다.
  - 절대 줄이지 않음(기부여 보호 §8.5.8 준수, 추가만 함 → 미래변경/근속감소면 차액 ≤ 0 → 추가 없음, 옵션2와 동일 동작).
  - 추가분 GRANT_TYPE = `STATUTORY_ANNUAL`, GRANT_REASON = 입사일 변경 보전(`입사일 변경 보전(INSADAY_CHANGE_BACKFILL)`). 보전 전용 멱등키 접미사 `_BF{histId}`.
  - 소멸일(AVAIL_TO_DATE) = 새 기준 최신 발생항목의 정상 소멸일 규칙(발생일 + AXIS6).
  - **기존 백필(computeBackfillPeriods)·당기 부여와 이중부여가 안 나도록 재설계** — BACKFILL 경로는 당기/컴포넌트백필 INSERT를 끄고 차액 단건만 부여.
- **[정정 2026-05-26] "기존 부여누적"은 전 STATUTORY 유형(월차 포함)을 센다.** (이전 초안의 "월차 제외"는 오류 — 정답표 §2.1·경계B와 불일치였음.)
  - 산식: live(STATUS!='CANCELED' AND DEL_YN='N') STATUTORY_* 각 행에 대해 **`USED_DAYS + (AVAIL_TO_DATE >= today ? GRANT_DAYS-USED_DAYS : 0)`** 합 = "소멸 제외 + 사용 포함". 월차(STATUTORY_MONTHLY)도 포함.
  - 근거: 정답표 §2.1 "사용한 연차도 부여 누적에 포함(혜택 제공분)" + 경계B(기존 월차 누적 7 차감 → 차액 +8). 월차를 빼면 경계B가 +15로 과다부여됨.
  - 비경계(#1/#7/#13): 과거 월차는 모두 소멸+미사용 → 0 기여, 결과 불변(여전히 +1/+2). 새 기준 누적은 과거변경이라 유효 월차 0 → 본연차/가산만으로 충분(비대칭 무해).
- 경계 케이스(월차→연차 전환, 정답표 경계B): 기존 월차(STATUTORY_MONTHLY)는 유지, 부족분은 STATUTORY_ANNUAL로 보전. 차액 = 새기준 15 − 기존 월차누적 7 = +8.

### D2 — 1년 미만 월차 발생 기준: 절충안
- 현행: 월차=실근속(`actualMonths`, 경력인정 제외) 기준(line 543·817). 본연차/가산=`creditedMonths`(경력인정 포함).
- 정답서 §3: 월차도 산정근속(경력인정 포함) → **채택 안 함**(개근 근거 없는 유령 월차 발생, 위법 소지).
- 결정(절충안): **월차 일수는 실근속 기준 유지. 단 "경력인정으로 인한 고용승계 더블딥"인 경우에만 월차를 발생시키지 않는다.**
- **[정정 2026-05-26] 월차 차단 조건을 좁힘 — "더블딥"으로 한정:**
  > 월차 차단 = (실근속 `actualMonths` < 12) **AND** (경력인정 포함 산정근속 `creditedMonths` ≥ 12) **AND** (이번 부여 entitlement에 full 본연차 15 발생)
  - 이전 초안("본연차 발생 시 차단, FISCAL crossed≥1")은 **과잉**이었음 — 경력인정 없는 정상 FISCAL 비례부여 중도입사자의 법정 월차까지 차단(§8.5.4 위반, prafta-026 검증 동작 후퇴). 예: 2025-07-21 입사 비례부여 → 종전 비례7+월차9=16인데 월차 0으로 후퇴.
  - 좁힌 결과:
    - 정상 근로자(경력인정 0): 산정=실근속 → 조건 거짓 → **월차 보존**.
    - FISCAL 비례본연차(crossed==1 PRORATE, <15): full 15 아님 → 거짓 → 월차 보존.
    - FISCAL 첫 부분기(crossed==0): 본연차 미발생 → 거짓 → 월차 보존(공백 방지).
    - **오직 경력인정으로 실근속<1년인데 full 본연차15를 받는 경우(고용승계 더블딥)만 차단** → 합의한 D2 정신(중복 월차 제거)은 유지, 정상 근로자 법정 월차는 보존.
  - 근거: 본연차 15 ≥ 월차 상한 11 + 즉시 사용 가능 → "더 유리한 처우"라 §60② 위반 아님. **단 이 정당화는 full 15일 때만 성립**(비례 7 등 < 11이면 차단 시 법정 미달이므로 차단 안 함).
  - 구현: `computeMonthlyPeriods` 게이트가 위 3조건일 때만 빈 목록 반환. "full 15 발생" 판정은 `resolveEntitlement` 결과에 STATUTORY_ANNUAL days ≥ 15 컴포넌트가 있는지로.

### D2-B — 1년 미만 월차 소멸: 만 1년 일괄 (2026-05-26 추가 결정)
- 현행: 월차 소멸을 AXIS6(발생일+12개월) per-건으로 처리 → 12~23개월 직원에게 첫해 월차가 잔존(법정 과다 보유). 옵션별 미리보기 "예상 보유"에 이 잔존이 드러나며 발견됨.
- 정답표 §1.4 / 근기법 §60⑦: 1년 미만 월차는 **만 1년 도래일에 일괄 소멸**(입사 1주년 직전). 예: 2023-08-15 입사 → 2024-08-14 소멸.
- 결정(**B**): `computeMonthlyPeriods`에서 월차 AVAIL_TO_DATE = **만 1년 도래일(입사+1년−1일)**. 발생일(AVAIL_FROM)은 그대로. 본연차/가산 유효기간은 불변(AXIS6).
- apply(hireDateGrant=Attd_09 '정책 기준 부여')와 preview가 `computeMonthlyPeriods`를 공유 → 한 곳 수정으로 실제 부여까지 정합. 스케줄러는 AVAIL_TO 기준 만료라 신규 부여분 자동 정합(코드 변경 불요). 기존 DB 월차(구 availTo)는 점진 만료/RESET로 정리(마이그레이션 별도).
- 영향: #6(입사 2025-01-01, 16개월차)·standardModelFiscalRegression past2(2025-02-22, 15개월차) 등 만1년 경과 직원의 잔존 월차가 0으로 정합(정답표값 복원).

### D3 — FISCAL(회계연도) 고용승계자 즉시 본연차: 미포함
- FISCAL은 본연차를 `crossedFiscalStarts`(실제 회계연도 통과)로 게이팅(line 615·639). 고용승계자라도 첫 부분기간엔 `crossed==0` → 본연차 미발생.
- 결정: **즉시 본연차 부여 로직 추가하지 않음.** 현행대로 다음 회계연도 시작에 본연차+가산 발생(가산 tier는 creditedYears 반영). 첫 부분기간은 월차로 커버(D2 게이트가 본연차 미발생 시 월차 유지).
- 중도입사/승계 예외는 **Attd_09 수동부여**로 대응(기존 경로).
- HireDateEditPop 미리보기/영향분석에 **"본연차 다음 회계연도(YYYY-MM-DD) 발생 예정"**을 노출(수동 보강 여부를 관리자가 판단). 수동+자동 이중부여 방지는 관리자 책임(기존 동작 유지).

### D4 — HireDateEditPop.vue (영향도 분석 기반 수정)
- **포함**: 옵션별 시뮬레이션 미리보기(옵션1/2/3 각각 추가/회수/최종보유), 회수표시("본래 회수돼야 할 N일이 있으나 회수하지 않습니다"), FISCAL 다음 회계연도 발생시점 노출.
- **안내문구 수정 수준만**(전용 워크플로우 신설 안 함): 미래변경 시 "옵션1·2 동일 동작", 경계(월차↔연차) 안내, 옵션3(RESET_ALL) "근로자 동의/입력오류 정정 한정" 경고 문구. 변경사유 분기(오류정정/경력인정) 드롭다운·옵션3 동의 워크플로우는 신설 안 함.

### D5 — 데이터/범위
- 신규 테이블 만들지 않음. 기존 `TB_USER_LEAVE_GRANT` / `TB_USER_HIRE_DATE_HISTORY` 재사용. 보전 사유는 `GRANT_REASON`(코드/접두어로 명확히 구분 가능하게) 태깅 + 멱등키로 식별.
- 회계연도 형평성 보전(퇴직 시 입사일 기준 재계산 정산)은 요청서 §9.2상 **2차 제외**.

## 4. 범위 In / Out
- IN: D1(엔진 차액보전), D2(월차 게이트), 옵션별 시뮬레이션 미리보기 API + HireDateEditPop UI(D4), 안내문구, 테스트(정답표 18케이스 + 경계 A/B), 가이드/CHANGELOG/정책 갱신.
- OUT(2차/별건): 엑셀 대량 업로드, 자동 알림, 회계연도 형평성 자동 보전·퇴직정산, FISCAL 즉시 본연차, 변경사유 분기 워크플로우, 옵션3 전자동의.

## 5. 핵심 코드/스키마 위치
- 엔진: `prafta-backend/.../common/cmm/leave/service/impl/LeaveGrantEngineServiceImpl.java`
  - `hireDateGrant`(:115) / `previewPolicyGrant`(:227) / `buildUserPlan`(:445) / `resolveEntitlement`(:533) / `resolveHireDateEntitlement`(:558) / `resolveFiscalEntitlement`(:583) / `computeBackfillPeriods`(:697) / `computeMonthlyPeriods`(:809) / `grantComponent`(:926) / `buildIdempotencyKey`(:991) / `alreadyGranted`(:1015) / `tenureBonusDays`(:1045).
  - 멱등키 = `{userCd}_{periodLabel}_{grantType}{keySuffix}`. KEEP suffix="" / RESET suffix=`_R{histId}`. 레거시 `_HIRE` dual-read.
- 매퍼: `LeaveGrantEngineMapper.xml`, `LeaveDashboardMapper`(count/insert/reactivate/cancel/suffix-variant), `LeaveFlowMapper`(selectDeductibleGrant).
- 영향분석: `User01ServiceImpl`(buildApproxImpact, `estimateBackfillDays` 연동), 엔드포인트 `/webApi/user01/{userCd}/hire-date-impact`.
- 입사일 변경 저장/이력: `User01ServiceImpl.updateUserHireDate` / `User01Mapper.xml`.
- FE: `prafta-web-frontend/prafta-web-frontend/src/views/user/popup/HireDateEditPop.vue`, `HireDateHistoryPop.vue`. Attd_09 = 사용자 연차관리.
- 스키마: `TB_USER_LEAVE_GRANT`(GRANT_ID varchar20, GRANT_TYPE varchar40, GRANT_DAYS dec(5,1), USED_DAYS dec(8,5), GRANT_REASON varchar500, GRANT_BY_TYPE varchar2, STATUS varchar20, AVAIL_FROM/TO varchar8, IDEMPOTENCY_KEY varchar100, EXPIRE_YN…), `TB_USER_HIRE_DATE_HISTORY`(HIST_ID, PREV/NEW_HIRE_DATE varchar8, CHANGE_REASON varchar1000, HANDLING_TYPE varchar30, AFFECTED_GRANT_SNAPSHOT json, APPLIED_YN/DATE/BY).

## 6. 컨벤션/주의
- SQL: leading comma, `#{}` 바인딩, `SELECT *` 금지, 스키마와 100% 일치, MCP `prafta-mysql`로 사전 확인.
- DTO 필드명 대문자 SNAKE_CASE, MyBatis column↔property 명시.
- FE: Vue3+JS(타입스크립트 금지), scoped CSS + CSS 변수, 공통 컴포넌트 우선.
- 주석/로그 한국어, 식별자 영어. Bash heredoc 금지·비대화형 옵션·타임아웃.
- prafta-029의 RESET 회차키 누수/재활성화 가드(`countActiveBySuffixVariant`, `countLiveByIdempotencyKey`, `reactivateCanceledGrant`)를 깨지 말 것. 차액 보전 INSERT가 이 멱등 구조와 충돌하지 않게 별도 멱등키 체계 설계.
- 런타임(앱+DB) 시나리오 검증은 사용자 환경 몫. 에이전트는 컴파일 + 결정적 단위테스트(mockStatic(LocalDate))까지.
