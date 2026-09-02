-- =====================================================================================
-- 위치정보 동의철회·중지 도입 S5 — 좌표 파기 사유 컬럼
--
-- 근거 : .claude/refs/위치정보_동의철회_중지_작업지시서.md §5-2(배지 — 원인 3종 구분)
-- 작성 : 2026-09-02
--
-- ★개발 DB / 운영 DB 양쪽에 적용한다.
-- ★애플리케이션 배포보다 먼저 적용한다(신규 컬럼을 읽는 조회 쿼리가 배포와 함께 나간다).
-- =====================================================================================


-- =====================================================================================
-- §0. 왜 컬럼을 두는가
--
--   "행은 있는데 좌표가 없다" 는 상태가 세 가지 원인으로 생긴다.
--     ① 동의 철회로 파기        (WITHDRAW)
--     ② 3년 보존기간 경과로 파기 (RETENTION)
--     ③ 애초에 좌표가 안 잡힘    (기기 사정 — 종전부터 존재하던 폴백)
--   관리자 화면이 이 셋을 구분하지 못하면 ③ 까지 "철회됨" 으로 오해하게 된다.
--
--   ★파기 이력 테이블(TB_LOCATION_PURGE_HIST)만으로는 판정할 수 없다.
--     - 이력은 계정 단위·기간 단위라 개별 행에 정확히 대응시키기 어렵고,
--     - 3년 파기 배치는 이력을 남기지 않는다(계정 단위가 아니라 시간 경과 기준이라 대상이 전 회사에 걸친다).
--   조회 화면 3곳이 조인 없이 컬럼 하나만 더 읽으면 되도록 행에 직접 사유를 남긴다.
--
--   ★이 컬럼은 좌표가 아니다 — "지웠다는 사실"만 담는다. 파기 원칙에 어긋나지 않는다.
-- =====================================================================================


-- =====================================================================================
-- §1. 사전 확인 — 이미 적용됐으면 결과가 3행
-- =====================================================================================

SELECT TABLE_NAME, COLUMN_NAME
  FROM information_schema.COLUMNS
 WHERE TABLE_SCHEMA = DATABASE()
   AND COLUMN_NAME IN ('GPS_PURGE_REASON_CD', 'ENTRY_GPS_PURGE_REASON_CD', 'MANAGER_GPS_PURGE_REASON_CD')
 ORDER BY TABLE_NAME;


-- =====================================================================================
-- §2. 컬럼 추가
--
--   NULL = 파기되지 않음. 좌표가 NULL 인데 이 값도 NULL 이면 "원래 좌표가 없던 행"(③)이다.
-- =====================================================================================

ALTER TABLE TB_USER_ATTD_GPS
  ADD COLUMN GPS_PURGE_REASON_CD VARCHAR(20) NULL
  COMMENT '좌표 파기사유[WITHDRAW:동의철회/RETENTION:보존기간경과] NULL=미파기(원래 좌표 없음 포함)';

ALTER TABLE TB_TBM_ATTENDANCE
  ADD COLUMN ENTRY_GPS_PURGE_REASON_CD VARCHAR(20) NULL
  COMMENT '입실좌표 파기사유[WITHDRAW/RETENTION] NULL=미파기';

ALTER TABLE TB_TBM_SESSION
  ADD COLUMN MANAGER_GPS_PURGE_REASON_CD VARCHAR(20) NULL
  COMMENT '개설자좌표 파기사유[WITHDRAW/RETENTION] NULL=미파기';


-- =====================================================================================
-- §3. 사후 검증
-- =====================================================================================

-- (3-1) 컬럼 3종 생성 확인 — 3행이어야 한다.
SELECT TABLE_NAME, COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE
  FROM information_schema.COLUMNS
 WHERE TABLE_SCHEMA = DATABASE()
   AND COLUMN_NAME IN ('GPS_PURGE_REASON_CD', 'ENTRY_GPS_PURGE_REASON_CD', 'MANAGER_GPS_PURGE_REASON_CD')
 ORDER BY TABLE_NAME;

-- (3-2) 기존 행은 전부 NULL 이어야 한다(소급 채움 없음).
--   ★기왕에 좌표가 없던 행을 RETENTION/WITHDRAW 로 소급 표기하지 않는다 —
--     실제로는 기기 사정으로 안 잡힌 건들이라 잘못된 라벨이 된다.
SELECT 'TB_USER_ATTD_GPS'  AS TBL
     , COUNT(*)                                    AS TOTAL
     , SUM(GPS_PURGE_REASON_CD IS NOT NULL)        AS REASON_SET
     , SUM(LAT IS NULL AND LAT_ENC IS NULL)        AS NO_COORD
  FROM TB_USER_ATTD_GPS
 UNION ALL
SELECT 'TB_TBM_ATTENDANCE'
     , COUNT(*)
     , SUM(ENTRY_GPS_PURGE_REASON_CD IS NOT NULL)
     , SUM(ENTRY_GPS_LAT IS NULL AND ENTRY_GPS_LAT_ENC IS NULL)
  FROM TB_TBM_ATTENDANCE
 UNION ALL
SELECT 'TB_TBM_SESSION'
     , COUNT(*)
     , SUM(MANAGER_GPS_PURGE_REASON_CD IS NOT NULL)
     , SUM(MANAGER_GPS_LAT IS NULL AND MANAGER_GPS_LAT_ENC IS NULL)
  FROM TB_TBM_SESSION;

-- (3-3) ★불변식 — 사유가 있으면 좌표는 반드시 없어야 한다(결과 0).
--   사유가 있는데 좌표가 남아 있으면 파기가 덜 된 것이다.
SELECT SUM(GPS_PURGE_REASON_CD IS NOT NULL AND (LAT IS NOT NULL OR LAT_ENC IS NOT NULL)) AS BROKEN
  FROM TB_USER_ATTD_GPS;


-- =====================================================================================
-- §4. 롤백
--
--   ALTER TABLE TB_USER_ATTD_GPS   DROP COLUMN GPS_PURGE_REASON_CD;
--   ALTER TABLE TB_TBM_ATTENDANCE  DROP COLUMN ENTRY_GPS_PURGE_REASON_CD;
--   ALTER TABLE TB_TBM_SESSION     DROP COLUMN MANAGER_GPS_PURGE_REASON_CD;
--
--   ★애플리케이션이 배포된 상태에서 컬럼을 지우면 조회가 전멸한다.
--     롤백은 반드시 애플리케이션 롤백(이전 JAR) 이후에 수행한다.
-- =====================================================================================
