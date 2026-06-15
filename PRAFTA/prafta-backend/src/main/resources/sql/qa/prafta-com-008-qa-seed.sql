-- ============================================================================
-- prafta-com-008-qa-seed.sql — 통합 런타임 QA 검증용 seed (개발 DB 1회용)
-- 작성: 2026-06-12 (세션2). 적용: 사용자 수동 실행(MCP read-only).
-- 롤백: 동일 디렉토리 prafta-com-008-qa-seed-rollback.sql
--
-- 날짜는 CURDATE() 기준 상대 산출 → 적용일이 며칠 밀려도 도래 판정 유효.
--   1차 도래: AVAIL_TO_DATE = 적용일+6개월 (창 = 만료 6개월 전부터 10일, day 0)
--   2차 도래: AVAIL_TO_DATE = 적용일+3개월 (정확일)
-- ============================================================================

-- Workbench safe update mode 해제(USER_ID 가 비키 컬럼이라 1175 발생) — 스크립트 끝에서 원복.
SET SQL_SAFE_UPDATES = 0;

-- ----------------------------------------------------------------------------
-- [1] QA 계정 비밀번호 통일 — 4계정 모두 'QaTest2026!'
--     (PasswordHasher 동일 알고리즘으로 생성: BCrypt12(B64(HMAC-SHA256(pepper, pw))))
--     원래 해시는 롤백 파일에 보존되어 있음.
-- ----------------------------------------------------------------------------
UPDATE tb_user SET USER_PW = '$2a$12$UnM4zulckOh2vf1RU208VufFpXFSwKGhXwd2jjHFew7jOLmgAwnkS'
     , PWD_CHG_DTIME = NOW()   -- NULL 이면 매 로그인 PASSWORD_CHANGE 분기가 끼므로 QA 동안 세팅
 WHERE USER_ID IN ('TEST01', 'WLSGML108', 'SOON', 'ADMIN');

-- ----------------------------------------------------------------------------
-- [2] 촉진 도래 grant 3건
--     TEST01    (20260400014, 비교대) : 1차 도래 — S1(기본근무 게이트)→S3(계획서) 체인
--     WLSGML108 (20260400011, 교대)   : 1차 도래 — S5(차단)·S10(교대자 촉진) 용
--     SOON      (20260400013, 교대)   : 2차 도래 — S4(웹 직권지정) 용
--       * SOON 기존 grant(만료 20270505)보다 임박 → BASE 가 seed grant 로 잡힘
-- ----------------------------------------------------------------------------
INSERT INTO tb_user_leave_grant (
      GRANT_ID, CMPNY_CD, USER_CD, LEAVE_CD, GRANT_TYPE
    , GRANT_DAYS, USED_DAYS, GRANT_REASON, GRANT_BY_TYPE, POLICY_SEQ
    , GRANT_DATE, AVAIL_FROM_DATE, AVAIL_TO_DATE
    , IDEMPOTENCY_KEY, STATUS, EXPIRE_YN, DEL_YN, INSERT_NO, INSERT_DATE
) VALUES
  ('G20260612QA001', '001', '20260400014', 'SYS_ANNUAL', 'STATUTORY_ANNUAL'
    , 10.0, 0, 'QA seed com-008 (1차 도래)', '01', 7
    , DATE_FORMAT(CURDATE(), '%Y%m%d'), DATE_FORMAT(CURDATE(), '%Y%m%d')
    , DATE_FORMAT(DATE_ADD(CURDATE(), INTERVAL 6 MONTH), '%Y%m%d')
    , 'QA_SEED_COM008_TEST01_S1', 'ACTIVE', 'N', 'N', 'QA_SEED', NOW())
, ('G20260612QA002', '001', '20260400011', 'SYS_ANNUAL', 'STATUTORY_ANNUAL'
    , 10.0, 0, 'QA seed com-008 (1차 도래)', '01', 7
    , DATE_FORMAT(CURDATE(), '%Y%m%d'), DATE_FORMAT(CURDATE(), '%Y%m%d')
    , DATE_FORMAT(DATE_ADD(CURDATE(), INTERVAL 6 MONTH), '%Y%m%d')
    , 'QA_SEED_COM008_WLSGML108_S1', 'ACTIVE', 'N', 'N', 'QA_SEED', NOW())
, ('G20260612QA003', '001', '20260400013', 'SYS_ANNUAL', 'STATUTORY_ANNUAL'
    , 8.0, 0, 'QA seed com-008 (2차 도래)', '01', 7
    , DATE_FORMAT(CURDATE(), '%Y%m%d'), DATE_FORMAT(CURDATE(), '%Y%m%d')
    , DATE_FORMAT(DATE_ADD(CURDATE(), INTERVAL 3 MONTH), '%Y%m%d')
    , 'QA_SEED_COM008_SOON_S2', 'ACTIVE', 'N', 'N', 'QA_SEED', NOW());

-- ----------------------------------------------------------------------------
-- [3] 교대 근무계획 — WLSGML108·SOON, 오늘~+3개월 평일, GEN_SOURCE='SHIFT'
--     (교대자 촉진 등록은 work_plan 존재 필수 / S10 Attd_05 잠금 오버레이 셀 표시용)
--     WORK_PLAN_CD='00003' (ST002, 07:00~15:00). 현재 테이블 0행이라 충돌 없음.
-- ----------------------------------------------------------------------------
INSERT INTO tb_user_work_plan (CMPNY_CD, SITE_CD, USER_CD, WORK_YMD, WORK_PLAN_CD, GEN_SOURCE, INSERT_NO, INSERT_DATE)
WITH RECURSIVE d AS (
    SELECT CURDATE() AS dt
    UNION ALL
    SELECT dt + INTERVAL 1 DAY FROM d WHERE dt < DATE_ADD(CURDATE(), INTERVAL 3 MONTH)
)
SELECT '001', '00001', u.USER_CD, DATE_FORMAT(d.dt, '%Y%m%d'), '00003', 'SHIFT', 'QA_SEED', NOW()
  FROM d
  JOIN (SELECT '20260400011' AS USER_CD UNION ALL SELECT '20260400013') u
 WHERE DAYOFWEEK(d.dt) NOT IN (1, 7);

-- ----------------------------------------------------------------------------
-- [4] B(노무수령거부) 검증용 — WLSGML108 오늘자 촉진 확정 연차 1건 + USED_DAYS 동기화
--     오늘 출근/퇴근/OT 시도 → ATTD_400_150/151 차단 + BLOCKED 이력(REQUIRES_NEW) 검증.
--     DIRECT_USE_KEY 는 STORED GENERATED — INSERT 대상 아님.
-- ----------------------------------------------------------------------------
INSERT INTO tb_user_leave_use (
      LEAVE_ID, CMPNY_CD, SITE_CD, USER_CD, LEAVE_CD
    , REQ_ID, GRANT_ID, START_DATE, END_DATE
    , USE_UNIT_TYPE, LEAVE_DAYS, LEAVE_REASON, LEAVE_STATUS
    , PROMOTION_STAGE, DESIGNATOR_TYPE, ORIG_DESIGNATED_DATE
    , DEL_YN, INSERT_NO, INSERT_DATE
) VALUES
  ('LV20260612QA001', '001', '00001', '20260400011', 'SYS_ANNUAL'
    , NULL, 'G20260612QA002', DATE_FORMAT(CURDATE(), '%Y%m%d'), DATE_FORMAT(CURDATE(), '%Y%m%d')
    , '00', 1.0, 'QA seed 노무수령거부 차단 검증', 'CONFIRMED'
    , 'FIRST', 'VOLUNTARY', DATE_FORMAT(CURDATE(), '%Y%m%d')
    , 'N', 'QA_SEED', NOW());

UPDATE tb_user_leave_grant SET USED_DAYS = 1.0, UPDATE_NO = 'QA_SEED', UPDATE_DATE = NOW()
 WHERE GRANT_ID = 'G20260612QA002';

-- ----------------------------------------------------------------------------
-- [5] S7(자발 연차일 출근 허용) 검증용 — SOON 오늘자 "자발(비촉진)" 연차 1건
--     PROMOTION_STAGE NULL → 노무수령거부 차단 미적용 → 출근 허용 + OT만 ATTD_400_151.
--     WLSGML108(촉진, 차단)과 같은 날 대조 검증.
-- ----------------------------------------------------------------------------
INSERT INTO tb_user_leave_use (
      LEAVE_ID, CMPNY_CD, SITE_CD, USER_CD, LEAVE_CD
    , REQ_ID, GRANT_ID, START_DATE, END_DATE
    , USE_UNIT_TYPE, LEAVE_DAYS, LEAVE_REASON, LEAVE_STATUS
    , PROMOTION_STAGE, DESIGNATOR_TYPE, ORIG_DESIGNATED_DATE
    , DEL_YN, INSERT_NO, INSERT_DATE
) VALUES
  ('LV20260612QA002', '001', '00001', '20260400013', 'SYS_ANNUAL'
    , NULL, 'G20260612QA003', DATE_FORMAT(CURDATE(), '%Y%m%d'), DATE_FORMAT(CURDATE(), '%Y%m%d')
    , '00', 1.0, 'QA seed 자발 연차(차단 미적용 대조)', 'CONFIRMED'
    , 'NONE', 'VOLUNTARY', NULL   -- 비촉진=NONE(SYS068, NOT NULL), 자발=VOLUNTARY(SYS069)
    , 'N', 'QA_SEED', NOW());

UPDATE tb_user_leave_grant SET USED_DAYS = 1.0, UPDATE_NO = 'QA_SEED', UPDATE_DATE = NOW()
 WHERE GRANT_ID = 'G20260612QA003';

-- safe update mode 원복
SET SQL_SAFE_UPDATES = 1;

-- ============================================================================
-- 적용 후 확인 쿼리 (선택)
-- SELECT GRANT_ID, USER_CD, AVAIL_TO_DATE, GRANT_DAYS, USED_DAYS FROM tb_user_leave_grant WHERE GRANT_ID LIKE 'G20260612QA%';
-- SELECT COUNT(*) FROM tb_user_work_plan WHERE INSERT_NO='QA_SEED';
-- SELECT LEAVE_ID, USER_CD, START_DATE, PROMOTION_STAGE, DIRECT_USE_KEY FROM tb_user_leave_use WHERE LEAVE_ID='LV20260612QA001';
-- ============================================================================
