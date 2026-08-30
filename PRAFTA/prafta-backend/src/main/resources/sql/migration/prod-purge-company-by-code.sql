-- ============================================================================
-- prod-purge-company-by-code : 회사 단위 데이터 전량 삭제 (재사용 템플릿, 2026-08-31)
-- ============================================================================
-- 목적
--   지정한 **회사코드 1개**의 모든 데이터를 삭제한다(계정·근태·연차·TBM·안전관리·기준정보 전부).
--   테스트로 만든 고객사를 정리할 때 반복 사용한다. 001(테스트 기준 회사)과
--   prafta_system_admin(플랫폼 관리자)은 스크립트가 스스로 거부한다.
--
-- ★★ 사용법: 아래 [1] 구획의 '{{TARGET_CMPNY_CD}}' 한 곳만 실제 회사코드로 바꾼 뒤,
--            스크립트 전체를 1회 실행한다. 그 외에는 아무것도 고치지 않는다.
--
-- ★★ 실행 주체: 사용자가 MySQL Workbench 로 직접 실행 (CLAUDE.md — Claude 는 운영 DB 에 쓰지 않는다).
-- ★★ 실행 전 필수: RDS 수동 스냅샷 (예: prafta-db-before-purge-20260831). 되돌릴 방법은 이것뿐이다.
-- ★★ 실행 전 권장: 백엔드 앱 정지(삭제 중 신규 기록이 섞이면 고아가 다시 생긴다).
--                  ssh ... "sudo systemctl stop prafta-backend"  → 완료 후 start
-- ★★ Workbench: Query 메뉴의 "Stop Script Execution on Errors" 가 켜져 있어야 한다
--               (아래 가드가 "오류로 정지"에 의존한다. 계속 실행 옵션 금지).
--
-- 안전장치
--   가드1 회사코드 미교체(플레이스홀더) 차단
--   가드2 보존 회사(001 / prafta_system_admin) 지정 시 차단  ← 오지정 사고 방지
--   가드3 대상 회사가 실제로 존재하는지 확인(오타 차단)
--   가드4 같은 회사 이중 실행 차단(마커)
--   전체를 단일 트랜잭션으로 감싼다(중간 실패 시 전부 롤백)
--
-- 설계 메모
--   - 회사코드 비교는 임시 테이블(tmp_purge_cmpny, 동일 collation)과의 컬럼 비교로만 한다.
--     ★사용자변수(SET @x)를 컬럼과 직접 비교하면 collation 오류(1267)가 실제로 발생한 이력이 있다.
--   - 회사 스코프 테이블 102개는 운영 information_schema 실측 목록이며 누락 0건으로 대조 검증했다.
--   - 회사 간 연계(협력사/사업장연계/정보공유/TBM 공유)는 CMPNY_CD 컬럼이 없어 별도 조건으로 지운다.
--
-- 이 스크립트가 하지 않는 것
--   - EC2 디스크의 업로드 실파일 삭제(DB 행만 지운다 → 디스크에 고아 파일이 남는다).
--   - 전사 공용 테이블(tb_syst_val_*, tb_syst_menu_*, tb_terms*, tb_sms_send_policy,
--     tb_acct_legal_step_master, seq_site_cd)은 건드리지 않는다.
-- ============================================================================

SET SQL_SAFE_UPDATES = 0;
SET SESSION innodb_lock_wait_timeout = 120;


-- ============================================================================
-- [1] 삭제 대상 회사코드 지정  ★★ 여기 한 줄만 바꾼다 ★★
-- ============================================================================
DROP TABLE IF EXISTS tmp_purge_cmpny;
CREATE TABLE tmp_purge_cmpny (
  CMPNY_CD varchar(50) NOT NULL,
  PRIMARY KEY (CMPNY_CD)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='prod-purge-company 대상 회사. 종료 후 DROP';

INSERT INTO tmp_purge_cmpny (CMPNY_CD) VALUES ('{{TARGET_CMPNY_CD}}');


-- ============================================================================
-- [2] 가드 — 하나라도 어긋나면 오류(1242)로 즉시 정지한다
-- ============================================================================

-- 가드1: 회사코드를 실제 값으로 바꿨는지
SELECT IF((SELECT COUNT(*) FROM tmp_purge_cmpny WHERE CMPNY_CD LIKE '{{%') = 0,
          1, (SELECT 1 UNION ALL SELECT 2)) AS guard_placeholder;

-- 가드2: 보존 회사를 지정하지 않았는지(001 / prafta_system_admin 는 삭제 불가)
SELECT IF((SELECT COUNT(*) FROM tmp_purge_cmpny
            WHERE CMPNY_CD IN ('001','prafta_system_admin')) = 0,
          1, (SELECT 1 UNION ALL SELECT 2)) AS guard_protected_company;

-- 가드3: 대상 회사가 실제로 존재하는지(오타면 여기서 정지)
SELECT IF((SELECT COUNT(*) FROM tb_cmpny C
             JOIN tmp_purge_cmpny T ON T.CMPNY_CD = C.CMPNY_CD) = 1,
          1, (SELECT 1 UNION ALL SELECT 2)) AS guard_company_exists;

-- 실행 전 규모 확인(참고 출력)
SELECT T.CMPNY_CD
     , (SELECT COUNT(*) FROM tb_user U           WHERE U.CMPNY_CD = T.CMPNY_CD) AS users
     , (SELECT COUNT(*) FROM tb_daily_user D     WHERE D.CMPNY_CD = T.CMPNY_CD) AS daily_users
     , (SELECT COUNT(*) FROM tb_site S           WHERE S.CMPNY_CD = T.CMPNY_CD) AS sites
     , (SELECT COUNT(*) FROM tb_user_attd_mgmt A WHERE A.CMPNY_CD = T.CMPNY_CD) AS attd
     , (SELECT COUNT(*) FROM tb_user_work_plan W WHERE W.CMPNY_CD = T.CMPNY_CD) AS work_plan
     , (SELECT COUNT(*) FROM tb_tbm_session M    WHERE M.CMPNY_CD = T.CMPNY_CD) AS tbm_sessions
     , (SELECT COUNT(*) FROM tb_file_info F      WHERE F.CMPNY_CD = T.CMPNY_CD) AS files
  FROM tmp_purge_cmpny T;

-- 이중 실행 가드 마커(회사코드별로 1회). 재실행 시 Duplicate entry 오류로 정지.
CREATE TABLE IF NOT EXISTS tz_migration_marker (
  STEP        varchar(100) NOT NULL COMMENT '마이그레이션 단계 식별자',
  EXECUTED_AT datetime     NOT NULL COMMENT '실행 시각(KST)',
  PRIMARY KEY (STEP)
) COMMENT='마이그레이션 실행 마커';


-- ============================================================================
-- [2-1] 삭제 대상 보조 목록 수집 (조회 전용 — 반드시 트랜잭션 "밖"에서 만든다)
--   ★★MySQL 은 CREATE/DROP TABLE 을 만나면 진행 중인 트랜잭션을 암묵 커밋한다.
--     이 DDL 이 START TRANSACTION 안에 있으면 마커 INSERT 가 그 시점에 확정되어,
--     이후 단계가 실패해도 마커만 남아 재실행이 막힌다(2026-08-31 PREFLIGHT01 실사고).
--     그래서 모든 임시 테이블 생성은 트랜잭션 시작 전에 끝낸다.
-- ============================================================================

-- 2-1-a. 삭제 대상 공유 스냅샷 ID (제공사가 대상 회사이거나, 대상 회사가 낀 요청의 스냅샷)
DROP TABLE IF EXISTS tmp_purge_snapshot;
CREATE TABLE tmp_purge_snapshot (
  SNAPSHOT_ID bigint NOT NULL,
  PRIMARY KEY (SNAPSHOT_ID)
) COMMENT='prod-purge-company 대상 스냅샷. 종료 후 DROP';

INSERT IGNORE INTO tmp_purge_snapshot (SNAPSHOT_ID)
SELECT S.SNAPSHOT_ID
  FROM tb_cmpny_share_snapshot S
 WHERE S.OWNER_CMPNY_CD IN (SELECT CMPNY_CD FROM tmp_purge_cmpny)
    OR S.SHARE_REQ_ID IN (
         SELECT R.SHARE_REQ_ID FROM tb_cmpny_share_req R
          WHERE R.REQ_CMPNY_CD IN (SELECT CMPNY_CD FROM tmp_purge_cmpny)
             OR R.PRV_CMPNY_CD IN (SELECT CMPNY_CD FROM tmp_purge_cmpny));

-- 2-1-b. 삭제 대상 TBM 세션 코드(타사 참석자 출결 정리용 — 아래 3-7)
DROP TABLE IF EXISTS tmp_purge_session;
CREATE TABLE tmp_purge_session (
  SESSION_CD varchar(20) NOT NULL,
  PRIMARY KEY (SESSION_CD)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='prod-purge-company 대상 TBM 세션. 종료 후 DROP';

INSERT IGNORE INTO tmp_purge_session (SESSION_CD)
SELECT SESSION_CD FROM tb_tbm_session
 WHERE CMPNY_CD IN (SELECT CMPNY_CD FROM tmp_purge_cmpny);


-- ============================================================================
-- 여기서부터 COMMIT 까지는 DDL 을 넣지 말 것(원자성 보장 구간).
-- ============================================================================
START TRANSACTION;

INSERT INTO tz_migration_marker (STEP, EXECUTED_AT)
SELECT CONCAT('prod-purge-company:', CMPNY_CD), NOW() FROM tmp_purge_cmpny;


-- ============================================================================
-- [3] 회사 간 연계 데이터 — 대상 회사가 한쪽에라도 걸린 행 제거
--     ※ 남겨두면 001 쪽 화면(Subcon_01~03, TBM 출결)에 상대가 사라진 깨진 행이 남는다.
-- ============================================================================

-- 3-0. (대상 스냅샷 목록은 [2-1-a] 에서 트랜잭션 밖에 미리 수집해 두었다)

-- 3-1. 스냅샷 자식 → 부모
DELETE FROM tb_cmpny_share_snapshot_risk_improve
 WHERE SNAPSHOT_ID IN (SELECT SNAPSHOT_ID FROM tmp_purge_snapshot);
DELETE FROM tb_cmpny_share_snapshot_risk
 WHERE SNAPSHOT_ID IN (SELECT SNAPSHOT_ID FROM tmp_purge_snapshot);
DELETE FROM tb_cmpny_share_snapshot_nearmiss
 WHERE SNAPSHOT_ID IN (SELECT SNAPSHOT_ID FROM tmp_purge_snapshot);
DELETE FROM tb_cmpny_share_snapshot_attd
 WHERE SNAPSHOT_ID IN (SELECT SNAPSHOT_ID FROM tmp_purge_snapshot);
DELETE FROM tb_cmpny_share_snapshot_bundle
 WHERE OWNER_CMPNY_CD IN (SELECT CMPNY_CD FROM tmp_purge_cmpny)
    OR SNAPSHOT_ID IN (SELECT SNAPSHOT_ID FROM tmp_purge_snapshot)
    OR INCLUDED_RCV_SNAPSHOT_ID IN (SELECT SNAPSHOT_ID FROM tmp_purge_snapshot);
DELETE FROM tb_cmpny_share_snapshot
 WHERE SNAPSHOT_ID IN (SELECT SNAPSHOT_ID FROM tmp_purge_snapshot);

-- 3-2. 정보공유 요청(요청사/제공사 어느 쪽이든)
DELETE FROM tb_cmpny_share_req
 WHERE REQ_CMPNY_CD IN (SELECT CMPNY_CD FROM tmp_purge_cmpny)
    OR PRV_CMPNY_CD IN (SELECT CMPNY_CD FROM tmp_purge_cmpny);

-- 3-3. 사업장 연계
DELETE FROM tb_site_link
 WHERE SRC_CMPNY_CD IN (SELECT CMPNY_CD FROM tmp_purge_cmpny)
    OR DST_CMPNY_CD IN (SELECT CMPNY_CD FROM tmp_purge_cmpny);

-- 3-4. 협력사 관계(이력 → 본문)
DELETE FROM tb_cmpny_relation_hist
 WHERE ACTION_CMPNY_CD IN (SELECT CMPNY_CD FROM tmp_purge_cmpny)
    OR RELATION_ID IN (
         SELECT RELATION_ID FROM (
           SELECT RELATION_ID FROM tb_cmpny_relation
            WHERE REQ_CMPNY_CD IN (SELECT CMPNY_CD FROM tmp_purge_cmpny)
               OR TGT_CMPNY_CD IN (SELECT CMPNY_CD FROM tmp_purge_cmpny)
         ) X);
DELETE FROM tb_cmpny_relation
 WHERE REQ_CMPNY_CD IN (SELECT CMPNY_CD FROM tmp_purge_cmpny)
    OR TGT_CMPNY_CD IN (SELECT CMPNY_CD FROM tmp_purge_cmpny);

-- 3-5. TBM 세션 공유 지정(개최사/공유사 어느 쪽이든)
DELETE FROM tb_tbm_session_share
 WHERE HOST_CMPNY_CD IN (SELECT CMPNY_CD FROM tmp_purge_cmpny)
    OR SHARE_CMPNY_CD IN (SELECT CMPNY_CD FROM tmp_purge_cmpny);

-- 3-6. 위치정보 열람 로그(대상 회사가 열람 대상인 행)
DELETE FROM tb_location_access_log
 WHERE TARGET_CMPNY_CD IN (SELECT CMPNY_CD FROM tmp_purge_cmpny);

-- 3-7. ★대상 회사 TBM 세션에 참석한 "타사(001 등) 근로자" 출결 정리.
--      출결행은 참석자 회사 소속이라 [4] 의 CMPNY_CD 조건으로는 지워지지 않는다.
--      세션이 사라진 뒤 남으면 001 화면에 세션 없는 유령 이수 이력이 된다.
--      (대상 세션 목록은 [2-1-b] 에서 트랜잭션 밖에 미리 수집해 두었다)
DELETE FROM tb_tbm_attendance_event
 WHERE SESSION_CD IN (SELECT SESSION_CD FROM tmp_purge_session);
DELETE FROM tb_tbm_attendance
 WHERE SESSION_CD IN (SELECT SESSION_CD FROM tmp_purge_session);


-- ============================================================================
-- [4] 회사 스코프 테이블 102개 — 대상 회사 데이터 전량 삭제
--     순서: 자식 → 부모 (FK 2건 중 tb_tbm_edu_mtrl_item → tb_tbm_edu_mtrl 는 순서 필수)
-- ============================================================================

DELETE FROM tb_acct_link WHERE CMPNY_CD IN (SELECT CMPNY_CD FROM tmp_purge_cmpny);
DELETE FROM tb_acct_legal_step WHERE CMPNY_CD IN (SELECT CMPNY_CD FROM tmp_purge_cmpny);
DELETE FROM tb_acct WHERE CMPNY_CD IN (SELECT CMPNY_CD FROM tmp_purge_cmpny);
DELETE FROM tb_ai_token_usage WHERE CMPNY_CD IN (SELECT CMPNY_CD FROM tmp_purge_cmpny);
DELETE FROM tb_ai_token_quota WHERE CMPNY_CD IN (SELECT CMPNY_CD FROM tmp_purge_cmpny);
DELETE FROM tb_aprv_line_preset_d WHERE CMPNY_CD IN (SELECT CMPNY_CD FROM tmp_purge_cmpny);
DELETE FROM tb_aprv_line_preset WHERE CMPNY_CD IN (SELECT CMPNY_CD FROM tmp_purge_cmpny);
DELETE FROM tb_attd_close_hist WHERE CMPNY_CD IN (SELECT CMPNY_CD FROM tmp_purge_cmpny);
DELETE FROM tb_attd_close WHERE CMPNY_CD IN (SELECT CMPNY_CD FROM tmp_purge_cmpny);
DELETE FROM tb_audit_log WHERE CMPNY_CD IN (SELECT CMPNY_CD FROM tmp_purge_cmpny);
DELETE FROM tb_auth_token WHERE CMPNY_CD IN (SELECT CMPNY_CD FROM tmp_purge_cmpny);
DELETE FROM tb_baim_val_d WHERE CMPNY_CD IN (SELECT CMPNY_CD FROM tmp_purge_cmpny);
DELETE FROM tb_baim_val_m WHERE CMPNY_CD IN (SELECT CMPNY_CD FROM tmp_purge_cmpny);
DELETE FROM tb_chkpt_defect_action_hist WHERE CMPNY_CD IN (SELECT CMPNY_CD FROM tmp_purge_cmpny);
DELETE FROM tb_chkpt_defect_action WHERE CMPNY_CD IN (SELECT CMPNY_CD FROM tmp_purge_cmpny);
DELETE FROM tb_chkpt_inspect_answer_hist WHERE CMPNY_CD IN (SELECT CMPNY_CD FROM tmp_purge_cmpny);
DELETE FROM tb_chkpt_inspect_answer WHERE CMPNY_CD IN (SELECT CMPNY_CD FROM tmp_purge_cmpny);
DELETE FROM tb_chkpt_inspect_item_hist WHERE CMPNY_CD IN (SELECT CMPNY_CD FROM tmp_purge_cmpny);
DELETE FROM tb_chkpt_inspect_item WHERE CMPNY_CD IN (SELECT CMPNY_CD FROM tmp_purge_cmpny);
DELETE FROM tb_chkpt_type_mgmt WHERE CMPNY_CD IN (SELECT CMPNY_CD FROM tmp_purge_cmpny);
DELETE FROM tb_cmm_seq WHERE CMPNY_CD IN (SELECT CMPNY_CD FROM tmp_purge_cmpny);
DELETE FROM tb_cmpny_std_work_policy WHERE CMPNY_CD IN (SELECT CMPNY_CD FROM tmp_purge_cmpny);
DELETE FROM tb_daily_blacklist WHERE CMPNY_CD IN (SELECT CMPNY_CD FROM tmp_purge_cmpny);
DELETE FROM tb_daily_contract_sign WHERE CMPNY_CD IN (SELECT CMPNY_CD FROM tmp_purge_cmpny);
DELETE FROM tb_daily_contract WHERE CMPNY_CD IN (SELECT CMPNY_CD FROM tmp_purge_cmpny);
DELETE FROM tb_daily_entry_request WHERE CMPNY_CD IN (SELECT CMPNY_CD FROM tmp_purge_cmpny);
DELETE FROM tb_daily_link_mgmt WHERE CMPNY_CD IN (SELECT CMPNY_CD FROM tmp_purge_cmpny);
DELETE FROM tb_daily_user_slot_his WHERE CMPNY_CD IN (SELECT CMPNY_CD FROM tmp_purge_cmpny);
DELETE FROM tb_daily_user_slot WHERE CMPNY_CD IN (SELECT CMPNY_CD FROM tmp_purge_cmpny);
DELETE FROM tb_daily_user_link_policy WHERE CMPNY_CD IN (SELECT CMPNY_CD FROM tmp_purge_cmpny);
DELETE FROM tb_daily_user WHERE CMPNY_CD IN (SELECT CMPNY_CD FROM tmp_purge_cmpny);
DELETE FROM tb_del_user WHERE CMPNY_CD IN (SELECT CMPNY_CD FROM tmp_purge_cmpny);
DELETE FROM tb_file_info WHERE CMPNY_CD IN (SELECT CMPNY_CD FROM tmp_purge_cmpny);
DELETE FROM tb_holiday WHERE CMPNY_CD IN (SELECT CMPNY_CD FROM tmp_purge_cmpny);
DELETE FROM tb_holiday_rule WHERE CMPNY_CD IN (SELECT CMPNY_CD FROM tmp_purge_cmpny);
DELETE FROM tb_leave_change_request WHERE CMPNY_CD IN (SELECT CMPNY_CD FROM tmp_purge_cmpny);
DELETE FROM tb_leave_conv2_backfill_bak WHERE CMPNY_CD IN (SELECT CMPNY_CD FROM tmp_purge_cmpny);
DELETE FROM tb_leave_conv_policy_bak_fix480 WHERE CMPNY_CD IN (SELECT CMPNY_CD FROM tmp_purge_cmpny);
DELETE FROM tb_leave_conversion_policy WHERE CMPNY_CD IN (SELECT CMPNY_CD FROM tmp_purge_cmpny);
DELETE FROM tb_leave_usage_policy WHERE CMPNY_CD IN (SELECT CMPNY_CD FROM tmp_purge_cmpny);
DELETE FROM tb_leave_policy_history WHERE CMPNY_CD IN (SELECT CMPNY_CD FROM tmp_purge_cmpny);
DELETE FROM tb_leave_policy WHERE CMPNY_CD IN (SELECT CMPNY_CD FROM tmp_purge_cmpny);
DELETE FROM tb_leave_promotion_log WHERE CMPNY_CD IN (SELECT CMPNY_CD FROM tmp_purge_cmpny);
DELETE FROM tb_leave_refusal_log WHERE CMPNY_CD IN (SELECT CMPNY_CD FROM tmp_purge_cmpny);
DELETE FROM tb_leave_remnant_cover WHERE CMPNY_CD IN (SELECT CMPNY_CD FROM tmp_purge_cmpny);
DELETE FROM tb_leave_type_mgmt WHERE CMPNY_CD IN (SELECT CMPNY_CD FROM tmp_purge_cmpny);
DELETE FROM tb_risk_improvement_item WHERE CMPNY_CD IN (SELECT CMPNY_CD FROM tmp_purge_cmpny);
DELETE FROM tb_risk_near_miss_link WHERE CMPNY_CD IN (SELECT CMPNY_CD FROM tmp_purge_cmpny);
DELETE FROM tb_risk_ai_derivation WHERE CMPNY_CD IN (SELECT CMPNY_CD FROM tmp_purge_cmpny);
DELETE FROM tb_risk_assessment WHERE CMPNY_CD IN (SELECT CMPNY_CD FROM tmp_purge_cmpny);
DELETE FROM tb_risk_site_hazard WHERE CMPNY_CD IN (SELECT CMPNY_CD FROM tmp_purge_cmpny);
DELETE FROM tb_risk_type WHERE CMPNY_CD IN (SELECT CMPNY_CD FROM tmp_purge_cmpny);
DELETE FROM tb_near_miss WHERE CMPNY_CD IN (SELECT CMPNY_CD FROM tmp_purge_cmpny);
DELETE FROM tb_noti_outbox WHERE CMPNY_CD IN (SELECT CMPNY_CD FROM tmp_purge_cmpny);
DELETE FROM tb_notice_user_ack WHERE CMPNY_CD IN (SELECT CMPNY_CD FROM tmp_purge_cmpny);
DELETE FROM tb_notice_target WHERE CMPNY_CD IN (SELECT CMPNY_CD FROM tmp_purge_cmpny);
DELETE FROM tb_notice_file WHERE CMPNY_CD IN (SELECT CMPNY_CD FROM tmp_purge_cmpny);
DELETE FROM tb_notice WHERE CMPNY_CD IN (SELECT CMPNY_CD FROM tmp_purge_cmpny);
DELETE FROM tb_sch_mgmt_hist WHERE CMPNY_CD IN (SELECT CMPNY_CD FROM tmp_purge_cmpny);
DELETE FROM tb_sch_mgmt WHERE CMPNY_CD IN (SELECT CMPNY_CD FROM tmp_purge_cmpny);
DELETE FROM tb_shift_sch_assign_mgmt WHERE CMPNY_CD IN (SELECT CMPNY_CD FROM tmp_purge_cmpny);
DELETE FROM tb_shift_sch_ptrn_mgmt WHERE CMPNY_CD IN (SELECT CMPNY_CD FROM tmp_purge_cmpny);
DELETE FROM tb_shift_sch_team_user WHERE CMPNY_CD IN (SELECT CMPNY_CD FROM tmp_purge_cmpny);
DELETE FROM tb_shift_sch_team_meta_info WHERE CMPNY_CD IN (SELECT CMPNY_CD FROM tmp_purge_cmpny);
DELETE FROM tb_shift_sch_team_mgmt WHERE CMPNY_CD IN (SELECT CMPNY_CD FROM tmp_purge_cmpny);
DELETE FROM tb_shift_sch_mgmt WHERE CMPNY_CD IN (SELECT CMPNY_CD FROM tmp_purge_cmpny);
DELETE FROM tb_syst_auth_menu WHERE CMPNY_CD IN (SELECT CMPNY_CD FROM tmp_purge_cmpny);
DELETE FROM tb_tbm_attendance_event WHERE CMPNY_CD IN (SELECT CMPNY_CD FROM tmp_purge_cmpny);
DELETE FROM tb_tbm_attendance WHERE CMPNY_CD IN (SELECT CMPNY_CD FROM tmp_purge_cmpny);
DELETE FROM tb_tbm_edu_mtrl_item WHERE CMPNY_CD IN (SELECT CMPNY_CD FROM tmp_purge_cmpny);
DELETE FROM tb_tbm_edu_mtrl WHERE CMPNY_CD IN (SELECT CMPNY_CD FROM tmp_purge_cmpny);
DELETE FROM tb_tbm_pwd_fail WHERE CMPNY_CD IN (SELECT CMPNY_CD FROM tmp_purge_cmpny);
DELETE FROM tb_tbm_session_content WHERE CMPNY_CD IN (SELECT CMPNY_CD FROM tmp_purge_cmpny);
DELETE FROM tb_tbm_session_risk WHERE CMPNY_CD IN (SELECT CMPNY_CD FROM tmp_purge_cmpny);
DELETE FROM tb_tbm_session_state WHERE CMPNY_CD IN (SELECT CMPNY_CD FROM tmp_purge_cmpny);
DELETE FROM tb_tbm_session WHERE CMPNY_CD IN (SELECT CMPNY_CD FROM tmp_purge_cmpny);
DELETE FROM tb_terms_user_agr_hist WHERE CMPNY_CD IN (SELECT CMPNY_CD FROM tmp_purge_cmpny);
DELETE FROM tb_terms_user_agr_mgmt WHERE CMPNY_CD IN (SELECT CMPNY_CD FROM tmp_purge_cmpny);
DELETE FROM tb_user_attd_hist WHERE CMPNY_CD IN (SELECT CMPNY_CD FROM tmp_purge_cmpny);
DELETE FROM tb_user_attd_gps WHERE CMPNY_CD IN (SELECT CMPNY_CD FROM tmp_purge_cmpny);
DELETE FROM tb_user_attd_mgmt WHERE CMPNY_CD IN (SELECT CMPNY_CD FROM tmp_purge_cmpny);
DELETE FROM tb_user_attd_req_approval WHERE CMPNY_CD IN (SELECT CMPNY_CD FROM tmp_purge_cmpny);
DELETE FROM tb_user_attd_req WHERE CMPNY_CD IN (SELECT CMPNY_CD FROM tmp_purge_cmpny);
DELETE FROM tb_user_device_login_hist WHERE CMPNY_CD IN (SELECT CMPNY_CD FROM tmp_purge_cmpny);
DELETE FROM tb_user_device_occupancy_anomaly WHERE CMPNY_CD IN (SELECT CMPNY_CD FROM tmp_purge_cmpny);
DELETE FROM tb_user_device WHERE CMPNY_CD IN (SELECT CMPNY_CD FROM tmp_purge_cmpny);
DELETE FROM tb_user_hire_date_history WHERE CMPNY_CD IN (SELECT CMPNY_CD FROM tmp_purge_cmpny);
DELETE FROM tb_user_leave_use WHERE CMPNY_CD IN (SELECT CMPNY_CD FROM tmp_purge_cmpny);
DELETE FROM tb_user_leave_grant WHERE CMPNY_CD IN (SELECT CMPNY_CD FROM tmp_purge_cmpny);
DELETE FROM tb_user_menu_favorite WHERE CMPNY_CD IN (SELECT CMPNY_CD FROM tmp_purge_cmpny);
DELETE FROM tb_user_overtime_mgmt WHERE CMPNY_CD IN (SELECT CMPNY_CD FROM tmp_purge_cmpny);
DELETE FROM tb_user_push_setting WHERE CMPNY_CD IN (SELECT CMPNY_CD FROM tmp_purge_cmpny);
DELETE FROM tb_user_service_credit WHERE CMPNY_CD IN (SELECT CMPNY_CD FROM tmp_purge_cmpny);
DELETE FROM tb_user_site_auth WHERE CMPNY_CD IN (SELECT CMPNY_CD FROM tmp_purge_cmpny);
DELETE FROM tb_user_std_work_hours WHERE CMPNY_CD IN (SELECT CMPNY_CD FROM tmp_purge_cmpny);
DELETE FROM tb_user_transfer_reservation WHERE CMPNY_CD IN (SELECT CMPNY_CD FROM tmp_purge_cmpny);
DELETE FROM tb_user_upload_job WHERE CMPNY_CD IN (SELECT CMPNY_CD FROM tmp_purge_cmpny);
DELETE FROM tb_user_work_plan WHERE CMPNY_CD IN (SELECT CMPNY_CD FROM tmp_purge_cmpny);
DELETE FROM tb_site_node WHERE CMPNY_CD IN (SELECT CMPNY_CD FROM tmp_purge_cmpny);
DELETE FROM tb_site WHERE CMPNY_CD IN (SELECT CMPNY_CD FROM tmp_purge_cmpny);
DELETE FROM tb_user WHERE CMPNY_CD IN (SELECT CMPNY_CD FROM tmp_purge_cmpny);
DELETE FROM tb_cmpny WHERE CMPNY_CD IN (SELECT CMPNY_CD FROM tmp_purge_cmpny);


COMMIT;


-- ============================================================================
-- [5] 사후 검증 — 아래 결과를 기대값과 대조한 뒤 임시 테이블을 정리한다
-- ============================================================================

-- 5-1. 대상 회사 잔여 데이터(전부 0 이어야 함)
SELECT
   (SELECT COUNT(*) FROM tb_cmpny           WHERE CMPNY_CD IN (SELECT CMPNY_CD FROM tmp_purge_cmpny)) AS company
 , (SELECT COUNT(*) FROM tb_user            WHERE CMPNY_CD IN (SELECT CMPNY_CD FROM tmp_purge_cmpny)) AS users
 , (SELECT COUNT(*) FROM tb_daily_user      WHERE CMPNY_CD IN (SELECT CMPNY_CD FROM tmp_purge_cmpny)) AS daily_users
 , (SELECT COUNT(*) FROM tb_site            WHERE CMPNY_CD IN (SELECT CMPNY_CD FROM tmp_purge_cmpny)) AS sites
 , (SELECT COUNT(*) FROM tb_user_work_plan  WHERE CMPNY_CD IN (SELECT CMPNY_CD FROM tmp_purge_cmpny)) AS work_plan
 , (SELECT COUNT(*) FROM tb_user_attd_mgmt  WHERE CMPNY_CD IN (SELECT CMPNY_CD FROM tmp_purge_cmpny)) AS attd
 , (SELECT COUNT(*) FROM tb_tbm_session     WHERE CMPNY_CD IN (SELECT CMPNY_CD FROM tmp_purge_cmpny)) AS tbm
 , (SELECT COUNT(*) FROM tb_syst_auth_menu  WHERE CMPNY_CD IN (SELECT CMPNY_CD FROM tmp_purge_cmpny)) AS menu
 , (SELECT COUNT(*) FROM tb_file_info       WHERE CMPNY_CD IN (SELECT CMPNY_CD FROM tmp_purge_cmpny)) AS files;

-- 5-2. 삭제된 세션을 가리키는 고아 출결(0 이어야 함)
SELECT COUNT(*) AS orphan_tbm_attendance
  FROM tb_tbm_attendance A
 WHERE NOT EXISTS (SELECT 1 FROM tb_tbm_session S WHERE S.SESSION_CD = A.SESSION_CD);

-- 5-3. 회사 간 연계에 사라진 회사가 남아 있는지(0 이어야 함)
SELECT
   (SELECT COUNT(*) FROM tb_cmpny_relation R
     WHERE R.REQ_CMPNY_CD NOT IN (SELECT CMPNY_CD FROM tb_cmpny)
        OR R.TGT_CMPNY_CD NOT IN (SELECT CMPNY_CD FROM tb_cmpny)) AS relation
 , (SELECT COUNT(*) FROM tb_site_link L
     WHERE L.SRC_CMPNY_CD NOT IN (SELECT CMPNY_CD FROM tb_cmpny)
        OR L.DST_CMPNY_CD NOT IN (SELECT CMPNY_CD FROM tb_cmpny)) AS site_link
 , (SELECT COUNT(*) FROM tb_cmpny_share_req Q
     WHERE Q.REQ_CMPNY_CD NOT IN (SELECT CMPNY_CD FROM tb_cmpny)
        OR Q.PRV_CMPNY_CD NOT IN (SELECT CMPNY_CD FROM tb_cmpny)) AS share_req
 , (SELECT COUNT(*) FROM tb_tbm_session_share H
     WHERE H.HOST_CMPNY_CD NOT IN (SELECT CMPNY_CD FROM tb_cmpny)
        OR H.SHARE_CMPNY_CD NOT IN (SELECT CMPNY_CD FROM tb_cmpny)) AS tbm_share;

-- 5-4. 보존 회사(001) 무결 확인 — 전부 0 보다 커야 정상
SELECT
   (SELECT COUNT(*) FROM tb_user            WHERE CMPNY_CD='001') AS users001
 , (SELECT COUNT(*) FROM tb_site            WHERE CMPNY_CD='001') AS sites001
 , (SELECT COUNT(*) FROM tb_syst_auth_menu  WHERE CMPNY_CD='001') AS menu001
 , (SELECT COUNT(*) FROM tb_baim_val_d      WHERE CMPNY_CD='001') AS baimD001
 , (SELECT COUNT(*) FROM tb_leave_type_mgmt WHERE CMPNY_CD='001') AS leaveType001
 , (SELECT COUNT(*) FROM tb_sch_mgmt        WHERE CMPNY_CD='001') AS sch001
 , (SELECT COUNT(*) FROM tb_user WHERE CMPNY_CD='prafta_system_admin') AS platformAdmin;

-- 5-5. 남은 회사 목록(대상 회사가 빠졌는지 눈으로 확인)
SELECT CMPNY_CD, USE_YN FROM tb_cmpny ORDER BY CMPNY_CD;


-- 검증이 끝나면 임시 테이블 정리
DROP TABLE IF EXISTS tmp_purge_session;
DROP TABLE IF EXISTS tmp_purge_snapshot;
DROP TABLE IF EXISTS tmp_purge_cmpny;

SET SQL_SAFE_UPDATES = 1;
