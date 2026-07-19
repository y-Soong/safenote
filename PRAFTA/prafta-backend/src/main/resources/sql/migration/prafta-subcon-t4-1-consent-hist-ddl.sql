-- ============================================================================
-- PRAFTA-SUBCON-T4-1 — 약관 동의 이력 테이블 신설 (tb_terms_user_agr_hist)
-- 작성일: 2026-07-14
-- 적용 환경: MySQL 8.0.42
-- 출처: PRAFTA-SUBCON.md §1-7 + PRAFTA-SUBCON-T4.md §3·§5-3 + T4.plan §0-2(확정 2)·§3-1
--
-- 배경:
--   tb_terms_user_agr_mgmt 는 PK(CMPNY_CD, USER_CD, TERMS_ID, TERMS_VERSION) 의 "현재 상태"
--   테이블이며 UPDATE_* 컬럼조차 없어 동의→철회 전이가 덮어써진다.
--   제3자 제공 동의(006)는 "철회"가 정상 시나리오이므로 전이 이력이 법적 근거로 필수다.
--
-- 설계:
--   - append-only. 서비스/매퍼에 본 테이블의 UPDATE/DELETE 구문을 단 하나도 만들지 않는다(plan D8).
--   - 동의/철회 "전이마다 1행". 동일 값 재저장(멱등)은 이력을 남기지 않는다.
--   - 기존 128행 백필 없음(plan D7): 과거 전이 시점을 알 수 없어 백필하면 허위 이력이 된다.
--   - 006 전용이 아니라 "모든 약관"의 전이를 기록한다(필수약관 일괄동의·선택약관 토글 포함).
--
-- ★ 컬럼 길이는 tb_terms_user_agr_mgmt / tb_terms 실측과 100% 일치시켰다:
--     CMPNY_CD varchar(50) / USER_CD varchar(20) / TERMS_ID varchar(3) / TERMS_VERSION varchar(10) / AGR_YN varchar(2)
--   (plan §3-1 초안은 USER_CD 를 varchar(50) 으로 적었으나, 실측 tb_terms_user_agr_mgmt.USER_CD = varchar(20) 이므로 20 으로 확정)
--
-- 적용 전 부재 확인:
--   SHOW TABLES LIKE 'tb_terms_user_agr_hist';   -- 0건이어야 함
--
-- 멱등성: CREATE TABLE 재실행 시 에러 → 이미 반영된 환경에서는 건너뛸 것.
-- 운영 적용: 사용자 수동(MCP read-only). 본 파일은 작성만, DB 직접 적용 금지.
-- ============================================================================

CREATE TABLE `tb_terms_user_agr_hist` (
    `HIST_ID`         bigint       NOT NULL AUTO_INCREMENT COMMENT '동의이력ID(PK)',
    `CMPNY_CD`        varchar(50)  COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '회사코드(동의 주체 소속 — USER_CD 는 회사별 채번이라 단독 식별 불가)',
    `USER_CD`         varchar(20)  COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사용자코드(정규 tb_user 또는 일용직 tb_daily_user)',
    `TERMS_ID`        varchar(3)   COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '약관ID[SYS008] 006:연동 회사 제3자 제공 동의(그 외 약관 전이도 동일 기록)',
    `TERMS_VERSION`   varchar(10)  COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '약관 버전(응답 시점의 tb_terms.TERMS_VERSION — 어떤 문구에 응답했는지의 근거)',
    `BEFORE_AGR_YN`   varchar(2)   COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '전이 전 동의여부[Y:동의/N:미동의/NULL:최초 응답(기록 없음)]',
    `AFTER_AGR_YN`    varchar(2)   COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '전이 후 동의여부[Y:동의/N:미동의(철회 포함)]',
    `AGR_SOURCE`      varchar(10)  COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '응답 경로[GATE:로그인 게이트/MYPAGE:마이페이지 토글/JOIN:가입 시(현재 미기록 — 확장 예약)]',
    `ACTOR_CMPNY_CD`  varchar(50)  COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '변경 주체 소속 회사코드(현재는 본인=CMPNY_CD. 관리자 대행 도입 대비)',
    `ACTOR_USER_CD`   varchar(20)  COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '변경 주체 사용자코드(현재는 본인=USER_CD)',
    `ACTION_DTIME`    datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '전이 일시(서버 NOW() — 클라 시각 불신)',
    `INSERT_NO`       varchar(50)  COLLATE utf8mb4_unicode_ci DEFAULT 'SYSTEM' COMMENT '입력자',
    `INSERT_DATE`     datetime     DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
    PRIMARY KEY (`HIST_ID`),
    KEY `IX_TERMS_AGR_HIST_USER` (`CMPNY_CD`, `USER_CD`, `TERMS_ID`, `HIST_ID`),
    KEY `IX_TERMS_AGR_HIST_TERMS` (`TERMS_ID`, `TERMS_VERSION`, `ACTION_DTIME`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='약관 동의/철회 전이 이력(append-only — UPDATE/DELETE 경로 없음)';

-- ============================================================================
-- 롤백 (필요 시 수동 실행)
-- ----------------------------------------------------------------------------
-- DROP TABLE IF EXISTS `tb_terms_user_agr_hist`;
-- ============================================================================
