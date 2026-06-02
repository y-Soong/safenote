-- ============================================================================
-- PRAFTA-019-A — 시간차 연차 사용단위 도입 (prafta-017 사용단위 로직 보완)
-- 작성일: 2026-05-23
-- 적용 환경: MySQL 8.0.42
-- 참조: .claude/requests/prafta-019-A-leave-time-unit.md, prafta-019-plan.md
--
-- 변경 요약
--  1) SYS025 재정렬(B안): 01~05 → 00~04 (1일/반차/시간차2h/시간차1h/시간차30분)
--     - 기존 참조값(tb_leave_type_mgmt.USE_UNIT_TYPE, tb_user_leave_use.USE_UNIT_TYPE)을
--       -1 시프트로 선(先) 리매핑한 뒤 상세코드 재정의.
--  2) tb_leave_usage_policy 재모델: ALLOW_QUARTER_DAY/ALLOW_HOURLY 제거,
--     ALLOW_HOUR_2(02)/ALLOW_HOUR_1(03)/ALLOW_MIN_30(04) 신설.
--  3) 정밀도 상향: tb_user_leave_use.LEAVE_DAYS, tb_user_leave_grant.USED_DAYS → decimal(8,5)
--     (GRANT_DAYS는 변경하지 않음 — 부여는 일 단위)
--  4) 연차타입 화면 정리: tb_leave_type_mgmt.APRV_STEP_CNT / HR_FINAL_APRV_YN 제거
--
-- 멱등성: ALTER ADD/DROP COLUMN은 중복 실행 시 에러(8.0은 IF [NOT] EXISTS 미지원).
--         이미 반영된 환경에서는 해당 구문을 건너뛸 것. 운영 적용 후 본 파일 보관용.
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1) SYS025 재정렬 (01~05 → 00~04)
--    참조값 -1 시프트를 먼저 적용(상세코드 PK 변경 전 데이터 정합 유지).
-- ----------------------------------------------------------------------------

-- 1-1) 연차타입 마스터의 사용단위 코드 리매핑
UPDATE tb_leave_type_mgmt
   SET USE_UNIT_TYPE = CASE USE_UNIT_TYPE
                         WHEN '01' THEN '00'
                         WHEN '02' THEN '01'
                         WHEN '03' THEN '02'
                         WHEN '04' THEN '03'
                         WHEN '05' THEN '04'
                         ELSE USE_UNIT_TYPE
                       END
 WHERE USE_UNIT_TYPE IN ('01','02','03','04','05');

-- 1-2) 사용 이력의 사용단위 코드 리매핑 (착수 시 0건 확인 — 안전 차원 동일 적용)
UPDATE tb_user_leave_use
   SET USE_UNIT_TYPE = CASE USE_UNIT_TYPE
                         WHEN '01' THEN '00'
                         WHEN '02' THEN '01'
                         WHEN '03' THEN '02'
                         WHEN '04' THEN '03'
                         WHEN '05' THEN '04'
                         ELSE USE_UNIT_TYPE
                       END
 WHERE USE_UNIT_TYPE IN ('01','02','03','04','05');

-- 1-3) SYS025 상세코드 재정의 (기존 삭제 후 00~04 재등록)
DELETE FROM tb_syst_val_d WHERE SYST_VAL_CD = 'SYS025';

INSERT INTO tb_syst_val_d (SYST_VAL_CD, SYST_VAL_D_CD, SYST_VAL_D_NM, SORT_IDX, USE_YN, INSERT_NO, INSERT_DATE) VALUES
   ('SYS025', '00', '1일',          1, 'Y', 'SYSTEM', NOW())
 , ('SYS025', '01', '반차',         2, 'Y', 'SYSTEM', NOW())
 , ('SYS025', '02', '시간차(2시간)', 3, 'Y', 'SYSTEM', NOW())
 , ('SYS025', '03', '시간차(1시간)', 4, 'Y', 'SYSTEM', NOW())
 , ('SYS025', '04', '시간차(30분)',  5, 'Y', 'SYSTEM', NOW());

-- ----------------------------------------------------------------------------
-- 2) tb_leave_usage_policy 재모델
--    ALLOW_FULL_DAY(00, 항상 Y) / ALLOW_HALF_DAY(01) / MAX_DAILY_REQUEST 유지.
--    ALLOW_QUARTER_DAY / ALLOW_HOURLY 제거 → 시간차 토글 3종 신설.
-- ----------------------------------------------------------------------------
ALTER TABLE tb_leave_usage_policy
    DROP COLUMN ALLOW_QUARTER_DAY,
    DROP COLUMN ALLOW_HOURLY,
    ADD COLUMN ALLOW_HOUR_2 char(1) NOT NULL DEFAULT 'N' COMMENT '시간차 2시간 허용 (SYS025-02)' AFTER ALLOW_HALF_DAY,
    ADD COLUMN ALLOW_HOUR_1 char(1) NOT NULL DEFAULT 'N' COMMENT '시간차 1시간 허용 (SYS025-03)' AFTER ALLOW_HOUR_2,
    ADD COLUMN ALLOW_MIN_30 char(1) NOT NULL DEFAULT 'N' COMMENT '시간차 30분 허용 (SYS025-04)' AFTER ALLOW_HOUR_1;

-- ----------------------------------------------------------------------------
-- 3) 정밀도 상향 (시간차 동적 환산 차감 수용)
-- ----------------------------------------------------------------------------
ALTER TABLE tb_user_leave_use
    MODIFY COLUMN LEAVE_DAYS decimal(8,5) NOT NULL COMMENT '사용 일수 (시간차 동적 환산)';

ALTER TABLE tb_user_leave_grant
    MODIFY COLUMN USED_DAYS decimal(8,5) NOT NULL DEFAULT 0.00000 COMMENT '사용 일수 캐시 (tb_user_leave_use 합계와 동기화)';

-- ----------------------------------------------------------------------------
-- 4) 연차타입 화면 정리 (결정 #3 — 결재 단계 수 / 인사팀 최종 승인 제거, APRV_USE_YN 유지)
-- ----------------------------------------------------------------------------
ALTER TABLE tb_leave_type_mgmt
    DROP COLUMN APRV_STEP_CNT,
    DROP COLUMN HR_FINAL_APRV_YN;
