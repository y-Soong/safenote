-- ============================================================================
-- PRAFTA-daily-user-dept-1 — 일일계정 슬롯에 소속부서(NODE_CD) 컬럼 추가
-- 작성일: 2026-06-24
-- 적용 환경: MySQL 8.0.42
-- 참조: .claude/requests/web_requests/prafta-daily-user-dept-and-mgmt.md (확정 결정 D3)
--
-- 변경 요약
--   tb_daily_user_slot 에 슬롯에 지정된 소속부서(NODE_CD)를 보관할 컬럼을 추가한다.
--   Baim_05 에서 슬롯에 부서를 지정해두면, 그 슬롯으로 가입/점유하는 일일사용자의
--   TB_USER.NODE_CD 로 복사된다(부서 매칭). NULL = 부서 미지정(매칭 생략).
--   위치는 CURR_USER_CD 다음(점유 관련 컬럼 인접).
--
-- 멱등성: 컬럼이 이미 있으면 ADD COLUMN 에러. 이미 반영된 환경에서는 건너뛸 것.
--   확인: SHOW COLUMNS FROM tb_daily_user_slot LIKE 'NODE_CD';
--
-- 운영 적용: 사용자 수동(read-only MCP). 본 파일은 작성만, DB 직접 적용 금지.
-- ★ 본 마이그(001)는 코드 배포 전 선적용 필수(매퍼/EP 가 NODE_CD 컬럼을 참조).
-- ============================================================================

ALTER TABLE tb_daily_user_slot
    ADD COLUMN NODE_CD varchar(50) NULL COMMENT '슬롯 지정 소속부서(가입/점유 시 TB_USER.NODE_CD 로 복사)'
    AFTER CURR_USER_CD;

-- ============================================================================
-- 롤백 (필요 시 수동 실행)
-- ----------------------------------------------------------------------------
-- ALTER TABLE tb_daily_user_slot DROP COLUMN NODE_CD;
-- ============================================================================
