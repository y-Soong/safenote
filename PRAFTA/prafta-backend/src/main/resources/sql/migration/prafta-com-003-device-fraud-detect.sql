-- ============================================================================
-- PRAFTA-COM-003 — 디바이스 식별 기반 부정 출퇴근(대리 출퇴근) 탐지
-- 작성일: 2026-06-03
-- 적용 환경: MySQL 8.0.42
-- 참조: .claude/requests/common/prafta-com-003.md (확정결정 D1~D6),
--       .claude/requests/common/prafta-com-003-plan.md §0-3, §2 (C2)
--
-- 변경 요약
--   1) tb_user_device_login_hist 신규 — 로그인 시점 디바이스/계정/IP 이력(append-only).
--      "이 기기가 지금껏 어떤 계정들에 쓰였는지" 부정탐지 baseline 소스(D2).
--      PK = DEVICE_LOGIN_NO = CONCAT(YYYYMM, FNC_CMM_SEQ_NEXTVAL(cmpnyCd,'DEVICE_LOGIN_NO')).
--   2) tb_user_attd_mgmt 에 CHECK_IN_DEVICE_UUID / CHECK_OUT_DEVICE_UUID 추가(D3 직접증거).
--      NULL 허용(기존 행/웹 등록분/구버전 앱 호환). 규칙1 그룹 조회용 인덱스 동반.
--
-- ★ tb_user_device.DEL_YN 은 com-002(prafta-com-002-user-device-del-yn.sql)에서 이미
--   라이브 DB 에 적용됨(MCP 확인: SHOW COLUMNS FROM tb_user_device 에 DEL_YN 존재).
--   따라서 본 마이그는 DEL_YN 을 추가하지 않는다. C3 로그인 upsert 는 DEL_YN='N' 으로 되살린다.
--   (com-002 가 미적용인 환경에 배포할 경우 com-002 를 선행 적용할 것.)
--
-- 적용 전 부재/현황 확인 (운영 적용 직전 권장):
--   SHOW TABLES LIKE 'tb_user_device_login_hist';
--   SHOW COLUMNS FROM tb_user_attd_mgmt LIKE 'CHECK_IN_DEVICE_UUID';
--   SHOW COLUMNS FROM tb_user_device LIKE 'DEL_YN';   -- com-002 적용 여부 확인
--
-- ⚠️ 적용 선행성(중요): 본 마이그가 운영 DB 에 적용되기 전에는
--   - C3 로그인 훅의 insertDeviceLoginHist 가 "Unknown table" 으로 실패(단, try-catch
--     격리되어 로그인 자체는 정상). 디바이스 이력만 미적재.
--   - C5 출퇴근 도장의 CHECK_IN/OUT_DEVICE_UUID 가 "Unknown column" 으로 INSERT/UPDATE
--     전면 실패(체크인/체크아웃 불가). ★ C5 배포 전 반드시 본 마이그 선행 적용. ★
--   - C6 탐지 API 조회가 "Unknown column/table" 으로 실패.
--
-- 멱등성: CREATE TABLE / ADD COLUMN 중복 실행 시 에러. 이미 반영된 환경에서는 건너뛸 것.
-- 운영 적용: 사용자 수동(read-only MCP). 본 파일은 작성만, DB 직접 적용 금지.
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1) 디바이스 로그인 이력 테이블 신설 (append-only, 부정탐지 baseline 소스)
-- ----------------------------------------------------------------------------
CREATE TABLE `tb_user_device_login_hist` (
      `DEVICE_LOGIN_NO` varchar(20)   NOT NULL COMMENT '디바이스 로그인 이력 번호(PK, 회사별 채번: YYYYMM + SEQ)'
    , `CMPNY_CD`        varchar(50)   NOT NULL COMMENT '회사 코드'
    , `DEVICE_UUID`     varchar(100)  NOT NULL COMMENT '디바이스UUID(클라 제공, 네이티브 ANDROID_ID/IDFV 우선)'
    , `USER_CD`         varchar(20)   NOT NULL COMMENT '로그인 사용자 코드'
    , `DEVICE_TYPE`     varchar(20)            DEFAULT NULL COMMENT '디바이스 종류[자유값] ANDROID:안드로이드 / IOS:iOS (네이티브 미주입 시 NULL)'
    , `DEVICE_MODEL`    varchar(50)            DEFAULT NULL COMMENT '디바이스 모델'
    , `OS_VERSION`      varchar(20)            DEFAULT NULL COMMENT 'OS 버전'
    , `APP_VERSION`     varchar(20)            DEFAULT NULL COMMENT '앱 버전'
    , `CLIENT_TYPE`     varchar(10)            DEFAULT NULL COMMENT '클라이언트 구분[자유값] APP:앱 / WEB:웹'
    , `LOGIN_IP`        varchar(45)            DEFAULT NULL COMMENT '로그인 IP(HttpServletRequest 추출)'
    , `LOGIN_DTIME`     datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '로그인 일시'
    , `INSERT_NO`       varchar(50)            DEFAULT 'SYSTEM' COMMENT '입력자'
    , `INSERT_DATE`     datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시'
    , PRIMARY KEY (`DEVICE_LOGIN_NO`)
    , KEY `IDX_DLH_DEVICE` (`CMPNY_CD`, `DEVICE_UUID`, `LOGIN_DTIME`)
    , KEY `IDX_DLH_USER`   (`CMPNY_CD`, `USER_CD`, `LOGIN_DTIME`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='디바이스 로그인 이력(append-only, 부정탐지 baseline 소스)';

-- ----------------------------------------------------------------------------
-- 2) tb_user_attd_mgmt 디바이스 도장 컬럼 추가 (출/퇴근 실행 기기 직접증거, D3)
--    NULL 허용 — 기존 행/웹 등록분/구버전 앱은 NULL 유지.
-- ----------------------------------------------------------------------------
ALTER TABLE `tb_user_attd_mgmt`
    ADD COLUMN `CHECK_IN_DEVICE_UUID` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL
        COMMENT '출근 실행 디바이스UUID(클라 제공, 부정탐지 보조)' AFTER `CHECK_IN_METHOD`,
    ADD COLUMN `CHECK_OUT_DEVICE_UUID` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL
        COMMENT '퇴근 실행 디바이스UUID(클라 제공, 부정탐지 보조)' AFTER `CHECK_OUT_METHOD`;

-- 규칙1(한 기기 → 같은 날 2계정 이상 출근) 그룹 조회 성능용 인덱스.
ALTER TABLE `tb_user_attd_mgmt`
    ADD KEY `IDX_ATTD_INDEVICE` (`CMPNY_CD`, `WORK_YMD`, `CHECK_IN_DEVICE_UUID`, `DEL_YN`);

-- ----------------------------------------------------------------------------
-- 3) 부정 출퇴근 의심 모니터링 화면(Attd_12) 메뉴 등록.
--    viewResolver 는 컴포넌트명으로 자동 로드하나 tb_syst_menu_d + tb_syst_auth_menu
--    가 있어야 LNB 노출/진입 가능(prafta-019-F 패턴). MENU_IDX=12(Attd_11 다음).
--    화면은 읽기 전용 → 조회/엑셀 버튼만 'Y', 신규/삭제/저장 'N'.
--    실제 데이터 접근 인가는 서버 canManageNode 가 강제(master/hr/safe 전사·노드관리자).
--    LNB 노출 AUTH 세트는 Attd_10(요청 승인 관리)과 동일 폭(관리자 + 일반관리권 직책).
-- ----------------------------------------------------------------------------
INSERT INTO tb_syst_menu_d (MENU_D_ID, MENU_M_ID, MENU_VIEW, MENU_NM, MENU_IDX, USE_YN, INSERT_NO, INSERT_DATE) VALUES
    ('Attd_12', 'attd', 'attd/Attd_12.vue', '부정 출퇴근 의심 모니터링', 12, 'Y', 'SYSTEM', NOW());

INSERT INTO tb_syst_auth_menu (CMPNY_CD, AUTH_CD, MENU_D_ID, USE_YN, BTN_SRCH, BTN_NEW, BTN_DELT, BTN_SAVE, BTN_EXCL, INSERT_NO, INSERT_DATE) VALUES
    ('001', '00001',  'Attd_12', 'Y', 'Y', 'N', 'N', 'N', 'Y', 'SYSTEM', NOW())
  , ('001', '00004',  'Attd_12', 'Y', 'Y', 'N', 'N', 'N', 'Y', 'SYSTEM', NOW())
  , ('001', '00006',  'Attd_12', 'Y', 'Y', 'N', 'N', 'N', 'Y', 'SYSTEM', NOW())
  , ('001', '00008',  'Attd_12', 'Y', 'Y', 'N', 'N', 'N', 'Y', 'SYSTEM', NOW())
  , ('001', '99999',  'Attd_12', 'Y', 'Y', 'N', 'N', 'N', 'Y', 'SYSTEM', NOW())
  , ('001', 'hr',     'Attd_12', 'Y', 'Y', 'N', 'N', 'N', 'Y', 'SYSTEM', NOW())
  , ('001', 'master', 'Attd_12', 'Y', 'Y', 'N', 'N', 'N', 'Y', 'SYSTEM', NOW())
  , ('001', 'safe',   'Attd_12', 'Y', 'Y', 'N', 'N', 'N', 'Y', 'SYSTEM', NOW())
  , ('001', 'system', 'Attd_12', 'Y', 'Y', 'N', 'N', 'N', 'Y', 'SYSTEM', NOW());

-- ============================================================================
-- 롤백 (필요 시 수동 실행)
-- ----------------------------------------------------------------------------
-- DELETE FROM tb_syst_auth_menu WHERE MENU_D_ID = 'Attd_12';
-- DELETE FROM tb_syst_menu_d   WHERE MENU_D_ID = 'Attd_12';
-- ALTER TABLE `tb_user_attd_mgmt` DROP KEY `IDX_ATTD_INDEVICE`;
-- ALTER TABLE `tb_user_attd_mgmt`
--     DROP COLUMN `CHECK_OUT_DEVICE_UUID`,
--     DROP COLUMN `CHECK_IN_DEVICE_UUID`;
-- DROP TABLE IF EXISTS `tb_user_device_login_hist`;
-- ============================================================================
