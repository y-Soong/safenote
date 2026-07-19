-- ============================================================================
-- PRAFTA-SUBCON-T0-01 — 순회점검 문항 사업장 단위 분리 (SITE_CD 키 확장 + fan-out)
-- 작성일: 2026-07-12
-- 적용 환경: MySQL 8.0.42
-- 출처: .claude/requests/web_requests/PRAFTA-SUBCON-T0.md §2
--       .claude/requests/web_requests/PRAFTA-SUBCON-T0.plan.md §2 (T0-01)
--       .claude/requests/web_requests/PRAFTA-SUBCON.md §2.2 (DDL 계약)
--
-- [선행조건 — 반드시 확인 후 실행]
--   1) prafta-chklst-strdate-day-and-item-hist.sql 적용 완료 상태여야 함:
--      - TB_CHKPT_INSPECT_ITEM.STR_DATE varchar(8) (YYYYMMDD)
--      - TB_CHKPT_INSPECT_ITEM_HIST 테이블 존재 (HIST_ID auto_increment PK)
--      (2026-07-12 메인 세션 MCP 실측으로 로컬 DB 적용 확인됨 — plan §4-4)
--   2) 본 SQL 적용과 T0-02/03 백엔드 배포는 같은 릴리즈로 묶을 것.
--      코드만 먼저 배포하면 SITE_CD 컬럼 부재로 즉시 장애.
--
-- 처리 순서 (순서 엄수 — 구 PK 상태에서 fan-out 하면 중복 PK 에러):
--   STEP 0. 사전 점검 SELECT (문항은 있는데 활성 사업장이 0개인 회사 확인)
--   STEP 1. TB_CHKPT_INSPECT_ITEM: SITE_CD 컬럼 추가(DEFAULT '') → PK 재구성
--   STEP 2. TB_CHKPT_INSPECT_ITEM: 활성 사업장별 fan-out 복제 → 원본('') 삭제 → DEFAULT 제거
--   STEP 3. TB_CHKPT_INSPECT_ITEM_HIST: SITE_CD 컬럼 추가 → 인덱스 재구성 → fan-out → 원본 삭제
--   STEP 4. 건수 검증 SELECT
--
-- 멱등성: 1회성 스크립트. 재실행 시 ALTER 중복 에러(컬럼/PK 존재)로 중단됨.
--         fan-out 은 SITE_CD='' 행만 대상으로 하므로 부분 재실행은 안전하나,
--         원칙적으로 처음부터 끝까지 1회 실행 후 보관용.
-- 기존 INSPECT_ITEM_CD 값은 유지(사업장 간 동일 코드 허용 — 키에 SITE_CD 추가됨).
-- 문항 코드 채번 시퀀스(COM001-{type})는 회사 단위 유지 — 본 SQL 에서 건드리지 않음.
--
-- [MySQL Workbench safe update mode — 필수]
--   STEP 2 / STEP 3 의 원본 삭제는 `WHERE SITE_CD = ''` 로 값 기준 전량 삭제다.
--   SITE_CD 는 ITEM 의 PK 선두 컬럼이 아니고(HIST 는 PK 가 HIST_ID) 인덱스 선두도 아니라서,
--   Workbench 기본 설정(SQL_SAFE_UPDATES=1)은 이 DELETE 를 "키 없는 전체 스캔"으로 보고
--   Error 1175 로 거부한다. 아래에서 세션 한정으로 해제하고, 스크립트 말미에 되돌린다.
--   (의도된 전량 삭제이므로 조건을 우회하는 것이 아니라 안전장치를 명시적으로 잠시 내리는 것.)
-- ============================================================================

SET SQL_SAFE_UPDATES = 0;


-- ============================================================================
-- STEP 0. 사전 점검 — 활성 사업장 0개인 회사에 문항이 존재하는지 확인
--   ※ 결과가 1행이라도 나오면 해당 회사의 문항이 fan-out 에서 소실된다.
--     [실행 중단] 하고 해당 회사의 사업장 상태(TB_SITE.USE_YN)를 먼저 정리한 뒤
--     본 스크립트를 처음부터 다시 실행할 것.
-- ============================================================================
SELECT
      A.CMPNY_CD
    , COUNT(1) AS ITEM_CNT
FROM TB_CHKPT_INSPECT_ITEM A
WHERE NOT EXISTS (
        SELECT 1
        FROM TB_SITE S
        WHERE S.CMPNY_CD = A.CMPNY_CD
          AND S.USE_YN = 'Y'
      )
GROUP BY A.CMPNY_CD;
-- 기대 결과: 0행. 1행 이상이면 여기서 중단.

-- 0-2) 이력(HIST)만 잔존하는 회사 확인 — 문항 없이 이력만 남은 회사가 활성 사업장 0개면
--      STEP 3 의 DELETE 에서 감사 이력이 무경고 소실된다. (QA D-1 보강)
SELECT
      H.CMPNY_CD
    , COUNT(1) AS HIST_CNT
FROM TB_CHKPT_INSPECT_ITEM_HIST H
WHERE NOT EXISTS (
        SELECT 1
        FROM TB_SITE S
        WHERE S.CMPNY_CD = H.CMPNY_CD
          AND S.USE_YN = 'Y'
      )
GROUP BY H.CMPNY_CD;
-- 기대 결과: 0행. 1행 이상이면 여기서 중단 (해당 회사 사업장 정리 후 재실행).


-- ============================================================================
-- STEP 1. TB_CHKPT_INSPECT_ITEM — SITE_CD 컬럼 추가 + PK 재구성
--   (fan-out 전에 PK 를 먼저 신 키로 바꿔야 사업장별 복제행이 들어갈 수 있음)
-- ============================================================================
ALTER TABLE TB_CHKPT_INSPECT_ITEM
    ADD COLUMN SITE_CD varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '사업장코드' AFTER CMPNY_CD;

ALTER TABLE TB_CHKPT_INSPECT_ITEM
    DROP PRIMARY KEY,
    ADD PRIMARY KEY (CMPNY_CD, SITE_CD, CHKLST_TYPE, INSPECT_ITEM_CD);


-- ============================================================================
-- STEP 2. TB_CHKPT_INSPECT_ITEM — 활성 사업장별 fan-out 복제
--   원본(회사 단위, SITE_CD='') 행을 해당 회사의 활성 사업장 각각으로 복제한 뒤
--   원본 행을 삭제하고 DEFAULT 를 제거한다.
-- ============================================================================
INSERT INTO TB_CHKPT_INSPECT_ITEM (
      CMPNY_CD
    , SITE_CD
    , CHKLST_TYPE
    , INSPECT_ITEM_CD
    , INSPECT_ITEM_SUBJ
    , SORT_IDX
    , STR_DATE
    , USE_YN
    , INSERT_NO
    , INSERT_DATE
    , UPDATE_NO
    , UPDATE_DATE
)
SELECT
      A.CMPNY_CD
    , S.SITE_CD
    , A.CHKLST_TYPE
    , A.INSPECT_ITEM_CD
    , A.INSPECT_ITEM_SUBJ
    , A.SORT_IDX
    , A.STR_DATE
    , A.USE_YN
    , A.INSERT_NO
    , A.INSERT_DATE        -- 등록 시각 원본 보존(확인서 회색 게이팅의 폴백 기준)
    , A.UPDATE_NO
    , A.UPDATE_DATE        -- 수정 시각 원본 보존
FROM TB_CHKPT_INSPECT_ITEM A
     INNER JOIN TB_SITE S
        ON (
            S.CMPNY_CD = A.CMPNY_CD
        AND S.USE_YN = 'Y'
        )
WHERE A.SITE_CD = '';

-- 원본(회사 단위) 행 제거
DELETE FROM TB_CHKPT_INSPECT_ITEM WHERE SITE_CD = '';

-- 이후 신규 행은 반드시 SITE_CD 명시 → DEFAULT 제거
ALTER TABLE TB_CHKPT_INSPECT_ITEM
    MODIFY COLUMN SITE_CD varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사업장코드';


-- ============================================================================
-- STEP 3. TB_CHKPT_INSPECT_ITEM_HIST — SITE_CD 추가 + 인덱스 재구성 + fan-out
--   PK 는 HIST_ID(auto_increment) 유지. 복제행의 HIST_ID 는 신규 채번된다.
--   INSERT_DATE(변경 시각)는 원본을 그대로 보존 — 확인서 회색 게이팅이
--   이력 시각 기반이므로 시각이 바뀌면 게이팅이 왜곡된다.
-- ============================================================================
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


-- ============================================================================
-- STEP 4. 검증 SELECT
-- ============================================================================

-- 안전장치 원복(세션 한정 해제였으므로 재연결 시에도 기본값으로 돌아오지만 명시적으로 되돌린다).
SET SQL_SAFE_UPDATES = 1;

-- 4-1) SITE_CD='' 잔존 확인 — 두 쿼리 모두 0 이어야 함
SELECT COUNT(1) AS REMAIN_ITEM FROM TB_CHKPT_INSPECT_ITEM WHERE SITE_CD = '';
SELECT COUNT(1) AS REMAIN_HIST FROM TB_CHKPT_INSPECT_ITEM_HIST WHERE SITE_CD = '';

-- 4-2) 문항 건수 검증 — 회사별: 복제 후 전체 건수 = (사업장당 문항 건수 균일) × 활성 사업장 수
--   DIFF_ITEM = 0 이어야 함. (사업장별 건수가 전부 동일한지 = fan-out 균일성 확인)
SELECT
      T.CMPNY_CD
    , T.SITE_CNT                            -- 활성 사업장 수
    , T.TOTAL_ITEM                          -- 복제 후 전체 문항 건수
    , T.DISTINCT_ITEM                       -- 고유 (CHKLST_TYPE, INSPECT_ITEM_CD) 수 = 원본 건수
    , T.TOTAL_ITEM - (T.DISTINCT_ITEM * T.SITE_CNT) AS DIFF_ITEM   -- 0 기대
FROM (
    SELECT
          A.CMPNY_CD
        , (SELECT COUNT(1) FROM TB_SITE S WHERE S.CMPNY_CD = A.CMPNY_CD AND S.USE_YN = 'Y') AS SITE_CNT
        , COUNT(1) AS TOTAL_ITEM
        , COUNT(DISTINCT A.CHKLST_TYPE, A.INSPECT_ITEM_CD) AS DISTINCT_ITEM
    FROM TB_CHKPT_INSPECT_ITEM A
    GROUP BY A.CMPNY_CD
) T;

-- 4-3) 이력 건수 검증 — 사업장별 이력 건수 균일성 확인 (QA D-2 보강)
--   fan-out 은 회사 원본을 전 활성 사업장에 동일 복제하므로, 회사 내 모든 사업장의
--   이력 건수가 같아야 한다(MAX-MIN=0). DISTINCT 키 재구성 방식(고유 이력키 × 사업장 수)은
--   같은 문항이 같은 초에 같은 유형으로 2회 변경된 이력이 있으면 오탐하므로 사용하지 않음.
--   UNEVEN = 0, SITE_CNT_IN_HIST = 활성 사업장 수 이어야 함.
SELECT
      T.CMPNY_CD
    , (SELECT COUNT(1) FROM TB_SITE S WHERE S.CMPNY_CD = T.CMPNY_CD AND S.USE_YN = 'Y') AS SITE_CNT_ACTIVE
    , COUNT(1) AS SITE_CNT_IN_HIST          -- 이력이 존재하는 사업장 수 (= SITE_CNT_ACTIVE 기대)
    , MIN(T.CNT) AS MIN_PER_SITE
    , MAX(T.CNT) AS MAX_PER_SITE
    , MAX(T.CNT) - MIN(T.CNT) AS UNEVEN     -- 0 기대 (사업장 간 건수 불균일 = fan-out 이상)
FROM (
    SELECT
          H.CMPNY_CD
        , H.SITE_CD
        , COUNT(1) AS CNT
    FROM TB_CHKPT_INSPECT_ITEM_HIST H
    GROUP BY H.CMPNY_CD, H.SITE_CD
) T
GROUP BY T.CMPNY_CD;

-- 4-4) 이력 INSERT_DATE 보존 확인 — 복제행 시각이 실행 시각으로 밀리지 않았는지 표본 확인
--   (실행일 당일 시각의 CHG_TYPE='01' 대량 발생 여부 점검. 대량이면 시각 보존 실패 의심)
SELECT DATE_FORMAT(INSERT_DATE, '%Y-%m-%d') AS CHG_YMD, COUNT(1) AS CNT
FROM TB_CHKPT_INSPECT_ITEM_HIST
GROUP BY DATE_FORMAT(INSERT_DATE, '%Y-%m-%d')
ORDER BY CHG_YMD DESC
LIMIT 10;

-- ============================================================================
-- 끝. 적용 후 T0-02/03 백엔드를 같은 릴리즈로 배포할 것.
-- ============================================================================
