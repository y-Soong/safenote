-- ============================================================================
-- PRAFTA-SUBCON-T1-2 — 회사 연동 관계 상태 공통코드 시드 (SYS076 신규)
-- 작성일: 2026-07-13
-- 적용 환경: MySQL 8.0.42
-- 출처: PRAFTA-SUBCON.md §2.4 (SYS 채번은 마스터 정의), PRAFTA-SUBCON-T1.plan.md §4
-- 참조: prafta-com-001-sys-codes.sql (시드 스타일 미러)
--
-- 채번 확정 (2026-07-13 로컬 DB 실측 — tb_syst_val_m 현행 최대 SYS075):
--   - SYS076 = 회사 연동 관계 상태 (본 파일에서 시드) ← plan 후보와 일치
--   - SYS077 = 공유 데이터유형(ATTD/RISK/NEARMISS) — T3 예약, 본 파일에서 시드하지 않음
--   - SYS078 = 공유요청 상태 — T3 예약, 본 파일에서 시드하지 않음
--   (예약 2건은 마스터 §2.4 에 따라 메인 세션 공지 대상)
--
-- 변경 요약
--   1) SYS076(회사 연동 관계 상태) 마스터 + 디테일 5종:
--      REQUESTED(요청중) / ACCEPTED(연동중) / REJECTED(거부됨) / CANCELLED(요청취소) / TERMINATED(해지됨)
--      (tb_cmpny_relation.STATUS 코드 카탈로그)
--
-- 적용 전 부재 확인:
--   SELECT 1 FROM tb_syst_val_m WHERE SYST_VAL_CD = 'SYS076';   -- 0건이어야 함
-- 멱등성: PK 중복 시 에러. 운영 적용 후 보관용(재실행 금지). 이미 존재 시 건너뛸 것.
-- 운영 적용: 사용자 수동(read-only MCP). 본 파일은 작성만, DB 직접 적용 금지.
-- ============================================================================

-- ── 1) SYS076 마스터 (회사 연동 관계 상태) ──
INSERT INTO `tb_syst_val_m` (`SYST_VAL_CD`, `SYST_VAL_NM`, `USE_YN`, `VAL_DESC`, `INSERT_NO`)
VALUES ('SYS076', '회사 연동 관계 상태', 'Y', 'tb_cmpny_relation.STATUS 코드', 'SYSTEM');

-- ── 2) SYS076 디테일 (상태 5종) ──
INSERT INTO `tb_syst_val_d`
      (`SYST_VAL_CD`, `SYST_VAL_D_CD`, `SYST_VAL_D_NM`, `SORT_IDX`, `USE_YN`, `INSERT_NO`)
VALUES
      ('SYS076', 'REQUESTED',  '요청중',   1, 'Y', 'SYSTEM')
    , ('SYS076', 'ACCEPTED',   '연동중',   2, 'Y', 'SYSTEM')
    , ('SYS076', 'REJECTED',   '거부됨',   3, 'Y', 'SYSTEM')
    , ('SYS076', 'CANCELLED',  '요청취소', 4, 'Y', 'SYSTEM')
    , ('SYS076', 'TERMINATED', '해지됨',   5, 'Y', 'SYSTEM');

-- ============================================================================
-- 롤백 (필요 시 수동 실행)
-- ----------------------------------------------------------------------------
-- DELETE FROM `tb_syst_val_d` WHERE SYST_VAL_CD = 'SYS076';
-- DELETE FROM `tb_syst_val_m` WHERE SYST_VAL_CD = 'SYS076';
-- ============================================================================
