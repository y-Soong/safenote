-- ============================================================================
-- PRAFTA-COM-016-B (3-2) — 관리자 부여 연차 "사용 가능 기간"(SYS026='03' 기간설정)
--   절대 날짜 범위 → "부여일로부터 N개월" 상대기간으로 재정의
-- 작성일: 2026-06-18
-- 적용 환경: MySQL 8.0.42
-- 참조: .claude/requests/common/prafta-com-016-B.md (3-2)
--       .claude/context/policies/attd/08-leave.md §8.1.1 (사용가능기간 속성)
--
-- 변경 요약
--   관리자 부여 타입(LEAVE_TYPE='02') + 사용가능기간 '03'(기간설정)의 의미를
--   기존 절대 날짜 범위(ADMIN_AVAIL_FROM_DT/TO_DT, YYYYMMDD)에서
--   "부여일로부터 N개월 내 사용"(상대기간)으로 바꾼다.
--   이를 저장할 신규 정수 컬럼 ADMIN_AVAIL_MONTHS(tinyint unsigned, 1~99)를 추가한다.
--   만료(AVAIL_TO_DATE) = 부여일 + N개월의 해당일(존재 안 하는 날은 말일 보정).
--
--   ※ 기존 ADMIN_AVAIL_FROM_DT/TO_DT 컬럼은 더 이상 코드에서 소비/적재하지 않는다
--     (DROP 은 하지 않고 유지 — 본 마이그 범위 밖). 관리자 '03' 실데이터는 0건이라
--     별도 데이터 변환은 불필요하다.
--   ※ 사용자 신청 타입('01')의 AVAIL_FROM_DT/TO_DT(varchar(4), MMDD)는 무관(미변경).
--
-- 데이터 손실 위험: 없음(컬럼 ADD 만).
--
-- 적용 전 현재 상태 확인 (운영 적용 직전 권장):
--   SHOW COLUMNS FROM tb_leave_type_mgmt LIKE 'ADMIN_AVAIL_MONTHS';  -- 미존재(추가 대상) 확인
--   SELECT COUNT(*) FROM tb_leave_type_mgmt
--    WHERE LEAVE_TYPE='02' AND ADMIN_AVAIL_TERM_TYPE='03';  -- 0 기대(관리자 '03' 실데이터 없음)
--
-- 멱등성: 동일 컬럼이 이미 있으면 ADD COLUMN 은 에러. 재실행 시 위 SHOW COLUMNS 로 선확인.
-- 운영 적용: 사용자 수동(read-only MCP). 본 파일은 작성만, DB 직접 적용 금지.
-- ============================================================================

ALTER TABLE tb_leave_type_mgmt
    ADD COLUMN ADMIN_AVAIL_MONTHS tinyint unsigned DEFAULT NULL
        COMMENT '관리자부여 기간설정[SYS026=03] 부여일로부터 N개월(1~99). 그 외 null';

-- ────────────────────────────────────────────────────────────────────────────
-- 롤백 (컬럼 제거).
-- ────────────────────────────────────────────────────────────────────────────
-- ALTER TABLE tb_leave_type_mgmt DROP COLUMN ADMIN_AVAIL_MONTHS;
