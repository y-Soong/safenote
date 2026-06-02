-- ============================================================================
-- PRAFTA-031 — 관리자 수동 부여 연차 회수(soft cancel) 컬럼 추가
-- 작성일: 2026-05-26
-- 적용 환경: MySQL 8.0.42
-- 참조: .claude/requests/prafta-031.md, 정책서 attd/08-leave.md §8.5.7 / §8.5.8
--
-- 변경 요약
--  1) tb_user_leave_grant 에 회수(취소) 메타 컬럼 3종 추가.
--     - 회수 = soft cancel: STATUS='CANCELED' 로만 전환하고 행을 보존한다(§8.5.8 #2/#3/#5).
--     - USED_DAYS 는 회수 시에도 절대 갱신하지 않는다(사용분 보존, 사후차감 금지).
--     - 기존 RESET_ALL 의 CANCELED 와 동일 상태값을 공유하되, 회수 메타(사유/일시/수행자)로 출처를 식별한다.
--
-- 멱등성: ALTER ADD COLUMN 중복 실행 시 에러. 이미 반영된 환경에서는 건너뛸 것.
-- ============================================================================

ALTER TABLE `tb_user_leave_grant`
    ADD COLUMN `CANCEL_REASON` varchar(500) NULL COMMENT '회수(취소) 사유' AFTER `EXPIRE_DATE`,
    ADD COLUMN `CANCEL_DATE`   datetime     NULL COMMENT '회수(취소) 일시' AFTER `CANCEL_REASON`,
    ADD COLUMN `CANCEL_BY`     varchar(50)  NULL COMMENT '회수 수행자 (USER_CD)' AFTER `CANCEL_DATE`;
