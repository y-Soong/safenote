-- ============================================================================
-- PRAFTA-047-1 — 공지사항(Notice) 도메인 신규 테이블 DDL
-- 작성일: 2026-06-05
-- 적용 환경: MySQL 8.0.42
-- 출처: .claude/requests/web_requests/prafta-047.md §1-1~1-4
--       .claude/requests/web_requests/prafta-047-plan.md §3 (PRAFTA-047-1)
--
-- 생성 테이블(4종):
--   1) tb_notice           공지 마스터 (PK: CMPNY_CD, NOTICE_ID)
--   2) tb_notice_target    공지 대상 매핑 (PK: CMPNY_CD, NOTICE_ID, TARGET_SEQ)
--   3) tb_notice_file      공지 첨부 매핑 (PK: CMPNY_CD, NOTICE_ID, FILE_MGMT_CD)
--   4) tb_notice_user_ack  사용자 확인/숨김 이력 (PK: CMPNY_CD, NOTICE_ID, USER_CD)
--
-- 컨벤션(CLAUDE.md): 멀티테넌트 CMPNY_CD 모든 PK 선두 / 날짜 VARCHAR(8) YYYYMMDD /
--                    감사 INSERT_NO·INSERT_DATE·UPDATE_NO·UPDATE_DATE / 삭제 DEL_YN /
--                    utf8mb4 utf8mb4_unicode_ci.
-- 코드성 컬럼 COMMENT 규칙: '설명[SYS코드] 값:의미'. TARGET_SCOPE/ACK_TYPE 는 SYS코드 없는
--                          컬럼 상수(plan §2 13-1 결정)이므로 의미만 명시.
--
-- 적용 전 부재 확인(이미 일부 반영된 환경이면 해당 구문만 건너뛸 것):
--   SELECT 1 FROM information_schema.tables WHERE table_name='tb_notice';
--   SELECT 1 FROM information_schema.tables WHERE table_name='tb_notice_target';
--   SELECT 1 FROM information_schema.tables WHERE table_name='tb_notice_file';
--   SELECT 1 FROM information_schema.tables WHERE table_name='tb_notice_user_ack';
-- 멱등성: 이미 존재하는 테이블에는 재실행 금지(CREATE 충돌). 운영 적용 후 보관용.
-- ============================================================================

-- ----------------------------------------------------------------------------
-- (1) tb_notice — 공지 마스터
--     EDIT_PWD 는 BCrypt 해시(평문 저장 금지). POPUP_FROM/TO_YMD 는 POPUP_YN='Y' 시 필수.
--     PIN_ORDER 는 서버 정규화(요청서 §5). "수정됨" 뱃지는 UPDATE_DATE 기준(요청서 §7, 사용자별 계산).
-- ----------------------------------------------------------------------------
CREATE TABLE `tb_notice` (
    `CMPNY_CD`          varchar(50)  NOT NULL COMMENT '회사코드',
    `NOTICE_ID`         varchar(20)  NOT NULL COMMENT '공지ID (회사별 채번: N + YYYYMMDD + 3자리 SEQ)',
    `TITLE`             varchar(200) NOT NULL COMMENT '제목',
    `CONTENT`           longtext     NOT NULL COMMENT '내용(리치텍스트 가능)',
    `EDIT_PWD`          varchar(100) NOT NULL COMMENT '수정 비밀번호 BCrypt 해시(평문 저장 금지)',
    `TARGET_SCOPE`      varchar(10)  NOT NULL COMMENT '대상 스코프(컬럼 상수) ALL:전사 SITE:사업장 NODE:사업장+노드. 상세 대상은 tb_notice_target',
    `INCLUDE_DAILY_YN`  varchar(1)   NOT NULL DEFAULT 'N' COMMENT '일용직 포함 여부 Y/N',
    `POPUP_YN`          varchar(1)   NOT NULL DEFAULT 'N' COMMENT '로그인 시 팝업 여부 Y/N',
    `POPUP_FROM_YMD`    varchar(8)            DEFAULT NULL COMMENT '팝업 시작일 YYYYMMDD (POPUP_YN=Y 시 필수)',
    `POPUP_TO_YMD`      varchar(8)            DEFAULT NULL COMMENT '팝업 종료일 YYYYMMDD (POPUP_YN=Y 시 필수)',
    `PIN_YN`            varchar(1)   NOT NULL DEFAULT 'N' COMMENT '상단 고정 여부 Y/N',
    `PIN_ORDER`         int                   DEFAULT NULL COMMENT '고정 순번(1부터, PIN_YN=Y 시만). 서버 정규화(요청서 §5)',
    `DEL_YN`            varchar(1)   NOT NULL DEFAULT 'N' COMMENT '삭제 여부 Y/N (논리삭제)',
    `INSERT_NO`         varchar(50)  NOT NULL COMMENT '등록자 USER_CD',
    `INSERT_DATE`       datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '등록 일시',
    `UPDATE_NO`         varchar(50)           DEFAULT NULL COMMENT '수정자 USER_CD',
    `UPDATE_DATE`       datetime              DEFAULT NULL COMMENT '수정 일시(사용자별 "수정됨" 뱃지 판정 기준, 요청서 §7)',
    PRIMARY KEY (`CMPNY_CD`, `NOTICE_ID`),
    KEY `IX_TB_NOTICE_LIST` (`CMPNY_CD`, `DEL_YN`, `PIN_YN`, `PIN_ORDER`),
    KEY `IX_TB_NOTICE_POPUP` (`CMPNY_CD`, `POPUP_YN`, `POPUP_FROM_YMD`, `POPUP_TO_YMD`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='공지사항 마스터';

-- ----------------------------------------------------------------------------
-- (2) tb_notice_target — 공지 대상 매핑 (공지 1 : 대상 N)
--     TARGET_SCOPE 가 SITE/NODE 일 때만 행 존재(ALL 이면 행 없음).
--     SITE 스코프 = NODE_CD NULL 행(사업장 전체). NODE 스코프 = NODE_CD 지정.
-- ----------------------------------------------------------------------------
CREATE TABLE `tb_notice_target` (
    `CMPNY_CD`                varchar(50)  NOT NULL COMMENT '회사코드',
    `NOTICE_ID`               varchar(20)  NOT NULL COMMENT '공지ID',
    `TARGET_SEQ`              int          NOT NULL COMMENT '대상 순번(1부터)',
    `SITE_CD`                 varchar(50)  NOT NULL COMMENT '대상 사업장코드 (SITE/NODE 공통 필수)',
    `NODE_CD`                 varchar(50)           DEFAULT NULL COMMENT '대상 노드코드 (NODE 일 때만, NULL=사업장 전체)',
    `INCLUDE_DESCENDANTS_YN`  varchar(1)   NOT NULL DEFAULT 'Y' COMMENT '하위(자손) 노드 포함 여부 Y/N (NODE 일 때만 의미)',
    `INSERT_NO`               varchar(50)  NOT NULL COMMENT '등록자',
    `INSERT_DATE`             datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '등록 일시',
    PRIMARY KEY (`CMPNY_CD`, `NOTICE_ID`, `TARGET_SEQ`),
    KEY `IX_TB_NOTICE_TARGET_MATCH` (`CMPNY_CD`, `SITE_CD`, `NODE_CD`),
    KEY `IX_TB_NOTICE_TARGET_NOTICE` (`CMPNY_CD`, `NOTICE_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='공지 대상 매핑';

-- ----------------------------------------------------------------------------
-- (3) tb_notice_file — 공지 첨부 매핑
--     실제 파일 메타는 tb_file_info(FILE_TYPE='005' 공지첨부, prafta-047-2). 공지 삭제 시 물리 삭제 안 함.
-- ----------------------------------------------------------------------------
CREATE TABLE `tb_notice_file` (
    `CMPNY_CD`      varchar(50)  NOT NULL COMMENT '회사코드',
    `NOTICE_ID`     varchar(20)  NOT NULL COMMENT '공지ID',
    `FILE_MGMT_CD`  varchar(50)  NOT NULL COMMENT 'tb_file_info 파일관리코드(FK)',
    `SORT_IDX`      int          NOT NULL DEFAULT '1' COMMENT '첨부 정렬순서',
    `INSERT_NO`     varchar(50)  NOT NULL COMMENT '등록자',
    `INSERT_DATE`   datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '등록 일시',
    PRIMARY KEY (`CMPNY_CD`, `NOTICE_ID`, `FILE_MGMT_CD`),
    KEY `IX_TB_NOTICE_FILE_NOTICE` (`CMPNY_CD`, `NOTICE_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='공지 첨부 매핑';

-- ----------------------------------------------------------------------------
-- (4) tb_notice_user_ack — 사용자 확인/숨김 이력 (사용자×공지당 1행, UPSERT 대상)
--     ACK_TYPE 컬럼 상수: CONFIRMED 영구확인 / SNOOZED 한시숨김(7일) /
--                        READ 열람마킹(LAST_READ_DATE 만 갱신하는 신규행 기본값. 팝업/배지 판정은 CONFIRMED/SNOOZED 만 참조).
--     일용직 USER_CD 도 적재되나 SNOOZED 미사용(요청서 §10). 정리는 prafta-047-5 배치.
-- ----------------------------------------------------------------------------
CREATE TABLE `tb_notice_user_ack` (
    `CMPNY_CD`          varchar(50)  NOT NULL COMMENT '회사코드',
    `NOTICE_ID`         varchar(20)  NOT NULL COMMENT '공지ID',
    `USER_CD`           varchar(20)  NOT NULL COMMENT '사용자코드(정규/일용 공통)',
    `ACK_TYPE`          varchar(10)  NOT NULL COMMENT '확인 유형(컬럼 상수) CONFIRMED:영구확인 SNOOZED:한시숨김 READ:열람마킹(LAST_READ_DATE 갱신용 신규행 기본값, 확인/숨김 아님)',
    `SNOOZE_UNTIL_YMD`  varchar(8)            DEFAULT NULL COMMENT '숨김 만료일 YYYYMMDD (ACK_TYPE=SNOOZED 시, 처리일+7일)',
    `LAST_READ_DATE`    datetime              DEFAULT NULL COMMENT '마지막 열람 일시("수정됨" 뱃지 판정, 요청서 §7)',
    `INSERT_NO`         varchar(50)  NOT NULL COMMENT '등록자',
    `INSERT_DATE`       datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '등록 일시',
    `UPDATE_NO`         varchar(50)           DEFAULT NULL COMMENT '수정자',
    `UPDATE_DATE`       datetime              DEFAULT NULL COMMENT '수정 일시',
    PRIMARY KEY (`CMPNY_CD`, `NOTICE_ID`, `USER_CD`),
    KEY `IX_TB_NOTICE_ACK_USER` (`CMPNY_CD`, `USER_CD`, `NOTICE_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='공지 사용자 확인/숨김 이력';

-- ============================================================================
-- 끝. 적용 후 검증:
--   SELECT COUNT(*) FROM information_schema.tables
--    WHERE table_name IN ('tb_notice','tb_notice_target','tb_notice_file','tb_notice_user_ack'); -- 4
-- ============================================================================
