-- ============================================================
-- PRAFTA-010-2-005 테스트 데이터 코드값화 스크립트
-- 대상 사용자: CMPNY 001 / SITE 00001 / NODE n1 / USER 20260400010
-- INSERT_NO: 'P0101TST'  (원복 식별자)
-- 배경 : prafta-010-1-021-testdata.sql 의 TB_USER_ATTD_REQ 4행이
--        REQ_TYPE/REQ_STATUS 를 enum 문자열로 적재했다. PRAFTA-010-2 에서
--        SYS032/SYS033 코드값으로 전환했으므로 기존 4행을 코드값으로 갱신한다.
-- 매핑 : REQ_TYPE   ATTD_CREATE->'01' / ATTD_MODIFY->'02'
--        REQ_STATUS REQUESTED->'01'
-- 방식 : 기존 P0101TST REQ 행의 ID(REQ_ID/TARGET_ID 등)는 유지하고
--        REQ_TYPE/REQ_STATUS 값만 UPDATE 한다 (ID 재채번 없음).
-- ============================================================

START TRANSACTION;

-- ----- 멱등 보장: 이미 enum 문자열이거나 코드값인 P0101TST 4행을 코드값으로 통일 -----

-- 근태수정 요청: ATTD_MODIFY -> '02'
UPDATE TB_USER_ATTD_REQ
   SET REQ_TYPE = '02'
 WHERE INSERT_NO = 'P0101TST'
   AND REQ_TYPE  = 'ATTD_MODIFY';

-- 근태생성 요청: ATTD_CREATE -> '01'
UPDATE TB_USER_ATTD_REQ
   SET REQ_TYPE = '01'
 WHERE INSERT_NO = 'P0101TST'
   AND REQ_TYPE  = 'ATTD_CREATE';

-- 요청 상태: REQUESTED -> '01'
UPDATE TB_USER_ATTD_REQ
   SET REQ_STATUS = '01'
 WHERE INSERT_NO  = 'P0101TST'
   AND REQ_STATUS = 'REQUESTED';

COMMIT;

-- ----- 결과 확인 -----
SELECT 'REQ' AS t, REQ_ID, REQ_TYPE, REQ_STATUS, WORK_YMD, WORK_SEQ
  FROM TB_USER_ATTD_REQ
 WHERE INSERT_NO = 'P0101TST'
 ORDER BY WORK_YMD;

-- ============================================================
-- 원복 SQL (P0101TST - 코드값을 enum 문자열로 되돌림)
--   START TRANSACTION;
--   UPDATE TB_USER_ATTD_REQ SET REQ_TYPE='ATTD_MODIFY'
--     WHERE INSERT_NO='P0101TST' AND REQ_TYPE='02';
--   UPDATE TB_USER_ATTD_REQ SET REQ_TYPE='ATTD_CREATE'
--     WHERE INSERT_NO='P0101TST' AND REQ_TYPE='01';
--   UPDATE TB_USER_ATTD_REQ SET REQ_STATUS='REQUESTED'
--     WHERE INSERT_NO='P0101TST' AND REQ_STATUS='01';
--   COMMIT;
-- ============================================================
