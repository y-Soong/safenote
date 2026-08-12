-- ============================================================================
-- SOJEONG-2-1 — 단축근무자 연장근로 "근로자 명시 청구" 확인 기록 컬럼 추가
--                (TB_USER_OVERTIME_MGMT)
-- 작성일: 2026-08-12
-- 적용 환경: MySQL 8.0.42 이상 (★개발·운영 동시 적용 — feedback_db_migration_apply_both_envs)
-- 참조: 작업지시서_근로자별-소정근로시간-관리-도입.md §단축근무자 "OT 게이트"
--       작업지시서_근로자별-소정근로시간-관리-도입.plan.md §4 소정-07
--       security 검토 M-4(명시 청구 확인 기록 미영속) — 2026-08-12 사용자 승인
--
-- 변경 요약
--  1) TB_USER_OVERTIME_MGMT 에 감사 컬럼 3개 추가 (DEL_YN 앞 = 감사 컬럼군 직전).
--     - REDUCED_CLAIM_YN    : 단축근무 근로자의 연장근로 명시 청구 확인 여부
--     - REDUCED_CLAIM_BY    : 확인 주체(사용자 코드)
--     - REDUCED_CLAIM_DTIME : 확인 시각
--
-- 배경 (왜 남겨야 하는가)
--  육아기·가족돌봄 근로시간 단축 기간의 연장근로는 사업주가 요구할 수 없고 근로자가 명시적으로
--  청구한 경우에만 가능하며, 위반 시 1천만원 이하 벌금이다. 소정-07 게이트가 요청의
--  reducedWorkOtClaimYn 으로 판정만 하고 값을 버리면 법정 요건의 유일한 증빙이 남지 않아
--  사후 분쟁에서 "근로자가 청구했다"를 입증할 수 없다(정책 §11.3 누가/언제/무엇을/왜).
--
-- 규약
--  - ★3컬럼 전부 NULL 허용. 단축 대상이 아닌 대다수 OT 는 "해당 없음"이므로 NULL 로 둔다.
--    'N' 으로 채우면 "해당 없음"과 "확인하지 않음"이 구분되지 않아 감사 가치가 사라진다.
--    (게이트 미진입 = 3컬럼 전부 NULL 이 불변식이다.)
--  - REDUCED_CLAIM_YN 은 사실상 'Y' 또는 NULL 만 적재된다. 청구 미확인('N' 상태)은
--    게이트가 ATTD_400_201 로 거부해 OT 행 자체가 생성되지 않기 때문이다.
--    'N' 코드값은 향후 정책 변경(예: 확인 없이 등록 허용 + 경고) 여지로만 남긴다.
--  - ★단축 사유코드(PREGNANCY/CHILDCARE/FAMILY_CARE)는 저장하지 않는다. userCd 와 결합하면
--    임신 사실(건강정보)·가족관계 정보가 되어 목적 범위를 넘는다(security M-3 규약,
--    정책 §11.1 목적제한·최소수집). 사유가 필요하면 TB_USER_STD_WORK_HOURS 를 조회한다.
--  - REDUCED_CLAIM_BY 의 의미는 경로별로 다르다(코드 주석과 동일 규약):
--      · 근로자 신청(REQ) 경유 승인 → 신청한 근로자 본인 USER_CD
--        (REQ 는 근로자 본인만 생성할 수 있고, 신청 시점에 게이트를 이미 통과했다)
--      · 관리자 직접 등록/수정      → 청구를 확인한 관리자 USER_CD
--  - 타입은 같은 테이블 관례를 따른다: Y/N 플래그 = varchar(1), 사용자 코드 = varchar(50)
--    (INSERT_NO/UPDATE_NO 와 동형), 일시 = datetime.
--  - COMMENT 는 코드성 컬럼 코드값 나열 관례 준수(feedback_db_comment_code_convention).
--
-- 인덱스: 추가하지 않는다. 본 컬럼은 감사 추적(특정 OT 행을 이미 특정한 뒤 확인)용이고,
--         현 단계에 "청구 확인 건 전수 조회" 화면이 없다. 필요 시 후속 작업에서 추가.
--
-- 멱등성: ALTER 중복 실행 시 에러(1060 Duplicate column). 이미 반영된 환경에서는 건너뛸 것.
-- 적용 순서: sojeong-1-1 ~ 1-5 적용 후 본 파일(2-1).
--            ★BE 재기동 전 선적용 필수 — 미적용 상태로 신규 코드가 뜨면 초과근무 등록(INSERT)과
--            수정(UPDATE)이 신규 컬럼 참조로 전면 실패한다(1054 Unknown column).
--            상세 실패 지점은 작업 보고 §④ 참조(insertUserOvertime / updateUserOvertimeModify /
--            updateUserOvertimeDirect).
-- 운영 적용: 사용자 수동(Workbench). 본 파일은 작성만, DB 직접 적용 금지.
-- ============================================================================

ALTER TABLE `tb_user_overtime_mgmt`
  ADD COLUMN `REDUCED_CLAIM_YN` varchar(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci
      DEFAULT NULL
      COMMENT '단축근무 연장근로 명시 청구 확인 Y/N (Y:근로자 청구 확인됨, NULL:해당없음-단축기간 아님. 육아기·가족돌봄 단축 기간 연장근로의 법정 요건 증빙)'
      AFTER `OT_STATUS`,
  ADD COLUMN `REDUCED_CLAIM_BY` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci
      DEFAULT NULL
      COMMENT '단축근무 연장근로 명시 청구 확인 주체 USER_CD (근로자 신청 경유=신청 근로자 본인, 관리자 직접등록=확인한 관리자)'
      AFTER `REDUCED_CLAIM_YN`,
  ADD COLUMN `REDUCED_CLAIM_DTIME` datetime
      DEFAULT NULL
      COMMENT '단축근무 연장근로 명시 청구 확인 일시 (OT 행 생성/갱신 시점)'
      AFTER `REDUCED_CLAIM_BY`;
