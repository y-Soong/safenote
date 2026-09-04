-- ============================================================================
-- 아차사고 처리상태 컬럼 코멘트 정정 (SYS063 재번호 D4 반영 누락분)
--   - tb_near_miss.REPORT_STATUS_CD 코멘트가 구 체계로 남아 있다:
--       (구) 100:접수 200:검토중 300:조치중 400:완료 900:반려
--     실제 SYS063 코드값(tb_syst_val_d 실측, 2026-09-05):
--       100:접수 / 200:조치중 / 300:완료 / 400:미처리대상   (900 없음)
--   - 코드는 이미 신 체계로 동작 중이며(서비스 상수·매퍼), 코멘트만 과거에 머물러 있다.
--     스키마만 읽고 작업하면 "200=검토중" 으로 오독하게 되므로 정정한다.
--   - 데이터 변경 없음(코멘트만). 컬럼 정의(varchar(10) NOT NULL)는 그대로 유지한다.
--   - 개발/운영 양 DB 에 동일 적용한다(DB 마이그레이션 원칙).
--   - 운영은 사용자가 Workbench 로 직접 실행한다.
--
-- 참고: REJECT_REASON 컬럼도 "반려 사유" 로 남아 있으나, 재번호 이후 실제 용도는
--       "미처리대상(400) 사유" 다. 같은 맥락이라 함께 정정한다.
-- ============================================================================

-- 적용 전 확인
-- SELECT COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE, COLUMN_COMMENT
--   FROM INFORMATION_SCHEMA.COLUMNS
--  WHERE TABLE_SCHEMA = DATABASE()
--    AND TABLE_NAME   = 'tb_near_miss'
--    AND COLUMN_NAME IN ('REPORT_STATUS_CD', 'REJECT_REASON');

ALTER TABLE tb_near_miss
    MODIFY COLUMN REPORT_STATUS_CD VARCHAR(10) NOT NULL
    COMMENT '처리상태[SYS063] 100:접수 200:조치중 300:완료 400:미처리대상';

ALTER TABLE tb_near_miss
    MODIFY COLUMN REJECT_REASON VARCHAR(500) NULL
    COMMENT '미처리 사유(처리상태 400 미처리대상 시 기록, 추정원인 CAUSE_DESC 와 분리)';

-- 적용 후 검증(코멘트가 바뀌고 타입/NULL 여부가 그대로인지)
-- SELECT COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE, COLUMN_COMMENT
--   FROM INFORMATION_SCHEMA.COLUMNS
--  WHERE TABLE_SCHEMA = DATABASE()
--    AND TABLE_NAME   = 'tb_near_miss'
--    AND COLUMN_NAME IN ('REPORT_STATUS_CD', 'REJECT_REASON');
