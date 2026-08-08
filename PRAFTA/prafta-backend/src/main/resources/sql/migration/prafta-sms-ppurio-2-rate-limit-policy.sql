-- ============================================================================
-- PRAFTA-SMS-PPURIO-2 — SMS 발송 다층 상한 정책 + 킬스위치 + IP/사용자 축 컬럼
-- 작성일: 2026-08-07
-- 적용 환경: 운영 RDS MySQL 8.4.10 / 개발 MySQL 8.0.42 (양쪽 모두 적용)
-- 참조: 작업지시서_SMS-발송-뿌리오-연동-2차.md §1-B(B-1·3·4) §1-C(C-1·5) §1-D(D-4) / plan §3 SMS2-B0
--
-- 변경 요약
--   1) TB_SMS_SEND_POLICY 신설 — 발송 다층 상한 임계값 + 킬스위치 상태(전역 단일행 'DEFAULT').
--   2) tb_sms_auth_code — SEND_IP_HASH / SEND_USER_CD 2컬럼 추가(IP축·사용자축 카운트 재료).
--   3) tb_sms_auth_code — 인덱스 3개 추가(IP축 / 사용자축 / 전역축).
--   4) tb_sms_auth_code.PURPOSE_CD — COMMENT 갱신(MOBILE_CHANGE 신설 반영. 값 변환 없음).
--   5) tb_syst_val_d — SYS060(감사 액션) '설정 변경' 코드 1건 시드.
--   6) tb_syst_menu_d / tb_syst_auth_menu — Platform_05(SMS 발송 관리) 메뉴 시드.
--
-- ============================================================================
-- ⚠️ 적용 순서 (엄수)
-- ----------------------------------------------------------------------------
--   [prafta-platform-4-location-console.sql]                ← ★★선행 필수(4차 / qa R-5)
--        ↓   PURPOSE_CD / FAIL_CNT 컬럼을 만든다. 본 파일의 statement 다수가 PURPOSE_CD 를 참조한다.
--   [1차 DDL: prafta-sms-ppurio-1-send-result-columns.sql]  ← 반드시 먼저
--        ↓
--   [본 파일: prafta-sms-ppurio-2-rate-limit-policy.sql]
--        ↓
--   [3차 DDL: prafta-sms-ppurio-3-verify-limit-and-fixes.sql]
--        ↓
--   [4차 DDL: prafta-sms-ppurio-4-verify-redesign-and-retention.sql]
--        ↓
--   [백엔드 배포]  →  [웹 프론트 배포(Platform_05)]
--
--   ★platform-4 선행 확인(개발·운영 양쪽):
--     SHOW COLUMNS FROM tb_sms_auth_code LIKE 'PURPOSE_CD';   -- 1건이어야 한다
--     SHOW COLUMNS FROM tb_sms_auth_code LIKE 'FAIL_CNT';     -- 1건이어야 한다
--   ★lower_case_table_names 확인: SELECT @@lower_case_table_names;  -- 1 이어야 한다(sec T-10)
--
--   ★1차 DDL 미적용 상태에서 본 파일의 ③ 인덱스(idx_sms_auth_send_stat)를 실행하면
--     SEND_STATUS / SEND_DATE 컬럼이 없어 1072(Key column doesn't exist) 로 실패한다.
--   ★본 DDL 없이 2차 백엔드 코드를 먼저 기동하면 1146(TB_SMS_SEND_POLICY 부재) /
--     1054(SEND_IP_HASH·SEND_USER_CD 부재) 로 5개 인증 흐름이 전부 사망한다.
--   ★반대로 DDL 만 먼저 적용하는 것은 전면 무회귀다(구 코드는 신규 컬럼/테이블을 참조하지 않는다).
--
-- 적용 전 확인(필수 — MCP read-only 로 실행):
--   SHOW COLUMNS FROM tb_sms_auth_code LIKE 'SEND_%';
--     → 6건(1차 DDL 적용 완료)이어야 본 파일 적용 가능. 0건이면 1차부터.
--   SHOW TABLES LIKE 'TB_SMS_SEND_POLICY';                    -- 0건이어야 신규 적용 대상
--   SELECT SYST_VAL_D_CD, SYST_VAL_D_NM FROM tb_syst_val_d WHERE SYST_VAL_CD='SYS060';
--     → ★아래 ⑤ 의 '07' 이 이미 점유돼 있으면 다음 미사용 번호로 바꿔 실행할 것(추측 금지).
--   SELECT MENU_D_ID, MENU_IDX FROM tb_syst_menu_d WHERE MENU_M_ID='platform' ORDER BY MENU_IDX;
--     → ★아래 ⑥ 의 MENU_IDX=5 가 실측과 다르면 실측 최대값+1 로 바꿔 실행할 것
--       (선례: Attd_15 시드 IDX 가 실측과 달라 결함 발생).
--
-- 멱등성: CREATE/ALTER/INSERT 중복 실행 시 1050/1060/1061/PK 충돌. 이미 반영된 환경에서는 건너뛸 것.
-- ⚠️ 개발 DB·운영 DB 양쪽에 적용한다(feedback_db_migration_apply_both_envs).
-- ⚠️ 운영 적용은 사용자 수동(read-only MCP). 본 파일은 작성만, DB 직접 적용 금지.
-- ============================================================================


-- ── 1) 발송 다층 상한 정책 + 킬스위치 (전역 단일행) ──
--   ★저장소 선정 근거(plan §0-5): TB_SYST_VAL_D 재사용을 기각했다.
--     ① tb_syst_val_d.UPDATE_DATE 가 date 타입이라 킬스위치 "마지막 발동 시각"을 초 단위로 못 남긴다(결정적).
--     ② 값 슬롯이 varchar(50) 2개뿐이라 임계값 8 + 킬스위치 4필드를 담으려면 행 12개로 쪼개야 한다.
--     ③ 코드 테이블은 공통 코드 조회 경로로 고객사에 노출될 수 있다(운영 임계값이 테넌트로 샌다).
--     ④ 동종 선례가 이미 전용 테이블이다(회사별 AI 토큰 한도 = TB_AI_TOKEN_QUOTA).
--     ⑤ TOCTOU 봉인에 "잠글 행"이 필요한데 코드 테이블 행을 발송 경로에서 매번 잠그는 것은 명백한 오용.
--   ★키-값형이 아니라 컬럼형인 이유: 뿌리오 단일 계정(D1)이라 전역 1벌 고정이고, 행이 늘지 않으므로
--     타입 안전 + 판정 쿼리 1회로 끝난다.
--   ★확정 수치는 전부 이 DDL 의 DEFAULT 로만 존재한다. 백엔드 코드에는 어떤 임계값도 하드코딩하지 않는다.
CREATE TABLE `TB_SMS_SEND_POLICY` (
  `POLICY_ID`               varchar(20)  NOT NULL COMMENT '정책 ID. 뿌리오 단일 계정(D1)이므로 DEFAULT 1행 고정',
  `PHONE_WINDOW_SEC`        int          NOT NULL DEFAULT 55  COMMENT '번호별 연속 발송 최소 간격(초). ★프론트 재발송 타이머 60초보다 반드시 짧아야 한다(60이면 타이머 만료 직후 클릭이 초 절단 때문에 약 90% 차단된다). 화면에서 1~59 만 저장 허용',
  `PHONE_HOUR_LIMIT`        int          NOT NULL DEFAULT 10  COMMENT '번호별 시간당 발송 상한(건). 0 이하=무제한',
  `PHONE_DAY_LIMIT`         int          NOT NULL DEFAULT 20  COMMENT '번호별 일별 발송 상한(건). 0 이하=무제한',
  `IP_AXIS_ENABLED_YN`      varchar(1)   NOT NULL DEFAULT 'N' COMMENT 'IP축 실차단 여부 Y:차단 N:관측만(기본). ★1단계는 반드시 N - 운영 nginx 의 X-Forwarded-For 구성(홉 수)을 정적으로 확인할 수 없어, 확정 전에 차단하면 전 사용자 오차단 또는 무의미 둘 중 하나가 된다. 운영 진단 로그로 홉 수를 확정한 뒤 Platform_05 화면에서 Y 로 전환한다',
  `IP_HOUR_LIMIT`           int          NOT NULL DEFAULT 10  COMMENT '요청 IP별 시간당 상한(건). ★IP 를 신뢰 수준으로 확정하지 못하면 이 축은 판정하지 않는다(fail-open). 0 이하=무제한',
  `IP_DAY_LIMIT`            int          NOT NULL DEFAULT 20  COMMENT '요청 IP별 일별 상한(건). 0 이하=무제한',
  `USER_HOUR_LIMIT`         int          NOT NULL DEFAULT 10  COMMENT '로그인 사용자별 시간당 상한(건). 진입점 B(앱 휴대폰변경)·C(플랫폼 위치열람) 전용. 무인증 흐름(A)은 미적용. 0 이하=무제한',
  `USER_DAY_LIMIT`          int          NOT NULL DEFAULT 20  COMMENT '로그인 사용자별 일별 상한(건). 0 이하=무제한',
  `GLOBAL_HOUR_LIMIT`       int          NOT NULL DEFAULT 500 COMMENT '전역 시간당 실발송 상한(건). 초과 시 킬스위치 자동 발동. 0 이하=무제한',
  `KILL_SWITCH_YN`          varchar(1)   NOT NULL DEFAULT 'N' COMMENT '킬스위치 Y:발동(발송 전면중지) N:정상. ★자동 해제 금지 - 해제는 Platform_05 화면에서 운영자 수동만',
  `KILL_SWITCH_AT`          datetime     NULL     COMMENT '최근 발동 시각. ★해제해도 지우지 않는다(마지막 발동 이력 보존 - 화면 표시 요건)',
  `KILL_SWITCH_REASON`      varchar(200) NULL     COMMENT '최근 발동 사유(예: 전역 시간당 상한 초과(512/500)). ★PII 적재 금지',
  `KILL_SWITCH_RELEASE_AT`  datetime     NULL     COMMENT '최근 수동 해제 시각',
  `KILL_SWITCH_RELEASE_NO`  varchar(50)  NULL     COMMENT '최근 해제자 USER_CD(플랫폼 운영자, 토큰에서만 결정)',
  `INSERT_NO`               varchar(50)  NOT NULL COMMENT '입력자',
  `INSERT_DATE`             datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  `UPDATE_NO`               varchar(50)  NULL     COMMENT '수정자',
  `UPDATE_DATE`             datetime     NULL     COMMENT '수정일시. ★datetime 이다 - tb_syst_val_d 처럼 date 로 두면 변경 시각을 초 단위로 못 남긴다',
  PRIMARY KEY (`POLICY_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='SMS 발송 다층 상한 정책 + 킬스위치(전역 1행. 뿌리오 단일 계정 D1)';

-- 시드 1행. 나머지 컬럼은 위 DEFAULT 로 채워진다(= 확정 수치가 DDL 에만 존재).
--   ★행이 없으면 백엔드는 차단하지 않고(fail-open) error 로그만 남긴다 —
--     시드 누락으로 5개 인증 흐름이 통째로 죽는 사고를 막기 위함(최종 방어는 prafta.sms.enabled 게이트).
INSERT INTO `TB_SMS_SEND_POLICY` (`POLICY_ID`, `INSERT_NO`) VALUES ('DEFAULT', 'SYSTEM');


-- ── 2) IP축 / 사용자축 카운트 재료 컬럼 ──
--   ★SEND_IP_HASH 는 HMAC 해시만 저장한다(IP 평문 저장 금지 - 공통 정책서 §11.1 최소 수집).
--     길이 43 = HmacSigner.hmacSha256Base64Url 출력(MBL_NO_HMAC char(43) 과 동일).
ALTER TABLE `tb_sms_auth_code`
    ADD COLUMN `SEND_IP_HASH` char(43) NULL
        COMMENT '요청 IP 해시(HMAC-SHA256 Base64Url, 도메인 구분자 sms-ip: 접두). ★IP 평문 저장 금지. IP축 카운트 전용이며 역추적 용도가 아니다. 신뢰 수준 미확정 시 NULL(fail-open)'
        AFTER `SEND_DATE`,
    ADD COLUMN `SEND_USER_CD` varchar(20) NULL
        COMMENT '발송 요청 사용자 USER_CD. 로그인 흐름(B:앱 휴대폰변경 / C:플랫폼 위치열람)만 적재. 무인증 흐름(A)은 NULL'
        AFTER `SEND_IP_HASH`;


-- ── 3) 상한 판정용 인덱스 ──
--   번호 축은 기존 idx_sms_auth_mbl_hmac_ins 로 커버되므로 추가하지 않는다.
--   ★전역 축의 PENDING 분기는 INSERT_DATE 를 쓰는데 아래 인덱스로 커버되지 않는다.
--     현재 규모(누적 272행)에서는 무해하고 발송당 1회 판정이라 추가하지 않았다.
--     테이블이 수십만 행 규모가 되면 (SEND_STATUS, INSERT_DATE) 인덱스를 추가할 것.
ALTER TABLE `tb_sms_auth_code`
    ADD KEY `idx_sms_auth_ip_ins`    (`SEND_IP_HASH`, `INSERT_DATE`),
    ADD KEY `idx_sms_auth_user_ins`  (`SEND_USER_CD`, `INSERT_DATE`),
    ADD KEY `idx_sms_auth_send_stat` (`SEND_STATUS`, `SEND_DATE`);


-- ── 4) PURPOSE_CD COMMENT 갱신 (SMS2-D5 전제) ──
--   ★기존 행의 값을 변환하지 않는다. 변환하면 배포 순간 진행 중이던 인증 코드가 통째로 깨진다.
--     신규 INSERT 부터 MOBILE_CHANGE 가 적재된다.
--   ★타입/제약은 prafta-platform-4-location-console.sql 원문과 100% 동일하게 재기술한다(COMMENT 만 변경).
ALTER TABLE `tb_sms_auth_code`
    MODIFY COLUMN `PURPOSE_CD` varchar(20) NOT NULL DEFAULT 'SELF_JOIN'
        COMMENT '인증 목적 SELF_JOIN:셀프가입·본인인증·비밀번호재설정 PLATFORM_LOCATION:플랫폼 위치정보 열람 게이트 MOBILE_CHANGE:앱 마이페이지 휴대폰 변경(SMS2-D5 신설)';


-- ── 5) 감사 액션 코드 시드 (SYS060) ──
--   ★SYST_VAL_D_CD 값은 적용 전 확인 쿼리로 실측한 뒤 확정할 것. '07' 이 점유돼 있으면 다음 미사용 번호로.
--     (AuditActionType javadoc 이 02~06 을 권한/상태/조직/삭제/조회용으로 예약해 두었다)
--   ★번호를 바꿔 실행했다면 AuditActionType.SETTING_CHANGE 상수값도 함께 바꿔야 한다(코드-DB 짝).
INSERT INTO `tb_syst_val_d`
      (`SYST_VAL_CD`, `SYST_VAL_D_CD`, `SYST_VAL_D_NM`, `SORT_IDX`, `USE_YN`, `VAL_D_DESC`, `INSERT_NO`)
VALUES ('SYS060', '07', '설정 변경', 7, 'Y', '운영 설정값 변경(SMS 발송 임계값·킬스위치 해제 등)', 'SYSTEM');


-- ── 6) Platform_05 메뉴 시드 ──
--   ★MENU_IDX 는 적용 전 확인 쿼리로 실측한 뒤 확정할 것(Platform_01~04 가 1~4 라는 전제).
--   ★BTN_SAVE='Y' — Platform_03/04 는 read-only 콘솔이라 전부 'N' 이었으나 Platform_05 는 임계값 저장이 있다.
INSERT INTO `tb_syst_menu_d`
      (`MENU_D_ID`, `MENU_M_ID`, `MENU_VIEW`, `MENU_NM`, `SUB_GROUP_NM`, `SUB_GROUP_IDX`, `MENU_IDX`, `USE_YN`, `INSERT_NO`)
VALUES ('Platform_05', 'platform', 'platform/Platform_05.vue', 'SMS 발송 관리', '플랫폼 운영', 1, 5, 'Y', 'SYSTEM');

INSERT INTO `tb_syst_auth_menu`
      (`CMPNY_CD`, `AUTH_CD`, `MENU_D_ID`, `USE_YN`, `BTN_SRCH`, `BTN_NEW`, `BTN_DELT`, `BTN_SAVE`, `BTN_EXCL`, `INSERT_NO`)
VALUES ('prafta_system_admin', 'master', 'Platform_05', 'Y', 'Y', 'N', 'N', 'Y', 'N', 'SYSTEM');


-- ============================================================================
-- 적용 후 검증 (권장)
-- ----------------------------------------------------------------------------
-- SHOW CREATE TABLE TB_SMS_SEND_POLICY;
--   → 컬럼 19개, PRIMARY KEY(POLICY_ID), UPDATE_DATE 가 datetime 인지 확인.
-- SELECT POLICY_ID, PHONE_WINDOW_SEC, PHONE_HOUR_LIMIT, PHONE_DAY_LIMIT
--      , IP_AXIS_ENABLED_YN, IP_HOUR_LIMIT, IP_DAY_LIMIT, USER_HOUR_LIMIT, USER_DAY_LIMIT
--      , GLOBAL_HOUR_LIMIT, KILL_SWITCH_YN FROM TB_SMS_SEND_POLICY;
--   → 1행 / 55,10,20,'N',10,20,10,20,500,'N'
--   → ★IP_AXIS_ENABLED_YN 이 'N' 인지 반드시 확인(1단계는 관측 전용).
-- SHOW COLUMNS FROM tb_sms_auth_code LIKE 'SEND_%';
--   → 8건(1차 6 + 2차 2).
-- SHOW INDEX FROM tb_sms_auth_code;
--   → idx_sms_auth_ip_ins / idx_sms_auth_user_ins / idx_sms_auth_send_stat 존재.
-- SELECT PURPOSE_CD, COUNT(1) FROM tb_sms_auth_code GROUP BY PURPOSE_CD;
--   → ★분포 무변화(MODIFY 는 COMMENT 만 바꿨다). 변했다면 즉시 중단하고 보고.
-- SELECT SYST_VAL_D_CD, SYST_VAL_D_NM FROM tb_syst_val_d WHERE SYST_VAL_CD='SYS060' ORDER BY SORT_IDX;
-- SELECT MENU_D_ID, MENU_IDX, USE_YN FROM tb_syst_menu_d WHERE MENU_D_ID='Platform_05';
-- ============================================================================

-- ============================================================================
-- 롤백 (필요 시 수동 실행)
-- ----------------------------------------------------------------------------
-- DELETE FROM `tb_syst_auth_menu` WHERE `MENU_D_ID`='Platform_05';
-- DELETE FROM `tb_syst_menu_d`    WHERE `MENU_D_ID`='Platform_05';
-- DELETE FROM `tb_syst_val_d`     WHERE `SYST_VAL_CD`='SYS060' AND `SYST_VAL_D_CD`='07';
-- ALTER TABLE `tb_sms_auth_code`
--     DROP KEY `idx_sms_auth_send_stat`,
--     DROP KEY `idx_sms_auth_user_ins`,
--     DROP KEY `idx_sms_auth_ip_ins`,
--     DROP COLUMN `SEND_USER_CD`,
--     DROP COLUMN `SEND_IP_HASH`;
-- DROP TABLE `TB_SMS_SEND_POLICY`;
--   ※ PURPOSE_CD COMMENT 는 원문(prafta-platform-4-location-console.sql §2)으로 되돌릴 것.
-- ============================================================================
