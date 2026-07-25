-- ============================================================================
-- ATTD15 — 주52시간 관리 화면(Attd_15) 메뉴/권한 등록
-- 작성일: 2026-07-25
-- 적용 환경: MySQL 8.0.42
-- 참조: .claude/requests/web_requests/작업지시서_근태관리-주52시간관리-신규화면.md,
--       .claude/requests/web_requests/작업지시서_근태관리-주52시간관리-신규화면.plan.md,
--       prafta-com-016-H-attd14-menu-auth.sql (등록 패턴 미러),
--       prafta-com-003-device-fraud-detect.sql §3 (Attd_12 메뉴/권한 시드 미러 — 근태 관리 중분류)
--
-- 변경 요약
--   1) tb_syst_menu_d : 신규 소메뉴 Attd_15('주52시간 관리') 등록.
--      대메뉴 = attdHr(근태/인사), 중분류 = '근태 관리'(SUB_GROUP_IDX=3 — 2026-07-25 운영 실측,
--      1은 '근태 설정'). MENU_IDX=7 (실측: Attd_05=1 ~ Attd_12=6 사용 중, 5는 Attd_11 과 충돌).
--      화면=attd/Attd_15.vue.
--   2) tb_syst_auth_menu : master/hr/safe/노드 정·부 관리자(+system)에 읽기 전용 부여.
--      Attd_12(부정 출퇴근 의심 모니터링, 동일 중분류) 시드 세트와 동일 폭 — 근로기준법 준수
--      모니터링 화면 성격상 안전관리자(safe)도 조회 대상에 포함한다(Attd_14 와 달리 연차 전용
--      화면이 아니므로 safe 제외 사유 없음).
--      본 화면은 엑셀 export 미제공(Attd_15.vue 골격에서 버튼 비활성) → BTN_EXCL='N'.
--
-- viewResolver 는 컴포넌트명(Attd_15)으로 자동 로드하나, tb_syst_menu_d + tb_syst_auth_menu
-- 가 있어야 LNB 노출/진입 가능(prafta-019-F / prafta-com-003 패턴).
-- 실제 데이터 접근 인가는 서버 SiteAccessService.assertSiteAccess + AttdCloseService.canManageNode
-- 가 강제한다(ATTD15-T1).
--
-- 적용 전 부재/현황 확인 (운영 적용 직전 권장):
--   SELECT * FROM tb_syst_menu_d  WHERE MENU_D_ID = 'Attd_15';
--   SELECT * FROM tb_syst_auth_menu WHERE MENU_D_ID = 'Attd_15';
--   SELECT MENU_D_ID, SUB_GROUP_NM, MENU_IDX FROM tb_syst_menu_d
--    WHERE MENU_M_ID = 'attdHr' AND SUB_GROUP_NM = '근태 관리' ORDER BY MENU_IDX;  -- IDX 충돌 확인
--
-- 멱등성: INSERT 중복 실행 시 PK 충돌 에러. 이미 반영된 환경에서는 건너뛸 것.
-- 운영 적용: 2026-07-25 배포 시 SSH 경유(mysql) 실행 예정 — 배포 매뉴얼 SQL 반영 절차 준수.
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1) 메뉴 등록 (근태/인사 탭(attdHr) 하위 '근태 관리' 중분류, 말미 IDX=7)
-- ----------------------------------------------------------------------------
INSERT INTO tb_syst_menu_d (MENU_D_ID, MENU_M_ID, MENU_VIEW, MENU_NM, SUB_GROUP_NM, SUB_GROUP_IDX, MENU_IDX, USE_YN, INSERT_NO, INSERT_DATE) VALUES
    ('Attd_15', 'attdHr', 'attd/Attd_15.vue', '주52시간 관리', '근태 관리', 3, 7, 'Y', 'SYSTEM', NOW());

-- ----------------------------------------------------------------------------
-- 2) 권한 시드 (읽기 전용). Attd_12 세트와 동일 폭(safe 포함).
--    부여 대상: 00001/00004/00006/00008(노드 정·부 관리권 직책), 99999, hr, master, safe, system.
--    읽기 전용 → BTN_SRCH='Y', BTN_NEW/BTN_DELT/BTN_SAVE='N'. 엑셀 미제공 → BTN_EXCL='N'.
-- ----------------------------------------------------------------------------
INSERT INTO tb_syst_auth_menu (CMPNY_CD, AUTH_CD, MENU_D_ID, USE_YN, BTN_SRCH, BTN_NEW, BTN_DELT, BTN_SAVE, BTN_EXCL, INSERT_NO, INSERT_DATE) VALUES
    ('001', '00001',  'Attd_15', 'Y', 'Y', 'N', 'N', 'N', 'N', 'SYSTEM', NOW())
  , ('001', '00004',  'Attd_15', 'Y', 'Y', 'N', 'N', 'N', 'N', 'SYSTEM', NOW())
  , ('001', '00006',  'Attd_15', 'Y', 'Y', 'N', 'N', 'N', 'N', 'SYSTEM', NOW())
  , ('001', '00008',  'Attd_15', 'Y', 'Y', 'N', 'N', 'N', 'N', 'SYSTEM', NOW())
  , ('001', '99999',  'Attd_15', 'Y', 'Y', 'N', 'N', 'N', 'N', 'SYSTEM', NOW())
  , ('001', 'hr',     'Attd_15', 'Y', 'Y', 'N', 'N', 'N', 'N', 'SYSTEM', NOW())
  , ('001', 'master', 'Attd_15', 'Y', 'Y', 'N', 'N', 'N', 'N', 'SYSTEM', NOW())
  , ('001', 'safe',   'Attd_15', 'Y', 'Y', 'N', 'N', 'N', 'N', 'SYSTEM', NOW())
  , ('001', 'system', 'Attd_15', 'Y', 'Y', 'N', 'N', 'N', 'N', 'SYSTEM', NOW());

-- ----------------------------------------------------------------------------
-- 3) 001 외 기존 고객사 권한 시드 — Attd_12 의 회사×권한 분포를 그대로 미러(읽기 전용 강제).
--    2026-07-25 운영 실측: Attd_12/14 는 001 외 9개 고객사에 hr/master/safe 로 시드돼 있으나
--    본 시드 원안은 001 만 커버 → 타사 관리자에게 메뉴 미노출 결함이라 미러 INSERT 추가.
--    신규 고객사는 프로비저닝(CompanyProvisionMapper: 템플릿 회사 tb_syst_auth_menu 복제)이
--    자동 커버하므로 본 구문은 "이미 존재하는" 회사만 대상.
-- ----------------------------------------------------------------------------
INSERT INTO tb_syst_auth_menu (CMPNY_CD, AUTH_CD, MENU_D_ID, USE_YN, BTN_SRCH, BTN_NEW, BTN_DELT, BTN_SAVE, BTN_EXCL, INSERT_NO, INSERT_DATE)
SELECT
      AM.CMPNY_CD
    , AM.AUTH_CD
    , 'Attd_15'
    , AM.USE_YN
    , 'Y'
    , 'N'
    , 'N'
    , 'N'
    , 'N'
    , 'SYSTEM'
    , NOW()
FROM tb_syst_auth_menu AM
WHERE AM.MENU_D_ID = 'Attd_12'
  AND AM.CMPNY_CD <> '001';

-- ============================================================================
-- 롤백 (필요 시 수동 실행)
-- ----------------------------------------------------------------------------
-- DELETE FROM tb_syst_auth_menu WHERE MENU_D_ID = 'Attd_15';
-- DELETE FROM tb_syst_menu_d    WHERE MENU_D_ID = 'Attd_15';
-- ============================================================================
