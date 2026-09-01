-- =====================================================================================
-- 위치정보(GPS 좌표) 3년 보존기간 파기 배치 — 인덱스 DDL + 사전/사후 검증
--
-- 근거 : 위치기반서비스 이용약관 제7조 ① ("수집일로부터 3년간 보존 후 파기"),
--        위치정보법 제23조(개인위치정보의 파기)
-- 코드 : com.prafta.common.schedule.gps.GpsRetentionScheduler (기본 비활성 + 기본 dry-run)
-- 작성 : 2026-09-01
--
-- ★★적용 순서
--   1) §1 인덱스 3종 적용 (개발 DB / 운영 DB 양쪽 — 마이그레이션 양환경 동시적용 원칙)
--   2) §2 사전 건수 확인. 이 시점의 기대값은 "0건" 이다(서비스 개시 3년 미경과).
--   3) 배치 게이트를 GPS_RETENTION_ENABLED=true 로 올린다. dry-run 은 true 로 둔다.
--      → 매일 04:40 로그에 대상 건수가 찍힌다. §2 결과와 일치하는지 대조한다.
--   4) 대상이 실제로 발생하고 건수가 예상과 맞을 때 GPS_RETENTION_DRY_RUN=false 로 내린다.
--   5) 첫 실전 실행 후 §3 사후 검증.
--
-- ★이 파일에는 파기 DML 이 없다. 파기는 배치만 수행한다.
--   운영 DB 쓰기는 사용자가 Workbench 로 직접 수행하는 것이 원칙이나, 보존기간 파기는
--   일회성 작업이 아니라 상시 의무이므로 배치가 담당하는 것이 맞다.
-- =====================================================================================


-- =====================================================================================
-- §1. 인덱스 (파기 배치의 날짜 조건이 풀스캔이 되지 않게 한다)
--
--   세 테이블의 기존 인덱스는 전부 CMPNY_CD 선두라, 회사 구분 없이 날짜로만 훑는
--   파기 배치에서는 하나도 쓰이지 못한다.
--   ★INSERT 빈도가 낮은 테이블(출퇴근 1건 / TBM 입실 1건)이라 쓰기 부담은 무시할 수준이다.
--
--   ※ MySQL 8.0 은 CREATE INDEX IF NOT EXISTS 를 지원하지 않는다.
--     아래 SELECT 로 존재 여부를 먼저 확인하고, 없을 때만 CREATE 를 실행한다.
-- =====================================================================================

-- (1-0) 이미 있는지 확인 — 결과가 0 인 것만 아래에서 생성한다.
SELECT TABLE_NAME
     , INDEX_NAME
     , COUNT(*) AS EXISTS_CNT
  FROM information_schema.STATISTICS
 WHERE TABLE_SCHEMA = DATABASE()
   AND INDEX_NAME IN ('IX_GPS_PURGE', 'IX_TBM_ATTENDANCE_PURGE', 'IX_TBM_SESSION_PURGE')
 GROUP BY TABLE_NAME, INDEX_NAME;

-- (1-1) 출퇴근 좌표 — 기준 컬럼 API_CALL_DATE(측정일자 YYYYMMDD, NOT NULL)
CREATE INDEX IX_GPS_PURGE ON TB_USER_ATTD_GPS (API_CALL_DATE);

-- (1-2) TBM 입실 좌표 — 기준 컬럼 INSERT_DATE
--       ★ENTRY_AT 이 아니라 INSERT_DATE 인 이유: ENTRY_AT 은 nullable 이라
--         기준으로 쓰면 NULL 인 행이 영원히 파기되지 않는다.
CREATE INDEX IX_TBM_ATTENDANCE_PURGE ON TB_TBM_ATTENDANCE (INSERT_DATE);

-- (1-3) TBM 개설자 좌표 — 기준 컬럼 INSERT_DATE
--       기존 IX_TBM_SESSION_03 은 (CMPNY_CD, INSERT_DATE) 라 선두가 맞지 않아 쓰이지 못한다.
CREATE INDEX IX_TBM_SESSION_PURGE ON TB_TBM_SESSION (INSERT_DATE);


-- =====================================================================================
-- §2. 사전 검증 — 파기 대상 건수 (배치의 count 쿼리와 동일 조건)
--
--   ★배치를 켜기 전에 반드시 이 결과를 확인한다. 되돌릴 수 없는 파기다.
--   ★36 = 보존 개월수(prafta.gps.retention.months). 값을 바꿔 테스트할 때는 세 쿼리를 함께 바꾼다.
-- =====================================================================================

-- (2-1) 출퇴근 좌표
SELECT COUNT(*) AS EXPIRED_ATTD_GPS
  FROM TB_USER_ATTD_GPS
 WHERE API_CALL_DATE < DATE_FORMAT(DATE_SUB(NOW(), INTERVAL 36 MONTH), '%Y%m%d')
   AND (LAT IS NOT NULL OR LON IS NOT NULL OR LAT_ENC IS NOT NULL OR LON_ENC IS NOT NULL);

-- (2-2) TBM 입실 좌표
SELECT COUNT(*) AS EXPIRED_TBM_ATTENDANCE_GPS
  FROM TB_TBM_ATTENDANCE
 WHERE INSERT_DATE < DATE_SUB(NOW(), INTERVAL 36 MONTH)
   AND (ENTRY_GPS_LAT IS NOT NULL OR ENTRY_GPS_LON IS NOT NULL
        OR ENTRY_GPS_LAT_ENC IS NOT NULL OR ENTRY_GPS_LON_ENC IS NOT NULL);

-- (2-3) TBM 개설자 좌표
SELECT COUNT(*) AS EXPIRED_TBM_SESSION_GPS
  FROM TB_TBM_SESSION
 WHERE INSERT_DATE < DATE_SUB(NOW(), INTERVAL 36 MONTH)
   AND (MANAGER_GPS_LAT IS NOT NULL OR MANAGER_GPS_LON IS NOT NULL
        OR MANAGER_GPS_LAT_ENC IS NOT NULL OR MANAGER_GPS_LON_ENC IS NOT NULL);

-- (2-4) 보유 좌표의 가장 오래된 수집일 — "언제부터 파기 대상이 생기는가" 확인용
SELECT 'TB_USER_ATTD_GPS'  AS TBL, MIN(API_CALL_DATE)            AS OLDEST, COUNT(*) AS COORD_ROWS
  FROM TB_USER_ATTD_GPS  WHERE LAT_ENC IS NOT NULL OR LAT IS NOT NULL
 UNION ALL
SELECT 'TB_TBM_ATTENDANCE' AS TBL, DATE_FORMAT(MIN(INSERT_DATE), '%Y%m%d'), COUNT(*)
  FROM TB_TBM_ATTENDANCE WHERE ENTRY_GPS_LAT_ENC IS NOT NULL OR ENTRY_GPS_LAT IS NOT NULL
 UNION ALL
SELECT 'TB_TBM_SESSION'    AS TBL, DATE_FORMAT(MIN(INSERT_DATE), '%Y%m%d'), COUNT(*)
  FROM TB_TBM_SESSION    WHERE MANAGER_GPS_LAT_ENC IS NOT NULL OR MANAGER_GPS_LAT IS NOT NULL;

-- (2-5) 인덱스가 실제로 쓰이는지 확인(풀스캔이면 EXPLAIN 의 key 가 NULL 로 나온다)
EXPLAIN SELECT COUNT(*)
  FROM TB_USER_ATTD_GPS
 WHERE API_CALL_DATE < DATE_FORMAT(DATE_SUB(NOW(), INTERVAL 36 MONTH), '%Y%m%d')
   AND (LAT IS NOT NULL OR LON IS NOT NULL OR LAT_ENC IS NOT NULL OR LON_ENC IS NOT NULL);


-- =====================================================================================
-- §3. 사후 검증 — 첫 실전 실행(dry-run=false) 직후 확인
-- =====================================================================================

-- (3-1) 파기 대상이 0 이 되었는가 → §2 의 세 쿼리를 다시 실행. 전부 0 이어야 한다.

-- (3-2) ★행이 사라지지 않았는가(이 배치는 행을 지우지 않는다).
--       실행 전에 아래 총계를 기록해 두고, 실행 후 같은 값인지 대조한다.
SELECT 'TB_USER_ATTD_GPS'  AS TBL, COUNT(*) AS TOTAL_ROWS FROM TB_USER_ATTD_GPS
 UNION ALL
SELECT 'TB_TBM_ATTENDANCE' AS TBL, COUNT(*)               FROM TB_TBM_ATTENDANCE
 UNION ALL
SELECT 'TB_TBM_SESSION'    AS TBL, COUNT(*)               FROM TB_TBM_SESSION;

-- (3-3) ★업무 기록이 남았는가 — 좌표는 지워졌지만 외근 사유는 남아야 한다.
--       파기된 행 중 외근 사유를 가진 건수(0 이 아니어야 정상. 0 이면 행이 지워진 것).
SELECT COUNT(*) AS PURGED_WITH_REASON
  FROM TB_USER_ATTD_GPS
 WHERE API_CALL_DATE < DATE_FORMAT(DATE_SUB(NOW(), INTERVAL 36 MONTH), '%Y%m%d')
   AND LAT_ENC IS NULL
   AND LAT     IS NULL
   AND OFFSITE_REASON IS NOT NULL;


-- =====================================================================================
-- §4. 리허설 — ★개발 DB 에서만 실행할 것
--
--   ★왜 필요한가: 현재 파기 대상이 전 테이블 0건이다(서비스 개시 3년 미경과).
--     즉 배치를 실전으로 켜도 2029년까지는 UPDATE 가 한 번도 실행되지 않아,
--     "파기가 실제로 동작하는가" 를 3년 뒤에야 알게 된다.
--     아래 블록은 보존기간을 1개월로 낮춰 UPDATE 경로를 실제로 태워 보고 되돌린다.
--
--   ★★운영 DB 에서 실행 금지. 롤백을 잊으면 실데이터가 파기된다.
--   ★★autocommit 이 켜져 있으면 ROLLBACK 이 듣지 않는다. START TRANSACTION 을 반드시 먼저 실행.
--
--   START TRANSACTION;
--
--   -- (4-1) 파기 전 좌표 보유 건수
--   SELECT COUNT(*) AS BEFORE_COORD FROM TB_USER_ATTD_GPS WHERE LAT_ENC IS NOT NULL OR LAT IS NOT NULL;
--
--   -- (4-2) 보존 1개월로 낮춰 파기 실행(배치의 purgeAttdGps 와 동일 문장, 36 → 1)
--   UPDATE TB_USER_ATTD_GPS
--      SET LAT = NULL, LON = NULL, LAT_ENC = NULL, LON_ENC = NULL
--    WHERE API_CALL_DATE < DATE_FORMAT(DATE_SUB(NOW(), INTERVAL 1 MONTH), '%Y%m%d')
--      AND (LAT IS NOT NULL OR LON IS NOT NULL OR LAT_ENC IS NOT NULL OR LON_ENC IS NOT NULL)
--    LIMIT 1000;
--
--   -- (4-3) 검증: 좌표 보유 건수는 줄고, 전체 행 수와 외근 사유는 그대로여야 한다
--   SELECT COUNT(*) AS AFTER_COORD   FROM TB_USER_ATTD_GPS WHERE LAT_ENC IS NOT NULL OR LAT IS NOT NULL;
--   SELECT COUNT(*) AS TOTAL_ROWS    FROM TB_USER_ATTD_GPS;
--   SELECT COUNT(*) AS REASON_ALIVE  FROM TB_USER_ATTD_GPS WHERE OFFSITE_REASON IS NOT NULL;
--
--   -- (4-4) 반드시 되돌린다
--   ROLLBACK;
--
--   ※ TBM 두 테이블도 같은 방식으로 확인할 수 있다(§2 의 각 조건에서 36 → 1).
-- =====================================================================================


-- =====================================================================================
-- §5. 롤백
--
--   ★파기 자체는 롤백할 수 없다(복구 불가능한 파기가 목적이다).
--     되돌릴 수 있는 것은 게이트와 인덱스뿐이다.
--
--   (5-1) 배치 중단  : GPS_RETENTION_ENABLED=false (또는 GPS_RETENTION_DRY_RUN=true) 후 재기동
--   (5-2) 인덱스 제거:
--         DROP INDEX IX_GPS_PURGE            ON TB_USER_ATTD_GPS;
--         DROP INDEX IX_TBM_ATTENDANCE_PURGE ON TB_TBM_ATTENDANCE;
--         DROP INDEX IX_TBM_SESSION_PURGE    ON TB_TBM_SESSION;
-- =====================================================================================
