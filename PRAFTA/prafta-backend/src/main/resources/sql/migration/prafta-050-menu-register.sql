-- ============================================================================
-- PRAFTA-050-2 — ChkLst_04(점검 불량 관리) 소메뉴 + 권한 시드 DML
-- 작성일: 2026-06-07
-- 적용 환경: MySQL 8.0.42
-- 출처: .claude/requests/web_requests/prafta-050-plan.md §4 (운영 DB 조회로 확정된 시드)
-- 선행: prafta-050-chkpt-defect-action.sql
--
-- 등록 항목:
--   1) tb_syst_menu_d  ChkLst_04 소메뉴 (점검 대메뉴 MENU_M_ID='chkLst' 하위, MENU_IDX=4)
--      - MENU_VIEW='chkLst/ChkLst_04.vue', MENU_NM='점검 불량 관리'
--      - 현행 ChkLst_01=1 / _02=2 / _03=3 → 다음 IDX=4 (운영 DB 조회 확정).
--   2) tb_syst_auth_menu  ChkLst_04 권한 매핑 = ChkLst_03 의 AUTH_CD/USE_YN/BTN 세트를 그대로 복제.
--      (가장 안전·정확. 점검 도메인 기존 권한 정책 승계, 전용 규칙 신설 없음.)
--
-- ⚠️ 스키마 주의(검증됨): tb_syst_menu_d 에는 CMPNY_CD 컬럼이 없다(전사 공통).
--    tb_syst_auth_menu 만 CMPNY_CD(='001') 단일 테넌트. 멀티테넌트면 ChkLst_03 복제 SELECT 가
--    회사별 행을 그대로 복제하므로 추가 조치 불필요.
--
-- 적용 전 부재 확인(이미 일부 반영된 환경이면 해당 구문만 건너뛸 것):
--   SELECT MENU_D_ID FROM tb_syst_menu_d WHERE MENU_D_ID='ChkLst_04';
--   SELECT COUNT(*) FROM tb_syst_auth_menu WHERE MENU_D_ID='ChkLst_04';
--   SELECT MENU_M_ID, MENU_IDX FROM tb_syst_menu_d WHERE MENU_D_ID IN ('ChkLst_01','ChkLst_02','ChkLst_03');
-- 멱등성: 재적용 대비 INSERT 전 DELETE 로 기존 ChkLst_04 행 제거 후 재삽입(아래).
--         재실행 안전(idempotent). 운영 적용 후 보관용.
-- ============================================================================

-- ----------------------------------------------------------------------------
-- (멱등성) 기존 ChkLst_04 시드 제거
-- ----------------------------------------------------------------------------
DELETE FROM `tb_syst_auth_menu` WHERE `MENU_D_ID` = 'ChkLst_04';
DELETE FROM `tb_syst_menu_d`    WHERE `MENU_D_ID` = 'ChkLst_04';

-- ----------------------------------------------------------------------------
-- (1) 소메뉴 등록 (tb_syst_menu_d) — ChkLst_04
--     MENU_VIEW = views 하위 상대경로. viewResolver 가 컴포넌트명으로 자동 라우팅.
-- ----------------------------------------------------------------------------
INSERT INTO `tb_syst_menu_d`
    (`MENU_D_ID`, `MENU_M_ID`, `MENU_VIEW`, `MENU_NM`, `MENU_IDX`, `USE_YN`, `INSERT_NO`, `INSERT_DATE`) VALUES
    ('ChkLst_04', 'chkLst', 'chkLst/ChkLst_04.vue', '점검 불량 관리', 4, 'Y', 'SYSTEM', NOW());

-- ----------------------------------------------------------------------------
-- (2) 권한 매핑 (tb_syst_auth_menu) — ChkLst_03 의 권한 세트를 그대로 복제.
--     (00001/00004/00006/00008/99999/master/safe = 전부 Y, hr = 전부 N,
--      system = USE_YN/BTN_SRCH Y · NEW/DELT/SAVE/EXCL N — ChkLst_03 실제 행을 그대로 따름.)
-- ----------------------------------------------------------------------------
INSERT INTO `tb_syst_auth_menu`
    (`CMPNY_CD`, `AUTH_CD`, `MENU_D_ID`, `USE_YN`, `BTN_SRCH`, `BTN_NEW`, `BTN_DELT`, `BTN_SAVE`, `BTN_EXCL`, `INSERT_NO`, `INSERT_DATE`)
  SELECT `CMPNY_CD`, `AUTH_CD`, 'ChkLst_04', `USE_YN`, `BTN_SRCH`, `BTN_NEW`, `BTN_DELT`, `BTN_SAVE`, `BTN_EXCL`, 'SYSTEM', NOW()
    FROM `tb_syst_auth_menu`
   WHERE `MENU_D_ID` = 'ChkLst_03';

-- ============================================================================
-- 끝. 적용 후 검증:
--   SELECT MENU_NM FROM tb_syst_menu_d WHERE MENU_D_ID='ChkLst_04';        -- 점검 불량 관리
--   SELECT MENU_VIEW FROM tb_syst_menu_d WHERE MENU_D_ID='ChkLst_04';      -- chkLst/ChkLst_04.vue
--   SELECT COUNT(*) FROM tb_syst_auth_menu WHERE MENU_D_ID='ChkLst_04';    -- ChkLst_03 권한 행 수와 동일
-- ============================================================================
