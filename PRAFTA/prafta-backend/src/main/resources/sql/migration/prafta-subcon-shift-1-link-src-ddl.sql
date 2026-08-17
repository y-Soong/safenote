-- ============================================================================
-- SHIFT-LINK-T1 — tb_shift_sch_mgmt 에 연동 출처(LINK_SRC_*) 컬럼 추가 DDL
-- 작성일: 2026-08-17
-- 적용 환경: MySQL 8 (개발·운영 **양쪽 모두 적용** — 한쪽만 적용 시 장애 반복 실증)
-- 출처: 작업지시서_교대근무타입-사업장연동-복제전파 §2.1-3·§3.1,
--       선례 = prafta-subcon-t2-1-site-link-ddl.sql (tb_site / tb_sch_mgmt ALTER 구성 동형)
-- 선행: prafta-subcon-t2-1-site-link-ddl.sql (tb_site_link + tb_site/tb_sch_mgmt LINK_SRC)
--
-- 목적:
--   교대근무 타입 정의(TB_SHIFT_SCH_MGMT)를 사업장 연동 3단 체계(복제·전파·독립화)에
--   편입하기 위한 출처 표식 컬럼 추가(NULL=일반, NOT NULL=미러=정의 잠금).
--
-- 설계(지시서 §2.1-3 그대로):
--   - LINK_SRC 표식은 부모 테이블(TB_SHIFT_SCH_MGMT)에만 둔다.
--     하위 3테이블(PTRN/TEAM_META/ASSIGN)은 부모 (CMPNY_CD, SITE_CD, SHIFT_CD)로
--     미러 여부를 판정할 수 있으므로 컬럼을 늘리지 않는다(TB_SCH_MGMT 선례와 동형).
--   - LINK_SRC_SHIFT_CD 는 원본 사이트 스코프 코드 — SHIFT_CD 채번이
--     FNC_CMM_SEQ_NEXTVAL(cmpny, CONCAT('SHIFT_CD-', siteCd)) 사이트별 시퀀스라
--     (CMPNY, SHIFT_CD)만으로는 모호하다. 반드시 미러 사이트의
--     tb_site.LINK_SRC_SITE_CD 경유로만 해석할 것(T2 DDL 헤더 주석 선례와 동형).
--
-- 적용 전 부재 확인(운영 적용 직전 권장):
--   SHOW COLUMNS FROM tb_shift_sch_mgmt LIKE 'LINK_SRC_%';   -- 0건이어야 함
--
-- 멱등성: ADD COLUMN 재실행 시 에러 → 이미 반영된 환경에서는 건너뛸 것.
-- 운영 적용: 사용자 수동(Workbench). 본 파일은 작성만, DB 직접 적용 금지.
-- 적용 순서: 본 DDL 을 코드 배포 **전에** 선적용(LINK_SRC 참조 코드가 배포되면
--   컬럼 부재 시 SQL 에러 — plan §3 배포 순서).
-- ============================================================================

ALTER TABLE `tb_shift_sch_mgmt`
    ADD COLUMN `LINK_SRC_CMPNY_CD` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '연동 원본(직상위) 회사코드 — NULL=일반, NOT NULL=미러(정의 잠금)',
    ADD COLUMN `LINK_SRC_SHIFT_CD` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '연동 원본(직상위) 교대타입코드(원본 사이트 스코프 — 미러 사이트의 LINK_SRC_SITE_CD 경유로만 해석)',
    ADD KEY `IX_TB_SHIFT_SCH_MGMT_LINK_SRC` (`LINK_SRC_CMPNY_CD`, `LINK_SRC_SHIFT_CD`);

-- ============================================================================
-- 롤백 (필요 시 수동 실행)
-- ----------------------------------------------------------------------------
-- ALTER TABLE `tb_shift_sch_mgmt` DROP KEY `IX_TB_SHIFT_SCH_MGMT_LINK_SRC`,
--     DROP COLUMN `LINK_SRC_CMPNY_CD`, DROP COLUMN `LINK_SRC_SHIFT_CD`;
-- ============================================================================
