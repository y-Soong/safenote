-- ============================================================================
-- prafta-028: 연차 유효기간(AXIS6_VALIDITY_MONTHS) 12개월 법정 고정
--
-- 배경: AXIS6은 12(법정)/24(연장) 선택 옵션이었으나, 24개월 연장옵션을 폐지하고
--   12개월(법정) 단일 고정으로 전환한다(Baim_07 화면 + LeavePolicyServiceImpl 검증 동반 수정).
--   기존에 활성/비활성 정책이 24로 저장돼 있으면 엔진(resolveValidityMonths)이 24를 읽어
--   부여 시 유효종료일을 +24개월로 찍는다 → 본 스크립트로 24 → 12 정규화한다.
--
-- 주의(절대 규칙, 정책서 §8.5.8 #2): 이미 부여된 연차(tb_user_leave_grant)의 AVAIL_TO_DATE는
--   사후 차감/축소하지 않는다. 본 스크립트는 정책(tb_leave_policy)만 정규화하며, 기존 24개월
--   유효기간으로 부여된 GRANT 행은 그대로 둔다(신규 부여분부터 12개월 적용).
--
-- 실행 환경: 로컬/개발 DB. 운영 적용 시 백업 후 실행.
-- ============================================================================

-- (사전 점검) 24개월(또는 12 외) 정책 건수 확인:
--   SELECT POLICY_SEQ, CMPNY_CD, USE_YN, AXIS6_VALIDITY_MONTHS
--     FROM tb_leave_policy
--    WHERE AXIS6_VALIDITY_MONTHS <> 12;

-- (정규화) 12개월 외 값을 12로 일괄 전환 (활성/비활성 모두 — 24옵션 폐지)
UPDATE tb_leave_policy
   SET AXIS6_VALIDITY_MONTHS = 12
 WHERE AXIS6_VALIDITY_MONTHS <> 12;

-- (COMMENT 정정) 컬럼 정의(타입/NULL/기본값)는 유지하고 COMMENT만 '12 고정'으로 재지정.
ALTER TABLE `tb_leave_policy`
  MODIFY COLUMN `AXIS6_VALIDITY_MONTHS` int NOT NULL DEFAULT 12
    COMMENT '6번: 유효기간(개월) 12개월 법정 고정 (prafta-028, 24 연장옵션 폐지)';

-- (확인) 모두 12인지:
--   SELECT AXIS6_VALIDITY_MONTHS, COUNT(*) FROM tb_leave_policy GROUP BY AXIS6_VALIDITY_MONTHS;
--   SHOW FULL COLUMNS FROM tb_leave_policy WHERE Field = 'AXIS6_VALIDITY_MONTHS';
