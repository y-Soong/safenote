-- ============================================================================
-- PRAFTA-058-3 — tb_near_miss.INCIDENT_TYPE_CD 컬럼 DROP (DDL)
-- 적용 환경: MySQL 8.0.42 / 출처: T6-findings D3, T6-P3 §마이그
-- ★★ 선행 필수: 058-1(경미사고 행 삭제) 완료 + 아래 코드 배포 완료 후에만 적용 ★★
--    이 컬럼을 SELECT/필터하는 코드(배포 동반 수정 대상):
--      · nearmiss01(앱 AppNearMiss01Mapper.xml / 웹 NearMiss01Mapper.xml)  → T6-15-1/2 (본 PR)
--      · acct01 Acct01Mapper.selectNearMissList                            → D-ACCT (본 PR 동반)
--      · risklink01 RiskLink01Mapper available-near-miss                    → D-P1 (별도 소유, 동시 배포)
--    위 3개 모두 배포 전 DROP 시 런타임 'Unknown column INCIDENT_TYPE_CD' 오류.
-- 멱등성: 컬럼 부재 시 에러(IF EXISTS 미지원 8.0). 적용 전 존재 확인:
--    SELECT COUNT(*) FROM information_schema.columns
--     WHERE table_name='tb_near_miss' AND column_name='INCIDENT_TYPE_CD'; -- 1이면 적용
-- ============================================================================
ALTER TABLE `tb_near_miss`
  DROP COLUMN `INCIDENT_TYPE_CD`;
-- 검증:
--   SELECT COUNT(*) FROM information_schema.columns
--    WHERE table_name='tb_near_miss' AND column_name='INCIDENT_TYPE_CD'; -- 0
