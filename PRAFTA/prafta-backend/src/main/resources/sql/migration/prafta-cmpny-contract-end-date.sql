-- ============================================================================
-- PRAFTA — TB_CMPNY 계약 종료일자(CONTRACT_END_DATE) 컬럼 추가
-- 작성일: 2026-06-28
-- 적용 환경: MySQL 8.0.42
--
-- 변경 요약
--   1) tb_cmpny.CONTRACT_END_DATE varchar(8) NULL 추가 (계약 종료일자, 'YYYYMMDD').
--      - 기존 CONTRACT_YN(계약여부)과 짝을 이루는 컬럼. 종료일 미정/무기한 계약은 NULL.
--      - 날짜 표현은 프로젝트 관례(varchar(8) 'YYYYMMDD')를 따른다
--        (cf. tb_site.STR_DATE/END_DATE, tb_daily_user.WORK_EXPIRE_DATE 동일 패턴).
--      - 기존 행은 NULL 백필(계약 종료일 미설정). NOT NULL/DEFAULT 미부여.
--      - 위치: CONTRACT_YN 다음 (AFTER CONTRACT_YN) — 계약 정보 인접 배치, 감사 컬럼 앞.
--
-- 적용 전 부재 확인 (운영 적용 직전 권장):
--   SHOW COLUMNS FROM tb_cmpny LIKE 'CONTRACT_END_DATE';
--
-- 멱등성: 본 파일은 운영 적용 후 보관용(재실행 금지). 적용 전 상태 확인 후 수행.
-- 운영 적용: 사용자 수동(read-only MCP). 본 파일은 작성만, DB 직접 적용 금지.
-- ============================================================================

-- 1) tb_cmpny.CONTRACT_END_DATE 컬럼 추가 (계약 종료일자, YYYYMMDD)
ALTER TABLE `tb_cmpny`
    ADD COLUMN `CONTRACT_END_DATE` varchar(8) COLLATE utf8mb4_unicode_ci NULL
        COMMENT '계약종료일자(YYYYMMDD)' AFTER `CONTRACT_YN`;

-- ============================================================================
-- 롤백 (필요 시 수동 실행)
-- ----------------------------------------------------------------------------
-- ALTER TABLE `tb_cmpny` DROP COLUMN `CONTRACT_END_DATE`;
-- ============================================================================
