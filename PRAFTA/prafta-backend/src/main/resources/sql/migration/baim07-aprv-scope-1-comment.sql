-- ============================================================================
-- Baim_07 "연차·월차·근속가산 신청 결재" 적용범위 축소에 따른 컬럼 코멘트 정정
--   - 2026-09-04 사용자 확정: tb_leave_policy.APRV_USE_YN 이 지배하는 대상은
--     법정 3종(SYS_ANNUAL 연차 / SYS_MONTHLY 월차 / SYS_TENURE_BONUS 근속가산 연차)이다.
--     종전 코드는 SYSTEM_YN='Y' 인 시스템 시드 7종 전체가 이 값을 탔고(코멘트도 "법정연차"),
--     SYS_PREGRANT / SYS_PROMOTION / SYS_BIRTHDAY / SYS_CAREER 는 이제 타입별
--     tb_leave_type_mgmt.APRV_USE_YN 을 따른다.
--   - 데이터 변경 없음(코멘트만). 컬럼 정의(char(1) NOT NULL DEFAULT 'N')는 그대로 유지한다.
--   - 개발/운영 양 DB 에 동일 적용한다(DB 마이그레이션 원칙).
--   - 운영은 사용자가 Workbench 로 직접 실행한다.
-- ============================================================================

-- 적용 전 확인(현재 코멘트 조회)
-- SELECT COLUMN_NAME, COLUMN_TYPE, COLUMN_DEFAULT, COLUMN_COMMENT
--   FROM INFORMATION_SCHEMA.COLUMNS
--  WHERE TABLE_SCHEMA = DATABASE()
--    AND TABLE_NAME   = 'tb_leave_policy'
--    AND COLUMN_NAME  = 'APRV_USE_YN';

ALTER TABLE tb_leave_policy
    MODIFY COLUMN APRV_USE_YN CHAR(1) NOT NULL DEFAULT 'N'
    COMMENT '법정 3종(연차/월차/근속가산) 신청 결재 여부 (Y: 결재라인, N: 즉시확정). 그 외 휴가는 tb_leave_type_mgmt.APRV_USE_YN';

-- 적용 후 검증(코멘트가 바뀌고 타입/기본값이 그대로인지)
-- SELECT COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE, COLUMN_DEFAULT, COLUMN_COMMENT
--   FROM INFORMATION_SCHEMA.COLUMNS
--  WHERE TABLE_SCHEMA = DATABASE()
--    AND TABLE_NAME   = 'tb_leave_policy'
--    AND COLUMN_NAME  = 'APRV_USE_YN';
