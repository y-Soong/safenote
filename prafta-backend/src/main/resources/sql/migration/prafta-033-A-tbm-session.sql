-- ============================================================================
-- PRAFTA-033-A — TBM 세션/매핑/상태 테이블 신설 (4종)
-- 작성일: 2026-05-27
-- 적용 환경: MySQL 8.0.42
-- 참조: .claude/requests/prafta-033-A-ddl-content.md §2
--
-- 변경 요약 (생성만; INSERT/UPDATE 쓰기 경로는 B/C 단계에서 구현)
--  1) tb_tbm_session         — TBM 세션 마스터
--  2) tb_tbm_session_content — 세션 ↔ 콘텐츠 묶음 매핑(M:N, 방향 A)
--  3) tb_tbm_session_risk    — 세션 ↔ 위험성평가 매핑(M:N, 옵션)
--  4) tb_tbm_session_state   — 실시간 동기화 상태(세션 1:1, UPSERT) [C단계 쓰기]
--
-- 규약
--  - PK는 varchar 코드 채번(FNC_CMM_SEQ_NEXTVAL). CMPNY_CD = varchar(50).
--  - GPS: decimal(10,7), 컬럼명 *_GPS_LAT / *_GPS_LON (기존 tb_user_attd_gps 규약, LON 통일).
--  - 코드성 컬럼 COMMENT: '설명[SYSxxx] 코드값:의미'.
--  - MTRL_CD/위험성평가 FK 제약은 걸지 않음(콘텐츠 소프트삭제·이력 보존; 무결성은 서비스).
--
-- 적용 전 부재 확인: SHOW TABLES LIKE 'tb_tbm_session';
-- 멱등성: CREATE TABLE 중복 실행 시 에러. 운영 적용 후 보관용(재실행 금지).
-- ⚠️ 운영 적용은 사용자 수동(read-only MCP). 본 파일은 작성만, DB 직접 적용 금지.
-- ============================================================================

-- (1) TBM 세션 마스터
CREATE TABLE `tb_tbm_session` (
  `SESSION_CD`            varchar(20)  NOT NULL COMMENT 'TBM 세션코드 (PK, 채번 T+YYYYMMDD+SEQ)',
  `CMPNY_CD`              varchar(50)  NOT NULL COMMENT '회사코드',
  `SITE_CD`               varchar(50)  NOT NULL COMMENT '사업장코드',
  `EDU_TYPE_CD`           varchar(20)  NOT NULL DEFAULT 'TBM' COMMENT '교육유형[SYS047] TBM:툴박스미팅 (확장용 고정값)',
  `TITLE`                 varchar(200) NOT NULL COMMENT '세션 제목',
  `CONTENT_BODY`          mediumtext   NULL     COMMENT '교육 내용(리치 HTML). 개설 시 필수(서버 검증)',
  `CONTENT_FORMAT_CD`     varchar(20)  NOT NULL DEFAULT 'RICH_HTML' COMMENT '교육내용 형식 RICH_HTML:리치텍스트(MVP 고정값)',
  `STATUS_CD`             varchar(20)  NOT NULL DEFAULT 'DRAFT' COMMENT '세션상태[SYS046] DRAFT:작성중 OPENED:개설 IN_PROGRESS:진행중 COMPLETED:종료 CANCELLED:취소',
  `ENTRY_PWD`             varchar(10)  NULL     COMMENT '입실 비밀번호(랜덤6자리, OPENED부터 생성)',
  `EXIT_PWD`              varchar(10)  NULL     COMMENT '종료 비밀번호(입실≠종료)',
  `MANAGER_USER_CD`       varchar(20)  NOT NULL COMMENT '개설자 USER_CD',
  `MANAGER_GPS_LAT`       decimal(10,7) NULL    COMMENT '개설 위도(AUTO 모드 시)',
  `MANAGER_GPS_LON`       decimal(10,7) NULL    COMMENT '개설 경도(AUTO 모드 시)',
  `GPS_VERIFY_TYPE_CD`    varchar(10)  NOT NULL DEFAULT 'AUTO' COMMENT 'GPS검증유형[SYS048] AUTO:자동 MANUAL:수동확인 DISABLED:비활성',
  `GPS_VERIFY_RADIUS_M`   int          NOT NULL DEFAULT 100 COMMENT 'GPS 검증반경(m, 50~1000)',
  `GPS_MANUAL_CONFIRM_YN` varchar(2)   NOT NULL DEFAULT 'N' COMMENT 'MANUAL 모드 관리자 확인여부 Y:확인',
  `OPENED_AT`             datetime     NULL     COMMENT '개설 시각',
  `STARTED_AT`            datetime     NULL     COMMENT '교육 시작 시각(IN_PROGRESS 전이) [C단계]',
  `ENDED_AT`              datetime     NULL     COMMENT '교육 종료 시각 [C단계]',
  `CANCELLED_AT`          datetime     NULL     COMMENT '취소 시각',
  `CANCEL_REASON`         varchar(500) NULL     COMMENT '취소 사유',
  `DEL_YN`                varchar(2)   NOT NULL DEFAULT 'N' COMMENT '삭제여부 Y/N (DRAFT 물리관리용, OPENED+ 는 STATUS_CD=CANCELLED 사용)',
  `INSERT_NO`             varchar(50)  NOT NULL COMMENT '입력자',
  `INSERT_DATE`           datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  `UPDATE_NO`             varchar(50)  NOT NULL COMMENT '수정자',
  `UPDATE_DATE`           datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
  PRIMARY KEY (`SESSION_CD`),
  KEY `IX_TBM_SESSION_01` (`CMPNY_CD`, `SITE_CD`, `STATUS_CD`),
  KEY `IX_TBM_SESSION_02` (`CMPNY_CD`, `MANAGER_USER_CD`),
  KEY `IX_TBM_SESSION_03` (`CMPNY_CD`, `INSERT_DATE`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='TBM 세션';

-- (2) 세션 ↔ 콘텐츠 묶음 매핑(M:N, 방향 A: 묶음 단위 첨부)
CREATE TABLE `tb_tbm_session_content` (
  `CMPNY_CD`      varchar(50)  NOT NULL COMMENT '회사코드',
  `SESSION_CD`    varchar(20)  NOT NULL COMMENT 'TBM 세션코드',
  `MTRL_CD`       varchar(20)  NOT NULL COMMENT '교육자료 묶음코드 (TB_TBM_EDU_MTRL)',
  `DISPLAY_ORDER` int          NOT NULL DEFAULT 0 COMMENT '세션 내 표시 순서',
  `OVERRIDE_DESC` varchar(500) NULL     COMMENT '세션별 설명 override (이 세션에서만 다른 설명)',
  `INSERT_NO`     varchar(50)  NOT NULL COMMENT '입력자',
  `INSERT_DATE`   datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  PRIMARY KEY (`CMPNY_CD`, `SESSION_CD`, `MTRL_CD`),
  KEY `IX_TBM_SESSION_CONTENT_01` (`CMPNY_CD`, `SESSION_CD`, `DISPLAY_ORDER`),
  KEY `IX_TBM_SESSION_CONTENT_02` (`CMPNY_CD`, `MTRL_CD`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='TBM 세션-콘텐츠 묶음 매핑';

-- (3) 세션 ↔ 위험성평가 매핑(M:N, 옵션). TB_RISK_ASSESSMENT 복합 PK 보유
CREATE TABLE `tb_tbm_session_risk` (
  `CMPNY_CD`      varchar(50) NOT NULL COMMENT '회사코드',
  `SESSION_CD`    varchar(20) NOT NULL COMMENT 'TBM 세션코드',
  `SITE_CD`       varchar(50) NOT NULL COMMENT '위험성평가 사업장코드',
  `PROCESS_CD`    varchar(10) NOT NULL COMMENT '위험성평가 공정코드[COM002]',
  `ASSESSMENT_CD` varchar(10) NOT NULL COMMENT '위험성평가 평가코드',
  `DISPLAY_ORDER` int         NOT NULL DEFAULT 0 COMMENT '표시 순서',
  `INSERT_NO`     varchar(50) NOT NULL COMMENT '입력자',
  `INSERT_DATE`   datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  PRIMARY KEY (`CMPNY_CD`, `SESSION_CD`, `SITE_CD`, `PROCESS_CD`, `ASSESSMENT_CD`),
  KEY `IX_TBM_SESSION_RISK_01` (`CMPNY_CD`, `SESSION_CD`, `DISPLAY_ORDER`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='TBM 세션-위험성평가 매핑';

-- (4) 실시간 동기화 상태(세션 1:1, UPSERT) [C단계 쓰기]
CREATE TABLE `tb_tbm_session_state` (
  `CMPNY_CD`            varchar(50) NOT NULL COMMENT '회사코드',
  `SESSION_CD`          varchar(20) NOT NULL COMMENT 'TBM 세션코드',
  `CURRENT_MTRL_CD`     varchar(20) NULL     COMMENT '현재 표시중 콘텐츠 묶음코드',
  `CURRENT_ITEM_CD`     varchar(20) NULL     COMMENT '현재 표시중 세부항목코드',
  `CURRENT_SLIDE_INDEX` int         NOT NULL DEFAULT 0 COMMENT '현재 슬라이드 인덱스',
  `SYNC_STATE_CD`       varchar(20) NOT NULL DEFAULT 'PAUSED' COMMENT '동기화상태[SYS049] PLAYING:재생 PAUSED:정지',
  `LAST_UPDATED_BY`     varchar(20) NULL     COMMENT '마지막 갱신 관리자',
  `INSERT_NO`           varchar(50) NOT NULL COMMENT '입력자',
  `INSERT_DATE`         datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  `UPDATE_NO`           varchar(50) NULL     COMMENT '수정자',
  `UPDATE_DATE`         datetime    NULL     COMMENT '수정일시',
  PRIMARY KEY (`CMPNY_CD`, `SESSION_CD`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='TBM 세션 실시간 동기화 상태(UPSERT)';
