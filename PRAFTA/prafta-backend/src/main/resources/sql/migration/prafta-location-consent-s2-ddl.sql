-- =====================================================================================
-- 위치정보 동의철회·중지 도입 S2 — DDL + 기존 동의 상태 승계
--
-- 근거 : .claude/refs/위치정보_동의철회_중지_작업지시서.md §2
-- 작성 : 2026-09-01
--
-- ★★적용 순서
--   1) §1 사전 현황 확인
--   2) §2 ~ §5 DDL 실행
--   3) §6 기존 동의 상태 승계 (안전 업데이트 모드 토글 필요)
--   4) §7 사후 검증
--
-- ★개발 DB / 운영 DB 양쪽에 적용한다(마이그레이션 양환경 동시적용 원칙).
-- ★애플리케이션 배포보다 DDL 을 먼저 적용한다(신규 컬럼을 읽는 쿼리가 배포와 함께 나간다).
-- =====================================================================================


-- =====================================================================================
-- §1. 사전 현황
-- =====================================================================================

-- (1-1) 약관 현황 — 005(위치기반서비스)가 REQUIRED_YN='Y' 인지 확인
SELECT TERMS_ID, TERMS_VERSION, REQUIRED_YN, USE_YN, STR_DATE, LEFT(TERMS_DESC, 40) AS TERMS_DESC
  FROM TB_TERMS
 ORDER BY TERMS_ID;

-- (1-2) 005 동의 현황 — 승계 대상 건수
SELECT TERMS_VERSION
     , COUNT(*)              AS ROWS_CNT
     , SUM(AGR_YN = 'Y')     AS AGREED
     , SUM(IFNULL(AGR_YN, 'N') <> 'Y') AS NOT_AGREED
  FROM TB_TERMS_USER_AGR_MGMT
 WHERE TERMS_ID = '005'
 GROUP BY TERMS_VERSION
 ORDER BY CAST(TERMS_VERSION AS UNSIGNED);

-- (1-3) 이미 적용됐는지 확인 — 결과가 있으면 §2~§5 를 건너뛴다
SELECT TABLE_NAME, COLUMN_NAME
  FROM information_schema.COLUMNS
 WHERE TABLE_SCHEMA = DATABASE()
   AND COLUMN_NAME IN ('LOGIN_GATE_YN', 'CONSENT_STATE', 'BEFORE_STATE', 'AFTER_STATE')
 ORDER BY TABLE_NAME, COLUMN_NAME;


-- =====================================================================================
-- §2. 약관 게이트 분리 — ★현행/이력 짝 테이블
--
--   REQUIRED_YN 하나가 두 곳을 동시에 제어하고 있어 005 를 표현할 수 없다.
--     가입 목록      BaseinfoMapper.selectJoinTermsList        WHERE REQUIRED_YN = 'Y'
--     로그인 게이트   Terms01Mapper.selectPendingRequiredTerms  WHERE REQUIRED_YN = 'Y'
--   005 는 "가입 때는 필수로 받고, 로그인 게이트는 안 잡아야" 하는 제3의 부류다.
--   005 를 REQUIRED_YN='N' 으로 내리면 가입 화면에서도 사라져 신규 가입자가 전원 미동의로 시작한다.
--
--   ★★TB_TERMS(현행, 가입·게이트가 읽음)와 TB_TERMS_ID_VERSION(이력, 관리 화면이 읽음)은
--     반드시 짝으로 처리한다. 2026-07-20 prafta-terms-real-content-1.sql 이 현행만 UPDATE 하고
--     이력을 누락해 6건 중 5건에서 현행 약관이 관리 화면에 보이지 않는 사고가 있었다.
-- =====================================================================================

ALTER TABLE TB_TERMS
  ADD COLUMN LOGIN_GATE_YN VARCHAR(1) NOT NULL DEFAULT 'Y'
  COMMENT '로그인 게이트 대상 여부[Y:게이트가 미동의를 잡음/N:제외] — 위치정보 동의철회 도입 S2';

ALTER TABLE TB_TERMS_ID_VERSION
  ADD COLUMN LOGIN_GATE_YN VARCHAR(1) NOT NULL DEFAULT 'Y'
  COMMENT '로그인 게이트 대상 여부(현행 TB_TERMS 와 짝)';


-- =====================================================================================
-- §3. 동의 상태 확장 (현재상태)
--
--   ★AGR_YN 의 의미를 바꾸지 않는다.
--     AGREED = AGR_YN 'Y', SUSPENDED/PENDING_REAGREE/WITHDRAWN = 전부 AGR_YN 'N'.
--     그래야 기존 AGR_YN='Y' 조건을 쓰는 모든 쿼리가 수정 없이 정확하게 동작한다
--     (중지·철회·재동의대기 = 미동의). 이것이 무회귀의 핵심 장치다.
-- =====================================================================================

ALTER TABLE TB_TERMS_USER_AGR_MGMT
  ADD COLUMN CONSENT_STATE VARCHAR(20) NULL
  COMMENT '동의상태[AGREED:동의/SUSPENDED:본인중지/PENDING_REAGREE:재동의대기/WITHDRAWN:철회] — NULL=상태관리 대상 아닌 약관';


-- =====================================================================================
-- §4. 동의 상태 확장 (전이 이력)
-- =====================================================================================

ALTER TABLE TB_TERMS_USER_AGR_HIST
  ADD COLUMN BEFORE_STATE VARCHAR(20) NULL
  COMMENT '전이 전 동의상태(NULL=최초 응답 또는 상태관리 대상 아님)';

ALTER TABLE TB_TERMS_USER_AGR_HIST
  ADD COLUMN AFTER_STATE VARCHAR(20) NULL
  COMMENT '전이 후 동의상태(NULL=상태관리 대상 아님)';


-- =====================================================================================
-- §5. 위치정보 파기 이력 (신규)
--
--   ★★설계 제약: 이 테이블에 좌표를 남기면 파기가 파기가 아니게 된다.
--     좌표 원본은 물론 해시·마스킹·부분값 등 어떤 파생 형태도 저장하지 않는다.
--     남기는 것은 "누가·언제·무엇을·몇 건·왜" 뿐이다.
--   위치정보법 시행령 제20조①의 취급대장 실체로도 사용한다.
--
--   동의 전이 이력(TB_TERMS_USER_AGR_HIST)과 별개 테이블이다 —
--   전자는 "상태가 바뀌었다", 후자는 "그래서 무엇을 지웠다"로 성격이 다르다.
-- =====================================================================================

CREATE TABLE TB_LOCATION_PURGE_HIST (
    PURGE_ID            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '파기이력ID(PK)'
  , CMPNY_CD            VARCHAR(50)  NOT NULL COMMENT '대상 계정 회사코드'
  , USER_CD             VARCHAR(20)  NOT NULL COMMENT '대상 계정 사용자코드'
  , USER_TYPE_CD        VARCHAR(10)  NOT NULL COMMENT '계정 계통[SYS050] REGULAR:정규직 DAILY:일용직'
  , PURGE_REASON_CD     VARCHAR(20)  NOT NULL COMMENT '파기사유[WITHDRAW:동의철회/RETENTION:보존기간경과]'
  , TERMS_VERSION       VARCHAR(10)  NULL     COMMENT '철회 시점 약관 버전(RETENTION 이면 NULL)'
  , ATTD_GPS_ROWS       INT          NOT NULL DEFAULT 0 COMMENT '출퇴근 좌표 파기 건수'
  , TBM_ATTENDANCE_ROWS INT          NOT NULL DEFAULT 0 COMMENT 'TBM 입실 좌표 파기 건수'
  , TBM_SESSION_ROWS    INT          NOT NULL DEFAULT 0 COMMENT 'TBM 개설자 좌표 파기 건수'
  , OLDEST_COLLECTED    VARCHAR(8)   NULL     COMMENT '파기 대상의 최초 수집일(YYYYMMDD)'
  , LATEST_COLLECTED    VARCHAR(8)   NULL     COMMENT '파기 대상의 최종 수집일(YYYYMMDD)'
  , ACTOR_CMPNY_CD      VARCHAR(50)  NOT NULL COMMENT '실행 주체 회사코드'
  , ACTOR_USER_CD       VARCHAR(20)  NOT NULL COMMENT '실행 주체 사용자코드(SYSTEM=배치)'
  , ACTION_DTIME        DATETIME     NOT NULL COMMENT '파기 일시(서버 NOW())'
  , INSERT_NO           VARCHAR(50)  NULL DEFAULT 'SYSTEM' COMMENT '입력자'
  , INSERT_DATE         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시'
  , PRIMARY KEY (PURGE_ID)
  , KEY IX_LOC_PURGE_USER (CMPNY_CD, USER_CD, ACTION_DTIME)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='위치정보 파기 이력(append-only) — 좌표 값은 어떤 형태로도 저장하지 않는다';


-- =====================================================================================
-- §6. 005 를 로그인 게이트에서 제외 + 기존 동의 상태 승계
--
--   ★안전 업데이트 모드(1175) 우회가 필요하다 — WHERE 절이 키 컬럼을 쓰지 않는다.
--     SET SQL_SAFE_UPDATES = 1 을 반드시 같이 실행할 것(세션 설정이라 커넥션 내내 꺼진 채 남는다).
-- =====================================================================================

SET SQL_SAFE_UPDATES = 0;

-- (6-1) 005 만 로그인 게이트에서 제외 — 현행/이력 짝으로
UPDATE TB_TERMS            SET LOGIN_GATE_YN = 'N' WHERE TERMS_ID = '005';
UPDATE TB_TERMS_ID_VERSION SET LOGIN_GATE_YN = 'N' WHERE TERMS_ID = '005';

-- (6-2) 기존 005 동의 상태 승계.
--   현재 005 는 필수라 사실상 전원이 AGR_YN='Y' 다. 예외 행이 있으면 §1-2 에서 먼저 원인을 확인할 것.
--   ★모든 버전에 대해 채운다 — 구버전 행의 상태도 있어야 "구버전만 동의(=재동의 대기)" 판정이 가능하다.
UPDATE TB_TERMS_USER_AGR_MGMT
   SET CONSENT_STATE = CASE WHEN AGR_YN = 'Y' THEN 'AGREED' ELSE 'WITHDRAWN' END
 WHERE TERMS_ID = '005'
   AND CONSENT_STATE IS NULL;

SET SQL_SAFE_UPDATES = 1;


-- =====================================================================================
-- §7. 사후 검증
-- =====================================================================================

-- (7-1) 게이트 플래그 — 005 만 'N', 나머지는 'Y' 여야 한다. 현행/이력이 같아야 한다.
SELECT 'TB_TERMS' AS TBL, TERMS_ID, TERMS_VERSION, REQUIRED_YN, LOGIN_GATE_YN
  FROM TB_TERMS
 UNION ALL
SELECT 'TB_TERMS_ID_VERSION', TERMS_ID, TERMS_VERSION, REQUIRED_YN, LOGIN_GATE_YN
  FROM TB_TERMS_ID_VERSION
 ORDER BY TBL, TERMS_ID, CAST(TERMS_VERSION AS UNSIGNED);

-- (7-2) 상태 승계 — 005 행 중 CONSENT_STATE 가 비어 있으면 0 이어야 한다.
SELECT COUNT(*) AS STATE_NOT_FILLED
  FROM TB_TERMS_USER_AGR_MGMT
 WHERE TERMS_ID = '005'
   AND CONSENT_STATE IS NULL;

-- (7-3) 상태 분포 — §1-2 의 AGREED 건수와 일치해야 한다.
SELECT CONSENT_STATE, COUNT(*) AS CNT
  FROM TB_TERMS_USER_AGR_MGMT
 WHERE TERMS_ID = '005'
 GROUP BY CONSENT_STATE;

-- (7-4) ★AGR_YN 과 상태의 정합 — 결과가 0 이어야 한다(AGREED 인데 AGR_YN 이 'Y' 가 아닌 행 등).
SELECT COUNT(*) AS STATE_AGR_MISMATCH
  FROM TB_TERMS_USER_AGR_MGMT
 WHERE TERMS_ID = '005'
   AND CONSENT_STATE IS NOT NULL
   AND ((CONSENT_STATE = 'AGREED' AND IFNULL(AGR_YN, 'N') <> 'Y')
     OR (CONSENT_STATE <> 'AGREED' AND IFNULL(AGR_YN, 'N') =  'Y'));

-- (7-5) 파기 이력 테이블 생성 확인
SELECT COUNT(*) AS PURGE_HIST_EXISTS
  FROM information_schema.TABLES
 WHERE TABLE_SCHEMA = DATABASE()
   AND TABLE_NAME = 'TB_LOCATION_PURGE_HIST';


-- =====================================================================================
-- §8. 롤백
--
--   ALTER TABLE TB_TERMS            DROP COLUMN LOGIN_GATE_YN;
--   ALTER TABLE TB_TERMS_ID_VERSION DROP COLUMN LOGIN_GATE_YN;
--   ALTER TABLE TB_TERMS_USER_AGR_MGMT DROP COLUMN CONSENT_STATE;
--   ALTER TABLE TB_TERMS_USER_AGR_HIST DROP COLUMN BEFORE_STATE;
--   ALTER TABLE TB_TERMS_USER_AGR_HIST DROP COLUMN AFTER_STATE;
--   DROP TABLE TB_LOCATION_PURGE_HIST;
--
--   ★단, 애플리케이션이 이미 배포된 상태에서 컬럼을 지우면 조회가 전멸한다.
--     롤백은 반드시 애플리케이션 롤백(이전 JAR) 이후에 수행한다.
--   ★파기가 이미 실행된 좌표는 어떤 롤백으로도 복구되지 않는다.
-- =====================================================================================
