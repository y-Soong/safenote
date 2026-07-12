-- =============================================================================
-- PRAFTA 테스트 고객사(DMO, DMO_CMPNY) 데이터 삭제 스크립트
-- =============================================================================
-- 목적   : 회사명 'DMO', 'DMO_CMPNY' 에 속한 모든 업무 데이터 삭제
-- 생성   : 실제 DB 스키마(information_schema) 조회 기반으로 작성됨
-- 주의   : 데이터를 영구 삭제합니다. 실행 전 반드시 백업하세요.
--
-- [안전 설계]
--  1) CMPNY_CD(전역 유일) 로만 스코프 → 다른 회사 데이터는 절대 건드리지 않음.
--  2) 회사코드는 회사명으로 자동 해석(@dmo/@dmo2). 없으면 NULL → 해당 회사는 no-op.
--  3) 전역 공유 테이블(tb_syst_val_*, tb_syst_menu_*, tb_terms, tb_terms_id_version,
--     tb_acct_legal_step_master, tb_sms_auth_code, seq_site_cd)은 삭제 대상에서 제외.
--  4) CMPNY_CD 컬럼이 없는 회사 종속 자식:
--       - tb_tbm_edu_mtrl_item : 부모(tb_tbm_edu_mtrl) 조인으로 정확히 삭제(부모보다 먼저).
--       - tb_user_device       : 대상 회사 USER_CD 행 0건이라 영향 없음(아래 [제외] 참고).
--       - tb_terms_user_agr_mgmt: USER_CD 가 회사 간 충돌(다른 회사도 동일 USER_CD '20260600001'
--                                  사용)하고 CMPNY_CD 컬럼이 없어 대상만 골라낼 수 없음 → 안전상 [제외].
--  5) 트랜잭션으로 감싸고, 커밋 전 검증 SELECT 제공.
--
-- [실행 방법]
--   mysql -u <user> -p <DB명> < prafta-delete-test-company-data.sql
--   또는 MySQL Workbench 에서 스크립트 열고 실행.
-- =============================================================================

-- 대상 회사코드 해석(회사명 기준). 없으면 NULL → IN (NULL) 은 아무 행도 매칭 안 함.
SET @dmo  = (SELECT CMPNY_CD FROM tb_cmpny WHERE CMPNY_NM = 'DMO'       LIMIT 1);
SET @dmo2 = (SELECT CMPNY_CD FROM tb_cmpny WHERE CMPNY_NM = 'DMO_CMPNY' LIMIT 1);

-- -----------------------------------------------------------------------------
-- [사전 검증] 삭제 직전, 대상 회사/사용자 건수 확인 (선택 실행 권장)
-- -----------------------------------------------------------------------------
SELECT @dmo AS dmo_cmpny_cd, @dmo2 AS dmo2_cmpny_cd;
SELECT CMPNY_CD, CMPNY_NM FROM tb_cmpny WHERE CMPNY_CD IN (@dmo, @dmo2);
SELECT COUNT(*) AS user_cnt_to_delete FROM tb_user WHERE CMPNY_CD IN (@dmo, @dmo2);

-- -----------------------------------------------------------------------------
-- [삭제] 트랜잭션 시작. FK 제약은 세션 한정으로 잠시 해제(삭제 순서 부담 제거).
-- 검증 후 문제 없으면 맨 아래 COMMIT, 이상하면 ROLLBACK.
-- -----------------------------------------------------------------------------
START TRANSACTION;
SET FOREIGN_KEY_CHECKS = 0;
-- Workbench 세이프 업데이트 모드(Error 1175) 회피 — 세션 한정.
SET SQL_SAFE_UPDATES = 0;

-- (1) CMPNY_CD 가 없는 회사 종속 자식: 부모 조인으로 먼저 삭제
DELETE FROM tb_tbm_edu_mtrl_item
 WHERE MTRL_CD IN (SELECT MTRL_CD FROM tb_tbm_edu_mtrl WHERE CMPNY_CD IN (@dmo, @dmo2));

-- (2) CMPNY_CD 스코프 테이블 일괄 삭제 (tb_user, tb_cmpny 제외 — 마지막에 처리)
DELETE FROM tb_acct WHERE CMPNY_CD IN (@dmo, @dmo2);
DELETE FROM tb_acct_legal_step WHERE CMPNY_CD IN (@dmo, @dmo2);
DELETE FROM tb_acct_link WHERE CMPNY_CD IN (@dmo, @dmo2);
DELETE FROM tb_aprv_line_preset WHERE CMPNY_CD IN (@dmo, @dmo2);
DELETE FROM tb_aprv_line_preset_d WHERE CMPNY_CD IN (@dmo, @dmo2);
DELETE FROM tb_attd_close WHERE CMPNY_CD IN (@dmo, @dmo2);
DELETE FROM tb_attd_close_hist WHERE CMPNY_CD IN (@dmo, @dmo2);
DELETE FROM tb_audit_log WHERE CMPNY_CD IN (@dmo, @dmo2);
DELETE FROM tb_auth_token WHERE CMPNY_CD IN (@dmo, @dmo2);
DELETE FROM tb_baim_val_d WHERE CMPNY_CD IN (@dmo, @dmo2);
DELETE FROM tb_baim_val_m WHERE CMPNY_CD IN (@dmo, @dmo2);
DELETE FROM tb_chkpt_defect_action WHERE CMPNY_CD IN (@dmo, @dmo2);
DELETE FROM tb_chkpt_inspect_answer WHERE CMPNY_CD IN (@dmo, @dmo2);
DELETE FROM tb_chkpt_inspect_item WHERE CMPNY_CD IN (@dmo, @dmo2);
DELETE FROM tb_chkpt_type_mgmt WHERE CMPNY_CD IN (@dmo, @dmo2);
DELETE FROM tb_cmm_seq WHERE CMPNY_CD IN (@dmo, @dmo2);
DELETE FROM tb_daily_blacklist WHERE CMPNY_CD IN (@dmo, @dmo2);
DELETE FROM tb_daily_link_mgmt WHERE CMPNY_CD IN (@dmo, @dmo2);
DELETE FROM tb_daily_user WHERE CMPNY_CD IN (@dmo, @dmo2);
DELETE FROM tb_daily_user_link_policy WHERE CMPNY_CD IN (@dmo, @dmo2);
DELETE FROM tb_daily_user_slot WHERE CMPNY_CD IN (@dmo, @dmo2);
DELETE FROM tb_daily_user_slot_his WHERE CMPNY_CD IN (@dmo, @dmo2);
DELETE FROM tb_del_user WHERE CMPNY_CD IN (@dmo, @dmo2);
DELETE FROM tb_file_info WHERE CMPNY_CD IN (@dmo, @dmo2);
DELETE FROM tb_holiday WHERE CMPNY_CD IN (@dmo, @dmo2);
DELETE FROM tb_holiday_rule WHERE CMPNY_CD IN (@dmo, @dmo2);
DELETE FROM tb_leave_change_request WHERE CMPNY_CD IN (@dmo, @dmo2);
DELETE FROM tb_leave_policy WHERE CMPNY_CD IN (@dmo, @dmo2);
DELETE FROM tb_leave_policy_history WHERE CMPNY_CD IN (@dmo, @dmo2);
DELETE FROM tb_leave_promotion_log WHERE CMPNY_CD IN (@dmo, @dmo2);
DELETE FROM tb_leave_refusal_log WHERE CMPNY_CD IN (@dmo, @dmo2);
DELETE FROM tb_leave_type_mgmt WHERE CMPNY_CD IN (@dmo, @dmo2);
DELETE FROM tb_leave_usage_policy WHERE CMPNY_CD IN (@dmo, @dmo2);
DELETE FROM tb_near_miss WHERE CMPNY_CD IN (@dmo, @dmo2);
DELETE FROM tb_noti_outbox WHERE CMPNY_CD IN (@dmo, @dmo2);
DELETE FROM tb_notice WHERE CMPNY_CD IN (@dmo, @dmo2);
DELETE FROM tb_notice_file WHERE CMPNY_CD IN (@dmo, @dmo2);
DELETE FROM tb_notice_target WHERE CMPNY_CD IN (@dmo, @dmo2);
DELETE FROM tb_notice_user_ack WHERE CMPNY_CD IN (@dmo, @dmo2);
DELETE FROM tb_risk_assessment WHERE CMPNY_CD IN (@dmo, @dmo2);
DELETE FROM tb_risk_improvement_item WHERE CMPNY_CD IN (@dmo, @dmo2);
DELETE FROM tb_risk_near_miss_link WHERE CMPNY_CD IN (@dmo, @dmo2);
DELETE FROM tb_risk_site_hazard WHERE CMPNY_CD IN (@dmo, @dmo2);
DELETE FROM tb_risk_type WHERE CMPNY_CD IN (@dmo, @dmo2);
DELETE FROM tb_sch_mgmt WHERE CMPNY_CD IN (@dmo, @dmo2);
DELETE FROM tb_sch_mgmt_hist WHERE CMPNY_CD IN (@dmo, @dmo2);
DELETE FROM tb_shift_sch_assign_mgmt WHERE CMPNY_CD IN (@dmo, @dmo2);
DELETE FROM tb_shift_sch_mgmt WHERE CMPNY_CD IN (@dmo, @dmo2);
DELETE FROM tb_shift_sch_ptrn_mgmt WHERE CMPNY_CD IN (@dmo, @dmo2);
DELETE FROM tb_shift_sch_team_meta_info WHERE CMPNY_CD IN (@dmo, @dmo2);
DELETE FROM tb_shift_sch_team_mgmt WHERE CMPNY_CD IN (@dmo, @dmo2);
DELETE FROM tb_shift_sch_team_user WHERE CMPNY_CD IN (@dmo, @dmo2);
DELETE FROM tb_site WHERE CMPNY_CD IN (@dmo, @dmo2);
DELETE FROM tb_site_node WHERE CMPNY_CD IN (@dmo, @dmo2);
DELETE FROM tb_syst_auth_menu WHERE CMPNY_CD IN (@dmo, @dmo2);
DELETE FROM tb_tbm_attendance WHERE CMPNY_CD IN (@dmo, @dmo2);
DELETE FROM tb_tbm_attendance_event WHERE CMPNY_CD IN (@dmo, @dmo2);
DELETE FROM tb_tbm_edu_mtrl WHERE CMPNY_CD IN (@dmo, @dmo2);
DELETE FROM tb_tbm_pwd_fail WHERE CMPNY_CD IN (@dmo, @dmo2);
DELETE FROM tb_tbm_session WHERE CMPNY_CD IN (@dmo, @dmo2);
DELETE FROM tb_tbm_session_content WHERE CMPNY_CD IN (@dmo, @dmo2);
DELETE FROM tb_tbm_session_risk WHERE CMPNY_CD IN (@dmo, @dmo2);
DELETE FROM tb_tbm_session_state WHERE CMPNY_CD IN (@dmo, @dmo2);
DELETE FROM tb_user_attd_gps WHERE CMPNY_CD IN (@dmo, @dmo2);
DELETE FROM tb_user_attd_hist WHERE CMPNY_CD IN (@dmo, @dmo2);
DELETE FROM tb_user_attd_mgmt WHERE CMPNY_CD IN (@dmo, @dmo2);
DELETE FROM tb_user_attd_req WHERE CMPNY_CD IN (@dmo, @dmo2);
DELETE FROM tb_user_attd_req_approval WHERE CMPNY_CD IN (@dmo, @dmo2);
DELETE FROM tb_user_device_login_hist WHERE CMPNY_CD IN (@dmo, @dmo2);
DELETE FROM tb_user_device_occupancy_anomaly WHERE CMPNY_CD IN (@dmo, @dmo2);
DELETE FROM tb_user_hire_date_history WHERE CMPNY_CD IN (@dmo, @dmo2);
DELETE FROM tb_user_leave_grant WHERE CMPNY_CD IN (@dmo, @dmo2);
DELETE FROM tb_user_leave_use WHERE CMPNY_CD IN (@dmo, @dmo2);
DELETE FROM tb_user_menu_favorite WHERE CMPNY_CD IN (@dmo, @dmo2);
DELETE FROM tb_user_overtime_mgmt WHERE CMPNY_CD IN (@dmo, @dmo2);
DELETE FROM tb_user_push_setting WHERE CMPNY_CD IN (@dmo, @dmo2);
DELETE FROM tb_user_service_credit WHERE CMPNY_CD IN (@dmo, @dmo2);
DELETE FROM tb_user_site_auth WHERE CMPNY_CD IN (@dmo, @dmo2);
DELETE FROM tb_user_upload_job WHERE CMPNY_CD IN (@dmo, @dmo2);
DELETE FROM tb_user_work_plan WHERE CMPNY_CD IN (@dmo, @dmo2);

-- (3) 사용자 → (4) 회사 마스터 순으로 마지막 삭제
DELETE FROM tb_user WHERE CMPNY_CD IN (@dmo, @dmo2);
DELETE FROM tb_cmpny WHERE CMPNY_CD IN (@dmo, @dmo2);

SET FOREIGN_KEY_CHECKS = 1;
SET SQL_SAFE_UPDATES = 1;

-- -----------------------------------------------------------------------------
-- [검증] 아래 결과가 모두 0 이면 정상 삭제. 이상 시 COMMIT 대신 ROLLBACK 하세요.
-- -----------------------------------------------------------------------------
SELECT 'tb_cmpny' AS tbl, COUNT(*) AS remain FROM tb_cmpny WHERE CMPNY_CD IN (@dmo, @dmo2)
UNION ALL SELECT 'tb_user', COUNT(*) FROM tb_user WHERE CMPNY_CD IN (@dmo, @dmo2)
UNION ALL SELECT 'tb_site', COUNT(*) FROM tb_site WHERE CMPNY_CD IN (@dmo, @dmo2)
UNION ALL SELECT 'tb_user_site_auth', COUNT(*) FROM tb_user_site_auth WHERE CMPNY_CD IN (@dmo, @dmo2);

-- 확인 후 아래 한 줄의 주석을 풀어 확정하거나, 문제 시 ROLLBACK; 하세요.
COMMIT;
-- ROLLBACK;

-- =============================================================================
-- [제외/주의 — 수동 검토 필요]
--  · tb_terms_user_agr_mgmt : USER_CD 가 다른 회사와 충돌(공통 '20260600001')하고 CMPNY_CD
--      컬럼이 없어 대상 회사 행만 안전하게 골라낼 수 없으므로 이 스크립트에서 삭제하지 않음.
--      (현재 해당 USER_CD 약관동의 6건은 여러 회사 공유 가능성. 정말 지우려면 운영자가
--       대상 사용자 식별 후 개별 PK(USER_CD,TERMS_ID,TERMS_VERSION) 로 직접 삭제할 것.)
--  · tb_user_device : 대상 회사 USER_CD 의 디바이스 행 0건이라 영향 없음(동일 충돌 이슈 존재).
--  · tb_file_info 삭제는 DB 행만 지웁니다. 실제 업로드 파일(디스크/스토리지) 정리는 별도.
-- =============================================================================
