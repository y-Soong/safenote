-- ============================================================================
-- PRAFTA-044 후속2 (FU2) — 관리자 부여 연차 "사용 가능 기간"(SYS026='03' 기간설정)
--   FROM/TO 컬럼을 YYYYMMDD(8자) 절대 날짜로 정합
-- 작성일: 2026-06-03
-- 적용 환경: MySQL 8.0.42
-- 참조: prafta-044 FU2 작업 지시 (사용자 확정)
--       .claude/context/policies/attd/08-leave.md §8.1.1 (사용가능기간 속성)
--
-- 변경 요약
--   tb_leave_type_mgmt.ADMIN_AVAIL_FROM_DT / ADMIN_AVAIL_TO_DT 가 현재 varchar(6) 라
--   화면(YYYY-MM-DD 풀날짜 → 전송 시 YYYYMMDD 8자)과 길이가 맞지 않아 저장이 막힌다.
--   두 컬럼을 varchar(8) 로 확장하여 절대 날짜 범위(예: 20260101~20261231)를 수용한다.
--
--   ※ 사용자 신청 타입('01')의 AVAIL_FROM_DT / AVAIL_TO_DT(varchar(4), MMDD)는
--     별개 설계(매년 반복되는 월일)이므로 본 마이그 대상이 아니며 건드리지 않는다.
--
-- 데이터 손실 위험: 없음.
--   기존 ADMIN_AVAIL_FROM_DT / ADMIN_AVAIL_TO_DT 데이터는 전부 NULL 이며(이번 FU 이전에
--   varchar(6) 와 화면 포맷 불일치로 저장 자체가 안 됐음), varchar(6)→varchar(8) 확장은
--   기존 값을 절단하지 않는 안전한 변경이다.
--
-- 적용 전 현재 상태 확인 (운영 적용 직전 권장):
--   SHOW COLUMNS FROM tb_leave_type_mgmt LIKE 'ADMIN_AVAIL_FROM_DT';  -- Type=varchar(6) 확인
--   SHOW COLUMNS FROM tb_leave_type_mgmt LIKE 'ADMIN_AVAIL_TO_DT';    -- Type=varchar(6) 확인
--   -- 데이터 전부 NULL 인지 확인(손실 없음 보장)
--   SELECT COUNT(*) FROM tb_leave_type_mgmt
--    WHERE ADMIN_AVAIL_FROM_DT IS NOT NULL OR ADMIN_AVAIL_TO_DT IS NOT NULL;  -- 0 기대
--
-- 멱등성: MODIFY COLUMN 은 동일 정의로 재실행해도 안전(동일 결과).
-- 운영 적용: 사용자 수동(read-only MCP). 본 파일은 작성만, DB 직접 적용 금지.
-- ============================================================================

ALTER TABLE tb_leave_type_mgmt
    MODIFY COLUMN ADMIN_AVAIL_FROM_DT varchar(8) COLLATE utf8mb4_unicode_ci DEFAULT NULL
        COMMENT '관리자 부여 연차 사용기간 FROM[YYYYMMDD]',
    MODIFY COLUMN ADMIN_AVAIL_TO_DT   varchar(8) COLLATE utf8mb4_unicode_ci DEFAULT NULL
        COMMENT '관리자 부여 연차 사용기간 TO[YYYYMMDD]';

-- ────────────────────────────────────────────────────────────────────────────
-- 롤백 (varchar(8) → varchar(6) 환원). 기존 데이터가 전부 NULL 이므로 안전.
--   단, 롤백 시점에 이미 8자 값이 적재되어 있으면 6자로 절단되니 적재 데이터부터 확인할 것.
-- ────────────────────────────────────────────────────────────────────────────
-- ALTER TABLE tb_leave_type_mgmt
--     MODIFY COLUMN ADMIN_AVAIL_FROM_DT varchar(6) COLLATE utf8mb4_unicode_ci DEFAULT NULL
--         COMMENT '관리자 부여 연차 사용기간 FROM',
--     MODIFY COLUMN ADMIN_AVAIL_TO_DT   varchar(6) COLLATE utf8mb4_unicode_ci DEFAULT NULL
--         COMMENT '관리자 부여 연차 사용기간 TO';
