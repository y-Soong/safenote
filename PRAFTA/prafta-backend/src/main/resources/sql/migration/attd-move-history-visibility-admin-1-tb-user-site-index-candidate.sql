-- ============================================================================
-- 작업지시서_소속이동-이력가시성-보정(관리자화면) — 대상자 산정(target_user) 재설계에 따른
-- TB_USER 인덱스 보강 후보(선택 적용, 이번 배포 필수 아님)
-- 작성일: 2026-08-23
-- 적용 환경: MySQL 8.0.42
-- 출처: .claude/requests/web_requests/작업지시서_소속이동-이력가시성-보정(관리자화면).plan.md §4
--
-- 배경:
--   소속이동이력가시성-01/02(Attd07Mapper.selectMonthlyAttdList, Attd15Mapper 4곳)의 target_user
--   CTE 분기 B(이동자)가 TB_USER 를 회사 스코프(CMPNY_CD)로 한 번 더 스캔한다. TB_USER 는
--   SITE_CD 단독/복합 인덱스가 없어(PK=CMPNY_CD+USER_CD, 그 외 IX_TB_USER_STATUS
--   (CMPNY_CD,USE_YN,ACCOUNT_STATUS) 뿐) 기존 분기 A 도 이미 회사 범위 풀스캔 후 SITE_CD
--   필터링이었다 — 분기 B 추가로 "회사 전체 사용자 스캔 1회"가 늘어나는 수준(plan §4-2).
--
--   사용자 수가 많은 회사에서 체감 저하가 실측되면 아래 인덱스를 후속 적용 검토한다.
--   plan §4-4 는 이를 "본 작업 범위 밖 — DDL 변경이므로 별도 승인 필요"로 명시했다.
--   ★ 본 파일은 작성만 — DB 직접 적용 금지(운영은 사용자 Workbench 수동, 개발/운영 동시 적용 원칙).
--
-- 실측 가이드(적용 전 EXPLAIN 필수, 사용자 수 많은 CMPNY_CD 기준):
--   EXPLAIN SELECT U.USER_CD FROM TB_USER U WHERE U.CMPNY_CD=? AND U.SITE_CD<>?;
--
-- 멱등성: 이미 존재하는 인덱스명이면 ADD INDEX 가 에러 → 반영된 환경에서는 건너뛸 것.
-- ============================================================================

-- 분기 B(target_user 이동자 서브셀렉트)의 "U.CMPNY_CD=? AND U.SITE_CD<>?" 술어는 부등호라
-- 인덱스 seek 효과가 제한적이지만, CMPNY_CD 접두사만으로도 동일 회사 내 좁은 스캔 범위를 보장한다.
ALTER TABLE TB_USER
  ADD INDEX IDX_USER_CMPNY_SITE (CMPNY_CD, SITE_CD);

-- ============================================================================
-- 롤백 (필요 시 수동 실행)
-- ----------------------------------------------------------------------------
-- ALTER TABLE TB_USER DROP INDEX IDX_USER_CMPNY_SITE;
-- ============================================================================
