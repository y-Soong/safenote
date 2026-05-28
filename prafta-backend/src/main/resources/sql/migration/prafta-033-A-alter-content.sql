-- ============================================================================
-- PRAFTA-033-A — 콘텐츠 라이브러리 테이블 ALTER (방향 A 확장)
-- 작성일: 2026-05-27
-- 적용 환경: MySQL 8.0.42
-- 참조: .claude/requests/prafta-033-A-ddl-content.md §1
--
-- 변경 요약
--  1) tb_tbm_edu_mtrl 에 SITE_CD(콘텐츠 스코프) 추가 + 스코프 조회 인덱스.
--     - SITE_CD IS NULL = 회사공통, 값 = 해당 사업장 전용.
--     - 카테고리는 신규 코드그룹 미생성, 기존 MTRL_TYPE(COM003 "TBM교육타입") 재사용.
--     - CMPNY_CD 는 기존 varchar(10) 유지(데이터/제약 리스크 회피). 신규 테이블만 varchar(50).
--  2) tb_tbm_edu_mtrl_item 에 THUMB_FILE_MGMT_CD(썸네일)/DURATION_SEC(영상길이) 추가.
--     - W-02 미리보기, W-03 상세에서 사용.
--     - 신규 파일참조 컬럼은 TB_FILE_INFO.FILE_MGMT_CD(varchar50)에 맞춰 varchar(50).
--     - 기존 FILE_MGMT_CD(varchar40)는 레거시 호환 위해 그대로 둔다.
--
-- 적용 전 부재 확인:
--   SHOW COLUMNS FROM tb_tbm_edu_mtrl LIKE 'SITE_CD';
--   SHOW COLUMNS FROM tb_tbm_edu_mtrl_item LIKE 'THUMB_FILE_MGMT_CD';
-- 멱등성: ADD COLUMN/INDEX 중복 실행 시 에러. 운영 적용 후 보관용(재실행 금지).
-- ⚠️ 운영 적용은 사용자 수동(read-only MCP). 본 파일은 작성만, DB 직접 적용 금지.
-- ============================================================================

-- (1) 콘텐츠 묶음 마스터: 스코프(회사공통/사업장) 추가
ALTER TABLE `tb_tbm_edu_mtrl`
  ADD COLUMN `SITE_CD` varchar(50) NULL COMMENT '사업장코드 (NULL=회사공통, 값=해당 사업장 전용)' AFTER `CMPNY_CD`;

-- 조회 인덱스 보강 (스코프 필터: 회사공통 OR 자기사업장)
ALTER TABLE `tb_tbm_edu_mtrl`
  ADD INDEX `IX_TBM_EDU_MTRL_02` (`CMPNY_CD`, `SITE_CD`, `USE_YN`);

-- (2) 콘텐츠 세부항목: 썸네일/영상길이 추가
ALTER TABLE `tb_tbm_edu_mtrl_item`
  ADD COLUMN `THUMB_FILE_MGMT_CD` varchar(50) NULL COMMENT '썸네일 파일코드 (동영상 첫프레임/PDF 첫페이지/이미지 리사이즈 자동생성)' AFTER `FILE_MGMT_CD`,
  ADD COLUMN `DURATION_SEC` int NULL COMMENT '미디어 길이(초) - 동영상만' AFTER `THUMB_FILE_MGMT_CD`;
