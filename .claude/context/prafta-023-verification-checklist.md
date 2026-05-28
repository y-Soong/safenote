# prafta-023 검증 체크리스트 (런타임 테스트 → 일괄 피드백용)

> 2026-05-24 구현분(prafta-023 A~H + follow-up #1/#2/#3). **전부 컴파일 + 순수로직 단위테스트 통과**.
> 단, 실제 직원·부여 데이터로 도는 **런타임 시나리오는 미검증(이 항목들)** — 로컬에서 확인 후 묶어서 피드백 주세요.
> 운영 DB 변경 없음(멱등키 dual-read). 자동 배치는 기본 비활성(게이트).

## 0. 사전 / 테스트 방법
- 빌드: `gradlew.bat -p prafta-backend compileJava --no-daemon --offline`
- 순수로직 테스트: `gradlew.bat -p prafta-backend test --tests "com.prafta.common.cmm.leave.service.impl.LeaveGrantEngineProrationTest" --no-daemon --offline`
  - ※ `test`(전체)는 `DemoApplicationTests`(@SpringBootTest)가 DB를 요구하므로 `--tests`로 한정 권장.
- 화면: 사용자 연차관리(Attd_09) "정책 기준 부여"(프리뷰→적용), 사용자관리→사용자정보→입사일 수정(HireDateEditPop).
- 부여 확인 쿼리(§끝 SQL 참조). **대상은 USER_CD 기준**(USER_ID 아님).

---

## 1. 항목별 확인 리스트

### A. 멱등키 dual-read (이중부여 차단)
- [ ] 같은 직원에 "정책 기준 부여"를 **2번** 실행 → 두 번째는 추가 부여 0 (중복 없음).
- [ ] prafta-022 시절 `_HIRE` 키로 이미 부여된 직원 → 신규 키 전환 후에도 **이중부여 안 됨**.
- 기대: `TB_USER_LEAVE_GRANT`에 같은 (연도·유형) 부여가 1건만.
	>> 정상

### C. 본연차/근속 소급 백필 — HIRE_DATE 정책 ((i)+유효기간)
- [ ] 입사일을 **과거로** 정정 + 처리방식 **BACKFILL** → Attd_09 정책 기준 부여 → 유효기간(AXIS6) 내 **과거 누락 본연차(+근속가산) 소급** 부여.
- [ ] 처리방식 **APPLY_NEW** → 과거 소급 **안 함**(당기분만). ← 두 KEEP이 달라졌는지 핵심 확인.
- [ ] **유효기간 지난(소멸) 과거분은 부여 안 됨**(예: 12개월 유효인데 2년 전 발생분).
- 기대: 프리뷰 "추가 예정 N일(소급 X / 월차 Y)" 표기, 적용 후 GRANT 증가.
	>> 오류, 20260400013 사용자의 입사일을 2022. 11월 달에서 2022. 05. 03으로 변경(기존 입사일로 이미 연차를 부여받은 상태에서 수행).  BACKFILL 처리방식으로 입사일 수정 후 Attd_09 정책 기준 부여 했더니 기존 16개의 연차에 + 16개가 더 추가돼서 32개가 됨
	>> [prafta-028 분석] 직접 원인 2가지. ① 활성 정책 AXIS6=24개월(연장)이라 3년차(소멸 전)+4년차가 동시 유효 → 사용자 케이스(5월 입사)의 32는 사실상 의도된 값. ② **라벨 버그**: 당기부여는 멱등키를 '달력연도'(now().getYear)로, 백필은 '기념일연도'(accrual.getYear)로 매겨 같은 근속연차가 두 키로 갈려 이중부여 — 입사월이 6~12월(올해 기념일 미도래)인 직원에서 발현(예: 11월 입사면 47일).
	>> [prafta-028 수정] (A안) AXIS6 12개월 법정 고정(24옵션 폐지: Baim_07 + LeavePolicyServiceImpl 검증 + 마이그레이션 `prafta-028-axis6-validity-12-fixed.sql`). (C) 당기부여 라벨을 `latestAnniversaryYearLabel`(최근 기념일연도)로 통일 + 백필 제외 라벨 동일화 + 기존 달력연도 키 dual-read 전환가드(재키잉 마이그레이션 불요). 회귀 테스트 `c_labelBug_novemberHire_noDuplicate`(11월 입사 47→31) 추가. ※ 기존에 24개월/달력연도로 부여된 GRANT 행은 §8.5.8 #2(사후 차감 금지)로 보존 — 깨끗한 재검증은 해당 테스트 GRANT만 삭제 후 재부여.

### #2. FISCAL_YEAR 과거 백필
- [ ] AXIS1=FISCAL_YEAR 정책 + 입사일 과거로 + BACKFILL → 입사 후 도래한 **과거 회계연도(당해 제외)** 본연차 소급(유효기간 내).
- 기대: 회계연도 시작일 기준 발생, 당해 회계연도분은 당기 부여가 담당(중복 없음).

### D / #3. PRORATE 비례부여 + AXIS4 (HALF_DAY 0.5 포함)
- [ ] FISCAL_YEAR + AXIS3=PRORATE + **첫 부분기간** 직원(입사 후 첫 회계연도 미도래) → 비례 본연차 `(입사~다음 회계연도 시작 일수÷365)×15` 부여.
- [ ] AXIS4 **CEIL/ROUND/FLOOR** 각각 반올림 결과 확인.
- [ ] AXIS4 **HALF_DAY** → **0.5일 단위** 부여(예 3.78→3.5). `GRANT_DAYS`에 .5 저장되는지.
  - ⚠️ HALF_DAY는 정책상 `TB_LEAVE_USAGE_POLICY.ALLOW_HALF_DAY='Y'` 강제(§8.5.3) — 사용 단위 정책도 0.5 허용인지 함께 확인.

### #1. 월차 per-월 누적
- [ ] **1년 미만 신규 직원**: 매월(완성 시) 1일씩 누적되는지(키 `{userCd}_{YYYYMM}_STATUTORY_MONTHLY`). 같은 달 재실행 시 중복 없음.
- [ ] **레거시(연 집계 월차 보유) 직원**: 그 해 per-월이 **중복 부여되지 않음**(상호배타). ← 중요
- [ ] **RESET_ALL**: 기존 집계 취소 후 per-월로 재발급되는지.
- [ ] 1년 이상 직원: 1년차 월차(최대 11)가 유효기간 내면 소급되는지(+ 본연차 15 별도).
- 한계: 레거시 ACTIVE 집계 보유 연도는 만료/RESET 전까지 집계 유지(완전 전환은 일회성 마이그레이션 — 미적용).

### E. 자동 정기부여 배치 (게이트)
- [ ] **기본 비활성 확인**: 설정 없이 배치 시각 도래 → 부여 0, 로그 "비활성 — 건너뜀".
- [ ] `prafta.leave.auto-grant.enabled=true` 설정 후 → 활성 정책 회사 직원에게 멱등 부여(중복 없음). cron 기본 매일 00:30(`prafta.leave.auto-grant.cron` 재정의 가능).
- ⚠️ **엔진 결과(위 항목들) 확인 전에는 켜지 말 것** — 무인 대량부여.

### F. 입사일 변경 영향분석 정합
- [ ] 입사일 수정 팝업 "누락된 부여(변경 후 기준)" 수치가 **실제 BACKFILL 소급 일수와 일치**(HIRE_DATE 정책). FISCAL/미래 입사일은 0.

### G. 프론트/문서
- [ ] HireDateEditPop 처리방식 3종 설명이 실제 동작과 일치(소급/당기/재계산 + "정책 기준 부여에서 처리").
- [ ] PolicyGrantPreviewPop 프리뷰 행에 추가 일수/노트 표시.
- [ ] PRORATE 정책이어도 "비례부여 미적용·차년도 일괄" 안내가 **더 이상 안 뜸**(구현됨).

### H. EXPIRE_YN Phase1 (읽기 의존 제거)
- [ ] 연차 차감 시 **만료 부여가 여전히 제외**되는지(STATUS=EXPIRED 또는 AVAIL_TO_DATE 경과 부여는 차감 후보에서 빠짐).
- 변경: `selectDeductibleGrant`에서 redundant `EXPIRE_YN='N'` 제거(STATUS='ACTIVE'로 충분). 동작 불변이어야 함.

---

## 2. 검증 SQL (로컬/개발만, USER_CD 기준)
```sql
-- 부여 현황 + 멱등키 (per-월 YYYYMM / 연 YYYY / 비례 .5 확인)
SELECT GRANT_ID, GRANT_TYPE, GRANT_DAYS, USED_DAYS, STATUS,
       AVAIL_FROM_DATE, AVAIL_TO_DATE, IDEMPOTENCY_KEY, GRANT_DATE
  FROM TB_USER_LEAVE_GRANT
 WHERE CMPNY_CD = :cmpny AND USER_CD = :userCd
   AND GRANT_TYPE LIKE 'STATUTORY\_%'
 ORDER BY GRANT_DATE DESC, IDEMPOTENCY_KEY;

-- 입사일 변경 이력(처리방식/적용여부)
SELECT HIST_ID, PREV_HIRE_DATE, NEW_HIRE_DATE, HANDLING_TYPE, APPLIED_YN, APPLIED_DATE, INSERT_DATE
  FROM TB_USER_HIRE_DATE_HISTORY
 WHERE CMPNY_CD = :cmpny AND USER_CD = :userCd
 ORDER BY INSERT_DATE DESC;
```

## 3. 알려진 한계 / 후속 (테스트 시 참고)
- **#1 레거시 집계 월차 전환**: 레거시 ACTIVE 연-집계 보유 연도는 per-월 미적용(상호배타). 완전 전환은 일회성 마이그레이션 필요(미적용).
- **H Phase2(EXPIRE_YN 컬럼 DROP)**: 외부(앱/리포트) 읽기 점검 + 승인 후 별도(`prafta-023-H-plan.md` Phase2).
- **HALF_DAY**: 사용 단위 정책(ALLOW_HALF_DAY) 연계는 본 작업 범위 밖(부여 측만 0.5 지원).
- 출처/현황: `requests/prafta-023-plan.md §11`, `requests/prafta-023-H-plan.md`.

## 4. 피드백 주실 때
항목별 [기대 vs 실제] + 재현 조건(정책 AXIS 설정, 직원 입사일/근속, 처리방식)을 적어주시면 한두 번 후처리로 수렴 가능합니다.
