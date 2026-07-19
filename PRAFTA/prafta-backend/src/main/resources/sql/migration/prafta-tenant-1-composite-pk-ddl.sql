-- =====================================================================
-- PRAFTA 멀티테넌시 결함 수정 (1/3) — PK 복합키 전환
--   작성: 2026-07-12
--   배경: ID 채번은 회사별(FNC_CMM_SEQ_NEXTVAL(cmpnyCd, ...))인데 PK 는 ID 단독이라
--         **2번째 고객사부터 PK 충돌로 INSERT 가 실패**한다(실증: 신규사 2곳의 첫 근태가
--         둘 다 ATTD_ID='2026071200001' → Duplicate entry).
--         이미 60개 테이블은 (CMPNY_CD, ID) 복합키 관례를 따르고 있고, 아래 22개만 예외다.
--
--   ★ 실행 전 반드시 백업. 트랜잭션으로 감싸고 검증 후 COMMIT.
--   ★★ START TRANSACTION 으로 열리므로 반드시 COMMIT 또는 ROLLBACK 으로 닫을 것
--       (열어두면 행 락이 유지되어 애플리케이션 전체가 멈춘다).
--   ※ DDL(ALTER)은 MySQL 에서 암묵적 커밋을 유발하므로 트랜잭션으로 롤백되지 않는다.
--      따라서 본 스크립트는 트랜잭션을 쓰지 않고, 단계별 검증 SELECT 로 확인한다.
--      실패 시 §9 롤백 스크립트로 원복한다.
-- =====================================================================

-- ---------------------------------------------------------------------
-- 0. 사전 점검 — 전환 대상이 맞는지 확인(모두 'ID 단독 PK' 여야 한다)
-- ---------------------------------------------------------------------
SELECT TABLE_NAME, GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) AS PK_COLS
  FROM information_schema.STATISTICS
 WHERE TABLE_SCHEMA = DATABASE()
   AND INDEX_NAME = 'PRIMARY'
   AND TABLE_NAME IN (
        'tb_user_attd_mgmt','tb_user_attd_req','tb_user_attd_req_approval','tb_user_attd_hist'
      , 'tb_user_leave_use','tb_user_leave_grant','tb_user_overtime_mgmt','tb_noti_outbox'
      , 'tb_leave_change_request','tb_leave_policy_history','tb_leave_promotion_log','tb_leave_refusal_log'
      , 'tb_user_hire_date_history','tb_user_service_credit','tb_user_upload_job'
      , 'tb_user_device_login_hist','tb_user_device_occupancy_anomaly','tb_daily_user_slot_his'
      , 'tb_audit_log','tb_tbm_session','tb_tbm_attendance','tb_tbm_edu_mtrl'
   )
 GROUP BY TABLE_NAME;

-- ---------------------------------------------------------------------
-- 1. 근태 도메인
-- ---------------------------------------------------------------------
ALTER TABLE tb_user_attd_mgmt        DROP PRIMARY KEY, ADD PRIMARY KEY (CMPNY_CD, ATTD_ID);
ALTER TABLE tb_user_attd_req         DROP PRIMARY KEY, ADD PRIMARY KEY (CMPNY_CD, REQ_ID);
ALTER TABLE tb_user_attd_req_approval DROP PRIMARY KEY, ADD PRIMARY KEY (CMPNY_CD, REQ_ID, APPROVAL_STEP);
ALTER TABLE tb_user_attd_hist        DROP PRIMARY KEY, ADD PRIMARY KEY (CMPNY_CD, HIST_ID);
ALTER TABLE tb_user_overtime_mgmt    DROP PRIMARY KEY, ADD PRIMARY KEY (CMPNY_CD, OT_ID);

-- ---------------------------------------------------------------------
-- 2. 연차 도메인
--    ※ tb_leave_policy / tb_leave_usage_policy 의 POLICY_SEQ 는 AUTO_INCREMENT(전역 유일)라
--       충돌 위험이 없어 대상에서 제외한다.
-- ---------------------------------------------------------------------
ALTER TABLE tb_user_leave_use        DROP PRIMARY KEY, ADD PRIMARY KEY (CMPNY_CD, LEAVE_ID);
ALTER TABLE tb_user_leave_grant      DROP PRIMARY KEY, ADD PRIMARY KEY (CMPNY_CD, GRANT_ID);
ALTER TABLE tb_leave_change_request  DROP PRIMARY KEY, ADD PRIMARY KEY (CMPNY_CD, CHANGE_REQ_ID);
ALTER TABLE tb_leave_policy_history  DROP PRIMARY KEY, ADD PRIMARY KEY (CMPNY_CD, HIST_ID);
ALTER TABLE tb_leave_promotion_log   DROP PRIMARY KEY, ADD PRIMARY KEY (CMPNY_CD, PROMO_ID);
ALTER TABLE tb_leave_refusal_log     DROP PRIMARY KEY, ADD PRIMARY KEY (CMPNY_CD, REFUSAL_ID);

-- ---------------------------------------------------------------------
-- 3. 사용자/계정 도메인
-- ---------------------------------------------------------------------
ALTER TABLE tb_user_hire_date_history DROP PRIMARY KEY, ADD PRIMARY KEY (CMPNY_CD, HIST_ID);
ALTER TABLE tb_user_service_credit    DROP PRIMARY KEY, ADD PRIMARY KEY (CMPNY_CD, CREDIT_ID);
ALTER TABLE tb_user_upload_job        DROP PRIMARY KEY, ADD PRIMARY KEY (CMPNY_CD, JOB_ID);
ALTER TABLE tb_daily_user_slot_his    DROP PRIMARY KEY, ADD PRIMARY KEY (CMPNY_CD, HIS_ID);

-- ---------------------------------------------------------------------
-- 4. 디바이스/알림/감사
-- ---------------------------------------------------------------------
ALTER TABLE tb_user_device_login_hist        DROP PRIMARY KEY, ADD PRIMARY KEY (CMPNY_CD, DEVICE_LOGIN_NO);
ALTER TABLE tb_user_device_occupancy_anomaly DROP PRIMARY KEY, ADD PRIMARY KEY (CMPNY_CD, ANOMALY_NO);
ALTER TABLE tb_noti_outbox                   DROP PRIMARY KEY, ADD PRIMARY KEY (CMPNY_CD, NOTI_ID);
ALTER TABLE tb_audit_log                     DROP PRIMARY KEY, ADD PRIMARY KEY (CMPNY_CD, AUDIT_ID);

-- ---------------------------------------------------------------------
-- 5. TBM 도메인
--    tb_tbm_edu_mtrl 은 tb_tbm_edu_mtrl_item 이 FK 로 참조한다.
--    자식 테이블에 CMPNY_CD 가 아예 없어(테넌트 격리 부재) 컬럼 추가 + 백필 후 복합 FK 로 재구성한다.
-- ---------------------------------------------------------------------
ALTER TABLE tb_tbm_session    DROP PRIMARY KEY, ADD PRIMARY KEY (CMPNY_CD, SESSION_CD);
ALTER TABLE tb_tbm_attendance DROP PRIMARY KEY, ADD PRIMARY KEY (CMPNY_CD, ATTENDANCE_CD);

-- 5-1) 자식 FK 제거(부모 PK 를 바꾸려면 먼저 끊어야 한다)
ALTER TABLE tb_tbm_edu_mtrl_item DROP FOREIGN KEY FK_TBM_EDU_MTRL_ITEM_01;

-- 5-2) 자식에 CMPNY_CD 추가 + 부모에서 백필(기존 행은 부모의 회사코드를 그대로 상속)
ALTER TABLE tb_tbm_edu_mtrl_item
    ADD COLUMN CMPNY_CD VARCHAR(50) NOT NULL DEFAULT '' COMMENT '회사코드' AFTER MTRL_ITEM_CD;

UPDATE tb_tbm_edu_mtrl_item I
  JOIN tb_tbm_edu_mtrl M ON M.MTRL_CD = I.MTRL_CD
   SET I.CMPNY_CD = M.CMPNY_CD;

-- 백필 누락(고아 행) 확인 — 0 이어야 한다.
SELECT COUNT(*) AS orphan_items FROM tb_tbm_edu_mtrl_item WHERE CMPNY_CD = '';

-- 5-3) 부모 PK 복합키 전환
ALTER TABLE tb_tbm_edu_mtrl DROP PRIMARY KEY, ADD PRIMARY KEY (CMPNY_CD, MTRL_CD);

-- 5-4) 자식 PK 복합키 전환 + 복합 FK 재생성
ALTER TABLE tb_tbm_edu_mtrl_item DROP PRIMARY KEY, ADD PRIMARY KEY (CMPNY_CD, MTRL_ITEM_CD);
ALTER TABLE tb_tbm_edu_mtrl_item
    ADD CONSTRAINT FK_TBM_EDU_MTRL_ITEM_01
        FOREIGN KEY (CMPNY_CD, MTRL_CD) REFERENCES tb_tbm_edu_mtrl (CMPNY_CD, MTRL_CD);

-- ---------------------------------------------------------------------
-- 6. 검증 — 대상 테이블 PK 가 모두 CMPNY_CD 로 시작해야 한다(결과 0행이면 성공).
-- ---------------------------------------------------------------------
SELECT TABLE_NAME, GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) AS PK_COLS
  FROM information_schema.STATISTICS
 WHERE TABLE_SCHEMA = DATABASE()
   AND INDEX_NAME = 'PRIMARY'
   AND TABLE_NAME IN (
        'tb_user_attd_mgmt','tb_user_attd_req','tb_user_attd_req_approval','tb_user_attd_hist'
      , 'tb_user_leave_use','tb_user_leave_grant','tb_user_overtime_mgmt','tb_noti_outbox'
      , 'tb_leave_change_request','tb_leave_policy_history','tb_leave_promotion_log','tb_leave_refusal_log'
      , 'tb_user_hire_date_history','tb_user_service_credit','tb_user_upload_job'
      , 'tb_user_device_login_hist','tb_user_device_occupancy_anomaly','tb_daily_user_slot_his'
      , 'tb_audit_log','tb_tbm_session','tb_tbm_attendance','tb_tbm_edu_mtrl','tb_tbm_edu_mtrl_item'
   )
 GROUP BY TABLE_NAME
HAVING PK_COLS NOT LIKE 'CMPNY_CD%';

-- ---------------------------------------------------------------------
-- 9. 롤백(필요 시) — PK 를 원래대로 되돌린다.
--    ※ 이미 2개 이상 회사에서 같은 ID 가 생성된 뒤에는 원복이 불가능하다(중복 위반).
--       롤백은 "전환 직후 문제가 발견됐고 신규 데이터가 없을 때"만 유효하다.
-- ---------------------------------------------------------------------
-- ALTER TABLE tb_tbm_edu_mtrl_item DROP FOREIGN KEY FK_TBM_EDU_MTRL_ITEM_01;
-- ALTER TABLE tb_tbm_edu_mtrl_item DROP PRIMARY KEY, ADD PRIMARY KEY (MTRL_ITEM_CD);
-- ALTER TABLE tb_tbm_edu_mtrl_item DROP COLUMN CMPNY_CD;
-- ALTER TABLE tb_tbm_edu_mtrl  DROP PRIMARY KEY, ADD PRIMARY KEY (MTRL_CD);
-- ALTER TABLE tb_tbm_edu_mtrl_item ADD CONSTRAINT FK_TBM_EDU_MTRL_ITEM_01
--     FOREIGN KEY (MTRL_CD) REFERENCES tb_tbm_edu_mtrl (MTRL_CD);
-- ALTER TABLE tb_tbm_attendance DROP PRIMARY KEY, ADD PRIMARY KEY (ATTENDANCE_CD);
-- ALTER TABLE tb_tbm_session    DROP PRIMARY KEY, ADD PRIMARY KEY (SESSION_CD);
-- ALTER TABLE tb_audit_log      DROP PRIMARY KEY, ADD PRIMARY KEY (AUDIT_ID);
-- ALTER TABLE tb_noti_outbox    DROP PRIMARY KEY, ADD PRIMARY KEY (NOTI_ID);
-- ALTER TABLE tb_user_device_occupancy_anomaly DROP PRIMARY KEY, ADD PRIMARY KEY (ANOMALY_NO);
-- ALTER TABLE tb_user_device_login_hist DROP PRIMARY KEY, ADD PRIMARY KEY (DEVICE_LOGIN_NO);
-- ALTER TABLE tb_daily_user_slot_his   DROP PRIMARY KEY, ADD PRIMARY KEY (HIS_ID);
-- ALTER TABLE tb_user_upload_job       DROP PRIMARY KEY, ADD PRIMARY KEY (JOB_ID);
-- ALTER TABLE tb_user_service_credit   DROP PRIMARY KEY, ADD PRIMARY KEY (CREDIT_ID);
-- ALTER TABLE tb_user_hire_date_history DROP PRIMARY KEY, ADD PRIMARY KEY (HIST_ID);
-- ALTER TABLE tb_leave_refusal_log     DROP PRIMARY KEY, ADD PRIMARY KEY (REFUSAL_ID);
-- ALTER TABLE tb_leave_promotion_log   DROP PRIMARY KEY, ADD PRIMARY KEY (PROMO_ID);
-- ALTER TABLE tb_leave_policy_history  DROP PRIMARY KEY, ADD PRIMARY KEY (HIST_ID);
-- ALTER TABLE tb_leave_change_request  DROP PRIMARY KEY, ADD PRIMARY KEY (CHANGE_REQ_ID);
-- ALTER TABLE tb_user_leave_grant      DROP PRIMARY KEY, ADD PRIMARY KEY (GRANT_ID);
-- ALTER TABLE tb_user_leave_use        DROP PRIMARY KEY, ADD PRIMARY KEY (LEAVE_ID);
-- ALTER TABLE tb_user_overtime_mgmt    DROP PRIMARY KEY, ADD PRIMARY KEY (OT_ID);
-- ALTER TABLE tb_user_attd_hist        DROP PRIMARY KEY, ADD PRIMARY KEY (HIST_ID);
-- ALTER TABLE tb_user_attd_req_approval DROP PRIMARY KEY, ADD PRIMARY KEY (REQ_ID, APPROVAL_STEP);
-- ALTER TABLE tb_user_attd_req         DROP PRIMARY KEY, ADD PRIMARY KEY (REQ_ID);
-- ALTER TABLE tb_user_attd_mgmt        DROP PRIMARY KEY, ADD PRIMARY KEY (ATTD_ID);
