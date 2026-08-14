-- =====================================================================================
-- 스토어 심사용: 데모 계정(ADMIN / SOON)이 앱 "일용직" 탭에서도 로그인되도록 하는 시드
--
-- 배경
--   Google Play 심사에서 심사자가 안내(정규 사용자 탭 사용)를 무시하고 "일용직" 탭으로
--   로그인을 시도 → 실패 → 반려. 안내문으로 막는 대신 그 탭에서도 열리게 만든다.
--
-- 방식 (A안)
--   일용직 로그인은 별도 EP(/prafta/comApi/dailyLogin/login)이고
--     · 인증 조회 = TB_DAILY_USER.USER_ID + USER_PW(BCrypt+pepper, 정규직과 동일 해시기)
--     · 인가/클레임 조회 = TB_USER 를 (CMPNY_CD, USER_CD) 로 조회
--   이므로, 기존 TB_USER 행을 그대로 재사용하는 TB_DAILY_USER 행 1개만 추가하면
--   같은 아이디·비밀번호로 양쪽 탭 모두 로그인된다. TB_USER 는 건드리지 않는다.
--
-- 부가 조건 (전부 불필요 — 코드 확인 완료)
--   · TB_DAILY_USER_SLOT 배정      : ACCOUNT_STATUS='01' 경로는 슬롯을 검사하지 않음
--   · TB_DAILY_ENTRY_REQUEST 승인  : '01' 이므로 승인 판정 자체를 하지 않음
--   · TB_DAILY_CONTRACT_* 서명     : 서버 게이트 없음, judgeSignGate 는 활성 계약서 없으면 skip
--
-- 특성 (의도된 것)
--   JWT 신분은 TB_USER 기준이라 일용직 탭으로 들어가도 employmentType='REGULAR' 이다.
--   즉 화면은 정규직 UX 로 뜬다. 심사 목적상 이미 검증된 화면이 그대로 뜨는 편이 안전하므로
--   의도한 동작이다. (진짜 DAILY 신분으로 띄우려면 TB_USER 행을 새로 만들어야 하는데,
--    슬롯·배정 없이 일용직 화면이 정상 렌더되는지 미검증이라 채택하지 않았다.)
--
-- ★ TB_USER.EMPLOYMENT_TYPE 을 'DAILY' 로 바꾸는 방식은 절대 쓰지 말 것.
--   정규 로그인 SQL(LoginMapper.xml:60)이 EMPLOYMENT_TYPE='DAILY' 를 차단하므로
--   정규 탭 로그인이 즉시 죽는다(비대칭 게이트).
--
-- 실행: 운영 DB 는 사용자가 Workbench 로 직접 수행. 본 파일은 실행하지 않는다.
-- 작성: 2026-08-14
-- =====================================================================================


-- -------------------------------------------------------------------------------------
-- STEP 0. 사전 확인 (INSERT 전에 반드시 눈으로 볼 것)
--   기대: 2행(ADMIN, SOON)이 나오고 SITE_USE_YN='Y', ALREADY_EXISTS=0
--   0행이면 운영의 아이디가 다른 것이므로 중단하고 실제 아이디를 확인할 것.
-- -------------------------------------------------------------------------------------
SELECT
      U.USER_ID
    , U.CMPNY_CD
    , U.USER_CD
    , U.SITE_CD
    , U.AUTH_CD
    , U.USE_YN                                          AS USER_USE_YN
    , U.ACCOUNT_STATUS
    , U.EMPLOYMENT_TYPE
    , S.USE_YN                                          AS SITE_USE_YN
    , (SELECT COUNT(1)
         FROM TB_DAILY_USER D
        WHERE D.CMPNY_CD = U.CMPNY_CD
          AND D.USER_CD  = U.USER_CD)                   AS ALREADY_EXISTS
  FROM TB_USER U
  LEFT JOIN TB_SITE S
    ON S.CMPNY_CD = U.CMPNY_CD
   AND S.SITE_CD  = U.SITE_CD
 WHERE U.USER_ID IN ('ADMIN', 'SOON')
 LIMIT 10;


-- -------------------------------------------------------------------------------------
-- STEP 1. TB_DAILY_USER 행 생성 (TB_USER 에서 값을 그대로 승계 — 하드코딩 없음)
--
--   · USER_PW  : TB_USER 의 해시를 그대로 복사 → 같은 평문으로 인증됨
--   · MBL_NO_* : 전부 NULL. 로그인에 미사용이며,
--                UX_TB_DAILY_USER_MBL (CMPNY_CD, if(USE_YN='Y', MBL_NO_HMAC, NULL)) 유니크
--                충돌을 피하기 위해 반드시 NULL 로 둔다.
--   · USER_NM  : 심사자 화면 노출 가능성을 고려해 영문 리터럴 사용
--                (1차 반려 사유가 "이름이 영문이 아님" 이었음 — PII 복사도 회피)
--   · WORK_EXPIRE_DATE : 자정 만료 배치(WORK_EXPIRE_DATE < 오늘 → USE_YN='N')를 피하려
--                        충분한 미래로 둔다. 이 값이 지나면 일용직 탭 로그인이 조용히 죽는다.
--   · NOT EXISTS 가드가 있어 재실행해도 중복 INSERT 되지 않는다.
-- -------------------------------------------------------------------------------------
INSERT INTO TB_DAILY_USER (
      CMPNY_CD
    , SITE_CD
    , USER_CD
    , USER_ID
    , USER_NM
    , USER_PW
    , MBL_NO_ENC
    , MBL_NO_HMAC
    , MBL_NO_LAST4
    , REG_TYPE
    , USE_YN
    , ACCOUNT_STATUS
    , WORK_EXPIRE_DATE
    , WITHDRAWAL_DATE
    , PWD_FAIL_CNT
    , PWD_LOCK_YN
    , PWD_LOCK_EXPIRE_DTIME
    , INSERT_NO
    , INSERT_DATE
)
SELECT
      U.CMPNY_CD
    , U.SITE_CD
    , U.USER_CD
    , U.USER_ID
    , CONCAT('Store Review ', U.USER_ID)                AS USER_NM
    , U.USER_PW
    , NULL                                              AS MBL_NO_ENC
    , NULL                                              AS MBL_NO_HMAC
    , NULL                                              AS MBL_NO_LAST4
    , '01'                                              AS REG_TYPE
    , 'Y'                                               AS USE_YN
    , '01'                                              AS ACCOUNT_STATUS
    , '20991231'                                        AS WORK_EXPIRE_DATE
    , NULL                                              AS WITHDRAWAL_DATE
    , 0                                                 AS PWD_FAIL_CNT
    , 'N'                                               AS PWD_LOCK_YN
    , NULL                                              AS PWD_LOCK_EXPIRE_DTIME
    , 'STORE_REVIEW'                                    AS INSERT_NO
    , NOW()                                             AS INSERT_DATE
  FROM TB_USER U
  INNER JOIN TB_SITE S
    ON S.CMPNY_CD = U.CMPNY_CD
   AND S.SITE_CD  = U.SITE_CD
   AND S.USE_YN   = 'Y'
 WHERE U.USER_ID IN ('ADMIN', 'SOON')
   AND U.USE_YN         = 'Y'
   AND U.ACCOUNT_STATUS = '01'
   AND NOT EXISTS (
           SELECT 1
             FROM TB_DAILY_USER D
            WHERE D.CMPNY_CD = U.CMPNY_CD
              AND D.USER_CD  = U.USER_CD
       );


-- -------------------------------------------------------------------------------------
-- STEP 2. 결과 확인
--   기대: 2행, USE_YN='Y', ACCOUNT_STATUS='01', MBL_NO_HMAC IS NULL, WORK_EXPIRE_DATE='20991231'
-- -------------------------------------------------------------------------------------
SELECT
      D.USER_ID
    , D.CMPNY_CD
    , D.SITE_CD
    , D.USER_CD
    , D.USER_NM
    , D.USE_YN
    , D.ACCOUNT_STATUS
    , D.WORK_EXPIRE_DATE
    , D.PWD_LOCK_YN
    , CASE WHEN D.USER_PW = U.USER_PW THEN 'SAME' ELSE 'DIFF' END   AS PW_MATCH_TB_USER
  FROM TB_DAILY_USER D
  INNER JOIN TB_USER U
    ON U.CMPNY_CD = D.CMPNY_CD
   AND U.USER_CD  = D.USER_CD
 WHERE D.INSERT_NO = 'STORE_REVIEW'
 LIMIT 10;


-- -------------------------------------------------------------------------------------
-- STEP 3. 롤백 (필요 시에만 주석 해제)
--   INSERT_NO='STORE_REVIEW' 로 본 시드가 만든 행만 정확히 지운다.
--   TB_USER 는 애초에 건드리지 않았으므로 정규 탭 로그인에는 아무 영향이 없다.
-- -------------------------------------------------------------------------------------
-- DELETE FROM TB_DAILY_USER WHERE INSERT_NO = 'STORE_REVIEW';
