-- ============================================================================
-- PRAFTA-SUBCON-T2-3 — 사업장 연동 관리 화면(Subcon_02) 메뉴/권한 등록
-- 작성일: 2026-07-13
-- 적용 환경: MySQL 8.0.42
-- 출처: PRAFTA-SUBCON-T2.plan.md §2(T2-01)·§7(UI-T2-01),
--       prafta-subcon-t1-3-menu-auth.sql (시드 스타일 미러 — 패턴 승계)
--
-- 변경 요약
--   1) tb_syst_menu_d : 신규 소메뉴 Subcon_02('사업장 연동 관리') 등록.
--      대메뉴 = system(시스템 관리), MENU_VIEW='subcon/Subcon_02.vue',
--      SUB_GROUP_NM=NULL(빌더가 대분류명으로 대체), SUB_GROUP_IDX=1, MENU_IDX=10.
--      ★ MENU_IDX=10 근거(2026-07-13 메인 세션 실측): system 소메뉴 현행 최대 = Subcon_01(9).
--   2) tb_syst_auth_menu : 전 활성 회사 × (master, system) 권한 시드 (t1-3 패턴 승계).
--      버튼-액션 매핑(서버 게이트 기준): 목록/후보 조회=BTN_SRCH, 연동 제안=BTN_NEW,
--      수락/거부/취소=BTN_SAVE, 해지=BTN_DELT → SRCH/NEW/SAVE/DELT='Y', EXCL='N'.
--      의사회사 prafta_system_admin 제외.
--
-- 신규 회사 프로비저닝(CompanyProvisionServiceImpl) 정합 — T1 확인 결과 승계:
--   copyAuthMenuFromTemplate 가 템플릿 '001' 의 AUTH_CD IN ('master','hr','safe','99999')
--   행을 신규 회사로 복제(BTN_* 미복제 → 테이블 DEFAULT 'Y' 적용).
--   → 본 시드가 '001'/master 행을 넣으므로 신규 회사는 master 권한 자동 획득(코드 수정 불요).
--
-- viewResolver 는 import.meta.glob("/src/views/**/*.vue") 로 subcon/Subcon_02.vue 를 자동
-- 로드하므로 라우터 코드 수정 불요(메뉴/권한 시드만으로 LNB 노출·라우팅·버튼 권한 완성).
--
-- 적용 전 부재/현황 확인(운영 적용 직전 권장):
--   SELECT * FROM tb_syst_menu_d    WHERE MENU_D_ID = 'Subcon_02';   -- 0건이어야 함
--   SELECT * FROM tb_syst_auth_menu WHERE MENU_D_ID = 'Subcon_02';   -- 0건이어야 함
--   SELECT MENU_D_ID, SUB_GROUP_IDX, MENU_IDX FROM tb_syst_menu_d
--    WHERE MENU_M_ID = 'system' ORDER BY SUB_GROUP_IDX, MENU_IDX;    -- 말미 = Subcon_01(9) 확인
--
-- 멱등성: INSERT 재실행 시 PK 충돌 에러 → 이미 반영된 환경에서는 건너뛸 것.
-- 운영 적용: 사용자 수동(read-only MCP). 본 파일은 작성만, DB 직접 적용 금지.
-- ============================================================================

-- ----------------------------------------------------------------------------
-- (1) 메뉴 등록 (system 대메뉴 하위, SUB_GROUP_NM=NULL, SUB_GROUP_IDX=1, MENU_IDX=10)
-- ----------------------------------------------------------------------------
INSERT INTO tb_syst_menu_d
    (MENU_D_ID, MENU_M_ID, MENU_VIEW, MENU_NM, SUB_GROUP_NM, SUB_GROUP_IDX, MENU_IDX, USE_YN, INSERT_NO, INSERT_DATE) VALUES
    ('Subcon_02', 'system', 'subcon/Subcon_02.vue', '사업장 연동 관리', NULL, 1, 10, 'Y', 'SYSTEM', NOW());

-- ----------------------------------------------------------------------------
-- (2) 권한 시드 — 전 활성 회사 × (master, system). t1-3 패턴 승계(좁게 시작 —
--     TB_SYST_AUTH_MENU 기반이므로 추후 User_02 화면에서 확대 가능).
--     SRCH(목록/후보 조회)/NEW(연동 제안)/SAVE(수락/거부/취소)/DELT(해지)='Y', EXCL='N'.
-- ----------------------------------------------------------------------------
INSERT INTO tb_syst_auth_menu
    (CMPNY_CD, AUTH_CD, MENU_D_ID, USE_YN, BTN_SRCH, BTN_NEW, BTN_DELT, BTN_SAVE, BTN_EXCL, INSERT_NO, INSERT_DATE)
SELECT
    C.CMPNY_CD
    , A.AUTH_CD
    , 'Subcon_02'
    , 'Y'
    , 'Y'   -- BTN_SRCH: 링크 목록/제안 후보 조회
    , 'Y'   -- BTN_NEW : 연동 제안 생성
    , 'Y'   -- BTN_DELT: 해지(독립화)
    , 'Y'   -- BTN_SAVE: 수락/거부/취소
    , 'N'   -- BTN_EXCL: 엑셀 없음
    , 'SYSTEM'
    , NOW()
FROM tb_cmpny C
    CROSS JOIN (
        SELECT 'master' AS AUTH_CD
        UNION ALL
        SELECT 'system' AS AUTH_CD
    ) A
WHERE IFNULL(C.USE_YN, 'N') = 'Y'
  AND C.CMPNY_CD != 'prafta_system_admin';  -- 플랫폼 운영 의사회사 제외(사업장 연동 관리 메뉴 불필요)

-- ============================================================================
-- 롤백 (필요 시 수동 실행)
-- ----------------------------------------------------------------------------
-- DELETE FROM tb_syst_auth_menu WHERE MENU_D_ID = 'Subcon_02';
-- DELETE FROM tb_syst_menu_d    WHERE MENU_D_ID = 'Subcon_02';
-- ============================================================================
