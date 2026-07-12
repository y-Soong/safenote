-- ============================================================================
-- PRAFTA-PLATFORM-2 — 플랫폼 운영자 전용 메뉴 + 권한 시드
-- 작성일: 2026-06-28
-- 적용 환경: MySQL 8.0.42
-- 참조: prafta-lnb-1-menu-restructure.sql(대분류/중분류 구조)·prafta-com-008-a-menu-seed.sql(auth_menu 관례)
--
-- 변경 요약
--   1) tb_syst_menu_m — 신규 대분류 'platform'("플랫폼 운영", MENU_SRC='001' 웹) 1행.
--   2) tb_syst_menu_d — 플랫폼 화면 2개("신규 고객사 등록", "이용약관 관리").
--      - SUB_GROUP_NM/SUB_GROUP_IDX 는 lnb-1 마이그로 추가된 컬럼(중분류). 미적용 환경이면
--        해당 두 컬럼을 INSERT 목록에서 제거하고 실행할 것.
--   3) tb_syst_auth_menu — (CMPNY_CD='prafta_system_admin', AUTH_CD='master') 로 위 2개 메뉴 권한 INSERT.
--      → 운영자에게만 노출. 고객 테넌트('001' 등)에는 행을 만들지 않으므로 자동 미노출.
--
-- 적용 전 확인(권장):
--   SELECT MENU_M_ID FROM tb_syst_menu_m WHERE MENU_M_ID='platform';
--   SELECT MENU_D_ID FROM tb_syst_menu_d WHERE MENU_D_ID IN ('Platform_01','Platform_02');
--   SELECT CMPNY_CD, AUTH_CD, MENU_D_ID FROM tb_syst_auth_menu
--    WHERE CMPNY_CD='prafta_system_admin';
--   SHOW COLUMNS FROM tb_syst_menu_d LIKE 'SUB_GROUP_NM';  -- 없으면 (2)에서 해당 컬럼 제외
--
-- 멱등성: INSERT 중복 실행 시 PK 충돌. 이미 반영된 환경에서는 건너뛸 것.
-- 운영 적용: 사용자(운영자) 직접 적용(MCP read-only). 본 파일은 작성만, DB 직접 적용 금지.
--           미적용 시 화면이 메뉴에 노출되지 않음(EP 자체는 게이트로 동작 — 메뉴 노출만의 문제).
-- ============================================================================

-- ── 1) 대분류(플랫폼 운영) ──
INSERT INTO `tb_syst_menu_m`
      (`MENU_M_ID`, `MENU_SRC`, `MENU_NM`, `MENU_IDX`, `USE_YN`, `INSERT_NO`, `INSERT_DATE`)
VALUES
      ('platform', '001', '플랫폼 운영', 9, 'Y', 'SYSTEM', NOW());

-- ── 2) 메뉴 디테일(플랫폼 운영 하위 2개) ──
--   MENU_VIEW 는 웹 FE viewResolver 가 로드할 컴포넌트 경로(프론트 화면은 별도 작업).
INSERT INTO `tb_syst_menu_d`
      (`MENU_D_ID`, `MENU_M_ID`, `MENU_VIEW`, `MENU_NM`, `SUB_GROUP_NM`, `SUB_GROUP_IDX`, `MENU_IDX`, `USE_YN`, `INSERT_NO`)
VALUES
      ('Platform_01', 'platform', 'platform/Platform_01.vue', '신규 고객사 등록', '플랫폼 운영', 1, 1, 'Y', 'SYSTEM')
    , ('Platform_02', 'platform', 'platform/Platform_02.vue', '이용약관 관리',   '플랫폼 운영', 1, 2, 'Y', 'SYSTEM');

-- ── 3) 메뉴 권한(운영자 회사 master 만) ──
INSERT INTO `tb_syst_auth_menu`
      (`CMPNY_CD`, `AUTH_CD`, `MENU_D_ID`, `USE_YN`, `BTN_SRCH`, `BTN_NEW`, `BTN_DELT`, `BTN_SAVE`, `BTN_EXCL`, `INSERT_NO`)
VALUES
      ('prafta_system_admin', 'master', 'Platform_01', 'Y', 'Y', 'Y', 'Y', 'Y', 'Y', 'SYSTEM')
    , ('prafta_system_admin', 'master', 'Platform_02', 'Y', 'Y', 'Y', 'Y', 'Y', 'Y', 'SYSTEM');

-- ============================================================================
-- 롤백 (필요 시 수동 실행)
-- ----------------------------------------------------------------------------
-- DELETE FROM `tb_syst_auth_menu` WHERE `CMPNY_CD`='prafta_system_admin'
--    AND `MENU_D_ID` IN ('Platform_01','Platform_02');
-- DELETE FROM `tb_syst_menu_d` WHERE `MENU_D_ID` IN ('Platform_01','Platform_02');
-- DELETE FROM `tb_syst_menu_m` WHERE `MENU_M_ID`='platform';
-- ============================================================================
