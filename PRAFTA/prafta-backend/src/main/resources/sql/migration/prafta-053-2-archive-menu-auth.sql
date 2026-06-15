-- ============================================================================
-- PRAFTA-053-2 — 자료실(Archive) 메뉴 + 권한 DML
-- 작성일: 2026-06-08
-- 적용 환경: MySQL 8.0.42
-- 출처: .claude/requests/web_requests/ref/prafta-053/prafta-053-plan.md §2(053-6), 확정사항
-- 선행: prafta-053-1-notice-archive-ddl.sql, prafta-047-2-notice-codes-menu-auth.sql
--       (대메뉴 notice, SYS010 '005' 공지첨부는 047-2 에서 이미 등록됨 → 재사용. 신규 SYS 코드 없음)
--
-- 등록 항목:
--   1) tb_syst_menu_d  Notice_02 소메뉴 (MENU_VIEW='notice/Notice_02.vue', MENU_NM='안전자료실', IDX=2)
--      → 대메뉴 notice 하위. Notice_01(공지사항 관리, IDX=1) 다음.
--   2) tb_syst_auth_menu  master/hr/safe 3행 (CMPNY_CD='001', BTN_SRCH/NEW/SAVE/DELT='Y', BTN_EXCL='N')
--
-- ⚠️ 스키마 주의(047-2 검증 미러): tb_syst_menu_m/d 에는 CMPNY_CD 컬럼이 없다(전사 공통).
--    tb_syst_auth_menu 만 CMPNY_CD='001' 단일 테넌트. 멀티테넌트면 권한 행을 회사별로 추가.
--    자료실은 NEW/UPDATE/SAVE 가 핵심이고 엑셀 내보내기는 미사용 → BTN_EXCL='N'.
--
-- 적용 전 부재 확인(이미 일부 반영된 환경이면 해당 구문만 건너뛸 것):
--   SELECT MENU_M_ID FROM tb_syst_menu_m WHERE MENU_M_ID='notice';           -- 1 (047-2 선행)
--   SELECT MENU_D_ID FROM tb_syst_menu_d WHERE MENU_D_ID='Notice_02';        -- 0 이어야 함
--   SELECT MENU_IDX FROM tb_syst_menu_d WHERE MENU_M_ID='notice';            -- IDX 충돌 확인(1=Notice_01)
--   SELECT COUNT(*) FROM tb_syst_auth_menu WHERE MENU_D_ID='Notice_02';      -- 0 이어야 함
-- 멱등성: PK 중복 시 에러. 운영 적용 후 보관용(재실행 금지).
-- ============================================================================

-- ----------------------------------------------------------------------------
-- (1) 소메뉴 등록 (tb_syst_menu_d) — Notice_02 안전자료실
--     MENU_VIEW = views 하위 상대경로. viewResolver 가 컴포넌트명으로 자동 라우팅.
-- ----------------------------------------------------------------------------
INSERT INTO `tb_syst_menu_d` (`MENU_D_ID`, `MENU_M_ID`, `MENU_VIEW`, `MENU_NM`, `MENU_IDX`, `USE_YN`, `INSERT_NO`, `INSERT_DATE`) VALUES
    ('Notice_02', 'notice', 'notice/Notice_02.vue', '안전자료실', 2, 'Y', 'SYSTEM', NOW());

-- ----------------------------------------------------------------------------
-- (2) 권한 매핑 (tb_syst_auth_menu) — master / hr / safe 3행
--     USE_YN='Y' + 조회/신규/저장/삭제='Y', 엑셀='N'(자료실 엑셀 내보내기 미사용).
-- ----------------------------------------------------------------------------
INSERT INTO `tb_syst_auth_menu`
    (`CMPNY_CD`, `AUTH_CD`, `MENU_D_ID`, `USE_YN`, `BTN_SRCH`, `BTN_NEW`, `BTN_DELT`, `BTN_SAVE`, `BTN_EXCL`, `INSERT_NO`, `INSERT_DATE`) VALUES
    ('001', 'master', 'Notice_02', 'Y', 'Y', 'Y', 'Y', 'Y', 'N', 'SYSTEM', NOW()),
    ('001', 'hr',     'Notice_02', 'Y', 'Y', 'Y', 'Y', 'Y', 'N', 'SYSTEM', NOW()),
    ('001', 'safe',   'Notice_02', 'Y', 'Y', 'Y', 'Y', 'Y', 'N', 'SYSTEM', NOW());

-- ============================================================================
-- 끝. 적용 후 검증:
--   SELECT MENU_VIEW FROM tb_syst_menu_d WHERE MENU_D_ID='Notice_02';   -- notice/Notice_02.vue
--   SELECT COUNT(*) FROM tb_syst_auth_menu WHERE MENU_D_ID='Notice_02'; -- 3 (master/hr/safe)
-- ============================================================================
