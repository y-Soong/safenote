-- ============================================================================
-- PRAFTA-021 — 직접 연차 차감(근무계획 적용) 이중차감 방지 유니크 가드
-- 작성일: 2026-05-23
-- 적용 환경: MySQL 8.0.42
-- 참조: .claude/requests/prafta-021.md item 3 / 보안 리뷰 prafta-021-003 (TOCTOU 이중차감)
--
-- 결재 없는 직접 차감(tb_user_leave_use: REQ_ID IS NULL AND LEAVE_STATUS='CONFIRMED') 건에 대해
-- (회사 + 직원 + 일자 + 연차코드) 유니크를 강제하여, 동시 저장(check-then-insert TOCTOU) 시
-- DB가 두 번째 INSERT를 거부하게 한다. 결재 경유/취소건은 키가 NULL이라 제약 대상 아님
-- (MySQL 유니크 인덱스는 NULL 다중 허용). 취소(CANCELLED) 시 키가 NULL로 재계산되어 재사용 가능.
--
-- 멱등성: 컬럼/인덱스 중복 생성 시 에러. 이미 반영된 환경에서는 건너뛸 것.
-- ============================================================================

ALTER TABLE `tb_user_leave_use`
    ADD COLUMN `DIRECT_USE_KEY` varchar(80)
        GENERATED ALWAYS AS (
            CASE WHEN `REQ_ID` IS NULL AND `LEAVE_STATUS` = 'CONFIRMED'
                 THEN CONCAT(`USER_CD`, '|', `START_DATE`, '|', `LEAVE_CD`)
                 ELSE NULL END
        ) STORED COMMENT '직접 차감(결재 없음) 멱등 키 — 결재경유/취소건은 NULL',
    ADD UNIQUE INDEX `UK_LEAVE_USE_DIRECT` (`CMPNY_CD`, `DIRECT_USE_KEY`);
