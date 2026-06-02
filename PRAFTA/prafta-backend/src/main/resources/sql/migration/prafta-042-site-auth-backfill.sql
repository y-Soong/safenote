-- ============================================================================
-- PRAFTA-042-7 — 전사 접근 역할(master/hr/safe) 사업장권한 백필 (D3-③)
-- 작성일: 2026-06-01
-- 적용 환경: MySQL 8.0.42
-- 참조: .claude/requests/web_requests/prafta-042.md (D3/D7), prafta-042-plan.md §3 PRAFTA-042-7
--       정책서 공통 §8.3.1 / §8.5
--
-- 목적
--  기존(이미 존재하는) master/hr/safe 사용자 × 기존 사업장(tb_site) 조합을
--  tb_user_site_auth 에 멱등 부여한다. 신규 사업장/신규·역할변경 사용자에 대한
--  자동부여는 042-4/042-5 런타임 로직이 담당하고, 본 SQL 은 그 이전 데이터의 1회 정합용이다.
--
-- 대상 역할: master / hr / safe (system 계정은 042-4 mergeMasterSiteAuthSet 와 동일하게 포함).
-- 멱등성: ON DUPLICATE KEY UPDATE 로 재실행 안전(이미 'Y' 면 그대로 유지, 'N' 이면 'Y' 복구).
--
-- ⚠️ 운영 적용 금지(파일만). 적용 전 아래 사전 점검 쿼리로 대상 건수를 확인할 것.
--    (사용자 수 × 사업장 수의 카티전 곱 — 회사 규모가 크면 INSERT 건수가 많을 수 있음.)
-- ============================================================================

-- [사전 점검] 백필 예상 대상 건수(회사 단위로 확인 권장).
-- SELECT U.CMPNY_CD, COUNT(*) AS expectedRows
-- FROM TB_USER U
--      JOIN TB_SITE S
--        ON S.CMPNY_CD = U.CMPNY_CD
-- WHERE U.AUTH_CD IN ('master', 'hr', 'safe', 'system')
--   AND IFNULL(U.USE_YN, 'Y') = 'Y'
-- GROUP BY U.CMPNY_CD;

INSERT INTO TB_USER_SITE_AUTH (
    CMPNY_CD
    , USER_CD
    , SITE_CD
    , USE_YN
    , INSERT_NO
    , INSERT_DATE
    , UPDATE_NO
    , UPDATE_DATE
)
SELECT
    U.CMPNY_CD
    , U.USER_CD
    , S.SITE_CD
    , 'Y' AS USE_YN
    , 'SYSTEM' AS INSERT_NO
    , NOW() AS INSERT_DATE
    , 'SYSTEM' AS UPDATE_NO
    , NOW() AS UPDATE_DATE
FROM TB_USER U
     JOIN TB_SITE S
       ON S.CMPNY_CD = U.CMPNY_CD
WHERE U.AUTH_CD IN ('master', 'hr', 'safe', 'system')
  AND IFNULL(U.USE_YN, 'Y') = 'Y'
ON DUPLICATE KEY UPDATE
    USE_YN        = VALUES(USE_YN)
    , UPDATE_NO   = VALUES(UPDATE_NO)
    , UPDATE_DATE = VALUES(UPDATE_DATE);
