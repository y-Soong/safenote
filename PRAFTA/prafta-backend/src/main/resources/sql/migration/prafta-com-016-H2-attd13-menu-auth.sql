-- ============================================================================
-- PRAFTA-COM-016-H2 — 관리자 연차 변경/삭제 동의 관리 화면(Attd_13) 메뉴/권한 등록
-- 작성일: 2026-06-20
-- 적용 환경: MySQL 8.0.42
-- 참조: prafta-com-016-H-attd14-menu-auth.sql (자매 화면 Attd_14 시드 미러),
--       Attd_13.vue 헤더 주석(관리자 연차 변경/삭제 동의 관리, prafta-com-008-C-3)
--
-- 배경
--   Attd_13.vue(연차 변경/삭제 처리 화면)는 컴포넌트는 존재하나 tb_syst_menu_d /
--   tb_syst_auth_menu 시드가 없어 LNB 미노출·진입 불가 상태였다(viewResolver 는
--   컴포넌트명으로 자동 로드하나 메뉴+권한 행이 있어야 노출/진입 가능).
--   또한 화면이 props.buttons(메뉴 BTN_*)에서 헤더 버튼을 받으므로, 시드가 없으면
--   조회 버튼도 렌더되지 않는다. 본 시드로 LNB 노출 + 조회 버튼 노출을 함께 해결한다.
--
-- 변경 요약
--   1) tb_syst_menu_d : 신규 소메뉴 Attd_13('연차 변경 동의 관리') 등록.
--      대메뉴 = attd(근태). 화면=attd/Attd_13.vue.
--      ★ LNB/권한관리 화면의 메뉴 정렬은 MENU_IDX 가 아니라 MENU_D_ID 기준이다
--        (User02Mapper ORDER BY A.MENU_SRC, A.MENU_M_ID, A.MENU_D_ID — 어떤 매퍼 ORDER BY 에도
--         MENU_IDX 없음). 따라서 'Attd_13' 은 IDX 값과 무관하게 Attd_12 와 Attd_14 사이에
--         자연 배치된다. MENU_IDX=13 은 표기상 값일 뿐이며 Attd_14(=13)와의 충돌은 무해하다
--         (기존에도 Attd_14·LeavePromotion_01 이 동일 IDX 13 으로 공존).
--   2) tb_syst_auth_menu : Attd_14 시드 세트와 동일하게 부여(자매 화면 동일 도메인).
--      부여 대상: 00001/00004/00006/00008(노드 정·부 관리권 직책), 99999, hr, master, system.
--      ★ 'safe' 는 의도적으로 제외(연차 변경은 hr/master·노드관리자 영역 — Attd_14 와 동일).
--      쓰기 액션(발의/확인/반려)은 그리드 행 단위 팝업이며 헤더 버튼이 아니므로,
--      헤더 버튼은 조회만 노출: BTN_SRCH='Y', BTN_NEW/BTN_DELT/BTN_SAVE='N'.
--      엑셀 미제공 → BTN_EXCL='N'. (실제 쓰기 인가는 서버 canManageNodeExcludeSafe 등이 강제)
--
-- 적용 전 부재/현황 확인 (운영 적용 직전 권장):
--   SELECT * FROM tb_syst_menu_d  WHERE MENU_D_ID = 'Attd_13';
--   SELECT * FROM tb_syst_auth_menu WHERE MENU_D_ID = 'Attd_13';
--   SELECT MENU_D_ID, MENU_IDX FROM tb_syst_menu_d WHERE MENU_M_ID = 'attd' ORDER BY MENU_D_ID;
--
-- 멱등성: INSERT 중복 실행 시 PK 충돌 에러. 이미 반영된 환경에서는 건너뛸 것.
-- 운영 적용: 사용자 수동(read-only MCP). 본 파일은 작성만, DB 직접 적용 금지.
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1) 메뉴 등록 (근태 대메뉴 attd 하위). 정렬은 MENU_D_ID 기준이라 Attd_12~Attd_14 사이 배치.
-- ----------------------------------------------------------------------------
INSERT INTO tb_syst_menu_d (MENU_D_ID, MENU_M_ID, MENU_VIEW, MENU_NM, MENU_IDX, USE_YN, INSERT_NO, INSERT_DATE) VALUES
    ('Attd_13', 'attd', 'attd/Attd_13.vue', '연차 변경 동의 관리', 13, 'Y', 'SYSTEM', NOW());

-- ----------------------------------------------------------------------------
-- 2) 권한 시드. Attd_14 세트와 동일(safe 제외). 헤더는 조회만(BTN_SRCH='Y').
-- ----------------------------------------------------------------------------
INSERT INTO tb_syst_auth_menu (CMPNY_CD, AUTH_CD, MENU_D_ID, USE_YN, BTN_SRCH, BTN_NEW, BTN_DELT, BTN_SAVE, BTN_EXCL, INSERT_NO, INSERT_DATE) VALUES
    ('001', '00001',  'Attd_13', 'Y', 'Y', 'N', 'N', 'N', 'N', 'SYSTEM', NOW())
  , ('001', '00004',  'Attd_13', 'Y', 'Y', 'N', 'N', 'N', 'N', 'SYSTEM', NOW())
  , ('001', '00006',  'Attd_13', 'Y', 'Y', 'N', 'N', 'N', 'N', 'SYSTEM', NOW())
  , ('001', '00008',  'Attd_13', 'Y', 'Y', 'N', 'N', 'N', 'N', 'SYSTEM', NOW())
  , ('001', '99999',  'Attd_13', 'Y', 'Y', 'N', 'N', 'N', 'N', 'SYSTEM', NOW())
  , ('001', 'hr',     'Attd_13', 'Y', 'Y', 'N', 'N', 'N', 'N', 'SYSTEM', NOW())
  , ('001', 'master', 'Attd_13', 'Y', 'Y', 'N', 'N', 'N', 'N', 'SYSTEM', NOW())
  , ('001', 'system', 'Attd_13', 'Y', 'Y', 'N', 'N', 'N', 'N', 'SYSTEM', NOW());
-- ※ 'safe' 는 의도적으로 제외(Attd_14 시드와 동일 정책).

-- ============================================================================
-- 롤백 (필요 시 수동 실행)
-- ----------------------------------------------------------------------------
-- DELETE FROM tb_syst_auth_menu WHERE MENU_D_ID = 'Attd_13';
-- DELETE FROM tb_syst_menu_d    WHERE MENU_D_ID = 'Attd_13';
-- ============================================================================
