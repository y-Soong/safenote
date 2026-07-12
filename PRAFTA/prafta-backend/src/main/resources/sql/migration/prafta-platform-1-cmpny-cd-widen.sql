-- ============================================================================
-- PRAFTA-PLATFORM-1 — CMPNY_CD 컬럼폭 정규화 (varchar(10) → varchar(50))
-- 작성일: 2026-06-28
-- 적용 환경: MySQL 8.0.42
--
-- 배경
--   신규 회사 등록 시 CMPNY_CD 를 추측 불가한 20자 랜덤값으로 발급한다(테넌트 enumeration 방지).
--   전체 81개 CMPNY_CD 컬럼 중 79개는 이미 varchar(50) 이나, 아래 2개만 varchar(10) 이라
--   20자 회사코드가 오버플로된다. 본 마이그로 50 으로 정규화한다(나머지와 일치).
--   ※ 채번(USER_CD/SITE_CD/NODE_CD)은 CMPNY_CD 를 임베드하지 않으므로(FNC_CMM_SEQ_NEXTVAL
--     은 시퀀스만 반환) 생성 코드 컬럼은 영향 없음. 폭 문제는 이 2개 컬럼뿐.
--
-- 변경 요약
--   1) tb_site_node.CMPNY_CD     varchar(10) → varchar(50)
--   2) tb_tbm_edu_mtrl.CMPNY_CD  varchar(10) → varchar(50)
--
-- 적용 전 현황 확인 (운영 적용 직전 권장):
--   SELECT TABLE_NAME, CHARACTER_MAXIMUM_LENGTH
--     FROM INFORMATION_SCHEMA.COLUMNS
--    WHERE TABLE_SCHEMA = DATABASE() AND COLUMN_NAME = 'CMPNY_CD'
--      AND CHARACTER_MAXIMUM_LENGTH < 50;
--   -- 결과가 위 2개 테이블만 나와야 한다(다른 테이블이 더 있으면 본 파일에 추가).
--
-- 안전성: 확장(widen) 전용 — 기존 데이터/인덱스 무손실. NOT NULL 여부는 기존 정의 유지.
-- 멱등성: 적용 후 보관용(재실행 금지). 적용 전 상태 확인 후 수행.
-- 운영 적용: 사용자 수동(read-only MCP). 본 파일은 작성만, DB 직접 적용 금지.
-- ============================================================================

-- 1) tb_site_node.CMPNY_CD 확장
ALTER TABLE `tb_site_node`
    MODIFY COLUMN `CMPNY_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '회사코드';

-- 2) tb_tbm_edu_mtrl.CMPNY_CD 확장
ALTER TABLE `tb_tbm_edu_mtrl`
    MODIFY COLUMN `CMPNY_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '회사코드';

-- ============================================================================
-- 롤백: 폭 축소는 데이터 손실 위험이 있어 권장하지 않음(필요 시 데이터 확인 후 수동).
-- ============================================================================
