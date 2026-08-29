-- ============================================================================
-- PRAFTA-app-evidence-file-1 — 연차 증빙자료 파일타입 공통코드 시드 (SYS010 '008' 디테일 추가)
-- 작성일: 2026-08-29 (developer 산출)
-- 적용 환경: MySQL 8.0.42 / 적용은 사용자 수동(read-only MCP)
-- 참조: prafta-daily-contract-4-file-type-sys010.sql (SYS010 '007' 시드 스타일 미러)
--
-- 배경:
--   현행 SYS010(FILE_TYPE) 디테일: 001 일일점검 / 002 위험성평가 / 003 TBM /
--   004 아차사고 / 005 공지첨부 / 006 점검조치사진 / 007 일용직계약서.
--   연차 신청 시 증빙(EVIDENCE_YN='Y' 종류) 첨부 파일은 근로계약서와 동일하게
--   PII 성격 법정 문서이므로 미사용 다음 번호 '008' 을 전용 채번한다(디렉토리 그룹 겸용,
--   FileServiceImpl.PROTECTED_FILE_TYPES 에 함께 등록해 secure-base-dir 분리 저장).
--
-- 적용 전 부재 확인(필수 — 0건이어야 함):
--   SELECT SYST_VAL_D_CD FROM tb_syst_val_d WHERE SYST_VAL_CD='SYS010' AND SYST_VAL_D_CD='008';
-- ============================================================================

INSERT INTO `tb_syst_val_d` (`SYST_VAL_CD`,`SYST_VAL_D_CD`,`SYST_VAL_D_NM`,`SORT_IDX`,`USE_YN`,`INSERT_NO`) VALUES
    ('SYS010', '008', '연차 증빙자료', 8, 'Y', 'SYSTEM');

-- ----------------------------------------------------------------------------
-- 적용 후 검증:
--   SELECT SYST_VAL_D_NM FROM tb_syst_val_d WHERE SYST_VAL_CD='SYS010' AND SYST_VAL_D_CD='008'; -- 연차 증빙자료
-- 롤백 (증빙 파일이 이미 적재된 뒤에는 삭제 금지):
--   DELETE FROM `tb_syst_val_d` WHERE SYST_VAL_CD='SYS010' AND SYST_VAL_D_CD='008';
-- ============================================================================
