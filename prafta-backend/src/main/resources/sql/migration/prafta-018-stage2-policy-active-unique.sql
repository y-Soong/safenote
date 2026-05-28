-- ============================================================================
-- PRAFTA-018 단계 2 보강 DDL — TB_LEAVE_POLICY 활성 1건 UNIQUE 보장
-- 적용 작업: PLNprafta-018003 (보안 검토 prafta-018003-001 후속 보정)
-- 작성일: 2026-05-20
-- 사유: 정책서 §8.5.2 "회사당 활성 정책 1건" 절대 규칙을 DB 레벨로 강제.
--       기존 IX_TB_LEAVE_POLICY_ACTIVE는 단순 KEY라 동시 INSERT 차단 불가.
--       MySQL 8.0+ expression UNIQUE 사용:
--         USE_YN='Y' 행만 CMPNY_CD 기준 UNIQUE (NULL 행은 중복 허용).
-- 적용 환경: MySQL 8.0.42 (expression index 지원: 8.0.13+)
-- 멱등성: ALTER ADD UNIQUE는 중복 실행 시 에러. 운영 적용 후 본 파일 보관용.
-- ============================================================================

ALTER TABLE `TB_LEAVE_POLICY`
  ADD UNIQUE KEY `UX_TB_LEAVE_POLICY_ACTIVE`
    ((CASE WHEN `USE_YN` = 'Y' THEN `CMPNY_CD` END));
