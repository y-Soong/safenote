-- ============================================================================
-- SOJEONG-1-2 — 통상근로자 주 소정근로시간 기준값 테이블 신설 (TB_CMPNY_STD_WORK_POLICY)
-- 작성일: 2026-08-12
-- 적용 환경: MySQL 8.0.42 이상 (★개발·운영 동시 적용 — feedback_db_migration_apply_both_envs)
-- 참조: 작업지시서_근로자별-소정근로시간-관리-도입.md §스코프 확정 B-1
--       작업지시서_근로자별-소정근로시간-관리-도입.plan.md §1.2
--
-- 변경 요약
--  1) TB_CMPNY_STD_WORK_POLICY 신설 — 회사 레벨 "통상근로자 주 소정근로분" 기준값 1벌.
--     역할 3가지로 한정(B-1): ①단시간 판정 비교 분모 ②연차 비례부여 공식 분모
--     ③소정 이력 미입력 계정의 폴백. ★계정에 자동 주입하는 기본값 용도가 아님.
--
-- 규약
--  - 기본값 2400분(주 40시간) = PRAFTA 표준 근무 형태(09~18 주5일)와 일치.
--  - ★행 부재 = 2400분 코드 폴백(StdWorkHoursService.DEFAULT_WEEK_STD_MINUTES) 이므로
--    기존 회사 백필 시드 불필요. CompanyProvision 시드도 불필요 — 기준값이 40h 와 다른
--    회사만 행을 생성한다.
--  - SCOPE_TYPE/SCOPE_CD 는 사업장·직군별 오버라이드 확장을 위한 예약 축. 0단계는
--    COMPANY/'-' 1행만 사용하며, 서비스도 COMPANY 스코프만 조회한다.
--  - 0단계에서는 관리 UI 를 만들지 않는다(스키마만 열어둠 — 지시서 B-1).
--
-- 멱등성: CREATE TABLE 중복 실행 시 에러(Table already exists). 이미 반영된 환경에서는 건너뛸 것.
-- 적용 순서: sojeong-1-1 → 1-2 → 1-3 → 1-4 → 1-5.
-- 운영 적용: 사용자 수동(Workbench). 본 파일은 작성만, DB 직접 적용 금지.
-- ============================================================================

CREATE TABLE `tb_cmpny_std_work_policy` (
  `CMPNY_CD` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '회사코드',
  `SCOPE_TYPE` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'COMPANY' COMMENT '적용 범위 COMPANY:회사 SITE:사업장(예약, 0단계 미사용)',
  `SCOPE_CD` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '-' COMMENT '범위 코드 (SCOPE_TYPE=COMPANY 이면 ''-'' 고정, SITE 이면 SITE_CD)',
  `WEEK_STD_MINUTES` int NOT NULL DEFAULT '2400' COMMENT '통상근로자 주 소정근로 분 (기본 2400=주40h). 행 부재 시 코드 폴백 2400',
  `INSERT_NO` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '입력자',
  `INSERT_DATE` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  `UPDATE_NO` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '수정자',
  `UPDATE_DATE` datetime DEFAULT NULL COMMENT '수정일시',
  PRIMARY KEY (`CMPNY_CD`,`SCOPE_TYPE`,`SCOPE_CD`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='통상근로자 주 소정근로시간 기준값 (①단시간 판정 분모 ②비례부여 분모 ③미입력 계정 폴백. 계정 자동주입 용도 아님 - B-1)';

-- ── 백필 시드 없음 ──
-- 행이 없으면 서비스가 2400분으로 폴백하므로 전 회사 시드를 두지 않는다.
-- 기준값이 주 40시간과 다른 회사가 나타나면 그 회사만 아래 형태로 1행 추가.
--   INSERT INTO `tb_cmpny_std_work_policy`
--     (`CMPNY_CD`, `SCOPE_TYPE`, `SCOPE_CD`, `WEEK_STD_MINUTES`, `INSERT_NO`)
--   VALUES ('회사코드', 'COMPANY', '-', 2280, 'SYSTEM');
