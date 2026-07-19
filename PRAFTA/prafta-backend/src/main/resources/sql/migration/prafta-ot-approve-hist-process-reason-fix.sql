-- ============================================================================
-- 초과근무 "요청 승인" 처리 이력의 관리자 처리사유 정정
--
-- 배경:
--   Attd_07 일자상세의 OT 저장 엔드포인트(update-user-overtime-requests)는 두 경로를 겸한다.
--     - 직접 등록(reqId=null) : reqReason = 관리자가 팝업 사유칸에 입력한 값 (정상)
--     - 요청 승인(reqId 보유) : 프론트가 근로자 신청 사유(REQ_REASON)를 reqReason 으로 보낸다
--   그런데 서비스가 이를 구분하지 않고 TB_USER_ATTD_HIST.PROCESS_REASON(=관리자 처리사유)에
--   그대로 적재해, 처리 이력 팝업에서 "근로자 사유"가 "관리자 처리사유"로 둔갑해 보였다.
--   (OT 승인에는 관리자 사유 입력 UI 자체가 없다.)
--
-- 조치:
--   코드는 요청 승인 경로에서 고정 라벨('사용자 요청 승인')을 남기도록 수정했다
--   (Attd07ServiceImpl.OT_APPROVE_PROCESS_REASON — 근태 보정 승인과 동일 문구).
--   본 스크립트는 이미 적재된 과거 이력을 같은 라벨로 정정한다.
--   근로자 사유는 소실되지 않는다 — 처리 이력의 '요청 사유' 컬럼이 TB_USER_ATTD_REQ 에서 직접 읽는다.
--
-- 대상 식별(보수적으로 3중 조건):
--   1) HIST_TYPE='08'(초과근무 승인)
--   2) 인박스 승인으로 닫힌 REQ(REQ_TYPE 03/04, REQ_STATUS='02', PROCESS_COMMENT='OT_APPROVED')와
--      같은 시각(±60초, 동일 트랜잭션)에 적재된 이력
--   3) PROCESS_REASON 이 그 REQ 의 REQ_REASON 과 정확히 일치(= 복사된 값)
--   → 관리자가 직접 등록하며 입력한 사유(경로 1)는 조건 2·3을 만족하지 않아 건드리지 않는다.
--
-- 2026-07-13 로컬 실측: 5건.
-- ============================================================================


-- ----------------------------------------------------------------------------
-- [1] 적용 전 대상 확인
-- ----------------------------------------------------------------------------
SELECT B.CMPNY_CD
     , B.HIST_ID
     , B.WORK_YMD
     , B.PROCESS_REASON   AS 현재_처리사유_잘못복사됨
     , R.REQ_ID
     , R.REQ_REASON       AS 근로자_신청사유
  FROM TB_USER_ATTD_HIST B
       INNER JOIN TB_USER_ATTD_MGMT A
       ON  A.CMPNY_CD = B.CMPNY_CD
       AND A.ATTD_ID  = B.ATTD_ID
       INNER JOIN TB_USER_ATTD_REQ R
       ON  R.CMPNY_CD        = B.CMPNY_CD
       AND R.SITE_CD         = B.SITE_CD
       AND R.WORK_YMD        = B.WORK_YMD
       AND R.USER_CD         = A.USER_CD
       AND R.DEL_YN          = 'N'
       AND R.REQ_TYPE        IN ('03', '04')
       AND R.REQ_STATUS      = '02'
       AND R.PROCESS_COMMENT = 'OT_APPROVED'
       AND R.PROCESS_DATE IS NOT NULL
       AND ABS(TIMESTAMPDIFF(SECOND, R.PROCESS_DATE, B.INSERT_DATE)) <= 60
 WHERE B.HIST_TYPE      = '08'
   AND B.PROCESS_REASON = R.REQ_REASON
 ORDER BY B.CMPNY_CD, B.INSERT_DATE;


-- ----------------------------------------------------------------------------
-- [2] 정정 — 관리자 처리사유를 고정 라벨로 교체
--     ※ MySQL Workbench safe update mode(Error 1175) 대비: HIST_ID 가 PK 선두가 아니므로
--       세션 한정으로 해제한다(의도된 조건부 일괄 갱신).
-- ----------------------------------------------------------------------------
SET SQL_SAFE_UPDATES = 0;

UPDATE TB_USER_ATTD_HIST B
       INNER JOIN TB_USER_ATTD_MGMT A
       ON  A.CMPNY_CD = B.CMPNY_CD
       AND A.ATTD_ID  = B.ATTD_ID
       INNER JOIN TB_USER_ATTD_REQ R
       ON  R.CMPNY_CD        = B.CMPNY_CD
       AND R.SITE_CD         = B.SITE_CD
       AND R.WORK_YMD        = B.WORK_YMD
       AND R.USER_CD         = A.USER_CD
       AND R.DEL_YN          = 'N'
       AND R.REQ_TYPE        IN ('03', '04')
       AND R.REQ_STATUS      = '02'
       AND R.PROCESS_COMMENT = 'OT_APPROVED'
       AND R.PROCESS_DATE IS NOT NULL
       AND ABS(TIMESTAMPDIFF(SECOND, R.PROCESS_DATE, B.INSERT_DATE)) <= 60
   SET B.PROCESS_REASON = '사용자 요청 승인'
 WHERE B.HIST_TYPE      = '08'
   AND B.PROCESS_REASON = R.REQ_REASON;

SET SQL_SAFE_UPDATES = 1;


-- ----------------------------------------------------------------------------
-- [3] 검증 — [1] 을 다시 실행해 0행이면 정상.
--     처리 이력 팝업에서 '요청 사유'(근로자) / '처리 사유'(관리자)가 서로 다른 값으로 보이면 완료.
-- ----------------------------------------------------------------------------
SELECT B.HIST_ID, B.WORK_YMD, B.PROCESS_REASON
  FROM TB_USER_ATTD_HIST B
 WHERE B.HIST_TYPE = '08'
 ORDER BY B.INSERT_DATE DESC
 LIMIT 20;
