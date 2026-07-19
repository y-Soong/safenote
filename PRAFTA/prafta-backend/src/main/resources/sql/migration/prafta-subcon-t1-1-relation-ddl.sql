-- ============================================================================
-- PRAFTA-SUBCON-T1-1 — 회사 간 연동 관계 테이블(tb_cmpny_relation / _hist) DDL
-- 작성일: 2026-07-13
-- 적용 환경: MySQL 8.0.42
-- 출처: .claude/requests/web_requests/PRAFTA-SUBCON.md §2.1 (2026-07-12 갱신 계약),
--       PRAFTA-SUBCON-T1.plan.md §3 + §9 결정(Q1: PROCESS_CMPNY_CD/ACTION_CMPNY_CD,
--       Q3: ACTIVE_PAIR_KEY 파생 컬럼 + UNIQUE 백스톱)
--
-- 목적:
--   회사(테넌트) 간 양방향 연동 관계의 수립/해지 상태 머신 저장소.
--   활성 관계(REQUESTED/ACCEPTED)는 회사 쌍당 1건(방향 불문)을 서비스 레벨 + DB UNIQUE 이중 강제.
--
-- 설계:
--   - RELATION_ID/HIST_ID = bigint AUTO_INCREMENT (FNC_CMM_SEQ_NEXTVAL 은 회사 단위 채번이라
--     회사 쌍을 잇는 관계 테이블에 부적합 — 하우스 선례: POLICY_SEQ, SMS_ID).
--   - PROCESS_CMPNY_CD / ACTION_CMPNY_CD: 행위자 소속 회사(Q1 승인 — USER_CD 가 회사 스코프
--     식별자라 소속 회사 없이는 감사 추적 불성립). 값은 서버가 토큰 gv_cmpnyCd 에서만 주입.
--   - ACTIVE_PAIR_KEY: 활성 상태(REQUESTED/ACCEPTED)일 때만 정렬된 회사쌍 문자열, 그 외 NULL.
--     STORED 생성 컬럼 + UNIQUE — 동시 요청(A→B, B→A) 레이스의 DB 백스톱(Q3 확정).
--     UNIQUE 는 NULL 다중 허용이라 이력행(REJECTED/CANCELLED/TERMINATED)은 무제약.
--     (활성 한정 UNIQUE 하우스 선례: tb_daily_blacklist.UX_TB_DAILY_BLACKLIST_MBL 함수형 인덱스)
--   - HIST 는 불변 이력이라 UPDATE 감사 컬럼 생략(하우스 선례: tb_attd_std_time_rule_his).
--   - ACTION_TYPE 은 SYS 그룹 미승격 — 리터럴 + COMMENT 나열(plan §3 확정).
--   - charset/collation = utf8mb4 / utf8mb4_unicode_ci, ENGINE=InnoDB (하우스 표준).
--   - CMPNY_CD 폭 = varchar(50) (tb_cmpny.CMPNY_CD 실측 정합). ACTIVE_PAIR_KEY = 50+1+50=101.
--
-- 적용 전 부재 확인(운영 적용 직전 권장):
--   SHOW TABLES LIKE 'tb_cmpny_relation%';   -- 0건이어야 함(이미 있으면 건너뛸 것).
--
-- 멱등성: CREATE TABLE 재실행 시 이미 존재하면 에러 → 반영된 환경에서는 건너뛸 것.
-- 운영 적용: 사용자 수동(read-only MCP). 본 파일은 작성만, DB 직접 적용 금지.
-- ============================================================================

CREATE TABLE `tb_cmpny_relation` (
    `RELATION_ID`      bigint       NOT NULL AUTO_INCREMENT COMMENT '관계ID(PK)',
    `REQ_CMPNY_CD`     varchar(50)  COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '요청측 회사코드',
    `TGT_CMPNY_CD`     varchar(50)  COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '상대측 회사코드',
    `STATUS`           varchar(20)  COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'REQUESTED'
        COMMENT '관계 상태[SYS076] REQUESTED:요청중, ACCEPTED:수락(연동중), REJECTED:거부, CANCELLED:요청취소, TERMINATED:해지',
    `REQ_USER_CD`      varchar(50)  COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '요청자 사용자코드(요청측 소속)',
    `PROCESS_CMPNY_CD` varchar(50)  COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '처리자 소속 회사코드(수락/거부/취소/해지 — 토큰 gv_cmpnyCd 주입)',
    `PROCESS_USER_CD`  varchar(50)  COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '처리자 사용자코드(수락/거부/취소/해지)',
    `PROCESS_DTIME`    datetime     DEFAULT NULL COMMENT '처리일시',
    `PROCESS_COMMENT`  varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '처리 코멘트(거부 사유 등)',
    -- 활성 상태 한정 정렬 회사쌍 키(동시 요청 레이스 DB 백스톱 — Q3). 비활성 전이 시 NULL 로 자동 소거.
    `ACTIVE_PAIR_KEY`  varchar(101) COLLATE utf8mb4_unicode_ci
        GENERATED ALWAYS AS (
            CASE WHEN `STATUS` IN ('REQUESTED', 'ACCEPTED')
                 THEN CONCAT(LEAST(`REQ_CMPNY_CD`, `TGT_CMPNY_CD`), '|', GREATEST(`REQ_CMPNY_CD`, `TGT_CMPNY_CD`))
                 ELSE NULL END
        ) STORED COMMENT '활성 관계쌍 키(REQUESTED/ACCEPTED 만 값, 그 외 NULL — UNIQUE 백스톱)',
    `INSERT_NO`        varchar(50)  COLLATE utf8mb4_unicode_ci DEFAULT 'SYSTEM' COMMENT '입력자(USER_CD)',
    `INSERT_DATE`      datetime     DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
    `UPDATE_NO`        varchar(50)  COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '수정자(USER_CD)',
    `UPDATE_DATE`      datetime     DEFAULT NULL COMMENT '수정일시',
    PRIMARY KEY (`RELATION_ID`),
    -- 활성 관계 쌍당 1건(방향 불문) — NULL 다중 허용으로 종결 이력행은 무제약.
    UNIQUE KEY `UX_CMPNY_RELATION_ACTIVE_PAIR` (`ACTIVE_PAIR_KEY`),
    KEY `IX_CMPNY_RELATION_REQ` (`REQ_CMPNY_CD`, `STATUS`),
    KEY `IX_CMPNY_RELATION_TGT` (`TGT_CMPNY_CD`, `STATUS`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='회사 간 연동 관계';

CREATE TABLE `tb_cmpny_relation_hist` (
    `HIST_ID`         bigint       NOT NULL AUTO_INCREMENT COMMENT '이력ID(PK)',
    `RELATION_ID`     bigint       NOT NULL COMMENT '관계ID(tb_cmpny_relation)',
    `ACTION_TYPE`     varchar(20)  COLLATE utf8mb4_unicode_ci NOT NULL
        COMMENT '액션 유형 REQUEST:요청, ACCEPT:수락, REJECT:거부, CANCEL:취소, TERMINATE:해지',
    `ACTION_CMPNY_CD` varchar(50)  COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '행위자 소속 회사코드(토큰 gv_cmpnyCd 주입 — Q1 승인)',
    `ACTION_USER_CD`  varchar(50)  COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '행위자 사용자코드',
    `ACTION_DTIME`    datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '행위일시',
    `ACTION_DESC`     varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '설명(거부 사유 등)',
    `INSERT_NO`       varchar(50)  COLLATE utf8mb4_unicode_ci DEFAULT 'SYSTEM' COMMENT '입력자(USER_CD)',
    `INSERT_DATE`     datetime     DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
    PRIMARY KEY (`HIST_ID`),
    KEY `IX_CMPNY_RELATION_HIST_REL` (`RELATION_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='회사 간 연동 관계 이력';

-- ============================================================================
-- 롤백 (필요 시 수동 실행)
-- ----------------------------------------------------------------------------
-- DROP TABLE IF EXISTS `tb_cmpny_relation_hist`;
-- DROP TABLE IF EXISTS `tb_cmpny_relation`;
-- ============================================================================
