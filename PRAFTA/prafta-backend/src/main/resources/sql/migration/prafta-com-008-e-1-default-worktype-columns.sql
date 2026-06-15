-- ============================================================================
-- PRAFTA-COM-008-E-1 — 사용자별 기본 근무타입 + 근무계획 생성출처 플래그 (마이그 단계 ①컬럼추가)
-- 작성일: 2026-06-11
-- 적용 환경: MySQL 8.0.42
-- 참조: .claude/requests/common/refs/prafta-com-008/prafta-com-008-E-decomposition.md §1(E-1)/§0
--       .claude/requests/common/prafta-com-008-E-default-worktype.md §2-1/§4-3
--       prafta-com-008-c-leave-source-consent.sql (SYS 시드 / ALTER 스타일 미러)
--
-- 변경 요약
--  1) tb_user 확장 — 사용자별 기본 근무타입(SCH_CD 참조) + 설정/변경 시점(스케줄러 트리거용).
--     교대팀 소속자는 NULL 무방(교대패턴이 스케줄 보장).
--  2) tb_user_work_plan 확장 — 자동생성분 식별용 생성출처 플래그 GEN_SOURCE(D-E3: 명시 컬럼).
--     기본근무 변경 시 갱신 대상 = GEN_SOURCE='DEFAULT_SCH' AND 미래일 AND 미마감월.
--     수동(MANUAL)·연차(LEAVE)·교대(SHIFT) 보존.
--  3) SYS074 신규 그룹(생성출처) 시드 — tb_syst_val_m / tb_syst_val_d.
--
-- 멱등성: ALTER/INSERT 중복 실행 시 에러. 이미 반영된 환경에서는 건너뛸 것.
-- 적용 순서: BE 재기동 전 선적용 필수(미적용 시 E-2/E-3/E-5 코드 전면 실패).
--            데이터 일괄삭제 DML(prafta-com-008-e-2-dev-data-purge.sql)은 본 파일 적용 후 사용자가 직접 적용.
--            BASE_YN DROP(prafta-com-008-e-4-drop-base-yn.sql)은 코드 배포 후 최후 적용.
-- ============================================================================

-- ── 1) tb_user 확장 (사용자별 기본 근무타입) ──
--   DEFAULT_SCH_CD       : 기본 근무타입 (tb_sch_mgmt.SCH_CD 참조, 회사/사업장 동적값 → SYS코드 아님).
--                          교대팀 소속자는 NULL 무방.
--   DEFAULT_SCH_SET_DATE : 기본 근무타입 설정/변경 시점(스케줄러 트리거용 audit).
ALTER TABLE `tb_user`
      ADD COLUMN `DEFAULT_SCH_CD`       varchar(20) DEFAULT NULL
          COMMENT '기본 근무타입(tb_sch_mgmt.SCH_CD 참조, 교대팀 소속자는 NULL 무방)' AFTER `EMPLOYMENT_TYPE`
    , ADD COLUMN `DEFAULT_SCH_SET_DATE` datetime    DEFAULT NULL
          COMMENT '기본 근무타입 설정/변경 시점(스케줄러 트리거용)' AFTER `DEFAULT_SCH_CD`;

-- ── 2) tb_user_work_plan 확장 (자동생성분 식별 플래그) ──
--   GEN_SOURCE : 생성출처[SYS074] MANUAL:수동 / DEFAULT_SCH:기본근무자동 / SHIFT:교대자동 / LEAVE:연차(레거시).
--                기본근무 변경 시 갱신 대상 = GEN_SOURCE='DEFAULT_SCH' (수동/연차/교대 보존).
--                기존 행 백필 불필요(데이터 일괄삭제 전제). 신규 INSERT 부터 채움(DEFAULT 'MANUAL').
ALTER TABLE `tb_user_work_plan`
      ADD COLUMN `GEN_SOURCE` varchar(10) NOT NULL DEFAULT 'MANUAL'
          COMMENT '생성출처[SYS074] MANUAL:수동 / DEFAULT_SCH:기본근무자동 / SHIFT:교대자동 / LEAVE:연차(레거시)' AFTER `WORK_PLAN_CD`;

-- ── 3) SYS074 생성출처 (WORK_PLAN_GEN_SOURCE) ──
INSERT INTO `tb_syst_val_m` (`SYST_VAL_CD`, `SYST_VAL_NM`, `USE_YN`, `VAL_DESC`, `INSERT_NO`)
VALUES ('SYS074', '근무계획 생성출처', 'Y', 'tb_user_work_plan.GEN_SOURCE 코드 (WORK_PLAN_GEN_SOURCE)', 'SYSTEM');
INSERT INTO `tb_syst_val_d`
      (`SYST_VAL_CD`, `SYST_VAL_D_CD`, `SYST_VAL_D_NM`, `SORT_IDX`, `USE_YN`, `INSERT_NO`)
VALUES
      ('SYS074', 'MANUAL',      '수동',         1, 'Y', 'SYSTEM')
    , ('SYS074', 'DEFAULT_SCH', '기본근무 자동', 2, 'Y', 'SYSTEM')
    , ('SYS074', 'SHIFT',       '교대 자동',     3, 'Y', 'SYSTEM')
    , ('SYS074', 'LEAVE',       '연차(레거시)',  4, 'Y', 'SYSTEM');
