-- =============================================================================
-- prafta-009 마이그레이션: TB_USER_ATTD_REQ.REQ_TYPE 컬럼 길이 확장
-- =============================================================================
-- 작성일 : 2026-05-17
-- 작업 ID : PRAFTA-009 마무리 (QA 재검증 D2 미해결 후속)
-- 배경    : REQ_TYPE 컬럼이 varchar(10) 이라 11자 'ATTD_CREATE' / 'OT_REGISTER'
--           /'LEAVE_REQUEST' 등 신규 요청 유형 문자열을 저장할 수 없다.
--           selectDailyAttdDetailHistory 의 orphan 분기(R.REQ_TYPE='ATTD_CREATE')
--           가 영구 미매치되어 D2(생성요청 반려 이력 표시)가 동작하지 않았다.
-- 조치    : REQ_TYPE 을 varchar(20) 으로 확장.
--           ('LEAVE_REQUEST' 13자 + 여유 포함, 가장 긴 후보 수용)
-- 비고    : REQ_STATUS 는 varchar(10), 최장값 'CANCELLED'(9자) 수용 가능 → 변경 없음.
-- =============================================================================

ALTER TABLE TB_USER_ATTD_REQ
    MODIFY COLUMN REQ_TYPE VARCHAR(20) NOT NULL
    COMMENT '요청 유형 (ATTD_MODIFY/ATTD_CREATE/OT_REGISTER/LEAVE_REQUEST)';

-- -----------------------------------------------------------------------------
-- 원복 SQL (롤백 시)
-- -----------------------------------------------------------------------------
-- ALTER TABLE TB_USER_ATTD_REQ
--     MODIFY COLUMN REQ_TYPE VARCHAR(10) NOT NULL
--     COMMENT '요청 유형 (SYS032: 01~06)';
-- ※ 원복 전 REQ_TYPE 에 10자 초과 값이 없는지 확인할 것:
--   SELECT REQ_ID, REQ_TYPE FROM TB_USER_ATTD_REQ WHERE CHAR_LENGTH(REQ_TYPE) > 10;
-- =============================================================================
