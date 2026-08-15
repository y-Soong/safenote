-- ============================================================================
-- 기간 연차 신청 + 묶음 승인 — 운영 실기기 테스트 검증 SQL (SELECT 전용)
--   작성: 2026-08-15
--   대상: 운영 DB (Workbench SSH 터널). 조회만 한다 — DML/DDL 없음.
--
--   식별 기준: TB_USER_ATTD_REQ.LEAVE_GROUP_ID IS NOT NULL
--     배포 시점 기준 운영에 이 값이 있는 행은 0건이었으므로, 여기 걸리는 건 전부
--     이번 테스트분이다(작업지시서 §3).
--
--   ★ 실고객 PII(이름/휴대폰/이메일) 컬럼은 일부러 SELECT 하지 않는다.
--   ★ 별칭은 백틱으로 감쌌다 — 일부 클라이언트가 따옴표 없는 한글 식별자를 파싱하지 못한다.
-- ============================================================================


-- ── Q1. 묶음 구성 · 승인 상태 (기대: 1묶음 / 2건 / 전건 REQ_STATUS='02') ────────
--   REQ_STATUS (SYS033): 01신청 / 02승인 / 03반려 / 04취소
--   LEAVE_GROUP_ID 형식: LG + yyyyMMdd + 일련번호 (예: LG2026081500001)
SELECT
       LEAVE_GROUP_ID
     , COUNT(*)                                        AS `건수`
     , MIN(WORK_YMD)                                   AS `시작일`
     , MAX(WORK_YMD)                                   AS `종료일`
     , SUM(LEAVE_DAYS)                                 AS `총일수`
     , GROUP_CONCAT(DISTINCT REQ_STATUS)               AS `상태코드`
     , GROUP_CONCAT(REQ_ID ORDER BY WORK_YMD)          AS `REQ_ID목록`
     , MIN(INSERT_DATE)                                AS `신청시각`
     , MAX(PROCESS_DATE)                               AS `처리시각`
  FROM TB_USER_ATTD_REQ
 WHERE LEAVE_GROUP_ID IS NOT NULL
   AND DEL_YN = 'N'
 GROUP BY LEAVE_GROUP_ID
 ORDER BY MIN(INSERT_DATE) DESC
 LIMIT 20;


-- ── Q2. ★핵심: 알림이 묶음당 1건씩만 적재됐는가 ─────────────────────────────
--   기대: 정확히 2행. 각 행 `건수`=1.
--     LV_TURN_GRP_<groupId>_1          → 결재자에게 차례 알림 (N-F)
--     LV_RESULT_GRP_<groupId>_APPROVED → 신청자에게 결과 알림 (U6)
--   `건수`가 2 이상이면 dedupKey 가 안 먹은 것이다.
--   SEND_STATUS 는 FAILED 여도 대부분 NO_DEVICE_TOKEN(수신 기기 미등록)이라 정상이다 —
--     실제 푸시를 받았다면 SENT 를 기대한다.
SELECT
       DEDUP_KEY
     , COUNT(*)              AS `건수`
     , MIN(NOTI_TYPE)        AS `알림유형`
     , MIN(CHANNEL)          AS `채널`
     , MIN(SEND_STATUS)      AS `발송상태`
     , MIN(ERROR_MSG)        AS `오류`
     , MIN(INSERT_DATE)      AS `적재시각`
     , MIN(SENT_DATE)        AS `발송시각`
  FROM TB_NOTI_OUTBOX
 WHERE (DEDUP_KEY LIKE 'LV\_TURN\_GRP\_%' OR DEDUP_KEY LIKE 'LV\_RESULT\_GRP\_%')
   AND DEL_YN = 'N'
 GROUP BY DEDUP_KEY
 ORDER BY MIN(INSERT_DATE) DESC
 LIMIT 20;


-- ── Q3. 묶음 건인데 단건 알림 키가 섞였는가 ─────────────────────────────────
--   ★판정 주의: 이 쿼리에 행이 나온다고 무조건 결함이 아니다.
--     웹에서 묶음을 ▸ 펼쳐 건별로 처리하면 단건 키(LV_RESULT_<reqId>_*)가 나오는 게 정상이다.
--     "일괄 승인 버튼으로 한 번에 처리했는데" 여기 행이 나오면 그때가 결함이다.
--   (개발 DB 실측: 5건짜리 묶음을 개별 승인한 이력이 이 쿼리에 정상적으로 잡힌다)
SELECT
       O.DEDUP_KEY
     , O.NOTI_TYPE
     , O.SEND_STATUS
     , O.INSERT_DATE
  FROM TB_NOTI_OUTBOX O
 WHERE O.DEL_YN = 'N'
   AND O.DEDUP_KEY REGEXP '^LV_(TURN|RESULT)_[^G]'
   AND EXISTS (
         SELECT 1
           FROM TB_USER_ATTD_REQ R
          WHERE R.LEAVE_GROUP_ID IS NOT NULL
            AND R.DEL_YN = 'N'
            AND O.DEDUP_KEY LIKE CONCAT('LV\_%\_', R.REQ_ID, '\_%')
       )
 ORDER BY O.INSERT_DATE DESC
 LIMIT 20;


-- ── Q4. 연차 사용 실적 — 날짜별로 정상 차감됐는가 ──────────────────────────
--   기대: 신청 건수와 같은 행 수(2행). 각 행 LEAVE_DAYS=1.0.
--   ★분할차감 불변식: 한 신청이 여러 GRANT 로 쪼개지면 LEAVE_MINUTES 는 첫 행에만 총량이
--     들어간다(나머지 NULL). 이 경우 행 수가 2보다 많아지는 것이 정상이다.
SELECT
       U.REQ_ID
     , U.LEAVE_CD
     , U.START_DATE
     , U.END_DATE
     , U.USE_UNIT_TYPE
     , U.LEAVE_DAYS
     , U.LEAVE_MINUTES
     , U.LEAVE_STATUS
     , U.GRANT_ID
  FROM TB_USER_LEAVE_USE U
 WHERE U.DEL_YN = 'N'
   AND U.REQ_ID IN (
         SELECT R.REQ_ID
           FROM TB_USER_ATTD_REQ R
          WHERE R.LEAVE_GROUP_ID IS NOT NULL
            AND R.DEL_YN = 'N'
       )
 ORDER BY U.START_DATE, U.LEAVE_ID
 LIMIT 50;


-- ── Q5. 잔여 대조 — 부여 원장의 USED_DAYS 가 올라갔는가 ────────────────────
--   기대: 테스트 전 잔여 2.71 → 2일 차감 후 0.71.
--   (여러 GRANT 가 있으면 만료일 빠른 것부터 소진된다 — 행별 `잔여` 합계로 본다)
SELECT
       G.GRANT_ID
     , G.LEAVE_CD
     , G.GRANT_TYPE
     , G.GRANT_DAYS
     , G.USED_DAYS
     , (G.GRANT_DAYS - G.USED_DAYS) AS `잔여`
     , G.AVAIL_FROM_DATE
     , G.AVAIL_TO_DATE
     , G.STATUS
     , G.EXPIRE_YN
  FROM TB_USER_LEAVE_GRANT G
 WHERE G.DEL_YN = 'N'
   AND G.USER_CD = (
         SELECT DISTINCT R.USER_CD
           FROM TB_USER_ATTD_REQ R
          WHERE R.LEAVE_GROUP_ID IS NOT NULL
            AND R.DEL_YN = 'N'
          LIMIT 1
       )
 ORDER BY G.AVAIL_TO_DATE, G.GRANT_ID
 LIMIT 20;
