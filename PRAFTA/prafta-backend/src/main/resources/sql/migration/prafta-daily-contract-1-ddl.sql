-- ============================================================================
-- PRAFTA-daily-contract-1 — 일용직 근로계약서 서명 + 입장 승인제 DDL 3종
-- 작성일: 2026-07-16
-- 적용 환경: MySQL 8.0.42
-- 출처: .claude/requests/common/작업지시서_일용직-계약서-서명-승인제.md §5-1, §6
--       .claude/requests/common/작업지시서_일용직-계약서-서명-승인제.plan.md §3 (DDL 초안)
--
-- 목적:
--   1) TB_DAILY_CONTRACT      — 계약서 양식 (사업장 단위 버전 관리, R1. 활성 1건 = 기능성 유니크)
--   2) TB_DAILY_ENTRY_REQUEST — 입장 승인요청 (승인 사이클. open(01/02)은 계정당 1건)
--   3) TB_DAILY_CONTRACT_SIGN — 계약서 서명본 (append-only. 근로기준법 §42 3년 보존)
--
-- 설계 근거:
--   - 멀티테넌시: 3테이블 전부 PK 에 CMPNY_CD 포함 (신규 고객사 등록 결함 — 22테이블 복합키 교훈).
--   - 활성/open 한정 UNIQUE 는 tb_daily_user.UX_TB_DAILY_USER_MBL 의
--     (if(조건, 값, NULL)) 함수형 인덱스 패턴을 그대로 따른다 (tb_daily_blacklist 동일).
--   - charset/collation = utf8mb4 / utf8mb4_unicode_ci (tb_daily_user 정합).
--   - TB_DAILY_CONTRACT_SIGN 은 UPDATE 컬럼 없음 = 수정 금지 설계.
--     탈퇴/만료 후에도 미삭제 — PII 파기 배치에서 명시적 제외 대상 (근로기준법 §42).
--   - 채번: REQ_ID/SIGN_ID 는 회사별 MAX+1 채번 (prefix 'ER'/'CS', T2/T3 developer 확정).
--
-- 적용 전 존재 확인 (운영 적용 직전 필수 — 0건이어야 함. 있으면 해당 CREATE 건너뛸 것):
--   SELECT TABLE_NAME
--     FROM information_schema.TABLES
--    WHERE TABLE_SCHEMA = DATABASE()
--      AND TABLE_NAME IN ('TB_DAILY_CONTRACT','TB_DAILY_ENTRY_REQUEST','TB_DAILY_CONTRACT_SIGN');
--
-- 멱등성: CREATE TABLE 재실행 시 이미 존재하면 에러 → 반영된 환경에서는 건너뛸 것.
-- 운영 적용: 사용자 수동(read-only MCP). 본 파일은 작성만, DB 직접 적용 금지.
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1) 계약서 양식 (사업장 단위, R1. 활성 버전은 사업장당 1건 — 기능성 유니크)
-- ----------------------------------------------------------------------------
CREATE TABLE `TB_DAILY_CONTRACT` (
  `CMPNY_CD`      varchar(50)  COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '회사코드',
  `SITE_CD`       varchar(50)  COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사업장코드',
  `CONTRACT_VER`  int          NOT NULL COMMENT '계약서 버전(1부터 증가, 교체 시 +1)',
  `CONTRACT_NM`   varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '계약서명(표시용)',
  `FILE_MGMT_CD`  varchar(50)  COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '계약서 이미지 파일코드(TB_FILE_INFO)',
  `USE_YN`        varchar(2)   COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'Y' COMMENT '사용여부(교체/사용중지 시 N)',
  `INSERT_NO`     varchar(50)  COLLATE utf8mb4_unicode_ci DEFAULT 'SYSTEM' COMMENT '입력자',
  `INSERT_DATE`   datetime     DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  `UPDATE_NO`     varchar(50)  COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '수정자',
  `UPDATE_DATE`   datetime     DEFAULT NULL COMMENT '수정일시',
  PRIMARY KEY (`CMPNY_CD`,`SITE_CD`,`CONTRACT_VER`),
  -- 활성(USE_YN='Y') 계약서는 사업장당 1건만 허용 — 함수형 인덱스(tb_daily_user 패턴).
  UNIQUE KEY `UX_DAILY_CONTRACT_ACTIVE` (`CMPNY_CD`,`SITE_CD`,(if((`USE_YN` = _utf8mb4'Y'),_utf8mb4'Y',NULL)))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='일용직 근로계약서 양식(사업장 단위 버전 관리)';

-- ----------------------------------------------------------------------------
-- 2) 입장 승인요청 (승인 사이클. open(01/02)은 계정당 1건 — 기능성 유니크)
--    상태 전이: 01 대기 → 02 승인 → 05 소진(로그인 성공) / 03 거부 / 04 만료(자정)
-- ----------------------------------------------------------------------------
CREATE TABLE `TB_DAILY_ENTRY_REQUEST` (
  `CMPNY_CD`      varchar(50)  COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '회사코드',
  `REQ_ID`        varchar(20)  COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '승인요청ID(채번)',
  `SITE_CD`       varchar(50)  COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사업장코드',
  `USER_CD`       varchar(20)  COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '일용직 사용자코드',
  `REQ_TYPE`      varchar(2)   COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '요청유형[SYS081: 01신규가입/02재입장]',
  `REQ_STATUS`    varchar(2)   COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '01' COMMENT '요청상태[SYS082: 01대기/02승인/03거부/04만료/05소진]',
  `REQ_DTIME`     datetime     NOT NULL COMMENT '요청일시(서버)',
  `PROC_USER_CD`  varchar(20)  COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '처리자(승인/거부 관리자 USER_CD)',
  `PROC_DTIME`    datetime     DEFAULT NULL COMMENT '처리일시(승인/거부)',
  `REJECT_REASON` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '거부 사유(내부 기록용 — 일용직에게 미노출)',
  `CONSUME_DTIME` datetime     DEFAULT NULL COMMENT '소진일시(승인 후 실제 로그인 성공)',
  `INSERT_NO`     varchar(50)  COLLATE utf8mb4_unicode_ci DEFAULT 'SYSTEM' COMMENT '입력자',
  `INSERT_DATE`   datetime     DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  `UPDATE_NO`     varchar(50)  COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '수정자',
  `UPDATE_DATE`   datetime     DEFAULT NULL COMMENT '수정일시',
  PRIMARY KEY (`CMPNY_CD`,`REQ_ID`),
  -- open(대기/승인) 요청은 계정당 1건만 허용 — 중복 요청 DB 차원 방어.
  UNIQUE KEY `UX_DAILY_ENTRY_REQ_OPEN` (`CMPNY_CD`,`USER_CD`,(if((`REQ_STATUS` in (_utf8mb4'01',_utf8mb4'02')),_utf8mb4'O',NULL))),
  -- 관리자 승인 목록 조회(사업장+상태+요청일) 가속.
  KEY `IX_DAILY_ENTRY_REQ_SITE` (`CMPNY_CD`,`SITE_CD`,`REQ_STATUS`,`REQ_DTIME`),
  -- 로그인 판정(계정+상태+요청일) / 당일 거부 이력 조회 가속.
  KEY `IX_DAILY_ENTRY_REQ_USER` (`CMPNY_CD`,`USER_CD`,`REQ_STATUS`,`REQ_DTIME`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='일용직 입장 승인요청(승인 사이클)';

-- ----------------------------------------------------------------------------
-- 3) 계약서 서명본 (append-only. 탈퇴/만료 후에도 미삭제 — 근로기준법 §42 3년 보존.
--    UPDATE 컬럼 없음 = 수정 금지 설계. PII 파기 배치에서 명시적 제외 대상)
-- ----------------------------------------------------------------------------
CREATE TABLE `TB_DAILY_CONTRACT_SIGN` (
  `CMPNY_CD`            varchar(50)  COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '회사코드',
  `SIGN_ID`             varchar(20)  COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '서명ID(채번)',
  `SITE_CD`             varchar(50)  COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사업장코드',
  `USER_CD`             varchar(20)  COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '일용직 사용자코드',
  `USER_NM_SNAPSHOT`    varchar(50)  COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '서명 시점 사용자명 스냅샷(계정 만료 후 조회 대비)',
  `CONTRACT_VER`        int          NOT NULL COMMENT '서명 대상 계약서 버전(TB_DAILY_CONTRACT)',
  `REQ_ID`              varchar(20)  COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '승인요청ID(승인 사이클 연결. 배포 전 기존 활성 일용직 서명은 NULL)',
  `SIGN_FILE_MGMT_CD`   varchar(50)  COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '서명 PNG 원본 파일코드(TB_FILE_INFO)',
  `MERGED_FILE_MGMT_CD` varchar(50)  COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '합성본(계약서+계약정보블록+서명) 파일코드(TB_FILE_INFO)',
  `MERGED_SHA256`       char(64)     COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '합성본 SHA-256 해시(hex, 증적 무결성)',
  `FIRST_WORK_DATE`     char(8)      COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '최초 근로일(YYYYMMDD, =서명일. 미래 종료일 미기재 — D1)',
  `SIGN_DTIME`          datetime     NOT NULL COMMENT '서명일시(서버 NOW — 클라이언트 시각 불신)',
  `INSERT_NO`           varchar(50)  COLLATE utf8mb4_unicode_ci DEFAULT 'SYSTEM' COMMENT '입력자',
  `INSERT_DATE`         datetime     DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  PRIMARY KEY (`CMPNY_CD`,`SIGN_ID`),
  -- 서명 게이트 판정(계정+버전) / 본인 서명본 조회 가속.
  KEY `IX_DAILY_CONTRACT_SIGN_USER` (`CMPNY_CD`,`USER_CD`,`CONTRACT_VER`,`SIGN_DTIME`),
  -- 관리자 서명 이력 목록(사업장+기간) 가속.
  KEY `IX_DAILY_CONTRACT_SIGN_SITE` (`CMPNY_CD`,`SITE_CD`,`SIGN_DTIME`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='일용직 근로계약서 서명본(append-only, 3년 보존)';

-- ============================================================================
-- 적용 후 검증 (운영 적용 후 1회 실행)
-- ----------------------------------------------------------------------------
-- 1) 3테이블 생성 확인 (3행이어야 함):
--    SELECT TABLE_NAME, TABLE_COMMENT
--      FROM information_schema.TABLES
--     WHERE TABLE_SCHEMA = DATABASE()
--       AND TABLE_NAME IN ('TB_DAILY_CONTRACT','TB_DAILY_ENTRY_REQUEST','TB_DAILY_CONTRACT_SIGN');
-- 2) 기능성 유니크 인덱스 확인 (UX_DAILY_CONTRACT_ACTIVE / UX_DAILY_ENTRY_REQ_OPEN 존재):
--    SELECT TABLE_NAME, INDEX_NAME, NON_UNIQUE
--      FROM information_schema.STATISTICS
--     WHERE TABLE_SCHEMA = DATABASE()
--       AND INDEX_NAME IN ('UX_DAILY_CONTRACT_ACTIVE','UX_DAILY_ENTRY_REQ_OPEN')
--     GROUP BY TABLE_NAME, INDEX_NAME, NON_UNIQUE;
-- 3) 컬럼 구성 확인:
--    SHOW CREATE TABLE TB_DAILY_CONTRACT;
--    SHOW CREATE TABLE TB_DAILY_ENTRY_REQUEST;
--    SHOW CREATE TABLE TB_DAILY_CONTRACT_SIGN;
-- ============================================================================
-- 롤백 (필요 시 수동 실행 — 서명본은 3년 보존 대상이므로 운영 데이터 존재 시 DROP 금지)
-- ----------------------------------------------------------------------------
-- DROP TABLE IF EXISTS `TB_DAILY_CONTRACT_SIGN`;
-- DROP TABLE IF EXISTS `TB_DAILY_ENTRY_REQUEST`;
-- DROP TABLE IF EXISTS `TB_DAILY_CONTRACT`;
-- ============================================================================
