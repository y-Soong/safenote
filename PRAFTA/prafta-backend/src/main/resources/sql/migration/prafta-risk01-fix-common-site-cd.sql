-- ============================================================================
-- PRAFTA-RISK01 — 유해위험 구분 관리 공통항목 SITE_CD 정합 (1회성 데이터 보정)
-- 작성일: 2026-06-24
-- 적용 환경: MySQL 8.0.42
-- 참조: 채팅 요청 — Risk_01.vue "기타" 공정 공통관리 항목 체크박스 노출 결함
--
-- 배경
--  Risk_01.vue 초기 진입 시 commonChk=true 이지만 siteCd 에 세션 사업장코드가
--  남아있어, 신규 "기타" 공정(PROCESS_CD='00005')에 공통관리로 추가한 위험분류 6건이
--  SITE_CD=NULL 이 아닌 SITE_CD='00001' 로 저장되었다.
--  → 공통관리 해제 후 사업장 개별 조회 시 해당 공통 항목이 사업장 항목으로 오인되어
--    체크박스가 노출/편집 가능해지는 결함.
--  코드 수정(프론트 fnAddRow_fst/fnAddRow_sec + 매퍼 NULLIF)으로 재발은 차단되며,
--  본 SQL 은 이미 잘못 저장된 기존 데이터의 1회 정합용이다.
--
-- ⚠️ 운영 적용 전 아래 [사전 점검] 으로 대상 건수(6건 예상)를 반드시 확인할 것.
--    "기타" 공정에 의도된 사업장 개별 항목이 별도로 존재한다면 범위를 좁혀 적용한다.
-- ============================================================================

-- [사전 점검] 보정 대상 확인 (예상 6건: 00026~00031)
-- SELECT CMPNY_CD, RISK_TYPE_CD, RISK_TYPE_NM, SITE_CD, PROCESS_CD
-- FROM TB_RISK_TYPE
-- WHERE CMPNY_CD = '001'
--   AND PROCESS_CD = '00005'
--   AND SITE_CD = '00001';

-- [보정] "기타" 공정 공통관리 항목의 SITE_CD 를 NULL 로 정정한다.
UPDATE TB_RISK_TYPE
SET SITE_CD     = NULL
    , UPDATE_NO   = 'SYSTEM'
    , UPDATE_DATE = NOW()
WHERE CMPNY_CD = '001'
  AND PROCESS_CD = '00005'
  AND SITE_CD = '00001';

-- [참고] 위 위험분류에 종속된 공통 유해위험상황이 동일 결함으로 저장되었을 수 있으므로
--        함께 정정한다(현재는 0건). 사업장 개별 항목이 섞여 있지 않은지 사전 확인 후 적용.
-- UPDATE TB_RISK_SITE_HAZARD
-- SET SITE_CD     = NULL
--     , UPDATE_NO   = 'SYSTEM'
--     , UPDATE_DATE = NOW()
-- WHERE CMPNY_CD = '001'
--   AND RISK_TYPE_CD IN ('00026','00027','00028','00029','00030','00031')
--   AND SITE_CD = '00001';
