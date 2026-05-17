-- ---------------------------------------------------------------------------
-- prafta-009 / PRAFTA-009
-- 관리자 반려 처리 이력 구분용 SYS032 코드 신규 추가.
--
-- 배경:
--   TB_USER_ATTD_HIST.HIST_TYPE 컬럼은 SYS032 코드 그룹을 참조한다.
--   기존 SYS032 코드: 01~06 (요청 유형). HIST_TYPE 실데이터는 01/03 사용 중.
--   관리자가 근태 요청을 반려할 때 남기는 처리 이력을 구분할 코드가 없어
--   신규 코드 '07'(관리자 반려)을 추가한다.
--
-- 적용 대상: 로컬/개발 DB (prafta). 운영 적용 시 별도 검토.
-- 멱등성: WHERE NOT EXISTS 가드로 중복 실행 시 재삽입하지 않는다.
-- ---------------------------------------------------------------------------
INSERT INTO TB_SYST_VAL_D
    (SYST_VAL_CD, SYST_VAL_D_CD, SYST_VAL_D_NM, SORT_IDX, USE_YN, VAL_D_DESC, INSERT_NO, INSERT_DATE)
SELECT
      'SYS032'
    , '07'
    , '관리자 반려'
    , 7
    , 'Y'
    , 'prafta-009: 관리자의 근태/초과근무 요청 반려 처리 이력 구분 (TB_USER_ATTD_HIST.HIST_TYPE)'
    , 'SYSTEM'
    , NOW()
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM TB_SYST_VAL_D
     WHERE SYST_VAL_CD   = 'SYS032'
       AND SYST_VAL_D_CD = '07'
);
