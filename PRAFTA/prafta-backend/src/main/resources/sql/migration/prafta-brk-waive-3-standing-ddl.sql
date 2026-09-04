-- ============================================================================
-- prafta-brk-waive-3-standing-ddl.sql
-- 단시간(4시간·휴게 0) 근로자의 휴게 미이용 상시 요청 — 현행값(tb_user 2컬럼) + 변경 이력 테이블
--   근거: 근로기준법 제54조① 단서(법률 제21784호, 2026-06-09 공포, 2026-12-10 시행)
--   요청서: .claude/requests/app_requests/작업지시서_부분휴가-휴게무시.md §7-1
--   plan  : .claude/requests/app_requests/작업지시서_부분휴가-휴게무시.plan.md (BW-12, §3-3, §7 Q-7/Q-8)
--   정책서: .claude/context/policies/attd/08-leave.md §8.5.10(e)
-- 작성일: 2026-09-04
-- 적용 환경: MySQL 8
--
-- ★ 개발 DB · 운영 DB 양쪽에 "동시" 적용한다(2026-07-26 사용자 지시 — 한쪽만 적용 금지).
--    실행은 사용자가 Workbench 로 직접 수행한다(본 파일은 실행하지 않는다).
-- ★ 적용 순서: BE 재기동 "전" 선적용 필수 — 앱 마이페이지 EP(GET/PUT /appApi/mypage/brk-waive-standing)와
--    앱 홈 today / 웹 attd07 일자상세의 배지 산출이 신규 컬럼·테이블을 읽는다. 미적용 상태로 신규 코드가
--    뜨면 마이페이지 진입·홈 카드·일자상세 조회가 1054(Unknown column)/1146(Table doesn't exist) 로 실패한다.
-- ★ 비멱등: ALTER ADD COLUMN 은 재실행 시 1060(Duplicate column). CREATE TABLE 은 IF NOT EXISTS 로 멱등.
--    적용 여부는 §3 검증 SELECT 로 확인하고 이미 반영된 환경에서는 해당 구문을 건너뛴다.
-- ★ AFTER 위치 근거: tb_user.DEFAULT_SCH_SET_DATE 는 prafta-com-008-e-1-default-worktype-columns.sql 로
--    추가된 컬럼(EMPLOYMENT_TYPE → DEFAULT_SCH_CD → DEFAULT_SCH_SET_DATE 순). .claude/context/schema-full.sql
--    스냅샷에는 없다. 실행 전 DESCRIBE tb_user 로 직전 컬럼이 있는지 확인하고, 없으면 AFTER 절을 지우고
--    실행한다(위치는 의미 인접일 뿐 기능과 무관).
-- 롤백: 코드만 롤백하면 컬럼은 DEFAULT 'N' 으로 무해 잔존 — DDL 롤백 불필요.
--       필요 시 수동: ALTER TABLE tb_user DROP COLUMN BRK_WAIVE_STANDING_DTIME, DROP COLUMN BRK_WAIVE_STANDING_YN;
--                     DROP TABLE tb_user_brk_waive_standing_hist;   (요청 사실 기록이므로 운영에서는 DROP 금지)
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1) tb_user — 상시 요청 현행값
--    근로자 본인이 앱 마이페이지에서만 켜고 끈다(관리자 대리 불가, 서버가 토큰 본인으로 강제).
--    일용직(EMPLOYMENT_TYPE='DAILY')은 대상 아님(서버 거부). 노출 조건(정규직 + 기본 근무타입 소정 240·휴게 0)은
--    조회 시 서버가 판정한다(eligibleYn) — 컬럼으로 두지 않는다.
-- ----------------------------------------------------------------------------
ALTER TABLE tb_user
      ADD COLUMN BRK_WAIVE_STANDING_YN char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'N'
          COMMENT '4시간 근무일 휴게 미이용 상시 요청 Y:요청/N:미요청 (근기법 제54조① 단서, 근로자 본인만 설정, DAILY 제외)'
          AFTER DEFAULT_SCH_SET_DATE
    , ADD COLUMN BRK_WAIVE_STANDING_DTIME datetime NULL DEFAULT NULL
          COMMENT '상시 요청 최종 변경 시각(서버). 켠/끈 이력은 tb_user_brk_waive_standing_hist'
          AFTER BRK_WAIVE_STANDING_YN;

-- ----------------------------------------------------------------------------
-- 2) 상시 요청 변경 이력 — 켠 시각·끈 시각을 행으로 보관(요청 사실 기록 보관, 삭제 없음)
--    CHG_BY = 행위자. 본인 경로만 존재하므로 항상 USER_CD 와 동일(서버 강제) — 관리자 대리 경로가 생기면 구분값이 된다.
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS tb_user_brk_waive_standing_hist (
      SEQ          bigint      NOT NULL AUTO_INCREMENT COMMENT '이력 일련번호 (PK)'
    , CMPNY_CD     varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '회사코드 (tb_user.CMPNY_CD)'
    , USER_CD      varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사용자코드 (tb_user.USER_CD)'
    , STANDING_YN  char(1)     CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '변경 후 값 Y:요청/N:철회'
    , CHG_DTIME    datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '변경 시각(서버)'
    , CHG_BY       varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '행위자 사용자코드(본인 — USER_CD 와 동일, 서버 강제)'
    , PRIMARY KEY (SEQ)
    , KEY IX_TB_USER_BRK_WAIVE_STANDING_HIST (CMPNY_CD, USER_CD, CHG_DTIME)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='휴게 미이용 상시 요청 변경 이력 (근기법 제54조① 단서 — 근로자 명시 요청 사실 기록)';

-- ----------------------------------------------------------------------------
-- 3) 검증 (수동 실행)
-- ----------------------------------------------------------------------------
-- (3-1) tb_user 컬럼 2개 — 기대 2행, COLUMN_DEFAULT 'N' / NULL
SELECT COLUMN_NAME
     , COLUMN_TYPE
     , IS_NULLABLE
     , COLUMN_DEFAULT
  FROM INFORMATION_SCHEMA.COLUMNS
 WHERE TABLE_SCHEMA = DATABASE()
   AND TABLE_NAME = 'tb_user'
   AND COLUMN_NAME IN ('BRK_WAIVE_STANDING_YN', 'BRK_WAIVE_STANDING_DTIME')
 ORDER BY ORDINAL_POSITION;

-- (3-2) 이력 테이블 — 기대 1행
SELECT TABLE_NAME
     , TABLE_COMMENT
  FROM INFORMATION_SCHEMA.TABLES
 WHERE TABLE_SCHEMA = DATABASE()
   AND TABLE_NAME = 'tb_user_brk_waive_standing_hist';

-- (3-3) 인덱스 — 기대 3행(CMPNY_CD, USER_CD, CHG_DTIME 순)
SELECT INDEX_NAME
     , SEQ_IN_INDEX
     , COLUMN_NAME
  FROM INFORMATION_SCHEMA.STATISTICS
 WHERE TABLE_SCHEMA = DATABASE()
   AND TABLE_NAME = 'tb_user_brk_waive_standing_hist'
   AND INDEX_NAME = 'IX_TB_USER_BRK_WAIVE_STANDING_HIST'
 ORDER BY SEQ_IN_INDEX;

-- (3-4) 기존 사용자 전부 N 으로 시작, 이력 0건
SELECT COUNT(*) AS user_cnt
     , SUM(CASE WHEN BRK_WAIVE_STANDING_YN = 'Y' THEN 1 ELSE 0 END) AS standing_y_cnt
  FROM tb_user;

SELECT COUNT(*) AS hist_cnt
  FROM tb_user_brk_waive_standing_hist;
