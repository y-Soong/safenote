-- ============================================================================
-- PRAFTA-SUBCON-T0-01 재개 스크립트 — STEP 2 원본 삭제부터 (2026-07-13)
-- 원본: prafta-subcon-t0-chkpt-item-site-split.sql
--
-- [배경 — 2026-07-13 MCP 실측]
--   STEP 1 완료: TB_CHKPT_INSPECT_ITEM 에 SITE_CD 추가 + PK 재구성 확인.
--   STEP 2 부분 완료: fan-out INSERT 는 실행됨(001 회사, 원본 43 × 활성 사업장 10
--     = 복제 430행, DIFF=0 균일 검증 완료). 그러나 원본(SITE_CD='') 43행 삭제와
--     DEFAULT 제거 MODIFY 가 미실행 — Workbench safe update mode(Error 1175)로
--     DELETE 에서 중단된 것으로 추정.
--   STEP 3 미실행: TB_CHKPT_INSPECT_ITEM_HIST 에 SITE_CD 없음, 구 인덱스 그대로.
--   STEP 0 사전점검(0·0-2): 2026-07-13 재실측 — 두 쿼리 모두 0행, 재개 안전.
--
-- [주의]
--   원본 스크립트를 처음부터 재실행하지 말 것 —
--     STEP 1 ALTER 는 컬럼 중복 에러, STEP 2 INSERT 는 기존 복제행과 PK 충돌.
--   본 파일만 위에서 아래로 1회 실행한다.
-- ============================================================================

SET SQL_SAFE_UPDATES = 0;

-- ----------------------------------------------------------------------------
-- STEP 2 잔여 — 원본(회사 단위, SITE_CD='') 행 제거 + DEFAULT 제거
-- ----------------------------------------------------------------------------
DELETE FROM TB_CHKPT_INSPECT_ITEM WHERE SITE_CD = '';

ALTER TABLE TB_CHKPT_INSPECT_ITEM
    MODIFY COLUMN SITE_CD varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사업장코드';

-- ----------------------------------------------------------------------------
-- STEP 3. TB_CHKPT_INSPECT_ITEM_HIST — SITE_CD 추가 + 인덱스 재구성 + fan-out
--   PK 는 HIST_ID(auto_increment) 유지. 복제행의 HIST_ID 는 신규 채번된다.
--   INSERT_DATE(변경 시각)는 원본을 그대로 보존 — 확인서 회색 게이팅이
--   이력 시각 기반이므로 시각이 바뀌면 게이팅이 왜곡된다.
-- ----------------------------------------------------------------------------
ALTER TABLE TB_CHKPT_INSPECT_ITEM_HIST
    ADD COLUMN SITE_CD varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '사업장코드' AFTER CMPNY_CD;

ALTER TABLE TB_CHKPT_INSPECT_ITEM_HIST
    DROP INDEX IX_CHKPT_INSPECT_ITEM_HIST,
    ADD INDEX IX_CHKPT_INSPECT_ITEM_HIST (CMPNY_CD, SITE_CD, CHKLST_TYPE, INSPECT_ITEM_CD, INSERT_DATE);

INSERT INTO TB_CHKPT_INSPECT_ITEM_HIST (
      CMPNY_CD
    , SITE_CD
    , CHKLST_TYPE
    , INSPECT_ITEM_CD
    , CHG_TYPE
    , INSPECT_ITEM_SUBJ
    , STR_DATE
    , SORT_IDX
    , USE_YN
    , INSERT_NO
    , INSERT_DATE
)
SELECT
      H.CMPNY_CD
    , S.SITE_CD
    , H.CHKLST_TYPE
    , H.INSPECT_ITEM_CD
    , H.CHG_TYPE
    , H.INSPECT_ITEM_SUBJ
    , H.STR_DATE
    , H.SORT_IDX
    , H.USE_YN
    , H.INSERT_NO
    , H.INSERT_DATE        -- 변경 시각 원본 보존(게이팅 필수)
FROM TB_CHKPT_INSPECT_ITEM_HIST H
     INNER JOIN TB_SITE S
        ON (
            S.CMPNY_CD = H.CMPNY_CD
        AND S.USE_YN = 'Y'
        )
WHERE H.SITE_CD = '';

-- 원본(회사 단위) 이력 제거
DELETE FROM TB_CHKPT_INSPECT_ITEM_HIST WHERE SITE_CD = '';

-- 이후 신규 이력은 반드시 SITE_CD 명시 → DEFAULT 제거
ALTER TABLE TB_CHKPT_INSPECT_ITEM_HIST
    MODIFY COLUMN SITE_CD varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사업장코드';

-- 안전장치 원복
SET SQL_SAFE_UPDATES = 1;

-- ----------------------------------------------------------------------------
-- STEP 4. 검증 SELECT — 실행 후 결과를 확인할 것
-- ----------------------------------------------------------------------------

-- 4-1) SITE_CD='' 잔존 확인 — 두 쿼리 모두 0 이어야 함
SELECT COUNT(1) AS REMAIN_ITEM FROM TB_CHKPT_INSPECT_ITEM WHERE SITE_CD = '';
SELECT COUNT(1) AS REMAIN_HIST FROM TB_CHKPT_INSPECT_ITEM_HIST WHERE SITE_CD = '';

-- 4-2) 문항 건수 검증 — DIFF_ITEM = 0 기대
SELECT
      T.CMPNY_CD
    , T.SITE_CNT
    , T.TOTAL_ITEM
    , T.DISTINCT_ITEM
    , T.TOTAL_ITEM - (T.DISTINCT_ITEM * T.SITE_CNT) AS DIFF_ITEM
FROM (
    SELECT
          A.CMPNY_CD
        , (SELECT COUNT(1) FROM TB_SITE S WHERE S.CMPNY_CD = A.CMPNY_CD AND S.USE_YN = 'Y') AS SITE_CNT
        , COUNT(1) AS TOTAL_ITEM
        , COUNT(DISTINCT A.CHKLST_TYPE, A.INSPECT_ITEM_CD) AS DISTINCT_ITEM
    FROM TB_CHKPT_INSPECT_ITEM A
    GROUP BY A.CMPNY_CD
) T;

-- 4-3) 이력 건수 균일성 — UNEVEN = 0, SITE_CNT_IN_HIST = 활성 사업장 수 기대
SELECT
      T.CMPNY_CD
    , (SELECT COUNT(1) FROM TB_SITE S WHERE S.CMPNY_CD = T.CMPNY_CD AND S.USE_YN = 'Y') AS SITE_CNT_ACTIVE
    , COUNT(1) AS SITE_CNT_IN_HIST
    , MIN(T.CNT) AS MIN_PER_SITE
    , MAX(T.CNT) AS MAX_PER_SITE
    , MAX(T.CNT) - MIN(T.CNT) AS UNEVEN
FROM (
    SELECT
          H.CMPNY_CD
        , H.SITE_CD
        , COUNT(1) AS CNT
    FROM TB_CHKPT_INSPECT_ITEM_HIST H
    GROUP BY H.CMPNY_CD, H.SITE_CD
) T
GROUP BY T.CMPNY_CD;

-- 4-4) 이력 INSERT_DATE 보존 확인 — 실행일 당일로 시각이 밀린 대량 행이 없어야 함
SELECT DATE_FORMAT(INSERT_DATE, '%Y-%m-%d') AS CHG_YMD, COUNT(1) AS CNT
FROM TB_CHKPT_INSPECT_ITEM_HIST
GROUP BY DATE_FORMAT(INSERT_DATE, '%Y-%m-%d')
ORDER BY CHG_YMD DESC
LIMIT 10;

-- ============================================================================
-- 끝. 적용 후 T0-02/03 백엔드를 같은 릴리즈로 배포할 것.
-- ============================================================================
