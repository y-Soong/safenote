-- ============================================================================
-- PRAFTA-018 단계 2 — 시스템 LEAVE_CD 시드 6종 (신규 회사 단일 INSERT 템플릿)
-- 작업 ID    : PLNprafta-018002
-- 작성일자   : 2026-05-20
-- 작성자     : developer (Claude Code)
-- 참조 정책서: .claude/context/policies/attd/08-leave.md §8.5.5
--
-- 용도:
--   추후 작성될 "신규 고객 생성 프로시저"에 포함될 시드 INSERT 조각이다.
--   단일 CMPNY_CD 파라미터를 받아 시스템 LEAVE_CD 6종을 멱등 INSERT 한다.
--   본 파일은 자동 실행되지 않는다(보관용 + 프로시저 작성 시 복사 대상).
--
-- Collation 주의:
--   tb_cmpny / tb_leave_type_mgmt 의 CMPNY_CD 컬럼 collation 은
--   utf8mb4_unicode_ci 이다. MySQL 8 의 user variable 은 기본 connection
--   collation(보통 utf8mb4_0900_ai_ci)로 평가되므로 비교 시 collation
--   mismatch 가 발생한다. 이를 피하기 위해 user variable 참조부마다
--   COLLATE utf8mb4_unicode_ci 를 명시한다.
--   (프로시저 IN 파라미터로 변환할 때는 파라미터 타입을
--    VARCHAR(50) COLLATE utf8mb4_unicode_ci 로 선언하여 COLLATE 절을 생략 가능)
--
-- 호출 방식 (CLI에서 직접 실행하려면 user variable 사용):
--   mysql -h127.0.0.1 -P3306 -udev_prafta -p'prafta12345!' \
--         --default-character-set=utf8mb4 --batch prafta \
--         --init-command="SET @cmpny_cd := '신규회사코드'" \
--         < prafta-backend/src/main/resources/sql/migration/prafta-018-seed-system-leave-cd-template.sql
--
-- 프로시저 내 사용 시:
--   CREATE PROCEDURE create_company(
--       IN p_cmpny_cd VARCHAR(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
--       ...)
--   BEGIN
--     -- ...회사 마스터 INSERT...
--     SET @cmpny_cd := p_cmpny_cd;
--     -- 아래 6개 INSERT 본문 그대로 포함 (COLLATE 절 유지 또는 제거 가능)
--     -- ...
--   END
--
-- 시드 코드값 출처: 정책서 §8.5.5 표 (일괄 백필 SQL 파일과 동일)
-- 멱등성: 각 INSERT 는 WHERE NOT EXISTS 로 중복 차단.
-- ============================================================================

-- 1) SYS_ANNUAL — 본연차 (1년 이상 법정 연차)
INSERT INTO tb_leave_type_mgmt (
      CMPNY_CD
    , LEAVE_CD
    , LEAVE_NO
    , LEAVE_NM
    , LEAVE_TYPE
    , GRANT_TYPE
    , PAID_TYPE
    , LEAVE_NATURE_TYPE
    , USE_YN
    , SYSTEM_YN
    , INSERT_NO
    , INSERT_DATE
)
SELECT
      @cmpny_cd COLLATE utf8mb4_unicode_ci
    , 'SYS_ANNUAL'
    , 'SYS_ANNUAL'
    , '연차'
    , '02'
    , '01'
    , '01'
    , '01'
    , 'Y'
    , 'Y'
    , 'SYSTEM'
    , NOW()
  FROM DUAL
 WHERE NOT EXISTS (
       SELECT 1
         FROM tb_leave_type_mgmt t
        WHERE t.CMPNY_CD = @cmpny_cd COLLATE utf8mb4_unicode_ci
          AND t.LEAVE_CD = 'SYS_ANNUAL'
   );

-- 2) SYS_MONTHLY — 1년 미만 법정 월차
INSERT INTO tb_leave_type_mgmt (
      CMPNY_CD
    , LEAVE_CD
    , LEAVE_NO
    , LEAVE_NM
    , LEAVE_TYPE
    , GRANT_TYPE
    , PAID_TYPE
    , LEAVE_NATURE_TYPE
    , USE_YN
    , SYSTEM_YN
    , INSERT_NO
    , INSERT_DATE
)
SELECT
      @cmpny_cd COLLATE utf8mb4_unicode_ci
    , 'SYS_MONTHLY'
    , 'SYS_MONTHLY'
    , '월차'
    , '02'
    , '01'
    , '01'
    , '01'
    , 'Y'
    , 'Y'
    , 'SYSTEM'
    , NOW()
  FROM DUAL
 WHERE NOT EXISTS (
       SELECT 1
         FROM tb_leave_type_mgmt t
        WHERE t.CMPNY_CD = @cmpny_cd COLLATE utf8mb4_unicode_ci
          AND t.LEAVE_CD = 'SYS_MONTHLY'
   );

-- 3) SYS_TENURE_BONUS — AXIS5 근속 가산
INSERT INTO tb_leave_type_mgmt (
      CMPNY_CD
    , LEAVE_CD
    , LEAVE_NO
    , LEAVE_NM
    , LEAVE_TYPE
    , GRANT_TYPE
    , PAID_TYPE
    , LEAVE_NATURE_TYPE
    , USE_YN
    , SYSTEM_YN
    , INSERT_NO
    , INSERT_DATE
)
SELECT
      @cmpny_cd COLLATE utf8mb4_unicode_ci
    , 'SYS_TENURE_BONUS'
    , 'SYS_TENURE_BONUS'
    , '근속가산 연차'
    , '02'
    , '01'
    , '01'
    , '01'
    , 'Y'
    , 'Y'
    , 'SYSTEM'
    , NOW()
  FROM DUAL
 WHERE NOT EXISTS (
       SELECT 1
         FROM tb_leave_type_mgmt t
        WHERE t.CMPNY_CD = @cmpny_cd COLLATE utf8mb4_unicode_ci
          AND t.LEAVE_CD = 'SYS_TENURE_BONUS'
   );

-- 4) SYS_PROMOTION — AXIS7=Y 사용촉진 잔여 처리
INSERT INTO tb_leave_type_mgmt (
      CMPNY_CD
    , LEAVE_CD
    , LEAVE_NO
    , LEAVE_NM
    , LEAVE_TYPE
    , GRANT_TYPE
    , PAID_TYPE
    , LEAVE_NATURE_TYPE
    , USE_YN
    , SYSTEM_YN
    , INSERT_NO
    , INSERT_DATE
)
SELECT
      @cmpny_cd COLLATE utf8mb4_unicode_ci
    , 'SYS_PROMOTION'
    , 'SYS_PROMOTION'
    , '사용촉진 연차'
    , '02'
    , '01'
    , '01'
    , '01'
    , 'Y'
    , 'Y'
    , 'SYSTEM'
    , NOW()
  FROM DUAL
 WHERE NOT EXISTS (
       SELECT 1
         FROM tb_leave_type_mgmt t
        WHERE t.CMPNY_CD = @cmpny_cd COLLATE utf8mb4_unicode_ci
          AND t.LEAVE_CD = 'SYS_PROMOTION'
   );

-- 5) SYS_PREGRANT — AXIS3_PREGRANT_YN=Y 입사일 일괄선부여
INSERT INTO tb_leave_type_mgmt (
      CMPNY_CD
    , LEAVE_CD
    , LEAVE_NO
    , LEAVE_NM
    , LEAVE_TYPE
    , GRANT_TYPE
    , PAID_TYPE
    , LEAVE_NATURE_TYPE
    , USE_YN
    , SYSTEM_YN
    , INSERT_NO
    , INSERT_DATE
)
SELECT
      @cmpny_cd COLLATE utf8mb4_unicode_ci
    , 'SYS_PREGRANT'
    , 'SYS_PREGRANT'
    , '일괄선부여 연차'
    , '02'
    , '01'
    , '01'
    , '01'
    , 'Y'
    , 'Y'
    , 'SYSTEM'
    , NOW()
  FROM DUAL
 WHERE NOT EXISTS (
       SELECT 1
         FROM tb_leave_type_mgmt t
        WHERE t.CMPNY_CD = @cmpny_cd COLLATE utf8mb4_unicode_ci
          AND t.LEAVE_CD = 'SYS_PREGRANT'
   );

-- 6) SYS_BIRTHDAY — 생일 안식휴가 (특별)
INSERT INTO tb_leave_type_mgmt (
      CMPNY_CD
    , LEAVE_CD
    , LEAVE_NO
    , LEAVE_NM
    , LEAVE_TYPE
    , GRANT_TYPE
    , PAID_TYPE
    , LEAVE_NATURE_TYPE
    , USE_YN
    , SYSTEM_YN
    , INSERT_NO
    , INSERT_DATE
)
SELECT
      @cmpny_cd COLLATE utf8mb4_unicode_ci
    , 'SYS_BIRTHDAY'
    , 'SYS_BIRTHDAY'
    , '생일 안식휴가'
    , '02'
    , '01'
    , '01'
    , '02'                -- 특별 (정책서 §8.5.5)
    , 'Y'
    , 'Y'
    , 'SYSTEM'
    , NOW()
  FROM DUAL
 WHERE NOT EXISTS (
       SELECT 1
         FROM tb_leave_type_mgmt t
        WHERE t.CMPNY_CD = @cmpny_cd COLLATE utf8mb4_unicode_ci
          AND t.LEAVE_CD = 'SYS_BIRTHDAY'
   );

-- ============================================================================
-- 끝.
-- 프로시저로 통합 시 위 6개 INSERT 본문을 그대로 BEGIN...END 사이에 복사하고,
-- @cmpny_cd 자리를 IN 파라미터 명으로 치환한다. 파라미터 컬럼 타입을
-- VARCHAR(50) COLLATE utf8mb4_unicode_ci 로 선언하면 COLLATE 절을 생략해도 된다.
-- ============================================================================
