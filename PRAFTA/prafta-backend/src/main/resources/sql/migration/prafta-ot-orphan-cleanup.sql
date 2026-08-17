-- ============================================================================
-- 고아 초과근무(OT) 정리 — TB_USER_OVERTIME_MGMT soft-delete (2026-08-17)
--
-- [배경]
--   근태 삭제(daily-attd-detail-delete)의 OT 연쇄 삭제는 "ATTD_ID 가 일치하는 행"만
--   지운다. 그런데 상신→승인 경로(웹 Attd_10 접수함 / 일자상세 요청카드 / 앱 관리자
--   승인)로 생성된 OT 는 ATTD_ID 가 NULL 로 저장되어 연쇄 그물을 빠져나갔고,
--   근태를 삭제한 뒤에도 OT 만 남아 재등록 시 다시 나타나는 정합성 깨짐이 발생
--   (2026-08-17 테스트 실증). 본 스크립트는 이미 쌓인 고아 OT 를 일괄 soft-delete 한다.
--   (근본 수정 = OT 등록 시 서버가 ATTD_ID 를 도출·연결하는 A안 — 별도 배포.
--    A안 배포 전까지는 새 고아가 또 생길 수 있으므로, A안 배포 후 한 번 더 §A 로
--    잔존을 확인하고 필요 시 재실행한다.)
--
-- [정리 대상 2유형] (활성 행 DEL_YN='N' 만, OT_STATUS 무관)
--   유형① 링크 절단형: ATTD_ID 가 있으나 그 근태 행이 삭제/부재 (연쇄 도입 전 잔재 등)
--   유형② 무연결 고아형: ATTD_ID 가 NULL 이고, 같은 (회사·사업장·사용자·근무일)에
--          활성 근태 행이 하나도 없음 (승인 OT 생성 후 근태만 삭제된 케이스)
--   ★주의: ATTD_ID NULL 이어도 그날 활성 근태가 있으면 정상 승인 OT — 절대 건드리지 않는다.
--
-- [실행 절차 — Workbench 수동, 개발·운영 양쪽 모두 적용(동시적용 원칙)]
--   1) §A 사전 검증 3쿼리 실행 → 대상 건수·목록을 눈으로 확인 (예상 밖 회사/대량 건수면 중단·보고)
--   2) §B soft-delete 2문 실행 (SQL_SAFE_UPDATES 토글 포함)
--   3) §C 사후 검증 → 잔존 0건 확인
--   * soft-delete(DEL_YN='Y')라 하드 삭제 없음. 되돌리기는 말미 [롤백 참고] 주석 참조.
-- ============================================================================

-- ============================================================================
-- §A. 사전 검증 (읽기 전용)
-- ============================================================================

-- A-1. 유형① 링크 절단형 — 회사별 건수
SELECT O.CMPNY_CD, COUNT(*) AS ORPHAN_TYPE1_CNT
  FROM TB_USER_OVERTIME_MGMT O
 WHERE O.DEL_YN = 'N'
   AND O.ATTD_ID IS NOT NULL
   AND NOT EXISTS (
         SELECT 1
           FROM TB_USER_ATTD_MGMT A
          WHERE A.CMPNY_CD = O.CMPNY_CD
            AND A.ATTD_ID  = O.ATTD_ID
            AND A.DEL_YN   = 'N'
       )
 GROUP BY O.CMPNY_CD;

-- A-2. 유형② 무연결 고아형 — 회사별 건수
SELECT O.CMPNY_CD, COUNT(*) AS ORPHAN_TYPE2_CNT
  FROM TB_USER_OVERTIME_MGMT O
 WHERE O.DEL_YN = 'N'
   AND O.ATTD_ID IS NULL
   AND NOT EXISTS (
         SELECT 1
           FROM TB_USER_ATTD_MGMT A
          WHERE A.CMPNY_CD = O.CMPNY_CD
            AND A.SITE_CD  = O.SITE_CD
            AND A.USER_CD  = O.USER_CD
            AND A.WORK_YMD = O.WORK_YMD
            AND A.DEL_YN   = 'N'
       )
 GROUP BY O.CMPNY_CD;

-- A-3. 대상 상세 목록 (검토용 — 두 유형 합집합)
SELECT O.CMPNY_CD, O.SITE_CD, O.USER_CD, O.WORK_YMD, O.OT_ID, O.ATTD_ID, O.REQ_ID
     , O.ACTUAL_START_DATE, O.ACTUAL_START_TIME, O.ACTUAL_END_DATE, O.ACTUAL_END_TIME
     , O.OT_STATUS, O.INSERT_NO, O.INSERT_DATE
  FROM TB_USER_OVERTIME_MGMT O
 WHERE O.DEL_YN = 'N'
   AND (
         ( O.ATTD_ID IS NOT NULL
           AND NOT EXISTS (
                 SELECT 1 FROM TB_USER_ATTD_MGMT A
                  WHERE A.CMPNY_CD = O.CMPNY_CD
                    AND A.ATTD_ID  = O.ATTD_ID
                    AND A.DEL_YN   = 'N'
               )
         )
      OR ( O.ATTD_ID IS NULL
           AND NOT EXISTS (
                 SELECT 1 FROM TB_USER_ATTD_MGMT A
                  WHERE A.CMPNY_CD = O.CMPNY_CD
                    AND A.SITE_CD  = O.SITE_CD
                    AND A.USER_CD  = O.USER_CD
                    AND A.WORK_YMD = O.WORK_YMD
                    AND A.DEL_YN   = 'N'
               )
         )
       )
 ORDER BY O.CMPNY_CD, O.WORK_YMD, O.USER_CD
 LIMIT 200;

-- ============================================================================
-- §B. 정리 (soft-delete) — §A 검토 후에만 실행
-- ============================================================================
SET SQL_SAFE_UPDATES = 0;

-- B-1. 유형① 링크 절단형 soft-delete
UPDATE TB_USER_OVERTIME_MGMT O
   SET O.DEL_YN      = 'Y'
     , O.UPDATE_NO   = 'SYSTEM'
     , O.UPDATE_DATE = NOW()
 WHERE O.DEL_YN = 'N'
   AND O.ATTD_ID IS NOT NULL
   AND NOT EXISTS (
         SELECT 1
           FROM TB_USER_ATTD_MGMT A
          WHERE A.CMPNY_CD = O.CMPNY_CD
            AND A.ATTD_ID  = O.ATTD_ID
            AND A.DEL_YN   = 'N'
       );

-- B-2. 유형② 무연결 고아형 soft-delete
UPDATE TB_USER_OVERTIME_MGMT O
   SET O.DEL_YN      = 'Y'
     , O.UPDATE_NO   = 'SYSTEM'
     , O.UPDATE_DATE = NOW()
 WHERE O.DEL_YN = 'N'
   AND O.ATTD_ID IS NULL
   AND NOT EXISTS (
         SELECT 1
           FROM TB_USER_ATTD_MGMT A
          WHERE A.CMPNY_CD = O.CMPNY_CD
            AND A.SITE_CD  = O.SITE_CD
            AND A.USER_CD  = O.USER_CD
            AND A.WORK_YMD = O.WORK_YMD
            AND A.DEL_YN   = 'N'
       );

SET SQL_SAFE_UPDATES = 1;

-- ============================================================================
-- §C. 사후 검증 — 두 쿼리 모두 0건이어야 정상
-- ============================================================================
SELECT COUNT(*) AS REMAIN_TYPE1
  FROM TB_USER_OVERTIME_MGMT O
 WHERE O.DEL_YN = 'N'
   AND O.ATTD_ID IS NOT NULL
   AND NOT EXISTS (
         SELECT 1 FROM TB_USER_ATTD_MGMT A
          WHERE A.CMPNY_CD = O.CMPNY_CD AND A.ATTD_ID = O.ATTD_ID AND A.DEL_YN = 'N'
       );

SELECT COUNT(*) AS REMAIN_TYPE2
  FROM TB_USER_OVERTIME_MGMT O
 WHERE O.DEL_YN = 'N'
   AND O.ATTD_ID IS NULL
   AND NOT EXISTS (
         SELECT 1 FROM TB_USER_ATTD_MGMT A
          WHERE A.CMPNY_CD = O.CMPNY_CD AND A.SITE_CD = O.SITE_CD
            AND A.USER_CD = O.USER_CD AND A.WORK_YMD = O.WORK_YMD AND A.DEL_YN = 'N'
       );

-- ============================================================================
-- [롤백 참고] soft-delete 라 데이터는 남아 있다. 본 실행분만 되돌리려면 실행 시각을
--   기준으로 아래 형태를 사용한다(실행 직후 UPDATE_DATE 범위를 좁혀서):
--     UPDATE TB_USER_OVERTIME_MGMT
--        SET DEL_YN='N', UPDATE_DATE=NOW()
--      WHERE DEL_YN='Y' AND UPDATE_NO='SYSTEM'
--        AND UPDATE_DATE BETWEEN '<실행시각-1분>' AND '<실행시각+1분>';
-- ============================================================================
