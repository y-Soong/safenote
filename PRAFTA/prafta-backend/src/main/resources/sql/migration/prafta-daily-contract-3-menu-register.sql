-- ============================================================================
-- PRAFTA-daily-contract-3 — 웹 메뉴 등록 (User_07 계약서 관리 / User_08 입장 승인)
-- 작성일: 2026-07-16
-- 적용 환경: MySQL 8.0.42
-- 출처: .claude/requests/common/작업지시서_일용직-계약서-서명-승인제.plan.md §T1, §4(UI-DC-05/06)
-- 참조 스타일: prafta-daily-blacklist-2-menu-auth.sql (User_06 — dailyAcct 형제 메뉴 등록 단일 출처),
--             prafta-daily-user-dept-2-user05-menu-auth.sql (User_05),
--             prafta-050-menu-register.sql (권한 SELECT 복제 패턴),
--             prafta-lnb-1-menu-restructure.sql (dailyAcct 탭 = User_05 IDX1 / Baim_05 IDX2)
--
-- 변경 요약
--   1) tb_syst_menu_d : 신규 소메뉴 2건 등록 — 일일계정 관리(dailyAcct) 하위, User_05/06 형제.
--      - User_07 '계약서 관리'  MENU_VIEW='user/User_07.vue', SUB_GROUP_IDX=1, MENU_IDX=4
--      - User_08 '입장 승인'    MENU_VIEW='user/User_08.vue', SUB_GROUP_IDX=1, MENU_IDX=5
--        (dailyAcct 현행: User_05=1, Baim_05=2, User_06=3 → 다음 IDX=4,5)
--      - SUB_GROUP_NM=NULL (빌더가 대분류명으로 대체 — User_06 등록과 동일).
--   2) tb_syst_auth_menu : User_06 의 AUTH 세트를 SELECT 복제 (멀티테넌트 안전 — 회사별 행 그대로 복제).
--      권한 대상 = User_05/06 동일: 00001/00004/00006/00008/hr/master/safe/system ('99999' 일반근로자 제외 승계).
--      버튼은 화면 성격에 맞게 명시 지정:
--      - User_07 (등록/교체=쓰기 화면): BTN_SRCH='Y', BTN_NEW='Y'(ViewHeader 등록→DailyContractRegPop),
--        BTN_DELT='Y'(화면의 [사용중지] 버튼이 buttons.delete==='Y' 게이트 — UI-DC-05), BTN_SAVE/BTN_EXCL='N'.
--      - User_08 (승인/거부 인라인 처리 화면): BTN_SRCH='Y', 나머지 'N' (Attd_10 요청승인관리 전례).
--
-- viewResolver 는 컴포넌트명(User_07/User_08)으로 자동 로드하나, tb_syst_menu_d + tb_syst_auth_menu
-- 가 있어야 LNB 노출/진입 및 버튼 권한(BTN_*) 적용이 가능하다.
-- 실제 데이터 접근 인가는 서버 사업장 스코프(assertSiteAccess 패턴, T2/T3)가 강제한다.
--
-- ⚠️ 사전 조건: prafta-lnb-1-menu-restructure.sql(대분류 dailyAcct + SUB_GROUP_* 컬럼) 및
--    prafta-daily-blacklist-2-menu-auth.sql(User_06 — 권한 복제 출처)이 먼저 적용된 환경이어야 한다.
--
-- 적용 전 부재/현황 확인 (운영 적용 직전 필수):
--   SELECT MENU_D_ID FROM tb_syst_menu_d   WHERE MENU_D_ID IN ('User_07','User_08');   -- 0건이어야 함
--   SELECT COUNT(*) FROM tb_syst_auth_menu WHERE MENU_D_ID IN ('User_07','User_08');   -- 0건이어야 함
--   SELECT MENU_D_ID, SUB_GROUP_IDX, MENU_IDX FROM tb_syst_menu_d
--    WHERE MENU_M_ID = 'dailyAcct' ORDER BY SUB_GROUP_IDX, MENU_IDX;                    -- User_05=1, Baim_05=2, User_06=3 확인(IDX 충돌 점검)
--   SELECT CMPNY_CD, AUTH_CD, USE_YN FROM tb_syst_auth_menu
--    WHERE MENU_D_ID = 'User_06' ORDER BY CMPNY_CD, AUTH_CD;                            -- 복제 출처 실재 확인(0건이면 중단)
--
-- 멱등성: 재적용 대비 INSERT 전 DELETE 로 기존 행 제거 후 재삽입(prafta-050 패턴). 재실행 안전.
-- 운영 적용: 사용자 수동(read-only MCP). 본 파일은 작성만, DB 직접 적용 금지.
-- ============================================================================

-- ----------------------------------------------------------------------------
-- (멱등성) 기존 User_07/User_08 시드 제거
--   tb_syst_auth_menu 의 WHERE 가 MENU_D_ID(PK 선두 아님)라 Workbench 기본 설정
--   (SQL_SAFE_UPDATES=1)에서 Error 1175 로 차단됨 → 세션 한정 해제 후 말미 원복.
-- ----------------------------------------------------------------------------
SET @old_safe_updates := @@SQL_SAFE_UPDATES;
SET SQL_SAFE_UPDATES = 0;

DELETE FROM `tb_syst_auth_menu` WHERE `MENU_D_ID` IN ('User_07','User_08');
DELETE FROM `tb_syst_menu_d`    WHERE `MENU_D_ID` IN ('User_07','User_08');

-- ----------------------------------------------------------------------------
-- (1) 소메뉴 등록 (dailyAcct 대메뉴 하위, SUB_GROUP_NM=NULL, SUB_GROUP_IDX=1, IDX=4/5)
-- ----------------------------------------------------------------------------
INSERT INTO `tb_syst_menu_d`
    (`MENU_D_ID`, `MENU_M_ID`, `MENU_VIEW`, `MENU_NM`, `SUB_GROUP_NM`, `SUB_GROUP_IDX`, `MENU_IDX`, `USE_YN`, `INSERT_NO`, `INSERT_DATE`) VALUES
    ('User_07', 'dailyAcct', 'user/User_07.vue', '계약서 관리', NULL, 1, 4, 'Y', 'SYSTEM', NOW())
  , ('User_08', 'dailyAcct', 'user/User_08.vue', '입장 승인',   NULL, 1, 5, 'Y', 'SYSTEM', NOW());

-- ----------------------------------------------------------------------------
-- (2) 권한 매핑 — User_06 의 (CMPNY_CD, AUTH_CD, USE_YN) 세트를 그대로 복제(멀티테넌트 안전).
--     User_07: 계약서 등록/교체(쓰기) 화면 → BTN_SRCH='Y', BTN_NEW='Y', BTN_DELT='Y'([사용중지]), 나머지 'N'.
-- ----------------------------------------------------------------------------
INSERT INTO `tb_syst_auth_menu`
    (`CMPNY_CD`, `AUTH_CD`, `MENU_D_ID`, `USE_YN`, `BTN_SRCH`, `BTN_NEW`, `BTN_DELT`, `BTN_SAVE`, `BTN_EXCL`, `INSERT_NO`, `INSERT_DATE`)
  SELECT `CMPNY_CD`, `AUTH_CD`, 'User_07', `USE_YN`, 'Y', 'Y', 'Y', 'N', 'N', 'SYSTEM', NOW()
    FROM `tb_syst_auth_menu`
   WHERE `MENU_D_ID` = 'User_06';

-- ----------------------------------------------------------------------------
-- (3) 권한 매핑 — User_08: 승인/거부/열람은 인라인 처리 → BTN_SRCH='Y', 나머지 'N'.
-- ----------------------------------------------------------------------------
INSERT INTO `tb_syst_auth_menu`
    (`CMPNY_CD`, `AUTH_CD`, `MENU_D_ID`, `USE_YN`, `BTN_SRCH`, `BTN_NEW`, `BTN_DELT`, `BTN_SAVE`, `BTN_EXCL`, `INSERT_NO`, `INSERT_DATE`)
  SELECT `CMPNY_CD`, `AUTH_CD`, 'User_08', `USE_YN`, 'Y', 'N', 'N', 'N', 'N', 'SYSTEM', NOW()
    FROM `tb_syst_auth_menu`
   WHERE `MENU_D_ID` = 'User_06';

-- ----------------------------------------------------------------------------
-- 세션 안전모드 원복
-- ----------------------------------------------------------------------------
SET SQL_SAFE_UPDATES = @old_safe_updates;

-- ============================================================================
-- 적용 후 검증 (운영 적용 후 1회 실행)
-- ----------------------------------------------------------------------------
-- 1) 메뉴 2건 + dailyAcct 트리 순서(User_05=1 ~ User_08=5):
--    SELECT MENU_D_ID, MENU_NM, MENU_VIEW, SUB_GROUP_IDX, MENU_IDX FROM tb_syst_menu_d
--     WHERE MENU_M_ID = 'dailyAcct' ORDER BY SUB_GROUP_IDX, MENU_IDX;
-- 2) 권한 행 수 = User_06 행 수와 동일(화면별):
--    SELECT MENU_D_ID, COUNT(*) FROM tb_syst_auth_menu
--     WHERE MENU_D_ID IN ('User_06','User_07','User_08') GROUP BY MENU_D_ID;
-- 3) 버튼 매핑 확인 (User_07: NEW=Y, DELT=Y / User_08: SRCH만 Y):
--    SELECT MENU_D_ID, AUTH_CD, BTN_SRCH, BTN_NEW, BTN_DELT, BTN_SAVE, BTN_EXCL
--      FROM tb_syst_auth_menu WHERE MENU_D_ID IN ('User_07','User_08') ORDER BY MENU_D_ID, CMPNY_CD, AUTH_CD;
-- ============================================================================
-- 롤백 (필요 시 수동 실행)
-- ----------------------------------------------------------------------------
-- DELETE FROM tb_syst_auth_menu WHERE MENU_D_ID IN ('User_07','User_08');
-- DELETE FROM tb_syst_menu_d    WHERE MENU_D_ID IN ('User_07','User_08');
-- ============================================================================
