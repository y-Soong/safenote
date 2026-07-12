-- ============================================================================
-- PRAFTA-daily-blacklist-1 — 일일계정 블랙리스트 테이블(tb_daily_blacklist) DDL
-- 작성일: 2026-06-28
-- 적용 환경: MySQL 8.0.42
-- 출처: .claude/requests/web_requests/prafta-daily-blacklist.md (확정 결정 §2)
--
-- 목적:
--   관리자가 등록한 휴대폰번호(블랙리스트)로의 일용직 회원가입/자동재활성(로그인)을 차단한다.
--   매칭 규칙: CMPNY_CD + MBL_NO_HMAC + USE_YN='Y' 카운트 > 0 이면 블랙(차단).
--
-- 설계:
--   - 스코프 = 회사(CMPNY_CD). 휴대폰은 평문 저장 금지 → ENC(표시용 복호)/HMAC(매칭)/LAST4(마스킹).
--   - 해제 = soft delete(USE_YN 'Y'→'N'). 활성 중복은 생성 컬럼 기반 UNIQUE 로 DB 차원에서도 방어.
--   - 활성 한정 UNIQUE 패턴은 tb_daily_user.UX_TB_DAILY_USER_MBL 의
--     (if(USE_YN='Y', MBL_NO_HMAC, NULL)) 함수형 인덱스 패턴을 그대로 따른다.
--   - charset/collation = utf8mb4 / utf8mb4_unicode_ci (tb_daily_user 정합).
--
-- 적용 전 부재 확인(운영 적용 직전 권장):
--   SHOW TABLES LIKE 'tb_daily_blacklist';   -- 0건이어야 함(이미 있으면 (CREATE) 건너뛸 것).
--
-- 멱등성: CREATE TABLE 재실행 시 이미 존재하면 에러 → 반영된 환경에서는 건너뛸 것.
-- 운영 적용: 사용자 수동(read-only MCP). 본 파일은 작성만, DB 직접 적용 금지.
-- ============================================================================

CREATE TABLE `tb_daily_blacklist` (
    `CMPNY_CD`      varchar(50)  COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '회사코드',
    `BLACKLIST_ID`  varchar(20)  COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '블랙리스트ID(채번: B+YYYYMMDD+SEQ)',
    `MBL_NO_ENC`    text         COLLATE utf8mb4_unicode_ci          COMMENT '휴대폰번호 AES-GCM (v1.base64url, 표시 복호용)',
    `MBL_NO_HMAC`   varchar(43)  COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '휴대폰번호 HMAC-SHA256 Base64URL (매칭/중복검사)',
    `MBL_NO_LAST4`  char(4)      COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '휴대폰번호 마지막4자리(마스킹/리스트용)',
    `REASON`        varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '등록 사유(필수)',
    `USE_YN`        varchar(2)   COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'Y' COMMENT '사용여부(해제 시 N)',
    `INSERT_NO`     varchar(50)  COLLATE utf8mb4_unicode_ci DEFAULT 'SYSTEM' COMMENT '입력자(USER_CD)',
    `INSERT_DATE`   datetime     DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
    `UPDATE_NO`     varchar(50)  COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '수정자(USER_CD)',
    `UPDATE_DATE`   datetime     DEFAULT NULL COMMENT '수정일시',
    PRIMARY KEY (`CMPNY_CD`,`BLACKLIST_ID`),
    -- 활성행(USE_YN='Y') 한정 휴대폰 유니크 — 해제 후 동일번호 재등록 허용(tb_daily_user 패턴).
    UNIQUE KEY `UX_TB_DAILY_BLACKLIST_MBL` (`CMPNY_CD`,(if((`USE_YN` = _utf8mb4'Y'),`MBL_NO_HMAC`,NULL))),
    -- 가입/재활성 게이트 룩업(CMPNY_CD + MBL_NO_HMAC + USE_YN) 가속.
    KEY `IX_TB_DAILY_BLACKLIST_LOOKUP` (`CMPNY_CD`,`MBL_NO_HMAC`,`USE_YN`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='일일계정 블랙리스트';

-- ============================================================================
-- 롤백 (필요 시 수동 실행)
-- ----------------------------------------------------------------------------
-- DROP TABLE IF EXISTS `tb_daily_blacklist`;
-- ============================================================================
