-- ============================================================================
-- PRAFTA-leave-conv-1 (LC-01) — 연차 시간차 환산 개편: T0/T5 DDL + SYS025 반반차 코드
-- 작성일: 2026-07-11
-- 적용 환경: MySQL 8.0.42
-- 출처: 작업지시서_연차-시간차-환산-개편 T0(환산시간 설정)·T5(반반차 단위), F4(effective-dating)
--       plan 파일 §1(DDL 확정안)·§2 LC-01·§8(MCP 실물 스키마 검증 결과)
--       정책서: policies/attd/08-leave.md §8.1(연차 사용 단위)
--
-- 변경 요약
--  1) tb_leave_conversion_policy 신설 — 시간차 1일 환산시간(분) 정책.
--     회사 단위 + 적용일(APPLY_FROM_DATE) 이력 = PK(CMPNY_CD, APPLY_FROM_DATE).
--     행 미존재 시 서비스 레이어가 480분 폴백(시드 불필요 — 8시간 사업장 결과 불변).
--  2) SYS025(연차 사용단위) 상세코드 '05' 반반차 추가 — SORT_IDX=6 append.
--     (00~04 는 SORT_IDX 1~5 사용 중 — 019-A 재정렬 이력이 있으므로 이번엔 append 만,
--      기존 코드 재정렬·수정 절대 금지)
--  3) tb_leave_usage_policy 에 ALLOW_QUARTER 추가 — 반반차(0.25일) 허용 토글.
--     ALLOW_HALF_DAY 뒤 위치. 기존 행은 DEFAULT 'N' 자동 반영.
--     ※ §8-⑧ 실물 확인: 실물 컬럼은 ALLOW_FULL_DAY/ALLOW_HALF_DAY/ALLOW_HOUR_2/
--       ALLOW_HOUR_1/ALLOW_MIN_30/USAGE_UNIT (019-A 파일의 MAX_DAILY_REQUEST 는
--       실물 DB 에 없음 — 본 파일은 ALLOW_QUARTER 추가만 하므로 영향 없음).
--
-- 멱등성(재실행 안전):
--  1) CREATE TABLE IF NOT EXISTS — 이미 존재하면 no-op.
--  2) INSERT ... SELECT ... WHERE NOT EXISTS — 기존 행 존재 시 skip.
--  3) ADD COLUMN 은 MySQL 8.0 이 IF NOT EXISTS 미지원 → INFORMATION_SCHEMA 로
--     존재 여부 확인 후 동적 실행(PREPARE) — prafta-terms-cmpny-scope-1-ddl 패턴.
--
-- 운영 적용: 사용자 수동(read-only MCP). 본 파일은 작성만, DB 직접 적용 금지.
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1) tb_leave_conversion_policy 신설 (plan §1 DDL 확정안)
--    시간차 차감 분모(1일 환산시간, 분) — 회사 단위 + 적용일 이력.
--    조회: APPLY_FROM_DATE <= 신청 대상일(WORK_YMD) 최신 1행, 미존재 시 480 폴백.
--    유효범위(60~1440, 정수)는 서비스 레이어에서 검증(§8-②).
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `tb_leave_conversion_policy` (
    `CMPNY_CD`           varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '회사 코드',
    `APPLY_FROM_DATE`    varchar(8)  COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '적용 시작일 (YYYYMMDD, 이 날짜 이후 신청 대상일분부터 적용)',
    `DAILY_CONV_MINUTES` int         NOT NULL DEFAULT 480 COMMENT '1일 환산시간(분). 시간차 차감 분모 (유효범위 60~1440, 서비스 검증)',
    `INSERT_NO`          varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '입력자(USER_CD)',
    `INSERT_DATE`        datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
    `UPDATE_NO`          varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '수정자(USER_CD)',
    `UPDATE_DATE`        datetime    DEFAULT NULL COMMENT '수정일시',
    PRIMARY KEY (`CMPNY_CD`, `APPLY_FROM_DATE`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='연차 시간차 1일 환산시간 정책 (회사 단위, effective-dating)';

-- ----------------------------------------------------------------------------
-- 2) SYS025 상세코드 '05' 반반차 추가 (append 만 — 00~04 불변)
--    현행 SYS025: 00=1일(1) / 01=반차(2) / 02=시간차2h(3) / 03=시간차1h(4) / 04=시간차30분(5)
--    → '05' 반반차 SORT_IDX=6 으로 뒤에 붙인다.
--    멱등: 동일 (SYST_VAL_CD, SYST_VAL_D_CD) 행 존재 시 INSERT skip.
-- ----------------------------------------------------------------------------
INSERT INTO tb_syst_val_d (SYST_VAL_CD, SYST_VAL_D_CD, SYST_VAL_D_NM, SORT_IDX, USE_YN, INSERT_NO, INSERT_DATE)
SELECT 'SYS025', '05', '반반차', 6, 'Y', 'SYSTEM', NOW()
  FROM DUAL
 WHERE NOT EXISTS (
        SELECT 1
          FROM tb_syst_val_d
         WHERE SYST_VAL_CD   = 'SYS025'
           AND SYST_VAL_D_CD = '05'
       );

-- ----------------------------------------------------------------------------
-- 3) tb_leave_usage_policy — ALLOW_QUARTER 추가 (ALLOW_HALF_DAY 뒤)
--    MySQL 8.0 은 ADD COLUMN IF NOT EXISTS 미지원 → 재실행 시 1060 에러 방지 위해
--    INFORMATION_SCHEMA 존재 확인 후 동적 실행(재실행 안전).
--    기존 행은 DEFAULT 'N' 자동 반영(반반차는 회사가 명시 허용해야 노출).
-- ----------------------------------------------------------------------------
SET @col_exists := (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME   = 'tb_leave_usage_policy'
       AND COLUMN_NAME  = 'ALLOW_QUARTER'
);
SET @ddl := IF(@col_exists = 0,
    'ALTER TABLE tb_leave_usage_policy ADD COLUMN ALLOW_QUARTER char(1) NOT NULL DEFAULT ''N'' COMMENT ''반반차(0.25일) 허용 Y/N (SYS025-05)'' AFTER ALLOW_HALF_DAY',
    'DO 0'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ----------------------------------------------------------------------------
-- 적용 후 확인 쿼리 (수동)
-- ----------------------------------------------------------------------------
-- SHOW CREATE TABLE tb_leave_conversion_policy;
-- SELECT SYST_VAL_D_CD, SYST_VAL_D_NM, SORT_IDX, USE_YN
--   FROM tb_syst_val_d WHERE SYST_VAL_CD = 'SYS025' ORDER BY SORT_IDX;
--   → 00~04 기존값 불변 + '05' 반반차(SORT_IDX=6) 1행 추가 확인
-- SELECT COLUMN_NAME, COLUMN_DEFAULT, IS_NULLABLE, ORDINAL_POSITION
--   FROM INFORMATION_SCHEMA.COLUMNS
--  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_leave_usage_policy'
--  ORDER BY ORDINAL_POSITION;
--   → ALLOW_QUARTER 가 ALLOW_HALF_DAY 바로 뒤, DEFAULT 'N' 확인

-- ============================================================================
-- 롤백 (필요 시 수동 실행)
-- ----------------------------------------------------------------------------
-- DROP TABLE IF EXISTS `tb_leave_conversion_policy`;
-- DELETE FROM tb_syst_val_d WHERE SYST_VAL_CD = 'SYS025' AND SYST_VAL_D_CD = '05';
-- ALTER TABLE tb_leave_usage_policy DROP COLUMN ALLOW_QUARTER;
-- ============================================================================
