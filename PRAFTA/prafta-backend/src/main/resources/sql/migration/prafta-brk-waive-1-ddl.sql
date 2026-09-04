-- ============================================================================
-- prafta-brk-waive-1-ddl.sql
-- 부분휴가(반차·시간차) 휴게 미이용 요청 기록 컬럼 + 회사 허용 토글
--   근거: 근로기준법 제54조① 단서(법률 제21784호, 2026-06-09 공포, 2026-12-10 시행)
--   요청서: .claude/requests/app_requests/작업지시서_부분휴가-휴게무시.md §1-5, §0-3-5·6
--   plan  : .claude/requests/app_requests/작업지시서_부분휴가-휴게무시.plan.md (BW-01)
-- 작성일: 2026-09-04
-- 적용 환경: MySQL 8
--
-- ★ 개발 DB · 운영 DB 양쪽에 "동시" 적용한다(2026-07-26 사용자 지시 — 한쪽만 적용 금지).
--    실행은 사용자가 Workbench 로 직접 수행한다(본 파일은 실행하지 않는다).
-- ★ 적용 순서: BE 재기동 "전" 선적용 필수 — BW-04 이후 insertLeaveUse(앱/웹)·selectCompanyUsageUnit 이
--    신규 컬럼을 읽고 쓴다. 미적용 상태로 신규 코드가 뜨면 연차 신청 전부 1054(Unknown column).
--    BW-02/BW-03(순수 산식·테스트)만 배포되는 시점에는 이 DDL 이 없어도 동작한다.
-- ★ 비멱등: ALTER ADD COLUMN 은 재실행 시 1060(Duplicate column). 적용 여부는 §3 검증 SELECT 로 확인하고
--    이미 반영된 환경에서는 해당 구문을 건너뛴다.
-- ★ AFTER 위치 근거(코드로 확인 — .claude/context/schema-full.sql 스냅샷은 stale):
--      tb_user_leave_use.LEAVE_MINUTES     — AppLeaveFlowMapper.xml insertLeaveUse 컬럼 목록에 존재
--      tb_leave_usage_policy.ALLOW_REMNANT_ROUND_UP — prafta-pc-1-remnant-ddl.sql 로 추가, LeavePolicyMapper.xml 사용 중
--    실행 전 DESCRIBE 로 직전 컬럼이 있는지 한 번 더 확인하고, 없으면 AFTER 절을 지우고 실행한다
--    (위치는 의미 인접일 뿐 기능과 무관).
-- 롤백: 코드만 롤백하면 컬럼은 DEFAULT 'N' / 'Y' 로 무해 잔존 — DDL 롤백 불필요.
--       필요 시 수동: ALTER TABLE tb_user_leave_use DROP COLUMN BRK_WAIVE_REQ_DTIME, DROP COLUMN BRK_WAIVE_YN;
--                     ALTER TABLE tb_leave_usage_policy DROP COLUMN BRK_WAIVE_ALLOW_YN;
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1) 연차 사용 실적 — 휴게 미이용 요청 기록
--    사용 행(tb_user_leave_use)에만 저장한다. 요청 테이블(tb_user_attd_req)은 무변경(요청서 §0-3-6).
--    분할 차감(여러 행)이면 모든 행에 동일 값 저장(BW-04 규약 — LEAVE_MINUTES 첫 행 한정 규약과 다름).
-- ----------------------------------------------------------------------------
ALTER TABLE tb_user_leave_use
      ADD COLUMN BRK_WAIVE_YN char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'N'
          COMMENT '휴게 미이용 명시 요청 여부 Y/N (근기법 제54조① 단서). 반차(01)·시간차(02/03/04) 행만 Y 가능. 근로자 본인 앱 신청 경로에서만 설정, 관리자 대리·엑셀 불가'
          AFTER LEAVE_MINUTES
    , ADD COLUMN BRK_WAIVE_REQ_DTIME datetime NULL DEFAULT NULL
          COMMENT '휴게 미이용 요청 시각(서버 시각). BRK_WAIVE_YN=Y 일 때만 값'
          AFTER BRK_WAIVE_YN;

-- ----------------------------------------------------------------------------
-- 2) 회사 연차 사용 단위 정책 — 휴게 미이용 요청 허용 토글(기본 허용, 요청서 §0-3-5)
--    N 이면 앱 체크박스 미노출 + 서버 ATTD_400_217 거부(BW-04). Baim_07 라디오 1행은 BW-09(Q-4 포함 확정).
-- ----------------------------------------------------------------------------
ALTER TABLE tb_leave_usage_policy
      ADD COLUMN BRK_WAIVE_ALLOW_YN char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'Y'
          COMMENT '부분휴가(반차·시간차) 휴게 미이용 요청 허용 Y/N (기본 Y). N 이면 앱 체크박스 미노출 + 서버 ATTD_400_217 거부'
          AFTER ALLOW_REMNANT_ROUND_UP;

-- ----------------------------------------------------------------------------
-- 3) 검증 (수동 실행)
-- ----------------------------------------------------------------------------
-- (3-1) 사용 행 컬럼 2개 — 기대 2행, COLUMN_DEFAULT 'N' / NULL
SELECT COLUMN_NAME
     , COLUMN_TYPE
     , IS_NULLABLE
     , COLUMN_DEFAULT
  FROM INFORMATION_SCHEMA.COLUMNS
 WHERE TABLE_SCHEMA = DATABASE()
   AND TABLE_NAME = 'tb_user_leave_use'
   AND COLUMN_NAME IN ('BRK_WAIVE_YN', 'BRK_WAIVE_REQ_DTIME')
 ORDER BY ORDINAL_POSITION;

-- (3-2) 회사 정책 토글 — 기대 1행, COLUMN_DEFAULT 'Y'
SELECT COLUMN_NAME
     , COLUMN_TYPE
     , COLUMN_DEFAULT
  FROM INFORMATION_SCHEMA.COLUMNS
 WHERE TABLE_SCHEMA = DATABASE()
   AND TABLE_NAME = 'tb_leave_usage_policy'
   AND COLUMN_NAME = 'BRK_WAIVE_ALLOW_YN';

-- (3-3) 기존 행 상태 — 기존 회사 전부 허용(Y)으로 시작, 기존 사용 행 전부 N
SELECT COUNT(*) AS policy_cnt
     , SUM(CASE WHEN BRK_WAIVE_ALLOW_YN = 'Y' THEN 1 ELSE 0 END) AS allow_y_cnt
  FROM tb_leave_usage_policy;

SELECT COUNT(*) AS use_cnt
     , SUM(CASE WHEN BRK_WAIVE_YN = 'Y' THEN 1 ELSE 0 END) AS waive_y_cnt
  FROM tb_user_leave_use;
