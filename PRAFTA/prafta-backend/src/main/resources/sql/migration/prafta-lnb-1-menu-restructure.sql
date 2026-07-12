-- ============================================================================
-- PRAFTA-LNB-1 — 웹 관리자 LNB 메뉴 재편 (대분류 4탭 + 중분류 컬럼 + MENU_D 재할당)
-- 작성일: 2026-06-25
-- 적용 환경: MySQL 8.0.42
-- 출처: .claude/requests/web_requests/mene_lnb/lnb-restructure-작업지시서.md §3 TO-BE 트리, §4 데이터 모델
--
-- 목적:
--   웹 관리자 LNB 를 "대분류(상단 탭) → 중분류(좌측 accordion) → 메뉴" 3단계로 재편한다.
--   1) TB_SYST_MENU_M : 신규 대분류 탭 4행 INSERT
--                       (attdHr=근태/인사, safety=산업안전, system=시스템 관리, dailyAcct=일일계정 관리)
--   2) TB_SYST_MENU_M : 기존 9개 그룹(user/baim/chkLst/risk/tbm/attd/nearMiss/notice/acct) USE_YN='N' 폐기
--                       (login 그룹은 절대 건드리지 않음 — 탭 비노출 메뉴 보존)
--   3) TB_SYST_MENU_D : 중분류 컬럼 2개 신규(SUB_GROUP_NM, SUB_GROUP_IDX)
--   4) TB_SYST_MENU_D : 40개 메뉴의 MENU_M_ID 재할당 + SUB_GROUP_NM/IDX + MENU_IDX(그룹 내 순서) + MENU_NM 정정
--
-- 불변 규칙(절대 변경 금지):
--   - MENU_D_ID / MENU_VIEW : 라우트·권한(tb_syst_auth_menu) 보존을 위해 변경 금지.
--   - tb_syst_auth_menu     : 권한은 MENU_D_ID 기준이라 무변경. (이 파일에서 손대지 않음)
--   - login 그룹             : MENU_M USE_YN, 하위 MENU_D 모두 무변경.
--   - User_05 MENU_NM        : "일일사용자 관리" 유지(D8). MENU_VIEW/MENU_D_ID 불변.
--
-- ⚠️ 데이터 검증 한계:
--   본 파일은 코드베이스(마이그 시드 + .vue 화면 파일)로 40개 MENU_D 존재를 대조해 작성했다.
--   read-only MCP(prafta-mysql) 및 dev 계정 직접 접속이 본 세션에서 불가하여 실 DB 행을
--   1:1 조회하지 못했다. 운영 적용 전 아래 "사전 검증 SELECT"를 반드시 실행할 것.
--   특히 'Tbm_03' 은 프론트엔드 화면/라우트/시드 어디에도 흔적이 없다(미확정). 아래 멱등 UPDATE 는
--   행이 없으면 0건 갱신으로 무해하나, 트리상 'TBM 진행관리'가 실제로 어느 MENU_D_ID 인지
--   운영 DB 로 확인 권장.
--
-- ⚠️ MySQL safe-update 모드 주의:
--   다수 UPDATE 가 PK(MENU_D_ID) 기반 WHERE 이므로 안전하나, 일부 클라이언트의 안전모드에서
--   막힐 수 있다. 막히면 세션에서 SQL_SAFE_UPDATES=0 후 재실행.
--   SET SQL_SAFE_UPDATES = 0;   -- (필요 시 주석 해제)
--
-- 멱등성:
--   - MENU_M INSERT : 재실행 시 PK 충돌 → 이미 등록된 환경에서는 (1) 블록을 건너뛸 것.
--   - MENU_D ALTER ADD COLUMN : 이미 컬럼이 있으면 에러 → 이미 추가된 환경에서는 (3) 블록을 건너뛸 것.
--   - MENU_D UPDATE : 멱등(재실행 안전). 동일 값으로 덮어씀.
-- ============================================================================

-- ----------------------------------------------------------------------------
-- [사전 검증 SELECT] — 운영 적용 전 1회 실행하여 실 DB 와 대조 (주석)
-- ----------------------------------------------------------------------------
-- 1) 현재 대분류 탭(login 포함) 확인:
--    SELECT MENU_M_ID, MENU_SRC, MENU_NM, MENU_IDX, USE_YN FROM tb_syst_menu_m ORDER BY MENU_IDX, MENU_M_ID;
-- 2) 신규 4탭 부재 확인(0건이어야 함):
--    SELECT MENU_M_ID FROM tb_syst_menu_m WHERE MENU_M_ID IN ('attdHr','safety','system','dailyAcct');
-- 3) 재편 대상 40개 MENU_D 실재 확인(40행이어야 함. Tbm_03 부재 가능성 주의):
--    SELECT MENU_D_ID, MENU_M_ID, MENU_NM, MENU_IDX FROM tb_syst_menu_d
--     WHERE MENU_D_ID IN (
--       'Attd_08','Attd_10','Attd_11','Attd_12','Attd_09','LeavePromotion_01','Attd_13','Attd_14',
--       'Attd_01','Attd_05','Attd_02','Attd_03','Baim_07','Attd_06','Attd_07',
--       'Risk_03','Tbm_02','Tbm_03','Tbm_04','ChkLst_03','ChkLst_04','NearMiss_01','Acct_01','Notice_02',
--       'Risk_01','Risk_02','Tbm_01','ChkLst_01','ChkLst_02',
--       'User_01','User_02','User_03','User_04','Baim_01','Baim_06','Baim_02','Baim_03','Notice_01',
--       'User_05','Baim_05'
--     ) ORDER BY MENU_M_ID, MENU_IDX;
-- 4) login 그룹 4개 무변경 대상 확인(건드리지 않음):
--    SELECT MENU_D_ID, MENU_M_ID FROM tb_syst_menu_d WHERE MENU_M_ID = 'login';
-- 5) 신규 컬럼 존재 여부(없어야 (3) 실행):
--    SHOW COLUMNS FROM tb_syst_menu_d LIKE 'SUB_GROUP_NM';


-- ============================================================================
-- (1) TB_SYST_MENU_M — 신규 대분류 탭 4행 INSERT
--     - MENU_SRC='001' : 웹 메뉴(앱은 '002'). 웹 LNB 노출 조건.
--     - 기존 MENU_M 행 컬럼 패턴(INSERT_NO='SYSTEM', USE_YN='Y')을 그대로 따름.
--     - MENU_DESC/UPDATE_NO/UPDATE_DATE 는 NULL 허용 → 미지정.
-- ============================================================================
INSERT INTO `tb_syst_menu_m`
    (`MENU_M_ID`, `MENU_SRC`, `MENU_NM`, `MENU_IDX`, `USE_YN`, `INSERT_NO`, `INSERT_DATE`) VALUES
    ('attdHr',    '001', '근태/인사',     1, 'Y', 'SYSTEM', NOW())
  , ('safety',    '001', '산업안전',      2, 'Y', 'SYSTEM', NOW())
  , ('system',    '001', '시스템 관리',   3, 'Y', 'SYSTEM', NOW())
  , ('dailyAcct', '001', '일일계정 관리', 4, 'Y', 'SYSTEM', NOW());


-- ============================================================================
-- (2) TB_SYST_MENU_M — 기존 9개 그룹 USE_YN='N' 폐기 (삭제 대신 보존)
--     - login 은 제외(탭 비노출 메뉴 보존).
--     - WHERE 는 PK(MENU_M_ID) 기반.
--     - DB 콜레이션이 대소문자 무시(utf8mb4_unicode_ci)이므로 표기는 실DB 값과 동일하게 둠.
-- ============================================================================
UPDATE `tb_syst_menu_m`
   SET `USE_YN` = 'N'
     , `UPDATE_NO` = 'SYSTEM'
     , `UPDATE_DATE` = NOW()
 WHERE `MENU_M_ID` IN ('user','baim','chkLst','risk','tbm','attd','nearMiss','notice','acct');


-- ============================================================================
-- (3) TB_SYST_MENU_D — 중분류 컬럼 2개 신규
--     - SUB_GROUP_NM  : 중분류명. NULL 이면 빌더가 대분류명(MENU_M.MENU_NM)으로 대체(D4).
--     - SUB_GROUP_IDX : 중분류 순서(탭 내 그룹 정렬).
--     - 이미 추가된 환경에서는 이 블록을 건너뛸 것(ADD COLUMN 중복 시 에러).
-- ============================================================================
ALTER TABLE `tb_syst_menu_d`
    ADD COLUMN `SUB_GROUP_NM`  varchar(50) NULL COMMENT '중분류명(NULL이면 대분류명으로 대체)' AFTER `MENU_NM`
  , ADD COLUMN `SUB_GROUP_IDX` int         NULL COMMENT '중분류 순서(탭 내 그룹 정렬)'        AFTER `SUB_GROUP_NM`;


-- ============================================================================
-- (4) TB_SYST_MENU_D — 40개 메뉴 재할당 (MENU_M_ID / SUB_GROUP_NM / SUB_GROUP_IDX / MENU_IDX / MENU_NM)
--     - WHERE 는 PK 의 일부인 MENU_D_ID 기반(MENU_D_ID 는 화면당 1행이라 유일).
--     - MENU_VIEW / MENU_D_ID 는 SET 에 포함하지 않음(불변).
--     - MENU_IDX = 그룹(중분류) 내 순서로 재활용.
--     - 멱등: 재실행 시 동일 값으로 갱신(안전).
-- ----------------------------------------------------------------------------

-- ---- [탭] 근태/인사 (attdHr) ------------------------------------------------
-- 중분류 1: 근태 관리 (SUB_GROUP_IDX=1)
UPDATE `tb_syst_menu_d` SET `MENU_M_ID`='attdHr', `SUB_GROUP_NM`='근태 관리', `SUB_GROUP_IDX`=1, `MENU_IDX`=1, `MENU_NM`='근로자 근태조회',          `UPDATE_NO`='SYSTEM', `UPDATE_DATE`=NOW() WHERE `MENU_D_ID`='Attd_08';
UPDATE `tb_syst_menu_d` SET `MENU_M_ID`='attdHr', `SUB_GROUP_NM`='근태 관리', `SUB_GROUP_IDX`=1, `MENU_IDX`=2, `MENU_NM`='요청 승인 관리',           `UPDATE_NO`='SYSTEM', `UPDATE_DATE`=NOW() WHERE `MENU_D_ID`='Attd_10';
UPDATE `tb_syst_menu_d` SET `MENU_M_ID`='attdHr', `SUB_GROUP_NM`='근태 관리', `SUB_GROUP_IDX`=1, `MENU_IDX`=3, `MENU_NM`='월별 사용자 근태 판정',      `UPDATE_NO`='SYSTEM', `UPDATE_DATE`=NOW() WHERE `MENU_D_ID`='Attd_11';
UPDATE `tb_syst_menu_d` SET `MENU_M_ID`='attdHr', `SUB_GROUP_NM`='근태 관리', `SUB_GROUP_IDX`=1, `MENU_IDX`=4, `MENU_NM`='부정출퇴근 의심 모니터링',   `UPDATE_NO`='SYSTEM', `UPDATE_DATE`=NOW() WHERE `MENU_D_ID`='Attd_12';
-- 중분류 2: 연차 관리 (SUB_GROUP_IDX=2)
UPDATE `tb_syst_menu_d` SET `MENU_M_ID`='attdHr', `SUB_GROUP_NM`='연차 관리', `SUB_GROUP_IDX`=2, `MENU_IDX`=1, `MENU_NM`='사용자 연차관리',           `UPDATE_NO`='SYSTEM', `UPDATE_DATE`=NOW() WHERE `MENU_D_ID`='Attd_09';
UPDATE `tb_syst_menu_d` SET `MENU_M_ID`='attdHr', `SUB_GROUP_NM`='연차 관리', `SUB_GROUP_IDX`=2, `MENU_IDX`=2, `MENU_NM`='연차 사용촉진(2차)',         `UPDATE_NO`='SYSTEM', `UPDATE_DATE`=NOW() WHERE `MENU_D_ID`='LeavePromotion_01';
UPDATE `tb_syst_menu_d` SET `MENU_M_ID`='attdHr', `SUB_GROUP_NM`='연차 관리', `SUB_GROUP_IDX`=2, `MENU_IDX`=3, `MENU_NM`='연차 변경 동의 관리',        `UPDATE_NO`='SYSTEM', `UPDATE_DATE`=NOW() WHERE `MENU_D_ID`='Attd_13';
UPDATE `tb_syst_menu_d` SET `MENU_M_ID`='attdHr', `SUB_GROUP_NM`='연차 관리', `SUB_GROUP_IDX`=2, `MENU_IDX`=4, `MENU_NM`='연차 변경 요청 이력',        `UPDATE_NO`='SYSTEM', `UPDATE_DATE`=NOW() WHERE `MENU_D_ID`='Attd_14';
-- 중분류 3: 근태 설정 (SUB_GROUP_IDX=3)
UPDATE `tb_syst_menu_d` SET `MENU_M_ID`='attdHr', `SUB_GROUP_NM`='근태 설정', `SUB_GROUP_IDX`=3, `MENU_IDX`=1, `MENU_NM`='근무타입 관리',             `UPDATE_NO`='SYSTEM', `UPDATE_DATE`=NOW() WHERE `MENU_D_ID`='Attd_01';
UPDATE `tb_syst_menu_d` SET `MENU_M_ID`='attdHr', `SUB_GROUP_NM`='근태 설정', `SUB_GROUP_IDX`=3, `MENU_IDX`=2, `MENU_NM`='근무계획 관리',             `UPDATE_NO`='SYSTEM', `UPDATE_DATE`=NOW() WHERE `MENU_D_ID`='Attd_05';
UPDATE `tb_syst_menu_d` SET `MENU_M_ID`='attdHr', `SUB_GROUP_NM`='근태 설정', `SUB_GROUP_IDX`=3, `MENU_IDX`=3, `MENU_NM`='휴일 관리',                 `UPDATE_NO`='SYSTEM', `UPDATE_DATE`=NOW() WHERE `MENU_D_ID`='Attd_02';
UPDATE `tb_syst_menu_d` SET `MENU_M_ID`='attdHr', `SUB_GROUP_NM`='근태 설정', `SUB_GROUP_IDX`=3, `MENU_IDX`=4, `MENU_NM`='연차타입 관리',             `UPDATE_NO`='SYSTEM', `UPDATE_DATE`=NOW() WHERE `MENU_D_ID`='Attd_03';
UPDATE `tb_syst_menu_d` SET `MENU_M_ID`='attdHr', `SUB_GROUP_NM`='근태 설정', `SUB_GROUP_IDX`=3, `MENU_IDX`=5, `MENU_NM`='연차부여정책',             `UPDATE_NO`='SYSTEM', `UPDATE_DATE`=NOW() WHERE `MENU_D_ID`='Baim_07';
UPDATE `tb_syst_menu_d` SET `MENU_M_ID`='attdHr', `SUB_GROUP_NM`='근태 설정', `SUB_GROUP_IDX`=3, `MENU_IDX`=6, `MENU_NM`='교대근무 팀 관리',           `UPDATE_NO`='SYSTEM', `UPDATE_DATE`=NOW() WHERE `MENU_D_ID`='Attd_06';
UPDATE `tb_syst_menu_d` SET `MENU_M_ID`='attdHr', `SUB_GROUP_NM`='근태 설정', `SUB_GROUP_IDX`=3, `MENU_IDX`=7, `MENU_NM`='근무 관리',                 `UPDATE_NO`='SYSTEM', `UPDATE_DATE`=NOW() WHERE `MENU_D_ID`='Attd_07';

-- ---- [탭] 산업안전 (safety) --------------------------------------------------
-- 중분류 1: 안전 관리 (SUB_GROUP_IDX=1)
UPDATE `tb_syst_menu_d` SET `MENU_M_ID`='safety', `SUB_GROUP_NM`='안전 관리', `SUB_GROUP_IDX`=1, `MENU_IDX`=1, `MENU_NM`='위험성평가 관리',           `UPDATE_NO`='SYSTEM', `UPDATE_DATE`=NOW() WHERE `MENU_D_ID`='Risk_03';
UPDATE `tb_syst_menu_d` SET `MENU_M_ID`='safety', `SUB_GROUP_NM`='안전 관리', `SUB_GROUP_IDX`=1, `MENU_IDX`=2, `MENU_NM`='TBM 교육 관리',            `UPDATE_NO`='SYSTEM', `UPDATE_DATE`=NOW() WHERE `MENU_D_ID`='Tbm_02';
UPDATE `tb_syst_menu_d` SET `MENU_M_ID`='safety', `SUB_GROUP_NM`='안전 관리', `SUB_GROUP_IDX`=1, `MENU_IDX`=3, `MENU_NM`='TBM 진행관리',             `UPDATE_NO`='SYSTEM', `UPDATE_DATE`=NOW() WHERE `MENU_D_ID`='Tbm_03';
UPDATE `tb_syst_menu_d` SET `MENU_M_ID`='safety', `SUB_GROUP_NM`='안전 관리', `SUB_GROUP_IDX`=1, `MENU_IDX`=4, `MENU_NM`='TBM 이력관리',             `UPDATE_NO`='SYSTEM', `UPDATE_DATE`=NOW() WHERE `MENU_D_ID`='Tbm_04';
UPDATE `tb_syst_menu_d` SET `MENU_M_ID`='safety', `SUB_GROUP_NM`='안전 관리', `SUB_GROUP_IDX`=1, `MENU_IDX`=5, `MENU_NM`='순회점검 결과 관리',         `UPDATE_NO`='SYSTEM', `UPDATE_DATE`=NOW() WHERE `MENU_D_ID`='ChkLst_03';
UPDATE `tb_syst_menu_d` SET `MENU_M_ID`='safety', `SUB_GROUP_NM`='안전 관리', `SUB_GROUP_IDX`=1, `MENU_IDX`=6, `MENU_NM`='점검 불량 관리',            `UPDATE_NO`='SYSTEM', `UPDATE_DATE`=NOW() WHERE `MENU_D_ID`='ChkLst_04';
UPDATE `tb_syst_menu_d` SET `MENU_M_ID`='safety', `SUB_GROUP_NM`='안전 관리', `SUB_GROUP_IDX`=1, `MENU_IDX`=7, `MENU_NM`='아차사고 관리',            `UPDATE_NO`='SYSTEM', `UPDATE_DATE`=NOW() WHERE `MENU_D_ID`='NearMiss_01';
UPDATE `tb_syst_menu_d` SET `MENU_M_ID`='safety', `SUB_GROUP_NM`='안전 관리', `SUB_GROUP_IDX`=1, `MENU_IDX`=8, `MENU_NM`='사고관리',                `UPDATE_NO`='SYSTEM', `UPDATE_DATE`=NOW() WHERE `MENU_D_ID`='Acct_01';
UPDATE `tb_syst_menu_d` SET `MENU_M_ID`='safety', `SUB_GROUP_NM`='안전 관리', `SUB_GROUP_IDX`=1, `MENU_IDX`=9, `MENU_NM`='안전자료실',              `UPDATE_NO`='SYSTEM', `UPDATE_DATE`=NOW() WHERE `MENU_D_ID`='Notice_02';
-- 중분류 2: 안전 설정 (SUB_GROUP_IDX=2)
UPDATE `tb_syst_menu_d` SET `MENU_M_ID`='safety', `SUB_GROUP_NM`='안전 설정', `SUB_GROUP_IDX`=2, `MENU_IDX`=1, `MENU_NM`='유해/위험 구분 관리',        `UPDATE_NO`='SYSTEM', `UPDATE_DATE`=NOW() WHERE `MENU_D_ID`='Risk_01';
UPDATE `tb_syst_menu_d` SET `MENU_M_ID`='safety', `SUB_GROUP_NM`='안전 설정', `SUB_GROUP_IDX`=2, `MENU_IDX`=2, `MENU_NM`='위험성 평가 기준',          `UPDATE_NO`='SYSTEM', `UPDATE_DATE`=NOW() WHERE `MENU_D_ID`='Risk_02';
UPDATE `tb_syst_menu_d` SET `MENU_M_ID`='safety', `SUB_GROUP_NM`='안전 설정', `SUB_GROUP_IDX`=2, `MENU_IDX`=3, `MENU_NM`='TBM 교육자료 관리',         `UPDATE_NO`='SYSTEM', `UPDATE_DATE`=NOW() WHERE `MENU_D_ID`='Tbm_01';
UPDATE `tb_syst_menu_d` SET `MENU_M_ID`='safety', `SUB_GROUP_NM`='안전 설정', `SUB_GROUP_IDX`=2, `MENU_IDX`=4, `MENU_NM`='순회점검 대상 관리',        `UPDATE_NO`='SYSTEM', `UPDATE_DATE`=NOW() WHERE `MENU_D_ID`='ChkLst_01';
UPDATE `tb_syst_menu_d` SET `MENU_M_ID`='safety', `SUB_GROUP_NM`='안전 설정', `SUB_GROUP_IDX`=2, `MENU_IDX`=5, `MENU_NM`='점검문항관리',             `UPDATE_NO`='SYSTEM', `UPDATE_DATE`=NOW() WHERE `MENU_D_ID`='ChkLst_02';

-- ---- [탭] 시스템 관리 (system) — 중분류명 없음(NULL → 빌더가 대분류명으로 대체, D4) -----
UPDATE `tb_syst_menu_d` SET `MENU_M_ID`='system', `SUB_GROUP_NM`=NULL, `SUB_GROUP_IDX`=1, `MENU_IDX`=1, `MENU_NM`='사용자관리',           `UPDATE_NO`='SYSTEM', `UPDATE_DATE`=NOW() WHERE `MENU_D_ID`='User_01';
UPDATE `tb_syst_menu_d` SET `MENU_M_ID`='system', `SUB_GROUP_NM`=NULL, `SUB_GROUP_IDX`=1, `MENU_IDX`=2, `MENU_NM`='권한별 화면 제어',     `UPDATE_NO`='SYSTEM', `UPDATE_DATE`=NOW() WHERE `MENU_D_ID`='User_02';
UPDATE `tb_syst_menu_d` SET `MENU_M_ID`='system', `SUB_GROUP_NM`=NULL, `SUB_GROUP_IDX`=1, `MENU_IDX`=3, `MENU_NM`='사업장 권한 관리',     `UPDATE_NO`='SYSTEM', `UPDATE_DATE`=NOW() WHERE `MENU_D_ID`='User_03';
UPDATE `tb_syst_menu_d` SET `MENU_M_ID`='system', `SUB_GROUP_NM`=NULL, `SUB_GROUP_IDX`=1, `MENU_IDX`=4, `MENU_NM`='결재라인 관리',         `UPDATE_NO`='SYSTEM', `UPDATE_DATE`=NOW() WHERE `MENU_D_ID`='User_04';
UPDATE `tb_syst_menu_d` SET `MENU_M_ID`='system', `SUB_GROUP_NM`=NULL, `SUB_GROUP_IDX`=1, `MENU_IDX`=5, `MENU_NM`='사업장관리',           `UPDATE_NO`='SYSTEM', `UPDATE_DATE`=NOW() WHERE `MENU_D_ID`='Baim_01';
UPDATE `tb_syst_menu_d` SET `MENU_M_ID`='system', `SUB_GROUP_NM`=NULL, `SUB_GROUP_IDX`=1, `MENU_IDX`=6, `MENU_NM`='조직관리',             `UPDATE_NO`='SYSTEM', `UPDATE_DATE`=NOW() WHERE `MENU_D_ID`='Baim_06';
UPDATE `tb_syst_menu_d` SET `MENU_M_ID`='system', `SUB_GROUP_NM`=NULL, `SUB_GROUP_IDX`=1, `MENU_IDX`=7, `MENU_NM`='운영 기초정보 관리',   `UPDATE_NO`='SYSTEM', `UPDATE_DATE`=NOW() WHERE `MENU_D_ID`='Baim_02';
UPDATE `tb_syst_menu_d` SET `MENU_M_ID`='system', `SUB_GROUP_NM`=NULL, `SUB_GROUP_IDX`=1, `MENU_IDX`=8, `MENU_NM`='이용약관',             `UPDATE_NO`='SYSTEM', `UPDATE_DATE`=NOW() WHERE `MENU_D_ID`='Baim_03';
UPDATE `tb_syst_menu_d` SET `MENU_M_ID`='system', `SUB_GROUP_NM`=NULL, `SUB_GROUP_IDX`=1, `MENU_IDX`=9, `MENU_NM`='공지사항 관리',         `UPDATE_NO`='SYSTEM', `UPDATE_DATE`=NOW() WHERE `MENU_D_ID`='Notice_01';

-- ---- [탭] 일일계정 관리 (dailyAcct) — 중분류명 없음(NULL → 대분류명 대체, D4) -----------
-- User_05 MENU_NM 은 "일일사용자 관리" 유지(D8). MENU_VIEW/MENU_D_ID 불변.
UPDATE `tb_syst_menu_d` SET `MENU_M_ID`='dailyAcct', `SUB_GROUP_NM`=NULL, `SUB_GROUP_IDX`=1, `MENU_IDX`=1, `MENU_NM`='일일사용자 관리',     `UPDATE_NO`='SYSTEM', `UPDATE_DATE`=NOW() WHERE `MENU_D_ID`='User_05';
UPDATE `tb_syst_menu_d` SET `MENU_M_ID`='dailyAcct', `SUB_GROUP_NM`=NULL, `SUB_GROUP_IDX`=1, `MENU_IDX`=2, `MENU_NM`='계정슬롯 관리',       `UPDATE_NO`='SYSTEM', `UPDATE_DATE`=NOW() WHERE `MENU_D_ID`='Baim_05';


-- ============================================================================
-- [적용 후 검증 SELECT] — 주석 (운영 적용 후 1회 실행)
-- ----------------------------------------------------------------------------
-- 1) 신규 4탭 + login 만 USE_YN='Y' (기존 9그룹은 'N') 확인:
--    SELECT MENU_M_ID, MENU_NM, MENU_IDX, USE_YN FROM tb_syst_menu_m ORDER BY USE_YN DESC, MENU_IDX;
-- 2) 탭별 메뉴 분포 확인(attdHr=15, safety=14, system=9, dailyAcct=2 = 40):
--    SELECT MENU_M_ID, COUNT(*) FROM tb_syst_menu_d
--     WHERE MENU_M_ID IN ('attdHr','safety','system','dailyAcct') GROUP BY MENU_M_ID;
-- 3) 중분류/순서 트리 확인:
--    SELECT MENU_M_ID, SUB_GROUP_IDX, SUB_GROUP_NM, MENU_IDX, MENU_D_ID, MENU_NM
--      FROM tb_syst_menu_d
--     WHERE MENU_M_ID IN ('attdHr','safety','system','dailyAcct')
--     ORDER BY MENU_M_ID, SUB_GROUP_IDX, MENU_IDX;
-- 4) 어느 신규 탭에도 안 붙은 잔여 행(login 제외) 점검 — 0건 기대(Tbm_03 부재 시 0건 갱신됨):
--    SELECT MENU_D_ID, MENU_M_ID FROM tb_syst_menu_d
--     WHERE MENU_M_ID NOT IN ('attdHr','safety','system','dailyAcct','login');
-- ============================================================================
-- 끝.
-- ============================================================================
