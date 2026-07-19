-- ============================================================
-- PRAFTA-SUBCON-T6-AUDIT-01 — 순회점검 결과 덮어쓰기 감사 이력(append-only) DDL
-- 작성일: 2026-07-15 / 적용 환경: MySQL 8.0.42
-- 출처: .claude/requests/web_requests/PRAFTA-SUBCON-T6-AUDIT.plan.md §3-1
--
-- 배경: T6 점검 결과 통합이 후행 덮어쓰기(last-writer-wins)로 전환되어, 하위 티어 저장이
--   상위 티어의 판정/증거사진을 이력 없이 덮어쓰는 결함이 있었다. 순회점검 확인서는 법적 기록이라
--   추적이 필요하다. 덮어쓰기 정책은 유지하되, write 될 때마다 "변경 후 값"을 1행 append 하여
--   좌표별 타임라인으로 "무엇을 무엇으로 덮었는지"를 복원한다.
--
-- append-only: 애플리케이션은 INSERT 만 수행한다(UPDATE/DELETE 매퍼 부재로 규율 보증).
-- 컬럼 타입/콜레이션은 원본(tb_chkpt_inspect_answer / tb_chkpt_defect_action)과 동일하게 맞췄다
--   (prafta-050-chkpt-defect-action.sql / T6 subcon 마이그레이션 실측 대조).
--   CMPNY/SITE/CHKPT_CD varchar(50), INSPECT_ITEM_CD varchar(20), WORK_DATE varchar(8),
--   INSPECT_ANSWER_TYPE varchar(2), *_DESC text, FILE_MGMT_CD varchar(50),
--   *_CMPNY_CD/*_USER_CD varchar(50), *_USER_NM varchar(100).
--
-- 적용 전 부재 확인(이미 반영된 환경이면 CREATE 건너뛸 것):
--   SELECT 1 FROM information_schema.tables WHERE table_name='tb_chkpt_inspect_answer_hist';
--   SELECT 1 FROM information_schema.tables WHERE table_name='tb_chkpt_defect_action_hist';
-- 멱등성: IF NOT EXISTS 로 재실행 안전.
-- backfill 없음: 감사 이력은 시행 시점 이후의 write 부터 축적한다(과거 덮어쓰기는 원본 최신값만
--   남아 소급 복원 불가 — 설계상 수용, 메인 세션 Q4 확정).
-- ============================================================

-- (1) 점검 응답 덮어쓰기 이력 — write 될 때마다 그 좌표의 새 값을 1행 append.
CREATE TABLE IF NOT EXISTS `tb_chkpt_inspect_answer_hist` (
      `HIST_ID`             bigint       NOT NULL AUTO_INCREMENT COMMENT '이력 PK'
    , `CMPNY_CD`            varchar(50)  COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '테넌트(이력 소유 회사)'
    , `SITE_CD`             varchar(50)  COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사업장'
    , `CHKPT_CD`            varchar(50)  COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '점검대상'
    , `INSPECT_ITEM_CD`     varchar(20)  COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '점검문항'
    , `WORK_DATE`           varchar(8)   COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '점검일자 YYYYMMDD'
    , `CHG_TYPE`            varchar(2)   COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '변경유형 01:신규(INSERT), 02:덮어쓰기(UPDATE)'
    , `INSPECT_ANSWER_TYPE` varchar(2)   COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '점검답변타입 스냅샷 Y:양호, N:불량'
    , `ANSWER_DESC`         text         COLLATE utf8mb4_unicode_ci COMMENT '답변 상세 스냅샷'
    , `FILE_MGMT_CD`        varchar(50)  COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '점검 사진 파일관리코드 스냅샷(그 티어 소유 파일)'
    , `PERFORM_CMPNY_CD`    varchar(50)  COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '수행 회사 스냅샷(타 티어는 인접 1차 relabel)'
    , `PERFORM_USER_CD`     varchar(50)  COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '수행자 USER_CD 스냅샷(표시 전용, 조인 금지)'
    , `PERFORM_USER_NM`     varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '수행자 성명 스냅샷'
    , `ACTION_DTIME`        datetime     NOT NULL COMMENT '이 write 발생 시각(서버 NOW, KST 고정) — 타임라인 정렬 기준'
    , `INSERT_NO`           varchar(50)  COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '기록 트리거 주체(자체저장=수행자 USER_CD, 전파=SYSTEM)'
    , `INSERT_DATE`         datetime     NOT NULL COMMENT '이력 적재 시각'
    , PRIMARY KEY (`HIST_ID`)
    , KEY `IX_ANSWER_HIST_FILTER` (`CMPNY_CD`, `SITE_CD`, `WORK_DATE`)
    , KEY `IX_ANSWER_HIST_COORD`  (`CMPNY_CD`, `SITE_CD`, `CHKPT_CD`, `INSPECT_ITEM_CD`, `WORK_DATE`, `ACTION_DTIME`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='순회점검 응답 덮어쓰기 감사 이력(append-only)';

-- (2) 불량조치 덮어쓰기 이력.
CREATE TABLE IF NOT EXISTS `tb_chkpt_defect_action_hist` (
      `HIST_ID`         bigint       NOT NULL AUTO_INCREMENT COMMENT '이력 PK'
    , `CMPNY_CD`        varchar(50)  COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '테넌트(이력 소유 회사)'
    , `SITE_CD`         varchar(50)  COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사업장'
    , `CHKPT_CD`        varchar(50)  COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '점검대상'
    , `INSPECT_ITEM_CD` varchar(20)  COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '점검문항'
    , `WORK_DATE`       varchar(8)   COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '점검일자 YYYYMMDD'
    , `CHG_TYPE`        varchar(2)   COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '변경유형 01:신규, 02:덮어쓰기'
    , `ACTION_DESC`     text         COLLATE utf8mb4_unicode_ci COMMENT '조치 상세 스냅샷'
    , `FILE_MGMT_CD`    varchar(50)  COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '조치 사진 파일관리코드 스냅샷'
    , `ACTION_CMPNY_CD` varchar(50)  COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '조치 회사 스냅샷(타 티어는 인접 1차 relabel)'
    , `ACTION_USER_CD`  varchar(50)  COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '조치자 USER_CD 스냅샷(표시 전용, 조인 금지)'
    , `ACTION_USER_NM`  varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '조치자 성명 스냅샷'
    , `ACTION_DTIME`    datetime     NOT NULL COMMENT '이 write 발생 시각(서버 NOW, KST) — 타임라인 정렬 기준'
    , `INSERT_NO`       varchar(50)  COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '기록 트리거 주체(자체저장=조치자, 전파=SYSTEM)'
    , `INSERT_DATE`     datetime     NOT NULL COMMENT '이력 적재 시각'
    , PRIMARY KEY (`HIST_ID`)
    , KEY `IX_DEFECT_HIST_FILTER` (`CMPNY_CD`, `SITE_CD`, `WORK_DATE`)
    , KEY `IX_DEFECT_HIST_COORD`  (`CMPNY_CD`, `SITE_CD`, `CHKPT_CD`, `INSPECT_ITEM_CD`, `WORK_DATE`, `ACTION_DTIME`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='순회점검 불량조치 덮어쓰기 감사 이력(append-only)';

-- 롤백: DROP TABLE tb_chkpt_inspect_answer_hist; DROP TABLE tb_chkpt_defect_action_hist;
