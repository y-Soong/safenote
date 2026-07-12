-- ============================================================================
-- prafta-057 : 사고관리 법정 처리/기한 탭 개편 (2) DML (seed 정정)
-- 근거: 작업지시서 prafta-057.md §2 (좌→우 교체값). 조문/과태료/기한 기준일 2026.06.
-- ※ 단일출처 문서(사고관리_6단계_법령매칭.md)가 워크스페이스에 없어 §2 명시값을 권위로 사용.
-- 선행: prafta-057-acct-legal-step-1-ddl.sql (STEP_TYPE 컬럼) 적용 필수.
-- 적용 후 BE 재기동(쿼리 SELECT 컬럼 추가 동반).
-- ============================================================================

-- 1단계 초기 조치/응급 (ALL) : "현장 보존" 제거, 조문 §54①
UPDATE TB_ACCT_LEGAL_STEP_MASTER
   SET ACTION_GUIDE = '작업 즉시 중지 · 부상자 처치 · 2차 재해 방지 조치를 즉시 시행하세요.'
     , LEGAL_BASIS  = '산안법 §54①'
     , STEP_TYPE    = 'PROCESS'
     , UPDATE_NO    = 'prafta-057'
     , UPDATE_DATE  = NOW()
 WHERE STEP_CD = 'STEP_INIT';

-- 2단계 중대재해 발생보고 (100) : 미검증 시행규칙 §67 제거, 조치·전망 풀이 병기, 과태료 기준일 병기
UPDATE TB_ACCT_LEGAL_STEP_MASTER
   SET ACTION_GUIDE = '재해개요·피해상황·조치·전망(=현재까지 취한 조치 / 향후 예상·계획)을 관할 지방고용노동관서에 보고하세요.'
     , LEGAL_BASIS  = '산안법 §54② · 미이행 과태료 3,000만원 (기준일 2026.06)'
     , STEP_NOTE    = '시스템이 기한을 계산하지 않습니다. 즉시 보고 후 완료 처리하세요.'
     , STEP_TYPE    = 'PROCESS'
     , UPDATE_NO    = 'prafta-057'
     , UPDATE_DATE  = NOW()
 WHERE STEP_CD = 'STEP_CRIT_REPORT';

-- 3단계 산업재해조사표 제출 (100·200) : 조문 §57③/시행규칙§73, 과태료 1,500만원 이하 기준일 병기
UPDATE TB_ACCT_LEGAL_STEP_MASTER
   SET ACTION_GUIDE = '관할 지방고용노동관서에 산업재해조사표를 제출하세요(중대재해도 별도 제출).'
     , LEGAL_BASIS  = '산안법 §57③ / 시행규칙 §73 · 미제출 과태료 1,500만원 이하 (기준일 2026.06)'
     , STEP_NOTE    = '근로자대표 확인 필요(없으면 재해자 본인 확인).'
     , STEP_TYPE    = 'PROCESS'
     , UPDATE_NO    = 'prafta-057'
     , UPDATE_DATE  = NOW()
 WHERE STEP_CD = 'STEP_CRIT_INVST';

UPDATE TB_ACCT_LEGAL_STEP_MASTER
   SET ACTION_GUIDE = '관할 지방고용노동관서에 산업재해조사표를 제출하세요.'
     , LEGAL_BASIS  = '산안법 §57③ / 시행규칙 §73 · 미제출 과태료 1,500만원 이하 (기준일 2026.06)'
     , STEP_NOTE    = '근로자대표 확인 필요(없으면 재해자 본인 확인).'
     , STEP_TYPE    = 'PROCESS'
     , UPDATE_NO    = 'prafta-057'
     , UPDATE_DATE  = NOW()
 WHERE STEP_CD = 'STEP_NORM_INVST';

-- 4단계 근로복지공단 요양급여 신청 (100·200) : 참고 항목 전환, 조문 §41(신청)·§116
UPDATE TB_ACCT_LEGAL_STEP_MASTER
   SET ACTION_GUIDE = '요양급여 신청은 근로복지공단 소관이며 재해자·유족이 신청합니다. 회사는 재해발생 경위 확인(서명·날인) 요청 시 협조하세요.'
     , LEGAL_BASIS  = '산재보상보험법 §41(신청) · §116(회사의 조력·증명 의무)'
     , STEP_TYPE    = 'REFERENCE'
     , UPDATE_NO    = 'prafta-057'
     , UPDATE_DATE  = NOW()
 WHERE STEP_CD IN ('STEP_COMP_CLAIM_100', 'STEP_COMP_CLAIM_200');

-- 5단계 사고 조사/재발방지 (ALL) : §57①(오기) → §57②/시행규칙§72(3년), 갈음 안내(자동완료 금지)
UPDATE TB_ACCT_LEGAL_STEP_MASTER
   SET ACTION_GUIDE = '원인 분석·재발방지 대책 수립 후 기록·보존하세요.'
     , LEGAL_BASIS  = '산안법 §57② / 시행규칙 §72 · 기록·보존 의무 (3년)'
     , STEP_NOTE    = '산업재해조사표 사본을 보존하면 별도 기록·보존을 갈음할 수 있음(시행규칙 §72 단서). 자동 완료 처리되지 않으니 직접 확인 후 완료하세요.'
     , STEP_TYPE    = 'PROCESS'
     , UPDATE_NO    = 'prafta-057'
     , UPDATE_DATE  = NOW()
 WHERE STEP_CD = 'STEP_INVESTIGATE';

-- 신고제외(300) 재해 기록·보존 : 동일한 §57①(오기) → §57②/시행규칙§72 정정 (기록·보존은 §57②)
UPDATE TB_ACCT_LEGAL_STEP_MASTER
   SET LEGAL_BASIS  = '산안법 §57② / 시행규칙 §72 · 기록·보존 의무 (3년, 3일 미만 휴업도 기록 대상)'
     , STEP_TYPE    = 'PROCESS'
     , UPDATE_NO    = 'prafta-057'
     , UPDATE_DATE  = NOW()
 WHERE STEP_CD = 'STEP_EXEMPT_REC';

-- 6단계 보상/합의 (100·200) : 참고 항목(최소 안내)로 전환, 조문 제거(민사 영역)
UPDATE TB_ACCT_LEGAL_STEP_MASTER
   SET ACTION_GUIDE = '※ 보상·합의는 민사 영역으로, 사고관리에서 별도 관리하지 않습니다.'
     , LEGAL_BASIS  = NULL
     , STEP_NOTE    = NULL
     , STEP_TYPE    = 'REFERENCE'
     , UPDATE_NO    = 'prafta-057'
     , UPDATE_DATE  = NOW()
 WHERE STEP_CD IN ('STEP_SETTLE_100', 'STEP_SETTLE_200');
