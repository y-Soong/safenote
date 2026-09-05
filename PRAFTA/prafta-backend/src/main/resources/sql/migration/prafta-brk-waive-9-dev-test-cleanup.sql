-- ============================================================================
-- prafta-brk-waive-9-dev-test-cleanup.sql
-- 휴게 넘김(v1·v2) 실기동 QA 테스트 데이터 정리 — ★★ 개발 DB(localhost:3306 prafta) 전용 ★★
--   운영 DB 에는 이 데이터가 없다. 운영에서 실행하지 말 것.
--   2026-09-04~05 QA 에서 만든 것: 근무타입 3종(사업장 00010)·연차 요청 3건(사용 행 4건, 분할 차감 포함).
--   연차 정책 이력(POLICY_SEQ 26/27, 토글 N→Y 원복)과 약관 동의(QTUSERG·QTUSERB 로그인 시 동의)는
--   이력 성격이라 남긴다.
--
-- ★ 실행 순서 = 사전 조회 → 원장 복원(USED_DAYS) → 사용 행 삭제 → 결재·요청 삭제 → 근무타입 삭제 → 사후 검증.
-- ★ 원장 복원이 먼저다: 사용 행을 지운 뒤에는 차감액을 알 수 없다.
-- ★ Workbench safe updates: 복합 PK 테이블 DELETE/UPDATE 라 세션에서 SET SQL_SAFE_UPDATES = 0; 후 실행, 끝나면 1 로.
-- ★ 비멱등 아님(대상이 없으면 0행) — 재실행 안전.
-- 작성 2026-09-05
-- ============================================================================

-- ----------------------------------------------------------------------------
-- [0] 사전 조회 — 대상 확인(기대: 사용 4행 / 요청 3행 / 결재 3행 / 근무타입 3행 + 이력 3행)
-- ----------------------------------------------------------------------------
SELECT LEAVE_ID, USER_CD, REQ_ID, GRANT_ID, LEAVE_DAYS, LEAVE_STATUS, BRK_WAIVE_YN, BRK_WAIVE_MIN
  FROM tb_user_leave_use
 WHERE CMPNY_CD = '001'
   AND LEAVE_ID IN ('LV2026090400115', 'LV2026090400116', 'LV2026090500117', 'LV2026090500118')
 ORDER BY LEAVE_ID;

SELECT GRANT_ID, USER_CD, GRANT_DAYS, USED_DAYS
  FROM tb_user_leave_grant
 WHERE CMPNY_CD = '001'
   AND GRANT_ID IN ('G2026071700306', 'G2026083100384', 'G2026071700308');
-- 기대(정리 전): G…306 USED 1.00000 / G…384 0.26666 / G…308 0.37500

SELECT REQ_ID, USER_CD, REQ_TYPE, REQ_STATUS, WORK_YMD
  FROM tb_user_attd_req
 WHERE CMPNY_CD = '001'
   AND REQ_ID IN ('2026090400203', '2026090400204', '2026090500205');

SELECT S.SCH_CD, S.SCH_NO
     , (SELECT COUNT(*) FROM tb_user_work_plan P
         WHERE P.CMPNY_CD = S.CMPNY_CD AND P.SITE_CD = S.SITE_CD AND P.WORK_PLAN_CD = S.SCH_CD) AS plan_rows
     , (SELECT COUNT(*) FROM tb_user U
         WHERE U.CMPNY_CD = S.CMPNY_CD AND U.SITE_CD = S.SITE_CD AND U.DEFAULT_SCH_CD = S.SCH_CD) AS default_users
  FROM tb_sch_mgmt S
 WHERE S.CMPNY_CD = '001' AND S.SITE_CD = '00010'
   AND S.SCH_NO IN ('QADAY', 'QAOVN', 'QA4H');
-- 기대: 3행, plan_rows 0, default_users 0 (0 이 아니면 근무타입 삭제 단계를 건너뛴다)

-- ----------------------------------------------------------------------------
-- [1] 연차 부여 원장 복원 — 삭제할 사용 행의 차감액만큼 USED_DAYS 를 되돌린다
--     (사용 행 삭제보다 먼저 실행. 한 번만 실행 — 두 번 돌리면 두 번째는 사용 행이 없어 0 감산이라 안전)
-- ----------------------------------------------------------------------------
UPDATE tb_user_leave_grant G
   SET G.USED_DAYS = G.USED_DAYS - IFNULL((
           SELECT SUM(U.LEAVE_DAYS)
             FROM tb_user_leave_use U
            WHERE U.CMPNY_CD = G.CMPNY_CD
              AND U.GRANT_ID = G.GRANT_ID
              AND U.LEAVE_ID IN ('LV2026090400115', 'LV2026090400116', 'LV2026090500117', 'LV2026090500118')
       ), 0)
     , G.UPDATE_NO   = 'BRK_WAIVE_9_CLEANUP'
     , G.UPDATE_DATE = NOW()
 WHERE G.CMPNY_CD = '001'
   AND G.GRANT_ID IN ('G2026071700306', 'G2026083100384', 'G2026071700308');
-- 기대(복원 후): G…306 0.37500 / G…384 0.00000 / G…308 0.00000

-- ----------------------------------------------------------------------------
-- [2] 연차 사용 행 삭제 (4행)
-- ----------------------------------------------------------------------------
DELETE FROM tb_user_leave_use
 WHERE CMPNY_CD = '001'
   AND LEAVE_ID IN ('LV2026090400115', 'LV2026090400116', 'LV2026090500117', 'LV2026090500118');

-- ----------------------------------------------------------------------------
-- [3] 결재 행 → 요청 행 삭제 (각 3행)
-- ----------------------------------------------------------------------------
DELETE FROM tb_user_attd_req_approval
 WHERE CMPNY_CD = '001'
   AND REQ_ID IN ('2026090400203', '2026090400204', '2026090500205');

DELETE FROM tb_user_attd_req
 WHERE CMPNY_CD = '001'
   AND REQ_ID IN ('2026090400203', '2026090400204', '2026090500205');

-- ----------------------------------------------------------------------------
-- [4] 테스트 근무타입 삭제 — 이력 → 현행 (사전 조회 plan_rows·default_users 가 0 일 때만)
-- ----------------------------------------------------------------------------
DELETE FROM tb_sch_mgmt_hist
 WHERE CMPNY_CD = '001' AND SITE_CD = '00010'
   AND SCH_CD IN (SELECT SCH_CD FROM (
         SELECT SCH_CD FROM tb_sch_mgmt
          WHERE CMPNY_CD = '001' AND SITE_CD = '00010'
            AND SCH_NO IN ('QADAY', 'QAOVN', 'QA4H')) X);

DELETE FROM tb_sch_mgmt
 WHERE CMPNY_CD = '001' AND SITE_CD = '00010'
   AND SCH_NO IN ('QADAY', 'QAOVN', 'QA4H')
   AND NOT EXISTS (SELECT 1 FROM tb_user_work_plan P
                    WHERE P.CMPNY_CD = tb_sch_mgmt.CMPNY_CD AND P.SITE_CD = tb_sch_mgmt.SITE_CD
                      AND P.WORK_PLAN_CD = tb_sch_mgmt.SCH_CD)
   AND NOT EXISTS (SELECT 1 FROM tb_user U
                    WHERE U.CMPNY_CD = tb_sch_mgmt.CMPNY_CD AND U.SITE_CD = tb_sch_mgmt.SITE_CD
                      AND U.DEFAULT_SCH_CD = tb_sch_mgmt.SCH_CD);

-- ----------------------------------------------------------------------------
-- [5] 사후 검증 — 전부 0 / 원장 기대값
-- ----------------------------------------------------------------------------
SELECT (SELECT COUNT(*) FROM tb_user_leave_use
         WHERE CMPNY_CD = '001'
           AND LEAVE_ID IN ('LV2026090400115', 'LV2026090400116', 'LV2026090500117', 'LV2026090500118')) AS use_left
     , (SELECT COUNT(*) FROM tb_user_attd_req
         WHERE CMPNY_CD = '001' AND REQ_ID IN ('2026090400203', '2026090400204', '2026090500205')) AS req_left
     , (SELECT COUNT(*) FROM tb_user_attd_req_approval
         WHERE CMPNY_CD = '001' AND REQ_ID IN ('2026090400203', '2026090400204', '2026090500205')) AS approval_left
     , (SELECT COUNT(*) FROM tb_sch_mgmt
         WHERE CMPNY_CD = '001' AND SITE_CD = '00010' AND SCH_NO IN ('QADAY', 'QAOVN', 'QA4H')) AS sch_left;

SELECT GRANT_ID, GRANT_DAYS, USED_DAYS, UPDATE_NO
  FROM tb_user_leave_grant
 WHERE CMPNY_CD = '001'
   AND GRANT_ID IN ('G2026071700306', 'G2026083100384', 'G2026071700308');
-- 기대: 0.37500 / 0.00000 / 0.00000
