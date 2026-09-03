-- ============================================================================
-- prafta-063 : 사고관리 법정절차 마스터 근거조문 정정 + 과태료 금액 표기 제거 (1) DML
-- 근거: 요청서 사고관리_개선사항_정리.md §1-1·§2-2 / 정책서 safety/06-accident.md §6.2
-- 대상: TB_ACCT_LEGAL_STEP_MASTER (전사 공통 마스터, CMPNY_CD 없음)
-- 선행: prafta-057-acct-legal-step-2-data.sql 적용 상태(개발·운영 동일 확인 2026-09-03)
-- ============================================================================
-- ⚠️ 노무사 검토 게이트 ⚠️
--   조문 번호는 법령정보센터(law.go.kr) 원문 대조로 정리한 것이나 법적 검증이 완료되지
--   않았다. 운영 적용 전 노무사(또는 공인노무사)의 확인을 받은 뒤 적용한다.
--   과태료 "금액"은 본 파일에서 전부 제거한다(사용자 결정 2026-09-03):
--   실제 부과액은 시행령 [별표 35] 에서 위반 횟수·사업장 규모별로 달라지므로 화면에
--   단일 금액을 표시하면 오해를 부른다. 화면 ②탭 범례에 §175·별표 35 원문 링크를 둔다.
-- ============================================================================
-- 적용 원칙: 개발 DB · 운영 DB 동시 적용 (한쪽만 적용 시 화면 문구 불일치).
--            Workbench 로 사용자가 직접 실행. Claude 는 실행하지 않는다.
-- 적용 전 확인:
--   SELECT STEP_CD, LEGAL_BASIS, UPDATE_NO FROM TB_ACCT_LEGAL_STEP_MASTER
--    WHERE STEP_CD IN ('STEP_INIT','STEP_CRIT_REPORT','STEP_CRIT_INVST','STEP_NORM_INVST',
--                      'STEP_INVESTIGATE','STEP_EXEMPT_REC');
--   -- UPDATE_NO 가 전부 'prafta-057' 이어야 본 파일 적용 대상
-- 멱등성: 동일 값 재적용은 무해(UPDATE_DATE 만 갱신). 적용 후 BE 재기동 불필요(문구만 변경).
-- ============================================================================

-- 1) STEP_INIT (ALL, 초기 조치/응급) : §54① 은 조문 자체가 "중대재해가 발생한 경우" 한정이라
--    전등급 공통 행의 근거로 부적합. 전등급 적용 가능한 §51(사업주의 작업중지 —
--    산업재해가 발생할 급박한 위험이 있을 때 즉시 작업 중지)로 교체하고 중대재해 시 §54① 을 병기.
UPDATE TB_ACCT_LEGAL_STEP_MASTER
   SET LEGAL_BASIS  = '산안법 §51(급박한 위험 시 작업중지) · 중대재해 시 §54①'
     , UPDATE_NO    = 'prafta-063'
     , UPDATE_DATE  = NOW()
 WHERE STEP_CD = 'STEP_INIT';

-- 2) STEP_CRIT_REPORT (100, 중대재해 발생보고) : 금액(3,000만원)·기준일 제거 → 부과 대상 사실 + §175 만
UPDATE TB_ACCT_LEGAL_STEP_MASTER
   SET LEGAL_BASIS  = '산안법 §54② · 미보고 시 과태료 부과 대상(§175)'
     , UPDATE_NO    = 'prafta-063'
     , UPDATE_DATE  = NOW()
 WHERE STEP_CD = 'STEP_CRIT_REPORT';

-- 3) 산업재해조사표 제출 (100 STEP_CRIT_INVST · 200 STEP_NORM_INVST) : 금액(1,500만원 이하)·기준일 제거
UPDATE TB_ACCT_LEGAL_STEP_MASTER
   SET LEGAL_BASIS  = '산안법 §57③ / 시행규칙 §73 · 미제출 시 과태료 부과 대상(§175)'
     , UPDATE_NO    = 'prafta-063'
     , UPDATE_DATE  = NOW()
 WHERE STEP_CD IN ('STEP_CRIT_INVST', 'STEP_NORM_INVST');

-- 4) 기록·보존 (ALL STEP_INVESTIGATE · 300 STEP_EXEMPT_REC) : 요청서 §2-2 — 위반 시 과태료 부과 대상임을 병기(금액 없음)
UPDATE TB_ACCT_LEGAL_STEP_MASTER
   SET LEGAL_BASIS  = '산안법 §57② / 시행규칙 §72 · 기록·보존 의무(3년) · 위반 시 과태료 부과 대상(§175)'
     , UPDATE_NO    = 'prafta-063'
     , UPDATE_DATE  = NOW()
 WHERE STEP_CD = 'STEP_INVESTIGATE';

UPDATE TB_ACCT_LEGAL_STEP_MASTER
   SET LEGAL_BASIS  = '산안법 §57② / 시행규칙 §72 · 기록·보존 의무(3년, 3일 미만 휴업도 기록 대상) · 위반 시 과태료 부과 대상(§175)'
     , UPDATE_NO    = 'prafta-063'
     , UPDATE_DATE  = NOW()
 WHERE STEP_CD = 'STEP_EXEMPT_REC';

-- 적용 후 확인 (6행 모두 UPDATE_NO = 'prafta-063', LEGAL_BASIS 에 '만원' 문자열 0건):
--   SELECT STEP_CD, ACCT_GRADE_CD, LEGAL_BASIS, UPDATE_NO FROM TB_ACCT_LEGAL_STEP_MASTER
--    WHERE STEP_CD IN ('STEP_INIT','STEP_CRIT_REPORT','STEP_CRIT_INVST','STEP_NORM_INVST',
--                      'STEP_INVESTIGATE','STEP_EXEMPT_REC');
--   SELECT COUNT(*) FROM TB_ACCT_LEGAL_STEP_MASTER WHERE LEGAL_BASIS LIKE '%만원%'; -- 0
