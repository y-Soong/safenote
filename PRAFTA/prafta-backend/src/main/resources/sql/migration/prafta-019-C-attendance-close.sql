-- ============================================================================
-- PRAFTA-019-C — 근태 마감
-- 작성일: 2026-05-23
-- 적용 환경: MySQL 8.0.42
-- 참조: .claude/requests/prafta-019-C-attendance-close.md, prafta-019-plan.md
--       정책서 attd/§13(근태 마감)·§12(스케줄 마감), 재기획서 §3.3(자동마감 금지)·§9.4(마감기준일)
--
-- 설계 결정 (정책 정독 후 확정)
--  - 마감 단위: 회사 + 사업장 + 마감월(YYYYMM). (계획서 "회사/사업장 단위" 채택.
--    정책서 §13의 조직(노드) 단위는 v-next 세분화로 분리. E의 사후신청 차단은 사업장+월로 판정.)
--  - 자동 마감 금지·강제 마감 미도입(§3.3): 차단 사유(미결 요청/GPS 미확인/미승인 초과근무)가
--    0건일 때만 마감 가능. 마감 도래해도 잔존 시 "마감 지연"은 화면 표시(미결 카운트)로만 처리.
--  - 마감/해제는 별도 권한(매니저)으로 통제(§12.2/§13.4). v1 임금 정산 미연동(§3.4).
--
-- 멱등성: CREATE TABLE 중복 실행 시 에러. 이미 반영된 환경에서는 건너뛸 것.
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1) 근태 마감 상태 테이블 (회사+사업장+마감월 1행)
-- ----------------------------------------------------------------------------
CREATE TABLE `tb_attd_close` (
    `CMPNY_CD`        varchar(50)  NOT NULL COMMENT '회사 코드',
    `SITE_CD`         varchar(50)  NOT NULL COMMENT '사업장 코드',
    `CLOSE_YM`        char(6)      NOT NULL COMMENT '마감 기준월 (YYYYMM)',
    `CLOSE_STATUS`    varchar(10)  NOT NULL DEFAULT 'OPEN' COMMENT '마감 상태 (OPEN 미마감 / CLOSED 마감)',
    `CLOSE_DTIME`     datetime              DEFAULT NULL COMMENT '마감 일시',
    `CLOSE_USER_CD`   varchar(20)           DEFAULT NULL COMMENT '마감자 사용자 코드',
    `UNCLOSE_DTIME`   datetime              DEFAULT NULL COMMENT '마감 해제 일시',
    `UNCLOSE_USER_CD` varchar(20)           DEFAULT NULL COMMENT '마감 해제자 사용자 코드',
    `CLOSE_DESC`      varchar(500)          DEFAULT NULL COMMENT '마감/해제 사유',
    `INSERT_NO`       varchar(50)           DEFAULT 'SYSTEM' COMMENT '입력자',
    `INSERT_DATE`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
    `UPDATE_NO`       varchar(50)           DEFAULT NULL COMMENT '수정자',
    `UPDATE_DATE`     datetime              DEFAULT NULL COMMENT '수정일시',
    PRIMARY KEY (`CMPNY_CD`, `SITE_CD`, `CLOSE_YM`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='근태 마감 상태 (회사+사업장+월)';

-- ----------------------------------------------------------------------------
-- 2) 근태 마감 이력 테이블 (마감/해제 액션 누적 — §13.4 마감 이력 조회)
-- ----------------------------------------------------------------------------
CREATE TABLE `tb_attd_close_hist` (
    `HIST_ID`        varchar(20)  NOT NULL COMMENT '이력 ID (FNC_CMM_SEQ_NEXTVAL — 회사별 시퀀스)',
    `CMPNY_CD`       varchar(50)  NOT NULL COMMENT '회사 코드',
    `SITE_CD`        varchar(50)  NOT NULL COMMENT '사업장 코드',
    `CLOSE_YM`       char(6)      NOT NULL COMMENT '마감 기준월 (YYYYMM)',
    `ACTION_TYPE`    varchar(10)  NOT NULL COMMENT '액션 (CLOSE 마감 / UNCLOSE 해제)',
    `ACTION_USER_CD` varchar(20)           DEFAULT NULL COMMENT '액션 수행자',
    `ACTION_DTIME`   datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '액션 일시',
    `ACTION_DESC`    varchar(500)          DEFAULT NULL COMMENT '액션 사유',
    `INSERT_NO`      varchar(50)           DEFAULT 'SYSTEM' COMMENT '입력자',
    `INSERT_DATE`    datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
    -- HIST_ID는 회사별 시퀀스라 회사 경계를 포함한 복합 PK로 멀티테넌트 충돌 방지
    PRIMARY KEY (`CMPNY_CD`, `HIST_ID`),
    KEY `IX_TB_ATTD_CLOSE_HIST` (`CMPNY_CD`, `SITE_CD`, `CLOSE_YM`, `ACTION_DTIME`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='근태 마감/해제 이력';
