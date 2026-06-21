-- ============================================================================
-- PRAFTA-ATTD02 — 휴일 레거시 등록자코드('ADMIN') 보정 백필
-- 작성일: 2026-06-19
-- 적용 환경: MySQL 8.0.42
-- 참조: 휴일관리(Attd_02) "등록 주체 = 시스템" 표기 이슈 (채팅 요청, 작업 2)
--
-- 목적
--  TB_HOLIDAY / TB_HOLIDAY_RULE 의 레거시 시드 데이터 중 INSERT_NO 가
--  실제 USER_CD 가 아닌 placeholder 문자열 'ADMIN' 으로 박힌 행을,
--  해당 회사의 실재 master 계정(USER_CD)으로 보정한다.
--  보정 후 화면의 FNC_CMM_INFO_SRCH(..., 'USER_NM', INSERT_NO, ...) 가
--  등록자명을 정상 해석하여 "등록 주체"가 실제 담당자명으로 노출된다.
--
-- 범위(2026-06-19 기준 확인): CMPNY_CD='001'
--   TB_HOLIDAY       INSERT_NO='ADMIN' → 2건
--   TB_HOLIDAY_RULE  INSERT_NO='ADMIN' → 2건
-- 보정 대상 계정: '20260400010' (CMPNY_CD='001', AUTH_CD='master', 활성, 시스템관리자)
--
-- 주의
--  - 공휴일(HOLIDAY_TYPE='01', INSERT_NO='SYSTEM')은 의도된 시스템 시드이므로 본 백필 대상이 아니다.
--    (화면 표기는 프론트 regByLabel 에서 "국가공휴일(기본 제공)"으로 분리 노출 — 작업 1)
--  - ⚠️ 운영 적용 금지(파일만). 적용 전 아래 사전 점검으로 대상/대상계정을 확인할 것.
--  - 다른 회사(CMPNY_CD)에서 동일한 'ADMIN' placeholder 가 추가로 발견되면,
--    그 회사의 master USER_CD 로 동일 패턴을 별도 추가할 것(아래는 '001' 한정).
-- ============================================================================

-- [사전 점검 1] 보정 대상 행 확인
-- SELECT 'TB_HOLIDAY' AS tbl, CMPNY_CD, HOLIDAY_ID AS id, HOLIDAY_NM AS nm, HOLIDAY_TYPE, INSERT_NO
--   FROM TB_HOLIDAY      WHERE INSERT_NO = 'ADMIN' AND CMPNY_CD = '001'
-- UNION ALL
-- SELECT 'TB_HOLIDAY_RULE', CMPNY_CD, HOLIDAY_RULE_ID, HOLIDAY_RULE_NM, HOLIDAY_TYPE, INSERT_NO
--   FROM TB_HOLIDAY_RULE WHERE INSERT_NO = 'ADMIN' AND CMPNY_CD = '001';

-- [사전 점검 2] 보정 대상 계정이 유효(활성)한지 확인
-- SELECT CMPNY_CD, USER_CD, AUTH_CD, ACCOUNT_STATUS,
--        FNC_CMM_INFO_SRCH(CMPNY_CD, 'USER_NM', USER_CD, NULL) AS nm
--   FROM TB_USER WHERE CMPNY_CD = '001' AND USER_CD = '20260400010';

-- ---------------------------------------------------------------------------
-- 보정 DML
-- ---------------------------------------------------------------------------
UPDATE TB_HOLIDAY
   SET INSERT_NO   = '20260400010'
     , UPDATE_NO   = 'SYSTEM'
     , UPDATE_DATE = NOW()
 WHERE CMPNY_CD  = '001'
   AND INSERT_NO = 'ADMIN';

UPDATE TB_HOLIDAY_RULE
   SET INSERT_NO   = '20260400010'
     , UPDATE_NO   = 'SYSTEM'
     , UPDATE_DATE = NOW()
 WHERE CMPNY_CD  = '001'
   AND INSERT_NO = 'ADMIN';

-- ---------------------------------------------------------------------------
-- [롤백 참고] 보정을 되돌리려면 (단, 'ADMIN' 으로 환원만 가능 — UPDATE_DATE 원복 불가)
-- UPDATE TB_HOLIDAY      SET INSERT_NO = 'ADMIN'
--   WHERE CMPNY_CD = '001' AND INSERT_NO = '20260400010' AND HOLIDAY_TYPE = '02';
-- UPDATE TB_HOLIDAY_RULE SET INSERT_NO = 'ADMIN'
--   WHERE CMPNY_CD = '001' AND INSERT_NO = '20260400010';
-- ⚠️ 환원 시 정상 등록한 다른 행(예: 테스트1/2, HOLIDAY_TYPE='02')과 USER_CD 가 겹칠 수 있으니
--    HOLIDAY_ID 를 직접 지정해 환원할 것.
-- ============================================================================
