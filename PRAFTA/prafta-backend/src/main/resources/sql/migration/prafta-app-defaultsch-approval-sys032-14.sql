-- ============================================================================
-- PRAFTA-기본근무타입-승인제-1 — SYS032(요청 유형) 코드 14 시드
-- 작성일: 2026-08-26
-- 적용 환경: MySQL 8.0.42
-- 참조: .claude/requests/app_requests/작업지시서_기본근무타입-변경-관리자승인제(앱).plan.md
--
-- 변경 요약
--   SYS032 에 '14 = 기본 근무타입 변경 요청' 디테일 행 추가.
--   TB_USER_ATTD_REQ.SCH_CD/WORK_YMD 는 prafta-app-007 마이그레이션으로 이미 존재(nullable) —
--   본 작업은 ALTER TABLE 불필요.
--
-- 적용 전 부재 확인 (운영 적용 직전 권장):
--   SELECT COUNT(*) FROM tb_syst_val_d WHERE SYST_VAL_CD='SYS032' AND SYST_VAL_D_CD='14';
--   개발 DB 재확인 결과(2026-08-26, developer): SYS032 는 01~13 이 사용 중이며 14 는 여유값임을
--   `SELECT SYST_VAL_D_CD, SYST_VAL_D_NM FROM tb_syst_val_d WHERE SYST_VAL_CD='SYS032' ORDER BY SYST_VAL_D_CD;` 로 재확인함.
--
-- 멱등성: 본 파일은 운영 적용 후 보관용(재실행 금지).
-- 운영 적용: 사용자 수동(read-only MCP). 본 파일은 작성만, DB 직접 적용 금지.
-- ============================================================================

INSERT INTO `tb_syst_val_d` (
      `SYST_VAL_CD`
    , `SYST_VAL_D_CD`
    , `SYST_VAL_D_NM`
    , `SORT_IDX`
    , `USE_YN`
    , `VAL_D_DESC`
    , `INSERT_NO`
) VALUES (
      'SYS032'
    , '14'
    , '기본 근무타입 변경 요청'
    , 14
    , 'Y'
    , '근로자 본인 기본 근무타입(DEFAULT_SCH_CD) 변경 신청 — 관리자 승인 시에만 반영(2026-08-26 정책)'
    , 'SYSTEM'
);
