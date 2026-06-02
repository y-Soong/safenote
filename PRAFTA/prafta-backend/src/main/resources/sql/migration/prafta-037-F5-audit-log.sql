-- ============================================================================
-- PRAFTA-037-F5 — 감사 로그 테이블 신설 + SYS060(감사 액션 유형) 시드
-- 작성일: 2026-05-29
-- 적용 환경: MySQL 8.0.42
-- 참조: .claude/requests/web_requests/prafta-037-F5-plan.md, 공통 정책서 §11.3(감사)
--
-- ⚠ 2026-05-29 정정: 본 파일 초안은 SYS046 으로 작성되었으나 적용 시 SYS046 이
--   이미 prafta-033 'TBM 세션 상태' 코드그룹으로 점유되어 있어 SYS060 으로 이동.
--   본 파일은 정정 후 보관용. 잘못 들어간 SYS046='01' 정리는
--   별도 `prafta-037-F5-audit-log-fix-sys.sql` 파일로 처리.
--
-- 변경 요약
--  1) tb_audit_log 신규 — 다운로드/권한 변경/상태 변경 등 감사 대상 액션 적재.
--     - 본 작업(prafta-036 양식 다운로드)에서는 양식 다운로드 1행 INSERT 만 한다.
--     - 향후 권한 변경(02)/상태 변경(03)/조직 변경(04)/삭제(05)/조회(06) 등은 follow-up.
--     - 회사 스코프(CMPNY_CD) + 시간/사용자/액션/리소스 4개 인덱스로 조회 최적화.
--  2) SYS060 마스터 1건 + 디테일 1건('01 다운로드') 시드.
--     - tb_audit_log.ACTION_TYPE 코드 카탈로그.
--
-- 채번: AUDIT_ID = 'A' + YYYYMMDD + FNC_CMM_SEQ_NEXTVAL(cmpnyCd, 'AUDIT_LOG_ID')
--       (prafta-031 NOTI_OUTBOX_ID 패턴 동일)
--
-- 적용 전 부재 확인:
--   SELECT 1 FROM information_schema.tables WHERE table_name='tb_audit_log';
--   SELECT 1 FROM tb_syst_val_m WHERE SYST_VAL_CD='SYS060';
-- 멱등성: CREATE TABLE / PK INSERT 중복 시 에러. 운영 적용 후 보관용(재실행 금지).
-- 운영 적용: 사용자 수동(read-only MCP). 본 파일은 작성만, DB 직접 적용 금지.
-- ============================================================================

-- 1) tb_audit_log 신규 테이블.
CREATE TABLE `tb_audit_log` (
    `AUDIT_ID`        varchar(25)   NOT NULL COMMENT '감사 로그 ID (PK, 회사별 채번: A + YYYYMMDD + SEQ)',
    `CMPNY_CD`        varchar(50)   NOT NULL COMMENT '회사 코드',
    `USER_CD`         varchar(20)   NULL     COMMENT '행위자 사용자 코드(비로그인 행위는 NULL)',
    `ACTION_TYPE`     varchar(30)   NOT NULL COMMENT '감사 액션 유형[SYS060] 01:다운로드',
    `RESOURCE_TYPE`   varchar(50)   NOT NULL COMMENT '대상 리소스 유형 (예: USER_CREATE_TEMPLATE)',
    `RESOURCE_KEY`    varchar(200)  NULL     COMMENT '대상 리소스 식별자(양식 다운로드는 NULL)',
    `IP_ADDRESS`      varchar(45)   NULL     COMMENT '요청 IP (IPv6 지원, 추출 실패 시 NULL)',
    `USER_AGENT`      varchar(500)  NULL     COMMENT '요청 User-Agent',
    `DETAIL`          json          NULL     COMMENT '추가 페이로드(JSON, PII 평문 금지)',
    `DEL_YN`          varchar(1)    NOT NULL DEFAULT 'N' COMMENT '삭제 여부(감사는 무삭제 원칙)',
    `INSERT_NO`       varchar(50)   NOT NULL COMMENT '등록자(=USER_CD or SYSTEM)',
    `INSERT_DATE`     datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '등록 일시',
    PRIMARY KEY (`AUDIT_ID`),
    KEY `IX_AUDIT_LOG_TIME` (`CMPNY_CD`, `INSERT_DATE`),
    KEY `IX_AUDIT_LOG_USER` (`CMPNY_CD`, `USER_CD`, `INSERT_DATE`),
    KEY `IX_AUDIT_LOG_ACTION` (`CMPNY_CD`, `ACTION_TYPE`, `INSERT_DATE`),
    KEY `IX_AUDIT_LOG_RESOURCE` (`CMPNY_CD`, `RESOURCE_TYPE`, `INSERT_DATE`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='감사 로그 (다운로드/권한 변경/상태 변경 등)';

-- 2) SYS060 마스터.
INSERT INTO `tb_syst_val_m` (`SYST_VAL_CD`, `SYST_VAL_NM`, `USE_YN`, `VAL_DESC`, `INSERT_NO`)
VALUES ('SYS060', '감사 액션 유형', 'Y', 'tb_audit_log.ACTION_TYPE 코드', 'SYSTEM');

-- 3) SYS060 디테일 1건 시드.
INSERT INTO `tb_syst_val_d`
  (`SYST_VAL_CD`, `SYST_VAL_D_CD`, `SYST_VAL_D_NM`, `SORT_IDX`, `USE_YN`, `INSERT_NO`)
VALUES
  ('SYS060', '01', '다운로드', 1, 'Y', 'SYSTEM');
