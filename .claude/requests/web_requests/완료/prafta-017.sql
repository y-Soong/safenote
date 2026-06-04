-- =====================================================================
-- prafta-017: 자동부여 '부여일지정' 추가 / 관리자 수동부여 '부여일수' 제거
-- =====================================================================
-- 변경 요약
--   1) TB_LEAVE_TYPE_MGMT.GRANT_DAYS 컬럼 DROP (수동부여 부여일수 제거)
--   2) TB_LEAVE_TYPE_MGMT.GRANT_ASSIGN_MMDD 컬럼 ADD (자동부여 지정일 MMDD)
--   3) TB_SYST_VAL_D SYS027='03' 부여일지정 INSERT (보강 - 이미 추가되어 있을 경우 IGNORE)
-- =====================================================================

-- (1) TB_LEAVE_TYPE_MGMT 컬럼 변경
ALTER TABLE TB_LEAVE_TYPE_MGMT
  DROP COLUMN GRANT_DAYS;

ALTER TABLE TB_LEAVE_TYPE_MGMT
  ADD COLUMN GRANT_ASSIGN_MMDD varchar(4) COLLATE utf8mb4_unicode_ci DEFAULT NULL
    COMMENT '자동부여 지정일(MMDD)' AFTER GRANT_OFFSET_MONTH;

-- (2) TB_SYST_VAL_D SYS027='03' 부여일지정 (이미 등록되어 있으면 IGNORE)
INSERT IGNORE INTO TB_SYST_VAL_D (
    CMPNY_CD
  , SYST_VAL_CD
  , SYST_VAL_D_CD
  , SYST_VAL_D_NM
  , USE_YN
  , SORT_NO
  , INSERT_NO
  , INSERT_DATE
)
SELECT
    A.CMPNY_CD
  , 'SYS027'
  , '03'
  , '부여일지정'
  , 'Y'
  , 3
  , 'SYSTEM'
  , NOW()
FROM (SELECT DISTINCT CMPNY_CD FROM TB_SYST_VAL_D WHERE SYST_VAL_CD = 'SYS027') A;
