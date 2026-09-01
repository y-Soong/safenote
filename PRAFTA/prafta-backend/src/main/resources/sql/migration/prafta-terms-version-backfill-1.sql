-- =====================================================================
-- prafta-terms-version-backfill-1.sql
-- TB_TERMS_ID_VERSION 이력 누락분 백필 (2026-09-01)
--
-- [배경]
--   prafta-terms-real-content-1.sql (2026-07-19 작성, 07-20 실행)이 약관 001~005 의
--   실문안을 교체하면서 TB_TERMS 만 UPDATE 하고 TB_TERMS_ID_VERSION INSERT 를 누락했다.
--   그 결과 "현재 서비스에 노출 중인 버전"이 이력 테이블에 없어,
--   Platform_02(이용약관 관리) 우측 상세에서 현행 약관 본문을 볼 수 없었다.
--     운영 실측(2026-09-01): 001 v3 / 002 v2 / 003 v3 / 004 v2 / 005 v3 누락, 006 v1 만 정상
--
--   화면(Baim03ServiceImpl.updateTermsInfo)을 통한 저장은 두 테이블을 한 트랜잭션에서
--   함께 쓰므로 정상이다. 이 결함은 직접 실행한 마이그레이션 SQL 한 건에서만 발생했다.
--
-- [무엇을 하는가]
--   TB_TERMS(현행)에 있으나 TB_TERMS_ID_VERSION 에 같은 (TERMS_ID, TERMS_VERSION) 이
--   없는 행을 이력 테이블로 복사한다. 값은 TB_TERMS 의 것을 그대로 옮긴다.
--
-- [설계 결정]
--   - INSERT_DATE 를 NOW() 로 넣지 않는다. 실제 반영 시각(TB_TERMS.UPDATE_DATE,
--     없으면 INSERT_DATE)을 그대로 복사해야 이력의 시간 순서가 실제와 맞는다.
--   - INSERT_NO 도 TB_TERMS 의 UPDATE_NO(=SYSTEM)를 승계한다. 누가 넣었는지 왜곡하지 않는다.
--   - NOT EXISTS 가드로 멱등하다. 여러 번 실행해도 중복 INSERT 되지 않는다.
--   - TB_TERMS 는 건드리지 않는다(현행 본문·버전 무변경). 순수 이력 보강이다.
--
-- [적용 범위]
--   개발 DB · 운영 DB 양쪽에 동일 적용한다(CLAUDE.md — DB 마이그레이션 동시 적용 원칙).
--   한쪽만 적용하면 두 환경의 이력이 어긋난다.
--
-- [실행 방법]
--   운영 DB 쓰기는 Workbench 로 사용자가 직접 수행한다. 본 파일은 실행되지 않는다.
--   §1 사전확인 → §2 백필 → §3 사후검증 순서로 실행할 것.
-- =====================================================================


-- =====================================================================
-- §1. 사전 확인 (백필 대상 조회 — 쓰기 없음)
--     실행 결과가 예상 대상(개발·운영 공통 5건: 001 v3 / 002 v2 / 003 v3 / 004 v2 / 005 v3)과
--     같은지 눈으로 확인한 뒤 §2 로 넘어간다. 0건이면 이미 백필된 상태이므로 중단한다.
-- =====================================================================
SELECT T.TERMS_ID
     , T.TERMS_VERSION
     , T.STR_DATE
     , T.REQUIRED_YN
     , CHAR_LENGTH(T.TERMS_CONTENT) AS CONTENT_LEN
     , COALESCE(T.UPDATE_NO, T.INSERT_NO)     AS WILL_INSERT_NO
     , COALESCE(T.UPDATE_DATE, T.INSERT_DATE) AS WILL_INSERT_DATE
  FROM TB_TERMS T
 WHERE NOT EXISTS (
           SELECT 1
             FROM TB_TERMS_ID_VERSION V
            WHERE V.TERMS_ID      = T.TERMS_ID
              AND V.TERMS_VERSION = T.TERMS_VERSION
       )
 ORDER BY T.TERMS_ID;


-- =====================================================================
-- §2. 백필 실행
--     PK = (TERMS_ID, TERMS_VERSION). NOT EXISTS 가드로 멱등.
-- =====================================================================
INSERT INTO TB_TERMS_ID_VERSION (
       TERMS_ID
     , TERMS_VERSION
     , REQUIRED_YN
     , TERMS_CONTENT
     , STR_DATE
     , TERMS_DESC
     , INSERT_NO
     , INSERT_DATE
)
SELECT T.TERMS_ID
     , T.TERMS_VERSION
     , T.REQUIRED_YN
     , T.TERMS_CONTENT
     , T.STR_DATE
     , T.TERMS_DESC
     , COALESCE(T.UPDATE_NO, T.INSERT_NO, 'SYSTEM')
     , COALESCE(T.UPDATE_DATE, T.INSERT_DATE, NOW())
  FROM TB_TERMS T
 WHERE NOT EXISTS (
           SELECT 1
             FROM TB_TERMS_ID_VERSION V
            WHERE V.TERMS_ID      = T.TERMS_ID
              AND V.TERMS_VERSION = T.TERMS_VERSION
       );

COMMIT;


-- =====================================================================
-- §3. 사후 검증
-- =====================================================================

-- 3-1) 누락 0건이어야 한다.
SELECT COUNT(*) AS REMAINING_MISSING
  FROM TB_TERMS T
 WHERE NOT EXISTS (
           SELECT 1
             FROM TB_TERMS_ID_VERSION V
            WHERE V.TERMS_ID      = T.TERMS_ID
              AND V.TERMS_VERSION = T.TERMS_VERSION
       );

-- 3-2) 약관별 현행/이력 대조.
--      CUR_VER 가 VERSIONS 목록에 포함되어 있어야 한다.
SELECT T.TERMS_ID
     , T.TERMS_VERSION AS CUR_VER
     , (SELECT COUNT(*)
          FROM TB_TERMS_ID_VERSION V
         WHERE V.TERMS_ID = T.TERMS_ID)                       AS VER_ROWS
     , (SELECT GROUP_CONCAT(V.TERMS_VERSION ORDER BY CAST(V.TERMS_VERSION AS UNSIGNED))
          FROM TB_TERMS_ID_VERSION V
         WHERE V.TERMS_ID = T.TERMS_ID)                       AS VERSIONS
  FROM TB_TERMS T
 ORDER BY T.TERMS_ID;

-- 3-3) 본문이 실제로 같은지(현행 vs 이력) 확인 — 길이·해시 비교.
--      두 값이 각 행에서 동일해야 한다.
SELECT T.TERMS_ID
     , T.TERMS_VERSION
     , CHAR_LENGTH(T.TERMS_CONTENT) AS CUR_LEN
     , CHAR_LENGTH(V.TERMS_CONTENT) AS VER_LEN
     , MD5(T.TERMS_CONTENT)         AS CUR_MD5
     , MD5(V.TERMS_CONTENT)         AS VER_MD5
  FROM TB_TERMS T
       INNER JOIN TB_TERMS_ID_VERSION V
          ON V.TERMS_ID      = T.TERMS_ID
         AND V.TERMS_VERSION = T.TERMS_VERSION
 ORDER BY T.TERMS_ID;


-- =====================================================================
-- §4. 롤백 (필요 시에만)
--     이번 백필로 새로 들어간 행만 지운다. 기존 이력(001 v1·v2 등)은 건드리지 않는다.
--     ※ 실행 전 §1 결과(대상 목록)를 캡처해 두고, 그 (TERMS_ID, TERMS_VERSION) 만 지울 것.
--     아래는 예시이며 실제 대상에 맞게 수정해서 쓴다.
-- =====================================================================
-- DELETE FROM TB_TERMS_ID_VERSION
--  WHERE (TERMS_ID, TERMS_VERSION) IN (
--        ('001','3'), ('002','2'), ('003','3'), ('004','2'), ('005','3')
--        );
-- COMMIT;
