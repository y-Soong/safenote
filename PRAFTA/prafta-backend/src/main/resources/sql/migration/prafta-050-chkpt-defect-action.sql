-- ============================================================================
-- PRAFTA-050-1 — 점검 불량 조치(개선) 내역 신규 테이블 DDL
-- 작성일: 2026-06-07
-- 적용 환경: MySQL 8.0.42
-- 출처: .claude/requests/web_requests/prafta-050-plan.md §3 (확정 DDL)
--       .claude/requests/web_requests/prafta-050.md
-- 선행: 없음
--
-- 등록 항목:
--   1) tb_chkpt_defect_action 신규 테이블
--      - PK = 불량 1:1 (CMPNY_CD, SITE_CD, CHKPT_CD, INSPECT_ITEM_CD, WORK_DATE)
--        → tb_chkpt_inspect_answer 와 동일 키. 불량(INSPECT_ANSWER_TYPE='N') 1건당 조치 1건(upsert).
--      - 조치여부는 본 테이블 행 존재 여부로 파생(별도 상태컬럼/SYS코드 없음, Q3).
--      - tb_chkpt_inspect_answer.INSPECT_ANSWER_TYPE 의 'N'(불량)은 조치해도 'Y'로 변경하지 않음.
--        조치 내역은 본 테이블에만 기록.
--   - 코드성 컬럼 없음 → SYSxxx 주석 규칙 적용 대상 컬럼 없음.
--   - FK 제약은 기존 chkLst 테이블 관례상 생성하지 않음(논리적 1:1, 앱 레벨 정합).
--
-- 적용 전 부재 확인(이미 반영된 환경이면 CREATE 구문 건너뛸 것):
--   SELECT 1 FROM information_schema.tables WHERE table_name='tb_chkpt_defect_action';
-- 멱등성: PK/테이블 중복 시 에러. 운영 적용 후 보관용(재실행 금지).
-- ============================================================================

CREATE TABLE `tb_chkpt_defect_action` (
    `CMPNY_CD`        varchar(50)  NOT NULL COMMENT '회사코드',
    `SITE_CD`         varchar(50)  NOT NULL COMMENT '사업장코드',
    `CHKPT_CD`        varchar(50)  NOT NULL COMMENT '체크포인트 코드(점검대상)',
    `INSPECT_ITEM_CD` varchar(20)  NOT NULL COMMENT '점검항목코드',
    `WORK_DATE`       varchar(8)   NOT NULL COMMENT '점검일자(YYYYMMDD) — 불량 발생일',
    `ACTION_DESC`     text         NOT NULL COMMENT '조치 상세 내역(불량 처리 내용)',
    `INSERT_NO`       varchar(50)           DEFAULT NULL COMMENT '입력자(tb_user.USER_CD)',
    `INSERT_DATE`     datetime              DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
    `UPDATE_NO`       varchar(50)           DEFAULT NULL COMMENT '수정자(tb_user.USER_CD)',
    `UPDATE_DATE`     datetime              DEFAULT NULL COMMENT '수정일시',
    PRIMARY KEY (`CMPNY_CD`, `SITE_CD`, `CHKPT_CD`, `INSPECT_ITEM_CD`, `WORK_DATE`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='점검 불량 조치(개선) 내역';

-- ============================================================================
-- 끝. 적용 후 검증:
--   SELECT 1 FROM information_schema.tables WHERE table_name='tb_chkpt_defect_action'; -- 1
-- ============================================================================
