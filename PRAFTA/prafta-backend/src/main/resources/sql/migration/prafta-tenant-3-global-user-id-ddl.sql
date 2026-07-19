-- =====================================================================
-- PRAFTA 멀티테넌시 결함 수정 (3/3) — 로그인 ID 전역 유일화
--   작성: 2026-07-13
--
--   배경(온보딩 차단):
--     로그인 쿼리(LoginMapper.Login)는 `WHERE A.USER_ID = #{userId}` 로 **회사 조건 없이** 사용자를 찾는데,
--     UNIQUE 제약은 `UX_TB_USER_ID = (CMPNY_CD, USER_ID)` 라 **ID 는 회사 안에서만 유일**했다.
--     → 두 번째 고객사가 같은 ID(admin/hr 등)를 만드는 순간 로그인 쿼리가 2행을 반환해
--       TooManyResultsException 이 나거나, 최악의 경우 **다른 회사 계정으로 인증**된다.
--
--   결정(사용자, 2026-07-13): **로그인 ID 를 전역 유일하게 한다.**
--     - 로그인 화면은 그대로(ID+비밀번호). 회사코드 입력 불필요.
--     - 계정 생성 시 전사 중복검사를 건다(코드 수정 동반: User01/DailyJoin/CompanyProvision).
--
--   ★ 실행 전 반드시 백업. 아래 §1 사전점검이 0행이어야 §2 를 실행할 수 있다.
--   ※ DDL 은 암묵적 커밋이라 롤백되지 않는다. 실패 시 §9 로 원복.
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1. 사전 점검 — 회사 간 중복 로그인 ID 가 있으면 유니크 제약을 걸 수 없다. 반드시 0행이어야 한다.
--    (0행이 아니면 §2 를 실행하지 말고, 중복 ID 를 먼저 개명한 뒤 재시도할 것)
-- ---------------------------------------------------------------------
SELECT USER_ID, COUNT(*) AS CNT, GROUP_CONCAT(CMPNY_CD) AS COMPANIES
  FROM TB_USER
 GROUP BY USER_ID
HAVING CNT > 1;

-- 일용직 계정(TB_DAILY_USER)도 TB_USER 에 미러되므로 함께 확인한다.
SELECT USER_ID, COUNT(*) AS CNT, GROUP_CONCAT(CMPNY_CD) AS COMPANIES
  FROM TB_DAILY_USER
 GROUP BY USER_ID
HAVING CNT > 1;

-- 2026-07-13 사전점검 결과: TB_USER 40행 / DISTINCT USER_ID 40 → 회사 간 중복 0건.
--   (TB_DAILY_USER 의 'QTDAILY1' 은 TB_USER 미러행이라 동일 회사·동일 USER_CD — 충돌 아님)

-- ---------------------------------------------------------------------
-- 2. UNIQUE 제약 전환 — (CMPNY_CD, USER_ID) → (USER_ID)
--    ※ PK(CMPNY_CD, USER_CD)는 그대로 둔다. 바꾸는 것은 "로그인 ID 유일성" 뿐이다.
-- ---------------------------------------------------------------------
ALTER TABLE TB_USER DROP INDEX UX_TB_USER_ID;
ALTER TABLE TB_USER ADD UNIQUE INDEX UX_TB_USER_ID (USER_ID);

-- TB_DAILY_USER 는 인덱스를 바꾸지 않는다(의도).
--   일용직 계정은 가입 시 TB_USER 에도 같은 (CMPNY_CD, USER_CD, USER_ID) 로 미러 적재된다
--   (확인: USER_ID='QTDAILY1' 이 두 테이블에 동일 회사·동일 USER_CD 로 존재).
--   따라서 위 TB_USER 의 전역 UNIQUE 가 일용직 ID 까지 자동으로 전역 유일하게 강제한다.
--   애플리케이션 측에서도 DailyJoin 의 ID 중복검사(TB_DAILY_USER + TB_USER 양쪽)를 전사 기준으로 전환했다.

-- ---------------------------------------------------------------------
-- 3. 검증 — UX_TB_USER_ID 가 USER_ID 단독 UNIQUE 여야 한다.
-- ---------------------------------------------------------------------
SELECT INDEX_NAME, GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) AS COLS, NON_UNIQUE
  FROM information_schema.STATISTICS
 WHERE TABLE_SCHEMA = DATABASE()
   AND TABLE_NAME = 'TB_USER'
   AND INDEX_NAME = 'UX_TB_USER_ID'
 GROUP BY INDEX_NAME, NON_UNIQUE;

-- ---------------------------------------------------------------------
-- 9. 롤백(필요 시)
-- ---------------------------------------------------------------------
-- ALTER TABLE TB_USER DROP INDEX UX_TB_USER_ID;
-- ALTER TABLE TB_USER ADD UNIQUE INDEX UX_TB_USER_ID (CMPNY_CD, USER_ID);
