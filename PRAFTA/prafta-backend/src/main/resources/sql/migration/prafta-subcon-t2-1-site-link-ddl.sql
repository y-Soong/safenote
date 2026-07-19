-- ============================================================================
-- PRAFTA-SUBCON-T2-1 — 사업장 연동 링크 테이블(tb_site_link) DDL + LINK_SRC 컬럼 ALTER
-- 작성일: 2026-07-13
-- 적용 환경: MySQL 8.0.42
-- 출처: PRAFTA-SUBCON.md §2.1(TB_SITE_LINK 계약)·§2.2(LINK_SRC 컬럼 계약),
--       PRAFTA-SUBCON-T2.plan.md §3 DDL 초안(메인 세션 실측 확정값 반영)
-- 선행: prafta-subcon-t1-1-relation-ddl.sql (tb_cmpny_relation — RELATION_ID 참조)
--
-- 목적:
--   회사 간 사업장 연동(미러) 링크의 제안/수락/해지 상태 머신 저장소 +
--   tb_site / tb_sch_mgmt 에 연동 출처(LINK_SRC_*) 컬럼 추가(NULL=일반, NOT NULL=미러=잠금).
--
-- 설계(plan §3 그대로):
--   - LINK_ID = bigint AUTO_INCREMENT (T1 관계 패턴 승계 — 회사 쌍을 잇는 테이블이라
--     회사 단위 시퀀스 FNC_CMM_SEQ_NEXTVAL 부적합).
--   - DST_SITE_CD: 수락 시 채번(PROPOSED 동안 NULL).
--   - ACTIVE_LINK_KEY: 활성(PROPOSED/ACTIVE) 한정 같은 (제공 사업장 → 수신 회사) 1건
--     UNIQUE 백스톱(T1 Q3 패턴 승계). 종결행(REJECTED/CANCELLED/TERMINATED)은 NULL 무제약.
--   - 거부/취소/해지 후 재제안 = 새 LINK_ID 행(기존 행 재활용 금지 — 링크 목록이 곧 이력, plan D2).
--   - 상태 코드 그룹 = SYS079 (메인 세션 실측 확정 — SYS077/078 은 T3 예약).
--   - LINK_SRC_SCH_CD 는 원본 사이트 스코프 코드 — 반드시 미러 사이트의
--     tb_site.LINK_SRC_SITE_CD 경유로만 해석할 것(SCH_CD 는 사이트별 시퀀스라 (CMPNY, SCH_CD)만으로 모호).
--
-- 적용 전 부재 확인(운영 적용 직전 권장):
--   SHOW TABLES LIKE 'tb_site_link';                                -- 0건이어야 함
--   SHOW COLUMNS FROM tb_site LIKE 'LINK_SRC_%';                    -- 0건이어야 함
--   SHOW COLUMNS FROM tb_sch_mgmt LIKE 'LINK_SRC_%';                -- 0건이어야 함
--
-- 멱등성: CREATE TABLE / ADD COLUMN 재실행 시 에러 → 이미 반영된 환경에서는 건너뛸 것.
-- 운영 적용: 사용자 수동(read-only MCP). 본 파일은 작성만, DB 직접 적용 금지.
-- ============================================================================

-- ── (1) 사업장 연동 링크 ──
-- 상태 전이: PROPOSED→ACTIVE|REJECTED, PROPOSED→CANCELLED(제안측), ACTIVE→TERMINATED(양측).
CREATE TABLE `tb_site_link` (
    `LINK_ID`          bigint       NOT NULL AUTO_INCREMENT COMMENT '링크ID(PK)',
    `RELATION_ID`      bigint       NOT NULL COMMENT '회사 관계ID(tb_cmpny_relation — ACCEPTED 관계 전제)',
    `SRC_CMPNY_CD`     varchar(50)  COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '제공측 회사코드(원본 또는 상위 미러 소유사)',
    `SRC_SITE_CD`      varchar(50)  COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '제공측 사업장코드',
    `DST_CMPNY_CD`     varchar(50)  COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '수신측 회사코드',
    `DST_SITE_CD`      varchar(50)  COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '수신측 미러 사업장코드(수락 시 채번 — PROPOSED 동안 NULL)',
    `STATUS`           varchar(20)  COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PROPOSED'
        COMMENT '링크 상태[SYS079] PROPOSED:제안중, ACTIVE:연동중, REJECTED:거부, CANCELLED:제안취소, TERMINATED:해지(독립화)',
    `PROPOSE_USER_CD`  varchar(50)  COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '제안자 사용자코드(제공측 소속)',
    `PROCESS_CMPNY_CD` varchar(50)  COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '처리자 소속 회사코드(수락/거부/취소/해지 — 토큰 gv_cmpnyCd 주입, T1 Q1 승계. 관계 해지 자동처리 시 NULL)',
    `PROCESS_USER_CD`  varchar(50)  COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '처리자 사용자코드',
    `PROCESS_DTIME`    datetime     DEFAULT NULL COMMENT '처리일시',
    `PROCESS_COMMENT`  varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '처리 코멘트(거부 사유 등)',
    -- 활성(PROPOSED/ACTIVE) 한정: 같은 제공 사업장 → 같은 수신 회사 링크 1건 백스톱(T1 Q3 패턴 승계).
    `ACTIVE_LINK_KEY`  varchar(160) COLLATE utf8mb4_unicode_ci
        GENERATED ALWAYS AS (
            CASE WHEN `STATUS` IN ('PROPOSED', 'ACTIVE')
                 THEN CONCAT(`SRC_CMPNY_CD`, '|', `SRC_SITE_CD`, '|', `DST_CMPNY_CD`)
                 ELSE NULL END
        ) STORED COMMENT '활성 링크 키(PROPOSED/ACTIVE 만 값 — UNIQUE 백스톱, 종결행 무제약)',
    `INSERT_NO`        varchar(50)  COLLATE utf8mb4_unicode_ci DEFAULT 'SYSTEM' COMMENT '입력자(USER_CD)',
    `INSERT_DATE`      datetime     DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
    `UPDATE_NO`        varchar(50)  COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '수정자(USER_CD)',
    `UPDATE_DATE`      datetime     DEFAULT NULL COMMENT '수정일시',
    PRIMARY KEY (`LINK_ID`),
    UNIQUE KEY `UX_SITE_LINK_ACTIVE` (`ACTIVE_LINK_KEY`),
    KEY `IX_SITE_LINK_SRC` (`SRC_CMPNY_CD`, `SRC_SITE_CD`, `STATUS`),
    KEY `IX_SITE_LINK_DST` (`DST_CMPNY_CD`, `DST_SITE_CD`, `STATUS`),
    KEY `IX_SITE_LINK_REL` (`RELATION_ID`, `STATUS`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='회사 간 사업장 연동 링크';

-- ── (2) 기존 테이블 출처 컬럼(마스터 §2.2 계약 — NULL=일반, NOT NULL=미러=잠금) ──
ALTER TABLE `tb_site`
    ADD COLUMN `LINK_SRC_CMPNY_CD` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '연동 원본(직상위) 회사코드 — NULL=일반, NOT NULL=미러(수정 잠금)',
    ADD COLUMN `LINK_SRC_SITE_CD`  varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '연동 원본(직상위) 사업장코드',
    ADD KEY `IX_TB_SITE_LINK_SRC` (`LINK_SRC_CMPNY_CD`, `LINK_SRC_SITE_CD`);

ALTER TABLE `tb_sch_mgmt`
    ADD COLUMN `LINK_SRC_CMPNY_CD` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '연동 원본(직상위) 회사코드 — NULL=일반, NOT NULL=미러(수정 잠금)',
    ADD COLUMN `LINK_SRC_SCH_CD`   varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '연동 원본(직상위) 근무타입코드(원본 사이트 스코프 — 미러 사이트의 LINK_SRC_SITE_CD 경유로만 해석)',
    ADD KEY `IX_TB_SCH_MGMT_LINK_SRC` (`LINK_SRC_CMPNY_CD`, `LINK_SRC_SCH_CD`);

-- ============================================================================
-- 롤백 (필요 시 수동 실행)
-- ----------------------------------------------------------------------------
-- ALTER TABLE `tb_sch_mgmt` DROP KEY `IX_TB_SCH_MGMT_LINK_SRC`,
--     DROP COLUMN `LINK_SRC_CMPNY_CD`, DROP COLUMN `LINK_SRC_SCH_CD`;
-- ALTER TABLE `tb_site` DROP KEY `IX_TB_SITE_LINK_SRC`,
--     DROP COLUMN `LINK_SRC_CMPNY_CD`, DROP COLUMN `LINK_SRC_SITE_CD`;
-- DROP TABLE IF EXISTS `tb_site_link`;
-- ============================================================================
