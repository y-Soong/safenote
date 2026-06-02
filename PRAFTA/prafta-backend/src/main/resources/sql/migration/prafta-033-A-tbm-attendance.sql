-- ============================================================================
-- PRAFTA-033-A — TBM 출결/이벤트/비번실패 테이블 신설 (3종)
-- 작성일: 2026-05-27
-- 적용 환경: MySQL 8.0.42
-- 참조: .claude/requests/prafta-033-A-ddl-content.md §3
--
-- 변경 요약 (생성만; INSERT/UPDATE 쓰기 경로는 C/앱 단계, 읽기는 D 단계)
--  1) tb_tbm_attendance       — 출결 통합(정규직 TB_USER / 일용직 TB_DAILY_USER)
--  2) tb_tbm_attendance_event — 출결 이벤트 로그(고볼륨 append)
--  3) tb_tbm_pwd_fail         — 입실/종료 비번 실패 로그
--
-- 규약 / 검토 반영
--  - 정규직/일용직 통합 한 테이블: USER_TYPE_CD 로 구분. UK 에 USER_TYPE_CD 포함(중복출결 방지·멱등).
--  - ATTENDANCE_CD surrogate PK(채번) → 이벤트 로그가 단일 컬럼으로 참조.
--  - 이벤트/비번실패 로그는 PRAFTA 관례 예외로 BIGINT AUTO_INCREMENT 채택(고볼륨 append).
--  - 비번 평문(ATTEMPTED_PWD)은 저장하지 않음(보안 권고). 실패 횟수/시각만 기록.
--  - GPS: decimal(10,7), *_GPS_LAT / *_GPS_LON. 코드성 컬럼 COMMENT '설명[SYSxxx] 코드값:의미'.
--
-- 적용 전 부재 확인: SHOW TABLES LIKE 'tb_tbm_attendance';
-- 멱등성: CREATE TABLE 중복 실행 시 에러. 운영 적용 후 보관용(재실행 금지).
-- ⚠️ 운영 적용은 사용자 수동(read-only MCP). 본 파일은 작성만, DB 직접 적용 금지.
-- ============================================================================

-- (1) 출결 통합(정규직/일용직)
CREATE TABLE `tb_tbm_attendance` (
  `ATTENDANCE_CD`            varchar(20)  NOT NULL COMMENT '출결코드 (PK, 채번 A+YYYYMMDD+SEQ)',
  `CMPNY_CD`                 varchar(50)  NOT NULL COMMENT '회사코드',
  `SESSION_CD`               varchar(20)  NOT NULL COMMENT 'TBM 세션코드',
  `USER_TYPE_CD`             varchar(20)  NOT NULL COMMENT '대상유형[SYS050] REGULAR:정규직(TB_USER) DAILY:일용직(TB_DAILY_USER)',
  `USER_CD`                  varchar(20)  NOT NULL COMMENT '대상 USER_CD (유형에 따라 TB_USER 또는 TB_DAILY_USER)',
  `ENTRY_TYPE_CD`            varchar(20)  NULL     COMMENT '입실경로[SYS051] SELF_DEVICE:본인디바이스 MANAGER_QR_SCAN:관리자QR스캔',
  `ENTRY_BY_MANAGER_USER_CD` varchar(20)  NULL     COMMENT 'QR 입실 처리 관리자 USER_CD',
  `ENTRY_AT`                 datetime     NULL     COMMENT '입실 시각',
  `ENTRY_GPS_LAT`            decimal(10,7) NULL    COMMENT '입실 위도',
  `ENTRY_GPS_LON`            decimal(10,7) NULL    COMMENT '입실 경도',
  `ENTRY_DISTANCE_M`         int          NULL     COMMENT '입실 시 개설지점과의 거리(m)',
  `ENTRY_SIGN_FILE_MGMT_CD`  varchar(50)  NULL     COMMENT '입실 서명 파일코드',
  `EXIT_TYPE_CD`             varchar(20)  NULL     COMMENT '종료경로[SYS052] SELF:본인 MANAGER_QR_SCAN:관리자QR MANAGER_FORCED:관리자강제',
  `EXIT_BY_MANAGER_USER_CD`  varchar(20)  NULL     COMMENT '종료 처리 관리자 USER_CD',
  `EXIT_AT`                  datetime     NULL     COMMENT '종료 시각(NULL=미종료)',
  `EXIT_SIGN_FILE_MGMT_CD`   varchar(50)  NULL     COMMENT '종료 서명 파일코드',
  `EXIT_FORCED_REASON`       varchar(500) NULL     COMMENT '강제종료 사유(관리자 책임 기록)',
  `COMPLETION_STATUS_CD`     varchar(20)  NULL     COMMENT '이수상태[SYS053] COMPLETED:이수 NOT_COMPLETED:미이수',
  `NOT_COMPLETED_REASON`     varchar(500) NULL     COMMENT '미이수 사유',
  `STATUS_UPDATED_BY`        varchar(20)  NULL     COMMENT '이수상태 마지막 변경자',
  `STATUS_UPDATED_AT`        datetime     NULL     COMMENT '이수상태 마지막 변경 시각',
  `DEL_YN`                   varchar(2)   NOT NULL DEFAULT 'N' COMMENT '삭제여부 Y/N',
  `INSERT_NO`                varchar(50)  NOT NULL COMMENT '입력자',
  `INSERT_DATE`              datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  `UPDATE_NO`                varchar(50)  NOT NULL COMMENT '수정자',
  `UPDATE_DATE`              datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
  PRIMARY KEY (`ATTENDANCE_CD`),
  UNIQUE KEY `UK_TBM_ATTENDANCE_01` (`CMPNY_CD`, `SESSION_CD`, `USER_TYPE_CD`, `USER_CD`),
  KEY `IX_TBM_ATTENDANCE_01` (`CMPNY_CD`, `SESSION_CD`),
  KEY `IX_TBM_ATTENDANCE_02` (`CMPNY_CD`, `USER_TYPE_CD`, `USER_CD`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='TBM 출결(정규직/일용직 통합)';

-- (2) 출결 이벤트 로그
CREATE TABLE `tb_tbm_attendance_event` (
  `EVENT_NO`           bigint       NOT NULL AUTO_INCREMENT COMMENT '이벤트 일련번호 (PK)',
  `CMPNY_CD`           varchar(50)  NOT NULL COMMENT '회사코드',
  `SESSION_CD`         varchar(20)  NOT NULL COMMENT 'TBM 세션코드(비정규화, 조회용)',
  `ATTENDANCE_CD`      varchar(20)  NOT NULL COMMENT '출결코드',
  `EVENT_TYPE_CD`      varchar(30)  NOT NULL COMMENT '이벤트유형[SYS054] ENTER/START/SLIDE_CHANGED/GPS_UPDATED/BACKGROUND_IN/BACKGROUND_OUT/NETWORK_LOST/SIGNATURE_STARTED/END/FORCED_END',
  `EVENT_TIME`         datetime(3)  NOT NULL COMMENT '이벤트 발생시각(클라이언트 보고, ms)',
  `SERVER_RECEIVED_AT` datetime(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '서버 수신시각(ms, 위조불가 기준)',
  `EVENT_DATA`         json         NULL     COMMENT '이벤트 부가데이터(JSON)',
  `INSERT_NO`          varchar(50)  NOT NULL COMMENT '입력자',
  `INSERT_DATE`        datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  PRIMARY KEY (`EVENT_NO`),
  KEY `IX_TBM_ATT_EVENT_01` (`CMPNY_CD`, `ATTENDANCE_CD`, `EVENT_TIME`),
  KEY `IX_TBM_ATT_EVENT_02` (`CMPNY_CD`, `SESSION_CD`, `EVENT_TYPE_CD`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='TBM 출결 이벤트 로그';

-- (3) 입실/종료 비번 실패 로그 (평문 미저장)
CREATE TABLE `tb_tbm_pwd_fail` (
  `FAIL_NO`       bigint      NOT NULL AUTO_INCREMENT COMMENT '실패 일련번호 (PK)',
  `CMPNY_CD`      varchar(50) NOT NULL COMMENT '회사코드',
  `SESSION_CD`    varchar(20) NOT NULL COMMENT 'TBM 세션코드',
  `PWD_TYPE_CD`   varchar(10) NOT NULL COMMENT '비번유형[SYS055] ENTRY:입실 EXIT:종료',
  `USER_TYPE_CD`  varchar(20) NULL     COMMENT '대상유형[SYS050] REGULAR:정규직 DAILY:일용직',
  `USER_CD`       varchar(20) NULL     COMMENT '시도자 USER_CD(식별 가능 시)',
  `ATTEMPTED_AT`  datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '시도 시각',
  `INSERT_NO`     varchar(50) NULL     COMMENT '입력자',
  `INSERT_DATE`   datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  PRIMARY KEY (`FAIL_NO`),
  KEY `IX_TBM_PWD_FAIL_01` (`CMPNY_CD`, `SESSION_CD`, `ATTEMPTED_AT`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='TBM 비밀번호 실패 로그';
