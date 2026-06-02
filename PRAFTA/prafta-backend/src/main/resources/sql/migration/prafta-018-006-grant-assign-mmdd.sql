-- ============================================================================
-- PRAFTA-018-006 보강 DDL — tb_leave_type_mgmt.GRANT_ASSIGN_MMDD 컬럼 추가
-- 적용 작업: PLNprafta-018006 (PLNprafta-018004 점검 보고서 §5 후속 보정)
-- 작성일: 2026-05-21
-- 사유: prafta-017 "부여일지정" 기능에서 백엔드 코드(grantAssignMmdd) +
--       SYS027='03'(부여일지정) 코드값 + Attd03Mapper.xml 참조(28/63/116/157)는
--       반영됐으나 실 DB 컬럼만 누락되어 연차타입 저장/조회가 런타임에서
--       'Unknown column GRANT_ASSIGN_MMDD' 오류로 실패하던 상태를 정상화.
-- 컬럼 사양: char(4) NULL, MMDD 4자리(예 '0315'=3월15일).
--           grantBaseType='03'(부여일지정) 자동부여 시 필수
--           (Attd03ServiceImpl cross-field 검증 + MmddValidator로 강제).
-- 적용 환경: MySQL 8.0.42
-- 멱등성: ALTER ADD COLUMN은 중복 실행 시 에러(8.0은 ADD COLUMN IF NOT EXISTS 미지원).
--         이미 컬럼이 존재하면 본 DDL을 건너뛸 것. 운영 적용 후 본 파일 보관용.
-- ============================================================================

ALTER TABLE `tb_leave_type_mgmt`
  ADD COLUMN `GRANT_ASSIGN_MMDD` char(4) DEFAULT NULL
    COMMENT '자동부여 지정일 MMDD (기준일=03 부여일지정 시 필수)'
    AFTER `GRANT_OFFSET_MONTH`;
