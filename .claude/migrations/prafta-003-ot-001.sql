-- =============================================================================
-- PRAFTA-003 / OT - 001
--
-- Adds TB_USER_OVERTIME_MGMT (new) and migrates the existing tb_user_attd_req
-- table to the generalized TB_USER_ATTD_REQ structure that can hold attendance,
-- overtime, and leave requests under one schema.
--
-- Author      : developer (Claude Code)
-- Created     : 2026-05-13
-- Target DB   : MySQL 8.0.42
-- Run order   : after PRAFTA-002 baseline; before deploying PRAFTA-003 backend.
-- Idempotency : not idempotent. Run exactly once. Wrap in transaction or take a
--               logical backup first.
-- =============================================================================

-- ---------------------------------------------------------------------------
-- 1. Create the new TB_USER_OVERTIME_MGMT table (policy lines 32-84).
-- ---------------------------------------------------------------------------
CREATE TABLE TB_USER_OVERTIME_MGMT (
    OT_ID                   VARCHAR(20)   NOT NULL                COMMENT '초과근무 ID (PK)',
    CMPNY_CD                VARCHAR(50)   NOT NULL                COMMENT '회사 코드',
    SITE_CD                 VARCHAR(50)   NOT NULL                COMMENT '사업장 코드',
    USER_CD                 VARCHAR(20)   NOT NULL                COMMENT '근무자 사용자 코드',

    -- 연관 정보
    ATTD_ID                 VARCHAR(20)   NULL                    COMMENT '연관 근태 ID (tb_user_attd_mgmt.ATTD_ID, 휴일근무 등 정규근태 없는 경우 NULL)',
    REQ_ID                  VARCHAR(20)   NULL                    COMMENT '연관 요청 ID (tb_user_attd_req.REQ_ID, 사후 등록 시 NULL)',

    -- 근무일/근무위치
    WORK_YMD                VARCHAR(8)    NOT NULL                COMMENT '근무 일자 (YYYYMMDD)',
    NODE_CD                 VARCHAR(50)   NULL                    COMMENT '근무 노드 코드',

    -- 초과근무 유형 (가산수당 계산용)
    OT_TYPE                 VARCHAR(10)   NOT NULL                COMMENT '초과근무 유형 (EXTEND:연장 / NIGHT:야간 / HOLIDAY:휴일)',

    -- 계획 시각 (신청/승인 시점)
    PLAN_START_DATE         VARCHAR(8)    NULL                    COMMENT '계획 시작 일자 (YYYYMMDD)',
    PLAN_START_TIME         VARCHAR(4)    NULL                    COMMENT '계획 시작 시각 (HHMM)',
    PLAN_END_DATE           VARCHAR(8)    NULL                    COMMENT '계획 종료 일자 (YYYYMMDD)',
    PLAN_END_TIME           VARCHAR(4)    NULL                    COMMENT '계획 종료 시각 (HHMM)',

    -- 실제 수행 시각 (가산수당 계산 기준)
    ACTUAL_START_DATE       VARCHAR(8)    NOT NULL                COMMENT '실제 시작 일자 (YYYYMMDD)',
    ACTUAL_START_TIME       VARCHAR(4)    NOT NULL                COMMENT '실제 시작 시각 (HHMM)',
    ACTUAL_START_METHOD     VARCHAR(2)    NULL                    COMMENT '시작 체크 방식 (GPS/QR/MANUAL 등)',
    ACTUAL_END_DATE         VARCHAR(8)    NULL                    COMMENT '실제 종료 일자 (YYYYMMDD)',
    ACTUAL_END_TIME         VARCHAR(4)    NULL                    COMMENT '실제 종료 시각 (HHMM)',
    ACTUAL_END_METHOD       VARCHAR(2)    NULL                    COMMENT '종료 체크 방식 (GPS/QR/MANUAL 등)',

    -- 근무시간 계산 결과 (가산수당 계산용 캐시)
    WORK_MINUTES            INT           NULL                    COMMENT '실제 근무 시간 (분 단위, 휴게시간 제외)',
    BREAK_MINUTES           INT           NULL  DEFAULT 0          COMMENT '휴게 시간 (분 단위)',

    -- OT 상태 (진행 중/완료/취소)
    OT_STATUS               VARCHAR(10)   NOT NULL                COMMENT '초과근무 상태 (IN_PROGRESS:진행중 / COMPLETED:완료 / CANCELLED:취소)',

    -- 공통 관리 컬럼
    DEL_YN                  VARCHAR(1)    NOT NULL  DEFAULT 'N'   COMMENT '삭제 여부',
    INSERT_NO               VARCHAR(50)   NOT NULL                COMMENT '등록자',
    INSERT_DATE             DATETIME      NOT NULL                COMMENT '등록 일시',
    UPDATE_NO               VARCHAR(50)   NULL                    COMMENT '수정자',
    UPDATE_DATE             DATETIME      NULL                    COMMENT '수정 일시',

    PRIMARY KEY (OT_ID),

    -- 조회 성능용 인덱스
    KEY IDX_OT_USER_YMD     (CMPNY_CD, SITE_CD, USER_CD, WORK_YMD),
    KEY IDX_OT_SITE_YMD     (CMPNY_CD, SITE_CD, WORK_YMD, OT_STATUS),
    KEY IDX_OT_ATTD         (ATTD_ID),
    KEY IDX_OT_REQ          (REQ_ID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='사용자 초과근무 실적 관리';


-- ---------------------------------------------------------------------------
-- 2. Migrate tb_user_attd_req in-place to the new TB_USER_ATTD_REQ structure.
--    Steps:
--      a. Rename existing columns (ATTD_ID -> TARGET_ID, CHECK_IN_* -> START_*,
--         CHECK_OUT_* -> END_*).
--      b. Add new columns (PROCESS_DATE, UPDATE_NO, UPDATE_DATE, OT_TYPE,
--         LEAVE_TYPE, LEAVE_DAYS).
--      c. Migrate REQ_STATUS values ('01' -> 'REQUESTED', '02' -> 'APPROVED').
--      d. Drop the old idx_req_attd index and create the new IDX_ATTD_REQ_*.
-- ---------------------------------------------------------------------------

-- 2a. Column renames.
ALTER TABLE tb_user_attd_req
    CHANGE ATTD_ID         TARGET_ID  VARCHAR(20)  NULL  COMMENT '수정 대상 ID (수정 요청 시: ATTD_ID / OT_ID / LEAVE_ID, 생성 요청 시 NULL)';

ALTER TABLE tb_user_attd_req
    CHANGE CHECK_IN_DATE   START_DATE VARCHAR(8)   NULL  COMMENT '시작 일자 (YYYYMMDD)';

ALTER TABLE tb_user_attd_req
    CHANGE CHECK_IN_TIME   START_TIME VARCHAR(4)   NULL  COMMENT '시작 시각 (HHMM)';

ALTER TABLE tb_user_attd_req
    CHANGE CHECK_OUT_DATE  END_DATE   VARCHAR(8)   NULL  COMMENT '종료 일자 (YYYYMMDD)';

ALTER TABLE tb_user_attd_req
    CHANGE CHECK_OUT_TIME  END_TIME   VARCHAR(4)   NULL  COMMENT '종료 시각 (HHMM)';

-- 2b. New columns.
ALTER TABLE tb_user_attd_req
    ADD COLUMN PROCESS_DATE  DATETIME      NULL                COMMENT '처리 일시'                           AFTER PROCESS_COMMENT,
    ADD COLUMN OT_TYPE       VARCHAR(10)   NULL                COMMENT '초과근무 유형 (EXTEND/NIGHT/HOLIDAY)' AFTER END_TIME,
    ADD COLUMN LEAVE_TYPE    VARCHAR(10)   NULL                COMMENT '연차 유형 (ANNUAL/HALF_AM/HALF_PM/SICK/FAMILY)'  AFTER OT_TYPE,
    ADD COLUMN LEAVE_DAYS    DECIMAL(3,1)  NULL                COMMENT '사용 일수'                            AFTER LEAVE_TYPE,
    ADD COLUMN UPDATE_NO     VARCHAR(50)   NULL                COMMENT '수정자',
    ADD COLUMN UPDATE_DATE   DATETIME      NULL                COMMENT '수정 일시';

-- 2c. Status value migration.
UPDATE tb_user_attd_req SET REQ_STATUS = 'REQUESTED' WHERE REQ_STATUS = '01';
UPDATE tb_user_attd_req SET REQ_STATUS = 'APPROVED'  WHERE REQ_STATUS = '02';

-- 2c-2. REQ_TYPE value migration (PRAFTA-003 QA-009 fix).
-- Existing production data only had numeric '01' (attendance modify request).
-- Convert to the enum form so the SYS032 master rows added below resolve.
-- NOTE: this UPDATE is NOT idempotent on its own; running twice will not match
-- anything the second time because the source value is gone. Take a logical
-- backup before applying. If additional numeric codes (e.g. '02'..'06') turn
-- out to exist in production, extend the mapping list before running this
-- block (e.g. '02' -> 'ATTD_CREATE', '03' -> 'OT_REGISTER',
-- '04' -> 'LEAVE_REQUEST', etc.) once the operator confirms the canonical
-- mapping. Until then this script migrates only the value actually emitted
-- by the existing prafta worker UI: '01' -> 'ATTD_MODIFY'.
UPDATE tb_user_attd_req SET REQ_TYPE = 'ATTD_MODIFY' WHERE REQ_TYPE = '01';

-- 2d. Index swap.
ALTER TABLE tb_user_attd_req DROP INDEX idx_req_attd;
ALTER TABLE tb_user_attd_req ADD KEY IDX_ATTD_REQ_TARGET (TARGET_ID);
-- The other indexes (idx_req_user / idx_req_admin) keep the same shape but are
-- renamed for consistency with the new naming convention. Drop+add is safer
-- than RENAME INDEX because it works across all 8.0 minor versions.
ALTER TABLE tb_user_attd_req DROP INDEX idx_req_user;
ALTER TABLE tb_user_attd_req ADD KEY IDX_ATTD_REQ_USER   (CMPNY_CD, SITE_CD, USER_CD, REQ_STATUS);
ALTER TABLE tb_user_attd_req DROP INDEX idx_req_admin;
ALTER TABLE tb_user_attd_req ADD KEY IDX_ATTD_REQ_STATUS (CMPNY_CD, SITE_CD, REQ_STATUS, REQ_TYPE);
ALTER TABLE tb_user_attd_req ADD KEY IDX_ATTD_REQ_WORK_YMD (CMPNY_CD, SITE_CD, WORK_YMD);


-- ---------------------------------------------------------------------------
-- 3. Code master rows for the new SYS033 (REQ_STATUS) values.
--    Existing prafta installations only had '01' (신청) and '02' (승인); add
--    REQUESTED / APPROVED / REJECTED / CANCELLED. Uses ON DUPLICATE KEY UPDATE
--    so the script can be re-run safely on already-seeded environments.
--
--    NOTE: the exact code-master table name varies between prafta deployments
--    (some installs use TB_BAIM_INFO, others TB_BAIM_LST). Adjust the table
--    name to match the operator's environment before running.
-- ---------------------------------------------------------------------------

-- The script below assumes TB_BAIM_INFO with columns
--   (CMPNY_CD, BAIM_TYP, BAIM_KEY, BAIM_GRP, BAIM_VAL, USE_YN, INSERT_NO, INSERT_DATE).
-- Adjust column list / table name to match your code-master layout.
INSERT INTO TB_BAIM_INFO (CMPNY_CD, BAIM_TYP, BAIM_KEY, BAIM_GRP, BAIM_VAL, USE_YN, INSERT_NO, INSERT_DATE)
SELECT DISTINCT CMPNY_CD, 'SYST_VAL', 'REQUESTED', 'SYS033', '신청',     'Y', 'SYSTEM', NOW() FROM TB_USER U
WHERE NOT EXISTS (
    SELECT 1 FROM TB_BAIM_INFO B
    WHERE B.CMPNY_CD = U.CMPNY_CD AND B.BAIM_TYP = 'SYST_VAL' AND B.BAIM_KEY = 'REQUESTED' AND B.BAIM_GRP = 'SYS033'
);
INSERT INTO TB_BAIM_INFO (CMPNY_CD, BAIM_TYP, BAIM_KEY, BAIM_GRP, BAIM_VAL, USE_YN, INSERT_NO, INSERT_DATE)
SELECT DISTINCT CMPNY_CD, 'SYST_VAL', 'APPROVED',  'SYS033', '승인',     'Y', 'SYSTEM', NOW() FROM TB_USER U
WHERE NOT EXISTS (
    SELECT 1 FROM TB_BAIM_INFO B
    WHERE B.CMPNY_CD = U.CMPNY_CD AND B.BAIM_TYP = 'SYST_VAL' AND B.BAIM_KEY = 'APPROVED' AND B.BAIM_GRP = 'SYS033'
);
INSERT INTO TB_BAIM_INFO (CMPNY_CD, BAIM_TYP, BAIM_KEY, BAIM_GRP, BAIM_VAL, USE_YN, INSERT_NO, INSERT_DATE)
SELECT DISTINCT CMPNY_CD, 'SYST_VAL', 'REJECTED',  'SYS033', '반려',     'Y', 'SYSTEM', NOW() FROM TB_USER U
WHERE NOT EXISTS (
    SELECT 1 FROM TB_BAIM_INFO B
    WHERE B.CMPNY_CD = U.CMPNY_CD AND B.BAIM_TYP = 'SYST_VAL' AND B.BAIM_KEY = 'REJECTED' AND B.BAIM_GRP = 'SYS033'
);
INSERT INTO TB_BAIM_INFO (CMPNY_CD, BAIM_TYP, BAIM_KEY, BAIM_GRP, BAIM_VAL, USE_YN, INSERT_NO, INSERT_DATE)
SELECT DISTINCT CMPNY_CD, 'SYST_VAL', 'CANCELLED', 'SYS033', '취소',     'Y', 'SYSTEM', NOW() FROM TB_USER U
WHERE NOT EXISTS (
    SELECT 1 FROM TB_BAIM_INFO B
    WHERE B.CMPNY_CD = U.CMPNY_CD AND B.BAIM_TYP = 'SYST_VAL' AND B.BAIM_KEY = 'CANCELLED' AND B.BAIM_GRP = 'SYS033'
);


-- ---------------------------------------------------------------------------
-- 3-2. Code master rows for SYS032 (REQ_TYPE) values (PRAFTA-003 QA-009 fix).
--      Aligns SYS032 with the enum-style codes that the backend allow-list
--      in Attd07ServiceImpl.isAttendanceReqType expects.
--
--      Pre-PRAFTA-003 the column held numeric '01' (worker attendance modify
--      request) only. Section 2c-2 above migrates the data; this section
--      registers the four canonical enum values so that
--      FNC_CMM_INFO_SRCH(..., 'SYS032') resolves correctly on the UI side.
--
--      The same table-name caveat as section 3 applies: adjust TB_BAIM_INFO
--      and the column list to match the operator's deployment.
-- ---------------------------------------------------------------------------
INSERT INTO TB_BAIM_INFO (CMPNY_CD, BAIM_TYP, BAIM_KEY, BAIM_GRP, BAIM_VAL, USE_YN, INSERT_NO, INSERT_DATE)
SELECT DISTINCT CMPNY_CD, 'SYST_VAL', 'ATTD_MODIFY',   'SYS032', 'Attendance modify request', 'Y', 'SYSTEM', NOW() FROM TB_USER U
WHERE NOT EXISTS (
    SELECT 1 FROM TB_BAIM_INFO B
    WHERE B.CMPNY_CD = U.CMPNY_CD AND B.BAIM_TYP = 'SYST_VAL' AND B.BAIM_KEY = 'ATTD_MODIFY' AND B.BAIM_GRP = 'SYS032'
);
INSERT INTO TB_BAIM_INFO (CMPNY_CD, BAIM_TYP, BAIM_KEY, BAIM_GRP, BAIM_VAL, USE_YN, INSERT_NO, INSERT_DATE)
SELECT DISTINCT CMPNY_CD, 'SYST_VAL', 'ATTD_CREATE',   'SYS032', 'Attendance create request', 'Y', 'SYSTEM', NOW() FROM TB_USER U
WHERE NOT EXISTS (
    SELECT 1 FROM TB_BAIM_INFO B
    WHERE B.CMPNY_CD = U.CMPNY_CD AND B.BAIM_TYP = 'SYST_VAL' AND B.BAIM_KEY = 'ATTD_CREATE' AND B.BAIM_GRP = 'SYS032'
);
INSERT INTO TB_BAIM_INFO (CMPNY_CD, BAIM_TYP, BAIM_KEY, BAIM_GRP, BAIM_VAL, USE_YN, INSERT_NO, INSERT_DATE)
SELECT DISTINCT CMPNY_CD, 'SYST_VAL', 'OT_REGISTER',   'SYS032', 'Overtime register request', 'Y', 'SYSTEM', NOW() FROM TB_USER U
WHERE NOT EXISTS (
    SELECT 1 FROM TB_BAIM_INFO B
    WHERE B.CMPNY_CD = U.CMPNY_CD AND B.BAIM_TYP = 'SYST_VAL' AND B.BAIM_KEY = 'OT_REGISTER' AND B.BAIM_GRP = 'SYS032'
);
INSERT INTO TB_BAIM_INFO (CMPNY_CD, BAIM_TYP, BAIM_KEY, BAIM_GRP, BAIM_VAL, USE_YN, INSERT_NO, INSERT_DATE)
SELECT DISTINCT CMPNY_CD, 'SYST_VAL', 'LEAVE_REQUEST', 'SYS032', 'Leave request',             'Y', 'SYSTEM', NOW() FROM TB_USER U
WHERE NOT EXISTS (
    SELECT 1 FROM TB_BAIM_INFO B
    WHERE B.CMPNY_CD = U.CMPNY_CD AND B.BAIM_TYP = 'SYST_VAL' AND B.BAIM_KEY = 'LEAVE_REQUEST' AND B.BAIM_GRP = 'SYS032'
);


-- ---------------------------------------------------------------------------
-- 4. Sequence master row for OT_ID. FNC_CMM_SEQ_NEXTVAL reads its current
--    counter from the company-scoped sequence master table. The exact table
--    name varies; adjust as needed.
--
--    Below assumes TB_CMM_SEQ_MGMT(CMPNY_CD, SEQ_KEY, SEQ_VAL, ...).
-- ---------------------------------------------------------------------------
INSERT INTO TB_CMM_SEQ_MGMT (CMPNY_CD, SEQ_KEY, SEQ_VAL, INSERT_NO, INSERT_DATE)
SELECT DISTINCT CMPNY_CD, 'OT_ID', 0, 'SYSTEM', NOW() FROM TB_USER U
WHERE NOT EXISTS (
    SELECT 1 FROM TB_CMM_SEQ_MGMT S
    WHERE S.CMPNY_CD = U.CMPNY_CD AND S.SEQ_KEY = 'OT_ID'
);


-- TODO operator: apply this migration manually, then update schema-full.sql.
