-- ============================================================================
-- PRAFTA-037-F6 — 사용자 일괄 생성 비동기 잡 테이블 신설 + SYS061 시드
-- 작성일: 2026-05-29
-- 적용 환경: MySQL 8.0.42
-- 참조: .claude/requests/web_requests/prafta-037-F6-plan.md, 공통 정책서 §10(알림)
--
-- 변경 요약
--  1) tb_user_upload_job 신규 — 사용자 일괄 생성(엑셀 업로드) 비동기 잡 상태/진행률 적재.
--     - 사용자별 본인 잡만 조회(D7). 회사 스코프(CMPNY_CD) 강제.
--     - 매 행 처리 직후 PROCESSED_ROWS / SUCCESS_COUNT / FAIL_COUNT UPDATE (D12).
--     - 완료 시 FAILS_JSON 에 실패 항목 배열 적재(D13).
--  2) SYS061 마스터 + 디테일 5건 시드 (PENDING/RUNNING/SUCCESS/PARTIAL/FAILED, D6).
--
-- 채번: JOB_ID = 'U' + YYYYMMDD + FNC_CMM_SEQ_NEXTVAL(cmpnyCd, 'USER_UPLOAD_JOB_ID')
--       (prafta-031 NOTI_OUTBOX_ID, prafta-037-F5 AUDIT_LOG_ID 패턴 동일)
--
-- 적용 전 부재 확인:
--   SELECT 1 FROM information_schema.tables WHERE table_name='tb_user_upload_job';
--   SELECT 1 FROM tb_syst_val_m WHERE SYST_VAL_CD='SYS061';
-- 멱등성: CREATE TABLE / PK INSERT 중복 시 에러. 운영 적용 후 보관용(재실행 금지).
-- 운영 적용: 사용자 수동(read-only MCP). 본 파일은 작성만, DB 직접 적용 금지.
-- ============================================================================

-- 1) tb_user_upload_job 신규 테이블.
CREATE TABLE `tb_user_upload_job` (
    `JOB_ID`          varchar(25)   NOT NULL COMMENT '잡 ID (PK, 회사별 채번: U + YYYYMMDD + SEQ)',
    `CMPNY_CD`        varchar(50)   NOT NULL COMMENT '회사 코드',
    `USER_CD`         varchar(20)   NOT NULL COMMENT '잡 생성한 사용자 (작업 조회 권한 검증용)',
    `FILE_NAME`       varchar(255)  NULL     COMMENT '원본 파일명 (감사용)',
    `FILE_SIZE`       bigint        NULL     COMMENT '파일 바이트 크기',
    `TOTAL_ROWS`      int           NOT NULL DEFAULT 0 COMMENT '파싱된 데이터 행 수',
    `PROCESSED_ROWS`  int           NOT NULL DEFAULT 0 COMMENT '처리 완료 행 수 (성공+실패)',
    `SUCCESS_COUNT`   int           NOT NULL DEFAULT 0 COMMENT '성공 행 수',
    `FAIL_COUNT`      int           NOT NULL DEFAULT 0 COMMENT '실패 행 수',
    `FAILS_JSON`      json          NULL     COMMENT '실패 항목 JSON 배열 [{index,userId,errorCode,message}]',
    `STATUS`          varchar(20)   NOT NULL DEFAULT 'PENDING' COMMENT '잡 상태[SYS061] PENDING:대기 RUNNING:진행 SUCCESS:성공 PARTIAL:일부실패 FAILED:실패',
    `ERROR_MSG`       varchar(1000) NULL     COMMENT '치명 예외 사유 (FAILED 상태일 때)',
    `INSERT_NO`       varchar(50)   NOT NULL COMMENT '등록자',
    `INSERT_DATE`     datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '등록 일시',
    `UPDATE_NO`       varchar(50)   NULL     COMMENT '수정자',
    `UPDATE_DATE`     datetime      NULL     COMMENT '수정 일시',
    PRIMARY KEY (`JOB_ID`),
    KEY `IX_USER_UPLOAD_JOB_USER` (`CMPNY_CD`, `USER_CD`, `INSERT_DATE`),
    KEY `IX_USER_UPLOAD_JOB_STATUS` (`CMPNY_CD`, `STATUS`, `INSERT_DATE`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='사용자 일괄 생성 잡 (PRAFTA-037-F6 비동기)';

-- 2) SYS061 마스터.
INSERT INTO `tb_syst_val_m` (`SYST_VAL_CD`, `SYST_VAL_NM`, `USE_YN`, `VAL_DESC`, `INSERT_NO`)
VALUES ('SYS061', '사용자 업로드 잡 상태', 'Y', 'tb_user_upload_job.STATUS 코드', 'SYSTEM');

-- 3) SYS061 디테일 5건 시드.
INSERT INTO `tb_syst_val_d`
  (`SYST_VAL_CD`, `SYST_VAL_D_CD`, `SYST_VAL_D_NM`, `SORT_IDX`, `USE_YN`, `INSERT_NO`)
VALUES
  ('SYS061', 'PENDING',  '대기',     1, 'Y', 'SYSTEM'),
  ('SYS061', 'RUNNING',  '진행',     2, 'Y', 'SYSTEM'),
  ('SYS061', 'SUCCESS',  '성공',     3, 'Y', 'SYSTEM'),
  ('SYS061', 'PARTIAL',  '일부실패', 4, 'Y', 'SYSTEM'),
  ('SYS061', 'FAILED',   '실패',     5, 'Y', 'SYSTEM');
