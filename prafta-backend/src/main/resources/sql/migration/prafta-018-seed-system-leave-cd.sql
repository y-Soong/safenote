-- ============================================================================
-- PRAFTA-018 단계 2 — 시스템 LEAVE_CD 시드 6종 일회성 백필 (회사 전체 대상)
-- 작업 ID    : PLNprafta-018002
-- 적용일자   : 2026-05-20
-- 작성자     : developer (Claude Code)
-- 참조 정책서: .claude/context/policies/attd/08-leave.md §8.5.5 (시스템 LEAVE_CD 시드 6종)
-- 선행 단계  : .claude/requests/prafta-018.sql 섹션 2 (tb_leave_type_mgmt.SYSTEM_YN 컬럼 추가)
--
-- 목적:
--   tb_cmpny.USE_YN='Y' 활성 회사 전체에 시스템 자동 부여용 시드 6종을
--   (CMPNY_CD, LEAVE_CD) PK 기준으로 멱등 INSERT 한다. 이미 시드가 존재하는
--   회사·시드 조합은 건너뛴다(WHERE NOT EXISTS). 본 파일은 단일 트랜잭션이며
--   중간 실패 시 전체 롤백된다.
--
-- 시드 6종 (정책서 §8.5.5 표):
--   LEAVE_CD           LEAVE_NM        LEAVE_NATURE_TYPE
--   SYS_ANNUAL         연차            01 (법정)
--   SYS_MONTHLY        월차            01 (법정)
--   SYS_TENURE_BONUS   근속가산 연차   01 (법정)
--   SYS_PROMOTION      사용촉진 연차   01 (법정)
--   SYS_PREGRANT       일괄선부여 연차 01 (법정)
--   SYS_BIRTHDAY       생일 안식휴가   02 (특별)
--
-- 공통 컬럼값 (정책서 §8.5.5):
--   LEAVE_TYPE='02'       관리자 부여
--   GRANT_TYPE='01'       자동부여
--   PAID_TYPE='01'        유급
--   SYSTEM_YN='Y'         시스템 시드 (attd03 화면 편집 차단)
--   USE_YN='Y'            사용중
--   INSERT_NO='SYSTEM'
--   LEAVE_NO=LEAVE_CD     (기존 시드 패턴 LEAVE_SUMMER/LEAVE_ADMIN_AUTH 등 따름)
--
-- 실행 방법 (Windows + Git Bash, dev 환경):
--   mysql -h127.0.0.1 -P3306 -udev_prafta -p'prafta12345!' \
--         --default-character-set=utf8mb4 --batch prafta \
--         < prafta-backend/src/main/resources/sql/migration/prafta-018-seed-system-leave-cd.sql
--
-- 멱등성:
--   각 INSERT는 INSERT ... SELECT ... WHERE NOT EXISTS 패턴이라 재실행해도 추가
--   INSERT 가 발생하지 않는다. 신규 회사가 추가된 후 본 파일을 재실행하면 해당
--   회사에만 시드가 INSERT 된다(단, 운영상 신규 회사 시드는
--   prafta-018-seed-system-leave-cd-template.sql 로 프로시저화 권장).
-- ============================================================================

START TRANSACTION;

-- ----------------------------------------------------------------------------
-- 1) SYS_ANNUAL — 본연차 (1년 이상 법정 연차)
-- ----------------------------------------------------------------------------
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
      c.CMPNY_CD
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
  FROM tb_cmpny c
 WHERE c.USE_YN = 'Y'
   AND NOT EXISTS (
       SELECT 1
         FROM tb_leave_type_mgmt t
        WHERE t.CMPNY_CD = c.CMPNY_CD
          AND t.LEAVE_CD = 'SYS_ANNUAL'
   );

-- ----------------------------------------------------------------------------
-- 2) SYS_MONTHLY — 1년 미만 법정 월차
-- ----------------------------------------------------------------------------
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
      c.CMPNY_CD
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
  FROM tb_cmpny c
 WHERE c.USE_YN = 'Y'
   AND NOT EXISTS (
       SELECT 1
         FROM tb_leave_type_mgmt t
        WHERE t.CMPNY_CD = c.CMPNY_CD
          AND t.LEAVE_CD = 'SYS_MONTHLY'
   );

-- ----------------------------------------------------------------------------
-- 3) SYS_TENURE_BONUS — AXIS5 근속 가산
-- ----------------------------------------------------------------------------
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
      c.CMPNY_CD
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
  FROM tb_cmpny c
 WHERE c.USE_YN = 'Y'
   AND NOT EXISTS (
       SELECT 1
         FROM tb_leave_type_mgmt t
        WHERE t.CMPNY_CD = c.CMPNY_CD
          AND t.LEAVE_CD = 'SYS_TENURE_BONUS'
   );

-- ----------------------------------------------------------------------------
-- 4) SYS_PROMOTION — AXIS7=Y 사용촉진 잔여 처리
-- ----------------------------------------------------------------------------
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
      c.CMPNY_CD
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
  FROM tb_cmpny c
 WHERE c.USE_YN = 'Y'
   AND NOT EXISTS (
       SELECT 1
         FROM tb_leave_type_mgmt t
        WHERE t.CMPNY_CD = c.CMPNY_CD
          AND t.LEAVE_CD = 'SYS_PROMOTION'
   );

-- ----------------------------------------------------------------------------
-- 5) SYS_PREGRANT — AXIS3_PREGRANT_YN=Y 입사일 일괄선부여
-- ----------------------------------------------------------------------------
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
      c.CMPNY_CD
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
  FROM tb_cmpny c
 WHERE c.USE_YN = 'Y'
   AND NOT EXISTS (
       SELECT 1
         FROM tb_leave_type_mgmt t
        WHERE t.CMPNY_CD = c.CMPNY_CD
          AND t.LEAVE_CD = 'SYS_PREGRANT'
   );

-- ----------------------------------------------------------------------------
-- 6) SYS_BIRTHDAY — 생일 안식휴가 (특별, PRAFTA-017 SYS027='02' 자동부여와 별개 예약)
-- ----------------------------------------------------------------------------
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
      c.CMPNY_CD
    , 'SYS_BIRTHDAY'
    , 'SYS_BIRTHDAY'
    , '생일 안식휴가'
    , '02'
    , '01'
    , '01'
    , '02'                -- 특별 (정책서 §8.5.5에서 유일하게 02)
    , 'Y'
    , 'Y'
    , 'SYSTEM'
    , NOW()
  FROM tb_cmpny c
 WHERE c.USE_YN = 'Y'
   AND NOT EXISTS (
       SELECT 1
         FROM tb_leave_type_mgmt t
        WHERE t.CMPNY_CD = c.CMPNY_CD
          AND t.LEAVE_CD = 'SYS_BIRTHDAY'
   );

COMMIT;

-- ----------------------------------------------------------------------------
-- 검증 쿼리 (read-only, 트랜잭션 외 실행 권장):
--   SELECT CMPNY_CD, LEAVE_CD, LEAVE_NM, LEAVE_NATURE_TYPE, SYSTEM_YN
--     FROM tb_leave_type_mgmt
--    WHERE SYSTEM_YN = 'Y'
--    ORDER BY CMPNY_CD, LEAVE_CD;
--   기대: 활성 회사 수 × 6 행 (CMPNY_CD 별로 SYS_ANNUAL/SYS_MONTHLY/SYS_TENURE_BONUS/
--        SYS_PROMOTION/SYS_PREGRANT/SYS_BIRTHDAY 6종)
-- ============================================================================
-- 끝.
-- ============================================================================
