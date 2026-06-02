-- ============================================================================
-- PRAFTA-036 — SYS013(계정상태) '04 인증대기' 코드 신설 + 컬럼 COMMENT 정렬
-- 작성일: 2026-05-28
-- 적용 환경: MySQL 8.0.42
-- 참조: .claude/requests/web_requests/prafta-036-plan.md §1 D2, §3.1, §3.4
--
-- 변경 요약
--  1) tb_syst_val_d 에 SYS013='04' '인증대기' 시드 1건.
--     - 관리자가 User_01 화면에서 단건/엑셀로 생성한 계정의 초기 상태.
--     - 첫 로그인 시 휴대폰 본인인증을 통과하면 '01 활성화'로 전이된다.
--  2) tb_user.ACCOUNT_STATUS / tb_daily_user.ACCOUNT_STATUS 의 COMMENT 를
--     '계정상태[SYS013] 01:활성화 02:잠김 03:탈퇴 04:인증대기' 로 정렬.
--     (코드성 컬럼 COMMENT 규칙 — '설명[SYS코드] 코드값:의미' 형식 준수)
--
-- 적용 전 부재 확인:
--   SELECT 1 FROM tb_syst_val_d WHERE SYST_VAL_CD='SYS013' AND SYST_VAL_D_CD='04';
-- 멱등성: PK 중복 시 에러. 운영 적용 후 보관용(재실행 금지). 이미 존재 시 건너뛸 것.
-- 운영 적용: 사용자 수동(read-only MCP). 본 파일은 작성만, DB 직접 적용 금지.
-- ============================================================================

-- 1) SYS013 '04 인증대기' 시드 1건.
INSERT INTO `tb_syst_val_d`
  (`SYST_VAL_CD`, `SYST_VAL_D_CD`, `SYST_VAL_D_NM`, `SORT_IDX`, `USE_YN`, `VAL_D_DESC`, `INSERT_NO`)
VALUES
  ('SYS013', '04', '인증대기', 4, 'Y', '관리자가 생성한 직후 휴대폰 본인인증 미완료 상태', 'SYSTEM');

-- 2) tb_user.ACCOUNT_STATUS COMMENT 정렬.
ALTER TABLE `tb_user`
  MODIFY COLUMN `ACCOUNT_STATUS` varchar(20)
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci
  NOT NULL DEFAULT '01'
  COMMENT '계정상태[SYS013] 01:활성화 02:잠김 03:탈퇴 04:인증대기';

-- 3) tb_daily_user.ACCOUNT_STATUS COMMENT 정렬 (스키마 일관성).
--    일용직(QR 발급)은 본 작업의 '인증대기' 흐름 적용 대상이 아니지만,
--    동일 SYS013 코드그룹을 참조하므로 COMMENT 만 동일 형식으로 정렬한다.
ALTER TABLE `tb_daily_user`
  MODIFY COLUMN `ACCOUNT_STATUS` varchar(20)
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci
  NOT NULL DEFAULT '01'
  COMMENT '계정상태[SYS013] 01:활성화 02:잠김 03:탈퇴 04:인증대기';
