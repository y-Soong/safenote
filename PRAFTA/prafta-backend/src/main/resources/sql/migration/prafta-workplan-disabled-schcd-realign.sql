-- ============================================================================
-- 비활성(USE_YN='N') 근무타입이 배정된 채 남은 미래 근무계획 정정
--
-- 배경:
--   근무타입을 사용중지(USE_YN='N')해도 이미 배정된 TB_USER_WORK_PLAN 은 정리되지 않았다.
--   조회 쿼리들이 USE_YN='Y' 만 유효 스케줄로 보기 때문에, 그런 날은 앱 홈에서 "스케줄 없음"으로
--   빠지거나(홈) 폐기된 옛 이력버전 시각으로 잘못 해석된다(앱 근태).
--   실사례: 001/00001 SCH_CD='00003'(ST002) 을 2026-06-17 사용중지 → 미래 근무계획 148건(4명) 고아화.
--
-- 조치:
--   비활성 근무타입이 배정된 "오늘 이후" 근무계획을 해당 사용자의 기본 근무타입(TB_USER.DEFAULT_SCH_CD)
--   으로 재배정한다. 과거 근무일은 손대지 않는다(실제 근무 이력이므로 그날 배정된 타입을 보존한다 —
--   과거일 표시는 조회 쿼리의 USE_YN 필터 제거로 해결한다).
--
--   재발 방지는 코드 가드(ATTD_400_163: 미래 근무계획이 있는 근무타입은 사용중지 거부)로 별도 처리.
--
-- 실행 순서: [1] 확인 → [2] STEP 1 → [3] STEP 2(잔여분 있을 때만) → [4] 검증
-- ============================================================================


-- ----------------------------------------------------------------------------
-- [1] 적용 전 영향행 확인
-- ----------------------------------------------------------------------------
SELECT P.CMPNY_CD
     , P.SITE_CD
     , P.WORK_PLAN_CD
     , COUNT(*)                  AS plan_cnt
     , COUNT(DISTINCT P.USER_CD) AS user_cnt
     , MIN(P.WORK_YMD)           AS min_ymd
     , MAX(P.WORK_YMD)           AS max_ymd
  FROM TB_USER_WORK_PLAN P
       INNER JOIN TB_SCH_MGMT S
       ON  S.CMPNY_CD = P.CMPNY_CD
       AND S.SITE_CD  = P.SITE_CD
       AND S.SCH_CD   = P.WORK_PLAN_CD
 WHERE P.WORK_YMD >= DATE_FORMAT(NOW(), '%Y%m%d')
   AND S.USE_YN    = 'N'
 GROUP BY P.CMPNY_CD, P.SITE_CD, P.WORK_PLAN_CD;


-- ----------------------------------------------------------------------------
-- [2] STEP 1 — 기본 근무타입(DEFAULT_SCH_CD)이 설정된 사용자: 그 값으로 재배정
--
--   대상: 오늘 이후 + 배정 근무타입이 USE_YN='N' + 사용자의 DEFAULT_SCH_CD 가 활성(USE_YN='Y')
--   교대팀 소속자 등 DEFAULT_SCH_CD 가 NULL 인 사용자는 STEP 2 에서 별도 처리.
-- ----------------------------------------------------------------------------
UPDATE TB_USER_WORK_PLAN P
       INNER JOIN TB_SCH_MGMT S
       ON  S.CMPNY_CD = P.CMPNY_CD
       AND S.SITE_CD  = P.SITE_CD
       AND S.SCH_CD   = P.WORK_PLAN_CD
       AND S.USE_YN   = 'N'
       INNER JOIN TB_USER U
       ON  U.CMPNY_CD = P.CMPNY_CD
       AND U.SITE_CD  = P.SITE_CD
       AND U.USER_CD  = P.USER_CD
       INNER JOIN TB_SCH_MGMT DS
       ON  DS.CMPNY_CD = U.CMPNY_CD
       AND DS.SITE_CD  = U.SITE_CD
       AND DS.SCH_CD   = U.DEFAULT_SCH_CD
       AND DS.USE_YN   = 'Y'
   SET P.WORK_PLAN_CD = U.DEFAULT_SCH_CD
     , P.UPDATE_NO    = 'SYSTEM'
     , P.UPDATE_DATE  = NOW()
 WHERE P.WORK_YMD >= DATE_FORMAT(NOW(), '%Y%m%d');


-- ----------------------------------------------------------------------------
-- [3] STEP 2 — DEFAULT_SCH_CD 가 없는(NULL) 사용자의 잔여분
--
--   자동 추정은 하지 않는다(잘못된 근무시간을 심는 것보다 남겨두고 수동 지정이 안전).
--   아래 SELECT 로 잔여 사용자를 확인한 뒤, 사업장 사정에 맞는 활성 근무타입으로 직접 지정한다.
--
--   실사례(001/00001): USER_CD='20260400011'(WLSGML108) 이 DEFAULT_SCH_CD NULL.
--   같은 사업장 기본 근무타입 ST001 = SCH_CD '00002' (09:30~18:00) 로 맞춘다.
-- ----------------------------------------------------------------------------

-- (3-1) 잔여 확인
SELECT P.CMPNY_CD, P.SITE_CD, P.USER_CD, U.USER_ID, U.DEFAULT_SCH_CD
     , P.WORK_PLAN_CD, COUNT(*) AS plan_cnt
  FROM TB_USER_WORK_PLAN P
       INNER JOIN TB_SCH_MGMT S
       ON  S.CMPNY_CD = P.CMPNY_CD
       AND S.SITE_CD  = P.SITE_CD
       AND S.SCH_CD   = P.WORK_PLAN_CD
       AND S.USE_YN   = 'N'
       INNER JOIN TB_USER U
       ON  U.CMPNY_CD = P.CMPNY_CD
       AND U.SITE_CD  = P.SITE_CD
       AND U.USER_CD  = P.USER_CD
 WHERE P.WORK_YMD >= DATE_FORMAT(NOW(), '%Y%m%d')
 GROUP BY P.CMPNY_CD, P.SITE_CD, P.USER_CD, U.USER_ID, U.DEFAULT_SCH_CD, P.WORK_PLAN_CD;

-- (3-2) 잔여분 지정 재배정 — 위 SELECT 결과에 맞춰 USER_CD / 대체 SCH_CD 를 확인하고 실행.
UPDATE TB_USER_WORK_PLAN P
       INNER JOIN TB_SCH_MGMT S
       ON  S.CMPNY_CD = P.CMPNY_CD
       AND S.SITE_CD  = P.SITE_CD
       AND S.SCH_CD   = P.WORK_PLAN_CD
       AND S.USE_YN   = 'N'
   SET P.WORK_PLAN_CD = '00002'
     , P.UPDATE_NO    = 'SYSTEM'
     , P.UPDATE_DATE  = NOW()
 WHERE P.CMPNY_CD  = '001'
   AND P.SITE_CD   = '00001'
   AND P.USER_CD   = '20260400011'
   AND P.WORK_YMD >= DATE_FORMAT(NOW(), '%Y%m%d');


-- ----------------------------------------------------------------------------
-- [4] 검증 — 0행이어야 정상(오늘 이후 근무계획에 비활성 근무타입이 남아 있지 않음).
--     이 상태가 곧 코드 가드(ATTD_400_163)가 유지하려는 불변식이다.
-- ----------------------------------------------------------------------------
SELECT P.CMPNY_CD, P.SITE_CD, P.USER_CD, P.WORK_YMD, P.WORK_PLAN_CD
  FROM TB_USER_WORK_PLAN P
       INNER JOIN TB_SCH_MGMT S
       ON  S.CMPNY_CD = P.CMPNY_CD
       AND S.SITE_CD  = P.SITE_CD
       AND S.SCH_CD   = P.WORK_PLAN_CD
 WHERE P.WORK_YMD >= DATE_FORMAT(NOW(), '%Y%m%d')
   AND S.USE_YN    = 'N'
 ORDER BY P.CMPNY_CD, P.SITE_CD, P.USER_CD, P.WORK_YMD;
