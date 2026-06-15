-- ============================================================================
-- prafta-com-008-fix1: tb_user_work_plan.GEN_SOURCE 길이 결함 수정 (QA 세션2 결함 #1)
--
-- 결함: GEN_SOURCE varchar(10) < 코드값 'DEFAULT_SCH'(11자, SYS074)
--   → 기본근무 즉시생성(set-default-sch)/연 1/1 배치/ensureWorkPlanDay(촉진 등록 폴백)
--     INSERT 전부 "Data too long for column 'GEN_SOURCE'" 실패.
-- 수정: varchar(20) 확장 (SYS074 코드 최장값 + 여유. 기본값/COMMENT 유지).
-- 적용: 개발/운영 공통 선적용 필수.
-- ============================================================================

ALTER TABLE tb_user_work_plan
    MODIFY COLUMN GEN_SOURCE varchar(20) NOT NULL DEFAULT 'MANUAL'
        COMMENT '생성출처[SYS074] MANUAL:수동 / DEFAULT_SCH:기본근무자동 / SHIFT:교대자동 / LEAVE:연차(레거시)';

-- 검증:
-- SELECT COLUMN_TYPE FROM information_schema.columns
--  WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='tb_user_work_plan' AND COLUMN_NAME='GEN_SOURCE';
-- → varchar(20)
