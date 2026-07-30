-- ============================================================================
-- ATTD16 — 연차 사용 현황 캘린더 화면(Attd_16) 메뉴/권한 등록
-- 작성일: 2026-07-30
-- 적용 환경: MySQL 8.0.42 (개발/운영 동일 구문)
-- 참조: .claude/requests/web_requests/작업지시서_연차사용현황-캘린더-신규화면-Attd_16.md,
--       .claude/requests/web_requests/작업지시서_연차사용현황-캘린더-신규화면-Attd_16.plan.md §2 T2,
--       prafta-attd15-weekly52h-menu-auth.sql (등록 패턴 미러 — 결함 2건 교정 반영),
--       prafta-018-seed-system-leave-cd.sql (INSERT ... SELECT ... WHERE NOT EXISTS 멱등 패턴)
--
-- 변경 요약
--   1) tb_syst_menu_d : 신규 소메뉴 Attd_16('연차 사용 현황') 등록.
--      대메뉴 = attdHr(근태/인사), 중분류 = '연차 관리'(SUB_GROUP_IDX=2), MENU_IDX=5
--      (실측 확정: Attd_09=1, Attd_13=2, Attd_14=3, LeavePromotion_01=4 사용 중).
--      화면 = attd/Attd_16.vue.
--   2) tb_syst_auth_menu : Attd_14(연차 관리 중분류 기존 화면)의 회사×권한 분포를 그대로 미러.
--      Attd_15 시드 결함 2건(메뉴 IDX 추측 / 타사 권한 미러 누락)을 교정한 형태로,
--      권한 목록을 하드코딩하지 않고 Attd_14 분포에서 INSERT ... SELECT 로 파생시킨다
--      (001 + 기존 타사 전체 커버 → 타사 관리자 메뉴 미노출 결함 원천 차단).
--      본 화면은 조회 전용 → BTN_SRCH='Y', BTN_NEW/BTN_DELT/BTN_SAVE/BTN_EXCL='N', USE_YN='Y'.
--
--      2026-07-30 실측(개발 사본) Attd_14 분포: 001 = 9종(00001, 00004, 00006, 00008,
--      system, master, 99999, hr, safe) / 타사 9개사 = 각 hr, master, safe.
--      ※ MCP prafta-mysql 은 개발 사본이므로(메모리 feedback_mcp_mysql_is_dev_copy_not_prod)
--        운영 분포는 아래 확인 쿼리로 적용 직전 사용자가 직접 확인한다.
--
--   라우터 등록 불필요: 웹 FE 라우팅은 viewResolver.js 의 import.meta.glob 동적 해석 +
--   MENU_VIEW 기반이다. Attd_16.vue 파일 존재 + 본 시드만으로 LNB 노출/진입이 된다.
--   실제 데이터 접근 인가는 서버 SiteAccessService.assertSiteAccess 가 최종 강제한다(ATTD16-T1).
--
-- 적용 원칙 (메모리 feedback_db_migration_apply_both_envs):
--   개발 DB 와 운영 DB 에 반드시 동일하게 적용한다. 한쪽만 적용하면 환경 불일치 장애가 재발한다.
--   실행은 사용자 수동(운영은 SSH 경유 mysql 또는 Workbench SSH 터널 — 배포 매뉴얼 SQL 절차 준수).
--
-- 멱등성:
--   두 INSERT 모두 WHERE NOT EXISTS 가드가 있어 재실행해도 중복 행이 생기지 않는다(재실행 안전).
--
-- 적용 전/후 확인 쿼리:
--   -- (적용 전) 부재 확인
--   SELECT MENU_D_ID, MENU_NM, SUB_GROUP_NM, MENU_IDX FROM tb_syst_menu_d WHERE MENU_D_ID = 'Attd_16';
--   SELECT CMPNY_CD, AUTH_CD FROM tb_syst_auth_menu WHERE MENU_D_ID = 'Attd_16';
--   -- (적용 전) MENU_IDX 충돌 확인 — '연차 관리' 중분류 현황
--   SELECT MENU_D_ID, MENU_NM, SUB_GROUP_IDX, MENU_IDX FROM tb_syst_menu_d
--    WHERE MENU_M_ID = 'attdHr' AND SUB_GROUP_NM = '연차 관리' ORDER BY MENU_IDX;
--   -- (적용 전) 미러 소스 분포 확인 — Attd_14 회사x권한
--   SELECT CMPNY_CD, AUTH_CD, USE_YN FROM tb_syst_auth_menu WHERE MENU_D_ID = 'Attd_14'
--    ORDER BY CMPNY_CD, AUTH_CD;
--   -- (적용 후) 미러 결과 대조 — 두 카운트가 같아야 한다
--   SELECT
--       (SELECT COUNT(1) FROM tb_syst_auth_menu WHERE MENU_D_ID = 'Attd_14') AS attd14_cnt
--     , (SELECT COUNT(1) FROM tb_syst_auth_menu WHERE MENU_D_ID = 'Attd_16') AS attd16_cnt;
-- ============================================================================

START TRANSACTION;

-- ----------------------------------------------------------------------------
-- 1) 메뉴 등록 (근태/인사(attdHr) > '연차 관리'(SUB_GROUP_IDX=2) 중분류, 말미 MENU_IDX=5)
-- ----------------------------------------------------------------------------
INSERT INTO tb_syst_menu_d (
      MENU_D_ID
    , MENU_M_ID
    , MENU_VIEW
    , MENU_NM
    , SUB_GROUP_NM
    , SUB_GROUP_IDX
    , MENU_IDX
    , USE_YN
    , INSERT_NO
    , INSERT_DATE
)
SELECT
      'Attd_16'
    , 'attdHr'
    , 'attd/Attd_16.vue'
    , '연차 사용 현황'
    , '연차 관리'
    , 2
    , 5
    , 'Y'
    , 'SYSTEM'
    , NOW()
  FROM DUAL
 WHERE NOT EXISTS (
       SELECT 1
         FROM tb_syst_menu_d M
        WHERE M.MENU_D_ID = 'Attd_16'
          AND M.MENU_M_ID = 'attdHr'
 );

-- ----------------------------------------------------------------------------
-- 2) 권한 시드 (조회 전용) — Attd_14 의 회사x권한 분포를 001 포함 전 회사에 미러.
--    신규 고객사는 프로비저닝(CompanyProvisionMapper: 템플릿 회사 tb_syst_auth_menu 복제)이
--    자동 커버하므로 본 구문은 "이미 존재하는" 회사만 대상이다.
-- ----------------------------------------------------------------------------
INSERT INTO tb_syst_auth_menu (
      CMPNY_CD
    , AUTH_CD
    , MENU_D_ID
    , USE_YN
    , BTN_SRCH
    , BTN_NEW
    , BTN_DELT
    , BTN_SAVE
    , BTN_EXCL
    , INSERT_NO
    , INSERT_DATE
)
SELECT
      SRC.CMPNY_CD
    , SRC.AUTH_CD
    , 'Attd_16'
    /* qa 결함 D3: USE_YN 은 하드코딩하지 않고 원본(Attd_14) 값을 승계한다.
       Attd_14 를 비활성('N')으로 둔 회사가 있어도 Attd_16 만 조용히 활성화되지 않게 한다.
       (현재 데이터는 36건 전부 'Y' — 결과 동일하나 회귀 안전장치로 승계 형태를 유지) */
    , SRC.USE_YN
    , 'Y'
    , 'N'
    , 'N'
    , 'N'
    , 'N'
    , 'SYSTEM'
    , NOW()
  FROM (
       SELECT
             AM.CMPNY_CD
           , AM.AUTH_CD
           , AM.USE_YN
         FROM tb_syst_auth_menu AM
        WHERE AM.MENU_D_ID = 'Attd_14'
  ) SRC
 WHERE NOT EXISTS (
       SELECT 1
         FROM tb_syst_auth_menu X
        WHERE X.CMPNY_CD  = SRC.CMPNY_CD
          AND X.AUTH_CD   = SRC.AUTH_CD
          AND X.MENU_D_ID = 'Attd_16'
 );

COMMIT;

-- ============================================================================
-- 롤백 (필요 시 수동 실행)
-- ----------------------------------------------------------------------------
-- START TRANSACTION;
-- DELETE FROM tb_syst_auth_menu WHERE MENU_D_ID = 'Attd_16';
-- DELETE FROM tb_syst_menu_d    WHERE MENU_D_ID = 'Attd_16';
-- COMMIT;
-- ============================================================================
