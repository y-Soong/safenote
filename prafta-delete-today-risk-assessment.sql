-- =============================================================================
-- PRAFTA 오늘(2026-06-29) 등록 위험성평가 + 연관 데이터 삭제 스크립트
-- =============================================================================
-- 목적   : INSERT_DATE 가 오늘인 tb_risk_assessment 행과 그 연관 데이터
--          (첨부파일/개선항목/아차사고연결)를 일괄 삭제.
-- 생성   : 실제 스키마(information_schema) + 실데이터 조회 기반.
-- 주의   : 데이터를 영구 삭제합니다. 실행 전 백업하세요.
--
-- [현재 대상 — 조회 시점 기준]
--   tb_risk_assessment 15건 (ASSESSMENT_CD 260600018 ~ 260600032, CMPNY '001' / SITE '00001')
--   tb_file_info       15건 (각 INIT_FILE_MGMT_CD)
--   tb_risk_improvement_item / tb_risk_near_miss_link : 0건(방어적으로 포함)
--
-- [스코프 기준]
--   "오늘"은 아래 @d(=2026-06-29) 의 INSERT_DATE 로 판정한다. 실행 시점에 오늘 추가된
--   위험성평가가 더 있으면 그것도 함께 삭제된다(= "오늘 올린 것 전부"). 날짜를 바꾸려면 @d 수정.
--
-- [실행]
--   mysql -u <user> -p <DB명> < prafta-delete-today-risk-assessment.sql
--   또는 MySQL Workbench 에서 열고 실행.
-- =============================================================================

SET @d = '2026-06-29';

-- -----------------------------------------------------------------------------
-- [사전 검증] 삭제 대상 확인 (선택 실행 권장)
-- -----------------------------------------------------------------------------
SELECT COUNT(*) AS risk_assessment_to_delete
  FROM tb_risk_assessment
 WHERE DATE(INSERT_DATE) = @d;

SELECT ASSESSMENT_CD, CMPNY_CD, SITE_CD, INIT_FILE_MGMT_CD, INSERT_DATE
  FROM tb_risk_assessment
 WHERE DATE(INSERT_DATE) = @d
 ORDER BY ASSESSMENT_CD;

-- -----------------------------------------------------------------------------
-- [삭제] 트랜잭션 시작. 자식 → 파일 → 부모(위험성평가) 순서.
--   FK/세이프모드는 세션 한정으로 잠시 해제. 검증 후 COMMIT, 이상하면 ROLLBACK.
-- -----------------------------------------------------------------------------
START TRANSACTION;
SET FOREIGN_KEY_CHECKS = 0;
SET SQL_SAFE_UPDATES = 0;

-- (1) 개선항목 — 오늘 위험성평가에 속한 행 (현재 0건)
DELETE FROM tb_risk_improvement_item
 WHERE (CMPNY_CD, SITE_CD, ASSESSMENT_CD) IN (
        SELECT CMPNY_CD, SITE_CD, ASSESSMENT_CD
          FROM tb_risk_assessment
         WHERE DATE(INSERT_DATE) = @d
       );

-- (2) 아차사고 연결 — 오늘 위험성평가에 속한 행 (현재 0건)
DELETE FROM tb_risk_near_miss_link
 WHERE (CMPNY_CD, SITE_CD, ASSESSMENT_CD) IN (
        SELECT CMPNY_CD, SITE_CD, ASSESSMENT_CD
          FROM tb_risk_assessment
         WHERE DATE(INSERT_DATE) = @d
       );

-- (3) 첨부파일 — 오늘 위험성평가의 INIT/REVAL 파일코드 (부모 삭제 前에 서브쿼리로 수집)
DELETE FROM tb_file_info
 WHERE (CMPNY_CD, FILE_MGMT_CD) IN (
        SELECT CMPNY_CD, INIT_FILE_MGMT_CD
          FROM tb_risk_assessment
         WHERE DATE(INSERT_DATE) = @d AND INIT_FILE_MGMT_CD IS NOT NULL
        UNION
        SELECT CMPNY_CD, REVAL_FILE_MGMT_CD
          FROM tb_risk_assessment
         WHERE DATE(INSERT_DATE) = @d AND REVAL_FILE_MGMT_CD IS NOT NULL
       );

-- (4) 위험성평가 본체 (마지막)
DELETE FROM tb_risk_assessment
 WHERE DATE(INSERT_DATE) = @d;

SET FOREIGN_KEY_CHECKS = 1;
SET SQL_SAFE_UPDATES = 1;

-- -----------------------------------------------------------------------------
-- [검증] 아래가 0 이면 정상 삭제. 이상 시 COMMIT 대신 ROLLBACK 하세요.
-- -----------------------------------------------------------------------------
SELECT COUNT(*) AS remain_risk_assessment
  FROM tb_risk_assessment
 WHERE DATE(INSERT_DATE) = @d;

-- 확인 후 확정. 문제 시 위 한 줄 대신 ROLLBACK; 을 실행하세요.
COMMIT;
-- ROLLBACK;

-- =============================================================================
-- [주의] tb_file_info 행만 삭제합니다. 디스크/스토리지의 실제 업로드 파일 정리는 별도입니다.
-- =============================================================================
