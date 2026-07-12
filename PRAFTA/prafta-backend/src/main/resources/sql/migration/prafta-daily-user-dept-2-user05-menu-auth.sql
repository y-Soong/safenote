-- ============================================================================
-- PRAFTA-daily-user-dept-2 — 일일사용자 관리(조회) 화면(User_05) 메뉴/권한 등록
-- 작성일: 2026-06-24
-- 적용 환경: MySQL 8.0.42
-- 참조: .claude/requests/web_requests/prafta-daily-user-dept-and-mgmt.md (확정 결정 D5/D6, #A),
--       prafta-019-F-menu-register.sql (User_01/User_04 = user 대메뉴 권한 세트 단일 출처)
--
-- 변경 요약
--   1) tb_syst_menu_d : 신규 소메뉴 User_05('일일사용자 관리') 등록.
--      대메뉴 = user(사용자관리), MENU_VIEW='user/User_05.vue', MENU_IDX=5 (User_04=4 다음).
--   2) tb_syst_auth_menu : User_01/User_04(user 대메뉴)와 동일 AUTH 세트 복제.
--      ★ 조회(읽기) 전용 화면(D5) → BTN_SRCH='Y', BTN_NEW/BTN_DELT/BTN_SAVE/BTN_EXCL='N'.
--      (User_01 은 생성/저장이 'Y' 였으나 본 화면은 조회 전용이라 'N'.)
--
-- viewResolver 는 컴포넌트명(User_05)으로 자동 로드하나, tb_syst_menu_d + tb_syst_auth_menu
-- 가 있어야 LNB 노출/진입 가능(prafta-019-F / prafta-com-016-H 패턴).
-- 실제 데이터 접근 인가는 서버 assertSiteAccess(사업장 스코프)가 강제한다.
--
-- 적용 전 부재/현황 확인 (운영 적용 직전 권장):
--   SELECT * FROM tb_syst_menu_d  WHERE MENU_D_ID = 'User_05';
--   SELECT * FROM tb_syst_auth_menu WHERE MENU_D_ID = 'User_05';
--   SELECT MENU_D_ID, MENU_IDX FROM tb_syst_menu_d WHERE MENU_M_ID = 'user' ORDER BY MENU_IDX;  -- IDX 충돌 확인
--
-- 멱등성: INSERT 중복 실행 시 PK 충돌 에러. 이미 반영된 환경에서는 건너뛸 것.
-- 운영 적용: 사용자 수동(read-only MCP). 본 파일은 작성만, DB 직접 적용 금지.
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1) 메뉴 등록 (사용자관리 대메뉴 user 하위, IDX=5)
-- ----------------------------------------------------------------------------
INSERT INTO tb_syst_menu_d (MENU_D_ID, MENU_M_ID, MENU_VIEW, MENU_NM, MENU_IDX, USE_YN, INSERT_NO, INSERT_DATE) VALUES
    ('User_05', 'user', 'user/User_05.vue', '일일사용자 관리', 5, 'Y', 'SYSTEM', NOW());

-- ----------------------------------------------------------------------------
-- 2) 권한 시드 (읽기 전용). 일일사용자 PII 목록 = 관리 목적 화면.
--    ★ 보안 검토(Medium) 반영: 일반근로자/일용직 본인(AUTH_CD='99999')은 제외한다.
--      User_05는 같은 사업장 일일사용자의 마스킹 PII·점유이력을 나열하므로(자기-필터 없음),
--      User_01/04 세트를 복제하되 '99999'만 빼서 관리 역할로 한정한다.
--    부여 대상: 00001/00004/00006/00008(노드 정·부 관리권 직책), hr, master, safe, system.
--    읽기 전용 → BTN_SRCH='Y', BTN_NEW/BTN_DELT/BTN_SAVE/BTN_EXCL='N'.
-- ----------------------------------------------------------------------------
INSERT INTO tb_syst_auth_menu (CMPNY_CD, AUTH_CD, MENU_D_ID, USE_YN, BTN_SRCH, BTN_NEW, BTN_DELT, BTN_SAVE, BTN_EXCL, INSERT_NO, INSERT_DATE) VALUES
    ('001', '00001',  'User_05', 'Y', 'Y', 'N', 'N', 'N', 'N', 'SYSTEM', NOW())
  , ('001', '00004',  'User_05', 'Y', 'Y', 'N', 'N', 'N', 'N', 'SYSTEM', NOW())
  , ('001', '00006',  'User_05', 'Y', 'Y', 'N', 'N', 'N', 'N', 'SYSTEM', NOW())
  , ('001', '00008',  'User_05', 'Y', 'Y', 'N', 'N', 'N', 'N', 'SYSTEM', NOW())
  , ('001', 'hr',     'User_05', 'Y', 'Y', 'N', 'N', 'N', 'N', 'SYSTEM', NOW())
  , ('001', 'master', 'User_05', 'Y', 'Y', 'N', 'N', 'N', 'N', 'SYSTEM', NOW())
  , ('001', 'safe',   'User_05', 'Y', 'Y', 'N', 'N', 'N', 'N', 'SYSTEM', NOW())
  , ('001', 'system', 'User_05', 'Y', 'Y', 'N', 'N', 'N', 'N', 'SYSTEM', NOW());

-- ============================================================================
-- 롤백 (필요 시 수동 실행)
-- ----------------------------------------------------------------------------
-- DELETE FROM tb_syst_auth_menu WHERE MENU_D_ID = 'User_05';
-- DELETE FROM tb_syst_menu_d    WHERE MENU_D_ID = 'User_05';
-- ============================================================================
