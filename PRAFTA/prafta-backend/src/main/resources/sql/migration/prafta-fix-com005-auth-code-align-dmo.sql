-- ============================================================================
-- PRAFTA-FIX — DMO 회사 COM005(권한) 코드값 표준화
-- 작성일: 2026-07-01
-- 적용 환경: MySQL 8.0.42
-- 대상 회사: CMPNY_CD = 'OOqRuIQHrnqp0qGAs1oe' (DMO_ADMIN 소속)
--
-- 증상
--  DMO_ADMIN 으로 로그인 → User_01(사용자관리) 화면의 표 "권한" 컬럼이 공란으로 표시.
--
-- 원인
--  User_01 화면의 권한 컬럼은 드롭다운으로, 옵션 value = TB_BAIM_VAL_D COM005 의
--  BAIM_VAL_D_CD 이고, 표시 조건은 이 value 가 사용자 TB_USER.AUTH_CD 와 정확히
--  일치할 때만 라벨이 렌더된다. 시스템 전체(TB_USER.AUTH_CD, TB_SYST_AUTH_MENU.AUTH_CD)는
--  표준 코드체계(system/master/hr/safe/99999)를 쓰는데, DMO 회사의 COM005 만
--  임의값(00001~00005)으로 시드되어 매칭 실패 → 공란.
--  같은 이유로 FNC_CMM_INFO_SRCH(..,'AUTH_LEVEL',..) 도 SORT_IDX 를 못 찾아 빈값 반환
--  (행 잠금 판정 authLevel 도 함께 깨짐).
--
-- 조치
--  DMO 회사 COM005 5행의 BAIM_VAL_D_CD / SORT_IDX 를 표준 회사(CMPNY_CD='001')와
--  동일한 표준 코드체계로 정정(이름 기준 1:1 매핑). PK(CMPNY_CD,BAIM_VAL_CD,BAIM_VAL_D_CD)
--  변경이나 타 테이블 참조는 오히려 정합을 복구하는 방향(기존 AUTH_CD 는 이미 표준값).
--
-- 안전성
--  - TB_BAIM_VAL_D 에 FK 없음. 타겟 표준코드(system/master/hr/safe/99999)는 DMO COM005 에
--    미존재 → PK 충돌 없음.
--  - 재실행 안전(멱등): 정정 후 00001~00005 가 사라지므로 재실행 시 0행 영향.
--
-- 매핑표 (이름 → 표준 BAIM_VAL_D_CD / SORT_IDX, 회사 '001' 기준)
--   00001 시스템관리자   → system  (SORT_IDX 0)
--   00002 마스터관리자   → master  (SORT_IDX 1)
--   00003 HR             → hr      (SORT_IDX 2)
--   00004 산업안전관리자 → safe    (SORT_IDX 2)
--   00005 일반사용자     → 99999   (SORT_IDX 999)
--
-- ⚠️ 적용 전 사전 점검 쿼리로 대상 5행이 예상대로인지 확인할 것.
-- ============================================================================

-- [사전 점검] 정정 대상 확인 (5행: 00001~00005 이 예상대로인지)
-- SELECT BAIM_VAL_D_CD, BAIM_VAL_D_NM, SORT_IDX, USE_YN
-- FROM TB_BAIM_VAL_D
-- WHERE CMPNY_CD = 'OOqRuIQHrnqp0qGAs1oe' AND BAIM_VAL_CD = 'COM005'
-- ORDER BY SORT_IDX;

UPDATE TB_BAIM_VAL_D
   SET BAIM_VAL_D_CD = 'system'
     , SORT_IDX      = 0
     , UPDATE_NO     = 'SYSTEM'
     , UPDATE_DATE   = CURDATE()
 WHERE CMPNY_CD      = 'OOqRuIQHrnqp0qGAs1oe'
   AND BAIM_VAL_CD   = 'COM005'
   AND BAIM_VAL_D_CD = '00001';

UPDATE TB_BAIM_VAL_D
   SET BAIM_VAL_D_CD = 'master'
     , SORT_IDX      = 1
     , UPDATE_NO     = 'SYSTEM'
     , UPDATE_DATE   = CURDATE()
 WHERE CMPNY_CD      = 'OOqRuIQHrnqp0qGAs1oe'
   AND BAIM_VAL_CD   = 'COM005'
   AND BAIM_VAL_D_CD = '00002';

UPDATE TB_BAIM_VAL_D
   SET BAIM_VAL_D_CD = 'hr'
     , SORT_IDX      = 2
     , UPDATE_NO     = 'SYSTEM'
     , UPDATE_DATE   = CURDATE()
 WHERE CMPNY_CD      = 'OOqRuIQHrnqp0qGAs1oe'
   AND BAIM_VAL_CD   = 'COM005'
   AND BAIM_VAL_D_CD = '00003';

UPDATE TB_BAIM_VAL_D
   SET BAIM_VAL_D_CD = 'safe'
     , SORT_IDX      = 2
     , UPDATE_NO     = 'SYSTEM'
     , UPDATE_DATE   = CURDATE()
 WHERE CMPNY_CD      = 'OOqRuIQHrnqp0qGAs1oe'
   AND BAIM_VAL_CD   = 'COM005'
   AND BAIM_VAL_D_CD = '00004';

UPDATE TB_BAIM_VAL_D
   SET BAIM_VAL_D_CD = '99999'
     , SORT_IDX      = 999
     , UPDATE_NO     = 'SYSTEM'
     , UPDATE_DATE   = CURDATE()
 WHERE CMPNY_CD      = 'OOqRuIQHrnqp0qGAs1oe'
   AND BAIM_VAL_CD   = 'COM005'
   AND BAIM_VAL_D_CD = '00005';

-- [사후 검증] 표준 코드로 정정되었는지 + 사용자 AUTH_CD 매칭 확인
-- SELECT D.BAIM_VAL_D_CD, D.BAIM_VAL_D_NM, D.SORT_IDX
-- FROM TB_BAIM_VAL_D D
-- WHERE D.CMPNY_CD = 'OOqRuIQHrnqp0qGAs1oe' AND D.BAIM_VAL_CD = 'COM005'
-- ORDER BY D.SORT_IDX;
--
-- SELECT FNC_CMM_INFO_SRCH('OOqRuIQHrnqp0qGAs1oe','AUTH_LEVEL','master',NULL) AS lvl; -- '1' 기대
--
-- SELECT U.AUTH_CD, D.BAIM_VAL_D_NM
-- FROM TB_USER U
--      LEFT JOIN TB_BAIM_VAL_D D
--        ON D.CMPNY_CD = U.CMPNY_CD AND D.BAIM_VAL_CD = 'COM005'
--       AND D.BAIM_VAL_D_CD = U.AUTH_CD
-- WHERE U.CMPNY_CD = 'OOqRuIQHrnqp0qGAs1oe';  -- BAIM_VAL_D_NM 이 채워지면(더이상 NULL 아님) 정상
