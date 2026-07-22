-- ============================================================================
-- prafta-demo-review-account-seed-ROLLBACK.sql
-- 플레이스토어 심사용 데모 계정(DEMO 심사용/리뷰 전용) 전면 원복 — NEWCOADMIN 계정 사용으로 방침 변경
-- CMPNY_CD = 6NQaZGt7L5STZqgekcbv
-- 실행: 사용자 Workbench 수동. 아래 순서 그대로 위에서부터 실행하면 됨(주석 없음, 전부 실행문).
-- ============================================================================

-- ---------- STEP 0. 안전확인 — 반드시 먼저 실행하고 결과가 "DEMO 심사용(리뷰 전용)" 인지 확인 ----------
SELECT CMPNY_CD, CMPNY_NM FROM TB_CMPNY WHERE CMPNY_CD = '6NQaZGt7L5STZqgekcbv';

-- ---------- 위 결과가 "DEMO 심사용(리뷰 전용)" 확인됐으면 아래부터 실행 ----------

-- Workbench safe update mode 대비: 아래 DELETE 문 대부분이 CMPNY_CD 단독 조건(복합PK)이라 1175 에러가
-- 날 수 있어 이 구간만 세션 토글. 마지막 문장 실행 후 즉시 복구되니 Preferences 는 건드리지 말 것.
SET SQL_SAFE_UPDATES = 0;

-- ---------- STEP 1. 알림/푸시/디바이스/토큰 (로그인·사용 부산물) ----------
DELETE FROM TB_NOTI_OUTBOX                       WHERE CMPNY_CD = '6NQaZGt7L5STZqgekcbv';
DELETE FROM TB_USER_PUSH_SETTING                 WHERE CMPNY_CD = '6NQaZGt7L5STZqgekcbv';
DELETE FROM TB_USER_DEVICE_OCCUPANCY_ANOMALY     WHERE CMPNY_CD = '6NQaZGt7L5STZqgekcbv';
DELETE FROM TB_USER_DEVICE_LOGIN_HIST            WHERE CMPNY_CD = '6NQaZGt7L5STZqgekcbv';
DELETE FROM TB_USER_DEVICE                       WHERE CMPNY_CD = '6NQaZGt7L5STZqgekcbv';
DELETE FROM TB_AUTH_TOKEN                        WHERE CMPNY_CD = '6NQaZGt7L5STZqgekcbv';

-- ---------- STEP 2. 공지 (화면 생성분 — 있었으면) ----------
DELETE FROM TB_NOTICE_USER_ACK                   WHERE CMPNY_CD = '6NQaZGt7L5STZqgekcbv';
DELETE FROM TB_NOTICE_FILE                       WHERE CMPNY_CD = '6NQaZGt7L5STZqgekcbv';
DELETE FROM TB_NOTICE_TARGET                     WHERE CMPNY_CD = '6NQaZGt7L5STZqgekcbv';
DELETE FROM TB_NOTICE                            WHERE CMPNY_CD = '6NQaZGt7L5STZqgekcbv';

-- ---------- STEP 3. TBM (화면 생성분 — 있었으면. FK 주의: EDU_MTRL_ITEM → EDU_MTRL) ----------
DELETE FROM TB_TBM_SESSION_SHARE                 WHERE HOST_CMPNY_CD = '6NQaZGt7L5STZqgekcbv'
                                                     OR SHARE_CMPNY_CD = '6NQaZGt7L5STZqgekcbv'
                                                     OR DESIGNATED_BY_CMPNY_CD = '6NQaZGt7L5STZqgekcbv';
DELETE FROM TB_TBM_ATTENDANCE                    WHERE CMPNY_CD = '6NQaZGt7L5STZqgekcbv';
DELETE FROM TB_TBM_SESSION_STATE                 WHERE CMPNY_CD = '6NQaZGt7L5STZqgekcbv';
DELETE FROM TB_TBM_SESSION_RISK                  WHERE CMPNY_CD = '6NQaZGt7L5STZqgekcbv';
DELETE FROM TB_TBM_SESSION_CONTENT               WHERE CMPNY_CD = '6NQaZGt7L5STZqgekcbv';
DELETE FROM TB_TBM_SESSION                       WHERE CMPNY_CD = '6NQaZGt7L5STZqgekcbv';
DELETE FROM TB_TBM_EDU_MTRL_ITEM                 WHERE MTRL_CD IN (
                                                     SELECT MTRL_CD FROM TB_TBM_EDU_MTRL
                                                      WHERE CMPNY_CD = '6NQaZGt7L5STZqgekcbv'
                                                 );
DELETE FROM TB_TBM_EDU_MTRL                      WHERE CMPNY_CD = '6NQaZGt7L5STZqgekcbv';

-- ---------- STEP 4. 요청승인/연차 사용 (화면 생성분 — 있었으면) ----------
DELETE FROM TB_USER_ATTD_REQ_APPROVAL            WHERE CMPNY_CD = '6NQaZGt7L5STZqgekcbv';
DELETE FROM TB_USER_LEAVE_USE                    WHERE CMPNY_CD = '6NQaZGt7L5STZqgekcbv';
DELETE FROM TB_USER_ATTD_REQ                     WHERE CMPNY_CD = '6NQaZGt7L5STZqgekcbv';

-- ---------- STEP 5. 근태/근무계획/연차 부여/약관 동의 (시드 데이터) ----------
DELETE FROM TB_USER_ATTD_GPS                     WHERE CMPNY_CD = '6NQaZGt7L5STZqgekcbv';
DELETE FROM TB_USER_ATTD_HIST                    WHERE CMPNY_CD = '6NQaZGt7L5STZqgekcbv';
DELETE FROM TB_USER_ATTD_MGMT                    WHERE CMPNY_CD = '6NQaZGt7L5STZqgekcbv';
DELETE FROM TB_USER_LEAVE_GRANT                  WHERE CMPNY_CD = '6NQaZGt7L5STZqgekcbv';
DELETE FROM TB_USER_WORK_PLAN                    WHERE CMPNY_CD = '6NQaZGt7L5STZqgekcbv';
DELETE FROM TB_TERMS_USER_AGR_MGMT               WHERE CMPNY_CD = '6NQaZGt7L5STZqgekcbv';

-- ---------- STEP 6. 프로비저닝 산출물 (회사·사업장·사용자·권한 등 전부) ----------
DELETE FROM TB_RISK_SITE_HAZARD                  WHERE CMPNY_CD = '6NQaZGt7L5STZqgekcbv';
DELETE FROM TB_RISK_TYPE                         WHERE CMPNY_CD = '6NQaZGt7L5STZqgekcbv';
DELETE FROM TB_LEAVE_POLICY_HISTORY              WHERE CMPNY_CD = '6NQaZGt7L5STZqgekcbv';
DELETE FROM TB_LEAVE_USAGE_POLICY                WHERE CMPNY_CD = '6NQaZGt7L5STZqgekcbv';
DELETE FROM TB_LEAVE_POLICY                      WHERE CMPNY_CD = '6NQaZGt7L5STZqgekcbv';
DELETE FROM TB_LEAVE_TYPE_MGMT                   WHERE CMPNY_CD = '6NQaZGt7L5STZqgekcbv';
DELETE FROM TB_SCH_MGMT                          WHERE CMPNY_CD = '6NQaZGt7L5STZqgekcbv';
DELETE FROM TB_CMM_SEQ                           WHERE CMPNY_CD = '6NQaZGt7L5STZqgekcbv';
DELETE FROM TB_BAIM_VAL_D                        WHERE CMPNY_CD = '6NQaZGt7L5STZqgekcbv';
DELETE FROM TB_BAIM_VAL_M                        WHERE CMPNY_CD = '6NQaZGt7L5STZqgekcbv';
DELETE FROM TB_SYST_AUTH_MENU                    WHERE CMPNY_CD = '6NQaZGt7L5STZqgekcbv';
DELETE FROM TB_USER_SITE_AUTH                    WHERE CMPNY_CD = '6NQaZGt7L5STZqgekcbv';
DELETE FROM TB_SITE_NODE                         WHERE CMPNY_CD = '6NQaZGt7L5STZqgekcbv';
DELETE FROM TB_USER                              WHERE CMPNY_CD = '6NQaZGt7L5STZqgekcbv';
DELETE FROM TB_SITE                              WHERE CMPNY_CD = '6NQaZGt7L5STZqgekcbv';
DELETE FROM TB_CMPNY                             WHERE CMPNY_CD = '6NQaZGt7L5STZqgekcbv';

SET SQL_SAFE_UPDATES = 1;

-- ---------- STEP 7. 삭제 후 검증 — 기대: 전부 0 ----------
SELECT (SELECT COUNT(1) FROM TB_CMPNY WHERE CMPNY_CD = '6NQaZGt7L5STZqgekcbv') AS CMPNY_CNT
     , (SELECT COUNT(1) FROM TB_USER  WHERE CMPNY_CD = '6NQaZGt7L5STZqgekcbv') AS USER_CNT
     , (SELECT COUNT(1) FROM TB_SITE  WHERE CMPNY_CD = '6NQaZGt7L5STZqgekcbv') AS SITE_CNT;

-- ============================================================================
-- (끝) STEP 7 결과가 전부 0이면 데모 회사 원복 완료.
-- ============================================================================
