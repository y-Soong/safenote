-- =============================================================================
-- prafta-034-001 마이그레이션: Attd_11 (월별 사용자 근태 판정) 메뉴/권한 등록
-- =============================================================================
-- 작성일 : 2026-05-27
-- 작업 ID : PRAFTA-034-2
-- 단일출처: .claude/requests/prafta-034-decisions.md §8
-- 배경    : 신규 읽기 전용 조회 화면 Attd_11.vue 를 메뉴(tb_syst_menu_d)에
--           등록하고, 기존 Attd_07 화면과 동일한 (CMPNY_CD, AUTH_CD) 권한 집합으로
--           조회 권한(tb_syst_auth_menu)을 부여한다.
-- 주의    : *** 운영 미적용. 사용자 확인 후 반영. ***
--           본 파일은 멱등하게(이미 존재하면 스킵) 작성되었다.
--           Attd_11 은 읽기 전용이므로 조회(BTN_SRCH)만 'Y', 신규/삭제/저장/엑셀은 'N'.
-- 롤백    : 파일 하단 "원복 SQL" 참조.
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1) tb_syst_menu_d — Attd_11 메뉴 등록 (PK = MENU_D_ID, MENU_M_ID)
--    값/감사 컬럼 패턴은 기존 attd 대메뉴 하위 화면(Attd_10/Attd_07) 행과 동일.
--    이미 동일 PK 행이 존재하면 INSERT 하지 않는다(멱등).
-- -----------------------------------------------------------------------------
INSERT INTO tb_syst_menu_d (
      MENU_D_ID
    , MENU_M_ID
    , MENU_VIEW
    , MENU_NM
    , MENU_IDX
    , MENU_DESC
    , USE_YN
    , INSERT_NO
    , INSERT_DATE
)
SELECT
      'Attd_11'
    , 'attd'
    , 'attd/Attd_11.vue'
    , '월별 사용자 근태 판정'
    , 11
    , NULL
    , 'Y'
    , 'SYSTEM'
    , NOW()
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM tb_syst_menu_d
    WHERE MENU_D_ID = 'Attd_11'
      AND MENU_M_ID = 'attd'
);

-- -----------------------------------------------------------------------------
-- 2) tb_syst_auth_menu — Attd_07 의 (CMPNY_CD, AUTH_CD) 권한 집합을
--    Attd_11 로 복제 (PK = CMPNY_CD, AUTH_CD, MENU_D_ID).
--    읽기 전용 화면이므로 조회(BTN_SRCH)만 'Y', 신규/삭제/저장/엑셀은 'N' 고정.
--    USE_YN 은 원본(Attd_07) 값을 그대로 승계.
--    이미 (CMPNY_CD, AUTH_CD, 'Attd_11') 행이 존재하면 INSERT 하지 않는다(멱등).
--    (Attd_07 매핑이 없는 회사/권한은 추측하지 않고 복제 대상에서 자연 제외.)
-- -----------------------------------------------------------------------------
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
      S.CMPNY_CD
    , S.AUTH_CD
    , 'Attd_11'        AS MENU_D_ID
    , S.USE_YN
    , 'Y'              AS BTN_SRCH   -- 조회 전용
    , 'N'              AS BTN_NEW
    , 'N'              AS BTN_DELT
    , 'N'              AS BTN_SAVE
    , 'N'              AS BTN_EXCL
    , 'SYSTEM'         AS INSERT_NO
    , NOW()            AS INSERT_DATE
FROM tb_syst_auth_menu S
WHERE S.MENU_D_ID = 'Attd_07'
  AND NOT EXISTS (
        SELECT 1 FROM tb_syst_auth_menu T
        WHERE T.CMPNY_CD  = S.CMPNY_CD
          AND T.AUTH_CD   = S.AUTH_CD
          AND T.MENU_D_ID = 'Attd_11'
  );

-- =============================================================================
-- 원복 SQL (롤백 시 — 위 INSERT 로 추가된 행만 제거)
-- =============================================================================
-- DELETE FROM tb_syst_auth_menu WHERE MENU_D_ID = 'Attd_11';
-- DELETE FROM tb_syst_menu_d    WHERE MENU_D_ID = 'Attd_11' AND MENU_M_ID = 'attd';
