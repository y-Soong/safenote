-- ============================================================================
-- SOJEONG-1-4 — 소정근로시간 사유코드 그룹 신설 (SYS083)
-- 작성일: 2026-08-12
-- 적용 환경: MySQL 8.0.42 이상 (★개발·운영 동시 적용 — feedback_db_migration_apply_both_envs)
-- 참조: 작업지시서_근로자별-소정근로시간-관리-도입.md §단축근무자
--       작업지시서_근로자별-소정근로시간-관리-도입.plan.md §1.4
--
-- 변경 요약
--  1) tb_syst_val_m 에 SYS083(소정근로시간 사유코드) 그룹 1건.
--  2) tb_syst_val_d 에 사유 5건 — NORMAL / PART_TIME / CHILDCARE / PREGNANCY / FAMILY_CARE.
--
-- ★코드 번호 정정 (2026-08-12 실DB 확인)
--   plan §1.4 는 SYS044 로 적었으나 SYS044 는 이미 "결재 단계 상태"로 점유되어 있다.
--   실DB tb_syst_val_m 의 SYS 최대값이 SYS082(일용직 입장 승인요청 상태)이므로
--   신규 그룹은 SYS083 으로 채번한다. (컬럼명도 plan 의 VAL_D_CD 표기가 오기 —
--   실제는 SYST_VAL_D_CD)
--
-- ★설계 핵심: 분기 규칙을 코드가 아니라 데이터로 싣는다
--   VAL_D_INFO_1 = 종일 사용 시 차감 규칙
--       FIXED_480 : 480분(1일 등가) 고정 차감
--       DAILY_STD : 그날 소정근로분 차감 (6h 날 = 360분 = 0.75일 효과)
--   VAL_D_INFO_2 = 연차 부여 규칙
--       NO_PRORATE : 비례 없음(통상 기준 그대로)
--       PRORATE    : 시행령 별표2 비례부여(시간 단위)
--   2단계(분 단위 원장)의 차감·부여 분기는 이 두 컬럼을 읽어 수행한다. 사유코드 상수를
--   if 로 나열하는 하드코딩 금지 — 행정해석 변동 이력이 있는 영역이라(법제처 22-0070,
--   여성고용정책과 2025-09-30) 규칙 변경을 UPDATE 1건으로 흡수해야 한다.
--   ★노무사 확인 포인트 N-1 대상 데이터가 정확히 이 5행이다.
--
-- 적용 전 부재 확인:
--   SELECT 1 FROM tb_syst_val_m WHERE SYST_VAL_CD='SYS083';
--   SELECT SYST_VAL_CD FROM tb_syst_val_m WHERE SYST_VAL_CD LIKE 'SYS%' ORDER BY SYST_VAL_CD DESC LIMIT 1;
-- 멱등성: PK 중복 시 에러. 운영 적용 후 보관용(재실행 금지). 이미 존재 시 건너뛸 것.
-- 적용 순서: sojeong-1-1 → 1-2 → 1-3 → 1-4 → 1-5.
--            ★TB_USER_STD_WORK_HOURS.REASON_CD 가 본 그룹을 참조하므로 이력 등록 전 필수.
-- 운영 적용: 사용자 수동(Workbench). 본 파일은 작성만, DB 직접 적용 금지.
-- ============================================================================

-- 1) 코드 그룹 마스터.
INSERT INTO `tb_syst_val_m`
  (`SYST_VAL_CD`, `SYST_VAL_NM`, `USE_YN`, `VAL_DESC`, `INSERT_NO`)
VALUES
  ('SYS083', '소정근로시간 사유코드', 'Y',
   '근로자별 소정근로시간 이력(TB_USER_STD_WORK_HOURS.REASON_CD)의 사유. 2단계 차감·부여 정책의 분기 키 — 규칙은 VAL_D_INFO_1(차감)/VAL_D_INFO_2(부여)에 데이터로 적재',
   'SYSTEM');

-- 2) 사유 상세 5건.
INSERT INTO `tb_syst_val_d`
  (`SYST_VAL_CD`, `SYST_VAL_D_CD`, `SYST_VAL_D_NM`, `SORT_IDX`, `USE_YN`, `VAL_D_INFO_1`, `VAL_D_INFO_2`, `VAL_D_DESC`, `INSERT_NO`)
VALUES
  ('SYS083', 'NORMAL',      '통상',         1, 'Y', 'FIXED_480', 'NO_PRORATE', '통상근로자. 종일 차감 480분, 연차 비례부여 없음', 'SYSTEM')
, ('SYS083', 'PART_TIME',   '단시간계약',   2, 'Y', 'DAILY_STD', 'PRORATE',    '단시간근로자(근기법 2조1항9호). 종일 차감 그날 소정, 시행령 별표2 비례부여', 'SYSTEM')
, ('SYS083', 'CHILDCARE',   '육아기단축',   3, 'Y', 'DAILY_STD', 'PRORATE',    '육아기 근로시간 단축. 종일 차감 그날 단축 소정, 비례부여 + 단축기간 제외 출근율 80% 이상 시 전부 부여 예외(법제처 22-0070)', 'SYSTEM')
, ('SYS083', 'PREGNANCY',   '임신기단축',   4, 'Y', 'FIXED_480', 'NO_PRORATE', '임신기 근로시간 단축. 종일 차감 480분(1일 등가, 여성고용정책과 2025-09-30), 비례부여 없음. 연장근로 전면 금지', 'SYSTEM')
, ('SYS083', 'FAMILY_CARE', '가족돌봄단축', 5, 'Y', 'DAILY_STD', 'PRORATE',    '가족돌봄 등 근로시간 단축. 육아기와 동일 축(차감·부여·OT 명시 청구 절차)', 'SYSTEM');
