-- ============================================================================
-- 작업지시서_소속이동-이력가시성-보정(근로자본인) — SITE_CD 필터 제거에 따른 인덱스 보강 후보
-- 작성일: 2026-08-23
-- 적용 환경: MySQL 8.0.42
-- 출처: .claude/requests/app_requests/작업지시서_소속이동-이력가시성-보정(근로자본인).plan.md
--       §1-5(T1), §2-4(T2), §2b-5(T2-b)
--
-- 배경:
--   001/002/005 작업으로 앱 "내 승인 요청"/"내 스케줄"/"내 근태기록"/"연차 사용내역"/
--   "초과근무 실적" 조회에서 SITE_CD 하드 필터를 제거했다(CMPNY_CD+USER_CD 만으로 본인 전체
--   이력 조회). 아래 3개 테이블은 기존 복합 인덱스가 SITE_CD 를 USER_CD 보다 선행 컬럼으로 두고
--   있어 필터 제거 후 인덱스 seek 열화(사실상 CMPNY_CD 접두사까지만 타는 풀스캔에 가까운 조회)가
--   우려된다. developer 는 EXPLAIN 실측 후 적용 여부를 확정한다(무조건 적용 아님).
--   TB_USER_WORK_PLAN(IX_WORK_PLAN_USER_YMD)/TB_USER_LEAVE_USE(IDX_LEAVE_USE_USER) 는
--   이미 USER_CD 선두 인덱스가 있어 보강 불요(plan §0 스키마 확인 결과).
--
-- 실측 가이드(적용 전 EXPLAIN 필수):
--   EXPLAIN SELECT ... FROM TB_USER_ATTD_MGMT  WHERE CMPNY_CD=? AND USER_CD=? AND WORK_YMD BETWEEN ? AND ? AND DEL_YN='N';
--   EXPLAIN SELECT ... FROM TB_USER_ATTD_REQ   WHERE CMPNY_CD=? AND USER_CD=? AND DEL_YN='N' AND REQ_TYPE IN (...);
--   EXPLAIN SELECT ... FROM TB_USER_OVERTIME_MGMT WHERE CMPNY_CD=? AND USER_CD=? AND WORK_YMD BETWEEN ? AND ?;
--
-- 멱등성: 이미 존재하는 인덱스명이면 ADD INDEX 가 에러 → 반영된 환경에서는 건너뛸 것.
-- 운영 적용: 사용자 수동(Workbench, read-only MCP 아님). 본 파일은 작성만, DB 직접 적용 금지.
-- 개발+운영 동시 적용 대상(feedback_db_migration_apply_both_envs 원칙).
-- ============================================================================

-- T1 §1-5: 내 승인 요청(TB_USER_ATTD_REQ) — 기존 IDX_ATTD_REQ_USER(CMPNY_CD,SITE_CD,USER_CD,REQ_STATUS) 는
--   SITE_CD 선행이라 필터 제거 후 미활용. USER_CD 선두 보강.
ALTER TABLE TB_USER_ATTD_REQ
  ADD INDEX IDX_ATTD_REQ_USER_NOSITE (CMPNY_CD, USER_CD, DEL_YN, REQ_TYPE);

-- T2 §2-4: 내 근태기록(TB_USER_ATTD_MGMT) — 기존 IDX_ATTD_USER_DATE(CMPNY_CD,SITE_CD,USER_CD,WORK_YMD,DEL_YN)
--   도 SITE_CD 선행. USER_CD 선두 보강.
ALTER TABLE TB_USER_ATTD_MGMT
  ADD INDEX IDX_ATTD_USER_DATE_NOSITE (CMPNY_CD, USER_CD, WORK_YMD, DEL_YN);

-- T2-b §2b-5: 초과근무 실적(TB_USER_OVERTIME_MGMT) — 기존 IDX_OT_USER_YMD(CMPNY_CD,SITE_CD,USER_CD,WORK_YMD)
--   도 SITE_CD 선행. USER_CD 선두 보강.
ALTER TABLE TB_USER_OVERTIME_MGMT
  ADD INDEX IDX_OT_USER_YMD_NOSITE (CMPNY_CD, USER_CD, WORK_YMD, OT_STATUS);

-- ============================================================================
-- 롤백 (필요 시 수동 실행)
-- ----------------------------------------------------------------------------
-- ALTER TABLE TB_USER_ATTD_REQ      DROP INDEX IDX_ATTD_REQ_USER_NOSITE;
-- ALTER TABLE TB_USER_ATTD_MGMT     DROP INDEX IDX_ATTD_USER_DATE_NOSITE;
-- ALTER TABLE TB_USER_OVERTIME_MGMT DROP INDEX IDX_OT_USER_YMD_NOSITE;
-- ============================================================================
