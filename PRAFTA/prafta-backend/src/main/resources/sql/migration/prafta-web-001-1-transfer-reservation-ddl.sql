-- ============================================================================
-- PRAFTA-WEB_001-1 — 사용자 소속이동 예약 테이블(tb_user_transfer_reservation) DDL
-- 작성일: 2026-06-29
-- 적용 환경: MySQL 8.0.42
-- 출처: .claude/requests/web_requests/PRAFTA-WEB_001-plan.md §3.2 (Terminal A)
--
-- 목적:
--   master/hr 가 다른 사용자를 "소속이동"(사업장/부서/기본근무 변경) 예약한다.
--   등록 시점에는 예약 레코드만 생성하고(STATUS='RESERVED'),
--   실제 발효(tb_user.SITE_CD/NODE_CD/DEFAULT_SCH_CD 변경 등)는 발효일 자정 스케줄러가 처리한다(Terminal B).
--
-- 설계:
--   - 스코프 = 회사(CMPNY_CD). PK = (CMPNY_CD, RESERVATION_ID) — 채번 시퀀스가 회사별이라 회사 간 ID 충돌 방지.
--   - 채번 = 'TR' + YYYYMMDD + FNC_CMM_SEQ_NEXTVAL(CMPNY_CD,'USER_TRANSFER') (tb_daily_blacklist 'B' 패턴 동형).
--   - 날짜 YYYYMMDD = varchar(8) (tb_user_* 관례). 일시 = datetime.
--   - 동일 사용자 활성 예약(RESERVED) 중복 방지: 활성 한정 함수형 UNIQUE
--     (tb_daily_user UX_TB_DAILY_USER_MBL / tb_daily_blacklist UX_TB_DAILY_BLACKLIST_MBL 패턴).
--     예약 취소/발효 시 STATUS 가 RESERVED 가 아니게 되어 동일 사용자 재예약 허용.
--   - charset/collation = utf8mb4 / utf8mb4_unicode_ci (tb_user 정합).
--
-- 적용 전 부재 확인(운영 적용 직전 권장):
--   SHOW TABLES LIKE 'tb_user_transfer_reservation';   -- 0건이어야 함(이미 있으면 CREATE 건너뛸 것).
--
-- 멱등성: CREATE TABLE 재실행 시 이미 존재하면 에러 → 반영된 환경에서는 건너뛸 것.
-- 운영 적용: 사용자 수동(read-only MCP). 본 파일은 작성만, DB 직접 적용 금지.
-- ============================================================================

CREATE TABLE `tb_user_transfer_reservation` (
    `CMPNY_CD`          varchar(50)  COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '회사 코드',
    `RESERVATION_ID`    varchar(20)  COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '소속이동 예약 ID(채번: TR+YYYYMMDD+SEQ)',
    `USER_CD`           varchar(20)  COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '대상 사용자 코드',
    `FROM_SITE_CD`      varchar(50)  COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '이동 전 사업장(등록시점 스냅샷)',
    `FROM_NODE_CD`      varchar(50)  COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '이동 전 소속부서(등록시점 스냅샷)',
    `TO_SITE_CD`        varchar(50)  COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '이동 사업장',
    `TO_NODE_CD`        varchar(50)  COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '이동 소속부서(NODE)',
    `TO_DEFAULT_SCH_CD` varchar(20)  COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '기본 근무타입(정규직만, 일용직 NULL)',
    `MOVE_DATE`         varchar(8)   COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '소속이동일(YYYYMMDD, 내일 이후)',
    `MOVE_REASON`       varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '소속이동 사유',
    `EMPLOYMENT_TYPE`   varchar(20)  COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '등록시점 고용형태 스냅샷(SYS041, DAILY=일용직)',
    `STATUS`            varchar(20)  COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'RESERVED' COMMENT 'RESERVED/EXECUTED/CANCELLED/FAILED',
    `NOTICE_ACK_YN`     varchar(1)   COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'N' COMMENT '대상자 안내 확인 여부',
    `NOTICE_ACK_DATE`   datetime     DEFAULT NULL COMMENT '안내 확인 일시',
    `EXECUTED_DATE`     datetime     DEFAULT NULL COMMENT '발효(실행) 일시(Terminal B)',
    `FAIL_REASON`       varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '발효 실패 사유(재검증 실패 등, Terminal B)',
    `DEL_YN`            varchar(1)   COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'N' COMMENT '삭제 여부',
    `INSERT_NO`         varchar(50)  COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '등록자(실행 master/hr USER_CD)',
    `INSERT_DATE`       datetime     DEFAULT CURRENT_TIMESTAMP COMMENT '등록 일시',
    `UPDATE_NO`         varchar(50)  COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '수정자(USER_CD)',
    `UPDATE_DATE`       datetime     DEFAULT NULL COMMENT '수정 일시',
    PRIMARY KEY (`CMPNY_CD`,`RESERVATION_ID`),
    -- 동일 사용자 활성 예약(STATUS='RESERVED') 한정 UNIQUE — 취소/발효 후 동일 사용자 재예약 허용.
    UNIQUE KEY `UX_TB_USER_TRANSFER_ACTIVE` (`CMPNY_CD`,(if((`STATUS` = _utf8mb4'RESERVED'),`USER_CD`,NULL))),
    -- 발효 스케줄러 룩업(회사+상태+이동일) 가속.
    KEY `IX_TB_USER_TRANSFER_EXEC` (`CMPNY_CD`,`STATUS`,`MOVE_DATE`),
    -- 대상자 본인 안내(notice) 조회 가속.
    KEY `IX_TB_USER_TRANSFER_USER` (`CMPNY_CD`,`USER_CD`,`STATUS`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='사용자 소속이동 예약';

-- ============================================================================
-- 롤백 (필요 시 수동 실행)
-- ----------------------------------------------------------------------------
-- DROP TABLE IF EXISTS `tb_user_transfer_reservation`;
-- ============================================================================
