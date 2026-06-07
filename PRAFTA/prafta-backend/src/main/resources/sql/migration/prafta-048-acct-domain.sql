-- ============================================================================
-- PRAFTA-048 — 사고관리(Acct Management) 도메인 운영 적용 마이그레이션
-- 작성일: 2026-06-06
-- 적용 환경: MySQL 8.0.42
-- 설계: .claude/context/accident-management-design.md
-- 작업지시서: .claude/requests/web_requests/prafta-048-plan.md (§2)
--
-- 적용 범위(웹 PRAFTA-048-01):
--   · 신규 테이블 4종
--       - tb_acct                   (사고 헤더)
--       - tb_acct_link              (연계 데이터 스냅샷, 도메인당 N행)
--       - tb_acct_legal_step        (법정 처리/기한 진행상태)
--       - tb_acct_legal_step_master (등급별 법정절차 정의 — seed 는 별도 파일 048-02)
--   · 코드그룹 SYS065 재해등급 / SYS066 사고 처리상태 / SYS067 연계 도메인 구분
--   · 대메뉴 acct(사고관리, IDX=8) + 소메뉴 Acct_01 + 권한 9종
--
-- ⚠️ 스키마 주의(검증됨): tb_syst_val_m / tb_syst_val_d / tb_syst_menu_m / tb_syst_menu_d 에는
--    CMPNY_CD 컬럼이 없다(전사 공통). 권한 tb_syst_auth_menu 만 CMPNY_CD='001' 단일.
--    멀티테넌트면 권한 행을 회사별로 추가 필요.
--
-- ⚠️ 재해자유형(tb_acct.VICTIM_USER_TYPE_CD)은 SYS050(REGULAR/DAILY) 재사용.
--    SYS050 코드값 REGULAR/DAILY 는 TBM 출결(Tbm04Mapper.xml 의 USER_TYPE_CD = SYS050.SYST_VAL_D_CD
--    직접 조인)로 검증됨 → 별도 코드그룹 신설 불필요.
--
-- ⚠️ DEADLINE_RULE_CD 는 코드그룹(SYS068) 미신설. 상수 문자열(IMMEDIATE/MONTH_PLUS_1/NONE/TRACK)을
--    사용하며 기한 D-day 계산은 프론트에서 수행한다(작업지시서 §2.4, 잔여확인 C).
--
-- ⚠️ tb_acct_link 의 JSON 컬럼(LINK_KEY_JSON/SNAPSHOT_JSON)은 작업지시서 권고대로 text 사용
--    (네이티브 JSON 타입 미사용, 잔여확인 B).
--
-- 적용 전 부재 확인(이미 일부 반영된 환경이면 해당 구문만 건너뛸 것):
--   SELECT 1 FROM information_schema.tables WHERE table_name='tb_acct';
--   SELECT SYST_VAL_CD FROM tb_syst_val_m WHERE SYST_VAL_CD IN ('SYS065','SYS066','SYS067');
--   SELECT SYST_VAL_D_CD FROM tb_syst_val_d WHERE SYST_VAL_CD='SYS050'; -- REGULAR/DAILY 존재 확인
--   SELECT MENU_M_ID FROM tb_syst_menu_m WHERE MENU_M_ID='acct';
-- 멱등성: PK 중복 시 에러. 운영 적용 후 보관용(재실행 금지).
-- 권장: DDL(자동 커밋) → 코드/메뉴/권한 INSERT 순으로 단계 실행 후 검증.
-- 절차 seed(tb_acct_legal_step_master)는 prafta-048-acct-legal-step-seed.sql 로 분리(노무사 게이트).
-- ============================================================================

-- ----------------------------------------------------------------------------
-- (1) tb_acct — 사고 헤더
-- ----------------------------------------------------------------------------
CREATE TABLE `tb_acct` (
    `CMPNY_CD`             varchar(50)   NOT NULL COMMENT '회사코드',
    `SITE_CD`             varchar(50)   NOT NULL COMMENT '사업장코드',
    `ACCT_ID`         varchar(20)   NOT NULL COMMENT '사고 ID (사업장별 채번: ACC + YYYYMMDD + SEQ4)',
    `VICTIM_USER_TYPE_CD` varchar(10)   NOT NULL COMMENT '재해자 사용자유형[SYS050] REGULAR:정규 DAILY:일용',
    `VICTIM_USER_CD`      varchar(20)   NOT NULL COMMENT '재해자 사용자코드(tb_user.USER_CD 또는 tb_daily_user.USER_CD)',
    `OCCUR_YMD`           varchar(8)    NOT NULL COMMENT '사고 발생일(YYYYMMDD)',
    `OCCUR_TIME`          varchar(4)    NOT NULL COMMENT '발생 시각(HHMM)',
    `OCCUR_PLACE`         varchar(200)           DEFAULT NULL COMMENT '발생 장소(직접입력)',
    `ACCT_GRADE_CD`   varchar(10)   NOT NULL COMMENT '재해등급[SYS065] 100:중대재해 200:일반산재 300:신고제외',
    `ACCT_DESC`       varchar(1000) NOT NULL COMMENT '사고 경위',
    `EMPLOYER_DESC`       varchar(200)           DEFAULT NULL COMMENT '신고의무자(직영/하수급 등 직접입력)',
    `PROCESS_STATUS_CD`   varchar(10)   NOT NULL DEFAULT '100' COMMENT '처리상태[SYS066] 100:접수 200:처리중 300:종결',
    `USE_YN`              varchar(2)    NOT NULL DEFAULT 'Y' COMMENT '사용여부[SYS003]',
    `DEL_YN`              varchar(1)    NOT NULL DEFAULT 'N' COMMENT '삭제여부',
    `INSERT_NO`           varchar(50)            DEFAULT 'SYSTEM' COMMENT '입력자',
    `INSERT_DATE`         datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
    `UPDATE_NO`           varchar(50)            DEFAULT NULL COMMENT '수정자',
    `UPDATE_DATE`         datetime               DEFAULT NULL COMMENT '수정일시',
    PRIMARY KEY (`CMPNY_CD`, `SITE_CD`, `ACCT_ID`),
    KEY `IX_TB_ACCT_GRADE`  (`CMPNY_CD`, `SITE_CD`, `ACCT_GRADE_CD`),
    KEY `IX_TB_ACCT_STATUS` (`CMPNY_CD`, `SITE_CD`, `PROCESS_STATUS_CD`),
    KEY `IX_TB_ACCT_OCCUR`  (`CMPNY_CD`, `SITE_CD`, `OCCUR_YMD`),
    KEY `IX_TB_ACCT_VICTIM` (`CMPNY_CD`, `VICTIM_USER_CD`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='사고관리 헤더';

-- ----------------------------------------------------------------------------
-- (2) tb_acct_link — 연계 데이터 스냅샷 (도메인당 N행)
--     LINK_KEY_JSON(원본키) + SNAPSHOT_JSON(확정 시점 값 고정). 법적 정합성 보존.
-- ----------------------------------------------------------------------------
CREATE TABLE `tb_acct_link` (
    `CMPNY_CD`        varchar(50)  NOT NULL COMMENT '회사코드',
    `SITE_CD`         varchar(50)  NOT NULL COMMENT '사업장코드',
    `ACCT_ID`     varchar(20)  NOT NULL COMMENT '사고 ID(tb_acct.ACCT_ID)',
    `LINK_DOMAIN_CD`  varchar(20)  NOT NULL COMMENT '연계도메인[SYS067] ATTD:근태 CHKPT:순회점검 RISK:위험성평가 TBM:TBM NEAR_MISS:아차사고',
    `LINK_SEQ`        int          NOT NULL COMMENT '도메인 내 확정 순번(다건)',
    `LINK_KEY_JSON`   text                  DEFAULT NULL COMMENT '연결 원본키 묶음(JSON 문자열; 예 {"chkptCd":"...","workDate":"..."} )',
    `SNAPSHOT_JSON`   text                  DEFAULT NULL COMMENT '확정 시점 조회값 고정(JSON 문자열; 사고 날짜·시각 기준 스냅샷)',
    `INSERT_NO`       varchar(50)           DEFAULT 'SYSTEM' COMMENT '입력자',
    `INSERT_DATE`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
    `UPDATE_NO`       varchar(50)           DEFAULT NULL COMMENT '수정자',
    `UPDATE_DATE`     datetime              DEFAULT NULL COMMENT '수정일시',
    PRIMARY KEY (`CMPNY_CD`, `SITE_CD`, `ACCT_ID`, `LINK_DOMAIN_CD`, `LINK_SEQ`),
    KEY `IX_TB_ACCT_LINK_DOMAIN` (`CMPNY_CD`, `SITE_CD`, `ACCT_ID`, `LINK_DOMAIN_CD`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='사고 연계 데이터 스냅샷';

-- ----------------------------------------------------------------------------
-- (3) tb_acct_legal_step — 법정 처리/기한 진행상태 (탭②)
--     master 절차에 대한 조치완료 체크/비고. lazy UPSERT(저장 시에만 행 생성).
-- ----------------------------------------------------------------------------
CREATE TABLE `tb_acct_legal_step` (
    `CMPNY_CD`     varchar(50)  NOT NULL COMMENT '회사코드',
    `SITE_CD`      varchar(50)  NOT NULL COMMENT '사업장코드',
    `ACCT_ID`  varchar(20)  NOT NULL COMMENT '사고 ID(tb_acct.ACCT_ID)',
    `STEP_CD`      varchar(20)  NOT NULL COMMENT '절차코드(tb_acct_legal_step_master.STEP_CD)',
    `IS_DONE_YN`   varchar(2)   NOT NULL DEFAULT 'N' COMMENT '조치완료여부(Y/N) — 처리버튼→체크 방식',
    `DONE_DTIME`   datetime              DEFAULT NULL COMMENT '조치완료 처리일시',
    `DONE_USER_CD` varchar(20)           DEFAULT NULL COMMENT '조치완료 처리자(tb_user.USER_CD)',
    `REMARK`       varchar(500)          DEFAULT NULL COMMENT '항목별 비고',
    `INSERT_NO`    varchar(50)           DEFAULT 'SYSTEM' COMMENT '입력자',
    `INSERT_DATE`  datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
    `UPDATE_NO`    varchar(50)           DEFAULT NULL COMMENT '수정자',
    `UPDATE_DATE`  datetime              DEFAULT NULL COMMENT '수정일시',
    PRIMARY KEY (`CMPNY_CD`, `SITE_CD`, `ACCT_ID`, `STEP_CD`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='사고 법정 처리/기한 진행상태';

-- ----------------------------------------------------------------------------
-- (4) tb_acct_legal_step_master — 등급별 법정절차 정의(seed 마스터)
--     CMPNY_CD 없음(전국 공통 법령 기준, 전사 공통). 행 데이터는 048-02 에서 INSERT.
-- ----------------------------------------------------------------------------
CREATE TABLE `tb_acct_legal_step_master` (
    `STEP_CD`            varchar(20)  NOT NULL COMMENT '절차코드(전사 공통)',
    `ACCT_GRADE_CD`  varchar(10)  NOT NULL COMMENT '적용 재해등급[SYS065] 100/200/300, 또는 ALL(전등급 공통)',
    `STEP_IDX`           int          NOT NULL COMMENT '절차 표시 순서',
    `STEP_NM`            varchar(100) NOT NULL COMMENT '절차명(예: 중대재해 발생보고)',
    `ACTION_GUIDE`       varchar(500) NOT NULL COMMENT '행동강령 문구(관리자 가이드)',
    `LEGAL_BASIS`        varchar(300)          DEFAULT NULL COMMENT '근거조문/과태료',
    `DEADLINE_RULE_CD`   varchar(20)  NOT NULL COMMENT '기한규칙(상수) IMMEDIATE:지체없이 MONTH_PLUS_1:발생일+1개월(산안법 시행규칙§73) NONE:기한없음 TRACK:별도트랙',
    `STEP_NOTE`          varchar(500)          DEFAULT NULL COMMENT '추가 안내(예: 시스템이 기한 계산 안 함)',
    `USE_YN`             varchar(2)   NOT NULL DEFAULT 'Y' COMMENT '사용여부[SYS003]',
    `INSERT_NO`          varchar(50)           DEFAULT 'SYSTEM' COMMENT '입력자',
    `INSERT_DATE`        datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
    `UPDATE_NO`          varchar(50)           DEFAULT NULL COMMENT '수정자',
    `UPDATE_DATE`        datetime              DEFAULT NULL COMMENT '수정일시',
    PRIMARY KEY (`STEP_CD`),
    KEY `IX_TB_ACCT_STEP_MASTER_GRADE` (`ACCT_GRADE_CD`, `STEP_IDX`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='사고 법정절차 정의(seed)';

-- ----------------------------------------------------------------------------
-- (5) 코드그룹 마스터 (tb_syst_val_m) — SYS065~067 (전사 공통, CMPNY_CD 없음)
-- ----------------------------------------------------------------------------
INSERT INTO `tb_syst_val_m` (`SYST_VAL_CD`, `SYST_VAL_NM`, `USE_YN`, `VAL_DESC`, `INSERT_NO`) VALUES
    ('SYS065', '재해등급',           'Y', 'tb_acct.ACCT_GRADE_CD 코드',     'SYSTEM')
  , ('SYS066', '사고 처리상태',       'Y', 'tb_acct.PROCESS_STATUS_CD 코드',     'SYSTEM')
  , ('SYS067', '사고 연계도메인 구분', 'Y', 'tb_acct_link.LINK_DOMAIN_CD 코드',   'SYSTEM');

-- ----------------------------------------------------------------------------
-- (6) 코드그룹 상세 (tb_syst_val_d)
-- ----------------------------------------------------------------------------
-- SYS065 재해등급
INSERT INTO `tb_syst_val_d`
    (`SYST_VAL_CD`, `SYST_VAL_D_CD`, `SYST_VAL_D_NM`, `SORT_IDX`, `USE_YN`, `INSERT_NO`) VALUES
    ('SYS065', '100', '중대재해', 1, 'Y', 'SYSTEM')
  , ('SYS065', '200', '일반산재', 2, 'Y', 'SYSTEM')
  , ('SYS065', '300', '신고제외', 3, 'Y', 'SYSTEM');

-- SYS066 사고 처리상태
INSERT INTO `tb_syst_val_d`
    (`SYST_VAL_CD`, `SYST_VAL_D_CD`, `SYST_VAL_D_NM`, `SORT_IDX`, `USE_YN`, `INSERT_NO`) VALUES
    ('SYS066', '100', '접수',   1, 'Y', 'SYSTEM')
  , ('SYS066', '200', '처리중', 2, 'Y', 'SYSTEM')
  , ('SYS066', '300', '종결',   3, 'Y', 'SYSTEM');

-- SYS067 사고 연계도메인 구분
INSERT INTO `tb_syst_val_d`
    (`SYST_VAL_CD`, `SYST_VAL_D_CD`, `SYST_VAL_D_NM`, `SORT_IDX`, `USE_YN`, `INSERT_NO`) VALUES
    ('SYS067', 'ATTD',      '근태',       1, 'Y', 'SYSTEM')
  , ('SYS067', 'CHKPT',     '순회점검',   2, 'Y', 'SYSTEM')
  , ('SYS067', 'RISK',      '위험성평가', 3, 'Y', 'SYSTEM')
  , ('SYS067', 'TBM',       'TBM',        4, 'Y', 'SYSTEM')
  , ('SYS067', 'NEAR_MISS', '아차사고',   5, 'Y', 'SYSTEM');

-- ----------------------------------------------------------------------------
-- (7) 대메뉴 등록 (tb_syst_menu_m) — acct, 웹[SYS007 '001'], IDX=8 (nearMiss=7 다음)
-- ----------------------------------------------------------------------------
INSERT INTO `tb_syst_menu_m` (`MENU_M_ID`, `MENU_SRC`, `MENU_NM`, `MENU_IDX`, `USE_YN`, `INSERT_NO`, `INSERT_DATE`) VALUES
    ('acct', '001', '사고관리', 8, 'Y', 'SYSTEM', NOW());

-- ----------------------------------------------------------------------------
-- (8) 소메뉴 등록 (tb_syst_menu_d) — Acct_01
--     MENU_VIEW = views 하위 상대경로. viewResolver가 컴포넌트명으로 자동 라우팅.
-- ----------------------------------------------------------------------------
INSERT INTO `tb_syst_menu_d` (`MENU_D_ID`, `MENU_M_ID`, `MENU_VIEW`, `MENU_NM`, `MENU_IDX`, `USE_YN`, `INSERT_NO`, `INSERT_DATE`) VALUES
    ('Acct_01', 'acct', 'acct/Acct_01.vue', '사고관리', 1, 'Y', 'SYSTEM', NOW());

-- ----------------------------------------------------------------------------
-- (9) 권한 매핑 (tb_syst_auth_menu) — CMPNY_CD='001' 단일
--     · 웹에서 직접 사고 등록(+사고 등록 버튼) → master/hr/safe/system 에 BTN_NEW='Y'.
--     · 삭제(BTN_DELT)는 master/system 만(설계 §7).
--     · 엑셀(BTN_EXCL)은 master/system 만(MenuLockPolicy 보정 정합, 아래 주석 참조).
--     · 일반 역할(00001/00004/00006/00008/99999)은 SRCH 만(열람).
--
--     ⚠️ MenuLockPolicy(prafta-042 SSOT) 정합:
--       MenuLockPolicy.isLockedMenu(master, *)=true → master 는 모든 대메뉴의 USE_YN+모든 BTN_*
--       를 항상 'Y' 로 강제 보정한다(User02ServiceImpl.updateAuthMenuInfo). 따라서 master 행은
--       BTN_DELT/BTN_EXCL 포함 전부 'Y' 로 시드한다(작업지시서 §2.7 의 master EXCL='N' 은 정책에
--       위배되므로 정책 우선 → 'Y' 로 보정). 'acct' 는 HR/SAFE 잠금셋(MenuLockPolicy)에 없으므로
--       hr/safe 는 강제 보정 대상이 아니며 아래 시드값이 그대로 유지된다(hr/safe BTN_NEW='Y' 유효).
-- ----------------------------------------------------------------------------
INSERT INTO `tb_syst_auth_menu`
    (`CMPNY_CD`, `AUTH_CD`, `MENU_D_ID`, `USE_YN`, `BTN_SRCH`, `BTN_NEW`, `BTN_DELT`, `BTN_SAVE`, `BTN_EXCL`, `INSERT_NO`, `INSERT_DATE`) VALUES
    ('001', 'master', 'Acct_01', 'Y', 'Y', 'Y', 'Y', 'Y', 'Y', 'SYSTEM', NOW())
  , ('001', 'hr',     'Acct_01', 'Y', 'Y', 'Y', 'N', 'Y', 'N', 'SYSTEM', NOW())
  , ('001', 'safe',   'Acct_01', 'Y', 'Y', 'Y', 'N', 'Y', 'N', 'SYSTEM', NOW())
  , ('001', 'system', 'Acct_01', 'Y', 'Y', 'Y', 'Y', 'Y', 'Y', 'SYSTEM', NOW())
  , ('001', '00001',  'Acct_01', 'Y', 'Y', 'N', 'N', 'N', 'N', 'SYSTEM', NOW())
  , ('001', '00004',  'Acct_01', 'Y', 'Y', 'N', 'N', 'N', 'N', 'SYSTEM', NOW())
  , ('001', '00006',  'Acct_01', 'Y', 'Y', 'N', 'N', 'N', 'N', 'SYSTEM', NOW())
  , ('001', '00008',  'Acct_01', 'Y', 'Y', 'N', 'N', 'N', 'N', 'SYSTEM', NOW())
  , ('001', '99999',  'Acct_01', 'Y', 'Y', 'N', 'N', 'N', 'N', 'SYSTEM', NOW());

-- ============================================================================
-- 끝. 적용 후 검증:
--   SELECT COUNT(*) FROM information_schema.tables WHERE table_name LIKE 'tb_acct%'; -- 4
--   SELECT SYST_VAL_D_CD FROM tb_syst_val_d WHERE SYST_VAL_CD='SYS065'; -- 3건
--   SELECT MENU_NM FROM tb_syst_menu_m WHERE MENU_M_ID='acct';      -- 사고관리
--   SELECT COUNT(*) FROM tb_syst_auth_menu WHERE MENU_D_ID='Acct_01'; -- 9
-- 다음 단계: prafta-048-acct-legal-step-seed.sql 적용(노무사 검토 게이트 후).
-- ============================================================================
