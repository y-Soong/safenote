-- ============================================================================
-- prafta-com-008-qa-seed-rollback.sql — QA seed 원복 (개발 DB)
-- prafta-com-008-qa-seed.sql 적용분 + QA 중 생성된 런타임 데이터 정리.
-- ============================================================================

SET SQL_SAFE_UPDATES = 0;

-- [1] 비밀번호 원복 (seed 적용 직전 2026-06-12 시점 해시 + PWD_CHG_DTIME 원복: ADMIN 외 3계정은 원래 NULL)
UPDATE tb_user SET USER_PW = '$2a$12$liZFlmJKetmozuiOXsmB3.f5Di14SoM8HwBMBSoX6wRDbR6a6oO.O', PWD_CHG_DTIME = NULL WHERE USER_ID = 'TEST01';
UPDATE tb_user SET USER_PW = '$2a$12$oNDvaZ3gPPXbjqRT6Gm4luKU6yPtLuMOjspvwb1Fu8CZBs.z8lCG.', PWD_CHG_DTIME = NULL WHERE USER_ID = 'WLSGML108';
UPDATE tb_user SET USER_PW = '$2a$12$ym1.0ElA0fkjLubMQiDcs.J38fM0kv/SM7nb5Lydr5DLInNVXRuyW', PWD_CHG_DTIME = NULL WHERE USER_ID = 'SOON';
UPDATE tb_user SET USER_PW = '$2a$12$YuLJFYJAlBSVLvhzsGTvHO05Ut.me7pRPbf.bEHfr6RqwqI./JPmy', PWD_CHG_DTIME = '2026-05-29 09:36:38' WHERE USER_ID = 'ADMIN';

-- [2] QA 중 생성된 런타임 데이터 정리 (촉진 등록 연차 → 진행 마스터 → outbox → 차단 이력 → 동의요청)
DELETE FROM tb_user_leave_use WHERE GRANT_ID LIKE 'G20260612QA%' OR LEAVE_ID = 'LV20260612QA001';
DELETE FROM tb_leave_promotion_log WHERE USER_CD IN ('20260400014', '20260400011', '20260400013');
DELETE FROM tb_noti_outbox WHERE INSERT_DATE >= '2026-06-12' AND NOTI_TYPE IN (
    SELECT SYST_VAL_D_CD FROM tb_syst_val_d WHERE SYST_VAL_CD = 'SYS045'
      AND SYST_VAL_D_CD LIKE 'LEAVE_%');
DELETE FROM tb_leave_refusal_log WHERE USER_CD IN ('20260400014', '20260400011', '20260400013');
DELETE FROM tb_leave_change_request WHERE USER_CD IN ('20260400014', '20260400011', '20260400013');

-- [2-B] QA 런타임 추가 생성분 (세션2 실측 후 추가)
DELETE FROM tb_user_device WHERE DEVICE_UUID LIKE 'QA-DEVICE-%';
-- ADMIN이 D-잠금 해제 검증으로 변경한 W108 20260619 셀(MANUAL 저장분)
DELETE FROM tb_user_work_plan WHERE USER_CD = '20260400011' AND WORK_YMD = '20260619' AND GEN_SOURCE = 'MANUAL';
-- attd_close 마감/해제 이력(S11 검증분, 202606)
DELETE FROM tb_attd_close WHERE CLOSE_YM = '202606' AND CLOSE_DESC LIKE 'QA S11%';

-- [3] seed 본체 제거
DELETE FROM tb_user_work_plan WHERE INSERT_NO = 'QA_SEED';
DELETE FROM tb_user_leave_grant WHERE GRANT_ID LIKE 'G20260612QA%';

-- [4] QA 중 TEST01 기본근무 설정 원복(게이트 재검증 가능 상태로)
UPDATE tb_user SET DEFAULT_SCH_CD = NULL, DEFAULT_SCH_SET_DATE = NULL WHERE USER_ID = 'TEST01';
DELETE FROM tb_user_work_plan WHERE USER_CD = '20260400014' AND GEN_SOURCE = 'DEFAULT_SCH';

-- [5] QA 중 생성된 근태(출퇴근 시도 성공분) 정리 — 필요 시 일자 확인 후 수동 실행
-- DELETE FROM tb_user_attd_mgmt WHERE USER_CD IN ('20260400014','20260400011','20260400013') AND WORK_YMD >= '20260612';

SET SQL_SAFE_UPDATES = 1;
