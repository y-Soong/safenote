-- =====================================================================================
-- 위치정보 동의철회·중지 도입 S5 — Location_01(위치정보 동의 현황) 메뉴/권한 등록
--
-- 근거 : .claude/refs/위치정보_동의철회_중지_작업지시서.md §5-1
-- 참조 : prafta-attd15-weekly52h-menu-auth.sql (등록 패턴 미러)
-- 작성 : 2026-09-02
--
-- ★★★적용 현황 (2026-09-02 18:50 실측)
--   개발 DB : ❌ 미적용
--   운영 DB : ❌ 미적용
--   → 양쪽 모두 실행 대상이다.
--
-- ★사전 확인 결과(2026-09-02): '근태 관리' 중분류 MENU_IDX=8 은 양쪽 다 비어 있다(충돌 없음).
--   001 외 Attd_15 가 시드된 고객사는 운영 기준 1개사 → §3-2 가 그 1개사에 master/hr/system 을 넣는다.
--
-- ★멱등하지 않다 — 중복 실행 시 PK 충돌. 이미 반영된 환경에서는 §1 확인 후 건너뛴다.
--   (§3-2 만은 NOT EXISTS 가드가 있어 재실행해도 안전하다)
--
-- 변경 요약
--   1) tb_syst_menu_d : 소메뉴 Location_01('위치정보 동의 현황') 등록.
--      대메뉴 = attdHr(근태/인사), 중분류 = '근태 관리'(SUB_GROUP_IDX=3), MENU_IDX=8
--      (2026-09-02 운영 실측: Attd_05=1 ~ Attd_15=7 사용 중).
--      화면 = location/Location_01.vue.
--   2) tb_syst_auth_menu : ★master / hr / system 에만 읽기 전용 부여.
--
-- ★★권한을 근태 화면들보다 좁게 준 이유
--   이 화면은 "누가 위치정보 동의를 철회했는가" 라는 인사·개인정보 성격의 정보를 다룬다.
--   Attd_12/15 등 근태 모니터링 화면은 safe(안전관리자)와 노드 관리자 직책(00001/00004/00006/
--   00008)·99999 까지 열려 있으나, 본 화면은 그 폭이 과하다.
--   서버(Location01ServiceImpl)도 AuthRoleUtils.isManager(master/hr) 만 사업장 전체를 허용하고
--   그 외는 canManageNode 로 검증한 본인 부서로 제한하므로, 메뉴 시드와 서버 게이트가 일치한다.
--   ※부서 관리자에게 열어야 한다는 판단이 서면 그때 AUTH_CD 를 추가하면 된다(서버는 이미 지원).
-- =====================================================================================


-- =====================================================================================
-- §1. 사전 확인 — 결과가 있으면 이미 적용된 것이므로 §2·§3 을 건너뛴다.
-- =====================================================================================

SELECT * FROM tb_syst_menu_d   WHERE MENU_D_ID = 'Location_01';
SELECT * FROM tb_syst_auth_menu WHERE MENU_D_ID = 'Location_01';

-- MENU_IDX 충돌 확인 — '근태 관리' 중분류의 사용 중 IDX 확인(8 이 비어 있어야 한다).
SELECT MENU_D_ID, MENU_NM, SUB_GROUP_NM, MENU_IDX
  FROM tb_syst_menu_d
 WHERE MENU_M_ID = 'attdHr' AND SUB_GROUP_NM = '근태 관리'
 ORDER BY MENU_IDX;


-- =====================================================================================
-- §2. 메뉴 등록
-- =====================================================================================

INSERT INTO tb_syst_menu_d
    (MENU_D_ID, MENU_M_ID, MENU_VIEW, MENU_NM, SUB_GROUP_NM, SUB_GROUP_IDX, MENU_IDX, USE_YN, INSERT_NO, INSERT_DATE)
VALUES
    ('Location_01', 'attdHr', 'location/Location_01.vue', '위치정보 동의 현황', '근태 관리', 3, 8, 'Y', 'SYSTEM', NOW());


-- =====================================================================================
-- §3. 권한 시드 (읽기 전용)
--
--   BTN_SRCH='Y', 나머지 'N' — 조회 전용 화면이다.
--   ★관리자가 타인의 동의 상태를 바꾸는 기능은 서버에도 없다(철회는 되돌릴 수 없는 파기를
--     동반하므로 본인만 수행). 저장/삭제 버튼 권한을 열어도 동작할 EP 가 없다.
-- =====================================================================================

-- (3-1) 기준 회사(001)
INSERT INTO tb_syst_auth_menu
    (CMPNY_CD, AUTH_CD, MENU_D_ID, USE_YN, BTN_SRCH, BTN_NEW, BTN_DELT, BTN_SAVE, BTN_EXCL, INSERT_NO, INSERT_DATE)
VALUES
    ('001', 'master', 'Location_01', 'Y', 'Y', 'N', 'N', 'N', 'N', 'SYSTEM', NOW())
  , ('001', 'hr',     'Location_01', 'Y', 'Y', 'N', 'N', 'N', 'N', 'SYSTEM', NOW())
  , ('001', 'system', 'Location_01', 'Y', 'Y', 'N', 'N', 'N', 'N', 'SYSTEM', NOW());

-- (3-2) 001 외 기존 고객사 — Attd_15 가 시드된 회사들에 동일 폭(master/hr/system)으로 부여.
--   ★Attd_15 의 회사 목록을 그대로 쓰되 권한은 좁힌다(safe/노드관리자 직책 제외).
INSERT INTO tb_syst_auth_menu
    (CMPNY_CD, AUTH_CD, MENU_D_ID, USE_YN, BTN_SRCH, BTN_NEW, BTN_DELT, BTN_SAVE, BTN_EXCL, INSERT_NO, INSERT_DATE)
SELECT DISTINCT A.CMPNY_CD
     , R.AUTH_CD
     , 'Location_01'
     , 'Y', 'Y', 'N', 'N', 'N', 'N'
     , 'SYSTEM', NOW()
  FROM tb_syst_auth_menu A
       CROSS JOIN (SELECT 'master' AS AUTH_CD UNION ALL SELECT 'hr' UNION ALL SELECT 'system') R
 WHERE A.MENU_D_ID = 'Attd_15'
   AND A.CMPNY_CD <> '001'
   AND NOT EXISTS (
        SELECT 1 FROM tb_syst_auth_menu B
         WHERE B.CMPNY_CD = A.CMPNY_CD AND B.AUTH_CD = R.AUTH_CD AND B.MENU_D_ID = 'Location_01'
       );


-- =====================================================================================
-- §4. 사후 검증
-- =====================================================================================

-- (4-1) 메뉴 1행
SELECT MENU_D_ID, MENU_NM, MENU_VIEW, MENU_M_ID, SUB_GROUP_NM, MENU_IDX, USE_YN
  FROM tb_syst_menu_d WHERE MENU_D_ID = 'Location_01';

-- (4-2) 권한 — 회사별로 master/hr/system 만 있어야 한다.
SELECT CMPNY_CD, GROUP_CONCAT(AUTH_CD ORDER BY AUTH_CD) AS AUTHS, COUNT(*) AS CNT
  FROM tb_syst_auth_menu WHERE MENU_D_ID = 'Location_01'
 GROUP BY CMPNY_CD ORDER BY CMPNY_CD;

-- (4-3) ★쓰기 버튼이 열린 행이 없어야 한다(결과 0).
SELECT COUNT(*) AS WRITE_BTN_OPEN
  FROM tb_syst_auth_menu
 WHERE MENU_D_ID = 'Location_01'
   AND (BTN_NEW = 'Y' OR BTN_DELT = 'Y' OR BTN_SAVE = 'Y');


-- =====================================================================================
-- §5. 롤백
--
--   DELETE FROM tb_syst_auth_menu WHERE MENU_D_ID = 'Location_01';
--   DELETE FROM tb_syst_menu_d    WHERE MENU_D_ID = 'Location_01';
--
--   ★master 는 MenuLockPolicy 상 모든 BTN_* 가 강제 'Y' 로 보정될 수 있으나,
--     서버에 쓰기 EP 자체가 없으므로 실질 위험은 없다.
-- =====================================================================================
