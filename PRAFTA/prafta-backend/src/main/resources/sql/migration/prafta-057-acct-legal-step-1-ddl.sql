-- ============================================================================
-- prafta-057 : 사고관리 법정 처리/기한 탭 개편 (1) DDL
-- 대상: TB_ACCT_LEGAL_STEP_MASTER
-- 목적: 처리 단계(1·2·3·5)와 참고 항목(4·6: 요양급여 신청 / 보상·합의)을
--       데이터로 구분하기 위한 STEP_TYPE 컬럼 추가.
--   - PROCESS  : 시스템이 상태(접수/완료)를 관리하는 처리 단계 (번호·체크박스 노출)
--   - REFERENCE: 회사가 시스템 안에서 처리할 게 없는 참고 항목 (무번호·체크박스 없음·완료집계 제외)
-- 적용 순서: 본 DDL → prafta-057-acct-legal-step-2-data.sql (DML)
-- ============================================================================

ALTER TABLE TB_ACCT_LEGAL_STEP_MASTER
    ADD COLUMN STEP_TYPE VARCHAR(20) NOT NULL DEFAULT 'PROCESS'
        COMMENT '단계유형 PROCESS:처리단계(상태관리) REFERENCE:참고항목(상태없음·완료집계제외)'
        AFTER DEADLINE_RULE_CD;
