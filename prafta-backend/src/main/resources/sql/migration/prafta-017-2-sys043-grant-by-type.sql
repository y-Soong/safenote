-- ============================================================================
-- PRAFTA-017-2 보강 시드 — SYS043 (연차 부여 방식) 공통코드 추가
-- 적용 작업: 연차현황(prafta-017-2) 수동부여 GRANT_BY_TYPE 코드화
-- 작성일: 2026-05-21
-- 사유: tb_user_leave_grant.GRANT_BY_TYPE(varchar(2))가 주석상 'AUTO'/'ADMIN'
--       문자열을 명시했으나 컬럼 폭(2)에 안 맞고 공통코드(tb_syst_val) 미등록 상태였음.
--       사용자 결정: SYS043으로 공통코드를 추가하고 코드값을 사용한다.
-- 코드값: GRANT_BY_TYPE 컬럼이 varchar(2)이므로 상세코드는 2자('01'/'02').
--         레거시 라벨(AUTO/ADMIN)은 VAL_D_INFO_1에 참조용 보존.
--           '01' = 자동 부여 (시스템 자동/스케줄러, 레거시 AUTO)
--           '02' = 관리자 수동 부여 (관리자 수동, 레거시 ADMIN)
-- 적용 환경: MySQL 8.0.42
-- 멱등성: PK 중복 시 에러. 운영 적용 후 보관용 (재실행 금지). 이미 존재 시 건너뛸 것.
-- ============================================================================

INSERT INTO `tb_syst_val_m` (`SYST_VAL_CD`, `SYST_VAL_NM`, `USE_YN`, `VAL_DESC`, `INSERT_NO`)
VALUES ('SYS043', '연차 부여 방식', 'Y', 'tb_user_leave_grant.GRANT_BY_TYPE 코드', 'SYSTEM');

INSERT INTO `tb_syst_val_d`
  (`SYST_VAL_CD`, `SYST_VAL_D_CD`, `SYST_VAL_D_NM`, `SORT_IDX`, `USE_YN`, `VAL_D_INFO_1`, `INSERT_NO`)
VALUES
  ('SYS043', '01', '자동 부여',       1, 'Y', 'AUTO',  'SYSTEM'),
  ('SYS043', '02', '관리자 수동 부여', 2, 'Y', 'ADMIN', 'SYSTEM');
