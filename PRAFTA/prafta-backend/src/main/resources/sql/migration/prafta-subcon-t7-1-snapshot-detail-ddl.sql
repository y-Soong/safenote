-- ============================================================================
-- PRAFTA-SUBCON-T7-1 — 위험성평가(RISK)/아차사고(NEARMISS) 공유 스냅샷 상세행 DDL
-- 작성일: 2026-07-14
-- 적용 환경: MySQL 8.0.42
-- 출처: PRAFTA-SUBCON.md §2.1 + PRAFTA-SUBCON-T7.plan.md §0-3(계약확장 C1)·§3-1·§1-1 실측
--       + 메인 세션 확정(Q1 성명 평문 저장 / Q4 전 파일타입 복제)
-- 선행: prafta-subcon-t3-1-share-ddl.sql (헤더 tb_cmpny_share_snapshot 존재 전제)
--
-- ★ 성명 저장 규약(메인 세션 Q1 확정 — plan §0-2 결정 1 을 뒤집음):
--   T3 실제 컬럼 tb_cmpny_share_snapshot_attd.WORKER_NM 은 평문 varchar(50) 다(암호화 아님).
--   본 T7 스냅샷의 작성자/제보자 성명도 동일하게 **평문 varchar(50)** 로 보관한다.
--   원천(tb_user.USER_NM)이 평문이라 정합하며, 보호는 접근통제(master/system 메뉴 권한 +
--   OWNER_CMPNY_CD 테넌트 스코프)로 한다. AES-GCM 암호화/복호화 컬럼(_ENC)을 두지 않는다.
--
-- 명칭 스냅샷 원칙(plan D3):
--   상세행에는 코드가 아니라 제공사 스코프에서 해석한 명칭을 저장한다(수신사가 제공사 코드
--   테이블을 갖지 않는다). 첨부는 복제 후 수신사 소유 FILE_MGMT_CD(NULL=없음/복제실패).
--   원본 USER_CD/회사코드/타사 조인 금지.
--
-- 적용 전 부재 확인(운영 적용 직전 권장):
--   SHOW TABLES LIKE 'tb_cmpny_share_snapshot_risk%';    -- 0건이어야 함
--   SHOW TABLES LIKE 'tb_cmpny_share_snapshot_nearmiss';  -- 0건이어야 함
-- 멱등성: CREATE TABLE 재실행 시 에러 → 이미 반영된 환경에서는 건너뛸 것.
-- 운영 적용: 사용자 수동(read-only MCP). 본 파일은 작성만, DB 직접 적용 금지.
-- ============================================================================

-- ── (1) 위험성평가 스냅샷 상세행(평가 1건 = 1행). 시점 고정 복사.
CREATE TABLE `tb_cmpny_share_snapshot_risk` (
    `DETAIL_ID`             bigint       NOT NULL AUTO_INCREMENT COMMENT '상세행ID(PK)',
    `SNAPSHOT_ID`           bigint       NOT NULL COMMENT '스냅샷ID(tb_cmpny_share_snapshot)',
    `ROW_SEQ`               int          NOT NULL COMMENT '스냅샷 내 표시 순번',
    `AFFIL_CMPNY_NM`        varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '소속표시(회사명 스냅샷). 릴레이 복사분은 제공사로 relabel',
    `ASSESSOR_SEQ`          int          NOT NULL DEFAULT 0 COMMENT '작성자 로컬 일련번호(동일 INIT 작성자=동일 번호. 원본 USER_CD 미반출 — 그룹핑 전용)',
    `PROCESS_NM`            varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '공정/작업 명칭 스냅샷(PROCESS_CD 해석값 — 코드 아님)',
    `RISK_TYPE_NM`          varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '위험분류 명칭 스냅샷',
    `HAZARD_NM`             varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '유해위험요인 명칭 스냅샷(self 는 ASSESSMENT_DESC)',
    `ASSESSMENT_DESC`       varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '평가 설명',
    `ASSESSMENT_STATUS_NM`  varchar(50)  COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '진행상태 명칭 스냅샷(SYS011 해석값)',
    `INIT_ASSESSOR_NM`      varchar(50)  COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '초기평가 작성자 성명(원천 평문 varchar(50) — 동일 형태 보관. 보호는 접근통제)',
    `INIT_LIKELIHOOD`       int          DEFAULT NULL COMMENT '초기 가능성 점수',
    `INIT_SEVERITY`         int          DEFAULT NULL COMMENT '초기 중대성 점수',
    `INIT_RISK_LV`          varchar(10)  COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '초기 위험도',
    `INIT_DESC`             varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '초기 현재 안전조치/설명',
    `INIT_ASSESS_DATE`      char(8)      COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '초기평가일(YYYYMMDD — 원본 datetime 절삭)',
    `INIT_FILE_MGMT_CD`     varchar(50)  COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '초기평가 첨부(복제된 수신사 소유 FILE_MGMT_CD. NULL=없음/복제실패)',
    `REVAL_ASSESSOR_NM`     varchar(50)  COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '재평가 작성자 성명 평문(재평가 없거나 미동의 마스킹 시 NULL)',
    `REVAL_LIKELIHOOD`      int          DEFAULT NULL COMMENT '재평가 가능성 점수',
    `REVAL_SEVERITY`        int          DEFAULT NULL COMMENT '재평가 중대성 점수',
    `REVAL_RISK_LV`         varchar(10)  COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '재평가 위험도',
    `REVAL_DESC`            varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '재평가 설명',
    `REVAL_ASSESS_DATE`     char(8)      COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '재평가일(YYYYMMDD)',
    `REVAL_FILE_MGMT_CD`    varchar(50)  COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '재평가 첨부(복제된 수신사 소유 FILE_MGMT_CD)',
    `INSERT_NO`             varchar(50)  COLLATE utf8mb4_unicode_ci DEFAULT 'SYSTEM' COMMENT '입력자',
    `INSERT_DATE`           datetime     DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
    `UPDATE_NO`             varchar(50)  COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '수정자(불변 — 미사용)',
    `UPDATE_DATE`           datetime     DEFAULT NULL COMMENT '수정일시(불변 — 미사용)',
    PRIMARY KEY (`DETAIL_ID`),
    KEY `IX_SNAP_RISK_LIST` (`SNAPSHOT_ID`, `ROW_SEQ`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='위험성평가 공유 스냅샷 상세행(시점 고정 복사)';

-- ── (2) 위험성평가 스냅샷 개선항목 자식행(1:N — 계약확장 C1)
CREATE TABLE `tb_cmpny_share_snapshot_risk_improve` (
    `IMPROVE_ID`     bigint       NOT NULL AUTO_INCREMENT COMMENT '개선항목행ID(PK)',
    `DETAIL_ID`      bigint       NOT NULL COMMENT '부모 위험성평가 상세행ID',
    `SNAPSHOT_ID`    bigint       NOT NULL COMMENT '스냅샷ID(스코프 강제 비정규화)',
    `IMPROVE_SEQ`    int          NOT NULL COMMENT '개선항목 순번(표시용)',
    `IMPROVE_DATE`   char(8)      COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '개선예정/완료일(YYYYMMDD)',
    `IMPROVE_DESC`   varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '개선대책 설명',
    `LIKELIHOOD`     int          DEFAULT NULL COMMENT '개선 후 가능성',
    `SEVERITY`       int          DEFAULT NULL COMMENT '개선 후 중대성',
    `RISK_LV`        varchar(10)  COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '개선 후 위험도',
    `FILE_MGMT_CD`   varchar(50)  COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '개선항목 첨부(복제된 수신사 소유 FILE_MGMT_CD)',
    `INSERT_NO`      varchar(50)  COLLATE utf8mb4_unicode_ci DEFAULT 'SYSTEM' COMMENT '입력자',
    `INSERT_DATE`    datetime     DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
    PRIMARY KEY (`IMPROVE_ID`),
    KEY `IX_SNAP_RISK_IMP_DETAIL` (`DETAIL_ID`, `IMPROVE_SEQ`),
    KEY `IX_SNAP_RISK_IMP_SNAP` (`SNAPSHOT_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='위험성평가 스냅샷 개선항목 자식행';

-- ── (3) 아차사고 스냅샷 상세행(사고 1건 = 1행)
CREATE TABLE `tb_cmpny_share_snapshot_nearmiss` (
    `DETAIL_ID`              bigint       NOT NULL AUTO_INCREMENT COMMENT '상세행ID(PK)',
    `SNAPSHOT_ID`            bigint       NOT NULL COMMENT '스냅샷ID',
    `ROW_SEQ`                int          NOT NULL COMMENT '스냅샷 내 표시 순번',
    `AFFIL_CMPNY_NM`         varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '소속표시(회사명 스냅샷). 릴레이 복사분은 제공사로 relabel',
    `REPORTER_SEQ`           int          NOT NULL DEFAULT 0 COMMENT '제보자 로컬 일련번호(원본 USER_CD 미반출)',
    `REPORTER_NM`            varchar(50)  COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '제보자 성명 평문(원천 평문 varchar(50) — 동일 형태 보관. 보호는 접근통제)',
    `OCCUR_DTIME`            datetime     DEFAULT NULL COMMENT '발생일시(스냅샷 복사)',
    `PROCESS_NM`             varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '공정/작업 명칭 스냅샷',
    `LOCATION_DESC`          varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '발생 장소',
    `DESCRIPTION`            varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '사고 내용',
    `POTENTIAL_SEVERITY_NM`  varchar(50)  COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '잠재 중대성 명칭 스냅샷(SYS062 해석값)',
    `IMMEDIATE_ACTION_DESC`  varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '즉시 조치',
    `ADMIN_TEMP_ACTION_DESC` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '관리자 임시 조치',
    `CAUSE_DESC`             varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '원인',
    `PREVENTION_DESC`        varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '재발방지 대책',
    `REPORT_STATUS_NM`       varchar(50)  COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '진행상태 명칭 스냅샷(SYS063 해석값)',
    `FILE_MGMT_CD`           varchar(50)  COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '사진 첨부(복제된 수신사 소유 FILE_MGMT_CD)',
    `INSERT_NO`              varchar(50)  COLLATE utf8mb4_unicode_ci DEFAULT 'SYSTEM' COMMENT '입력자',
    `INSERT_DATE`            datetime     DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
    `UPDATE_NO`              varchar(50)  COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '수정자(불변 — 미사용)',
    `UPDATE_DATE`            datetime     DEFAULT NULL COMMENT '수정일시(불변 — 미사용)',
    PRIMARY KEY (`DETAIL_ID`),
    KEY `IX_SNAP_NEARMISS_LIST` (`SNAPSHOT_ID`, `ROW_SEQ`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='아차사고 공유 스냅샷 상세행(시점 고정 복사)';

-- ============================================================================
-- 롤백 (필요 시 수동 실행)
-- ----------------------------------------------------------------------------
-- DROP TABLE IF EXISTS `tb_cmpny_share_snapshot_risk_improve`;
-- DROP TABLE IF EXISTS `tb_cmpny_share_snapshot_risk`;
-- DROP TABLE IF EXISTS `tb_cmpny_share_snapshot_nearmiss`;
-- ============================================================================
