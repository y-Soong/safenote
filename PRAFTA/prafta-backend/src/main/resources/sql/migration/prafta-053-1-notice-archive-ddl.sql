-- ============================================================================
-- PRAFTA-053-1 — 자료실(Archive) 분기 컬럼 추가 (tb_notice 공유)
-- 작성일: 2026-06-08
-- 적용 환경: MySQL 8.0.42
-- 출처: .claude/requests/web_requests/ref/prafta-053/prafta-053-plan.md §3, §2(053-1)
--       .claude/requests/web_requests/ref/prafta-053/작업지시서_자료실_기능.md §1-1
-- 선행: prafta-047-1-notice-ddl.sql (tb_notice 존재 전제)
--
-- 내용:
--   1) tb_notice 에 NOTICE_TYPE(게시구분), ARCHIVE_TYPE_CD(자료타입) 2컬럼 추가
--   2) 기존 공지 행 NOTICE_TYPE 백필 (DEFAULT 'NOTICE' 로 신규 INSERT 는 자동 처리)
--   3) 자료실 목록 필터/정렬용 인덱스 2종
--
-- 컨벤션:
--   - 코드성 컬럼 COMMENT 규칙(feedback_db_comment_code_convention): SYS코드가 아닌
--     컬럼 상수(NOTICE/ARCHIVE)는 의미만 명시. ARCHIVE_TYPE_CD 는 tb_baim_val_d 참조라
--     '[SYS]' 라벨 불요(plan §3 주석).
--   - utf8mb4 utf8mb4_unicode_ci 는 테이블 기본값 상속.
--
-- 적용 전 부재 확인(이미 일부 반영된 환경이면 해당 구문만 건너뛸 것):
--   SELECT COLUMN_NAME FROM information_schema.columns
--    WHERE table_name='tb_notice' AND column_name IN ('NOTICE_TYPE','ARCHIVE_TYPE_CD');
--   SELECT INDEX_NAME FROM information_schema.statistics
--    WHERE table_name='tb_notice' AND index_name IN ('IX_NOTICE_ARCHIVE_TYPE','IX_NOTICE_ARCHIVE_LIST');
-- 멱등성: 컬럼/인덱스가 이미 존재하면 ADD/CREATE 가 에러. 운영 적용 후 보관용(재실행 금지).
-- ============================================================================

-- ----------------------------------------------------------------------------
-- (1) 컬럼 추가
--     NOTICE_TYPE  : 게시구분(컬럼 상수). NOTICE=공지사항 / ARCHIVE=자료실. 기본 NOTICE.
--     ARCHIVE_TYPE_CD : 자료타입코드(tb_baim_val_d.BAIM_VAL_D_CD 참조).
--                       NOTICE_TYPE='ARCHIVE' 일 때 앱레벨 필수(FK 물리제약 미설정 — USE_YN='N'
--                       처리된 타입도 기존 자료엔 남아야 하므로 앱 검증).
-- ----------------------------------------------------------------------------
ALTER TABLE `tb_notice`
    ADD COLUMN `NOTICE_TYPE` varchar(10) NOT NULL DEFAULT 'NOTICE'
        COMMENT '게시구분(컬럼 상수) NOTICE:공지사항 ARCHIVE:자료실' AFTER `NOTICE_ID`,
    ADD COLUMN `ARCHIVE_TYPE_CD` varchar(50) DEFAULT NULL
        COMMENT '자료타입코드(tb_baim_val_d.BAIM_VAL_D_CD 참조) NOTICE_TYPE=ARCHIVE 시 앱레벨 필수' AFTER `NOTICE_TYPE`;

-- ----------------------------------------------------------------------------
-- (2) 기존 공지 행 백필
--     DEFAULT 'NOTICE' 로 신규 INSERT 는 자동 처리되나, ADD COLUMN 시점 기존 행도 DEFAULT
--     로 채워진다. 안전 보강(과거 환경에서 NULL/공백 잔재 방어).
-- ----------------------------------------------------------------------------
UPDATE `tb_notice`
   SET `NOTICE_TYPE` = 'NOTICE'
 WHERE `NOTICE_TYPE` IS NULL OR `NOTICE_TYPE` = '';

-- ----------------------------------------------------------------------------
-- (3) 인덱스
--     IX_NOTICE_ARCHIVE_TYPE : 자료실 목록 자료타입 필터
--     IX_NOTICE_ARCHIVE_LIST : 자료실 목록 최신순 정렬
-- ----------------------------------------------------------------------------
CREATE INDEX `IX_NOTICE_ARCHIVE_TYPE`
    ON `tb_notice` (`CMPNY_CD`, `NOTICE_TYPE`, `DEL_YN`, `ARCHIVE_TYPE_CD`);

CREATE INDEX `IX_NOTICE_ARCHIVE_LIST`
    ON `tb_notice` (`CMPNY_CD`, `NOTICE_TYPE`, `DEL_YN`, `INSERT_DATE`);

-- ============================================================================
-- 끝. 적용 후 검증:
--   SELECT COUNT(*) FROM information_schema.columns
--    WHERE table_name='tb_notice' AND column_name IN ('NOTICE_TYPE','ARCHIVE_TYPE_CD'); -- 2
--   SELECT COUNT(*) FROM tb_notice WHERE NOTICE_TYPE IS NULL OR NOTICE_TYPE=''; -- 0
-- ============================================================================
