-- ============================================================================
-- PRAFTA-032 — 입사일 변경 "처리방식" 폐기 및 수동 연차 조정 전환 컬럼 추가
-- 작성일: 2026-05-27
-- 적용 환경: MySQL 8.0.42
-- 참조: .claude/requests/prafta-032-decisions.md (D5/D7), ref/prafta-032-참고.md §8
--
-- 변경 요약
--  1) tb_user_hire_date_history 에 수동 연차 조정 추적 컬럼 3종 추가.
--     - 입사일 변경 시 처리방식 자동계산(KEEP_*/RESET_ALL)을 폐기하고, 관리자가 직접 입력한
--       목표 법정 부여량과의 차액을 추가/회수한다(D1~D5). 본 컬럼은 그 조정의 전/후 총량과
--       회수 사유를 노무 감사용으로 보존한다.
--     - HANDLING_TYPE 은 NOT NULL 제약을 유지한다. 신규 이력은 'MANUAL' 로 기록하며,
--       기존 이력 값(KEEP_AND_BACKFILL/KEEP_AND_APPLY_NEW/RESET_ALL)은 그대로 보존한다(감사 추적).
--     - WITHDRAW_REASON 은 회수(차액<0) 발생 시에만 채운다(요청서 TEXT 제안이나 prafta CANCEL_REASON
--       과 동일한 varchar(500)로 통일).
--
-- 멱등성: ALTER ADD COLUMN 중복 실행 시 에러. 이미 반영된 환경에서는 건너뛸 것.
-- ============================================================================

ALTER TABLE `tb_user_hire_date_history`
    ADD COLUMN `OLD_GRANT_TOTAL`  decimal(5,1) NULL COMMENT '변경 전 법정 부여 총량 (수동 조정 추적, MANUAL 한정)' AFTER `AFFECTED_GRANT_SNAPSHOT`,
    ADD COLUMN `NEW_GRANT_TOTAL`  decimal(5,1) NULL COMMENT '변경 후 목표 법정 부여 총량 (수동 조정 추적, MANUAL 한정)' AFTER `OLD_GRANT_TOTAL`,
    ADD COLUMN `WITHDRAW_REASON`  varchar(500) NULL COMMENT '회수 사유 (차액<0 회수 발생 시 필수, MANUAL 한정)' AFTER `NEW_GRANT_TOTAL`;
