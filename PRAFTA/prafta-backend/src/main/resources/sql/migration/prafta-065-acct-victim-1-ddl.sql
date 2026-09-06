-- ============================================================================
-- prafta-065 : 사고관리 재해자 다중 등록 (1) DDL + 코드그룹
-- 작성일: 2026-09-06 / 적용 환경: MySQL 8.x (개발·운영 동시 적용, 사용자 Workbench 직접 실행)
-- 근거: 요청서 prafta-065.md R1 · D1(헤더 재해자 컬럼 = 대표 유지) · D2 · D5 / 정책서 safety/06-accident.md §6.2
-- 참조 스타일: prafta-048-acct-domain.sql, prafta-daily-contract-2-sys-codes.sql
--
-- 변경 요약
--   1) tb_acct_victim 신설 — 사고 1:N 재해자(대표 재해자는 tb_acct 헤더 컬럼에 그대로 유지)
--   2) tb_syst_val_m / tb_syst_val_d : SYS084(재해 결과) 신설 — DEATH/INJURY/DISEASE
--
-- ★ SYS 채번 주의: 개발 DB 실측(2026-09-06) 최대 SYS083(통상근로 사유). SYS080 은 공번이나 재사용 금지.
-- 적용 전 확인(전부 0건이어야 함 — 1건 이상이면 중단 후 보고):
--   SELECT 1 FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'tb_acct_victim';
--   SELECT SYST_VAL_CD FROM tb_syst_val_m WHERE SYST_VAL_CD = 'SYS084';
--   SELECT SYST_VAL_D_CD FROM tb_syst_val_d WHERE SYST_VAL_CD = 'SYS084';
--   SELECT MAX(SYST_VAL_CD) FROM tb_syst_val_m WHERE SYST_VAL_CD LIKE 'SYS0%';   -- SYS083 이어야 함
-- 멱등성: CREATE TABLE IF NOT EXISTS + 코드 INSERT 는 WHERE NOT EXISTS → 재실행 무해.
-- 다음 단계: prafta-065-acct-victim-2-backfill.sql (기존 사고 → 재해자 1행 백필) 를 반드시 이어서 적용.
-- ============================================================================

-- ----------------------------------------------------------------------------
-- (1) tb_acct_victim — 사고 재해자(사고 1:N)
--     물리 삭제(제외 = DELETE). DEL_YN 없음(tb_acct_link/legal_step 자식 관례). 사고 soft delete 시 자식은 그대로 둔다.
--     대표 재해자 = 현재 남아있는 인원 중 VICTIM_SEQ 최소(서버가 tb_acct 헤더 컬럼에 반영).
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `tb_acct_victim` (
    `CMPNY_CD`         varchar(50)  NOT NULL COMMENT '회사코드',
    `SITE_CD`          varchar(50)  NOT NULL COMMENT '사업장코드',
    `ACCT_ID`          varchar(20)  NOT NULL COMMENT '사고 ID(tb_acct.ACCT_ID)',
    `VICTIM_SEQ`       int          NOT NULL COMMENT '재해자 순번(사고 내 1부터, MAX+1 채번)',
    `USER_TYPE_CD`     varchar(10)  NOT NULL COMMENT '재해자 사용자유형[SYS050] REGULAR:정규 DAILY:일용',
    `USER_CD`          varchar(20)  NOT NULL COMMENT '재해자 사용자코드(tb_user.USER_CD 또는 tb_daily_user.USER_CD)',
    `VICTIM_RESULT_CD` varchar(10)  NOT NULL COMMENT '재해 결과[SYS084] DEATH:사망 INJURY:부상 DISEASE:질병',
    `CARE_DAYS`        int                   DEFAULT NULL COMMENT '예상 요양 일수(등록 시 미확정 NULL)',
    `REST_DAYS`        int                   DEFAULT NULL COMMENT '예상 휴업 일수(등록 시 미확정 NULL)',
    `INJURY_PART`      varchar(100)          DEFAULT NULL COMMENT '부상 부위(직접입력)',
    `INJURY_DESC`      varchar(500)          DEFAULT NULL COMMENT '부상 내용(직접입력)',
    `INSERT_NO`        varchar(50)           DEFAULT 'SYSTEM' COMMENT '입력자',
    `INSERT_DATE`      datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
    `UPDATE_NO`        varchar(50)           DEFAULT NULL COMMENT '수정자',
    `UPDATE_DATE`      datetime              DEFAULT NULL COMMENT '수정일시',
    PRIMARY KEY (`CMPNY_CD`, `SITE_CD`, `ACCT_ID`, `VICTIM_SEQ`),
    UNIQUE KEY `UX_TB_ACCT_VICTIM_USER` (`CMPNY_CD`, `SITE_CD`, `ACCT_ID`, `USER_TYPE_CD`, `USER_CD`),
    KEY `IX_TB_ACCT_VICTIM_USER` (`CMPNY_CD`, `USER_CD`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='사고 재해자(사고 1:N)';

-- ----------------------------------------------------------------------------
-- (2) 코드그룹 SYS084 재해 결과 (전사 공통, CMPNY_CD 없음)
-- ----------------------------------------------------------------------------
INSERT INTO `tb_syst_val_m` (`SYST_VAL_CD`, `SYST_VAL_NM`, `USE_YN`, `VAL_DESC`, `INSERT_NO`)
SELECT 'SYS084', '재해 결과', 'Y', 'tb_acct_victim.VICTIM_RESULT_CD 코드(재해자별 결과 — 등급 자동 판정 없음)', 'SYSTEM'
  FROM DUAL
 WHERE NOT EXISTS (SELECT 1 FROM `tb_syst_val_m` WHERE `SYST_VAL_CD` = 'SYS084');

INSERT INTO `tb_syst_val_d` (`SYST_VAL_CD`, `SYST_VAL_D_CD`, `SYST_VAL_D_NM`, `SORT_IDX`, `USE_YN`, `VAL_D_DESC`, `INSERT_NO`)
SELECT 'SYS084', 'DEATH', '사망', 1, 'Y', '재해자 사망', 'SYSTEM' FROM DUAL
 WHERE NOT EXISTS (SELECT 1 FROM `tb_syst_val_d` WHERE `SYST_VAL_CD` = 'SYS084' AND `SYST_VAL_D_CD` = 'DEATH');
INSERT INTO `tb_syst_val_d` (`SYST_VAL_CD`, `SYST_VAL_D_CD`, `SYST_VAL_D_NM`, `SORT_IDX`, `USE_YN`, `VAL_D_DESC`, `INSERT_NO`)
SELECT 'SYS084', 'INJURY', '부상', 2, 'Y', '재해자 부상(백필 기본값)', 'SYSTEM' FROM DUAL
 WHERE NOT EXISTS (SELECT 1 FROM `tb_syst_val_d` WHERE `SYST_VAL_CD` = 'SYS084' AND `SYST_VAL_D_CD` = 'INJURY');
INSERT INTO `tb_syst_val_d` (`SYST_VAL_CD`, `SYST_VAL_D_CD`, `SYST_VAL_D_NM`, `SORT_IDX`, `USE_YN`, `VAL_D_DESC`, `INSERT_NO`)
SELECT 'SYS084', 'DISEASE', '질병', 3, 'Y', '재해자 질병(업무상 질병)', 'SYSTEM' FROM DUAL
 WHERE NOT EXISTS (SELECT 1 FROM `tb_syst_val_d` WHERE `SYST_VAL_CD` = 'SYS084' AND `SYST_VAL_D_CD` = 'DISEASE');

-- ============================================================================
-- 적용 후 확인:
--   SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'tb_acct_victim'; -- 1
--   SELECT SYST_VAL_D_CD, SYST_VAL_D_NM, SORT_IDX FROM tb_syst_val_d WHERE SYST_VAL_CD = 'SYS084' ORDER BY SORT_IDX;  -- 3행
-- ============================================================================
