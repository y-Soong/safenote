-- ============================================================================
-- PRAFTA-024 — 연차 부여 정책(Baim_07) 사용 단위 단일화
-- 작성일: 2026-05-24
-- 적용 환경: MySQL 8.0.42
-- 참조: .claude/requests/prafta-024.md
--
-- 변경 요약
--  1) tb_leave_usage_policy: 사용 단위를 다중(ALLOW_* 5컬럼) → 단일(USAGE_UNIT) 로 전환.
--     - USAGE_UNIT 신설: FULL_DAY / HALF_DAY / HOUR_2 / HOUR_1 / MIN_30 중 1개.
--     - 기존 ALLOW_FULL_DAY/ALLOW_HALF_DAY/ALLOW_HOUR_2/ALLOW_HOUR_1/ALLOW_MIN_30 은
--       애플리케이션에서 더 이상 사용하지 않음(폐기). 과거 이력 보존을 위해 컬럼은 잔존시킨다.
--  2) MAX_DAILY_REQUEST(같은 날 다중 신청) 제거 — 화면/저장 로직에서 삭제, 컬럼 DROP.
--
-- 결정 근거(prafta-024 채팅):
--  - (1a) 단일 단위는 신규 USAGE_UNIT 컬럼 하나로 저장, 기존 ALLOW_* 는 미사용 잔존.
--  - (2b) AXIS4_PRORATE_ROUNDING='HALF_DAY'(0.5일 단위 절사) 면 USAGE_UNIT=HALF_DAY 강제.
--  - (3a) MAX_DAILY_REQUEST 컬럼 DROP.
--
-- 멱등성: ALTER ADD/DROP COLUMN 은 중복 실행 시 에러(8.0은 IF [NOT] EXISTS 미지원).
--         이미 반영된 환경에서는 해당 구문을 건너뛸 것. 운영 적용 후 본 파일 보관용.
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1) USAGE_UNIT 신설 (단일 사용 단위)
--    ALLOW_MIN_30 다음 위치에 추가. 기본값 FULL_DAY(1일).
-- ----------------------------------------------------------------------------
ALTER TABLE tb_leave_usage_policy
    ADD COLUMN USAGE_UNIT varchar(20) NOT NULL DEFAULT 'FULL_DAY'
        COMMENT '회사 허용 사용 단위 (단일): FULL_DAY/HALF_DAY/HOUR_2/HOUR_1/MIN_30'
        AFTER ALLOW_MIN_30;

-- ----------------------------------------------------------------------------
-- 2) 기존 행 백필
--    우선순위: AXIS4=HALF_DAY(0.5일 절사) → HALF_DAY 강제(2b).
--    그 외에는 명시적으로 켜져 있던 가장 작은 단위를 단일 단위로 승계, 없으면 FULL_DAY.
-- ----------------------------------------------------------------------------
UPDATE tb_leave_usage_policy U
  JOIN tb_leave_policy P
    ON P.POLICY_SEQ = U.POLICY_SEQ
   SET U.USAGE_UNIT = CASE
                        WHEN P.AXIS4_PRORATE_ROUNDING = 'HALF_DAY' THEN 'HALF_DAY'
                        WHEN U.ALLOW_MIN_30 = 'Y'                  THEN 'MIN_30'
                        WHEN U.ALLOW_HOUR_1 = 'Y'                  THEN 'HOUR_1'
                        WHEN U.ALLOW_HOUR_2 = 'Y'                  THEN 'HOUR_2'
                        ELSE 'FULL_DAY'
                      END;

-- ----------------------------------------------------------------------------
-- 3) MAX_DAILY_REQUEST(같은 날 다중 신청) 제거
-- ----------------------------------------------------------------------------
ALTER TABLE tb_leave_usage_policy
    DROP COLUMN MAX_DAILY_REQUEST;
