-- ============================================================================
-- prafta-058-5-risk-improvement-item-ddl.sql
-- 위험성평가 개선항목 N건 종속 테이블 신규 생성 (DDL)
--   T6-P2 / findings §E D2. 항목 14.4 지속평가대상 관리(개선항목 N건).
--
-- 위험성평가 1건(PK 4축: CMPNY_CD/SITE_CD/PROCESS_CD/ASSESSMENT_CD)에 종속하는
-- 개선항목 N건. 현 tb_risk_assessment 는 REVAL_* 단일 세트(개선 후 1건)만 보유하므로
-- N건 이력을 담을 종속 테이블이 필요하다. "개선완료"(005→003) 시 개선항목은 보존하고
-- 최종 개선 후 위험도를 tb_risk_assessment.REVAL_* 로 승격한다.
--
-- 물리 FK 미설정(프로젝트 관례) — 무결성은 서비스 INSERT 시 평가 존재 검증으로 보장.
-- 적용 순서 (058 계열): P3 nearmiss 058-1~3 → 058-4(SYS011 005) → (본) 058-5
-- ============================================================================

CREATE TABLE `tb_risk_improvement_item` (
    `CMPNY_CD`          varchar(50)  NOT NULL COMMENT '회사코드',
    `SITE_CD`           varchar(50)  NOT NULL COMMENT '사업장코드(평가 스코프)',
    `PROCESS_CD`        varchar(10)  NOT NULL COMMENT '공정코드[COM002] (tb_risk_assessment.PROCESS_CD)',
    `ASSESSMENT_CD`     varchar(10)  NOT NULL COMMENT '위험성평가 코드 (tb_risk_assessment.ASSESSMENT_CD)',
    `IMPROVEMENT_SEQ`   int          NOT NULL COMMENT '개선항목 순번(평가건 내 1부터 증가)',
    `IMPROVE_DATE`      varchar(8)            DEFAULT NULL COMMENT '개선일자(YYYYMMDD)',
    `IMPROVE_DESC`      varchar(500)          DEFAULT NULL COMMENT '개선내용',
    `FILE_MGMT_CD`      varchar(50)           DEFAULT NULL COMMENT '개선사진(tb_file_info.FILE_MGMT_CD, FILE_TYPE[SYS010] 002:위험성평가)',
    `LIKELIHOOD_SCORE`  int                   DEFAULT NULL COMMENT '개선 후 발생빈도(1~5)',
    `SEVERITY_SCORE`    int                   DEFAULT NULL COMMENT '개선 후 중대성(1~4)',
    `RISK_LV`           varchar(10)           DEFAULT NULL COMMENT '개선 후 위험도LEVEL(빈도×강도, 6단계 매핑은 표시단)',
    `USE_YN`            varchar(2)   NOT NULL DEFAULT 'Y' COMMENT '사용여부[컬럼상수] Y:사용 N:삭제(soft delete)',
    `INSERT_NO`         varchar(50)           DEFAULT 'SYSTEM' COMMENT '입력자(tb_user.USER_CD)',
    `INSERT_DATE`       datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
    `UPDATE_NO`         varchar(50)           DEFAULT NULL COMMENT '수정자(tb_user.USER_CD)',
    `UPDATE_DATE`       datetime              DEFAULT NULL COMMENT '수정일시',
    PRIMARY KEY (`CMPNY_CD`, `SITE_CD`, `PROCESS_CD`, `ASSESSMENT_CD`, `IMPROVEMENT_SEQ`),
    KEY `IX_TB_RISK_IMPR_ITEM_ASSESS` (`CMPNY_CD`, `SITE_CD`, `PROCESS_CD`, `ASSESSMENT_CD`, `USE_YN`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='위험성평가 개선항목(1:N, 지속평가대상 개선 N건 이력)';
