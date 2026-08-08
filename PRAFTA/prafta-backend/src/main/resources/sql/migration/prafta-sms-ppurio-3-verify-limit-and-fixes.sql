-- ============================================================================
-- PRAFTA-SMS-PPURIO-3 — 2차 검증(sec N-1~N-15 / qa Q-1~Q-8) 결함 수정용 DDL
-- 작성일: 2026-08-08
-- 적용 환경: 운영 RDS MySQL 8.4.10 / 개발 MySQL 8.0.42 (양쪽 모두 적용)
-- 참조: 작업지시서_SMS-발송-뿌리오-연동-2차.sec.md N-2·N-3 / .qa.md Q-8
--
-- 변경 요약
--   1) TB_SMS_SEND_POLICY — 검증(대입) 방어 임계값 3컬럼 추가.
--        VERIFY_FAIL_LIMIT / VERIFY_LOCK_SEC / VERIFY_HOUR_LIMIT
--        ★2차까지 코드 상수였던 "대입 5회" 를 정책 테이블로 옮긴다(임계값 하드코딩 0건 원칙).
--   2) tb_sms_auth_code — FAIL_LOCKED_AT 컬럼 추가(N-2 잠금시간 기반 전환의 기준 시각).
--        ★UPDATE_DATE 를 기준 시각으로 재사용하지 않는다 — 인증 창 4개가 그 컬럼을 기산점으로 쓴다.
--   3) TB_SMS_VERIFY_ATTEMPT 신설 — 인증번호 "검증 시도" 자체의 시간당 상한(N-3) 재료.
--        ★발송 축(tb_sms_auth_code)과 분리한다. 검증 시도를 발송 카운트에 섞으면
--          발송 상한이 검증 트래픽으로 오염된다(sec N-3 명시 경고).
--
-- ============================================================================
-- ⚠️ 적용 순서 (엄수)  ★★[4차 / qa R-5] platform-4 선행 의존을 추가 명시했다
-- ----------------------------------------------------------------------------
--   [prafta-platform-4-location-console.sql]                ← ★★선행 필수
--        ↓
--   [1차 DDL: prafta-sms-ppurio-1-send-result-columns.sql]
--        ↓
--   [2차 DDL: prafta-sms-ppurio-2-rate-limit-policy.sql]
--        ↓
--   [본 파일: prafta-sms-ppurio-3-verify-limit-and-fixes.sql]
--        ↓
--   [4차 DDL: prafta-sms-ppurio-4-verify-redesign-and-retention.sql]
--        ↓
--   [백엔드 배포]  →  [웹 프론트 배포(Platform_05)]
--
--   ★★본 파일 ② 의 `AFTER FAIL_CNT` 는 prafta-platform-4-location-console.sql:54-58 이 만든 컬럼에 의존한다.
--     FAIL_CNT / PURPOSE_CD 는 1~3차가 만드는 것이 <b>아니다</b>. platform-4 미적용이면 여기서 1054 로 실패하고,
--     2·3·4차 코드 전체(모든 SMS statement 가 PURPOSE_CD 참조)가 동작하지 않는다.
--     개발 DB 실측(2026-08-08): PURPOSE_CD varchar(20) NOT NULL DEFAULT 'SELF_JOIN' / FAIL_CNT int NOT NULL DEFAULT 0 존재.
--     ★운영 DB 는 미확인 — 반드시 아래 확인 쿼리를 먼저 실행할 것.
--   ★2차 DDL 미적용 상태에서 본 파일의 ① 을 실행하면 TB_SMS_SEND_POLICY 부재로 1146 실패한다.
--   ★본 DDL 없이 3차 백엔드를 먼저 기동하면
--       - 1054(VERIFY_FAIL_LIMIT / FAIL_LOCKED_AT 부재)
--       - 1146(TB_SMS_VERIFY_ATTEMPT 부재)
--     로 인증번호 "검증" 5개 흐름이 전부 사망한다(발송이 아니라 검증이다 — 회원가입/계정찾기/
--     비밀번호재설정/일용직 셀프가입/앱 휴대폰변경).
--   ★반대로 DDL 만 먼저 적용하는 것은 전면 무회귀다(2차 코드는 신규 컬럼/테이블을 참조하지 않는다).
--
-- 적용 전 확인(필수 — MCP read-only 로 실행):
--   ★★platform-4 선행 확인(개발·운영 양쪽. 0건이면 여기서 멈추고 platform-4 부터):
--   SHOW COLUMNS FROM tb_sms_auth_code LIKE 'PURPOSE_CD';    -- 1건이어야 한다
--   SHOW COLUMNS FROM tb_sms_auth_code LIKE 'FAIL_CNT';      -- 1건이어야 한다
--   ★lower_case_table_names 확인(sec T-10):
--   SELECT @@lower_case_table_names;                         -- 1 이어야 한다
--
--   SHOW COLUMNS FROM TB_SMS_SEND_POLICY LIKE 'VERIFY_%';   -- 0건이어야 신규 적용 대상
--   SHOW COLUMNS FROM tb_sms_auth_code   LIKE 'FAIL_%';      -- FAIL_CNT 1건만 있어야 함
--   SHOW TABLES LIKE 'TB_SMS_VERIFY_ATTEMPT';                -- 0건이어야 신규 적용 대상
--
-- 멱등성: ALTER/CREATE 중복 실행 시 1060/1050. 이미 반영된 환경에서는 건너뛸 것.
-- ⚠️ 개발 DB·운영 DB 양쪽에 적용한다(feedback_db_migration_apply_both_envs).
-- ⚠️ 운영 적용은 사용자 수동(read-only MCP). 본 파일은 작성만, DB 직접 적용 금지.
-- ============================================================================


-- ── 1) 검증(대입) 방어 임계값 ──
--   ★2차에서는 "A1 은 DDL 불필요가 성립 조건" 이라 대입 상한 5 를 SQL 리터럴로 뒀다.
--     3차에서 N-2(잠금시간 전환)·N-3(검증 시도 상한)이 어차피 DDL 을 요구하므로,
--     이 참에 임계값을 전부 정책행으로 올려 "코드 하드코딩 0건" 을 완성한다.
--   ★백엔드는 정책행이 없을 때만 코드 폴백 상수(5 / 180 / 30)를 쓴다.
--     ★[4차 / qa R-4 정정] 3차 원문의 "그 경로는 부팅 시 발송 게이트 강제 OFF(N-9)로 즉시 드러난다" 는
--       <b>사실이 아니었다</b> — 부팅 검증이 VERIFY_* 컬럼과 TB_SMS_VERIFY_ATTEMPT 를 보지 않았고,
--       게이트 OFF 면 아예 실행되지도 않았다. 4차에서 둘 다 보완해 이제는 성립한다.
ALTER TABLE `TB_SMS_SEND_POLICY`
    ADD COLUMN `VERIFY_FAIL_LIMIT` int NOT NULL DEFAULT 5
        COMMENT '인증번호 대입 실패 허용 횟수. 도달하면 해당 번호의 검증을 VERIFY_LOCK_SEC 동안 거부한다. 0 이하=무제한(권장하지 않음 - 계정 탈취 방어가 사라진다)'
        AFTER `GLOBAL_HOUR_LIMIT`,
    ADD COLUMN `VERIFY_LOCK_SEC` int NOT NULL DEFAULT 180
        COMMENT '대입 실패 상한 도달 시 검증 거부 시간(초). ★영구 무효화가 아니라 시간 잠금인 이유: 공격자가 피해자 번호로 오답 5회만 던지면 피해자의 계정 복구가 무기한 차단되기 때문(sec N-2 / qa Q-8). login.lock.duration-minutes=3 선례와 정합해 기본 180'
        AFTER `VERIFY_FAIL_LIMIT`,
    ADD COLUMN `VERIFY_HOUR_LIMIT` int NOT NULL DEFAULT 30
        COMMENT '번호(HMAC)별 시간당 인증번호 검증 시도 상한(건). ★발송 축과 완전히 별개다 - 검증 EP 는 무인증인데 2차까지 호출 자체에 상한이 없어 DB 쓰기 DoS 가 가능했다(sec N-3). 0 이하=무제한'
        AFTER `VERIFY_LOCK_SEC`;


-- ── 2) 대입 잠금 기준 시각 ──
--   ★★UPDATE_DATE 를 재사용하지 않는 이유(절대 규칙):
--     tb_sms_auth_code.UPDATE_DATE 는 인증 창 4개의 기산점이다
--       - 비밀번호 재설정 10분 창(BaseinfoMapper.selectSmsVerifiedSmsId)
--       - 앱 휴대폰변경 5분 창(AppMypage01Mapper.selectRecentVerifiedSmsId)
--       - 플랫폼 위치열람 10분 창(PlatformLocationMapper.selectPlatformSmsVerified)
--     잠금 판정이 이 컬럼을 읽거나 쓰면 만료된 인증이 무한 연장되는 인증 우회가 된다.
--     그래서 잠금 전용 컬럼을 따로 둔다(1차의 SEND_DATE 분리와 동일한 사유).
ALTER TABLE `tb_sms_auth_code`
    ADD COLUMN `FAIL_LOCKED_AT` datetime NULL
        COMMENT '대입 실패 상한 도달 시각. 이 시각 + TB_SMS_SEND_POLICY.VERIFY_LOCK_SEC 까지 검증을 거부하고, 경과하면 FAIL_CNT 를 0 으로 되돌려 같은 코드로 재시도 가능하게 한다. ★UPDATE_DATE 를 쓰지 않는다(인증 창 4개 기산점 보호)'
        AFTER `FAIL_CNT`;


-- ── 3) 인증번호 검증 시도 로그(시간당 상한 재료) ──
--   ★왜 별도 테이블인가
--     ① tb_sms_auth_code 는 "발급된 코드" 의 테이블이라 "검증 시도" 를 셀 수 없다
--        (코드 1건에 여러 번 시도하고, 코드가 없어도 시도할 수 있다).
--     ② SmsRateLimitGuard(발송 축)를 재사용하면 검증 트래픽이 발송 상한을 소진시킨다(sec N-3 명시 금지).
--   ★PII: MBL_NO_HMAC 만 저장한다(평문 번호·인증번호·IP 평문 전부 금지).
--   ★보존: 판정은 최근 1시간만 본다. 무기한 누적되므로 purge 배치가 필요하다(sec 별건 백로그 5 와 동건).
--     → ★[4차] 4차 DDL 에서 idx_sms_verify_ins(INSERT_DATE) 인덱스를 추가하고
--       SmsRetentionScheduler(90일 purge, 기본 비활성)를 신설했다. 본 항목은 4차에서 해소됐다.
CREATE TABLE `TB_SMS_VERIFY_ATTEMPT` (
  `ATTEMPT_ID`  bigint      NOT NULL AUTO_INCREMENT COMMENT '검증 시도 일련번호',
  `MBL_NO_HMAC` char(43)    NOT NULL COMMENT '검증 대상 휴대폰 HMAC(HMAC-SHA256 Base64Url). ★평문 저장 금지',
  `PURPOSE_CD`  varchar(20) NOT NULL COMMENT '인증 목적 SELF_JOIN / MOBILE_CHANGE / PLATFORM_LOCATION. ★판정은 목적 무관 전체를 세지만(목적 교체 우회 차단) 진단용으로 남긴다',
  `INSERT_DATE` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '시도 시각(시간당 상한 판정 기준)',
  PRIMARY KEY (`ATTEMPT_ID`),
  KEY `idx_sms_verify_hmac_ins` (`MBL_NO_HMAC`, `INSERT_DATE`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='인증번호 검증 시도 로그(번호별 시간당 상한 재료. sec N-3)';


-- ============================================================================
-- 적용 후 검증 (권장)
-- ----------------------------------------------------------------------------
-- SELECT POLICY_ID, GLOBAL_HOUR_LIMIT, VERIFY_FAIL_LIMIT, VERIFY_LOCK_SEC, VERIFY_HOUR_LIMIT
--   FROM TB_SMS_SEND_POLICY;
--   → 1행 / 500, 5, 180, 30
--   → ★GLOBAL_HOUR_LIMIT 이 0 이면 즉시 1 이상으로 교정할 것(0=무제한이면 킬스위치가 영구 무력화된다 - sec N-7).
-- SHOW COLUMNS FROM tb_sms_auth_code LIKE 'FAIL_%';
--   → FAIL_CNT / FAIL_LOCKED_AT 2건.
-- SELECT COUNT(1) FROM tb_sms_auth_code WHERE FAIL_LOCKED_AT IS NOT NULL;
--   → 0 (신규 컬럼이라 기존 행은 전부 NULL = 잠금 없음 = 무회귀).
-- SHOW CREATE TABLE TB_SMS_VERIFY_ATTEMPT;
--   → idx_sms_verify_hmac_ins 존재.
--
-- ★N-1(킬스위치 자기 교착) 실증 — 백엔드 배포 후 반드시 수행할 것
--   UPDATE TB_SMS_SEND_POLICY SET GLOBAL_HOUR_LIMIT = 2 WHERE POLICY_ID = 'DEFAULT';
--   (게이트 ON 상태에서) 발송 3~4회 → 기대: KILL_SWITCH_YN='Y' 전이 / 응답 SMS_503_001 /
--   ★지연 50초 없음(있으면 수정이 반영되지 않은 것) / SHOW ENGINE INNODB STATUS 에 lock wait 0.
--   확인 후 UPDATE TB_SMS_SEND_POLICY SET GLOBAL_HOUR_LIMIT = 500, KILL_SWITCH_YN='N' WHERE POLICY_ID='DEFAULT';
-- ============================================================================

-- ============================================================================
-- 롤백 (필요 시 수동 실행)
-- ----------------------------------------------------------------------------
-- DROP TABLE `TB_SMS_VERIFY_ATTEMPT`;
-- ALTER TABLE `tb_sms_auth_code`   DROP COLUMN `FAIL_LOCKED_AT`;
-- ALTER TABLE `TB_SMS_SEND_POLICY` DROP COLUMN `VERIFY_HOUR_LIMIT`
--                                , DROP COLUMN `VERIFY_LOCK_SEC`
--                                , DROP COLUMN `VERIFY_FAIL_LIMIT`;
-- ============================================================================
