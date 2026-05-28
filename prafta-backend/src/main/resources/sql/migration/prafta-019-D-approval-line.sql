-- ============================================================================
-- PRAFTA-019-D — 연차 요청별 결재라인 (사용자 정의 라인)
-- 작성일: 2026-05-23
-- 적용 환경: MySQL 8.0.42
-- 참조: .claude/requests/prafta-019-D-approval-line.md, prafta-019-plan.md
--       재기획서 §9.1(ApprovalRequest)·§9.2(approvalSteps), 근태 §9.5
--
-- 변경 요약
--  1) tb_user_attd_req_approval 신규 — 사용자가 신청 시 직접 구성한 결재라인(스텝 = 지정 결재자).
--     (초안의 NODE 상향 자동추적 폐기 — 결정 #1)
--  2) SYS044 신규 코드그룹 — 결재 단계 상태(00 대기중/01 신청/02 승인/03 반려).
--     ⚠️ 계획서는 SYS043을 제안했으나 SYS043은 prafta-017-2 "연차 부여 방식"이 이미 점유.
--        SYS040~043 점유 → 다음 가용 SYS044 사용.
--
-- 멱등성: CREATE TABLE / INSERT 중복 실행 시 에러. 이미 반영된 환경에서는 건너뛸 것.
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1) 결재라인 저장 테이블
-- ----------------------------------------------------------------------------
-- PK는 (REQ_ID, APPROVAL_STEP) 자연키. REQ_ID(tb_user_attd_req.REQ_ID)는 전역 유일하므로
-- 별도 APPROVAL_ID 채번(회사별 시퀀스 → 멀티테넌트 충돌 위험) 없이 단계가 유일하게 식별된다.
CREATE TABLE `tb_user_attd_req_approval` (
    `REQ_ID`           varchar(20)  NOT NULL COMMENT '연관 요청 (tb_user_attd_req.REQ_ID)',
    `APPROVAL_STEP`    int          NOT NULL COMMENT '결재 단계 (1부터, 사용자가 구성한 순서)',
    `CMPNY_CD`         varchar(50)  NOT NULL COMMENT '회사 코드',
    `APPROVER_USER_CD` varchar(20)  NOT NULL COMMENT '사용자가 지정한 결재자 (NODE 파생 아님)',
    `APPROVAL_STATUS`  varchar(2)   NOT NULL DEFAULT '00' COMMENT '단계 상태 [SYS044] 00대기/01신청/02승인/03반려',
    `APPROVAL_COMMENT` varchar(500)          DEFAULT NULL COMMENT '결재 코멘트',
    `APPROVAL_DATE`    datetime              DEFAULT NULL COMMENT '처리 일시',
    `INSERT_NO`        varchar(50)           DEFAULT 'SYSTEM' COMMENT '입력자',
    `INSERT_DATE`      datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
    `UPDATE_NO`        varchar(50)           DEFAULT NULL COMMENT '수정자',
    `UPDATE_DATE`      datetime              DEFAULT NULL COMMENT '수정일시',
    PRIMARY KEY (`REQ_ID`, `APPROVAL_STEP`),
    KEY `IX_TB_USER_ATTD_REQ_APPROVAL_APPROVER` (`APPROVER_USER_CD`, `APPROVAL_STATUS`),
    KEY `IX_TB_USER_ATTD_REQ_APPROVAL_REQ` (`CMPNY_CD`, `REQ_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='연차 요청별 결재라인 (사용자 정의)';

-- ----------------------------------------------------------------------------
-- 2) SYS044 — 결재 단계 상태 코드그룹
-- ----------------------------------------------------------------------------
INSERT INTO tb_syst_val_m (SYST_VAL_CD, SYST_VAL_NM, USE_YN, INSERT_NO, INSERT_DATE) VALUES
    ('SYS044', '결재 단계 상태', 'Y', 'SYSTEM', NOW());

INSERT INTO tb_syst_val_d (SYST_VAL_CD, SYST_VAL_D_CD, SYST_VAL_D_NM, SORT_IDX, USE_YN, INSERT_NO, INSERT_DATE) VALUES
    ('SYS044', '00', '대기중', 1, 'Y', 'SYSTEM', NOW())
  , ('SYS044', '01', '신청',   2, 'Y', 'SYSTEM', NOW())
  , ('SYS044', '02', '승인',   3, 'Y', 'SYSTEM', NOW())
  , ('SYS044', '03', '반려',   4, 'Y', 'SYSTEM', NOW());
