-- ============================================================================
-- PRAFTA-COM-016-H — 관리자 발신 연차 변경 요청 이력 화면(Attd_14) 메뉴/권한 등록
-- 작성일: 2026-06-18
-- 적용 환경: MySQL 8.0.42
-- 참조: .claude/requests/common/prafta-com-016-H.md (확정결정 Q1~Q6),
--       prafta-com-003-device-fraud-detect.sql §3 (Attd_12 메뉴/권한 시드 미러)
--
-- 변경 요약
--   1) tb_syst_menu_d : 신규 소메뉴 Attd_14('연차 변경 요청 이력') 등록.
--      대메뉴 = attd(근태), MENU_IDX=13 (Attd_12=12 다음). 화면=attd/Attd_14.vue.
--   2) tb_syst_auth_menu : master/hr/노드 정·부 관리자(+system)에 읽기 전용 부여.
--      ★ Attd_12 시드 세트에서 safe(safe) 행만 제외(Q6 — 연차 변경은 hr/master·노드관리자 영역).
--      읽기 전용 화면 → BTN_SRCH='Y', BTN_NEW/BTN_DELT/BTN_SAVE='N'.
--      본 화면은 엑셀 export 미제공 → BTN_EXCL='N' (Attd_12 는 'Y' 였으나 본 화면은 제공 안 함).
--
-- viewResolver 는 컴포넌트명(Attd_14)으로 자동 로드하나, tb_syst_menu_d + tb_syst_auth_menu
-- 가 있어야 LNB 노출/진입 가능(prafta-019-F / prafta-com-003 패턴).
-- 실제 데이터 접근 인가는 서버 canManageNodeExcludeSafe / canManageUserExcludeSafe 가 강제한다.
--
-- 적용 전 부재/현황 확인 (운영 적용 직전 권장):
--   SELECT * FROM tb_syst_menu_d  WHERE MENU_D_ID = 'Attd_14';
--   SELECT * FROM tb_syst_auth_menu WHERE MENU_D_ID = 'Attd_14';
--   SELECT MENU_D_ID, MENU_IDX FROM tb_syst_menu_d WHERE MENU_M_ID = 'attd' ORDER BY MENU_IDX;  -- IDX 충돌 확인
--
-- 멱등성: INSERT 중복 실행 시 PK 충돌 에러. 이미 반영된 환경에서는 건너뛸 것.
-- 운영 적용: 사용자 수동(read-only MCP). 본 파일은 작성만, DB 직접 적용 금지.
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1) 메뉴 등록 (근태 대메뉴 attd 하위, 말미 IDX=13)
-- ----------------------------------------------------------------------------
INSERT INTO tb_syst_menu_d (MENU_D_ID, MENU_M_ID, MENU_VIEW, MENU_NM, MENU_IDX, USE_YN, INSERT_NO, INSERT_DATE) VALUES
    ('Attd_14', 'attd', 'attd/Attd_14.vue', '연차 변경 요청 이력', 13, 'Y', 'SYSTEM', NOW());

-- ----------------------------------------------------------------------------
-- 2) 권한 시드 (읽기 전용). Attd_12 세트에서 safe 제외.
--    부여 대상: 00001/00004/00006/00008(노드 정·부 관리권 직책), 99999, hr, master, system.
--    읽기 전용 → BTN_SRCH='Y', BTN_NEW/BTN_DELT/BTN_SAVE='N'. 엑셀 미제공 → BTN_EXCL='N'.
-- ----------------------------------------------------------------------------
INSERT INTO tb_syst_auth_menu (CMPNY_CD, AUTH_CD, MENU_D_ID, USE_YN, BTN_SRCH, BTN_NEW, BTN_DELT, BTN_SAVE, BTN_EXCL, INSERT_NO, INSERT_DATE) VALUES
    ('001', '00001',  'Attd_14', 'Y', 'Y', 'N', 'N', 'N', 'N', 'SYSTEM', NOW())
  , ('001', '00004',  'Attd_14', 'Y', 'Y', 'N', 'N', 'N', 'N', 'SYSTEM', NOW())
  , ('001', '00006',  'Attd_14', 'Y', 'Y', 'N', 'N', 'N', 'N', 'SYSTEM', NOW())
  , ('001', '00008',  'Attd_14', 'Y', 'Y', 'N', 'N', 'N', 'N', 'SYSTEM', NOW())
  , ('001', '99999',  'Attd_14', 'Y', 'Y', 'N', 'N', 'N', 'N', 'SYSTEM', NOW())
  , ('001', 'hr',     'Attd_14', 'Y', 'Y', 'N', 'N', 'N', 'N', 'SYSTEM', NOW())
  , ('001', 'master', 'Attd_14', 'Y', 'Y', 'N', 'N', 'N', 'N', 'SYSTEM', NOW())
  , ('001', 'system', 'Attd_14', 'Y', 'Y', 'N', 'N', 'N', 'N', 'SYSTEM', NOW());
-- ※ 'safe' 는 의도적으로 제외(Q6). prafta-com-003 Attd_12 시드 대비 유일한 차이.

-- ============================================================================
-- 롤백 (필요 시 수동 실행)
-- ----------------------------------------------------------------------------
-- DELETE FROM tb_syst_auth_menu WHERE MENU_D_ID = 'Attd_14';
-- DELETE FROM tb_syst_menu_d    WHERE MENU_D_ID = 'Attd_14';
-- ============================================================================
