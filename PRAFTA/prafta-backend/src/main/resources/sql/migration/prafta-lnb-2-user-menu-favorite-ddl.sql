-- ============================================================================
-- PRAFTA-LNB-2 — 사용자별 LNB 즐겨찾기 영속화 테이블 (TB_USER_MENU_FAVORITE)
-- 작성일: 2026-06-25
-- 적용 환경: MySQL 8.0.42
-- 출처: .claude/requests/web_requests/mene_lnb/lnb-restructure-작업지시서.md §4-3 (D6)
--
-- 목적:
--   관리자 웹 LNB 에서 사용자가 별표(즐겨찾기)한 메뉴를 영구 저장한다.
--   재로그인/기기변경에도 동기화되며, "즐겨찾기" 그룹으로 승격 노출(D6).
--   별표 해제 시 행 DELETE(단순). USE_YN 토글 없음.
--
-- 컬럼 타입 정합(실 DB 스키마 기준):
--   - CMPNY_CD : varchar(50)  (tb_user.CMPNY_CD 와 동일)
--   - USER_CD  : varchar(20)  (tb_user.USER_CD  와 동일)
--   - MENU_D_ID: varchar(50)  (tb_syst_menu_d.MENU_D_ID 와 동일)
--   ※ FK 제약은 명시하지 않음(prafta 관례상 tb_syst_menu_d/tb_user 에 명시 FK 미사용).
--     무결성은 애플리케이션 레이어에서 보장(즐겨찾기 토글 시 MENU_D_ID 유효성·USER_CD=JWT).
--
-- PK: (CMPNY_CD, USER_CD, MENU_D_ID) — 사용자 1명이 한 메뉴를 중복 별표 불가.
--
-- 보안(클러스터 B 백엔드에서 보장, 참고):
--   - USER_CD 는 JWT 도출만 신뢰(클라 입력 금지) → IDOR 방지.
--
-- 멱등성: CREATE TABLE IF NOT EXISTS — 재실행 안전.
-- ============================================================================

-- ----------------------------------------------------------------------------
-- [사전 검증 SELECT] — 주석
--   SHOW TABLES LIKE 'tb_user_menu_favorite';   -- 부재 확인(없으면 신규)
-- ----------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS `tb_user_menu_favorite` (
    `CMPNY_CD`    varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '회사코드',
    `USER_CD`     varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사용자코드(JWT 도출)',
    `MENU_D_ID`   varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '즐겨찾기 메뉴ID(tb_syst_menu_d.MENU_D_ID)',
    `INSERT_NO`   varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'SYSTEM' COMMENT '입력자',
    `INSERT_DATE` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
    PRIMARY KEY (`CMPNY_CD`, `USER_CD`, `MENU_D_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='사용자별 LNB 즐겨찾기(메뉴 별표 영속화)';

-- ============================================================================
-- [적용 후 검증 SELECT] — 주석
--   DESCRIBE tb_user_menu_favorite;
--   SHOW CREATE TABLE tb_user_menu_favorite;
-- ============================================================================
-- 끝.
-- ============================================================================
