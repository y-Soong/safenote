-- ============================================================================
-- prafta-061 (비범위 현황 파악): 입사 전 기간에 생성된 근무계획(TB_USER_WORK_PLAN) 조회
-- ============================================================================
-- ★조회 전용 — 삭제/수정 DML 없음. 요청서 비범위 확정: "이미 입사 전 기간에 잘못 깔린
--   기존 스케줄의 소급 삭제는 하지 않는다"(수동입력/연차 보존 원칙과 충돌 위험).
--   이 파일은 오염 규모 파악용 SELECT 만 담는다.
-- 실행 주체: 사용자가 Workbench 로 직접 실행.
-- 대상: WORK_YMD 가 tb_user.HIRE_DATE(YYYYMMDD) 보다 이른 근무계획 행.
--   HIRE_DATE 가 NULL 이거나 8자리 숫자가 아닌 사용자는 판정 불가라 제외(fail-open 과 동일 축).
-- PII: 이름/휴대폰 등 평문 PII 컬럼은 조회하지 않는다(USER_CD 코드만).
-- ============================================================================

-- 1) 요약 — 회사·사용자·GEN_SOURCE 별 입사 전 근무계획 건수
SELECT WP.CMPNY_CD
     , WP.SITE_CD
     , WP.USER_CD
     , U.HIRE_DATE
     , WP.GEN_SOURCE
     , COUNT(*)         AS PREHIRE_DAYS
     , MIN(WP.WORK_YMD) AS FIRST_YMD
     , MAX(WP.WORK_YMD) AS LAST_YMD
  FROM tb_user_work_plan WP
  JOIN tb_user U
    ON U.CMPNY_CD = WP.CMPNY_CD
   AND U.USER_CD  = WP.USER_CD
 WHERE U.HIRE_DATE IS NOT NULL
   AND U.HIRE_DATE REGEXP '^[0-9]{8}$'
   AND WP.WORK_YMD < U.HIRE_DATE
 GROUP BY WP.CMPNY_CD, WP.SITE_CD, WP.USER_CD, U.HIRE_DATE, WP.GEN_SOURCE
 ORDER BY WP.CMPNY_CD, WP.SITE_CD, WP.USER_CD, WP.GEN_SOURCE
 LIMIT 500;

-- 2) 상세 — 입사 전 근무계획 행 목록(필요 시 특정 회사로 좁혀서 실행 권장)
SELECT WP.CMPNY_CD
     , WP.SITE_CD
     , WP.USER_CD
     , U.HIRE_DATE
     , WP.WORK_YMD
     , WP.WORK_PLAN_CD
     , WP.GEN_SOURCE
     , WP.INSERT_NO
     , WP.INSERT_DATE
  FROM tb_user_work_plan WP
  JOIN tb_user U
    ON U.CMPNY_CD = WP.CMPNY_CD
   AND U.USER_CD  = WP.USER_CD
 WHERE U.HIRE_DATE IS NOT NULL
   AND U.HIRE_DATE REGEXP '^[0-9]{8}$'
   AND WP.WORK_YMD < U.HIRE_DATE
 ORDER BY WP.CMPNY_CD, WP.SITE_CD, WP.USER_CD, WP.WORK_YMD
 LIMIT 1000;
