-- ============================================================================
-- PRAFTA-PLATFORM-4 — 위치정보 열람 콘솔(Platform_03/04): 열람로그 + SMS목적 + 메뉴시드
-- 작성일: 2026-07-17
-- 적용 환경: MySQL 8.0.42
-- 참조: 작업지시서_플랫폼-고객리스트-위치정보열람.md §3-2·§5 / plan §2 / prafta-platform-2-menu-seed.sql
--
-- 변경 요약
--   1) tb_location_access_log — 위치정보 열람 로그 신설(append-only, LBS 확인자료).
--   2) tb_sms_auth_code.PURPOSE_CD — 인증 목적 분리(셀프가입 ↔ 플랫폼 위치열람 혼용 금지).
--   3) tb_syst_menu_d / tb_syst_auth_menu — Platform_03/04 메뉴 시드(대분류 'platform' 기존재).
--
-- ⚠️ 적용 순서 주의: 본 마이그레이션(특히 2)의 PURPOSE_CD)을 적용하기 전에
--   PLT-LOC-03 백엔드 코드(BaseinfoMapper.xml PURPOSE_CD 필터 반영분)를 기동하면
--   셀프가입 SMS 인증이 컬럼 부재로 실패한다. 반드시 [DB 적용 → 백엔드 배포] 순서 엄수.
--
-- 적용 전 확인(권장):
--   SHOW TABLES LIKE 'tb_location_access_log';
--   SHOW COLUMNS FROM tb_sms_auth_code LIKE 'PURPOSE_CD';
--   SELECT MENU_D_ID FROM tb_syst_menu_d WHERE MENU_D_ID IN ('Platform_03','Platform_04');
--
-- 멱등성: CREATE/ALTER/INSERT 중복 실행 시 에러 또는 PK 충돌. 이미 반영된 환경에서는 건너뛸 것.
-- ⚠️ 운영 적용은 사용자 수동(read-only MCP). 본 파일은 작성만, DB 직접 적용 금지.
-- ============================================================================

-- ── 1) 위치정보 열람 로그 (append-only) ──
--   위치정보법상 이용·제공사실 확인자료 대응(요청서 §5-2 / 공통 정책서 §11.3 "상세 위치 조회").
--   보존기간: 위치정보법 시행령상 확인자료 보존 의무(최소 6개월) 대응 — 자동 파기 배치는 본 스코프 외.
--   UPDATE/DELETE 경로를 만들지 않는다(append-only). 고볼륨 append 관례에 따라 BIGINT AUTO_INCREMENT PK.
CREATE TABLE `tb_location_access_log` (
  `ACCESS_NO`        bigint        NOT NULL AUTO_INCREMENT COMMENT '열람 일련번호 (PK)',
  `ACCESSOR_USER_CD` varchar(20)   NOT NULL COMMENT '열람자 USER_CD (플랫폼 운영자, CMPNY_CD=prafta_system_admin 소속)',
  `ACCESS_DTIME`     datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '열람일시(서버 시각 — 클라이언트 시각 불신)',
  `TARGET_CMPNY_CD`  varchar(50)   NOT NULL COMMENT '열람 대상 회사코드',
  `TARGET_SITE_CD`   varchar(50)   NOT NULL COMMENT '열람 대상 사업장코드',
  `TARGET_DATE`      varchar(8)    NOT NULL COMMENT '열람 대상 일자(YYYYMMDD)',
  `SMS_AUTH_ID`      bigint        NULL     COMMENT '통과한 SMS 인증 레코드(TB_SMS_AUTH_CODE.SMS_ID) — 인증 통과 후에만 조회 가능하므로 사실상 NOT NULL',
  `SMS_VERIFIED_AT`  datetime      NULL     COMMENT 'SMS 인증 통과 일시(TB_SMS_AUTH_CODE.UPDATE_DATE 스냅샷)',
  `RESULT_CNT`       int           NOT NULL DEFAULT 0 COMMENT '응답 위치정보 건수(LIMIT 절단 시 반환 건수 기준)',
  `CLIENT_IP`        varchar(45)   NULL     COMMENT '열람자 IP 해석값(신뢰 프록시(prafta.platform.trusted-proxies) 경유 시 X-Forwarded-For 선두, 그 외 RemoteAddr. IPv6 대응)',
  `REMOTE_ADDR`      varchar(45)   NULL     COMMENT '직접 연결 IP 원시값(RemoteAddr — 프록시 뒤면 프록시 IP. XFF 위조 대비 CLIENT_IP 와 병기)',
  `INSERT_NO`        varchar(50)   NOT NULL COMMENT '입력자',
  `INSERT_DATE`      datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  PRIMARY KEY (`ACCESS_NO`),
  KEY `IX_LOC_ACCESS_01` (`ACCESSOR_USER_CD`, `ACCESS_DTIME`),
  KEY `IX_LOC_ACCESS_02` (`TARGET_CMPNY_CD`, `TARGET_DATE`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='위치정보 열람 로그(LBS 이용·제공사실 확인자료, append-only)';

-- ── 2) SMS 인증 목적 코드 + 검증 실패 카운터 ──
--   기본값 'SELF_JOIN' 으로 기존 행/기존 INSERT(baseinfo insertSmsAuthNo — 목적 미지정) 하위호환.
--   기존 검증/상태 statement 에는 PURPOSE_CD='SELF_JOIN' 필터를 추가한다(PLT-LOC-03-5, 양방향 혼용 차단).
--   FAIL_CNT: 플랫폼 위치열람 verify 브루트포스 방어(보안 리뷰 V-2) — 5회 실패 시 코드 무효(재발송 필요).
--   레거시 흐름(셀프가입·앱 마이페이지)은 FAIL_CNT 를 사용하지 않는다(무회귀 — 기본값 0 유지).
ALTER TABLE `tb_sms_auth_code`
    ADD COLUMN `PURPOSE_CD` varchar(20) NOT NULL DEFAULT 'SELF_JOIN'
        COMMENT '인증 목적 SELF_JOIN:셀프가입·본인인증·비밀번호재설정(기존 흐름) PLATFORM_LOCATION:플랫폼 위치정보 열람 게이트'
        AFTER `AUTH_CD`,
    ADD COLUMN `FAIL_CNT` int NOT NULL DEFAULT 0
        COMMENT '인증번호 검증 실패 횟수(PLATFORM_LOCATION 전용 브루트포스 방어 — 5회 이상 시 코드 무효, 레거시 흐름 미사용)'
        AFTER `PURPOSE_CD`,
    ADD KEY `idx_sms_auth_purpose` (`MBL_NO_HMAC`, `PURPOSE_CD`, `VERIFIED_YN`, `UPDATE_DATE`);

-- ── 3) 메뉴 시드 (prafta-platform-2-menu-seed.sql 미러 — 대분류 'platform' 은 기존재) ──
--   Platform_01=IDX 1, Platform_02=IDX 2 기존재 → 신규는 3, 4.
--   버튼권한: 두 화면 모두 read-only 콘솔 — 조회(BTN_SRCH)만 'Y'.
INSERT INTO `tb_syst_menu_d`
      (`MENU_D_ID`, `MENU_M_ID`, `MENU_VIEW`, `MENU_NM`, `SUB_GROUP_NM`, `SUB_GROUP_IDX`, `MENU_IDX`, `USE_YN`, `INSERT_NO`)
VALUES
      ('Platform_03', 'platform', 'platform/Platform_03.vue', '고객 리스트',   '플랫폼 운영', 1, 3, 'Y', 'SYSTEM')
    , ('Platform_04', 'platform', 'platform/Platform_04.vue', '위치정보 열람', '플랫폼 운영', 1, 4, 'Y', 'SYSTEM');

INSERT INTO `tb_syst_auth_menu`
      (`CMPNY_CD`, `AUTH_CD`, `MENU_D_ID`, `USE_YN`, `BTN_SRCH`, `BTN_NEW`, `BTN_DELT`, `BTN_SAVE`, `BTN_EXCL`, `INSERT_NO`)
VALUES
      ('prafta_system_admin', 'master', 'Platform_03', 'Y', 'Y', 'N', 'N', 'N', 'N', 'SYSTEM')
    , ('prafta_system_admin', 'master', 'Platform_04', 'Y', 'Y', 'N', 'N', 'N', 'N', 'SYSTEM');

-- ============================================================================
-- 적용 후 검증 (권장)
-- ----------------------------------------------------------------------------
-- SHOW CREATE TABLE tb_location_access_log;
-- SHOW COLUMNS FROM tb_sms_auth_code LIKE 'PURPOSE_CD';
--   → Default='SELF_JOIN', Null=NO 확인.
-- SHOW COLUMNS FROM tb_sms_auth_code LIKE 'FAIL_CNT';
--   → Default=0, Null=NO 확인.
-- SELECT PURPOSE_CD, COUNT(1) FROM tb_sms_auth_code GROUP BY PURPOSE_CD;
--   → 기존 행 전부 'SELF_JOIN' 백필 확인.
-- SELECT MENU_D_ID, MENU_IDX, USE_YN FROM tb_syst_menu_d
--  WHERE MENU_D_ID IN ('Platform_03','Platform_04');
-- SELECT MENU_D_ID, BTN_SRCH, BTN_NEW, BTN_DELT, BTN_SAVE, BTN_EXCL
--   FROM tb_syst_auth_menu
--  WHERE CMPNY_CD='prafta_system_admin' AND MENU_D_ID IN ('Platform_03','Platform_04');
-- ============================================================================

-- ============================================================================
-- 롤백 (필요 시 수동 실행)
-- ----------------------------------------------------------------------------
-- DELETE FROM `tb_syst_auth_menu` WHERE `CMPNY_CD`='prafta_system_admin'
--    AND `MENU_D_ID` IN ('Platform_03','Platform_04');
-- DELETE FROM `tb_syst_menu_d` WHERE `MENU_D_ID` IN ('Platform_03','Platform_04');
-- ALTER TABLE `tb_sms_auth_code` DROP KEY `idx_sms_auth_purpose`, DROP COLUMN `FAIL_CNT`, DROP COLUMN `PURPOSE_CD`;
-- DROP TABLE IF EXISTS `tb_location_access_log`;
-- ============================================================================
