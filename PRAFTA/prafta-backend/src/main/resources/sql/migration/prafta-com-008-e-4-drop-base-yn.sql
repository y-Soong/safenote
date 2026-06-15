-- ============================================================================
-- PRAFTA-COM-008-E-4 — tb_sch_mgmt.BASE_YN DROP (마이그 단계 ③ — 최후 적용)
-- 작성일: 2026-06-11
-- 적용 환경: MySQL 8.0.42
-- 참조: prafta-com-008-E-decomposition.md §1(E-4)/§4, prafta-com-008-E-default-worktype.md §5/§7(3)
--
-- ★ 적용 순서(엄수): E-1(컬럼추가) → [데이터 일괄삭제 DML] → E-2~E-9 코드 배포(BE 재컴파일+FE 재빌드)
--   → 그 다음 본 파일 적용. 코드가 BASE_YN 을 참조하는 상태에서 DROP 하면 해당 SQL 전면 실패.
--   E-9a 에서 BASE_YN 참조(SchInfoResult / Attd01Mapper.xml / AppReq07Mapper.xml / SchedOptionResult)를
--   모두 제거했음을 확인한 뒤에만 적용한다.
--
-- 변경 요약
--  1) tb_sch_mgmt.BASE_YN 컬럼 DROP (사업장 단위 기본근무 개념 폐기 → 사용자별 DEFAULT_SCH_CD 로 대체).
--  2) SYS003 코드 그룹은 삭제하지 않는다(D-E5) — BASE_YN 전용이 아니라 범용 Y/N(사용여부 등) 공유.
--
-- 멱등성: 이미 DROP 된 환경에서는 에러. 건너뛸 것.
-- ============================================================================

ALTER TABLE `tb_sch_mgmt` DROP COLUMN `BASE_YN`;
