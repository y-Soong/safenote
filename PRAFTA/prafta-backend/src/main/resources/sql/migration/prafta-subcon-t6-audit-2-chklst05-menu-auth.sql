-- ============================================================
-- PRAFTA-SUBCON-T6-AUDIT-01 — ChkLst_05(순회점검 이력 조회) 메뉴/권한 시드
-- 작성일: 2026-07-15 / 출처: PRAFTA-SUBCON-T6-AUDIT.plan.md §3-2, prafta-050-menu-register.sql 방식 승계
--
-- ★★★ 적용 전 반드시 아래 SELECT 로 live 값 확인(그룹/순번은 환경 의존) ★★★
--   SELECT MENU_D_ID, MENU_M_ID, SUB_GROUP_NM, SUB_GROUP_IDX, MENU_IDX
--     FROM tb_syst_menu_d WHERE MENU_D_ID IN ('ChkLst_03','ChkLst_04') ORDER BY SUB_GROUP_IDX, MENU_IDX;
--   → ChkLst_04 의 (MENU_M_ID, SUB_GROUP_NM, SUB_GROUP_IDX) 를 그대로 계승하고
--     MENU_IDX = ChkLst_04.MENU_IDX + 1 로 둔다.
--
-- ┌─ [메뉴 위치 = 라이브 DB 실측 확정 (2026-07-15 메인 세션 MCP)] ──────────────────────
-- │ 커밋된 prafta-lnb-1-menu-restructure.sql 은 '안전 관리'/MENU_IDX 5~6 로 재편하나,
-- │ 이 마이그레이션은 대상 DB 에 미적용 상태다(라이브 실측: ChkLst_03/04 는 여전히
-- │ SUB_GROUP_NM='순회점검' / SUB_GROUP_IDX=1 / MENU_IDX=3,4). 시드는 커밋 파일이 아니라
-- │ '적용될 실제 DB' 의 현재 구조에 맞춰야 메뉴가 정상 렌더된다 → '순회점검'/MENU_IDX=5 채택.
-- │ ※ 만약 이 시드 적용 전에 LNB 재편(prafta-lnb-1)이 먼저 적용된다면 아래 값을
-- │   '안전 관리'/MENU_IDX=7 로 바꿔 적용할 것(적용 직전 위 SELECT 로 최종 확인).
-- └───────────────────────────────────────────────────────────────────────────────────
--
-- 멱등: 기존 ChkLst_05 시드 제거 후 재삽입.
--
-- [MySQL Workbench safe update mode — 필수]
--   아래 DELETE 는 WHERE 가 MENU_D_ID(비-KEY 컬럼)라, Workbench 기본 설정(SQL_SAFE_UPDATES=1)은
--   "키 없는 삭제"로 보고 Error 1175 로 거부한다. 세션 한정으로 해제하고 파일 말미에서 되돌린다.
--   (조건을 우회하는 게 아니라 안전장치를 명시적으로 잠시 내리는 것 — 재실행 멱등 목적의 의도된 삭제다.)
-- ============================================================

SET SQL_SAFE_UPDATES = 0;

-- (멱등) 기존 ChkLst_05 시드 제거
DELETE FROM `tb_syst_auth_menu` WHERE `MENU_D_ID` = 'ChkLst_05';
DELETE FROM `tb_syst_menu_d`    WHERE `MENU_D_ID` = 'ChkLst_05';

-- (1) 소메뉴 등록 — 순회점검 그룹, 점검 불량 관리(ChkLst_04, MENU_IDX=4) 다음(=5). [라이브 DB 실측]
INSERT INTO `tb_syst_menu_d`
    (`MENU_D_ID`, `MENU_M_ID`, `MENU_NM`, `SUB_GROUP_NM`, `SUB_GROUP_IDX`, `MENU_VIEW`, `MENU_IDX`, `USE_YN`, `INSERT_NO`, `INSERT_DATE`) VALUES
    ('ChkLst_05', 'safety', '순회점검 이력 조회', '순회점검', 1, 'chkLst/ChkLst_05.vue', 5, 'Y', 'SYSTEM', NOW());

-- (2) 권한 매핑 — ChkLst_04 권한 세트를 그대로 복제(회사별 행 포함).
--     읽기전용 감사화면이므로 조회(BTN_SRCH)만 원본 승계, NEW/DELT/SAVE/EXCL 은 강제 'N'.
--     USE_YN 은 ChkLst_04 를 따른다(무권한 역할에 신규 노출 없음).
INSERT INTO `tb_syst_auth_menu`
    (`CMPNY_CD`, `AUTH_CD`, `MENU_D_ID`, `USE_YN`, `BTN_SRCH`, `BTN_NEW`, `BTN_DELT`, `BTN_SAVE`, `BTN_EXCL`, `INSERT_NO`, `INSERT_DATE`)
  SELECT `CMPNY_CD`, `AUTH_CD`, 'ChkLst_05', `USE_YN`, `BTN_SRCH`, 'N', 'N', 'N', 'N', 'SYSTEM', NOW()
    FROM `tb_syst_auth_menu`
   WHERE `MENU_D_ID` = 'ChkLst_04';

-- 안전장치 원복(세션 한정 해제였으므로 재연결 시 기본값으로 돌아오나 명시적으로 되돌린다).
SET SQL_SAFE_UPDATES = 1;

-- 검증:
--   SELECT * FROM tb_syst_menu_d WHERE MENU_D_ID='ChkLst_05';
--   SELECT COUNT(1) FROM tb_syst_auth_menu WHERE MENU_D_ID='ChkLst_05';  -- ChkLst_04 권한행 수와 동일해야 함
-- 롤백:
--   DELETE FROM tb_syst_auth_menu WHERE MENU_D_ID='ChkLst_05';
--   DELETE FROM tb_syst_menu_d    WHERE MENU_D_ID='ChkLst_05';
