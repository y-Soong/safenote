-- ============================================================================
-- PRAFTA-SUBCON-T7-2 — 공유 데이터유형(SYS077) 디테일 확장: RISK / NEARMISS
-- 작성일: 2026-07-14
-- 적용 환경: MySQL 8.0.42
-- 출처: PRAFTA-SUBCON-T7.plan.md §3-2 + 메인 세션 DB 실측(SYS077 현재 ATTD 1건)
-- 참조: prafta-subcon-t3-2-sys-codes.sql (SYS077 마스터 + ATTD 디테일 생성분)
--
-- 변경 요약
--   - SYS077(공유 데이터유형) 디테일 2건 추가: RISK(위험성평가, 정렬 2) / NEARMISS(아차사고, 정렬 3).
--   - SYS077 마스터 행과 ATTD(정렬 1) 디테일은 T3 가 이미 생성 → 무변경(디테일만 INSERT).
--   - 신규 SYS 그룹 채번 없음(SYS080 은 T5 선점).
--
-- 적용 전 부재 확인:
--   SELECT SYST_VAL_D_CD FROM tb_syst_val_d
--    WHERE SYST_VAL_CD='SYS077' AND SYST_VAL_D_CD IN ('RISK','NEARMISS');  -- 0건이어야 함
-- 멱등성: PK 중복 시 에러. 이미 존재하면 건너뛸 것(재실행 금지).
-- 운영 적용: 사용자 수동(read-only MCP). 본 파일은 작성만, DB 직접 적용 금지.
-- ============================================================================

-- ── SYS077 디테일 추가(위험성평가 / 아차사고) ──
INSERT INTO `tb_syst_val_d`
      (`SYST_VAL_CD`, `SYST_VAL_D_CD`, `SYST_VAL_D_NM`, `SORT_IDX`, `USE_YN`, `INSERT_NO`)
VALUES
      ('SYS077', 'RISK',     '위험성평가', 2, 'Y', 'SYSTEM')
    , ('SYS077', 'NEARMISS', '아차사고',   3, 'Y', 'SYSTEM');

-- ============================================================================
-- 롤백 (필요 시 수동 실행)
-- ----------------------------------------------------------------------------
-- DELETE FROM `tb_syst_val_d`
--  WHERE SYST_VAL_CD='SYS077' AND SYST_VAL_D_CD IN ('RISK','NEARMISS');
-- ============================================================================
