-- ============================================================================
-- PRAFTA-COM-008-A — 연차 사용촉진(2차) 웹 화면 메뉴/권한 시드
-- 작성일: 2026-06-12
-- 적용 환경: MySQL 8.0.42
-- 참조: prafta-com-008-A-promotion.md §3(2차 회사직권 웹 화면)/§3-6(노드 권한)
--       Attd_09(사용자 연차관리) 시드 관례 미러 — auth_menu master 1행만(메모리 prafta-047)
--
-- 변경 요약
--  1) tb_syst_menu_d — 근태관리(attd) 대메뉴 하위 IDX 13 "연차 사용촉진(2차)" 신규.
--     화면 컴포넌트 = prafta-web-frontend src/views/leave/LeavePromotion_01.vue (viewResolver 자동 로드).
--  2) tb_syst_auth_menu — master(CMPNY_CD='001') 1행 시드(Attd_09 동일 관례).
--     hr/safe 전사접근·노드 main/sub 접근은 MenuLockPolicy / BE canManageNode 게이트가 처리(별도 시드 불요).
--
-- 멱등성: INSERT 중복 실행 시 PK 충돌 에러. 이미 반영된 환경에서는 건너뛸 것.
-- 적용: 사용자(운영자)가 직접 적용(MCP read-only). 미적용 시 화면이 메뉴에 노출되지 않음
--       (단, 백엔드 EP 자체는 동작 — 메뉴 노출만의 문제).
-- ============================================================================

-- ── 1) 메뉴 디테일 (근태관리 하위 IDX 13) ──
INSERT INTO `tb_syst_menu_d`
      (`MENU_D_ID`, `MENU_M_ID`, `MENU_VIEW`, `MENU_NM`, `MENU_IDX`, `USE_YN`, `INSERT_NO`)
VALUES
      ('LeavePromotion_01', 'attd', 'leave/LeavePromotion_01.vue', '연차 사용촉진(2차)', 13, 'Y', 'SYSTEM');

-- ── 2) 메뉴 권한 (master 1행 — Attd_09 관례 미러) ──
--   BTN_SRCH(조회)/BTN_NEW/BTN_DELT/BTN_SAVE(지정)/BTN_EXCL(엑셀) — Attd_09 동일 'Y'.
--   화면의 실제 버튼 노출은 FE fnButtonControll, 실 인가는 BE(canManageNode/canManageUser)가 강제.
INSERT INTO `tb_syst_auth_menu`
      (`CMPNY_CD`, `AUTH_CD`, `MENU_D_ID`, `USE_YN`, `BTN_SRCH`, `BTN_NEW`, `BTN_DELT`, `BTN_SAVE`, `BTN_EXCL`, `INSERT_NO`)
VALUES
      ('001', 'master', 'LeavePromotion_01', 'Y', 'Y', 'Y', 'Y', 'Y', 'Y', 'SYSTEM');
