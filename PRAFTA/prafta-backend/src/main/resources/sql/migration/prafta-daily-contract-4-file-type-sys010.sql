-- ============================================================================
-- PRAFTA-daily-contract-4 — 일용직 계약서 파일타입 공통코드 시드 (SYS010 '007' 디테일 추가)
-- 작성일: 2026-07-16 (T3 developer 산출 — T1 마이그 3본의 후속 보완)
-- 적용 환경: MySQL 8.0.42 / 적용은 사용자 수동(read-only MCP)
-- 참조: prafta-subcon-t6-3-file-type-sys010.sql (SYS010 '006' 시드 스타일 미러)
--
-- 배경:
--   현행 SYS010(FILE_TYPE) 디테일: 001 일일점검 / 002 위험성평가 / 003 TBM /
--   004 아차사고 / 005 공지첨부 / 006 점검조치사진.
--   일용직 근로계약서(양식 이미지·서명 PNG 원본·합성본)는 3년 보존(근로기준법 §42) 대상의
--   독립 파일 그룹이므로 미사용 다음 번호 '007' 을 전용 채번한다(디렉토리 그룹 겸용).
--
-- 적용 전 부재 확인(필수 — 0건이어야 함):
--   SELECT SYST_VAL_D_CD FROM tb_syst_val_d WHERE SYST_VAL_CD='SYS010' AND SYST_VAL_D_CD='007';
-- ============================================================================

INSERT INTO `tb_syst_val_d` (`SYST_VAL_CD`,`SYST_VAL_D_CD`,`SYST_VAL_D_NM`,`SORT_IDX`,`USE_YN`,`INSERT_NO`) VALUES
    ('SYS010', '007', '일용직계약서', 7, 'Y', 'SYSTEM');

-- ----------------------------------------------------------------------------
-- 적용 후 검증:
--   SELECT SYST_VAL_D_NM FROM tb_syst_val_d WHERE SYST_VAL_CD='SYS010' AND SYST_VAL_D_CD='007'; -- 일용직계약서
-- 롤백 (계약서/서명 파일이 이미 적재된 뒤에는 삭제 금지):
--   DELETE FROM `tb_syst_val_d` WHERE SYST_VAL_CD='SYS010' AND SYST_VAL_D_CD='007';
-- ============================================================================
