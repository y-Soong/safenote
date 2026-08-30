-- ============================================================================
-- PRAFTA-TBM-SIGN-FILE-1 — TBM 서명 전용 파일타입 공통코드 시드 (SYS010 '009' 디테일 추가)
-- 작성일: 2026-08-31 (developer 산출)
-- 적용 환경: MySQL 8.0.42 / 적용은 사용자 수동(read-only MCP)
-- ★★ 개발 DB · 운영 DB 양쪽에 동시 적용할 것 (한쪽만 적용 시 장애 반복 — CLAUDE.md 마이그레이션 원칙).
-- 참조: prafta-app-evidence-file-1-sys010.sql (SYS010 '008' 시드 스타일 미러)
--
-- 배경(security H-1 후속):
--   현행 SYS010(FILE_TYPE) 디테일: 001 일일점검 / 002 위험성평가 / 003 TBM(교육자료) /
--   004 아차사고 / 005 공지첨부 / 006 점검조치사진 / 007 일용직계약서 / 008 연차 증빙자료.
--   TBM 자필 서명 이미지(참석자 입실·종료 서명 / 주관자 서명)가 교육자료와 같은 '003' 에 얹혀
--   저장되어, 무인증 정적 마운트(/uploads/**)로 열람·열거가 가능했다(서명 PII 노출).
--   '003' 을 통째로 보호 편입하면 교육자료 미리보기(previewUrl 정적 URL)가 전부 깨지므로,
--   미사용 다음 번호 '009' 를 서명 전용으로 채번하고 '009' 만 보호 파일타입으로 편입한다
--   (FileServiceImpl.PROTECTED_FILE_TYPES → secure-base-dir 분리 저장, 인증 스트림 EP 로만 서빙).
--   교육자료는 '003' 그대로 유지한다.
--
-- 채번 시퀀스 시드 불필요:
--   FNC_CMM_SEQ_NEXTVAL 이 시퀀스 행을 INSERT ... ON DUPLICATE KEY 로 자동 생성한다.
--
-- 기존 '003' 서명 파일 소급 이전 없음(사용자 확정):
--   FileServiceImpl.resolveSavePath 가 DB FILE_PATH 선두 프리픽스(/uploads vs /uploads-secure)로
--   base 를 판별하므로, 인증 스트림 EP 는 신규(009/secure)·기존(003/public) 파일을 모두 읽는다.
--
-- 적용 전 부재 확인(0건이면 신규 INSERT, 1건이면 아래 문장이 멱등 UPDATE 로 흡수):
--   SELECT SYST_VAL_D_CD, SYST_VAL_D_NM FROM tb_syst_val_d
--    WHERE SYST_VAL_CD='SYS010' AND SYST_VAL_D_CD='009';
-- ============================================================================

-- 멱등(재실행 안전): PK(SYST_VAL_CD, SYST_VAL_D_CD) 중복 시 표시명/정렬/사용여부만 현행화한다.
INSERT INTO `tb_syst_val_d`
    (`SYST_VAL_CD`, `SYST_VAL_D_CD`, `SYST_VAL_D_NM`, `SORT_IDX`, `USE_YN`, `INSERT_NO`) VALUES
    ('SYS010', '009', 'TBM 서명', 9, 'Y', 'SYSTEM')
ON DUPLICATE KEY UPDATE
      `SYST_VAL_D_NM` = 'TBM 서명'
    , `SORT_IDX`      = 9
    , `USE_YN`        = 'Y';

-- ----------------------------------------------------------------------------
-- 적용 후 검증:
--   SELECT SYST_VAL_D_NM, SORT_IDX, USE_YN FROM tb_syst_val_d
--    WHERE SYST_VAL_CD='SYS010' AND SYST_VAL_D_CD='009';   -- TBM 서명 / 9 / Y
-- 롤백 (서명 파일이 이미 '009' 로 적재된 뒤에는 삭제 금지):
--   DELETE FROM `tb_syst_val_d` WHERE SYST_VAL_CD='SYS010' AND SYST_VAL_D_CD='009';
-- ============================================================================
