-- =============================================================================
-- prafta-010-2-001 마이그레이션: TB_USER_ATTD_REQ 컬럼 코멘트 코드값화
-- =============================================================================
-- 작성일 : 2026-05-17
-- 작업 ID : PRAFTA-010-2-001
-- 배경    : REQ_TYPE / REQ_STATUS 를 enum 문자열(ATTD_MODIFY 등)이 아니라
--           TB_SYST_VAL_D 의 SYST_VAL_CD 'SYS032'(요청 유형) / 'SYS033'(요청 상태)
--           코드값으로 관리하도록 전환한다.
-- 조치    : REQ_TYPE / REQ_STATUS 컬럼의 코멘트만 코드값 기준으로 변경한다.
--           타입은 유지한다 (REQ_TYPE varchar(20), REQ_STATUS varchar(10)).
-- 매핑    : REQ_TYPE   ATTD_CREATE->'01' / ATTD_MODIFY->'02' / OT_REGISTER->'03'
--                      (04 초과근무수정 / 05 연차사용 / 06 연차수정 은 SYS032 정의만,
--                       현재 코드 미매핑)
--           REQ_STATUS REQUESTED->'01' / APPROVED->'02' / REJECTED->'03' / CANCELLED->'04'
-- =============================================================================

ALTER TABLE TB_USER_ATTD_REQ
    MODIFY COLUMN REQ_TYPE VARCHAR(20) NOT NULL
    COMMENT '요청 유형 (SYS032: 01근태생성/02근태수정/03초과근무생성/04초과근무수정/05연차사용/06연차수정)';

ALTER TABLE TB_USER_ATTD_REQ
    MODIFY COLUMN REQ_STATUS VARCHAR(10) NOT NULL
    COMMENT '요청 상태 (SYS033: 01신청/02승인/03반려/04취소)';

-- -----------------------------------------------------------------------------
-- 원복 SQL (롤백 시)
-- -----------------------------------------------------------------------------
-- ALTER TABLE TB_USER_ATTD_REQ
--     MODIFY COLUMN REQ_TYPE VARCHAR(20) NOT NULL
--     COMMENT '요청 유형 (ATTD_MODIFY/ATTD_CREATE/OT_REGISTER/LEAVE_REQUEST)';
--
-- ALTER TABLE TB_USER_ATTD_REQ
--     MODIFY COLUMN REQ_STATUS VARCHAR(10) NOT NULL
--     COMMENT '요청 상태 (REQUESTED/APPROVED/REJECTED/CANCELLED)';
-- =============================================================================
