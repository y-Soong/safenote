-- ============================================================
-- 릴레이(재하청 데이터 묶음) 기능 테스트 시드 — 2026-07-20
-- 체인: 001(JPC, 원청 A)
--       → mLP5JWe5EOFPS17zZOKj(NEWCO 프로비저닝검증, 하청 B)
--       → IqzQKPKMpu75RsCoDN6m(NEWCO3 시드검증, 재하청 C)
--
-- ※ 실행 시점: [수동 STEP 1] B→C 사업장 링크 수락 "이후"에 실행할 것.
--    (C의 미러 사업장이 링크 수락 시 서버가 자동 생성/채번하기 때문)
--
-- 테스트 절차 전체:
--   1. (UI) NEWCOADMIN 으로 Subcon_02 에서 중곡사업장(00002)을
--      "NEWCO3 시드검증"에 링크 제안 → NEWCO3ADMIN 으로 수락
--   2. (SQL) 본 파일 STEP 0 으로 미러 사업장 코드 확인 후 STEP 1 실행
--   3. (UI) B가 C에게 근태 공유요청(기간 2026-07-01~07-15, 마감분만 해제)
--      → C 승인 → B 스냅샷 보유
--   4. (UI) A(001)가 B에게 근태 공유요청(동일 기간, 마감분만 해제)
--   5. (UI) B 승인 팝업에서 릴레이 후보(3번 스냅샷) 체크 후 승인
--   6. (UI) A 로 Subcon_04 확인 — C 행이 B 소속으로 relabel 되어 표시되면 성공
-- ============================================================

-- ------------------------------------------------------------
-- STEP 0. C 미러 사업장 코드 확인 (예상: 00002)
--   LINK_SRC_CMPNY_CD = 'mLP5JWe5EOFPS17zZOKj'(B) 인 행이
--   링크 수락으로 생성된 미러 사업장이다.
-- ------------------------------------------------------------
SELECT SITE_CD, SITE_NM, LINK_SRC_CMPNY_CD, LINK_SRC_SITE_CD
FROM TB_SITE
WHERE CMPNY_CD = 'IqzQKPKMpu75RsCoDN6m';

-- ★ 아래 INSERT 의 SITE_CD '00002' 가 위 조회의 미러 사업장 코드와 다르면 전부 치환할 것.

-- ------------------------------------------------------------
-- STEP 1. C(재하청) 근태 시드 — 미러 사업장, 2026-07-06(월)~07-10(금), 사용자 2명 = 10행
--   사용자: 20260700001(NEWCO3ADMIN/master), 20260700002(NC3USER/일반)
--   패턴: 대부분 정상 / 07-07 U2 지각성 출근(0915) / 07-09 U2 이른 퇴근(1650)
--   ※ 근무계획(스케줄) 미시드 — 스냅샷에서 계획시간은 공란, 판정은 NORMAL 로 표시됨
--   ※ 멱등 실행: 기존 시드 행을 먼저 제거하므로 재실행해도 1062(PK 중복)가 나지 않는다
-- ------------------------------------------------------------
DELETE FROM TB_USER_ATTD_MGMT
WHERE CMPNY_CD = 'IqzQKPKMpu75RsCoDN6m'
  AND INSERT_NO = 'RELAY_SEED';

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
) VALUES
  ('RS20260706U1', 'IqzQKPKMpu75RsCoDN6m', '00002', '20260700001', '20260706', NULL, 1, '20260706', '0857', '01', '20260706', '1803', '01', 'N', 'RELAY_SEED')
, ('RS20260706U2', 'IqzQKPKMpu75RsCoDN6m', '00002', '20260700002', '20260706', NULL, 1, '20260706', '0901', '01', '20260706', '1801', '01', 'N', 'RELAY_SEED')
, ('RS20260707U1', 'IqzQKPKMpu75RsCoDN6m', '00002', '20260700001', '20260707', NULL, 1, '20260707', '0855', '01', '20260707', '1805', '01', 'N', 'RELAY_SEED')
, ('RS20260707U2', 'IqzQKPKMpu75RsCoDN6m', '00002', '20260700002', '20260707', NULL, 1, '20260707', '0915', '01', '20260707', '1802', '01', 'N', 'RELAY_SEED')
, ('RS20260708U1', 'IqzQKPKMpu75RsCoDN6m', '00002', '20260700001', '20260708', NULL, 1, '20260708', '0859', '01', '20260708', '1800', '01', 'N', 'RELAY_SEED')
, ('RS20260708U2', 'IqzQKPKMpu75RsCoDN6m', '00002', '20260700002', '20260708', NULL, 1, '20260708', '0902', '01', '20260708', '1808', '01', 'N', 'RELAY_SEED')
, ('RS20260709U1', 'IqzQKPKMpu75RsCoDN6m', '00002', '20260700001', '20260709', NULL, 1, '20260709', '0856', '01', '20260709', '1804', '01', 'N', 'RELAY_SEED')
, ('RS20260709U2', 'IqzQKPKMpu75RsCoDN6m', '00002', '20260700002', '20260709', NULL, 1, '20260709', '0900', '01', '20260709', '1650', '01', 'N', 'RELAY_SEED')
, ('RS20260710U1', 'IqzQKPKMpu75RsCoDN6m', '00002', '20260700001', '20260710', NULL, 1, '20260710', '0858', '01', '20260710', '1806', '01', 'N', 'RELAY_SEED')
, ('RS20260710U2', 'IqzQKPKMpu75RsCoDN6m', '00002', '20260700002', '20260710', NULL, 1, '20260710', '0903', '01', '20260710', '1801', '01', 'N', 'RELAY_SEED')
;

-- 시드 확인
SELECT ATTD_ID, SITE_CD, USER_CD, WORK_YMD, CHECK_IN_TIME, CHECK_OUT_TIME
FROM TB_USER_ATTD_MGMT
WHERE CMPNY_CD = 'IqzQKPKMpu75RsCoDN6m'
  AND INSERT_NO = 'RELAY_SEED'
ORDER BY WORK_YMD, USER_CD
LIMIT 20;

-- ------------------------------------------------------------
-- (정리용) 테스트 후 시드 제거
-- ------------------------------------------------------------
-- DELETE FROM TB_USER_ATTD_MGMT
-- WHERE CMPNY_CD = 'IqzQKPKMpu75RsCoDN6m'
--   AND INSERT_NO = 'RELAY_SEED';
