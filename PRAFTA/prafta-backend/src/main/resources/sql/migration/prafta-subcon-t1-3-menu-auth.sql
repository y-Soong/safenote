-- ============================================================================
-- PRAFTA-SUBCON-T1-3 — 연동회사 관리 화면(Subcon_01) 메뉴/권한 등록
-- 작성일: 2026-07-13
-- 적용 환경: MySQL 8.0.42
-- 출처: PRAFTA-SUBCON-T1.plan.md §5 + §9-Q2 (권한 = master/system 만 — 2026-07-12 확정),
--       prafta-daily-blacklist-2-menu-auth.sql (시드 스타일 미러)
--
-- 변경 요약
--   1) tb_syst_menu_d : 신규 소메뉴 Subcon_01('연동회사 관리') 등록.
--      대메뉴 = system(시스템 관리), MENU_VIEW='subcon/Subcon_01.vue',
--      SUB_GROUP_NM=NULL(빌더가 대분류명으로 대체), SUB_GROUP_IDX=1, MENU_IDX=9.
--      ★ MENU_IDX=9 근거(2026-07-13 로컬 DB 실측): system 소메뉴 현행
--        User_01~04(1~4), Baim_01(5), Baim_06(6), Baim_02(7), Baim_03(8, USE_YN=N).
--        Notice_01 은 'notice' 대메뉴로 이관되어 system 에 없음(plan §1 의 IDX=10 가정 정정).
--   2) tb_syst_auth_menu : ★전 활성 회사★ 대상 master/system 권한 시드 (Q2 확정 — 좁게 시작).
--      수락은 상대 회사 화면에서 이뤄지므로 단일 회사('001') 시드로는 상대사가 메뉴를 못 본다
--      → INSERT ... SELECT 로 tb_cmpny USE_YN='Y' 전 회사에 시드(템플릿 '001' 포함).
--      버튼-액션 매핑(서버 게이트 기준): 조회/이력=BTN_SRCH, 요청 생성=BTN_NEW,
--      수락/거부/취소=BTN_SAVE, 해지=BTN_DELT → SRCH/NEW/SAVE/DELT='Y', EXCL='N'.
--
-- 신규 회사 프로비저닝(CompanyProvisionServiceImpl) 정합 — 2026-07-13 실측:
--   copyAuthMenuFromTemplate 가 템플릿 '001' 의 AUTH_CD IN ('master','hr','safe','99999')
--   행을 신규 회사로 복제한다(BTN_* 컬럼 미복제 → 테이블 DEFAULT 'Y' 적용).
--   → 본 시드가 '001'/master 행을 넣으므로 신규 회사는 master 권한을 자동 획득(코드 수정 불요).
--   'system' AUTH_CD 는 기존 프로비저닝도 복제하지 않음(전 메뉴 공통 동작) — 동일 유지.
--
-- viewResolver 는 import.meta.glob("/src/views/**/*.vue") 로 subcon/Subcon_01.vue 를 자동
-- 로드하므로 라우터 코드 수정 불요. tb_syst_menu_d + tb_syst_auth_menu 만으로 LNB 노출/
-- 라우팅/버튼 권한(BTN_*)이 완성된다.
--
-- 적용 전 부재/현황 확인(운영 적용 직전 권장):
--   SELECT * FROM tb_syst_menu_d    WHERE MENU_D_ID = 'Subcon_01';   -- 0건이어야 함
--   SELECT * FROM tb_syst_auth_menu WHERE MENU_D_ID = 'Subcon_01';   -- 0건이어야 함
--   SELECT MENU_D_ID, SUB_GROUP_IDX, MENU_IDX FROM tb_syst_menu_d
--    WHERE MENU_M_ID = 'system' ORDER BY SUB_GROUP_IDX, MENU_IDX;    -- 말미 = Baim_03(8) 확인
--
-- 멱등성: (1) INSERT 중복 실행 시 PK 충돌 에러. (2) INSERT...SELECT 는 재실행 시 PK 충돌.
--         이미 반영된 환경에서는 건너뛸 것.
-- 운영 적용: 사용자 수동(read-only MCP). 본 파일은 작성만, DB 직접 적용 금지.
-- ============================================================================

-- ----------------------------------------------------------------------------
-- (1) 메뉴 등록 (system 대메뉴 하위, SUB_GROUP_NM=NULL, SUB_GROUP_IDX=1, MENU_IDX=9)
-- ----------------------------------------------------------------------------
INSERT INTO tb_syst_menu_d
    (MENU_D_ID, MENU_M_ID, MENU_VIEW, MENU_NM, SUB_GROUP_NM, SUB_GROUP_IDX, MENU_IDX, USE_YN, INSERT_NO, INSERT_DATE) VALUES
    ('Subcon_01', 'system', 'subcon/Subcon_01.vue', '연동회사 관리', NULL, 1, 9, 'Y', 'SYSTEM', NOW());

-- ----------------------------------------------------------------------------
-- (2) 권한 시드 — 전 활성 회사 × (master, system). Q2 확정: 좁게 시작
--     (TB_SYST_AUTH_MENU 기반이므로 추후 User_02 화면에서 확대 가능).
--     SRCH(조회/이력)/NEW(요청 생성)/SAVE(수락/거부/취소)/DELT(해지)='Y', EXCL='N'.
-- ----------------------------------------------------------------------------
INSERT INTO tb_syst_auth_menu
    (CMPNY_CD, AUTH_CD, MENU_D_ID, USE_YN, BTN_SRCH, BTN_NEW, BTN_DELT, BTN_SAVE, BTN_EXCL, INSERT_NO, INSERT_DATE)
SELECT
    C.CMPNY_CD
    , A.AUTH_CD
    , 'Subcon_01'
    , 'Y'
    , 'Y'   -- BTN_SRCH: 목록/이력 조회
    , 'Y'   -- BTN_NEW : 연동 요청 생성(회사 정확일치 조회 포함)
    , 'Y'   -- BTN_DELT: 해지
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
  AND C.CMPNY_CD != 'prafta_system_admin';  -- 플랫폼 운영 의사회사 제외(연동회사 관리 메뉴 불필요)

-- ============================================================================
-- 롤백 (필요 시 수동 실행)
-- ----------------------------------------------------------------------------
-- DELETE FROM tb_syst_auth_menu WHERE MENU_D_ID = 'Subcon_01';
-- DELETE FROM tb_syst_menu_d    WHERE MENU_D_ID = 'Subcon_01';
-- ============================================================================
