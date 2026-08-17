-- ============================================================================
-- 초과근무 ATTD_ID 소급 연결(백필) — TB_USER_OVERTIME_MGMT (2026-08-17, A안)
--
-- [배경]
--   A안 배포 전에는 상신→승인 경로(웹 Attd_10 / 일자상세 요청카드 / 앱 관리자 승인)로
--   생성된 OT 가 ATTD_ID=NULL 로 저장되어 근태 삭제 연쇄를 빠져나갔다. 본 스크립트는
--   기존 NULL 행을 REQ 권위값(TB_USER_ATTD_REQ.WORK_SEQ)으로 그 구간의 활성 근태에
--   소급 연결하고, 구간 근태가 이미 삭제된 건은 소급 연쇄로 soft-delete 한다.
--
-- [실행 순서 — Workbench 수동, 개발·운영 양쪽 적용]
--   ★A안 배포 이후에 실행한다(배포 전 실행 시 새 NULL 행이 또 쌓임).
--   ① 본 파일 §A → §B → §C
--   ② 이어서 prafta-ot-orphan-cleanup.sql (REQ 미연결 잔여 고아 정리)
-- ============================================================================

-- ============================================================================
-- §A. 사전 검증 (읽기 전용)
-- ============================================================================

-- A-1. 연결 가능 건수 — NULL OT 중 REQ.WORK_SEQ 로 활성 근태에 붙일 수 있는 것 (회사별)
SELECT O.CMPNY_CD, COUNT(*) AS LINKABLE_CNT
  FROM TB_USER_OVERTIME_MGMT O
  JOIN TB_USER_ATTD_REQ R
    ON R.CMPNY_CD = O.CMPNY_CD
   AND R.REQ_ID   = O.REQ_ID
  JOIN TB_USER_ATTD_MGMT A
    ON A.CMPNY_CD = O.CMPNY_CD
   AND A.SITE_CD  = O.SITE_CD
   AND A.USER_CD  = O.USER_CD
   AND A.WORK_YMD = O.WORK_YMD
   AND A.WORK_SEQ = R.WORK_SEQ
   AND A.DEL_YN   = 'N'
 WHERE O.DEL_YN  = 'N'
   AND O.ATTD_ID IS NULL
 GROUP BY O.CMPNY_CD;

-- A-2. 소급 연쇄 삭제 대상 건수 — REQ 는 있으나 그 구간의 활성 근태가 없는 NULL OT (회사별)
--      (= 승인 후 그 구간 근태가 삭제된 케이스. A안 정책상 구간 삭제 시 OT 도 함께 삭제가 정답)
SELECT O.CMPNY_CD, COUNT(*) AS STALE_CNT
  FROM TB_USER_OVERTIME_MGMT O
  JOIN TB_USER_ATTD_REQ R
    ON R.CMPNY_CD = O.CMPNY_CD
   AND R.REQ_ID   = O.REQ_ID
 WHERE O.DEL_YN  = 'N'
   AND O.ATTD_ID IS NULL
   AND NOT EXISTS (
         SELECT 1
           FROM TB_USER_ATTD_MGMT A
          WHERE A.CMPNY_CD = O.CMPNY_CD
            AND A.SITE_CD  = O.SITE_CD
            AND A.USER_CD  = O.USER_CD
            AND A.WORK_YMD = O.WORK_YMD
            AND A.WORK_SEQ = R.WORK_SEQ
            AND A.DEL_YN   = 'N'
       )
 GROUP BY O.CMPNY_CD;

-- A-3. REQ 미보유 NULL OT (본 스크립트 비대상 — prafta-ot-orphan-cleanup.sql 유형② 로 처리)
SELECT O.CMPNY_CD, COUNT(*) AS NO_REQ_CNT
  FROM TB_USER_OVERTIME_MGMT O
 WHERE O.DEL_YN  = 'N'
   AND O.ATTD_ID IS NULL
   AND (O.REQ_ID IS NULL OR NOT EXISTS (
         SELECT 1 FROM TB_USER_ATTD_REQ R
          WHERE R.CMPNY_CD = O.CMPNY_CD AND R.REQ_ID = O.REQ_ID
       ))
 GROUP BY O.CMPNY_CD;

-- ============================================================================
-- §B. 백필 — §A 검토 후에만 실행
-- ============================================================================
SET SQL_SAFE_UPDATES = 0;

-- B-1. NULL OT → 그 구간(REQ.WORK_SEQ)의 활성 근태 ATTD_ID 소급 연결
UPDATE TB_USER_OVERTIME_MGMT O
  JOIN TB_USER_ATTD_REQ R
    ON R.CMPNY_CD = O.CMPNY_CD
   AND R.REQ_ID   = O.REQ_ID
  JOIN TB_USER_ATTD_MGMT A
    ON A.CMPNY_CD = O.CMPNY_CD
   AND A.SITE_CD  = O.SITE_CD
   AND A.USER_CD  = O.USER_CD
   AND A.WORK_YMD = O.WORK_YMD
   AND A.WORK_SEQ = R.WORK_SEQ
   AND A.DEL_YN   = 'N'
   SET O.ATTD_ID     = A.ATTD_ID
     , O.UPDATE_NO   = 'SYSTEM'
     , O.UPDATE_DATE = NOW()
 WHERE O.DEL_YN  = 'N'
   AND O.ATTD_ID IS NULL;

-- B-2. 소급 연쇄 삭제 — 그 구간의 활성 근태가 없는 NULL OT soft-delete (A-2 대상과 동일 술어)
UPDATE TB_USER_OVERTIME_MGMT O
  JOIN TB_USER_ATTD_REQ R
    ON R.CMPNY_CD = O.CMPNY_CD
   AND R.REQ_ID   = O.REQ_ID
   SET O.DEL_YN      = 'Y'
     , O.UPDATE_NO   = 'SYSTEM'
     , O.UPDATE_DATE = NOW()
 WHERE O.DEL_YN  = 'N'
   AND O.ATTD_ID IS NULL
   AND NOT EXISTS (
         SELECT 1
           FROM TB_USER_ATTD_MGMT A
          WHERE A.CMPNY_CD = O.CMPNY_CD
            AND A.SITE_CD  = O.SITE_CD
            AND A.USER_CD  = O.USER_CD
            AND A.WORK_YMD = O.WORK_YMD
            AND A.WORK_SEQ = R.WORK_SEQ
            AND A.DEL_YN   = 'N'
       );

SET SQL_SAFE_UPDATES = 1;

-- ============================================================================
-- §C. 사후 검증 — 잔여 NULL 활성 OT 는 REQ 미보유분(A-3)만 남아야 한다.
--     (그 잔여는 prafta-ot-orphan-cleanup.sql 로 이어서 정리)
-- ============================================================================
SELECT O.CMPNY_CD
     , SUM(CASE WHEN O.REQ_ID IS NOT NULL AND EXISTS (
             SELECT 1 FROM TB_USER_ATTD_REQ R
              WHERE R.CMPNY_CD = O.CMPNY_CD AND R.REQ_ID = O.REQ_ID
           ) THEN 1 ELSE 0 END) AS REMAIN_WITH_REQ   -- 0 이어야 정상
     , COUNT(*)                 AS REMAIN_TOTAL
  FROM TB_USER_OVERTIME_MGMT O
 WHERE O.DEL_YN  = 'N'
   AND O.ATTD_ID IS NULL
 GROUP BY O.CMPNY_CD;
