-- ============================================================================
-- tbm-manager-sign-1 — TBM 주관자(개설자) 종료 서명 컬럼 2종 추가
-- ⚠️ 본 파일은 작성만. 실행은 사용자가 MySQL Workbench 로 직접 수행.
-- ⚠️ 개발 DB · 운영 DB 양쪽에 동시 적용할 것 (한쪽만 적용 시 장애 재발 실증 — DB마이그 동시적용 원칙).
-- 적용 전 확인: SHOW COLUMNS FROM tb_tbm_session LIKE 'MANAGER_SIGN%'; (0건이어야 함)
-- ============================================================================
ALTER TABLE `tb_tbm_session`
  ADD COLUMN `MANAGER_SIGN_FILE_MGMT_CD` varchar(50) NULL
      COMMENT '주관자(개설자) 종료 서명 파일코드(TB_FILE_INFO, FILE_TYPE 003 — 출결 서명과 동일 체계)'
      AFTER `CANCEL_REASON`,
  ADD COLUMN `MANAGER_SIGNED_AT` datetime NULL
      COMMENT '주관자 서명 시각(DB NOW() 기록)'
      AFTER `MANAGER_SIGN_FILE_MGMT_CD`;
-- 롤백(참고): ALTER TABLE `tb_tbm_session` DROP COLUMN `MANAGER_SIGNED_AT`, DROP COLUMN `MANAGER_SIGN_FILE_MGMT_CD`;
