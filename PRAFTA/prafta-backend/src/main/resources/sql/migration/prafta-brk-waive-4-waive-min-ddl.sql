-- ============================================================================
-- prafta-brk-waive-4-waive-min-ddl.sql
-- 부분휴가(반차·시간차) 휴게 넘김 v2 — 넘긴 휴게 분량 컬럼(BRK_WAIVE_MIN) 신설
--   근거: 근로기준법 제54조① 단서(법률 제21784호, 2026-06-09 공포)
--   요청서: .claude/requests/app_requests/작업지시서_부분휴가-휴게넘김-v2-법정하한상한제.md §1-4
--   plan  : .claude/requests/app_requests/작업지시서_부분휴가-휴게넘김-v2-법정하한상한제.plan.md (BW2-01, §3-1, §7 Q2)
-- 작성일: 2026-09-05
-- 적용 환경: MySQL 8
--
-- ★ 개발 DB · 운영 DB 양쪽에 "동시" 적용한다(2026-07-26 사용자 지시 — 한쪽만 적용 금지).
--    실행은 사용자가 Workbench 로 직접 수행한다(본 파일은 실행하지 않는다).
-- ★ 적용 순서: BE 재기동 "전" 선적용 필수 — BW2-04 이후 insertLeaveUse 4본(앱/웹/짜투리/이동)과
--    SELECT 8본(승인상세·일자상세·Attd_10·Attd_13 등)이 이 컬럼을 읽고 쓴다.
--    미적용 상태로 신규 코드가 뜨면 연차 신청·승인상세·일자상세·Attd_10·Attd_13 이 1054(Unknown column)로 전멸.
--    BW2-02/BW2-03(순수 산식·테스트)만 배포되는 시점에는 이 DDL 이 없어도 동작한다.
-- ★ 비멱등: ALTER ADD COLUMN 은 재실행 시 1060(Duplicate column). 적용 여부는 §2 검증 SELECT 로 확인하고
--    이미 반영된 환경에서는 해당 구문을 건너뛴다.
-- ★ AFTER 위치 근거(코드로 확인 — .claude/context/schema-full.sql 스냅샷은 stale):
--      tb_user_leave_use.BRK_WAIVE_REQ_DTIME — prafta-brk-waive-1-ddl.sql 로 추가된 컬럼(LEAVE_MINUTES 뒤).
--    실행 전 DESCRIBE 로 직전 컬럼이 있는지 한 번 더 확인하고, 없으면 AFTER 절을 지우고 실행한다
--    (위치는 의미 인접일 뿐 기능과 무관).
-- ★ 백필 없음(plan §7 Q2): 기존 BRK_WAIVE_YN='Y' 행은 BRK_WAIVE_MIN NULL 유지 → 조회·표시에서 "휴게 전부 넘김(B)" 으로 해석.
-- 롤백: 코드만 롤백하면 컬럼은 NULL 로 무해 잔존 — DDL 롤백 불필요.
--       필요 시 수동: ALTER TABLE tb_user_leave_use DROP COLUMN BRK_WAIVE_MIN;
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1) 연차 사용 실적 — 넘긴 휴게 분량
--    BRK_WAIVE_YN='N' → NULL / 'Y' → 반차: 적용된 W(0=기록 전용) · 시간차: 편입 휴게분(0=기록 전용).
--    분할 차감(여러 행)이면 모든 행에 동일 값 저장(BRK_WAIVE_YN 규약과 동일 — LEAVE_MINUTES 첫 행 한정 규약과 다름).
-- ----------------------------------------------------------------------------
ALTER TABLE tb_user_leave_use
      ADD COLUMN BRK_WAIVE_MIN int NULL DEFAULT NULL
          COMMENT '넘긴 휴게 분(15분 단위). BRK_WAIVE_YN=Y 일 때만 값: 반차=경계 이동 분(0=기록 전용), 시간차=편입 휴게 분. NULL+Y 는 v2 이전 저장분(전부 넘김으로 해석). 근기법 제54조① 단서'
          AFTER BRK_WAIVE_REQ_DTIME;

-- ----------------------------------------------------------------------------
-- 2) 검증 (수동 실행)
-- ----------------------------------------------------------------------------
-- (2-1) 컬럼 1행 — 기대 COLUMN_TYPE 'int', IS_NULLABLE 'YES', COLUMN_DEFAULT NULL
SELECT COLUMN_NAME
     , COLUMN_TYPE
     , IS_NULLABLE
     , COLUMN_DEFAULT
     , ORDINAL_POSITION
  FROM INFORMATION_SCHEMA.COLUMNS
 WHERE TABLE_SCHEMA = DATABASE()
   AND TABLE_NAME = 'tb_user_leave_use'
   AND COLUMN_NAME = 'BRK_WAIVE_MIN';

-- (2-2) 컬럼 순서 — 기대 LEAVE_MINUTES → BRK_WAIVE_YN → BRK_WAIVE_REQ_DTIME → BRK_WAIVE_MIN 순
SELECT COLUMN_NAME
     , ORDINAL_POSITION
  FROM INFORMATION_SCHEMA.COLUMNS
 WHERE TABLE_SCHEMA = DATABASE()
   AND TABLE_NAME = 'tb_user_leave_use'
   AND COLUMN_NAME IN ('LEAVE_MINUTES', 'BRK_WAIVE_YN', 'BRK_WAIVE_REQ_DTIME', 'BRK_WAIVE_MIN')
 ORDER BY ORDINAL_POSITION;

-- (2-3) 백필 없음 확인 — 기대 waive_y_cnt = y_and_min_null_cnt (기존 Y 행 전부 NULL), n_and_min_not_null_cnt = 0
SELECT COUNT(*) AS use_cnt
     , SUM(CASE WHEN BRK_WAIVE_YN = 'Y' THEN 1 ELSE 0 END) AS waive_y_cnt
     , SUM(CASE WHEN BRK_WAIVE_YN = 'Y' AND BRK_WAIVE_MIN IS NULL THEN 1 ELSE 0 END) AS y_and_min_null_cnt
     , SUM(CASE WHEN BRK_WAIVE_YN = 'N' AND BRK_WAIVE_MIN IS NOT NULL THEN 1 ELSE 0 END) AS n_and_min_not_null_cnt
  FROM tb_user_leave_use;
