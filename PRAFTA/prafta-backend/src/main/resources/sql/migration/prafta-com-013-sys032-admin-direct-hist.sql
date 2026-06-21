-- ============================================================================
-- PRAFTA-COM-013 #5 — SYS032(요청/이력 유형) 디테일 2종 시드 (관리자 직접수정 이력 분리)
-- 작성일: 2026-06-18
-- 적용 환경: MySQL 8.0.42
-- 참조: .claude/requests/web_requests/prafta-com-013-attd-daydetail-fixes.md §#5,
--       prafta-com-004-sys045-noti-type.sql (SYS 디테일 시드 스타일 미러)
--
-- 변경 요약
--  1) SYS032(TB_SYST_VAL_D)에 관리자 직접수정 이력 유형 2종 추가:
--     - 11 = 관리자 생성  (관리자가 화면에서 근태를 직접 신규 생성한 이력)
--     - 12 = 관리자 수정  (관리자가 화면에서 기존 근태를 직접 수정한 이력)
--     기존 01~10 사용 중 → 빈 코드 11, 12 사용. SORT_IDX 11/12.
--  2) Attd07ServiceImpl.updateUserAttdInfos 가 attdId 유무로 11(생성)/12(수정)을 기록한다.
--     기존 HIST_TYPE='01'(근태 생성요청 승인)은 요청 승인 경로에서 계속 사용(의미 보존).
--
-- 이력 유형명(SYST_VAL_D_NM)은 일자 상세 처리이력 표 'histTypeNm'(FNC_CMM_INFO_SRCH SYS032)에 그대로 노출된다.
--
-- 적용 전 부재 확인:
--   SELECT 1 FROM tb_syst_val_m WHERE SYST_VAL_CD='SYS032';                                  -- 마스터 존재(=INSERT 생략)
--   SELECT SYST_VAL_D_CD FROM tb_syst_val_d WHERE SYST_VAL_CD='SYS032' AND SYST_VAL_D_CD IN ('11','12');
-- 멱등성: PK(SYST_VAL_CD, SYST_VAL_D_CD) 중복 시 에러. 운영 적용 후 보관용(재실행 금지). 이미 존재 시 건너뛸 것.
-- ============================================================================

-- ── SYS032 디테일 추가 (관리자 직접 생성/수정 이력 2종) ──
INSERT INTO `tb_syst_val_d`
      (`SYST_VAL_CD`, `SYST_VAL_D_CD`, `SYST_VAL_D_NM`, `SORT_IDX`, `USE_YN`, `VAL_D_DESC`, `INSERT_NO`)
VALUES
      ('SYS032', '11', '관리자 생성', 11, 'Y', '관리자 직접 근태 생성 이력 (Attd_07 updateUserAttdInfos, attdId 없음)', 'SYSTEM')
    , ('SYS032', '12', '관리자 수정', 12, 'Y', '관리자 직접 근태 수정 이력 (Attd_07 updateUserAttdInfos, attdId 있음)', 'SYSTEM');
