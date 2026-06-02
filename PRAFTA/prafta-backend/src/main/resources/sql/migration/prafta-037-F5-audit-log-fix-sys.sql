-- ============================================================================
-- PRAFTA-037-F5 (정정) — 감사 액션 유형 SYS 코드그룹 충돌 해소
-- 작성일: 2026-05-29
-- 적용 환경: MySQL 8.0.42
-- 참조: .claude/requests/web_requests/prafta-037-F5-plan.md
--
-- 배경
--   prafta-037-F5-audit-log.sql 적용 결과, SYS046 이 이미 prafta-033 'TBM 세션 상태'
--   코드그룹으로 점유 중이었음을 운영 적용 후 확인.
--    - tb_syst_val_m.SYS046 마스터 INSERT 는 PK 중복으로 실패(에러 무시 또는 1행 영향 없음).
--    - tb_syst_val_d.SYS046 디테일 '01=다운로드' 는 TBM 코드(DRAFT/OPENED/...)와 같은
--      그룹에 섞여 들어감.
--
-- 해소 방향
--   SYS046 → SYS060 으로 코드그룹 이동(SYS047~SYS055 도 모두 TBM 점유, SYS060 최초 빈 번호).
--   본 파일은 다음 3단계를 수행한다:
--    1) 잘못 들어간 SYS046='01' (다운로드) 디테일 1건 삭제.
--    2) SYS060 마스터('감사 액션 유형') + 디테일 '01=다운로드' 신규 시드.
--    3) tb_audit_log.ACTION_TYPE 컬럼 COMMENT 정정 ([SYS046] → [SYS060]).
--
-- 적용 후 코드 영향
--   - AuditActionType.DOWNLOAD 값은 "01" 그대로 (변경 없음 — 코드그룹만 이동).
--   - AuditActionType.java 의 javadoc COMMENT 만 SYS060 으로 정정(코드 변경 없음).
--
-- 적용 전 부재 확인:
--   SELECT 1 FROM tb_syst_val_d WHERE SYST_VAL_CD='SYS046' AND SYST_VAL_D_CD='01' AND SYST_VAL_D_NM='다운로드';
--   SELECT 1 FROM tb_syst_val_m WHERE SYST_VAL_CD='SYS060';
-- 멱등성: 본 파일은 운영 적용 후 보관용(재실행 금지). 적용 전 상태 확인 후 수행.
-- 운영 적용: 사용자 수동(read-only MCP). 본 파일은 작성만, DB 직접 적용 금지.
-- ============================================================================

-- 1) 잘못 들어간 SYS046='01' (다운로드) 디테일 행 삭제.
DELETE FROM `tb_syst_val_d`
 WHERE `SYST_VAL_CD`   = 'SYS046'
   AND `SYST_VAL_D_CD` = '01'
   AND `SYST_VAL_D_NM` = '다운로드';

-- 2) SYS060 마스터.
INSERT INTO `tb_syst_val_m` (`SYST_VAL_CD`, `SYST_VAL_NM`, `USE_YN`, `VAL_DESC`, `INSERT_NO`)
VALUES ('SYS060', '감사 액션 유형', 'Y', 'tb_audit_log.ACTION_TYPE 코드', 'SYSTEM');

-- 3) SYS060 디테일 1건 시드.
INSERT INTO `tb_syst_val_d`
  (`SYST_VAL_CD`, `SYST_VAL_D_CD`, `SYST_VAL_D_NM`, `SORT_IDX`, `USE_YN`, `INSERT_NO`)
VALUES
  ('SYS060', '01', '다운로드', 1, 'Y', 'SYSTEM');

-- 4) tb_audit_log.ACTION_TYPE COMMENT 정정 (SYS046 → SYS060).
ALTER TABLE `tb_audit_log`
  MODIFY COLUMN `ACTION_TYPE` varchar(30)
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci
  NOT NULL
  COMMENT '감사 액션 유형[SYS060] 01:다운로드';
