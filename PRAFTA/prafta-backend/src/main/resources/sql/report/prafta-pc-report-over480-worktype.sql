-- ============================================================================
-- PRAFTA-PC — 소정근로 480분(8시간) 초과 근무타입 점검 리포트 (PC-07, N6)
-- 작성일: 2026-07-29
-- 용도: 개인 분모 전환(D1) 배포 전 데이터 품질 점검 — 휴게 미입력 등으로 소정근로가
--       8시간을 초과하는 근무타입(예: 09:00~18:00 휴게 0 = 540분)을 식별한다.
--       분모는 480 캡(§5-③)이 적용되어 계산상 안전하지만, 근무타입 자체의 휴게 입력
--       정정은 고객 몫이다(N6 — 수정은 고객, 본 리포트는 전달용).
-- 실행: 읽기 전용(SELECT만). 개발/운영 어느 쪽이든 실행 가능. 사용자 수동 실행.
-- 산식: ScheduleWorkMinutesUtils.dailyStdWorkMinutes 미러 —
--       구간 근로분 = (종료 − 시작) − 휴게(분), 종료 <= 시작이면 야간 구간 +1440 보정,
--       1구간 필수 + 2구간(시작/종료 모두 입력 시) 합산. '2400' 종료는 1440분으로 계산됨.
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1) 소정근로 480분 초과 근무타입 목록 (현재본 TB_SCH_MGMT 기준) + 기본 근무타입 사용 인원수
-- ----------------------------------------------------------------------------
WITH SCH_MIN AS (
    SELECT
          M.CMPNY_CD
        , M.SITE_CD
        , M.SCH_CD
        , M.SCH_NO
        , M.SCH_TYPE
        , M.USE_YN
        , M.APPLY_DATE
        , M.FST_SCH_STR_TIME
        , M.FST_SCH_END_TIME
        , IFNULL(CAST(NULLIF(M.FST_SCH_BRK_MIN, '') AS SIGNED), 0) AS FST_BRK
        , M.SEC_SCH_STR_TIME
        , M.SEC_SCH_END_TIME
        , IFNULL(CAST(NULLIF(M.SEC_SCH_BRK_MIN, '') AS SIGNED), 0) AS SEC_BRK
        /* 1구간 근로분 = (종료-시작) - 휴게, 야간(종료<=시작) +1440 보정 */
        , (
            (CAST(SUBSTRING(M.FST_SCH_END_TIME, 1, 2) AS SIGNED) * 60
             + CAST(SUBSTRING(M.FST_SCH_END_TIME, 3, 2) AS SIGNED))
            - (CAST(SUBSTRING(M.FST_SCH_STR_TIME, 1, 2) AS SIGNED) * 60
               + CAST(SUBSTRING(M.FST_SCH_STR_TIME, 3, 2) AS SIGNED))
            + CASE WHEN (CAST(SUBSTRING(M.FST_SCH_END_TIME, 1, 2) AS SIGNED) * 60
                         + CAST(SUBSTRING(M.FST_SCH_END_TIME, 3, 2) AS SIGNED))
                        <= (CAST(SUBSTRING(M.FST_SCH_STR_TIME, 1, 2) AS SIGNED) * 60
                            + CAST(SUBSTRING(M.FST_SCH_STR_TIME, 3, 2) AS SIGNED))
                   THEN 1440 ELSE 0 END
            - IFNULL(CAST(NULLIF(M.FST_SCH_BRK_MIN, '') AS SIGNED), 0)
          ) AS FST_WORK_MIN
        /* 2구간 근로분(시작/종료 모두 입력 시) — 동일 산식 */
        , CASE WHEN M.SEC_SCH_STR_TIME IS NOT NULL AND M.SEC_SCH_STR_TIME != ''
                AND M.SEC_SCH_END_TIME IS NOT NULL AND M.SEC_SCH_END_TIME != ''
               THEN (CAST(SUBSTRING(M.SEC_SCH_END_TIME, 1, 2) AS SIGNED) * 60
                     + CAST(SUBSTRING(M.SEC_SCH_END_TIME, 3, 2) AS SIGNED))
                    - (CAST(SUBSTRING(M.SEC_SCH_STR_TIME, 1, 2) AS SIGNED) * 60
                       + CAST(SUBSTRING(M.SEC_SCH_STR_TIME, 3, 2) AS SIGNED))
                    + CASE WHEN (CAST(SUBSTRING(M.SEC_SCH_END_TIME, 1, 2) AS SIGNED) * 60
                                 + CAST(SUBSTRING(M.SEC_SCH_END_TIME, 3, 2) AS SIGNED))
                                <= (CAST(SUBSTRING(M.SEC_SCH_STR_TIME, 1, 2) AS SIGNED) * 60
                                    + CAST(SUBSTRING(M.SEC_SCH_STR_TIME, 3, 2) AS SIGNED))
                           THEN 1440 ELSE 0 END
                    - IFNULL(CAST(NULLIF(M.SEC_SCH_BRK_MIN, '') AS SIGNED), 0)
               ELSE 0 END AS SEC_WORK_MIN
      FROM TB_SCH_MGMT M
)
SELECT
      S.CMPNY_CD                                   AS 회사코드
    , S.SITE_CD                                    AS 사업장코드
    , S.SCH_CD                                     AS 근무타입코드
    , S.SCH_NO                                     AS 근무타입번호
    , S.SCH_TYPE                                   AS 타입구분
    , S.USE_YN                                     AS 사용여부
    , S.APPLY_DATE                                 AS 적용일자
    , CONCAT(S.FST_SCH_STR_TIME, '~', S.FST_SCH_END_TIME, ' (휴게 ', S.FST_BRK, '분)') AS 근무1구간
    , CASE WHEN S.SEC_WORK_MIN > 0
           THEN CONCAT(S.SEC_SCH_STR_TIME, '~', S.SEC_SCH_END_TIME, ' (휴게 ', S.SEC_BRK, '분)')
           ELSE '-' END                            AS 근무2구간
    , S.FST_WORK_MIN + S.SEC_WORK_MIN              AS 소정근로분
    , (S.FST_WORK_MIN + S.SEC_WORK_MIN) - 480      AS 초과분
    , (
        SELECT COUNT(*)
          FROM TB_USER U
         WHERE U.CMPNY_CD       = S.CMPNY_CD
           AND U.SITE_CD        = S.SITE_CD
           AND U.DEFAULT_SCH_CD = S.SCH_CD
           AND U.USE_YN         = 'Y'
           AND U.WITHDRAWAL_DATE IS NULL
           AND U.ACCOUNT_STATUS  = '01'
      )                                            AS 기본근무타입_사용인원
  FROM SCH_MIN S
 WHERE (S.FST_WORK_MIN + S.SEC_WORK_MIN) > 480
 ORDER BY S.CMPNY_CD, S.SITE_CD, (S.FST_WORK_MIN + S.SEC_WORK_MIN) DESC, S.SCH_CD
 LIMIT 500;

-- ----------------------------------------------------------------------------
-- 2) 위 초과 근무타입을 "기본 근무타입"으로 사용 중인 활성 사용자 상세
--    (분모 480 캡 적용 대상자 — 휴게 입력 정정 시 실분모로 복귀)
-- ----------------------------------------------------------------------------
WITH SCH_MIN AS (
    SELECT
          M.CMPNY_CD
        , M.SITE_CD
        , M.SCH_CD
        , (
            (CAST(SUBSTRING(M.FST_SCH_END_TIME, 1, 2) AS SIGNED) * 60
             + CAST(SUBSTRING(M.FST_SCH_END_TIME, 3, 2) AS SIGNED))
            - (CAST(SUBSTRING(M.FST_SCH_STR_TIME, 1, 2) AS SIGNED) * 60
               + CAST(SUBSTRING(M.FST_SCH_STR_TIME, 3, 2) AS SIGNED))
            + CASE WHEN (CAST(SUBSTRING(M.FST_SCH_END_TIME, 1, 2) AS SIGNED) * 60
                         + CAST(SUBSTRING(M.FST_SCH_END_TIME, 3, 2) AS SIGNED))
                        <= (CAST(SUBSTRING(M.FST_SCH_STR_TIME, 1, 2) AS SIGNED) * 60
                            + CAST(SUBSTRING(M.FST_SCH_STR_TIME, 3, 2) AS SIGNED))
                   THEN 1440 ELSE 0 END
            - IFNULL(CAST(NULLIF(M.FST_SCH_BRK_MIN, '') AS SIGNED), 0)
          )
          + CASE WHEN M.SEC_SCH_STR_TIME IS NOT NULL AND M.SEC_SCH_STR_TIME != ''
                  AND M.SEC_SCH_END_TIME IS NOT NULL AND M.SEC_SCH_END_TIME != ''
                 THEN (CAST(SUBSTRING(M.SEC_SCH_END_TIME, 1, 2) AS SIGNED) * 60
                       + CAST(SUBSTRING(M.SEC_SCH_END_TIME, 3, 2) AS SIGNED))
                      - (CAST(SUBSTRING(M.SEC_SCH_STR_TIME, 1, 2) AS SIGNED) * 60
                         + CAST(SUBSTRING(M.SEC_SCH_STR_TIME, 3, 2) AS SIGNED))
                      + CASE WHEN (CAST(SUBSTRING(M.SEC_SCH_END_TIME, 1, 2) AS SIGNED) * 60
                                   + CAST(SUBSTRING(M.SEC_SCH_END_TIME, 3, 2) AS SIGNED))
                                  <= (CAST(SUBSTRING(M.SEC_SCH_STR_TIME, 1, 2) AS SIGNED) * 60
                                      + CAST(SUBSTRING(M.SEC_SCH_STR_TIME, 3, 2) AS SIGNED))
                             THEN 1440 ELSE 0 END
                      - IFNULL(CAST(NULLIF(M.SEC_SCH_BRK_MIN, '') AS SIGNED), 0)
                 ELSE 0 END AS TOTAL_WORK_MIN
      FROM TB_SCH_MGMT M
)
SELECT
      U.CMPNY_CD          AS 회사코드
    , U.SITE_CD           AS 사업장코드
    , U.USER_CD           AS 사용자코드
    , U.USER_NM           AS 사용자명
    , U.DEFAULT_SCH_CD    AS 기본근무타입
    , S.TOTAL_WORK_MIN    AS 소정근로분
    , 480                 AS 적용분모_캡
  FROM TB_USER U
  JOIN SCH_MIN S
    ON S.CMPNY_CD = U.CMPNY_CD
   AND S.SITE_CD  = U.SITE_CD
   AND S.SCH_CD   = U.DEFAULT_SCH_CD
 WHERE U.USE_YN = 'Y'
   AND U.WITHDRAWAL_DATE IS NULL
   AND U.ACCOUNT_STATUS  = '01'
   AND S.TOTAL_WORK_MIN > 480
 ORDER BY U.CMPNY_CD, U.SITE_CD, S.TOTAL_WORK_MIN DESC, U.USER_CD
 LIMIT 1000;
