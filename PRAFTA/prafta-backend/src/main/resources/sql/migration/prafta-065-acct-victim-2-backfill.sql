-- ============================================================================
-- prafta-065 : 사고관리 재해자 다중 등록 (2) 기존 사고 백필
-- 작성일: 2026-09-06 / 선행: prafta-065-acct-victim-1-ddl.sql 적용 완료
-- 근거: 요청서 prafta-065.md R1 백필 절 / D1(대표 = 헤더 컬럼) / 원칙 1(무회귀 — 기존 사고는 재해자 1명)
--
-- 규칙
--   · tb_acct 전 행(DEL_YN 무관)을 tb_acct_victim VICTIM_SEQ=1 로 1행씩 생성.
--   · VICTIM_RESULT_CD = 'INJURY' 고정(등급 100 중대재해도 사망 여부를 알 수 없으므로 부상으로 넣고 편집 팝업에서 정정).
--   · 일수·부위·내용 NULL. INSERT_NO/INSERT_DATE 는 헤더 값 승계(시간순서 왜곡 금지 관례). UPDATE_* NULL.
--   · WHERE NOT EXISTS 로 멱등 — 이미 재해자가 있는 사고(신규 API 로 등록된 사고 포함)는 건너뜀.
-- 적용 전 확인:
--   SELECT COUNT(*) AS acct_cnt FROM tb_acct;                                  -- 개발·운영 각 2건 예상
--   SELECT COUNT(*) AS victim_cnt FROM tb_acct_victim;                         -- 0 (첫 적용)
-- ============================================================================

INSERT INTO `tb_acct_victim`
    (`CMPNY_CD`, `SITE_CD`, `ACCT_ID`, `VICTIM_SEQ`, `USER_TYPE_CD`, `USER_CD`, `VICTIM_RESULT_CD`
    , `CARE_DAYS`, `REST_DAYS`, `INJURY_PART`, `INJURY_DESC`, `INSERT_NO`, `INSERT_DATE`)
SELECT
    A.`CMPNY_CD`
  , A.`SITE_CD`
  , A.`ACCT_ID`
  , 1
  , A.`VICTIM_USER_TYPE_CD`
  , A.`VICTIM_USER_CD`
  , 'INJURY'
  , NULL
  , NULL
  , NULL
  , NULL
  , IFNULL(A.`INSERT_NO`, 'SYSTEM')
  , A.`INSERT_DATE`
FROM `tb_acct` A
WHERE NOT EXISTS (
    SELECT 1
    FROM `tb_acct_victim` V
    WHERE V.`CMPNY_CD` = A.`CMPNY_CD`
      AND V.`SITE_CD`  = A.`SITE_CD`
      AND V.`ACCT_ID`  = A.`ACCT_ID`
);

-- ============================================================================
-- 적용 후 확인:
--   SELECT A.ACCT_ID FROM tb_acct A LEFT JOIN tb_acct_victim V
--       ON (V.CMPNY_CD = A.CMPNY_CD AND V.SITE_CD = A.SITE_CD AND V.ACCT_ID = A.ACCT_ID)
--    WHERE V.ACCT_ID IS NULL;                                                   -- 0행
--   SELECT ACCT_ID, VICTIM_SEQ, USER_TYPE_CD, VICTIM_RESULT_CD, INSERT_DATE FROM tb_acct_victim ORDER BY ACCT_ID LIMIT 50;
--   -- INSERT_DATE 가 tb_acct.INSERT_DATE 와 동일해야 함
-- ============================================================================
