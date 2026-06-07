-- ============================================================================
-- PRAFTA-047-2 — 공지사항 SYS 코드 + 메뉴 + 권한 DML
-- 작성일: 2026-06-05
-- 적용 환경: MySQL 8.0.42
-- 출처: .claude/requests/web_requests/prafta-047.md §3, §2-1, §12
--       .claude/requests/web_requests/prafta-047-plan.md §6 (PRAFTA-047-2)
-- 선행: prafta-047-1-notice-ddl.sql
--
-- 등록 항목:
--   1) SYS010(FILE_TYPE) '005' 공지첨부 추가 (현행 최대 004 아차사고 → 다음 미사용 005, MCP 확인됨)
--   2) tb_syst_menu_m  notice 대메뉴 (웹 SYS007='001', IDX=8 — nearMiss=7 다음)
--   3) tb_syst_menu_d  Notice_01 소메뉴 (MENU_VIEW='notice/Notice_01.vue')
--   4) tb_syst_auth_menu  master 행만 시드(모든 BTN='Y', USE_YN='Y').
--      나머지 역할은 고객이 권한관리 화면에서 직접 ON(요청서 §2-1). nearMiss 시드 정책과 동일.
--
-- ⚠️ 스키마 주의(MCP 검증됨): tb_syst_val_m/d, tb_syst_menu_m/d 에는 CMPNY_CD 컬럼이 없다(전사 공통).
--    tb_syst_auth_menu 만 CMPNY_CD='001' 단일 테넌트. 멀티테넌트면 권한 행을 회사별로 추가.
--
-- 적용 전 부재 확인(이미 일부 반영된 환경이면 해당 구문만 건너뛸 것):
--   SELECT SYST_VAL_D_CD FROM tb_syst_val_d WHERE SYST_VAL_CD='SYS010' AND SYST_VAL_D_CD='005';
--   SELECT MENU_M_ID FROM tb_syst_menu_m WHERE MENU_M_ID='notice';
--   SELECT MENU_D_ID FROM tb_syst_menu_d WHERE MENU_D_ID='Notice_01';
--   SELECT MAX(MENU_IDX) FROM tb_syst_menu_m WHERE MENU_SRC='001';  -- 충돌 시 IDX 조정
--   SELECT COUNT(*) FROM tb_syst_auth_menu WHERE MENU_D_ID='Notice_01';
-- 멱등성: PK 중복 시 에러. 운영 적용 후 보관용(재실행 금지).
-- ============================================================================

-- ----------------------------------------------------------------------------
-- (1) SYS010(FILE_TYPE)에 '005' 공지첨부 추가
--     현행 SYS010: 001 일일점검 / 002 위험성평가 / 003 TBM / 004 아차사고 ('005' 미사용 확인됨).
--     공지 첨부 저장 시 tb_file_info.FILE_TYPE='005' 사용.
-- ----------------------------------------------------------------------------
INSERT INTO `tb_syst_val_d`
    (`SYST_VAL_CD`, `SYST_VAL_D_CD`, `SYST_VAL_D_NM`, `SORT_IDX`, `USE_YN`, `INSERT_NO`) VALUES
    ('SYS010', '005', '공지첨부', 5, 'Y', 'SYSTEM');

-- ----------------------------------------------------------------------------
-- (2) 대메뉴 등록 (tb_syst_menu_m) — notice, 웹[SYS007 '001'], IDX=8 (nearMiss=7 다음)
-- ----------------------------------------------------------------------------
INSERT INTO `tb_syst_menu_m` (`MENU_M_ID`, `MENU_SRC`, `MENU_NM`, `MENU_IDX`, `USE_YN`, `INSERT_NO`, `INSERT_DATE`) VALUES
    ('notice', '001', '공지사항', 8, 'Y', 'SYSTEM', NOW());

-- ----------------------------------------------------------------------------
-- (3) 소메뉴 등록 (tb_syst_menu_d) — Notice_01
--     MENU_VIEW = views 하위 상대경로. viewResolver 가 컴포넌트명으로 자동 라우팅.
-- ----------------------------------------------------------------------------
INSERT INTO `tb_syst_menu_d` (`MENU_D_ID`, `MENU_M_ID`, `MENU_VIEW`, `MENU_NM`, `MENU_IDX`, `USE_YN`, `INSERT_NO`, `INSERT_DATE`) VALUES
    ('Notice_01', 'notice', 'notice/Notice_01.vue', '공지사항 관리', 1, 'Y', 'SYSTEM', NOW());

-- ----------------------------------------------------------------------------
-- (4) 권한 매핑 (tb_syst_auth_menu) — master 행만 시드.
--     master: USE_YN='Y' + 모든 BTN='Y'(조회/생성/저장/삭제/엑셀). 사용자 지시: master 접근/생성 보장.
--     그 외 역할(00001/00004/00006/00008/99999/hr/safe/system)은 시드하지 않음 →
--       고객이 권한관리 화면에서 직접 ON/OFF(요청서 §2-1 발행권한=버튼권한, 공지 전용 규칙 신설 금지).
-- ----------------------------------------------------------------------------
INSERT INTO `tb_syst_auth_menu`
    (`CMPNY_CD`, `AUTH_CD`, `MENU_D_ID`, `USE_YN`, `BTN_SRCH`, `BTN_NEW`, `BTN_DELT`, `BTN_SAVE`, `BTN_EXCL`, `INSERT_NO`, `INSERT_DATE`) VALUES
    ('001', 'master', 'Notice_01', 'Y', 'Y', 'Y', 'Y', 'Y', 'Y', 'SYSTEM', NOW());

-- ============================================================================
-- 끝. 적용 후 검증:
--   SELECT SYST_VAL_D_NM FROM tb_syst_val_d WHERE SYST_VAL_CD='SYS010' AND SYST_VAL_D_CD='005'; -- 공지첨부
--   SELECT MENU_NM FROM tb_syst_menu_m WHERE MENU_M_ID='notice';        -- 공지사항
--   SELECT MENU_VIEW FROM tb_syst_menu_d WHERE MENU_D_ID='Notice_01';   -- notice/Notice_01.vue
--   SELECT COUNT(*) FROM tb_syst_auth_menu WHERE MENU_D_ID='Notice_01'; -- 1 (master)
-- ============================================================================
