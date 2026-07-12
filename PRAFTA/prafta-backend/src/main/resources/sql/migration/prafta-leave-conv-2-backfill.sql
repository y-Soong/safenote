-- ============================================================================
-- PRAFTA-leave-conv-2 (LC-12) — 연차 시간차 환산 개편: T4 기존 시간차 use 행 보정(backfill)
-- 작성일: 2026-07-11
-- 적용 환경: MySQL 8.0.42
-- 출처: 작업지시서_연차-시간차-환산-개편 T4 / 결정 ③(과거 전체 보정, 마감월·촉진발송분 제외)
--       plan 파일 §2 LC-12 · §8-⑥(마감 판정) · §8-⑦(촉진 제외) · §8-⑧(실물 스키마)
--       정책서: policies/attd/08-leave.md §8.5.8(이력 보존)
--
-- 배경
--  기존 시간차(USE_UNIT_TYPE 02/03/04) 차감은 "신청분 ÷ 그날 소정근로분"(HALF_UP scale5)
--  이어서 7h 스케줄 30분 = 0.07143 같은 비직관 값·합계 오차(30분×14회 ≠ 1.0)가 남아 있다.
--  LC-03(분모 고정 480 + 무반올림) 배포 후, 과거 CONFIRMED 시간차 행을 1회 보정한다.
--
-- 재계산 규칙
--  LEAVE_DAYS = LEAST(LEAVE_MINUTES / 480, 1.0)
--   - 분모 480 고정: 보정 시점에 tb_leave_conversion_policy 행이 없으므로 코드 폴백 480 과 동일
--     (지시서 T4 명시값 — 회사 설정과 무관).
--   - 무반올림: 30분 배수 ÷ 480 = 0.0625 배수(유한소수 scale4) → decimal(8,5) 무손실.
--     (계산은 scale 확보를 위해 decimal(14,5) 캐스팅 후 나눗셈 — 30분 배수가 아닌
--      비정형 분이 존재해도 scale5 에서 최근접 저장, 별도 반올림 로직 없음.)
--   - 캡 1.0 적용: 480 초과 스케줄(9h)의 과거 종일치 시간차가 1.125 로 커지는 것 방지
--     (R4 근로자 보호 — 안전 방향).
--   - 하한(마일스톤) 가드는 소급 미적용: 지시서 T4 는 "÷480 재계산"만 명시.
--     하한 소급 = 차감액 증가 = 근로자 불리 소급이므로 적용하지 않는다.
--
-- 제외 기준 (결정 ③ / plan §8-⑥·⑦ — 실물 스키마 확정값)
--  [촉진분 — 보수적 이중 제외]
--   ⓐ tb_user_leave_use.PROMOTION_STAGE <> 'NONE' 행 (SYS068 FIRST/SECOND — 촉진 지정 행)
--   ⓑ tb_leave_promotion_log(DEL_YN='N') 행의 (CMPNY_CD, USER_CD, BASE_GRANT_ID) 에
--      물린 행 — use 행의 GRANT_ID 가 BASE_GRANT_ID 와 일치하면 제외.
--  [마감월분 — 정밀 기준 채택]
--   tb_attd_close(CLOSE_STATUS='CLOSED') 의 부서 단위 커버리지 판정을
--   AttdCloseServiceImpl.isClosedForUser → AttdCloseMapper.countCovering /
--   selectUserNodeCd 로직 그대로 SQL 재현했다 (보수 기준 미채택 — 재현 가능 범위였음):
--     - 사용자 소속 = tb_user.NODE_CD (use 행의 CMPNY_CD+SITE_CD+USER_CD 로 조회, LIMIT 1
--       — selectUserNodeCd 와 동일. 런타임과 동일하게 "현재" 소속 기준 판정).
--     - 마감 커버: NODE_CD='*'(사업장 전체) / 소속 노드 자기자신 /
--       INC_SUB_YN='Y' 마감행의 하위 노드 전체(tb_site_node PARENT_NODE_CD 재귀 확장
--       — countCovering 의 조상 탐색을 하향 확장으로 등가 변환).
--     - 소속 노드 미해석(NULL) 시 '*' 마감만 커버 (런타임 normalizeNode('*') 와 동일).
--     - tb_site_node 에 DEL_YN 필터 없음 — countCovering 원본과 동일하게 미적용.
--   판정 월 = LEFT(use.START_DATE, 6) = CLOSE_YM (YYYYMM).
--
-- 스키마 실물 (plan §8-⑧ — schema-full.sql 스냅샷은 구버전)
--  tb_user_leave_use.LEAVE_DAYS = decimal(8,5), tb_user_leave_grant.USED_DAYS = decimal(8,5)
--  (019-A 반영. 스냅샷의 decimal(5,1) 은 구값이므로 무시.)
--
-- 멱등성 (재실행 안전)
--  - 백업 테이블 적재: LEAVE_ID 기존 행 skip(NOT EXISTS) — 최초 실행 시점의 원본 값 보존.
--  - 본 UPDATE: 재계산식이 결정적 + `LEAVE_DAYS <> NEW_LEAVE_DAYS` 조건 → 이미 보정된
--    행은 재실행 시 변경 0건 (자연 멱등).
--  - GRANT 재집계: `USED_DAYS <> 재집계값` 가드 → 재실행 시 변경 0건.
--
-- 실행 절차 (수동)
--  0) LC-03~05 배포 완료 상태에서만 실행 (신규 신청이 구식으로 들어오지 않는 상태).
--  1) 아래 스크립트를 위에서부터 순서대로 실행. [리포트]/[백업 덤프] SELECT 결과를 보관.
--  2) 리포트 건수가 상식 범위이면 COMMIT, 이상 징후 시 즉시 ROLLBACK.
--  3) COMMIT 후 하단 [검증] 쿼리 4종 실행 — 전부 기대값이어야 완료.
--
-- ⚠️ 운영 적용 금지(파일만). DB 직접 실행은 사용자 수동 (read-only MCP).
-- ============================================================================

-- ----------------------------------------------------------------------------
-- -1) 세션 안전모드 해제 (Workbench 기본 SQL_SAFE_UPDATES=1 대응)
--     2)·5) UPDATE 는 WHERE 에 키 컬럼이 직접 오지 않아(플래그/EXISTS 조건) 안전모드에서
--     1175 에러로 차단된다. 세션 한정 해제 후 스크립트 말미에 원복한다.
-- ----------------------------------------------------------------------------
SET @old_safe_updates := @@SQL_SAFE_UPDATES;
SET SQL_SAFE_UPDATES = 0;

-- ----------------------------------------------------------------------------
-- 0) 백업/작업 테이블 (DDL 은 암묵 커밋을 유발하므로 트랜잭션 밖에서 선행)
--    - 보정 후보 전체(제외분 포함)를 원본 값과 함께 적재 → 백업 + 제외 근거 + 원복 소스.
--    - 보정 완료·검증 종료 후에도 감사 목적상 보존 권장 (삭제는 사용자 판단).
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `tb_leave_conv2_backfill_bak` (
    `LEAVE_ID`         varchar(20)  COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '보정 후보 연차 사용 ID (tb_user_leave_use.LEAVE_ID)',
    `CMPNY_CD`         varchar(50)  COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '회사 코드',
    `SITE_CD`          varchar(50)  COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사업장 코드',
    `USER_CD`          varchar(20)  COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사용자 코드',
    `GRANT_ID`         varchar(20)  COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '차감 대상 부여 ID',
    `START_DATE`       varchar(8)   COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사용 시작일 (YYYYMMDD)',
    `USE_UNIT_TYPE`    varchar(2)   COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사용 단위 (02/03/04)',
    `LEAVE_MINUTES`    int          NOT NULL COMMENT '사용 분 (원본)',
    `OLD_LEAVE_DAYS`   decimal(8,5) NOT NULL COMMENT '보정 전 LEAVE_DAYS (원복용 원본 — 최초 실행 시점 값)',
    `NEW_LEAVE_DAYS`   decimal(8,5) NOT NULL COMMENT '재계산값 = LEAST(LEAVE_MINUTES/480, 1.0)',
    `USER_NODE_CD`     varchar(50)  COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '판정 시점 사용자 소속 노드 (tb_user.NODE_CD)',
    `EXCL_PROMO_ROW`   varchar(1)   COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'N' COMMENT '제외 ⓐ: 촉진 지정 행 (PROMOTION_STAGE<>NONE) Y/N',
    `EXCL_PROMO_GRANT` varchar(1)   COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'N' COMMENT '제외 ⓑ: 촉진 로그 BASE_GRANT_ID 물림 Y/N',
    `EXCL_CLOSED_YM`   varchar(1)   COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'N' COMMENT '제외: 근태 마감월 (부서 커버리지 판정) Y/N',
    `CHG_YN`           varchar(1)   COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'N' COMMENT '본 UPDATE 실제 반영 여부 Y/N',
    `INSERT_DATE`      datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '적재 일시',
    PRIMARY KEY (`LEAVE_ID`),
    KEY `IX_LEAVE_CONV2_BAK_GRANT` (`CMPNY_CD`, `GRANT_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='LC-12 시간차 LEAVE_DAYS 보정 백업/작업 테이블 (prafta-leave-conv-2)';

-- ----------------------------------------------------------------------------
-- 트랜잭션 시작 — 이하 적재/보정/재집계는 전부 하나의 트랜잭션.
--   중간 오류·리포트 이상 시 COMMIT 대신 ROLLBACK; 실행 (백업 적재분도 함께 롤백됨
--   — 아직 아무것도 변경 전이므로 안전).
-- ----------------------------------------------------------------------------
START TRANSACTION;

-- ----------------------------------------------------------------------------
-- 1) 보정 후보 적재 (기본 필터 전체 + 제외 사유 플래그 ⓐⓑ + 재계산값)
--    - 멱등: 이미 적재된 LEAVE_ID 는 skip → 최초 실행 시점의 OLD_LEAVE_DAYS 보존.
--    - 마감월 플래그(EXCL_CLOSED_YM)는 2)에서 별도 갱신.
-- ----------------------------------------------------------------------------
INSERT INTO tb_leave_conv2_backfill_bak (
      LEAVE_ID
    , CMPNY_CD
    , SITE_CD
    , USER_CD
    , GRANT_ID
    , START_DATE
    , USE_UNIT_TYPE
    , LEAVE_MINUTES
    , OLD_LEAVE_DAYS
    , NEW_LEAVE_DAYS
    , USER_NODE_CD
    , EXCL_PROMO_ROW
    , EXCL_PROMO_GRANT
)
SELECT
      U.LEAVE_ID
    , U.CMPNY_CD
    , U.SITE_CD
    , U.USER_CD
    , U.GRANT_ID
    , U.START_DATE
    , U.USE_UNIT_TYPE
    , U.LEAVE_MINUTES
    , U.LEAVE_DAYS
    -- 재계산: ÷480 무반올림 + 캡 1.0 (30분 배수는 scale4 유한소수 → decimal(8,5) 무손실)
    , LEAST(CAST(U.LEAVE_MINUTES AS decimal(14,5)) / 480, 1.00000)
    -- 사용자 소속 노드 — AttdCloseMapper.selectUserNodeCd 재현 (현재 소속 기준)
    , (SELECT USR.NODE_CD
         FROM tb_user USR
        WHERE USR.CMPNY_CD = U.CMPNY_CD
          AND USR.SITE_CD  = U.SITE_CD
          AND USR.USER_CD  = U.USER_CD
        LIMIT 1)
    -- 제외 ⓐ: 촉진 지정 행 (SYS068 NONE 외 전부)
    , CASE WHEN U.PROMOTION_STAGE <> 'NONE' THEN 'Y' ELSE 'N' END
    -- 제외 ⓑ: 촉진 로그(미삭제)의 (CMPNY_CD, USER_CD, BASE_GRANT_ID) 에 물린 GRANT
    , CASE WHEN EXISTS (
            SELECT 1
              FROM tb_leave_promotion_log P
             WHERE P.CMPNY_CD      = U.CMPNY_CD
               AND P.USER_CD       = U.USER_CD
               AND P.DEL_YN        = 'N'
               AND P.BASE_GRANT_ID = U.GRANT_ID
      ) THEN 'Y' ELSE 'N' END
  FROM tb_user_leave_use U
 WHERE U.USE_UNIT_TYPE IN ('02', '03', '04')
   AND U.LEAVE_STATUS  = 'CONFIRMED'
   AND U.DEL_YN        = 'N'
   AND U.LEAVE_MINUTES IS NOT NULL
   AND NOT EXISTS (
        SELECT 1
          FROM tb_leave_conv2_backfill_bak B
         WHERE B.LEAVE_ID = U.LEAVE_ID
   );

-- ----------------------------------------------------------------------------
-- 2) 마감월 제외 플래그 갱신 — isClosedForUser(countCovering) 정밀 SQL 재현
--    closed_scope: CLOSED 마감행(NODE_CD<>'*')을 "커버되는 하위 노드 집합"으로 하향 확장.
--      - 기저: 마감행의 노드 자기자신 (INC_SUB_YN 무관 — countCovering 의 NODE_CD=자기 매칭).
--      - 재귀: INC_SUB_YN='Y' 인 경우만 tb_site_node 자식으로 확장 (전 깊이).
--      → countCovering 의 "사용자 노드의 조상 중 INC_SUB='Y' 마감행" 탐색과 등가.
--    '*'(사업장 전체) 마감은 노드 무관이므로 별도 EXISTS 로 판정
--      (소속 노드 NULL 사용자도 '*' 마감만 커버 — normalizeNode 와 동일).
-- ----------------------------------------------------------------------------
WITH RECURSIVE closed_scope AS (
    SELECT
          C.CMPNY_CD
        , C.SITE_CD
        , C.CLOSE_YM
        , C.INC_SUB_YN
        , C.NODE_CD AS COVERED_NODE_CD
      FROM tb_attd_close C
     WHERE C.CLOSE_STATUS = 'CLOSED'
       AND C.NODE_CD <> '*'
    UNION ALL
    SELECT
          S.CMPNY_CD
        , S.SITE_CD
        , S.CLOSE_YM
        , S.INC_SUB_YN
        , N.NODE_CD
      FROM closed_scope S
      JOIN tb_site_node N
        ON N.CMPNY_CD       = S.CMPNY_CD
       AND N.SITE_CD        = S.SITE_CD
       AND N.PARENT_NODE_CD = S.COVERED_NODE_CD
     WHERE S.INC_SUB_YN = 'Y'
)
UPDATE tb_leave_conv2_backfill_bak B
   SET B.EXCL_CLOSED_YM = 'Y'
 WHERE B.EXCL_CLOSED_YM = 'N'
   AND (
        -- 사업장 전체('*') 마감
        EXISTS (
            SELECT 1
              FROM tb_attd_close C
             WHERE C.CMPNY_CD     = B.CMPNY_CD
               AND C.SITE_CD      = B.SITE_CD
               AND C.CLOSE_YM     = LEFT(B.START_DATE, 6)
               AND C.CLOSE_STATUS = 'CLOSED'
               AND C.NODE_CD      = '*'
        )
        -- 부서 커버리지 마감 (자기자신 / INC_SUB='Y' 상위의 하위 확장)
     OR EXISTS (
            SELECT 1
              FROM closed_scope S
             WHERE S.CMPNY_CD        = B.CMPNY_CD
               AND S.SITE_CD         = B.SITE_CD
               AND S.CLOSE_YM        = LEFT(B.START_DATE, 6)
               AND S.COVERED_NODE_CD = B.USER_NODE_CD
        )
   );

-- ----------------------------------------------------------------------------
-- 3) [사전 리포트] — COMMIT 판단 근거. 결과를 반드시 보관할 것.
-- ----------------------------------------------------------------------------
-- 3-1) 대상/제외 사유별/변경 예정 건수
SELECT
      COUNT(*)                                    AS `대상후보_전체`
    , SUM(EXCL_PROMO_ROW   = 'Y')                 AS `제외_촉진지정행(ⓐ)`
    , SUM(EXCL_PROMO_GRANT = 'Y')                 AS `제외_촉진GRANT물림(ⓑ)`
    , SUM(EXCL_CLOSED_YM   = 'Y')                 AS `제외_마감월`
    , SUM(EXCL_PROMO_ROW = 'N' AND EXCL_PROMO_GRANT = 'N' AND EXCL_CLOSED_YM = 'N')
                                                  AS `보정대상`
    , SUM(EXCL_PROMO_ROW = 'N' AND EXCL_PROMO_GRANT = 'N' AND EXCL_CLOSED_YM = 'N'
          AND OLD_LEAVE_DAYS <> NEW_LEAVE_DAYS)   AS `변경예정_행수`
  FROM tb_leave_conv2_backfill_bak;

-- 3-2) GRANT 재집계 예정 대상 수
SELECT COUNT(DISTINCT CMPNY_CD, GRANT_ID) AS `GRANT재집계_대상수`
  FROM tb_leave_conv2_backfill_bak
 WHERE EXCL_PROMO_ROW   = 'N'
   AND EXCL_PROMO_GRANT = 'N'
   AND EXCL_CLOSED_YM   = 'N'
   AND OLD_LEAVE_DAYS <> NEW_LEAVE_DAYS
   AND GRANT_ID IS NOT NULL;

-- 3-3) [백업 덤프] 변경 전 원본 값 (필요 시 결과를 파일로 저장 — bak 테이블 자체도 영구 보존)
SELECT
      LEAVE_ID
    , OLD_LEAVE_DAYS AS LEAVE_DAYS
    , LEAVE_MINUTES
    , GRANT_ID
  FROM tb_leave_conv2_backfill_bak
 ORDER BY LEAVE_ID;

-- ----------------------------------------------------------------------------
-- 4) 본 보정 UPDATE — 제외 플래그 전부 'N' 이고 값이 실제로 달라지는 행만.
--    - 멱등: 이미 보정된 행은 LEAVE_DAYS = NEW_LEAVE_DAYS 라 재실행 시 0건.
--    - 안전 재확인: 적재 이후 상태가 바뀐 행(취소/삭제) 방어를 위해 U 측 필터 재적용.
--    - CHG_YN 마킹을 같은 문장에서 수행 (multi-table UPDATE).
-- ----------------------------------------------------------------------------
UPDATE tb_user_leave_use U
  JOIN tb_leave_conv2_backfill_bak B
    ON B.LEAVE_ID = U.LEAVE_ID
   SET U.LEAVE_DAYS  = B.NEW_LEAVE_DAYS
     , U.UPDATE_NO   = 'SYSTEM'
     , U.UPDATE_DATE = NOW()
     , B.CHG_YN      = 'Y'
 WHERE B.EXCL_PROMO_ROW   = 'N'
   AND B.EXCL_PROMO_GRANT = 'N'
   AND B.EXCL_CLOSED_YM   = 'N'
   AND U.LEAVE_STATUS     = 'CONFIRMED'
   AND U.DEL_YN           = 'N'
   AND U.LEAVE_DAYS      <> B.NEW_LEAVE_DAYS;

-- ----------------------------------------------------------------------------
-- 5) 영향 GRANT 재집계 — LeaveFlowMapper.recomputeGrantUsedDays 원본 SQL 재현.
--    대상: 실제 변경된(CHG_YN='Y') 행의 distinct (CMPNY_CD, GRANT_ID).
--    멱등: USED_DAYS 가 이미 재집계값과 같으면 0건.
-- ----------------------------------------------------------------------------
UPDATE tb_user_leave_grant G
   SET G.USED_DAYS = IFNULL((
           SELECT SUM(U.LEAVE_DAYS)
             FROM tb_user_leave_use U
            WHERE U.CMPNY_CD     = G.CMPNY_CD
              AND U.GRANT_ID     = G.GRANT_ID
              AND U.LEAVE_STATUS = 'CONFIRMED'
              AND U.DEL_YN       = 'N'
       ), 0)
     , G.UPDATE_NO   = 'SYSTEM'
     , G.UPDATE_DATE = NOW()
 WHERE EXISTS (
        SELECT 1
          FROM tb_leave_conv2_backfill_bak B
         WHERE B.CHG_YN   = 'Y'
           AND B.GRANT_ID IS NOT NULL
           AND B.CMPNY_CD = G.CMPNY_CD
           AND B.GRANT_ID = G.GRANT_ID
   )
   AND G.USED_DAYS <> IFNULL((
           SELECT SUM(U2.LEAVE_DAYS)
             FROM tb_user_leave_use U2
            WHERE U2.CMPNY_CD     = G.CMPNY_CD
              AND U2.GRANT_ID     = G.GRANT_ID
              AND U2.LEAVE_STATUS = 'CONFIRMED'
              AND U2.DEL_YN       = 'N'
       ), 0);

-- ----------------------------------------------------------------------------
-- 6) [사후 리포트] — 변경 반영 건수 확인 후 COMMIT 판단.
-- ----------------------------------------------------------------------------
SELECT
      SUM(CHG_YN = 'Y')                           AS `변경_행수`
    , COUNT(DISTINCT IF(CHG_YN = 'Y' AND GRANT_ID IS NOT NULL,
                        CONCAT(CMPNY_CD, ':', GRANT_ID), NULL))
                                                  AS `GRANT재집계_수행수`
  FROM tb_leave_conv2_backfill_bak;

-- ----------------------------------------------------------------------------
-- 확정. 리포트(3·6)가 기대 범위를 벗어나면 COMMIT 대신 즉시 `ROLLBACK;` 실행.
-- ----------------------------------------------------------------------------
COMMIT;

-- 세션 안전모드 원복 (-1 에서 보관한 값)
SET SQL_SAFE_UPDATES = @old_safe_updates;

-- ============================================================================
-- [검증] COMMIT 후 수동 실행 (전부 read-only)
-- ============================================================================

-- V1) 샘플 — 30분 행이 정확히 0.06250 인지 (구 7h 스케줄 0.07143 행의 보정 확인)
--     기대: LEAVE_DAYS 전부 0.06250
SELECT
      B.LEAVE_ID
    , B.LEAVE_MINUTES
    , B.OLD_LEAVE_DAYS
    , U.LEAVE_DAYS AS NEW_LEAVE_DAYS
  FROM tb_leave_conv2_backfill_bak B
  JOIN tb_user_leave_use U
    ON U.LEAVE_ID = B.LEAVE_ID
 WHERE B.CHG_YN = 'Y'
   AND B.LEAVE_MINUTES = 30
 ORDER BY B.LEAVE_ID
 LIMIT 20;

-- V2) 합계 정확 소진 — 보정 대상 행의 사용자별 SUM(LEAVE_DAYS) = SUM(LEAVE_MINUTES)/480
--     (30분×14회 사용자 합계 = 정확히 1.0 케이스가 여기에 포함됨.
--      캡 발동 행(LEAVE_MINUTES > 480)은 등식이 성립하지 않으므로 제외.)
--     기대: 0건
SELECT
      B.CMPNY_CD
    , B.USER_CD
    , SUM(B.LEAVE_MINUTES)       AS TOT_MINUTES
    , SUM(U.LEAVE_DAYS)          AS TOT_DAYS
    , SUM(B.LEAVE_MINUTES) / 480 AS EXPECTED_DAYS
  FROM tb_leave_conv2_backfill_bak B
  JOIN tb_user_leave_use U
    ON U.LEAVE_ID = B.LEAVE_ID
 WHERE B.EXCL_PROMO_ROW   = 'N'
   AND B.EXCL_PROMO_GRANT = 'N'
   AND B.EXCL_CLOSED_YM   = 'N'
   AND B.LEAVE_MINUTES   <= 480
   AND U.LEAVE_STATUS     = 'CONFIRMED'
   AND U.DEL_YN           = 'N'
 GROUP BY B.CMPNY_CD, B.USER_CD
HAVING SUM(U.LEAVE_DAYS) <> SUM(B.LEAVE_MINUTES) / 480;

-- V3) GRANT 원장 전수 대조 — USED_DAYS = CONFIRMED use 합계 (recompute 기준식과 동일)
--     기대: 0건. (0건이 아니면 이번 보정과 무관하게 어긋나 있던 기존 결함 —
--      본 보정 대상 GRANT 는 5)에서 전부 동기화되므로 여기 나올 수 없음. 별도 보고 대상.)
SELECT
      G.CMPNY_CD
    , G.GRANT_ID
    , G.USED_DAYS
    , IFNULL(X.SUM_DAYS, 0) AS USE_SUM_DAYS
  FROM tb_user_leave_grant G
  LEFT JOIN (
        SELECT
              U.CMPNY_CD
            , U.GRANT_ID
            , SUM(U.LEAVE_DAYS) AS SUM_DAYS
          FROM tb_user_leave_use U
         WHERE U.LEAVE_STATUS = 'CONFIRMED'
           AND U.DEL_YN       = 'N'
           AND U.GRANT_ID IS NOT NULL
         GROUP BY U.CMPNY_CD, U.GRANT_ID
       ) X
    ON X.CMPNY_CD = G.CMPNY_CD
   AND X.GRANT_ID = G.GRANT_ID
 WHERE G.USED_DAYS <> IFNULL(X.SUM_DAYS, 0);

-- V4) 제외 대상 불변 확인 — 제외 플래그가 하나라도 'Y' 인 행은 값이 그대로여야 한다
--     기대: 0건
SELECT
      B.LEAVE_ID
    , B.EXCL_PROMO_ROW
    , B.EXCL_PROMO_GRANT
    , B.EXCL_CLOSED_YM
    , B.OLD_LEAVE_DAYS
    , U.LEAVE_DAYS
  FROM tb_leave_conv2_backfill_bak B
  JOIN tb_user_leave_use U
    ON U.LEAVE_ID = B.LEAVE_ID
 WHERE (B.EXCL_PROMO_ROW = 'Y' OR B.EXCL_PROMO_GRANT = 'Y' OR B.EXCL_CLOSED_YM = 'Y')
   AND U.LEAVE_DAYS <> B.OLD_LEAVE_DAYS;

-- ============================================================================
-- [원복 절차] (COMMIT 이후 되돌려야 할 때 — 수동, 순서 엄수)
-- ----------------------------------------------------------------------------
-- 1) use 행 원복 (최초 실행 시점 원본 값으로):
-- UPDATE tb_user_leave_use U
--   JOIN tb_leave_conv2_backfill_bak B
--     ON B.LEAVE_ID = U.LEAVE_ID
--    AND B.CHG_YN   = 'Y'
--    SET U.LEAVE_DAYS  = B.OLD_LEAVE_DAYS
--      , U.UPDATE_NO   = 'SYSTEM'
--      , U.UPDATE_DATE = NOW();
--
-- 2) GRANT 재집계 재실행 (본문 5) UPDATE 그대로 — 원복 값 기준 USED_DAYS 동기화):
--    ※ 5) 의 멱등 가드(USED_DAYS <> 재집계값) 덕에 그대로 재실행하면 된다.
--
-- 3) 마킹 초기화 (재보정 대비):
-- UPDATE tb_leave_conv2_backfill_bak SET CHG_YN = 'N' WHERE CHG_YN = 'Y';
-- ============================================================================
