-- ============================================================================
-- PRAFTA-019-F — 신규 화면 메뉴 등록 (Attd_10 요청 승인 관리 / User_04 연차 결재라인 구성)
-- 작성일: 2026-05-23
-- 적용 환경: MySQL 8.0.42
--
-- viewResolver는 컴포넌트명으로 화면을 자동 로드하나, 메뉴(tb_syst_menu_d) +
-- 권한 매핑(tb_syst_auth_menu)이 있어야 LNB에 노출되고 진입 가능하다.
--  - MENU_D_ID = 화면 키(Attd_10), MENU_VIEW = views 하위 상대경로(attd/Attd_10.vue)
--  - 권한: 회사 '001'. 결재자/신청자는 일반 직원도 될 수 있어 User_01과 동일한 폭넓은 AUTH 세트 부여.
--    (요청 승인 관리는 본인 결재 대기 건만 보이므로 비결재자는 빈 목록 — 광범위 노출 무해)
--  - 버튼: 조회(BTN_SRCH)만 사용. 신규/삭제/저장/엑셀은 인라인 처리라 N.
--
-- 멱등성: INSERT 중복 실행 시 PK 충돌. 이미 등록된 환경에서는 건너뛸 것.
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1) 메뉴 상세 등록
-- ----------------------------------------------------------------------------
INSERT INTO tb_syst_menu_d (MENU_D_ID, MENU_M_ID, MENU_VIEW, MENU_NM, MENU_IDX, USE_YN, INSERT_NO, INSERT_DATE) VALUES
    ('Attd_10', 'attd', 'attd/Attd_10.vue', '요청 승인 관리',        10, 'Y', 'SYSTEM', NOW())
  , ('User_04', 'user', 'user/User_04.vue', '연차 결재라인 구성',     4, 'Y', 'SYSTEM', NOW());

-- ----------------------------------------------------------------------------
-- 2) 권한 매핑 (회사 '001', User_01 동일 세트). 조회 버튼만 활성.
-- ----------------------------------------------------------------------------
INSERT INTO tb_syst_auth_menu (CMPNY_CD, AUTH_CD, MENU_D_ID, USE_YN, BTN_SRCH, BTN_NEW, BTN_DELT, BTN_SAVE, BTN_EXCL, INSERT_NO, INSERT_DATE) VALUES
    ('001', '00001',  'Attd_10', 'Y', 'Y', 'N', 'N', 'N', 'N', 'SYSTEM', NOW())
  , ('001', '00004',  'Attd_10', 'Y', 'Y', 'N', 'N', 'N', 'N', 'SYSTEM', NOW())
  , ('001', '00006',  'Attd_10', 'Y', 'Y', 'N', 'N', 'N', 'N', 'SYSTEM', NOW())
  , ('001', '00008',  'Attd_10', 'Y', 'Y', 'N', 'N', 'N', 'N', 'SYSTEM', NOW())
  , ('001', '99999',  'Attd_10', 'Y', 'Y', 'N', 'N', 'N', 'N', 'SYSTEM', NOW())
  , ('001', 'hr',     'Attd_10', 'Y', 'Y', 'N', 'N', 'N', 'N', 'SYSTEM', NOW())
  , ('001', 'master', 'Attd_10', 'Y', 'Y', 'N', 'N', 'N', 'N', 'SYSTEM', NOW())
  , ('001', 'safe',   'Attd_10', 'Y', 'Y', 'N', 'N', 'N', 'N', 'SYSTEM', NOW())
  , ('001', 'system', 'Attd_10', 'Y', 'Y', 'N', 'N', 'N', 'N', 'SYSTEM', NOW())
  , ('001', '00001',  'User_04', 'Y', 'Y', 'N', 'N', 'N', 'N', 'SYSTEM', NOW())
  , ('001', '00004',  'User_04', 'Y', 'Y', 'N', 'N', 'N', 'N', 'SYSTEM', NOW())
  , ('001', '00006',  'User_04', 'Y', 'Y', 'N', 'N', 'N', 'N', 'SYSTEM', NOW())
  , ('001', '00008',  'User_04', 'Y', 'Y', 'N', 'N', 'N', 'N', 'SYSTEM', NOW())
  , ('001', '99999',  'User_04', 'Y', 'Y', 'N', 'N', 'N', 'N', 'SYSTEM', NOW())
  , ('001', 'hr',     'User_04', 'Y', 'Y', 'N', 'N', 'N', 'N', 'SYSTEM', NOW())
  , ('001', 'master', 'User_04', 'Y', 'Y', 'N', 'N', 'N', 'N', 'SYSTEM', NOW())
  , ('001', 'safe',   'User_04', 'Y', 'Y', 'N', 'N', 'N', 'N', 'SYSTEM', NOW())
  , ('001', 'system', 'User_04', 'Y', 'Y', 'N', 'N', 'N', 'N', 'SYSTEM', NOW());
