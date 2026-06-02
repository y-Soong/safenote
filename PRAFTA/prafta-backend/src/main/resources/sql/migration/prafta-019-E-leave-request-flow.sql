-- ============================================================================
-- PRAFTA-019-E — 연차 신청·결재 흐름 본체 (선행 DDL)
-- 작성일: 2026-05-23
-- 적용 환경: MySQL 8.0.42
-- 참조: .claude/requests/prafta-019-E-leave-request-flow.md, prafta-019-plan.md
--
-- 변경 요약
--  1) tb_leave_policy.APRV_USE_YN — 법정연차 신청 결재 여부 (결정 #2). 회사정의는 기존 tb_leave_type_mgmt.APRV_USE_YN.
--  2) tb_sch_mgmt / tb_sch_mgmt_hist 휴게 시작/종료 시각 4컬럼 — 휴게 가로지름 판정(§8.5.9).
--     기존은 휴게를 분(FST_SCH_BRK_MIN)으로만 저장 → 시각 추가. NULL이면 가로지름 검증 skip(보수 처리).
--  3) tb_user_attd_req.LEAVE_DAYS decimal(3,1) → decimal(8,5) — 시간차 동적 환산 차감 수용.
--
-- 멱등성: ALTER ADD COLUMN 중복 실행 시 에러. 이미 반영된 환경에서는 건너뛸 것.
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1) 법정연차 결재 여부
-- ----------------------------------------------------------------------------
ALTER TABLE tb_leave_policy
    ADD COLUMN APRV_USE_YN char(1) NOT NULL DEFAULT 'N'
        COMMENT '법정연차 신청 결재 여부 (Y: 결재라인, N: 즉시확정)' AFTER AXIS7_USE_PROMOTION;

-- ----------------------------------------------------------------------------
-- 2) 스케줄 휴게 시작/종료 시각 (HHMM)
-- ----------------------------------------------------------------------------
ALTER TABLE tb_sch_mgmt
    ADD COLUMN FST_BRK_STR_TIME varchar(4) NULL COMMENT '1구간 휴게 시작(HHMM)' AFTER FST_SCH_BRK_MIN,
    ADD COLUMN FST_BRK_END_TIME varchar(4) NULL COMMENT '1구간 휴게 종료(HHMM)' AFTER FST_BRK_STR_TIME,
    ADD COLUMN SEC_BRK_STR_TIME varchar(4) NULL COMMENT '2구간 휴게 시작(HHMM)' AFTER SEC_SCH_BRK_MIN,
    ADD COLUMN SEC_BRK_END_TIME varchar(4) NULL COMMENT '2구간 휴게 종료(HHMM)' AFTER SEC_BRK_STR_TIME;

ALTER TABLE tb_sch_mgmt_hist
    ADD COLUMN FST_BRK_STR_TIME varchar(4) NULL COMMENT '1구간 휴게 시작(HHMM)' AFTER FST_SCH_BRK_MIN,
    ADD COLUMN FST_BRK_END_TIME varchar(4) NULL COMMENT '1구간 휴게 종료(HHMM)' AFTER FST_BRK_STR_TIME,
    ADD COLUMN SEC_BRK_STR_TIME varchar(4) NULL COMMENT '2구간 휴게 시작(HHMM)' AFTER SEC_SCH_BRK_MIN,
    ADD COLUMN SEC_BRK_END_TIME varchar(4) NULL COMMENT '2구간 휴게 종료(HHMM)' AFTER SEC_BRK_STR_TIME;

-- ----------------------------------------------------------------------------
-- 3) 요청 사용일수 정밀도 상향 (시간차 환산)
-- ----------------------------------------------------------------------------
ALTER TABLE tb_user_attd_req
    MODIFY COLUMN LEAVE_DAYS decimal(8,5) NULL COMMENT '사용 일수(시간차 환산)';
