-- ============================================================================
-- PRAFTA-CREDIT-DUAL-1 — 경력인정 이원화(반영/일수 모드) DDL + 코드테이블 시드
-- 작성일: 2026-08-21
-- 참조 지시서: .claude/requests/web_requests/작업지시서_경력인정-이원화_비례부여-차액보전.md §0(P-5~P-10) §1-1
-- 참조 plan  : .claude/requests/web_requests/작업지시서_경력인정-이원화_비례부여-차액보전.plan.md §D
-- 적용 환경 : MySQL 8.0.42
--
-- ★사전 실조회 결과(2026-08-21, 개발 DB `prafta-mysql` 직접 조회, mysql client 이용):
--   1) tb_user_service_credit PK = 복합 PK (CMPNY_CD, CREDIT_ID) 로 확정
--      (SHOW CREATE TABLE 실측 — schema-full.sql 스냅샷의 단독 PK(CREDIT_ID) 표기는 구버전).
--      본 파일은 컬럼 추가만 하므로 PK 형상과 무관하게 그대로 유효.
--   2) tb_syst_val_d SYS035 기존 값(SORT_IDX 전부 NULL — 정렬 미사용 컨벤션):
--        MANUAL_BONUS / MANUAL_CONDOLENCE / MANUAL_LONG_SERVICE / MANUAL_OTHER /
--        STATUTORY_ANNUAL / STATUTORY_MONTHLY / STATUTORY_TENURE_BONUS
--      → 신규 'MANUAL_CAREER' 도 SORT_IDX NULL 로 동일 컨벤션 유지.
--   3) tb_leave_type_mgmt SYSTEM_YN='Y' 시드 6종 확인(prafta-018 패턴) — SYS_CAREER 는
--      LEAVE_NATURE_TYPE='02'(약정, 지시서 P-10)로 동일 패턴 추가.
--
-- 변경 요지 (지시서 §1-1, §0 P-5~P-10):
--   1) TB_USER_SERVICE_CREDIT 2컬럼 추가 (기존 행은 DEFAULT 'Y' = 현행 동작 무회귀).
--   2) SYS035(GRANT_TYPE)에 'MANUAL_CAREER' 시드 추가(일수 모드 연간 자동 부여 유형, prafta-017-2 스타일 미러).
--   3) 시스템 LEAVE_CD 시드 'SYS_CAREER'(경력 인정 휴가, 약정) 전 활성 회사 백필(prafta-018 스타일 미러,
--      P-10: MANUAL_CAREER 자동 부여의 휴가 항목 — SYS_ANNUAL 재사용 기각, 신규 시드 채택).
--
-- 제약(서버 검증, DB CHECK 아님 — 현행 컨벤션):
--   LEAVE_CALC_YN='N' 이면 EXTRA_LEAVE_DAYS 필수(0.5 단위, 0 초과 25 이하).
--   LEAVE_CALC_YN='Y' 이면 EXTRA_LEAVE_DAYS 는 서버가 NULL 강제(무시).
--   → User01ServiceImpl.normalizeLeaveCalcYn / resolveExtraLeaveDays 에서 강제.
--
-- 실행 방법 (Windows + Git Bash, dev 환경 — 사용자 Workbench 실행 원칙, 본 파일은 실행하지 않음):
--   mysql -h127.0.0.1 -P3306 -udev_prafta -p'prafta12345!' \
--         --default-character-set=utf8mb4 --batch prafta \
--         < prafta-backend/src/main/resources/sql/migration/prafta-credit-dual-1-ddl.sql
--
-- 멱등성: ALTER 는 컬럼 존재 시 에러(재실행 금지, 운영 적용 후 보관용). SYS035/SYS_CAREER 시드는
--   INSERT ... WHERE NOT EXISTS 패턴이라 재실행해도 중복 추가되지 않는다(신규 회사 추가 후 재실행 가능).
--
-- 적용 순서: 개발 DB 먼저 적용 후 검증 → 운영 DB 동시 적용(CLAUDE.md "DB마이그=개발·운영 동시적용 원칙").
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1) TB_USER_SERVICE_CREDIT — 경력인정 이원화 2컬럼 추가
-- ----------------------------------------------------------------------------
ALTER TABLE tb_user_service_credit
  ADD COLUMN LEAVE_CALC_YN char(1) NOT NULL DEFAULT 'Y'
    COMMENT '연차 산정 반영 여부 (Y=개월수 산식 반영/N=기록용+일수 부여)' AFTER CREDIT_MONTHS,
  ADD COLUMN EXTRA_LEAVE_DAYS decimal(4,1) NULL
    COMMENT '일수 모드(N) 전용 연간 추가 부여 일수 (반영 모드 NULL, 0.5 단위, 0 초과 상한 25)' AFTER LEAVE_CALC_YN;

-- ----------------------------------------------------------------------------
-- 2) SYS035(GRANT_TYPE) — MANUAL_CAREER 시드 (일수 모드 연간 자동 부여 유형)
--    prafta-017-2-sys043-grant-by-type.sql 스타일 미러. SYS035 마스터는 이미 존재(INSERT 생략).
-- ----------------------------------------------------------------------------
INSERT INTO tb_syst_val_d
      (SYST_VAL_CD, SYST_VAL_D_CD, SYST_VAL_D_NM, SORT_IDX, USE_YN, VAL_D_DESC, INSERT_NO)
SELECT
      'SYS035', 'MANUAL_CAREER', '경력인정 연차(약정)', NULL, 'Y',
      '경력인정 일수 모드 연간 자동 부여 (GRANT_BY_TYPE=01, 촉진 비대상 약정, 지시서 §1-4)', 'SYSTEM'
 WHERE NOT EXISTS (
       SELECT 1 FROM tb_syst_val_d
        WHERE SYST_VAL_CD = 'SYS035' AND SYST_VAL_D_CD = 'MANUAL_CAREER'
 );

-- ----------------------------------------------------------------------------
-- 3) TB_LEAVE_TYPE_MGMT — 시스템 시드 'SYS_CAREER'(경력 인정 휴가, 약정) 전 활성 회사 백필
--    prafta-018-seed-system-leave-cd.sql 스타일 미러(단일 트랜잭션, 회사별 NOT EXISTS 멱등).
--    LEAVE_NATURE_TYPE='02'(약정) — 지시서 P-10 확정(법정 아님, 제61조 촉진 비대상).
-- ----------------------------------------------------------------------------
START TRANSACTION;

INSERT INTO tb_leave_type_mgmt (
      CMPNY_CD
    , LEAVE_CD
    , LEAVE_NO
    , LEAVE_NM
    , LEAVE_TYPE
    , GRANT_TYPE
    , PAID_TYPE
    , LEAVE_NATURE_TYPE
    , USE_YN
    , SYSTEM_YN
    , INSERT_NO
    , INSERT_DATE
)
SELECT
      c.CMPNY_CD
    , 'SYS_CAREER'
    , 'SYS_CAREER'
    , '경력 인정 휴가'
    , '02'
    , '01'
    , '01'
    , '02'                -- 약정(법정 아님) — 지시서 P-10
    , 'Y'
    , 'Y'
    , 'SYSTEM'
    , NOW()
  FROM tb_cmpny c
 WHERE c.USE_YN = 'Y'
   AND NOT EXISTS (
       SELECT 1
         FROM tb_leave_type_mgmt t
        WHERE t.CMPNY_CD = c.CMPNY_CD
          AND t.LEAVE_CD = 'SYS_CAREER'
   );

COMMIT;

-- ----------------------------------------------------------------------------
-- 검증 쿼리 (read-only, 트랜잭션 외 실행 권장):
--   SHOW CREATE TABLE tb_user_service_credit;
--   SELECT * FROM tb_syst_val_d WHERE SYST_VAL_CD='SYS035' AND SYST_VAL_D_CD='MANUAL_CAREER';
--   SELECT CMPNY_CD, LEAVE_CD, LEAVE_NM, LEAVE_NATURE_TYPE, SYSTEM_YN
--     FROM tb_leave_type_mgmt WHERE LEAVE_CD='SYS_CAREER' ORDER BY CMPNY_CD;
--   기대: 활성 회사 수만큼 SYS_CAREER 행 1건씩.
-- ============================================================================
-- 끝.
-- ============================================================================
