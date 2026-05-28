-- ============================================================================
-- PRAFTA-020 — 연차 결재라인 프리셋 (사용자별 명명 프리셋 + 기본 지정)
-- 작성일: 2026-05-23
-- 적용 환경: MySQL 8.0.42
-- 참조: .claude/requests/prafta-020.md (USER_04 화면 수정 §5 구조 변경)
--
-- 변경 요약
--  1) tb_aprv_line_preset 신규 — 로그인 사용자 본인 소유의 명명 결재라인 프리셋(마스터).
--     - 사용자별 여러 개 + PRESET_NM(이름) + DEFAULT_YN(기본 프리셋, 사용자당 최대 1개).
--  2) tb_aprv_line_preset_d 신규 — 프리셋별 결재 순서(스텝 = 지정 결재자).
--
-- 소유/스코프: (CMPNY_CD, USER_CD) 본인 소유. 신청 시 본인 프리셋을 재사용(기본 프리셋 자동 로드).
-- 멱등성: CREATE TABLE 중복 실행 시 에러. 이미 반영된 환경에서는 건너뛸 것.
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1) 결재라인 프리셋 마스터
-- ----------------------------------------------------------------------------
CREATE TABLE `tb_aprv_line_preset` (
    `CMPNY_CD`    varchar(50)  NOT NULL COMMENT '회사 코드',
    `PRESET_ID`   varchar(20)  NOT NULL COMMENT '프리셋 ID (회사별 채번: P + YYYYMMDD + SEQ)',
    `USER_CD`     varchar(20)  NOT NULL COMMENT '소유 사용자 (본인 프리셋)',
    `PRESET_NM`   varchar(100) NOT NULL COMMENT '프리셋 이름',
    `DEFAULT_YN`  char(1)      NOT NULL DEFAULT 'N' COMMENT '기본 프리셋 여부 (사용자당 최대 1개)',
    `USE_YN`      char(1)      NOT NULL DEFAULT 'Y' COMMENT '사용 여부',
    `INSERT_NO`   varchar(50)           DEFAULT 'SYSTEM' COMMENT '입력자',
    `INSERT_DATE` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
    `UPDATE_NO`   varchar(50)           DEFAULT NULL COMMENT '수정자',
    `UPDATE_DATE` datetime              DEFAULT NULL COMMENT '수정일시',
    PRIMARY KEY (`CMPNY_CD`, `PRESET_ID`),
    KEY `IX_TB_APRV_LINE_PRESET_OWNER` (`CMPNY_CD`, `USER_CD`, `USE_YN`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='연차 결재라인 프리셋 (사용자별 마스터)';

-- ----------------------------------------------------------------------------
-- 2) 결재라인 프리셋 디테일 (결재 순서)
-- ----------------------------------------------------------------------------
CREATE TABLE `tb_aprv_line_preset_d` (
    `CMPNY_CD`         varchar(50) NOT NULL COMMENT '회사 코드',
    `PRESET_ID`        varchar(20) NOT NULL COMMENT '프리셋 ID (tb_aprv_line_preset.PRESET_ID)',
    `STEP_NO`          int         NOT NULL COMMENT '결재 단계 순서 (1부터)',
    `APPROVER_USER_CD` varchar(20) NOT NULL COMMENT '지정 결재자',
    `INSERT_NO`        varchar(50)          DEFAULT 'SYSTEM' COMMENT '입력자',
    `INSERT_DATE`      datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
    `UPDATE_NO`        varchar(50)          DEFAULT NULL COMMENT '수정자',
    `UPDATE_DATE`      datetime             DEFAULT NULL COMMENT '수정일시',
    PRIMARY KEY (`CMPNY_CD`, `PRESET_ID`, `STEP_NO`),
    KEY `IX_TB_APRV_LINE_PRESET_D_APPROVER` (`APPROVER_USER_CD`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='연차 결재라인 프리셋 디테일 (결재 순서)';
