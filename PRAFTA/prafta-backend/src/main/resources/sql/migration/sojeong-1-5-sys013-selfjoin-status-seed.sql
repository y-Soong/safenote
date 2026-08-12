-- ============================================================================
-- SOJEONG-1-5 — SYS013(계정상태) 셀프가입 승인제 코드 신설 ('06 가입승인대기', '07 가입거부')
-- 작성일: 2026-08-12
-- 적용 환경: MySQL 8.0.42 이상 (★개발·운영 동시 적용 — feedback_db_migration_apply_both_envs)
-- 참조: 작업지시서_근로자별-소정근로시간-관리-도입.md §계정별 필수 입력 / 셀프가입 승인·거부
--       작업지시서_근로자별-소정근로시간-관리-도입.plan.md §1.5, §2, §8 Q2
--
-- 변경 요약
--  1) tb_syst_val_d 에 SYS013 '06 가입승인대기', '07 가입거부' 2건.
--  2) tb_user.ACCOUNT_STATUS COMMENT 를 코드값 전량 나열로 정렬
--     (feedback_db_comment_code_convention, prafta-036 관례).
--
-- ★코드 번호 정정 (2026-08-12 실DB 확인)
--   plan §1.5 는 '05 가입승인대기'를 제안했으나, 실DB SYS013 에는 이미
--   01 활성화 / 02 잠김 / 03 탈퇴 / 04 인증대기 / 05 비활성화 가 존재한다('05' 충돌).
--   → 신규는 '06 가입승인대기', '07 가입거부'로 채번한다.
--   ★후속 작업(소정-04) 및 앱/웹 분기에서 plan 의 '05' 표기를 전부 '06'으로 읽을 것.
--
-- 상태 의미
--  - '04 인증대기' : 관리자가 User_01 로 생성한 계정의 휴대폰 본인인증 미완료(PRAFTA-036).
--                    셀프가입과 의미가 다르므로 재활용하지 않는다(로그인 시 임시 PHONE_AUTH
--                    scope 토큰 분기와 충돌).
--  - '06 가입승인대기' : 셀프가입(회원가입) 직후 관리자 승인 대기. 로그인 시 승인대기 안내
--                    응답만 주고 정상 토큰을 발급하지 않는다.
--  - '07 가입거부' : 관리자가 거부한 셀프가입 신청. USE_YN='N' 로 보존한다.
--                    ★재가입 요건(plan §8 Q2): UX_TB_USER_ID(USER_ID 전역 유니크)와
--                    UX_TB_USER_MBL_NO(CMPNY_CD+MBL_NO_HMAC 유니크) 때문에 신규 INSERT 는
--                    충돌한다 → 동일 아이디/휴대폰이 '07' 행과 일치하면 그 행을 재활용해
--                    신청 정보를 갱신하고 '06'으로 전이한다. 거부 이력은 AuditLog 로 보존.
--
-- 적용 전 부재 확인:
--   SELECT SYST_VAL_D_CD, SYST_VAL_D_NM FROM tb_syst_val_d WHERE SYST_VAL_CD='SYS013' ORDER BY SORT_IDX;
-- 멱등성: PK 중복 시 에러. 운영 적용 후 보관용(재실행 금지). 이미 존재 시 건너뛸 것.
-- 적용 순서: sojeong-1-1 → 1-2 → 1-3 → 1-4 → 1-5.
-- 운영 적용: 사용자 수동(Workbench). 본 파일은 작성만, DB 직접 적용 금지.
-- ============================================================================

-- 1) SYS013 신규 상태 2건.
INSERT INTO `tb_syst_val_d`
  (`SYST_VAL_CD`, `SYST_VAL_D_CD`, `SYST_VAL_D_NM`, `SORT_IDX`, `USE_YN`, `VAL_D_DESC`, `INSERT_NO`)
VALUES
  ('SYS013', '06', '가입승인대기', 6, 'Y', '셀프가입(회원가입) 직후 관리자 승인 대기 상태. 승인 시 소정근로시간 입력 후 01 활성화로 전이', 'SYSTEM')
, ('SYS013', '07', '가입거부',     7, 'Y', '관리자가 셀프가입을 거부한 상태(USE_YN=N 보존). 동일 아이디/휴대폰 재가입 시 본 행을 재활용해 06 으로 전이', 'SYSTEM');

-- 2) tb_user.ACCOUNT_STATUS COMMENT 정렬 (코드성 컬럼 COMMENT 규칙).
--    tb_daily_user.ACCOUNT_STATUS 는 셀프가입 승인제 대상이 아니므로(일용직은 QR 발급 계통)
--    06/07 을 붙이지 않고 현행 COMMENT 를 유지한다.
ALTER TABLE `tb_user`
  MODIFY COLUMN `ACCOUNT_STATUS` varchar(20)
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci
  NOT NULL DEFAULT '01'
  COMMENT '계정상태[SYS013] 01:활성화 02:잠김 03:탈퇴 04:인증대기 05:비활성화 06:가입승인대기 07:가입거부';
