-- ============================================================================
-- PRAFTA-054-1 — 위험성평가↔아차사고 SRC_* 정리(DROP) + 신규 참조 연계 테이블 생성 (DDL)
-- 작성일: 2026-06-09
-- 적용 환경: MySQL 8.0.42
-- 출처: .claude/requests/web_requests/prafta-054-plan.md §054-1 (사용자 확정 §3·§4 c·e)
-- 선행: prafta-near-miss-deploy.sql (SRC_* 컬럼/IX_TB_NEAR_MISS_SRC 인덱스 운영 적용 전제)
--
-- ★★★ 적용 순서 (사용자 직접 적용 — 본 파일은 두 번째) ★★★
--   1순위) prafta-054-2-cleanup-data.sql              — 데이터 정리(UPDATE/DELETE) [먼저]
--   2순위) 본 파일(prafta-054-1-near-miss-link-ddl.sql) — SRC_* DROP + 신규 테이블 CREATE [나중]
--   ※ 이유: 054-2 의 DELETE 가 SRC_ASSESSMENT_CD 컬럼 조건을 사용한다. 본 파일에서 컬럼을
--           먼저 DROP 하면 054-2 의 DELETE 를 못 쓴다. 반드시 DML(054-2) → DDL(본 파일) 순서.
--   ※ 파일명 번호(054-1 / 054-2)는 053 산출물 네이밍 관례상 부여한 순번이며 적용 순서와 무관.
--     적용 순서는 본 헤더 기준(054-2 먼저).
--
-- 내용:
--   (1) SRC_* 컬럼 + 인덱스 DROP (전환 기능 폐기 → 출처 추적 컬럼 제거)
--   (2) 신규 연계 테이블 tb_risk_near_miss_link CREATE (위험성평가 1 ↔ 완료 아차사고 N, 순수 참조)
--
-- 멱등성: ALTER/CREATE 는 이미 적용된 환경에서 재실행하면 에러. 운영 적용 후 보관용(재실행 금지).
-- 적용 전 부재/존재 확인:
--   SELECT COLUMN_NAME FROM information_schema.columns
--    WHERE table_name='tb_near_miss' AND column_name IN ('SRC_PROCESS_CD','SRC_ASSESSMENT_CD'); -- 2 (DROP 대상)
--   SELECT INDEX_NAME FROM information_schema.statistics
--    WHERE table_name='tb_near_miss' AND index_name='IX_TB_NEAR_MISS_SRC';                      -- 1 (DROP 대상)
--   SELECT TABLE_NAME FROM information_schema.tables WHERE table_name='tb_risk_near_miss_link';  -- 0 (CREATE 대상)
-- ============================================================================

-- ----------------------------------------------------------------------------
-- (1) SRC_* 컬럼 + 인덱스 DROP
--     인덱스 IX_TB_NEAR_MISS_SRC 가 SRC_* 컬럼을 참조하므로 인덱스를 먼저 DROP 한다.
--     ⚠️ 반드시 prafta-054-2-cleanup-data.sql (DELETE) 적용 후 실행할 것.
-- ----------------------------------------------------------------------------
ALTER TABLE `tb_near_miss`
    DROP INDEX `IX_TB_NEAR_MISS_SRC`,
    DROP COLUMN `SRC_PROCESS_CD`,
    DROP COLUMN `SRC_ASSESSMENT_CD`;

-- ----------------------------------------------------------------------------
-- (2) 신규 연계 테이블: tb_risk_near_miss_link
--     위험성평가 1건 ↔ 완료(SYS063='400') 아차사고 N건의 순수 "참조" 연계.
--     아차사고 데이터를 평가로 복사하지 않으며, 키만 보유한다.
--     - 평가 키: (CMPNY_CD, SITE_CD, PROCESS_CD, ASSESSMENT_CD)
--     - 아차사고 키: (CMPNY_CD, SITE_CD, NEAR_MISS_ID)  ※ CMPNY_CD/SITE_CD 공유
--     해제는 soft delete(USE_YN='N'). 재연결은 서비스에서 USE_YN='Y' upsert.
--     물리 FK 미설정(프로젝트 관례) — 무결성은 서비스 INSERT 시 검증.
-- ----------------------------------------------------------------------------
CREATE TABLE `tb_risk_near_miss_link` (
    `CMPNY_CD`        varchar(50)  NOT NULL COMMENT '회사코드',
    `SITE_CD`         varchar(50)  NOT NULL COMMENT '사업장코드(평가/아차사고 공통 스코프)',
    `PROCESS_CD`      varchar(10)  NOT NULL COMMENT '위험성평가 공정코드[COM002] (tb_risk_assessment.PROCESS_CD)',
    `ASSESSMENT_CD`   varchar(10)  NOT NULL COMMENT '위험성평가 코드 (tb_risk_assessment.ASSESSMENT_CD)',
    `NEAR_MISS_ID`    varchar(20)  NOT NULL COMMENT '참조 아차사고 ID (tb_near_miss.NEAR_MISS_ID, 완료[SYS063=400] 건만 연결)',
    `USE_YN`          varchar(2)   NOT NULL DEFAULT 'Y' COMMENT '사용여부[컬럼상수] Y:연결 N:해제(soft delete)',
    `INSERT_NO`       varchar(50)           DEFAULT 'SYSTEM' COMMENT '연결자(tb_user.USER_CD)',
    `INSERT_DATE`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '연결일시',
    `UPDATE_NO`       varchar(50)           DEFAULT NULL COMMENT '수정자(tb_user.USER_CD)',
    `UPDATE_DATE`     datetime              DEFAULT NULL COMMENT '수정일시',
    PRIMARY KEY (`CMPNY_CD`, `SITE_CD`, `PROCESS_CD`, `ASSESSMENT_CD`, `NEAR_MISS_ID`),
    KEY `IX_TB_RISK_NM_LINK_ASSESS` (`CMPNY_CD`, `SITE_CD`, `PROCESS_CD`, `ASSESSMENT_CD`),
    KEY `IX_TB_RISK_NM_LINK_NM` (`CMPNY_CD`, `SITE_CD`, `NEAR_MISS_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='위험성평가-아차사고 참조 연계(1:N, 평가→완료 아차사고)';

-- ============================================================================
-- 끝. 적용 후 검증:
--   SELECT COUNT(*) FROM information_schema.columns
--    WHERE table_name='tb_near_miss' AND column_name IN ('SRC_PROCESS_CD','SRC_ASSESSMENT_CD'); -- 0
--   SELECT COUNT(*) FROM information_schema.statistics
--    WHERE table_name='tb_near_miss' AND index_name='IX_TB_NEAR_MISS_SRC';                      -- 0
--   SELECT COUNT(*) FROM information_schema.tables WHERE table_name='tb_risk_near_miss_link';    -- 1
-- ============================================================================
