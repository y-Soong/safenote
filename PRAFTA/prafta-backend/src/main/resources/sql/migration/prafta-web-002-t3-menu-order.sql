-- ============================================================================
-- PRAFTA-WEB-002-T3 — 산업안전 LNB 메뉴 순서 교정 (ChkLst_02 <-> ChkLst_03 스왑)
-- 작성일: 2026-06-30
-- 적용 환경: MySQL 8.0.42
--
-- 목적:
--   산업안전(MENU_M_ID='safety') 그룹에서 ChkLst_02 와 ChkLst_03 의 노출 순서를
--   서로 맞바꾼다. ChkLst_02: MENU_IDX 3 -> 2, ChkLst_03: MENU_IDX 2 -> 3.
--   MENU_NM / MENU_VIEW / SUB_GROUP_IDX 등 다른 컬럼은 변경하지 않는다.
--
-- 대상:
--   tb_syst_menu_d (PK = MENU_D_ID, MENU_M_ID)
--   - WHERE 절은 PK 정합을 위해 MENU_D_ID 와 MENU_M_ID='safety' 를 함께 명시.
--   - MENU_IDX 에 UNIQUE 인덱스가 없어 임시값 회피(3-step)가 불필요하며,
--     단순 2-UPDATE 로 안전하게 스왑 가능하다.
--
-- 적용 순서: 단독·독립 (선행/후행 마이그 의존 없음).
--
-- 멱등성: 동일 값으로 재설정해도 무해(이미 적용된 환경에서 재실행 시 영향 0행 또는 동일값).
--
-- 롤백 SQL:
--   UPDATE tb_syst_menu_d SET MENU_IDX = 3, UPDATE_NO = 'SYSTEM', UPDATE_DATE = NOW()
--    WHERE MENU_D_ID = 'ChkLst_02' AND MENU_M_ID = 'safety';
--   UPDATE tb_syst_menu_d SET MENU_IDX = 2, UPDATE_NO = 'SYSTEM', UPDATE_DATE = NOW()
--    WHERE MENU_D_ID = 'ChkLst_03' AND MENU_M_ID = 'safety';
-- ============================================================================

UPDATE tb_syst_menu_d SET MENU_IDX = 2, UPDATE_NO = 'SYSTEM', UPDATE_DATE = NOW()
 WHERE MENU_D_ID = 'ChkLst_02' AND MENU_M_ID = 'safety';
UPDATE tb_syst_menu_d SET MENU_IDX = 3, UPDATE_NO = 'SYSTEM', UPDATE_DATE = NOW()
 WHERE MENU_D_ID = 'ChkLst_03' AND MENU_M_ID = 'safety';
