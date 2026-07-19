-- =============================================================================
-- PRAFTA-SUBCON-T5 E2E 보조 픽스처 — B·C 테스트 직원 "오늘 출근" 시드
--
-- 이유: 정규직 대리입실 후보 쿼리(selectRegularCandidates)는 당일 출근한 정규직만
--       노출한다(기존 규칙 — "휴대폰 불가 정규직을 관리자가 대리입실"). 테스트 계정은
--       오늘 출근 기록이 없어 후보가 0건이 된다. E2E 진행을 위해 체크인 3건을 심는다.
--
-- 대상(전부 site 00001, node n1):
--   B  = mLP5JWe5EOFPS17zZOKj / 20260700001 (뉴코관리자)
--   C  = IqzQKPKMpu75RsCoDN6m / 20260700001 (뉴코쓰리관리자)
--   C  = IqzQKPKMpu75RsCoDN6m / 20260700002 (뉴코쓰리사원)
--
-- WORK_YMD = 오늘(20260714). 종료 후 [CLEANUP] 으로 제거.
-- =============================================================================

INSERT INTO `tb_user_attd_mgmt` (
      `ATTD_ID`, `CMPNY_CD`, `SITE_CD`, `USER_CD`, `WORK_YMD`, `NODE_CD`, `WORK_SEQ`
    , `CHECK_IN_DATE`, `CHECK_IN_TIME`, `CHECK_IN_METHOD`, `DEL_YN`, `INSERT_NO`, `INSERT_DATE`
) VALUES
      ('T5E2E0714B01', 'mLP5JWe5EOFPS17zZOKj', '00001', '20260700001', '20260714', 'n1', 1, '20260714', '0900', '01', 'N', 'SYSTEM', NOW())
    , ('T5E2E0714C01', 'IqzQKPKMpu75RsCoDN6m', '00001', '20260700001', '20260714', 'n1', 1, '20260714', '0900', '01', 'N', 'SYSTEM', NOW())
    , ('T5E2E0714C02', 'IqzQKPKMpu75RsCoDN6m', '00001', '20260700002', '20260714', 'n1', 1, '20260714', '0900', '01', 'N', 'SYSTEM', NOW());


-- #############################################################################
-- [CLEANUP] E2E 종료 후 실행
-- #############################################################################
-- SET @OLD_SU = @@SQL_SAFE_UPDATES; SET SQL_SAFE_UPDATES = 0;
-- DELETE FROM `tb_user_attd_mgmt` WHERE `ATTD_ID` IN ('T5E2E0714B01','T5E2E0714C01','T5E2E0714C02');
-- SET SQL_SAFE_UPDATES = @OLD_SU;
-- #############################################################################
