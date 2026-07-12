-- ============================================================================
-- PRAFTA-daily-blacklist-2 — 블랙리스트 관리 화면(User_06) 메뉴/권한 등록
-- 작성일: 2026-06-28
-- 적용 환경: MySQL 8.0.42
-- 출처: .claude/requests/web_requests/prafta-daily-blacklist.md (확정 결정 §4),
--       prafta-lnb-1-menu-restructure.sql (dailyAcct 탭 = User_05 IDX1 / Baim_05 IDX2),
--       prafta-daily-user-dept-2-user05-menu-auth.sql (User_05 권한 AUTH 세트 단일 출처)
--
-- 변경 요약
--   1) tb_syst_menu_d : 신규 소메뉴 User_06('일일계정 블랙리스트') 등록.
--      대메뉴 = dailyAcct(일일계정 관리, LNB 재편 후), MENU_VIEW='user/User_06.vue',
--      SUB_GROUP_NM=NULL(빌더가 대분류명으로 대체), SUB_GROUP_IDX=1, MENU_IDX=3 (User_05=1, Baim_05=2 다음).
--   2) tb_syst_auth_menu : User_05 와 동일 AUTH 세트 복제.
--      ★ 본 화면은 등록/해제(쓰기)가 필요 → BTN_SRCH/BTN_NEW/BTN_DELT='Y'.
--        BTN_SAVE/BTN_EXCL 는 User_05 값(='N')을 따른다(별도 저장/엑셀 버튼 없음).
--      권한 대상 AUTH_CD 집합 = User_05 동일: 00001/00004/00006/00008/hr/master/safe/system.
--      (요청서 결정: 화면 접근 권한이 있는 사용자는 모두 등록/해제 가능 — master/hr로 좁히지 않음.)
--
-- viewResolver 는 컴포넌트명(User_06)으로 자동 로드하나, tb_syst_menu_d + tb_syst_auth_menu
-- 가 있어야 LNB 노출/진입 및 버튼 권한(BTN_*) 적용이 가능하다.
-- 서버 데이터 접근 인가는 JWT 클레임(CMPNY_CD) 스코프가 강제한다(블랙리스트는 회사 단위).
--
-- ⚠️ 사전 조건: prafta-lnb-1-menu-restructure.sql(대분류 dailyAcct + SUB_GROUP_* 컬럼)이
--    먼저 적용된 환경이어야 한다. SUB_GROUP_NM/SUB_GROUP_IDX 컬럼이 없으면 (1) INSERT 에서
--    컬럼 부재 에러가 난다. 미적용 환경이면 아래 (1') 폴백(컬럼 미포함)을 사용할 것.
--
-- 적용 전 부재/현황 확인(운영 적용 직전 권장):
--   SELECT * FROM tb_syst_menu_d   WHERE MENU_D_ID = 'User_06';                       -- 0건이어야 함
--   SELECT * FROM tb_syst_auth_menu WHERE MENU_D_ID = 'User_06';                       -- 0건이어야 함
--   SELECT MENU_D_ID, MENU_M_ID, SUB_GROUP_IDX, MENU_IDX FROM tb_syst_menu_d
--    WHERE MENU_M_ID = 'dailyAcct' ORDER BY SUB_GROUP_IDX, MENU_IDX;                    -- User_05=1, Baim_05=2 확인
--   SHOW COLUMNS FROM tb_syst_menu_d LIKE 'SUB_GROUP_NM';                               -- 존재해야 (1) 사용
--   SELECT AUTH_CD, BTN_SRCH, BTN_NEW, BTN_DELT, BTN_SAVE, BTN_EXCL
--     FROM tb_syst_auth_menu WHERE MENU_D_ID = 'User_05' ORDER BY AUTH_CD;              -- 복제 출처 대조
--
-- 멱등성: INSERT 중복 실행 시 PK 충돌 에러. 이미 반영된 환경에서는 건너뛸 것.
-- 운영 적용: 사용자 수동(read-only MCP). 본 파일은 작성만, DB 직접 적용 금지.
-- ============================================================================

-- ----------------------------------------------------------------------------
-- (1) 메뉴 등록 (dailyAcct 대메뉴 하위, SUB_GROUP_NM=NULL, SUB_GROUP_IDX=1, IDX=3)
--     ※ SUB_GROUP_* 컬럼이 있는 환경(LNB 재편 적용 후) 기준.
-- ----------------------------------------------------------------------------
INSERT INTO tb_syst_menu_d
    (MENU_D_ID, MENU_M_ID, MENU_VIEW, MENU_NM, SUB_GROUP_NM, SUB_GROUP_IDX, MENU_IDX, USE_YN, INSERT_NO, INSERT_DATE) VALUES
    ('User_06', 'dailyAcct', 'user/User_06.vue', '일일계정 블랙리스트', NULL, 1, 3, 'Y', 'SYSTEM', NOW());

-- ----------------------------------------------------------------------------
-- (1') 폴백 — SUB_GROUP_* 컬럼이 아직 없는 환경(LNB 재편 미적용)이면 위 (1) 대신 아래 사용.
--      (그 경우 대메뉴는 LNB 재편 전 값 'user' 가 맞을 수 있으니 운영 트리에 맞춰 조정할 것.)
-- ----------------------------------------------------------------------------
-- INSERT INTO tb_syst_menu_d
--     (MENU_D_ID, MENU_M_ID, MENU_VIEW, MENU_NM, MENU_IDX, USE_YN, INSERT_NO, INSERT_DATE) VALUES
--     ('User_06', 'dailyAcct', 'user/User_06.vue', '일일계정 블랙리스트', 3, 'Y', 'SYSTEM', NOW());

-- ----------------------------------------------------------------------------
-- (2) 권한 시드 — User_05 동일 AUTH 세트 복제.
--     등록/해제(쓰기) 화면 → BTN_SRCH='Y', BTN_NEW='Y', BTN_DELT='Y'.
--     BTN_SAVE/BTN_EXCL 는 User_05 값(='N') 그대로(별도 저장/엑셀 버튼 없음).
-- ----------------------------------------------------------------------------
INSERT INTO tb_syst_auth_menu (CMPNY_CD, AUTH_CD, MENU_D_ID, USE_YN, BTN_SRCH, BTN_NEW, BTN_DELT, BTN_SAVE, BTN_EXCL, INSERT_NO, INSERT_DATE) VALUES
    ('001', '00001',  'User_06', 'Y', 'Y', 'Y', 'Y', 'N', 'N', 'SYSTEM', NOW())
  , ('001', '00004',  'User_06', 'Y', 'Y', 'Y', 'Y', 'N', 'N', 'SYSTEM', NOW())
  , ('001', '00006',  'User_06', 'Y', 'Y', 'Y', 'Y', 'N', 'N', 'SYSTEM', NOW())
  , ('001', '00008',  'User_06', 'Y', 'Y', 'Y', 'Y', 'N', 'N', 'SYSTEM', NOW())
  , ('001', 'hr',     'User_06', 'Y', 'Y', 'Y', 'Y', 'N', 'N', 'SYSTEM', NOW())
  , ('001', 'master', 'User_06', 'Y', 'Y', 'Y', 'Y', 'N', 'N', 'SYSTEM', NOW())
  , ('001', 'safe',   'User_06', 'Y', 'Y', 'Y', 'Y', 'N', 'N', 'SYSTEM', NOW())
  , ('001', 'system', 'User_06', 'Y', 'Y', 'Y', 'Y', 'N', 'N', 'SYSTEM', NOW());

-- ============================================================================
-- 롤백 (필요 시 수동 실행)
-- ----------------------------------------------------------------------------
-- DELETE FROM tb_syst_auth_menu WHERE MENU_D_ID = 'User_06';
-- DELETE FROM tb_syst_menu_d    WHERE MENU_D_ID = 'User_06';
-- ============================================================================
