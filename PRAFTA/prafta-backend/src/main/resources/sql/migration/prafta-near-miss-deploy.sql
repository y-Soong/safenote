-- ============================================================================
-- PRAFTA — 아차사고/사건 보고 도메인 운영 적용 통합 마이그레이션
-- 작성일: 2026-05-31
-- 적용 환경: MySQL 8.0.42
-- 통합 출처(아래 4종을 하나로 합침 — 운영 1회 적용용):
--   1) prafta-040-near-miss.sql            (웹: tb_near_miss + SYS061~063 + SYS011 005 + 메뉴 + 권한 + REJECT_REASON)
--   2) prafta-app-012-near-miss-admin-action.sql (앱: ADMIN_TEMP_ACTION_DESC 컬럼 → 본 통합에선 CREATE에 직접 포함)
--   3) prafta-app-012-sys045-near-miss.sql  (앱: SYS045 NEAR_MISS_REPORTED 알림유형)
--   4) prafta-app-012-sys010-filetype.sql   (앱: SYS010 004 아차사고 파일타입)
-- 설계: .claude/context/near-miss-incident-design.md
--
-- 적용 범위(웹 PRAFTA-040 + 앱 PRAFTA-app-012 공용):
--   · 신규 테이블 tb_near_miss (REJECT_REASON·ADMIN_TEMP_ACTION_DESC 포함)
--   · 코드그룹 SYS061 사건유형 / SYS062 잠재중대성 / SYS063 처리상태
--   · SYS011(위험성평가진행단계) 005 '아차사고로 이관' (재분류 D2)
--   · SYS045 NEAR_MISS_REPORTED (푸시 유형; 마스터는 prafta-031에서 시드됨 → 디테일만)
--   · SYS010 004 '아차사고' (보고 사진 FILE_TYPE; 현행 001/002/003 — '004' 미사용 확인됨)
--   · 대메뉴 nearMiss(사건관리, IDX=7) + 소메뉴 NearMiss_01 + 권한(Risk_03 동일 정책)
--
-- ⚠️ 스키마 주의(검증됨): tb_syst_val_m / tb_syst_val_d / tb_syst_menu_m / tb_syst_menu_d 에는
--    CMPNY_CD 컬럼이 없다(전사 공통). 권한 tb_syst_auth_menu 만 CMPNY_CD='001' 단일.
--    멀티테넌트면 권한 행을 회사별로 추가 필요.
--
-- 적용 전 부재 확인(이미 일부 반영된 환경이면 해당 구문만 건너뛸 것):
--   SELECT 1 FROM information_schema.tables WHERE table_name='tb_near_miss';
--   SELECT SYST_VAL_CD FROM tb_syst_val_m WHERE SYST_VAL_CD IN ('SYS061','SYS062','SYS063');
--   SELECT SYST_VAL_D_CD FROM tb_syst_val_d WHERE SYST_VAL_CD='SYS011' AND SYST_VAL_D_CD='005';
--   SELECT SYST_VAL_D_CD FROM tb_syst_val_d WHERE SYST_VAL_CD='SYS045' AND SYST_VAL_D_CD='NEAR_MISS_REPORTED';
--   SELECT SYST_VAL_D_CD FROM tb_syst_val_d WHERE SYST_VAL_CD='SYS010' AND SYST_VAL_D_CD='004';
--   SELECT MENU_M_ID FROM tb_syst_menu_m WHERE MENU_M_ID='nearMiss';
-- 멱등성: PK 중복 시 에러. 운영 적용 후 보관용(재실행 금지).
-- 권장: 단일 트랜잭션으로 실행(START TRANSACTION; ... COMMIT;). DDL은 자동 커밋되므로
--       tb_near_miss CREATE 후 코드/메뉴/권한 INSERT 단계를 묶어 확인할 것.
-- ============================================================================

-- ----------------------------------------------------------------------------
-- (1) tb_near_miss 신규 테이블
--     REJECT_REASON(반려 사유) + ADMIN_TEMP_ACTION_DESC(관리자 임시조치 메모) 포함.
-- ----------------------------------------------------------------------------
CREATE TABLE `tb_near_miss` (
    `CMPNY_CD`               varchar(50)  NOT NULL COMMENT '회사코드',
    `SITE_CD`                varchar(50)  NOT NULL COMMENT '사업장코드',
    `NEAR_MISS_ID`           varchar(20)  NOT NULL COMMENT '사건 ID (사업장별 채번: NM + YYYYMMDD + SEQ)',
    `INCIDENT_TYPE_CD`       varchar(10)  NOT NULL COMMENT '사건유형[SYS061] 100:아차사고 200:경미사고 300:유해·위험요인발견',
    `PROCESS_CD`             varchar(10)           DEFAULT NULL COMMENT '공정코드[COM002]',
    `OCCUR_DTIME`            datetime     NOT NULL COMMENT '발생일시',
    `LOCATION_DESC`          varchar(200)          DEFAULT NULL COMMENT '발생장소(직접입력)',
    `DESCRIPTION`            varchar(500) NOT NULL COMMENT '사건 경위(무슨 일이 있었나)',
    `POTENTIAL_SEVERITY_CD`  varchar(10)           DEFAULT NULL COMMENT '잠재적 중대성[SYS062] 100:경미 200:중대 300:치명(실제 사고였다면)',
    `IMMEDIATE_ACTION_DESC`  varchar(500)          DEFAULT NULL COMMENT '보고자 즉시 조치사항',
    `ADMIN_TEMP_ACTION_DESC` varchar(500)          DEFAULT NULL COMMENT '관리자 임시조치 메모(앱 1차확인 시 입력, 보고자 IMMEDIATE_ACTION_DESC 와 분리)',
    `CAUSE_DESC`             varchar(500)          DEFAULT NULL COMMENT '추정 원인(웹 정밀조사)',
    `PREVENTION_DESC`        varchar(500)          DEFAULT NULL COMMENT '재발방지 대책(웹 정밀조사)',
    `FILE_MGMT_CD`           varchar(50)           DEFAULT NULL COMMENT '현장 사진(tb_file_info 관리코드)',
    `REPORT_STATUS_CD`       varchar(10)  NOT NULL DEFAULT '100' COMMENT '처리상태[SYS063] 100:접수 200:검토중 300:조치중 400:완료 900:반려',
    `REPORTER_ID`            varchar(50)  NOT NULL COMMENT '보고자(tb_user.USER_CD)',
    `REPORT_DTIME`           datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '보고일시',
    `REVIEWER_ID`            varchar(50)           DEFAULT NULL COMMENT '검토 관리자(tb_user.USER_CD)',
    `REVIEW_DTIME`           datetime              DEFAULT NULL COMMENT '검토(분류)일시',
    `SRC_PROCESS_CD`         varchar(10)           DEFAULT NULL COMMENT '원 위험성평가요청 공정코드(재분류 출처)',
    `SRC_ASSESSMENT_CD`      varchar(10)           DEFAULT NULL COMMENT '원 위험성평가요청 ID(tb_risk_assessment.ASSESSMENT_CD, 재분류 출처)',
    `REJECT_REASON`          varchar(500)          DEFAULT NULL COMMENT '반려 사유(처리상태 900 반려 시 기록, 추정원인 CAUSE_DESC 와 분리)',
    `USE_YN`                 varchar(2)   NOT NULL DEFAULT 'Y' COMMENT '사용여부',
    `INSERT_NO`              varchar(50)           DEFAULT 'SYSTEM' COMMENT '입력자',
    `INSERT_DATE`            datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
    `UPDATE_NO`              varchar(50)           DEFAULT NULL COMMENT '수정자',
    `UPDATE_DATE`            datetime              DEFAULT NULL COMMENT '수정일시',
    PRIMARY KEY (`CMPNY_CD`, `SITE_CD`, `NEAR_MISS_ID`),
    KEY `IX_TB_NEAR_MISS_STATUS` (`CMPNY_CD`, `SITE_CD`, `REPORT_STATUS_CD`),
    KEY `IX_TB_NEAR_MISS_REPORTER` (`CMPNY_CD`, `REPORTER_ID`),
    KEY `IX_TB_NEAR_MISS_SRC` (`CMPNY_CD`, `SITE_CD`, `SRC_PROCESS_CD`, `SRC_ASSESSMENT_CD`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='아차사고/사건 보고';

-- ----------------------------------------------------------------------------
-- (2) 코드그룹 마스터 (tb_syst_val_m) — SYS061~063 (전사 공통, CMPNY_CD 없음)
-- ----------------------------------------------------------------------------
INSERT INTO `tb_syst_val_m` (`SYST_VAL_CD`, `SYST_VAL_NM`, `USE_YN`, `VAL_DESC`, `INSERT_NO`) VALUES
    ('SYS061', '사건유형',     'Y', 'tb_near_miss.INCIDENT_TYPE_CD 코드',      'SYSTEM')
  , ('SYS062', '잠재적 중대성', 'Y', 'tb_near_miss.POTENTIAL_SEVERITY_CD 코드', 'SYSTEM')
  , ('SYS063', '사건 처리상태', 'Y', 'tb_near_miss.REPORT_STATUS_CD 코드',      'SYSTEM');

-- ----------------------------------------------------------------------------
-- (3) 코드그룹 상세 (tb_syst_val_d)
-- ----------------------------------------------------------------------------
-- SYS061 사건유형
INSERT INTO `tb_syst_val_d`
    (`SYST_VAL_CD`, `SYST_VAL_D_CD`, `SYST_VAL_D_NM`, `SORT_IDX`, `USE_YN`, `INSERT_NO`) VALUES
    ('SYS061', '100', '아차사고',           1, 'Y', 'SYSTEM')
  , ('SYS061', '200', '경미사고',           2, 'Y', 'SYSTEM')
  , ('SYS061', '300', '유해·위험요인발견',  3, 'Y', 'SYSTEM');

-- SYS062 잠재적 중대성
INSERT INTO `tb_syst_val_d`
    (`SYST_VAL_CD`, `SYST_VAL_D_CD`, `SYST_VAL_D_NM`, `SORT_IDX`, `USE_YN`, `INSERT_NO`) VALUES
    ('SYS062', '100', '경미', 1, 'Y', 'SYSTEM')
  , ('SYS062', '200', '중대', 2, 'Y', 'SYSTEM')
  , ('SYS062', '300', '치명', 3, 'Y', 'SYSTEM');

-- SYS063 사건 처리상태
INSERT INTO `tb_syst_val_d`
    (`SYST_VAL_CD`, `SYST_VAL_D_CD`, `SYST_VAL_D_NM`, `SORT_IDX`, `USE_YN`, `INSERT_NO`) VALUES
    ('SYS063', '100', '접수',   1, 'Y', 'SYSTEM')
  , ('SYS063', '200', '검토중', 2, 'Y', 'SYSTEM')
  , ('SYS063', '300', '조치중', 3, 'Y', 'SYSTEM')
  , ('SYS063', '400', '완료',   4, 'Y', 'SYSTEM')
  , ('SYS063', '900', '반려',   9, 'Y', 'SYSTEM');

-- ----------------------------------------------------------------------------
-- (4) SYS011(위험성평가진행단계)에 코드 005 '아차사고로 이관' 추가 (재분류 D2)
--     현행 SYS011: 001 검토요청 / 002 개선예정 / 003 개선완료 / 004 미처리대상.
--     재분류(E6) 시 원 tb_risk_assessment.ASSESSMENT_STATUS='005' 로 전환(삭제/USE_YN 변경 아님).
-- ----------------------------------------------------------------------------
INSERT INTO `tb_syst_val_d`
    (`SYST_VAL_CD`, `SYST_VAL_D_CD`, `SYST_VAL_D_NM`, `SORT_IDX`, `USE_YN`, `INSERT_NO`) VALUES
    ('SYS011', '005', '아차사고로 이관', 6, 'Y', 'SYSTEM');

-- ----------------------------------------------------------------------------
-- (5) SYS045(알림 유형)에 NEAR_MISS_REPORTED 추가 (앱 푸시)
--     마스터(tb_syst_val_m SYS045)는 prafta-031 에서 이미 시드됨 → 디테일만 INSERT.
--     잠재중대성≥중대(SYS062 200/300) 신규 보고 시 사업장 안전관리자에게 PUSH.
--     consumer 미구현 → tb_noti_outbox INSERT 까지만(실발송 미연동).
-- ----------------------------------------------------------------------------
INSERT INTO `tb_syst_val_d`
    (`SYST_VAL_CD`, `SYST_VAL_D_CD`, `SYST_VAL_D_NM`, `SORT_IDX`, `USE_YN`, `VAL_D_INFO_1`, `INSERT_NO`) VALUES
    ('SYS045', 'NEAR_MISS_REPORTED', '아차사고 보고', 2, 'Y', 'PUSH', 'SYSTEM');

-- ----------------------------------------------------------------------------
-- (6) SYS010(FILE_TYPE)에 '004' 아차사고 추가 (앱 보고 사진)
--     현행 SYS010: 001 일일점검 / 002 위험성평가 / 003 TBM 교육자료 ('004' 미사용 확인됨).
--     앱 보고 등록(A1) 사진 저장 시 FILE_TYPE='004' 사용.
-- ----------------------------------------------------------------------------
INSERT INTO `tb_syst_val_d`
    (`SYST_VAL_CD`, `SYST_VAL_D_CD`, `SYST_VAL_D_NM`, `SORT_IDX`, `USE_YN`, `INSERT_NO`) VALUES
    ('SYS010', '004', '아차사고', 4, 'Y', 'SYSTEM');

-- ----------------------------------------------------------------------------
-- (7) 대메뉴 등록 (tb_syst_menu_m) — nearMiss, 웹[SYS007 '001'], IDX=7
-- ----------------------------------------------------------------------------
INSERT INTO `tb_syst_menu_m` (`MENU_M_ID`, `MENU_SRC`, `MENU_NM`, `MENU_IDX`, `USE_YN`, `INSERT_NO`, `INSERT_DATE`) VALUES
    ('nearMiss', '001', '사건관리', 7, 'Y', 'SYSTEM', NOW());

-- ----------------------------------------------------------------------------
-- (8) 소메뉴 등록 (tb_syst_menu_d) — NearMiss_01
--     MENU_VIEW = views 하위 상대경로. viewResolver가 컴포넌트명으로 자동 라우팅.
-- ----------------------------------------------------------------------------
INSERT INTO `tb_syst_menu_d` (`MENU_D_ID`, `MENU_M_ID`, `MENU_VIEW`, `MENU_NM`, `MENU_IDX`, `USE_YN`, `INSERT_NO`, `INSERT_DATE`) VALUES
    ('NearMiss_01', 'nearMiss', 'nearMiss/NearMiss_01.vue', '사건 관리', 1, 'Y', 'SYSTEM', NOW());

-- ----------------------------------------------------------------------------
-- (9) 권한 매핑 (tb_syst_auth_menu) — Risk_03 동일 정책
--     안전직군 USE_YN='Y' (조회+저장). hr='N' 차단.
--     BTN: 조회/저장만 사용. 신규/삭제/엑셀은 N(웹 직접생성 없음·물리삭제 금지·통계 추후).
--     ⚠️ 웹 사건관리 메뉴 접근 권한. (앱 백엔드 사업장 권한은 tb_user_site_auth 로 별도 검증)
-- ----------------------------------------------------------------------------
INSERT INTO `tb_syst_auth_menu`
    (`CMPNY_CD`, `AUTH_CD`, `MENU_D_ID`, `USE_YN`, `BTN_SRCH`, `BTN_NEW`, `BTN_DELT`, `BTN_SAVE`, `BTN_EXCL`, `INSERT_NO`, `INSERT_DATE`) VALUES
    ('001', '00001',  'NearMiss_01', 'Y', 'Y', 'N', 'N', 'Y', 'N', 'SYSTEM', NOW())
  , ('001', '00004',  'NearMiss_01', 'Y', 'Y', 'N', 'N', 'Y', 'N', 'SYSTEM', NOW())
  , ('001', '00006',  'NearMiss_01', 'Y', 'Y', 'N', 'N', 'Y', 'N', 'SYSTEM', NOW())
  , ('001', '00008',  'NearMiss_01', 'Y', 'Y', 'N', 'N', 'Y', 'N', 'SYSTEM', NOW())
  , ('001', '99999',  'NearMiss_01', 'Y', 'Y', 'N', 'N', 'Y', 'N', 'SYSTEM', NOW())
  , ('001', 'master', 'NearMiss_01', 'Y', 'Y', 'N', 'N', 'Y', 'N', 'SYSTEM', NOW())
  , ('001', 'safe',   'NearMiss_01', 'Y', 'Y', 'N', 'N', 'Y', 'N', 'SYSTEM', NOW())
  , ('001', 'system', 'NearMiss_01', 'Y', 'Y', 'N', 'N', 'Y', 'N', 'SYSTEM', NOW())
  , ('001', 'hr',     'NearMiss_01', 'N', 'N', 'N', 'N', 'N', 'N', 'SYSTEM', NOW());

-- ============================================================================
-- 끝. 적용 후 검증:
--   SELECT COUNT(*) FROM information_schema.columns WHERE table_name='tb_near_miss'; -- 25
--   SELECT SYST_VAL_D_CD FROM tb_syst_val_d WHERE SYST_VAL_CD='SYS063'; -- 5건
--   SELECT MENU_NM FROM tb_syst_menu_m WHERE MENU_M_ID='nearMiss';      -- 사건관리
--   SELECT COUNT(*) FROM tb_syst_auth_menu WHERE MENU_D_ID='NearMiss_01'; -- 9
-- ============================================================================
