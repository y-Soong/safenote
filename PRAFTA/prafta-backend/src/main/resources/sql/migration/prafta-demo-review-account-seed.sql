-- ============================================================================
-- prafta-demo-review-account-seed.sql
-- 플레이스토어 심사용 데모 계정 시드 (plan: 작업지시서_플레이스토어-데모계정-AAB.plan.md §3 T1~T4)
-- 작성: developer (2026-07-22) / 실행: 사용자 Workbench 수동
-- CMPNY_CD = 6NQaZGt7L5STZqgekcbv (2026-07-22 사용자 확인 완료, 프로비저닝 발급값으로 전체 치환 완료)
-- ============================================================================
--
-- [실행 전제]
--   S1. sysadmin 웹(플랫폼 회사등록)에서 데모 회사 프로비저닝을 먼저 실행한다.
--       (회사명 "DEMO 심사용(리뷰 전용)", adminId=DEMOADMIN — plan §4.1) — 완료됨(CMPNY_CD 확보).
--       프로비저닝이 22테이블 정합 시드를 전부 담당하므로 본 파일은 "잔여분"만 처리한다.
--
-- [실행 순서 — STEP 블록 단위로 나눠 실행]
--   STEP A : 관리자(DEMOADMIN) 게이트 해소 + 지오펜스 무력화
--   (S3)   : DEMOADMIN 으로 웹 로그인 → User_01 에서 일반 사용자 DEMOUSER1 등록
--   STEP B : DEMOUSER1 게이트 해소  (S3 이후에만 유효 — 이전 실행 시 0건 무동작)
--   STEP C : 데모 데이터 시드 (근무계획 + 근태 3일치, 연차 부여 1건)
--   STEP D : (실행 금지) 심사 종료 후 정리용 DELETE — 전 문장 주석 상태로 배포
--
-- [주의]
--   - 실행 전 Workbench 에서 DESCRIBE 로 대상 테이블 컬럼을 최종 확인할 것
--     (예: DESCRIBE TB_USER; DESCRIBE TB_SITE; DESCRIBE TB_USER_LEAVE_GRANT;)
--     — schema-full.sql 스냅샷이 stale 이라 본 파일은 매퍼 XML + 실 DB DESCRIBE 근거로 작성됨.
--   - 전 INSERT 는 NOT EXISTS 멱등 — 재실행해도 중복 생성되지 않는다.
--   - 사용자변수(@x)와 컬럼 비교는 콜레이션 충돌(1267) 위험이 있어 미사용 — 리터럴만 사용.
--   - FNC_CMM_SEQ_NEXTVAL 은 호출 시 시퀀스를 소모한다(스킵된 행 몫은 채번 gap — 무해).
-- ============================================================================


-- ============================================================================
-- STEP A : 데모 회사 · 관리자(DEMOADMIN) 게이트 해소 + 지오펜스 무력화
--   게이트 근거(plan §2 매트릭스):
--     PHONE_AUTH   : ACCOUNT_STATUS='04' → '01' (LoginServiceImpl.Login L131)
--     강제 비번변경 : PWD_CHG_DTIME IS NULL → NOW() (AuthAspect / AuthMapper.selectGateUserInfo)
--     DEFAULT_SCH  : DEFAULT_SCH_CD IS NULL → 'ST001' (프로비저닝 시드 근무타입, 09:00~18:00)
--     약관 동의    : TB_TERMS_USER_AGR_MGMT 동의행 시드 (AuthMapper.countWebPendingTerms / countAppPendingRequiredTerms)
--     GPS 지오펜스 : TB_SITE LAT/LON/GPS_RANGE 결측 → 온사이트 폴백 (AppAttd01ServiceImpl.isOutsideGeofence)
-- ============================================================================

-- A-1. DEMOADMIN 게이트 3종 해소 (본인인증 / 강제 비번변경 / 기본 근무타입)
--   - IFNULL 로 기존값 보존(멱등·재실행 안전).
--   - MySQL UPDATE SET 은 좌→우 순차 반영이므로 DEFAULT_SCH_SET_DATE(구 DEFAULT_SCH_CD 참조)를
--     DEFAULT_SCH_CD 갱신보다 먼저 둔다.
UPDATE TB_USER
   SET ACCOUNT_STATUS       = '01'
     , PWD_CHG_DTIME        = IFNULL(PWD_CHG_DTIME, NOW())
     , DEFAULT_SCH_SET_DATE = CASE WHEN DEFAULT_SCH_CD IS NULL THEN NOW()
                                   ELSE IFNULL(DEFAULT_SCH_SET_DATE, NOW()) END
     , DEFAULT_SCH_CD       = IFNULL(DEFAULT_SCH_CD, 'ST001')
     , UPDATE_NO            = 'DEMO_SEED'
     , UPDATE_DATE          = NOW()
 WHERE CMPNY_CD = '6NQaZGt7L5STZqgekcbv'
   AND USER_ID  = 'DEMOADMIN';

-- A-2. DEMOADMIN 약관 동의 이력 시드
--   - 대상 = USE_YN='Y' 전체 약관 (선택약관 006 포함 — 웹/앱 게이트 판정의 상위집합이라 양쪽 자동 충족.
--     데모 계정 실소유자는 운영자 본인이므로 대리 동의 문제 없음. plan §2.1)
--   - 컬럼 순서 = LoginMapper.insertTermsUserAgrMgmt 미러.
--   - 현재버전(TERMS_VERSION) 자동 추종 — 약관 개정 후 재실행하면 신버전 동의행이 추가된다(멱등).
INSERT INTO TB_TERMS_USER_AGR_MGMT (
      CMPNY_CD
    , USER_CD
    , TERMS_ID
    , TERMS_VERSION
    , AGR_YN
    , INSERT_NO
    , INSERT_DATE
)
SELECT
      U.CMPNY_CD
    , U.USER_CD
    , T.TERMS_ID
    , T.TERMS_VERSION
    , 'Y'
    , 'DEMO_SEED'
    , NOW()
  FROM TB_TERMS T
  JOIN TB_USER U
    ON U.CMPNY_CD = '6NQaZGt7L5STZqgekcbv'
   AND U.USER_ID  = 'DEMOADMIN'
 WHERE T.USE_YN = 'Y'
   AND NOT EXISTS (
        SELECT 1
          FROM TB_TERMS_USER_AGR_MGMT G
         WHERE G.CMPNY_CD      = U.CMPNY_CD
           AND G.USER_CD       = U.USER_CD
           AND G.TERMS_ID      = T.TERMS_ID
           AND G.TERMS_VERSION = T.TERMS_VERSION
       );

-- A-3. 지오펜스 무력화 — 좌표/반경 결측 → 온사이트 폴백(A안). 해외 심사관도 정상 출퇴근 가능.
--   (프로비저닝 SiteInsertCommand 는 좌표를 넣지 않아 보통 이미 NULL — 방어적 실행)
--   ★ Workbench safe update mode(sql_safe_updates) 주의: TB_SITE PK=(CMPNY_CD,SITE_CD) 복합키라
--     CMPNY_CD 단독 WHERE 는 "키 미사용"으로 판정되어 1175 에러가 난다. Preferences 에서 영구로 끄지
--     말고, 이 문장에서만 세션 단위로 토글할 것(안전장치는 문장 실행 직후 즉시 복구).
SET SQL_SAFE_UPDATES = 0;

UPDATE TB_SITE
   SET LAT         = NULL
     , LON         = NULL
     , GPS_RANGE   = NULL
     , UPDATE_NO   = 'DEMO_SEED'
     , UPDATE_DATE = NOW()
 WHERE CMPNY_CD = '6NQaZGt7L5STZqgekcbv';

SET SQL_SAFE_UPDATES = 1;

-- A-4. 검증 SELECT ------------------------------------------------------------

-- (1) 게이트 컬럼 확인 — 기대: ACCOUNT_STATUS='01', PWD_CHG_DTIME NOT NULL,
--     DEFAULT_SCH_CD='ST001', DEFAULT_SCH_SET_DATE NOT NULL, PWD_LOCK_YN='N', USE_YN='Y'
SELECT USER_ID
     , ACCOUNT_STATUS
     , PWD_CHG_DTIME
     , DEFAULT_SCH_CD
     , DEFAULT_SCH_SET_DATE
     , PWD_LOCK_YN
     , USE_YN
  FROM TB_USER
 WHERE CMPNY_CD = '6NQaZGt7L5STZqgekcbv'
   AND USER_ID  = 'DEMOADMIN';

-- (2) 웹 약관 게이트 동치 쿼리 (AuthMapper.countWebPendingTerms 미러: REQUIRED_YN='Y' + USE_YN='Y'
--     + (CMPNY_CD, USER_CD, 현재버전) 동의행 부재 집계) — 기대: 0
SELECT COUNT(1) AS WEB_PENDING_TERMS
  FROM TB_TERMS A
       LEFT OUTER JOIN TB_TERMS_USER_AGR_MGMT B
          ON (
              A.TERMS_ID      = B.TERMS_ID
          AND A.TERMS_VERSION = B.TERMS_VERSION
          AND B.CMPNY_CD      = '6NQaZGt7L5STZqgekcbv'
          AND B.USER_CD       = (SELECT U.USER_CD FROM TB_USER U
                                  WHERE U.CMPNY_CD = '6NQaZGt7L5STZqgekcbv' AND U.USER_ID = 'DEMOADMIN')
          )
 WHERE A.REQUIRED_YN = 'Y'
   AND A.USE_YN      = 'Y'
   AND B.TERMS_ID IS NULL;

-- (3) 앱 약관 게이트 동치 쿼리 (AuthMapper.countAppPendingRequiredTerms 미러: SYS008 한정
--     + AGR_YN='Y' 동의행 부재 집계) — 기대: 0
SELECT COUNT(1) AS APP_PENDING_TERMS
  FROM TB_TERMS B
       INNER JOIN TB_SYST_VAL_D S
          ON (
              S.SYST_VAL_CD   = 'SYS008'
          AND S.SYST_VAL_D_CD = B.TERMS_ID
          )
       LEFT OUTER JOIN TB_TERMS_USER_AGR_MGMT A
          ON (
              A.TERMS_ID      = B.TERMS_ID
          AND A.TERMS_VERSION = B.TERMS_VERSION
          AND A.CMPNY_CD      = '6NQaZGt7L5STZqgekcbv'
          AND A.USER_CD       = (SELECT U.USER_CD FROM TB_USER U
                                  WHERE U.CMPNY_CD = '6NQaZGt7L5STZqgekcbv' AND U.USER_ID = 'DEMOADMIN')
          AND A.AGR_YN        = 'Y'
          )
 WHERE B.REQUIRED_YN = 'Y'
   AND B.USE_YN      = 'Y'
   AND A.USER_CD IS NULL;

-- (4) 지오펜스 결측 + 사업장 활성 확인 — 기대: LAT/LON/GPS_RANGE 전부 NULL, USE_YN='Y'
SELECT SITE_CD
     , SITE_NM
     , LAT
     , LON
     , GPS_RANGE
     , USE_YN
  FROM TB_SITE
 WHERE CMPNY_CD = '6NQaZGt7L5STZqgekcbv';


-- ============================================================================
-- STEP B : 일반 사용자(DEMOUSER1) 게이트 해소
--   ★ 선행조건: S3 (DEMOADMIN 웹 로그인 → User_01 에서 DEMOUSER1 등록) 완료 후 실행.
--     DEMOUSER1 미존재 상태에서 실행하면 전 문장 0건 무동작(안전).
--   지오펜스는 STEP A-3 에서 회사 전체 처리 완료 — 본 STEP 은 사용자 게이트만.
-- ============================================================================

-- B-1. DEMOUSER1 게이트 3종 해소 (A-1 동일 패턴)
--   - User_01 등록 화면에서 기본 근무타입을 이미 지정했을 수 있어 IFNULL 로 기존값 보존.
UPDATE TB_USER
   SET ACCOUNT_STATUS       = '01'
     , PWD_CHG_DTIME        = IFNULL(PWD_CHG_DTIME, NOW())
     , DEFAULT_SCH_SET_DATE = CASE WHEN DEFAULT_SCH_CD IS NULL THEN NOW()
                                   ELSE IFNULL(DEFAULT_SCH_SET_DATE, NOW()) END
     , DEFAULT_SCH_CD       = IFNULL(DEFAULT_SCH_CD, 'ST001')
     , UPDATE_NO            = 'DEMO_SEED'
     , UPDATE_DATE          = NOW()
 WHERE CMPNY_CD = '6NQaZGt7L5STZqgekcbv'
   AND USER_ID  = 'DEMOUSER1';

-- B-2. DEMOUSER1 약관 동의 이력 시드 (A-2 동일 패턴)
INSERT INTO TB_TERMS_USER_AGR_MGMT (
      CMPNY_CD
    , USER_CD
    , TERMS_ID
    , TERMS_VERSION
    , AGR_YN
    , INSERT_NO
    , INSERT_DATE
)
SELECT
      U.CMPNY_CD
    , U.USER_CD
    , T.TERMS_ID
    , T.TERMS_VERSION
    , 'Y'
    , 'DEMO_SEED'
    , NOW()
  FROM TB_TERMS T
  JOIN TB_USER U
    ON U.CMPNY_CD = '6NQaZGt7L5STZqgekcbv'
   AND U.USER_ID  = 'DEMOUSER1'
 WHERE T.USE_YN = 'Y'
   AND NOT EXISTS (
        SELECT 1
          FROM TB_TERMS_USER_AGR_MGMT G
         WHERE G.CMPNY_CD      = U.CMPNY_CD
           AND G.USER_CD       = U.USER_CD
           AND G.TERMS_ID      = T.TERMS_ID
           AND G.TERMS_VERSION = T.TERMS_VERSION
       );

-- B-3. 검증 SELECT ------------------------------------------------------------

-- (1) 게이트 컬럼 확인 — 기대: A-4(1) 과 동일 기준 (1행 반환 — 0행이면 S3 미수행)
SELECT USER_ID
     , ACCOUNT_STATUS
     , PWD_CHG_DTIME
     , DEFAULT_SCH_CD
     , DEFAULT_SCH_SET_DATE
     , PWD_LOCK_YN
     , USE_YN
  FROM TB_USER
 WHERE CMPNY_CD = '6NQaZGt7L5STZqgekcbv'
   AND USER_ID  = 'DEMOUSER1';

-- (2) 웹 약관 게이트 동치 — 기대: 0
SELECT COUNT(1) AS WEB_PENDING_TERMS
  FROM TB_TERMS A
       LEFT OUTER JOIN TB_TERMS_USER_AGR_MGMT B
          ON (
              A.TERMS_ID      = B.TERMS_ID
          AND A.TERMS_VERSION = B.TERMS_VERSION
          AND B.CMPNY_CD      = '6NQaZGt7L5STZqgekcbv'
          AND B.USER_CD       = (SELECT U.USER_CD FROM TB_USER U
                                  WHERE U.CMPNY_CD = '6NQaZGt7L5STZqgekcbv' AND U.USER_ID = 'DEMOUSER1')
          )
 WHERE A.REQUIRED_YN = 'Y'
   AND A.USE_YN      = 'Y'
   AND B.TERMS_ID IS NULL;

-- (3) 앱 약관 게이트 동치 — 기대: 0
SELECT COUNT(1) AS APP_PENDING_TERMS
  FROM TB_TERMS B
       INNER JOIN TB_SYST_VAL_D S
          ON (
              S.SYST_VAL_CD   = 'SYS008'
          AND S.SYST_VAL_D_CD = B.TERMS_ID
          )
       LEFT OUTER JOIN TB_TERMS_USER_AGR_MGMT A
          ON (
              A.TERMS_ID      = B.TERMS_ID
          AND A.TERMS_VERSION = B.TERMS_VERSION
          AND A.CMPNY_CD      = '6NQaZGt7L5STZqgekcbv'
          AND A.USER_CD       = (SELECT U.USER_CD FROM TB_USER U
                                  WHERE U.CMPNY_CD = '6NQaZGt7L5STZqgekcbv' AND U.USER_ID = 'DEMOUSER1')
          AND A.AGR_YN        = 'Y'
          )
 WHERE B.REQUIRED_YN = 'Y'
   AND B.USE_YN      = 'Y'
   AND A.USER_CD IS NULL;


-- ============================================================================
-- STEP C : 데모 데이터 시드 (앱 홈/근태/연차 화면이 비지 않기 위한 최소치, plan §3 T3)
--   ★ 선행조건: STEP A·B 완료 (DEMOADMIN + DEMOUSER1 존재).
--
--   [근태 날짜 방식 — 동적 계산 채택]
--   실행일 기준 직전 7일 중 평일(월~금)만 골라 최근 3일을 사용한다.
--   (7일 창에는 항상 평일이 5일 있어 3일 확보가 보장되고, 사용자 치환 실수 여지가 없다.
--    DAYOFWEEK: 1=일, 7=토 → NOT IN (1,7) = 평일.)
--   C-1(근무계획)과 C-2(근태)가 같은 파생테이블 식을 반복 사용하므로 같은 날짜가 보장된다.
-- ============================================================================

-- C-1. 과거 근무계획 3일치 × 2인 (DEMOADMIN + DEMOUSER1)
--   - 컬럼 = DefaultSchGenMapper.insertDefaultSchDayIfAbsent 미러 + 실 DB DESCRIBE 확정
--     (PK=(CMPNY_CD,SITE_CD,USER_CD,WORK_YMD), 이외 WORK_PLAN_CD/GEN_SOURCE/감사 4컬럼이 전부).
--   - GEN_SOURCE='MANUAL': 'DEFAULT_SCH' 로 넣으면 기본근무타입 변경 시 자동갱신 대상이 된다
--     (updateFutureDefaultSch) — 시드는 수동 성격으로 보존.
INSERT INTO TB_USER_WORK_PLAN (
      CMPNY_CD
    , SITE_CD
    , USER_CD
    , WORK_YMD
    , WORK_PLAN_CD
    , GEN_SOURCE
    , INSERT_NO
    , INSERT_DATE
    , UPDATE_NO
    , UPDATE_DATE
)
SELECT
      U.CMPNY_CD
    , U.SITE_CD
    , U.USER_CD
    , DT.YMD
    , 'ST001'
    , 'MANUAL'
    , 'DEMO_SEED'
    , NOW()
    , 'DEMO_SEED'
    , NOW()
  FROM TB_USER U
  CROSS JOIN (
        SELECT D.YMD
          FROM (
                SELECT DATE_FORMAT(DATE_SUB(CURDATE(), INTERVAL N.n DAY), '%Y%m%d') AS YMD
                     , DAYOFWEEK(DATE_SUB(CURDATE(), INTERVAL N.n DAY))             AS DW
                  FROM (
                        SELECT 1 AS n UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
                        UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7
                       ) N
               ) D
         WHERE D.DW NOT IN (1, 7)          -- 평일만 (1=일, 7=토)
         ORDER BY D.YMD DESC
         LIMIT 3
       ) DT
 WHERE U.CMPNY_CD = '6NQaZGt7L5STZqgekcbv'
   AND U.USER_ID IN ('DEMOADMIN', 'DEMOUSER1')
   AND U.SITE_CD IS NOT NULL
   AND NOT EXISTS (
        SELECT 1
          FROM TB_USER_WORK_PLAN WP
         WHERE WP.CMPNY_CD = U.CMPNY_CD
           AND WP.SITE_CD  = U.SITE_CD
           AND WP.USER_CD  = U.USER_CD
           AND WP.WORK_YMD = DT.YMD
       );

-- C-2. 과거 근태 3일치 × 2인 (C-1 과 같은 날짜, 09:00 출근 / 18:00 퇴근)
--   - ATTD_ID 채번 = Attd07Mapper.selectAttdId 미러 (YYYYMMDD + FNC_CMM_SEQ_NEXTVAL('ATTD_ID')).
--   - CHECK_IN/OUT_METHOD='01'(SYS031 코드마스터: 01=사용자등록/02=관리자생성/03=QR체크 3개 등록돼 있으나,
--     실제 CHECK_IN/OUT_METHOD 값으로는 '01'만 코드 전반에서 사용됨 — self 앱 체크인 값이자
--     Attd07ServiceImpl.ATTD_METHOD_DEFAULT="01"(관리자 보정/백필 기본값)로 확립된 관례값.
--     '02'/'03'는 코드마스터엔 존재하나 CHECK_IN/OUT_METHOD 실사용 코드에서는 쓰이지 않음(QA 재검증 완료).
--   - NODE_CD 는 사용자 행의 소속부서를 그대로 사용 (프로비저닝 최초 노드='n1').
--   - 멱등: PK 는 ATTD_ID 단독이라 논리키(회사/사업장/사용자/근무일/차수/DEL_YN='N')로 NOT EXISTS 검사.
INSERT INTO TB_USER_ATTD_MGMT (
      ATTD_ID
    , CMPNY_CD
    , SITE_CD
    , USER_CD
    , WORK_YMD
    , NODE_CD
    , WORK_SEQ
    , CHECK_IN_DATE
    , CHECK_IN_TIME
    , CHECK_IN_METHOD
    , CHECK_OUT_DATE
    , CHECK_OUT_TIME
    , CHECK_OUT_METHOD
    , DEL_YN
    , INSERT_NO
    , INSERT_DATE
    , UPDATE_NO
    , UPDATE_DATE
)
SELECT
      CONCAT(DATE_FORMAT(NOW(), '%Y%m%d'), FNC_CMM_SEQ_NEXTVAL('6NQaZGt7L5STZqgekcbv', 'ATTD_ID'))
    , U.CMPNY_CD
    , U.SITE_CD
    , U.USER_CD
    , DT.YMD
    , U.NODE_CD
    , 1
    , DT.YMD
    , '0900'
    , '01'
    , DT.YMD
    , '1800'
    , '01'
    , 'N'
    , 'DEMO_SEED'
    , NOW()
    , 'DEMO_SEED'
    , NOW()
  FROM TB_USER U
  CROSS JOIN (
        SELECT D.YMD
          FROM (
                SELECT DATE_FORMAT(DATE_SUB(CURDATE(), INTERVAL N.n DAY), '%Y%m%d') AS YMD
                     , DAYOFWEEK(DATE_SUB(CURDATE(), INTERVAL N.n DAY))             AS DW
                  FROM (
                        SELECT 1 AS n UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
                        UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7
                       ) N
               ) D
         WHERE D.DW NOT IN (1, 7)          -- 평일만 (1=일, 7=토)
         ORDER BY D.YMD DESC
         LIMIT 3
       ) DT
 WHERE U.CMPNY_CD = '6NQaZGt7L5STZqgekcbv'
   AND U.USER_ID IN ('DEMOADMIN', 'DEMOUSER1')
   AND U.SITE_CD IS NOT NULL
   AND NOT EXISTS (
        SELECT 1
          FROM TB_USER_ATTD_MGMT M
         WHERE M.CMPNY_CD = U.CMPNY_CD
           AND M.SITE_CD  = U.SITE_CD
           AND M.USER_CD  = U.USER_CD
           AND M.WORK_YMD = DT.YMD
           AND M.WORK_SEQ = 1
           AND M.DEL_YN   = 'N'
       );

-- C-3. 연차 수동부여 1건 (DEMOUSER1, 15일 — 부여 정책 계산을 우회하는 관리자 수동 경로.
--       정책서 08-leave.md §8.1·§8.2 — 정책 위반 아님)
--   - 컬럼/값 = LeaveDashboardMapper.insertManualGrant 미러 + 실 DB SHOW CREATE 확정.
--   - GRANT_ID 채번 = LeaveDashboardMapper.selectNextGrantId 미러
--     ('G' + YYYYMMDD + FNC_CMM_SEQ_NEXTVAL(cmpnyCd,'LEAVE_GRANT_ID') — 코드 원문과 100% 동일 패턴).
--   - GRANT_TYPE='MANUAL_OTHER' — LeaveDashboardServiceImpl.MANUAL_GRANT_TYPE 상수값과 정확히 일치
--     (코드 근거 확인 완료, 추측 아님). GRANT_BY_TYPE='02'(관리자 수동).
--   - 멱등: UK=(CMPNY_CD, IDEMPOTENCY_KEY) — 'DEMOUSER1_REVIEW_SEED' 고정값으로 검사.
INSERT INTO TB_USER_LEAVE_GRANT (
      GRANT_ID
    , CMPNY_CD
    , USER_CD
    , LEAVE_CD
    , GRANT_TYPE
    , GRANT_DAYS
    , USED_DAYS
    , GRANT_REASON
    , GRANT_BY_TYPE
    , POLICY_SEQ
    , GRANT_DATE
    , AVAIL_FROM_DATE
    , AVAIL_TO_DATE
    , IDEMPOTENCY_KEY
    , STATUS
    , EXPIRE_YN
    , DEL_YN
    , INSERT_NO
    , INSERT_DATE
)
SELECT
      CONCAT('G', DATE_FORMAT(NOW(), '%Y%m%d'), FNC_CMM_SEQ_NEXTVAL('6NQaZGt7L5STZqgekcbv', 'LEAVE_GRANT_ID'))
    , U.CMPNY_CD
    , U.USER_CD
    , 'SYS_ANNUAL'                                            -- 프로비저닝 시드 시스템 연차 (seedSystemLeaveTypes)
    , 'MANUAL_OTHER'
    , 15
    , 0
    , '플레이스토어 심사용 데모 부여'
    , '02'
    , NULL
    , DATE_FORMAT(CURDATE(), '%Y%m%d')
    , DATE_FORMAT(CURDATE(), '%Y%m%d')
    , DATE_FORMAT(DATE_ADD(CURDATE(), INTERVAL 1 YEAR), '%Y%m%d')
    , 'DEMOUSER1_REVIEW_SEED'
    , 'ACTIVE'
    , 'N'
    , 'N'
    , 'DEMO_SEED'
    , NOW()
  FROM TB_USER U
 WHERE U.CMPNY_CD = '6NQaZGt7L5STZqgekcbv'
   AND U.USER_ID  = 'DEMOUSER1'
   AND NOT EXISTS (
        SELECT 1
          FROM TB_USER_LEAVE_GRANT G
         WHERE G.CMPNY_CD        = U.CMPNY_CD
           AND G.IDEMPOTENCY_KEY = 'DEMOUSER1_REVIEW_SEED'
       );

-- C-4. 검증 SELECT ------------------------------------------------------------
--   ※ TBM 세션·공지·연차 신청은 SQL 시드하지 않는다 — S6 에서 화면 직접 생성 (plan §5).

-- (1) 근무계획 — 기대: 6행 (2인 × 평일 3일), WORK_PLAN_CD='ST001', GEN_SOURCE='MANUAL'
SELECT WP.USER_CD
     , WP.WORK_YMD
     , WP.WORK_PLAN_CD
     , WP.GEN_SOURCE
  FROM TB_USER_WORK_PLAN WP
 WHERE WP.CMPNY_CD  = '6NQaZGt7L5STZqgekcbv'
   AND WP.INSERT_NO = 'DEMO_SEED'
 ORDER BY WP.USER_CD, WP.WORK_YMD;

-- (2) 근태 — 기대: 6행, 0900/1800, METHOD '01'/'01', DEL_YN='N'
SELECT M.ATTD_ID
     , M.USER_CD
     , M.WORK_YMD
     , M.WORK_SEQ
     , M.CHECK_IN_TIME
     , M.CHECK_IN_METHOD
     , M.CHECK_OUT_TIME
     , M.CHECK_OUT_METHOD
     , M.DEL_YN
  FROM TB_USER_ATTD_MGMT M
 WHERE M.CMPNY_CD  = '6NQaZGt7L5STZqgekcbv'
   AND M.INSERT_NO = 'DEMO_SEED'
 ORDER BY M.USER_CD, M.WORK_YMD;

-- (3) 연차 부여 — 기대: 1행, GRANT_DAYS=15, USED_DAYS=0, STATUS='ACTIVE', LEAVE_CD='SYS_ANNUAL'
SELECT G.GRANT_ID
     , G.USER_CD
     , G.LEAVE_CD
     , G.GRANT_TYPE
     , G.GRANT_BY_TYPE
     , G.GRANT_DAYS
     , G.USED_DAYS
     , G.GRANT_DATE
     , G.AVAIL_FROM_DATE
     , G.AVAIL_TO_DATE
     , G.STATUS
  FROM TB_USER_LEAVE_GRANT G
 WHERE G.CMPNY_CD        = '6NQaZGt7L5STZqgekcbv'
   AND G.IDEMPOTENCY_KEY = 'DEMOUSER1_REVIEW_SEED';


-- ============================================================================
-- STEP D : 정리용 DELETE 블록 — ★★ 실행 금지 (전 문장 주석 상태로 배포) ★★
--   심사 종료 후에만 주석 해제하여 실행한다. 데모 회사(CMPNY_CD='6NQaZGt7L5STZqgekcbv') 단위 전수 삭제.
--   ★ 주석 해제 전 반드시 확인: 6NQaZGt7L5STZqgekcbv 가 "데모 회사" 코드인지 재확인
--     (SELECT CMPNY_NM FROM TB_CMPNY WHERE CMPNY_CD='6NQaZGt7L5STZqgekcbv'; → "DEMO 심사용(리뷰 전용)" 이어야 함).
--   순서: 자식(사용/화면/로그인 부산물) → 부모(프로비저닝 산출물) → TB_CMPNY 마지막.
--   대상 = CompanyProvisionMapper.xml INSERT 전수 + 본 시드 테이블 + 로그인 부산물(LoginMapper.xml)
--        + 화면 생성분(공지 Notice01Mapper / TBM AppAdminTbmMapper·Tbm01·02 / 연차신청 LeaveFlow·ApprovalLine
--        / 출퇴근 AppAttd01 / 푸시설정 Notiset01 / 알림 NOTI_OUTBOX).
--   ※ TB_TBM_SESSION_SHARE 는 CMPNY_CD 컬럼이 없다(HOST_CMPNY_CD/SHARE_CMPNY_CD/DESIGNATED_BY_CMPNY_CD
--      3분할 구조, TbmSessionShareMapper.xml 확인) — 3컬럼 OR 조건으로 삭제. 데모 회사는 하도급(subcon)
--      관계를 맺지 않으므로 통상 0건이나, 방어적으로 포함한다.
--   ※ TB_TBM_EDU_MTRL_ITEM 은 CMPNY_CD 컬럼이 없다(MTRL_CD 로만 상위 TB_TBM_EDU_MTRL 에 연결,
--      FK_TBM_EDU_MTRL_ITEM_01 ON DELETE CASCADE 존재 — TB_TBM_EDU_MTRL 삭제만으로도 자동 정리되지만
--      명시적 삭제도 함께 남겨 가독성을 유지한다) — MTRL_CD 서브쿼리로 삭제.
-- ============================================================================

-- ---------- D-0. ★필수 안전확인★ 주석 해제 전 반드시 이 SELECT 를 먼저 실행하고
--   결과가 "DEMO 심사용(리뷰 전용)" 인지 육안 확인한 뒤에만 아래 DELETE 문들을 해제할 것.
--   (CMPNY_CD 오기입 시 전사 데이터 삭제로 이어질 수 있는 파급범위 — security 권고 반영)
--   ★ Workbench safe update mode 주의: 아래 DELETE 문 대부분이 CMPNY_CD 단독 조건이라(대상 테이블
--     PK 가 복합키) 1175 에러가 날 수 있다. D-0 확인 SELECT 로 회사가 맞는지 검증한 직후,
--     `SET SQL_SAFE_UPDATES = 0;` 으로 이 STEP D 실행 구간만 풀고, D-6 마지막 문장 실행 후
--     `SET SQL_SAFE_UPDATES = 1;` 로 즉시 복구할 것(Preferences 영구 해제 금지, A-3 사례와 동일 패턴).
-- SELECT CMPNY_CD, CMPNY_NM FROM TB_CMPNY WHERE CMPNY_CD = '6NQaZGt7L5STZqgekcbv';

-- ---------- D-1. 알림/푸시/디바이스/토큰 (로그인·사용 부산물) ----------
-- DELETE FROM TB_NOTI_OUTBOX                       WHERE CMPNY_CD = '6NQaZGt7L5STZqgekcbv';
-- DELETE FROM TB_USER_PUSH_SETTING                 WHERE CMPNY_CD = '6NQaZGt7L5STZqgekcbv';
-- DELETE FROM TB_USER_DEVICE_OCCUPANCY_ANOMALY     WHERE CMPNY_CD = '6NQaZGt7L5STZqgekcbv';
-- DELETE FROM TB_USER_DEVICE_LOGIN_HIST            WHERE CMPNY_CD = '6NQaZGt7L5STZqgekcbv';
-- DELETE FROM TB_USER_DEVICE                       WHERE CMPNY_CD = '6NQaZGt7L5STZqgekcbv';
-- DELETE FROM TB_AUTH_TOKEN                        WHERE CMPNY_CD = '6NQaZGt7L5STZqgekcbv';

-- ---------- D-2. 공지 (S6 화면 생성분) ----------
-- DELETE FROM TB_NOTICE_USER_ACK                   WHERE CMPNY_CD = '6NQaZGt7L5STZqgekcbv';
-- DELETE FROM TB_NOTICE_FILE                       WHERE CMPNY_CD = '6NQaZGt7L5STZqgekcbv';
-- DELETE FROM TB_NOTICE_TARGET                     WHERE CMPNY_CD = '6NQaZGt7L5STZqgekcbv';
-- DELETE FROM TB_NOTICE                            WHERE CMPNY_CD = '6NQaZGt7L5STZqgekcbv';

-- ---------- D-3. TBM (S6 화면 생성분 — FK 주의: EDU_MTRL_ITEM → EDU_MTRL) ----------
-- DELETE FROM TB_TBM_SESSION_SHARE                 WHERE HOST_CMPNY_CD = '6NQaZGt7L5STZqgekcbv'
--                                                      OR SHARE_CMPNY_CD = '6NQaZGt7L5STZqgekcbv'
--                                                      OR DESIGNATED_BY_CMPNY_CD = '6NQaZGt7L5STZqgekcbv';
-- DELETE FROM TB_TBM_ATTENDANCE                    WHERE CMPNY_CD = '6NQaZGt7L5STZqgekcbv';
-- DELETE FROM TB_TBM_SESSION_STATE                 WHERE CMPNY_CD = '6NQaZGt7L5STZqgekcbv';
-- DELETE FROM TB_TBM_SESSION_RISK                  WHERE CMPNY_CD = '6NQaZGt7L5STZqgekcbv';
-- DELETE FROM TB_TBM_SESSION_CONTENT               WHERE CMPNY_CD = '6NQaZGt7L5STZqgekcbv';
-- DELETE FROM TB_TBM_SESSION                       WHERE CMPNY_CD = '6NQaZGt7L5STZqgekcbv';
-- DELETE FROM TB_TBM_EDU_MTRL_ITEM                 WHERE MTRL_CD IN (
--                                                      SELECT MTRL_CD FROM TB_TBM_EDU_MTRL
--                                                       WHERE CMPNY_CD = '6NQaZGt7L5STZqgekcbv'
--                                                  );
-- DELETE FROM TB_TBM_EDU_MTRL                      WHERE CMPNY_CD = '6NQaZGt7L5STZqgekcbv';

-- ---------- D-4. 요청승인/연차 사용 (S6 화면 생성분) ----------
-- DELETE FROM TB_USER_ATTD_REQ_APPROVAL            WHERE CMPNY_CD = '6NQaZGt7L5STZqgekcbv';
-- DELETE FROM TB_USER_LEAVE_USE                    WHERE CMPNY_CD = '6NQaZGt7L5STZqgekcbv';
-- DELETE FROM TB_USER_ATTD_REQ                     WHERE CMPNY_CD = '6NQaZGt7L5STZqgekcbv';

-- ---------- D-5. 근태/근무계획/연차 부여/약관 동의 (본 시드 + 앱 사용 부산물) ----------
-- DELETE FROM TB_USER_ATTD_GPS                     WHERE CMPNY_CD = '6NQaZGt7L5STZqgekcbv';
-- DELETE FROM TB_USER_ATTD_HIST                    WHERE CMPNY_CD = '6NQaZGt7L5STZqgekcbv';
-- DELETE FROM TB_USER_ATTD_MGMT                    WHERE CMPNY_CD = '6NQaZGt7L5STZqgekcbv';
-- DELETE FROM TB_USER_LEAVE_GRANT                  WHERE CMPNY_CD = '6NQaZGt7L5STZqgekcbv';
-- DELETE FROM TB_USER_WORK_PLAN                    WHERE CMPNY_CD = '6NQaZGt7L5STZqgekcbv';
-- DELETE FROM TB_TERMS_USER_AGR_MGMT               WHERE CMPNY_CD = '6NQaZGt7L5STZqgekcbv';

-- ---------- D-6. 프로비저닝 산출물 (CompanyProvisionMapper.xml INSERT 전수, 역순) ----------
-- DELETE FROM TB_RISK_SITE_HAZARD                  WHERE CMPNY_CD = '6NQaZGt7L5STZqgekcbv';
-- DELETE FROM TB_RISK_TYPE                         WHERE CMPNY_CD = '6NQaZGt7L5STZqgekcbv';
-- DELETE FROM TB_LEAVE_POLICY_HISTORY              WHERE CMPNY_CD = '6NQaZGt7L5STZqgekcbv';
-- DELETE FROM TB_LEAVE_USAGE_POLICY                WHERE CMPNY_CD = '6NQaZGt7L5STZqgekcbv';
-- DELETE FROM TB_LEAVE_POLICY                      WHERE CMPNY_CD = '6NQaZGt7L5STZqgekcbv';
-- DELETE FROM TB_LEAVE_TYPE_MGMT                   WHERE CMPNY_CD = '6NQaZGt7L5STZqgekcbv';
-- DELETE FROM TB_SCH_MGMT                          WHERE CMPNY_CD = '6NQaZGt7L5STZqgekcbv';
-- DELETE FROM TB_CMM_SEQ                           WHERE CMPNY_CD = '6NQaZGt7L5STZqgekcbv';
-- DELETE FROM TB_BAIM_VAL_D                        WHERE CMPNY_CD = '6NQaZGt7L5STZqgekcbv';
-- DELETE FROM TB_BAIM_VAL_M                        WHERE CMPNY_CD = '6NQaZGt7L5STZqgekcbv';
-- DELETE FROM TB_SYST_AUTH_MENU                    WHERE CMPNY_CD = '6NQaZGt7L5STZqgekcbv';
-- DELETE FROM TB_USER_SITE_AUTH                    WHERE CMPNY_CD = '6NQaZGt7L5STZqgekcbv';
-- DELETE FROM TB_SITE_NODE                         WHERE CMPNY_CD = '6NQaZGt7L5STZqgekcbv';
-- DELETE FROM TB_USER                              WHERE CMPNY_CD = '6NQaZGt7L5STZqgekcbv';
-- DELETE FROM TB_SITE                              WHERE CMPNY_CD = '6NQaZGt7L5STZqgekcbv';
-- DELETE FROM TB_CMPNY                             WHERE CMPNY_CD = '6NQaZGt7L5STZqgekcbv';

-- ---------- D-7. 삭제 후 검증 (주석 해제 실행 시) — 기대: 전부 0 ----------
-- SELECT (SELECT COUNT(1) FROM TB_CMPNY WHERE CMPNY_CD = '6NQaZGt7L5STZqgekcbv') AS CMPNY_CNT
--      , (SELECT COUNT(1) FROM TB_USER  WHERE CMPNY_CD = '6NQaZGt7L5STZqgekcbv') AS USER_CNT
--      , (SELECT COUNT(1) FROM TB_SITE  WHERE CMPNY_CD = '6NQaZGt7L5STZqgekcbv') AS SITE_CNT;

-- ============================================================================
-- (끝) 시드 후 검증 절차는 plan §6 (앱/웹 수동 확인) 참조.
-- ============================================================================
