-- ============================================================================
-- PRAFTA-SUBCON-T3-3 — 데이터 공유 화면(Subcon_03 / Subcon_04) 메뉴/권한 등록
-- 작성일: 2026-07-13
-- 적용 환경: MySQL 8.0.42
-- 출처: PRAFTA-SUBCON-T3.plan.md §3-3·§7(UI-T3-01/02),
--       prafta-subcon-t2-3-menu-auth.sql (시드 스타일 미러 — 패턴 승계)
--
-- 변경 요약
--   1) tb_syst_menu_d : 신규 소메뉴 2건 등록(대메뉴 = system, SUB_GROUP_NM=NULL, SUB_GROUP_IDX=1).
--      - Subcon_03 '데이터 공유 요청함'  MENU_VIEW='subcon/Subcon_03.vue', MENU_IDX=11
--      - Subcon_04 '공유받은 자료'       MENU_VIEW='subcon/Subcon_04.vue', MENU_IDX=12
--      ★ MENU_IDX 근거(2026-07-13 메인 세션 실측): system 소메뉴 현행 최대 = Subcon_02(10).
--   2) tb_syst_auth_menu : 전 활성 회사 × (master, system) 권한 시드(t1-3/t2-3 패턴 승계).
--      - Subcon_03 : SRCH='Y'(목록/후보/승인정보 조회), NEW='Y'(요청 생성),
--                    SAVE='Y'(승인/거부/취소), DELT='N'(삭제 없음 — 스냅샷 불변), EXCL='N'.
--      - Subcon_04 : SRCH='Y'(수신 스냅샷 목록/상세), 나머지 'N'(읽기 전용 — 엑셀 반출 미포함).
--      의사회사 prafta_system_admin 제외.
--
-- 신규 회사 프로비저닝(CompanyProvisionServiceImpl) 정합 — T1/T2 확인 결과 승계:
--   copyAuthMenuFromTemplate 가 템플릿 '001' 의 AUTH_CD IN ('master','hr','safe','99999') 행을
--   신규 회사로 복제 → 본 시드가 '001'/master 행을 넣으므로 신규 회사는 master 권한 자동 획득.
--
-- viewResolver 가 import.meta.glob("/src/views/**/*.vue") 로 subcon/Subcon_03.vue·Subcon_04.vue 를
-- 자동 로드하므로 라우터 코드 수정 불요(메뉴/권한 시드만으로 LNB 노출·라우팅·버튼 권한 완성).
--
-- 적용 전 부재/현황 확인(운영 적용 직전 권장):
--   SELECT * FROM tb_syst_menu_d    WHERE MENU_D_ID IN ('Subcon_03','Subcon_04');  -- 0건이어야 함
--   SELECT * FROM tb_syst_auth_menu WHERE MENU_D_ID IN ('Subcon_03','Subcon_04');  -- 0건이어야 함
--   SELECT MENU_D_ID, SUB_GROUP_IDX, MENU_IDX FROM tb_syst_menu_d
--    WHERE MENU_M_ID = 'system' ORDER BY SUB_GROUP_IDX, MENU_IDX;                  -- 말미 = Subcon_02(10)
--
-- 멱등성: INSERT 재실행 시 PK 충돌 에러 → 이미 반영된 환경에서는 건너뛸 것.
-- 운영 적용: 사용자 수동(read-only MCP). 본 파일은 작성만, DB 직접 적용 금지.
-- ============================================================================

-- ----------------------------------------------------------------------------
-- (1) 메뉴 등록
-- ----------------------------------------------------------------------------
INSERT INTO tb_syst_menu_d
    (MENU_D_ID, MENU_M_ID, MENU_VIEW, MENU_NM, SUB_GROUP_NM, SUB_GROUP_IDX, MENU_IDX, USE_YN, INSERT_NO, INSERT_DATE) VALUES
    ('Subcon_03', 'system', 'subcon/Subcon_03.vue', '데이터 공유 요청함', NULL, 1, 11, 'Y', 'SYSTEM', NOW())
  , ('Subcon_04', 'system', 'subcon/Subcon_04.vue', '공유받은 자료',      NULL, 1, 12, 'Y', 'SYSTEM', NOW());

-- ----------------------------------------------------------------------------
-- (2) 권한 시드 — Subcon_03 (전 활성 회사 × master/system)
--     SRCH(목록/후보/승인정보 조회) / NEW(요청 생성) / SAVE(승인·거부·취소) = 'Y'
--     DELT(삭제) / EXCL(엑셀) = 'N'  ← 스냅샷 불변 + PII 2차 반출면 미개방
-- ----------------------------------------------------------------------------
INSERT INTO tb_syst_auth_menu
    (CMPNY_CD, AUTH_CD, MENU_D_ID, USE_YN, BTN_SRCH, BTN_NEW, BTN_DELT, BTN_SAVE, BTN_EXCL, INSERT_NO, INSERT_DATE)
SELECT
    C.CMPNY_CD
    , A.AUTH_CD
    , 'Subcon_03'
    , 'Y'
    , 'Y'   -- BTN_SRCH: 요청 목록/후보/승인 사전정보 조회
    , 'Y'   -- BTN_NEW : 공유 요청 생성
    , 'N'   -- BTN_DELT: 삭제 경로 없음(스냅샷 불변)
    , 'Y'   -- BTN_SAVE: 승인/거부/취소
    , 'N'   -- BTN_EXCL: 엑셀 반출 미포함
    , 'SYSTEM'
    , NOW()
FROM tb_cmpny C
    CROSS JOIN (
        SELECT 'master' AS AUTH_CD
        UNION ALL
        SELECT 'system' AS AUTH_CD
    ) A
WHERE IFNULL(C.USE_YN, 'N') = 'Y'
  AND C.CMPNY_CD != 'prafta_system_admin';  -- 플랫폼 운영 의사회사 제외

-- ----------------------------------------------------------------------------
-- (3) 권한 시드 — Subcon_04 (조회 전용)
-- ----------------------------------------------------------------------------
INSERT INTO tb_syst_auth_menu
    (CMPNY_CD, AUTH_CD, MENU_D_ID, USE_YN, BTN_SRCH, BTN_NEW, BTN_DELT, BTN_SAVE, BTN_EXCL, INSERT_NO, INSERT_DATE)
SELECT
    C.CMPNY_CD
    , A.AUTH_CD
    , 'Subcon_04'
    , 'Y'
    , 'Y'   -- BTN_SRCH: 수신 스냅샷 목록/상세 조회
    , 'N'
    , 'N'
    , 'N'
    , 'N'
    , 'SYSTEM'
    , NOW()
FROM tb_cmpny C
    CROSS JOIN (
        SELECT 'master' AS AUTH_CD
        UNION ALL
        SELECT 'system' AS AUTH_CD
    ) A
WHERE IFNULL(C.USE_YN, 'N') = 'Y'
  AND C.CMPNY_CD != 'prafta_system_admin';

-- ============================================================================
-- 롤백 (필요 시 수동 실행)
-- ----------------------------------------------------------------------------
-- DELETE FROM tb_syst_auth_menu WHERE MENU_D_ID IN ('Subcon_03', 'Subcon_04');
-- DELETE FROM tb_syst_menu_d    WHERE MENU_D_ID IN ('Subcon_03', 'Subcon_04');
-- ============================================================================
