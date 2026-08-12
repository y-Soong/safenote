-- ============================================================================
-- SOJEONG-3-1 — User_09(셀프가입 승인) / User_10(소정근로시간 관리) 메뉴·권한 시드
--                + SYS060 감사 액션 '03 상태 변경' 코드 시드
-- 작성일: 2026-08-12
-- 적용 환경: MySQL 8.0.42 이상 (★개발·운영 동시 적용 — feedback_db_migration_apply_both_envs)
-- 참조: .claude/requests/web_requests/작업지시서_근로자별-소정근로시간-관리-도입.md §0단계,
--       동 plan.md §3 UI-B/UI-C · §4 소정-04/09/10,
--       prafta-attd16-leave-usage-menu-auth.sql (INSERT ... SELECT 멱등 미러 패턴 — 본 파일의 원형)
--
-- ★본 파일에는 스키마 변경(DDL)이 없다. 테이블/컬럼 추가 없이 코드·메뉴 시드(DML)만 넣는다.
--   유일한 ALTER 는 tb_audit_log.ACTION_TYPE 의 COMMENT 정렬(코드값 나열 규칙)로, 구조 변경이
--   아니다(feedback_db_comment_code_convention). 부담되면 (3) 만 건너뛰어도 기능에 영향 없다.
--
-- 변경 요약
--   1) tb_syst_menu_d    : User_09 / User_10 소메뉴 등록. 대메뉴·중분류·정렬은 User_01 행에서
--                          파생시킨다(환경마다 다른 MENU_M_ID/SUB_GROUP 을 추측하지 않기 위함).
--   2) tb_syst_auth_menu : User_01 의 회사x권한 분포를 그대로 미러(전 회사 커버).
--                          ★권한 폭을 좁히지 않는 것이 의도다 — 두 화면의 실제 인가는 서버
--                          (canManageNodeExcludeSafe: master/hr + 부서 정·부 관리자)가 강제하는데,
--                          부서 관리자는 AUTH_CD 가 일반 등급(99999 등)일 수 있다. 메뉴를 등급으로
--                          좁히면 정작 승인 권한자에게 메뉴가 안 보인다.
--                          버튼: 조회(BTN_SRCH) + 저장(BTN_SAVE) = 'Y' (승인/거부·이력 등록 노출 게이트),
--                          나머지 'N'.
--   3) tb_syst_val_d     : SYS060(감사 액션 유형) '03 상태 변경' 1건. 소정-09 승인/거부 감사행의
--                          ACTION_TYPE 값이다(AuditActionType.STATUS_CHANGE). 코드표에 없으면
--                          감사 조회 화면에서 액션명이 빈칸으로 보인다.
--
--   라우터 등록 불필요: 웹 FE 라우팅은 viewResolver.js 의 import.meta.glob + MENU_VIEW 기반이다.
--   User_09.vue / User_10.vue 파일 존재 + 본 시드만으로 LNB 노출/진입이 된다.
--
-- ★적용 전 컬럼 확인: 본 시드는 tb_syst_menu_d 에 SUB_GROUP_NM / SUB_GROUP_IDX 가 있다고 가정한다
--   (prafta-attd16-leave-usage-menu-auth.sql 이 이미 사용 중 — schema-full.sql 스냅샷은 구버전이라
--   이 두 컬럼이 없다). 없는 환경이면 두 컬럼을 SELECT/INSERT 목록에서 빼고 실행할 것.
--   확인: SHOW COLUMNS FROM tb_syst_menu_d LIKE 'SUB\_GROUP%';
--
-- 멱등성: 전 구문 WHERE NOT EXISTS 가드. 재실행해도 중복 행이 생기지 않는다.
-- 실행: 사용자 수동(운영은 SSH mysql 또는 Workbench SSH 터널). 본 파일은 작성만, DB 직접 적용 금지.
--
-- 적용 전 확인 쿼리:
--   SELECT MENU_D_ID, MENU_M_ID, MENU_VIEW, MENU_NM, SUB_GROUP_NM, SUB_GROUP_IDX, MENU_IDX
--     FROM tb_syst_menu_d WHERE MENU_D_ID LIKE 'User\_%' ORDER BY MENU_IDX;
--   SELECT CMPNY_CD, AUTH_CD, USE_YN FROM tb_syst_auth_menu WHERE MENU_D_ID = 'User_01'
--    ORDER BY CMPNY_CD, AUTH_CD;
--   SELECT SYST_VAL_D_CD, SYST_VAL_D_NM FROM tb_syst_val_d WHERE SYST_VAL_CD = 'SYS060' ORDER BY SORT_IDX;
--
-- 적용 후 확인 쿼리:
--   SELECT MENU_D_ID, MENU_NM, MENU_IDX FROM tb_syst_menu_d WHERE MENU_D_ID IN ('User_09','User_10');
--   SELECT (SELECT COUNT(1) FROM tb_syst_auth_menu WHERE MENU_D_ID='User_01') AS user01_cnt
--        , (SELECT COUNT(1) FROM tb_syst_auth_menu WHERE MENU_D_ID='User_09') AS user09_cnt
--        , (SELECT COUNT(1) FROM tb_syst_auth_menu WHERE MENU_D_ID='User_10') AS user10_cnt;
-- ============================================================================

START TRANSACTION;

-- ----------------------------------------------------------------------------
-- 1-1) 메뉴 등록 — User_09 (셀프가입 승인)
--      대메뉴(MENU_M_ID)·중분류(SUB_GROUP_*)는 User_01 행에서 승계한다.
--      MENU_IDX 는 같은 대메뉴 내 최대값 + 1 (말미 배치).
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
      'User_09'
    , SRC.MENU_M_ID
    , 'user/User_09.vue'
    , '셀프가입 승인'
    , SRC.SUB_GROUP_NM
    , SRC.SUB_GROUP_IDX
    , SRC.NEXT_IDX
    , 'Y'
    , 'SYSTEM'
    , NOW()
  FROM (
       SELECT
             M.MENU_M_ID
           , M.SUB_GROUP_NM
           , M.SUB_GROUP_IDX
           , ( SELECT IFNULL(MAX(X.MENU_IDX), 0) + 1
                 FROM tb_syst_menu_d X
                WHERE X.MENU_M_ID = M.MENU_M_ID ) AS NEXT_IDX
         FROM tb_syst_menu_d M
        WHERE M.MENU_D_ID = 'User_01'
        LIMIT 1
  ) SRC
 WHERE NOT EXISTS (
       SELECT 1 FROM tb_syst_menu_d T WHERE T.MENU_D_ID = 'User_09'
 );

-- ----------------------------------------------------------------------------
-- 1-2) 메뉴 등록 — User_10 (소정근로시간 관리)
--      User_09 등록 직후이므로 NEXT_IDX 는 자연히 User_09 + 1 이 된다.
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
      'User_10'
    , SRC.MENU_M_ID
    , 'user/User_10.vue'
    , '소정근로시간 관리'
    , SRC.SUB_GROUP_NM
    , SRC.SUB_GROUP_IDX
    , SRC.NEXT_IDX
    , 'Y'
    , 'SYSTEM'
    , NOW()
  FROM (
       SELECT
             M.MENU_M_ID
           , M.SUB_GROUP_NM
           , M.SUB_GROUP_IDX
           , ( SELECT IFNULL(MAX(X.MENU_IDX), 0) + 1
                 FROM tb_syst_menu_d X
                WHERE X.MENU_M_ID = M.MENU_M_ID ) AS NEXT_IDX
         FROM tb_syst_menu_d M
        WHERE M.MENU_D_ID = 'User_01'
        LIMIT 1
  ) SRC
 WHERE NOT EXISTS (
       SELECT 1 FROM tb_syst_menu_d T WHERE T.MENU_D_ID = 'User_10'
 );

-- ----------------------------------------------------------------------------
-- 2-1) 권한 시드 — User_09 (User_01 회사x권한 분포 미러, USE_YN 승계)
--      신규 고객사는 프로비저닝(템플릿 회사 tb_syst_auth_menu 복제)이 자동 커버하므로
--      본 구문은 "이미 존재하는" 회사만 대상이다.
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
    , 'User_09'
    , SRC.USE_YN     /* 원본(User_01) 활성 여부 승계 — User_01 을 끈 회사에서 조용히 켜지지 않게 */
    , 'Y'            /* 조회 */
    , 'N'
    , 'N'
    , 'Y'            /* 승인/거부 버튼 노출 게이트 */
    , 'N'
    , 'SYSTEM'
    , NOW()
  FROM (
       SELECT AM.CMPNY_CD, AM.AUTH_CD, AM.USE_YN
         FROM tb_syst_auth_menu AM
        WHERE AM.MENU_D_ID = 'User_01'
  ) SRC
 WHERE NOT EXISTS (
       SELECT 1
         FROM tb_syst_auth_menu X
        WHERE X.CMPNY_CD  = SRC.CMPNY_CD
          AND X.AUTH_CD   = SRC.AUTH_CD
          AND X.MENU_D_ID = 'User_09'
 );

-- ----------------------------------------------------------------------------
-- 2-2) 권한 시드 — User_10 (동일 규칙)
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
    , 'User_10'
    , SRC.USE_YN
    , 'Y'            /* 조회 */
    , 'N'
    , 'N'
    , 'Y'            /* 이력 등록/정정 버튼 노출 게이트 */
    , 'N'
    , 'SYSTEM'
    , NOW()
  FROM (
       SELECT AM.CMPNY_CD, AM.AUTH_CD, AM.USE_YN
         FROM tb_syst_auth_menu AM
        WHERE AM.MENU_D_ID = 'User_01'
  ) SRC
 WHERE NOT EXISTS (
       SELECT 1
         FROM tb_syst_auth_menu X
        WHERE X.CMPNY_CD  = SRC.CMPNY_CD
          AND X.AUTH_CD   = SRC.AUTH_CD
          AND X.MENU_D_ID = 'User_10'
 );

-- ----------------------------------------------------------------------------
-- 3) 감사 액션 코드 시드 — SYS060 '03 상태 변경'
--    소정-09 승인/거부 감사행(AuditActionType.STATUS_CHANGE)의 코드값.
--    ★거부 사유의 유일한 보존처가 이 감사행의 detailJson 이므로 코드표 등록을 생략하지 말 것.
-- ----------------------------------------------------------------------------
INSERT INTO tb_syst_val_d (
      SYST_VAL_CD
    , SYST_VAL_D_CD
    , SYST_VAL_D_NM
    , SORT_IDX
    , USE_YN
    , VAL_D_DESC
    , INSERT_NO
)
SELECT
      'SYS060'
    , '03'
    , '상태 변경'
    , 3
    , 'Y'
    , '계정 상태 전이(셀프가입 승인 06→01 / 거부 06→07). 거부 사유는 감사 detailJson 에 보존'
    , 'SYSTEM'
  FROM DUAL
 WHERE NOT EXISTS (
       SELECT 1
         FROM tb_syst_val_d D
        WHERE D.SYST_VAL_CD   = 'SYS060'
          AND D.SYST_VAL_D_CD = '03'
 );

COMMIT;

-- ----------------------------------------------------------------------------
-- (선택) tb_audit_log.ACTION_TYPE COMMENT 정렬 — 코드성 컬럼 COMMENT 규칙.
--        구조 변경이 아니며, 실행하지 않아도 기능에 영향이 없다.
-- ----------------------------------------------------------------------------
ALTER TABLE `tb_audit_log`
  MODIFY COLUMN `ACTION_TYPE` varchar(30)
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci
  NOT NULL
  COMMENT '감사 액션 유형[SYS060] 01:다운로드 03:상태변경 07:설정변경';

-- ============================================================================
-- 롤백 (필요 시 수동 실행)
-- ----------------------------------------------------------------------------
-- START TRANSACTION;
-- DELETE FROM tb_syst_auth_menu WHERE MENU_D_ID IN ('User_09', 'User_10');
-- DELETE FROM tb_syst_menu_d    WHERE MENU_D_ID IN ('User_09', 'User_10');
-- DELETE FROM tb_syst_val_d     WHERE SYST_VAL_CD = 'SYS060' AND SYST_VAL_D_CD = '03';
-- COMMIT;
-- ============================================================================
