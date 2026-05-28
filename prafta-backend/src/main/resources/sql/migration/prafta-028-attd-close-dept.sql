-- ============================================================================
-- PRAFTA-028 — 근태 마감 부서(노드) 단위 확장
-- 작성일: 2026-05-25
-- 적용 환경: MySQL 8.0.42
-- 참조: .claude/requests/prafta-028.md (부서별 마감 / 하위부서 cascade / 조회 스코프 기준 마감)
--
-- 기존 마감 단위(회사+사업장+월)에 NODE_CD(부서)·INC_SUB_YN(하위부서 포함) 차원을 추가한다.
--   - 부서 선택 후 마감: NODE_CD = 해당 부서, INC_SUB_YN = 하위부서 조회 체크값(Y/N).
--   - 부서 없이(전체) 마감: NODE_CD = '*' (전체 사업장), INC_SUB_YN = 'Y'.
--   - 마감 판정(isClosed)은 사용자 소속부서가 마감행의 자기자신/(INC_SUB_YN='Y'인) 상위노드/'*' 에
--     포함되면 마감으로 본다.
--
-- 멱등성: 컬럼/PK 중복 변경 시 에러. 이미 반영된 환경에서는 건너뛸 것.
-- 기존 데이터: 기존 마감행은 전체('*') 마감으로 간주하여 백필한다.
-- ============================================================================

-- 1) TB_ATTD_CLOSE — NODE_CD / INC_SUB_YN 추가 후 PK 재구성
ALTER TABLE `tb_attd_close`
    ADD COLUMN `NODE_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '*'
        COMMENT '마감 대상 부서 노드 (전체 사업장 마감은 ''*'')' AFTER `SITE_CD`,
    ADD COLUMN `INC_SUB_YN` varchar(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'Y'
        COMMENT '하위부서 포함 여부 (Y 포함 / N 해당부서만)' AFTER `NODE_CD`;

-- 기존 행은 전체 마감으로 간주 (NODE_CD='*' 는 DEFAULT 로 이미 채워짐)
UPDATE `tb_attd_close` SET `NODE_CD` = '*', `INC_SUB_YN` = 'Y' WHERE `NODE_CD` IS NULL OR `NODE_CD` = '';

-- PK 재구성: (회사+사업장+월) → (회사+사업장+부서+월)
ALTER TABLE `tb_attd_close`
    DROP PRIMARY KEY,
    ADD PRIMARY KEY (`CMPNY_CD`, `SITE_CD`, `NODE_CD`, `CLOSE_YM`);

-- 2) TB_ATTD_CLOSE_HIST — 마감 스코프(부서/하위포함) 기록 컬럼 추가 (PK 불변)
ALTER TABLE `tb_attd_close_hist`
    ADD COLUMN `NODE_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '*'
        COMMENT '마감 대상 부서 노드 (전체 사업장 마감은 ''*'')' AFTER `CLOSE_YM`,
    ADD COLUMN `INC_SUB_YN` varchar(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'Y'
        COMMENT '하위부서 포함 여부 (Y 포함 / N 해당부서만)' AFTER `NODE_CD`;
