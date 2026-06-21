-- ============================================================================
-- PRAFTA-COM-016-E — SYS032(요청/이력 유형) 디테일 1종 시드 (초과근무 삭제 이력)
-- 작성일: 2026-06-18
-- 적용 환경: MySQL 8.0.42
-- 참조: .claude/requests/common/prafta-com-016-E.md §E,
--       prafta-com-013-sys032-admin-direct-hist.sql (SYS 디테일 시드 스타일 미러)
--
-- 변경 요약
--  1) SYS032(TB_SYST_VAL_D)에 초과근무 삭제 이력 유형 1종 추가:
--     - 13 = 초과근무 삭제  (관리자가 일자 상세에서 OT 를 직접 삭제한 이력)
--     기존 01~12 사용 중 → 빈 코드 13 사용. SORT_IDX 13.
--  2) Attd07ServiceImpl.deleteUserOvertime 가 OT 소프트삭제 시 HIST_TYPE='13' 이력을 기록한다.
--     기존 09(초과근무 반려)는 근로자 요청 반려 경로에서 계속 사용(의미 보존).
--
-- 이력 유형명(SYST_VAL_D_NM)은 일자 상세 처리이력 표 'histTypeNm'(FNC_CMM_INFO_SRCH SYS032)에 그대로 노출된다.
--
-- 적용 전 부재 확인:
--   SELECT 1 FROM tb_syst_val_m WHERE SYST_VAL_CD='SYS032';                                  -- 마스터 존재(=INSERT 생략)
--   SELECT SYST_VAL_D_CD FROM tb_syst_val_d WHERE SYST_VAL_CD='SYS032' AND SYST_VAL_D_CD = '13';
-- 멱등성: PK(SYST_VAL_CD, SYST_VAL_D_CD) 중복 시 에러. 운영 적용 후 보관용(재실행 금지). 이미 존재 시 건너뛸 것.
-- ============================================================================

-- ── SYS032 디테일 추가 (초과근무 삭제 이력 1종) ──
INSERT INTO `tb_syst_val_d`
      (`SYST_VAL_CD`, `SYST_VAL_D_CD`, `SYST_VAL_D_NM`, `SORT_IDX`, `USE_YN`, `VAL_D_DESC`, `INSERT_NO`)
VALUES
      ('SYS032', '13', '초과근무 삭제', 13, 'Y', '관리자 직접 초과근무 삭제 이력 (Attd_07 deleteUserOvertime)', 'SYSTEM');
