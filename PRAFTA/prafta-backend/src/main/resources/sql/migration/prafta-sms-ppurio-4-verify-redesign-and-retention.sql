-- ============================================================================
-- PRAFTA-SMS-PPURIO-4 — 3차 검증(sec T-1~T-11 / qa R-1~R-9) 결함 수정용 DDL
-- 작성일: 2026-08-08
-- 적용 환경: 운영 RDS MySQL 8.4.10 / 개발 MySQL 8.0.42 (양쪽 모두 적용)
-- 참조: 작업지시서_SMS-발송-뿌리오-연동-3차.sec.md T-3·T-8·T-11 / .qa.md R-5·R-6
--
-- 변경 요약
--   1) TB_SMS_VERIFY_ATTEMPT — idx_sms_verify_ins(INSERT_DATE) 인덱스 추가.
--        보존기간 purge 배치(SmsRetentionScheduler)가 풀스캔되지 않게 한다(sec T-3·T-11).
--   2) TB_SMS_SEND_POLICY.VERIFY_LOCK_SEC — DEFAULT 180 → 60, COMMENT 갱신(qa R-6).
--        + 기존 값이 3차 DEFAULT(180) 그대로인 행만 60 으로 정정한다(운영자가 손댄 값은 보존).
--   3) TB_SMS_VERIFY_ATTEMPT / tb_sms_auth_code — COMMENT 갱신(의미 변경 반영. 값 변환 없음).
--
-- ★DDL 이 아닌 코드 쪽 주요 변경(참고 — 여기서 실행할 것 없음)
--   - 검증 상한을 "실패한 시도" 에만 적용하고 코드 매칭을 먼저 한다(sec T-2 → 표적 DoS 해소).
--   - 상한 카운트 규칙을 화이트리스트 → 블랙리스트로 전환(sec T-4 → 2xx 접수분 누락 해소).
--   - guardAndInsert 를 READ_COMMITTED 로(sec T-1 → 잠금 안 재검사의 stale 스냅샷 해소).
--
-- ============================================================================
-- ⚠️ 적용 순서 (엄수)
-- ----------------------------------------------------------------------------
--   [prafta-platform-4-location-console.sql]                     ← ★★선행 필수(qa R-5)
--        ↓   PURPOSE_CD / FAIL_CNT 컬럼을 만든다. 3차 DDL 의 `AFTER FAIL_CNT` 가 여기에 의존한다.
--   [1차: prafta-sms-ppurio-1-send-result-columns.sql]
--        ↓
--   [2차: prafta-sms-ppurio-2-rate-limit-policy.sql]
--        ↓
--   [3차: prafta-sms-ppurio-3-verify-limit-and-fixes.sql]
--        ↓
--   [본 파일: prafta-sms-ppurio-4-verify-redesign-and-retention.sql]
--        ↓
--   [백엔드 배포]  →  [웹 프론트 배포(Platform_05)]
--
--   ★3차 DDL 미적용 상태에서 본 파일을 실행하면
--       - 1146 (TB_SMS_VERIFY_ATTEMPT 부재 — ①)
--       - 1054 (VERIFY_LOCK_SEC 부재 — ②)
--     로 실패한다. 반드시 3차를 먼저 적용할 것.
--   ★본 DDL 없이 4차 백엔드를 먼저 기동해도 <b>기동/동작은 된다</b>(회귀 없음).
--     다만 ① 인덱스가 없으면 purge 배치가 풀스캔이고, ② 없으면 잠금 시간이 180초로 남아
--     "기다렸다 같은 코드로 재시도" 가 여전히 성립하지 않는다.
--     purge 배치는 기본 비활성(prafta.sms.retention.enabled=false)이라 사고로 이어지지는 않는다.
--
-- 적용 전 확인(필수 — MCP read-only 로 실행):
--   SELECT @@lower_case_table_names;                             -- 1 이어야 한다(sec T-10)
--   SHOW COLUMNS FROM tb_sms_auth_code LIKE 'PURPOSE_CD';        -- 1건(platform-4 적용 확인)
--   SHOW COLUMNS FROM tb_sms_auth_code LIKE 'FAIL_CNT';          -- 1건(platform-4 적용 확인)
--   SHOW COLUMNS FROM TB_SMS_SEND_POLICY LIKE 'VERIFY_%';        -- 3건(3차 적용 확인)
--   SHOW TABLES LIKE 'TB_SMS_VERIFY_ATTEMPT';                    -- 1건(3차 적용 확인)
--   SHOW INDEX FROM TB_SMS_VERIFY_ATTEMPT WHERE Key_name='idx_sms_verify_ins';  -- 0건이어야 신규 적용 대상
--   SELECT VERIFY_LOCK_SEC FROM TB_SMS_SEND_POLICY WHERE POLICY_ID='DEFAULT';   -- 180 이면 ② 대상
--
-- 멱등성: ALTER 중복 실행 시 1061(Duplicate key name) / 1060. 이미 반영된 환경에서는 건너뛸 것.
-- ⚠️ 개발 DB·운영 DB 양쪽에 적용한다(feedback_db_migration_apply_both_envs).
-- ⚠️ 운영 적용은 사용자 수동(read-only MCP). 본 파일은 작성만, DB 직접 적용 금지.
-- ============================================================================


-- ── 1) 검증 시도 로그 — INSERT_DATE 단독 인덱스 ──
--   ★왜 필요한가(sec T-3 · T-11)
--     기존 인덱스는 idx_sms_verify_hmac_ins(MBL_NO_HMAC, INSERT_DATE) 하나뿐이라
--     "번호 무관 + 시각만" 으로 훑는 purge 배치(DELETE ... WHERE INSERT_DATE < cutoff)가 풀스캔이 된다.
--     행이 무기한 누적되는 테이블이라 나중에 켤 때 배치 1회가 테이블 전체를 잠글 수 있다.
--   ★판정 쿼리(countFailedVerifyAttempts)는 계속 hmac 인덱스를 쓴다 — 이 인덱스는 purge 전용이다.
ALTER TABLE `TB_SMS_VERIFY_ATTEMPT`
    ADD KEY `idx_sms_verify_ins` (`INSERT_DATE`);


-- ── 2) 대입 잠금 시간 기본값 180 → 60 ──
--   ★★왜 낮추는가(qa R-6)
--     잠금이 코드 TTL <b>이상</b>이면 잠금이 풀릴 때 코드가 이미 만료되어,
--     "기다렸다가 같은 코드로 재시도" 라는 시간 잠금의 존재 이유가 <b>어느 진입점에서도</b> 성립하지 않는다.
--       - 진입점 A(셀프가입·계정찾기·비번재설정): 코드 TTL 60초  < 잠금 180초  → 불성립
--       - 진입점 B(앱 휴대폰 변경)              : 코드 TTL 180초 <= 잠금 180초 → 불성립
--     ★3차 dev-progress §10-2-1 의 "진입점 B(TTL 3분)에서만 성립" 은 <b>사실이 아니다</b>(B 도 불성립).
--     60 으로 낮추면 B 에서는 실제로 성립하고, A 는 재발송이 필요한 상태가 그대로 유지된다.
--   ★방어가 약해지지 않는 근거
--     브루트포스 방어의 본체는 잠금이 아니라 "시간당 실패 시도 상한"(VERIFY_HOUR_LIMIT)이다.
--     4차부터 그 상한이 실패한 시도에만 적용되어 정상 사용자를 막지 않으면서 오답만 정확히 센다(sec T-2).
--     잠금은 한 코드에 대한 연속 대입 속도만 늦추는 보조 장치이므로 짧아도 무방하다.
--   ★VERIFY_LOCK_SEC <= 0 은 "무제한 잠금"(=2차 N-2 영구 무효화 부활)이라 백엔드가 하한을 강제한다(sec T-8).
--     DB 에 0 이 들어가도 SmsVerifyPolicy.effectiveLockSec() 이 60 으로 보정하고 error 로그를 남긴다.
ALTER TABLE `TB_SMS_SEND_POLICY`
    MODIFY COLUMN `VERIFY_LOCK_SEC` int NOT NULL DEFAULT 60
        COMMENT '대입 실패 상한 도달 시 검증 거부 시간(초). ★영구 무효화가 아니라 시간 잠금인 이유: 공격자가 피해자 번호로 오답 5회만 던지면 피해자의 계정 복구가 무기한 차단되기 때문(sec N-2 / qa Q-8). ★기본 60 인 이유: 코드 TTL(진입점 A 60초 / B 180초) 이상이면 잠금이 풀릴 때 코드가 이미 만료되어 잠금의 존재 이유가 사라진다(qa R-6). 0 이하는 설정 오류로 보고 백엔드가 60 으로 보정한다(sec T-8)';

--   기존 행 정정 — ★3차 DEFAULT(180) 그대로인 경우에만 바꾼다. 운영자가 의도적으로 넣은 값은 보존한다.
UPDATE `TB_SMS_SEND_POLICY`
   SET `VERIFY_LOCK_SEC` = 60
 WHERE `POLICY_ID` = 'DEFAULT'
   AND `VERIFY_LOCK_SEC` = 180;


-- ── 3) COMMENT 갱신(값 변환 없음 — 의미가 바뀐 컬럼의 설명을 실제와 맞춘다) ──
--   ★3차 COMMENT 는 "모든 검증 시도" 를 전제로 쓰였다. 4차부터는 <b>실패한 시도만</b> 적재된다(sec T-2).
--     설명이 사실과 다르면 다음 라운드의 판단을 오도한다.
ALTER TABLE `TB_SMS_VERIFY_ATTEMPT`
    MODIFY COLUMN `PURPOSE_CD` varchar(20) NOT NULL
        COMMENT '인증 목적 SELF_JOIN / MOBILE_CHANGE / PLATFORM_LOCATION. ★판정은 목적 무관 전체를 세지만(목적 교체 우회 차단) 진단용으로 남긴다',
    MODIFY COLUMN `INSERT_DATE` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP
        COMMENT '시도 시각(시간당 상한 판정 기준). ★4차부터 이 테이블에는 <실패한> 검증 시도만 적재된다 - 코드가 일치한 요청은 적재하지 않는다(sec T-2 표적 DoS 해소). 또한 그 번호에 최근 1일 내 발급된 코드가 있을 때만 적재한다(sec T-3)';

--   진입점 C(PLATFORM_LOCATION)는 아직 영구 무효화 체계라 FAIL_LOCKED_AT 이 항상 NULL 이다(qa R-9).
--   통계/purge 쿼리에서 오해하지 않도록 COMMENT 에 명기한다.
ALTER TABLE `tb_sms_auth_code`
    MODIFY COLUMN `FAIL_LOCKED_AT` datetime NULL
        COMMENT '대입 실패 상한 도달 시각. 이 시각 + TB_SMS_SEND_POLICY.VERIFY_LOCK_SEC 까지 검증을 거부하고, 경과하면 FAIL_CNT 를 0 으로 되돌려 같은 코드로 재시도 가능하게 한다. ★UPDATE_DATE 를 쓰지 않는다(인증 창 4개 기산점 보호). ★PURPOSE_CD=PLATFORM_LOCATION 행은 이 체계 밖이라 항상 NULL 이다(진입점 C 는 여전히 영구 무효화 - qa R-9)';


-- ============================================================================
-- 적용 후 검증 (권장)
-- ----------------------------------------------------------------------------
-- SHOW INDEX FROM TB_SMS_VERIFY_ATTEMPT;
--   → idx_sms_verify_hmac_ins / idx_sms_verify_ins 2건 + PRIMARY.
-- SELECT POLICY_ID, GLOBAL_HOUR_LIMIT, VERIFY_FAIL_LIMIT, VERIFY_LOCK_SEC, VERIFY_HOUR_LIMIT
--   FROM TB_SMS_SEND_POLICY;
--   → 1행 / 500, 5, 60, 30
--   → ★GLOBAL_HOUR_LIMIT 이 0 이면 즉시 1 이상으로 교정할 것(0=무제한이면 킬스위치가 영구 무력화된다 - sec N-7).
-- SELECT COUNT(1) FROM tb_sms_auth_code WHERE FAIL_LOCKED_AT IS NOT NULL;
--   → 기존 행은 전부 NULL(무회귀). 서비스 사용 후에는 0 이 아닐 수 있다.
--
-- ★purge 배치를 켜기 전 반드시 대상 건수를 먼저 확인할 것(되돌릴 수 없는 물리 삭제):
--   SELECT COUNT(1) FROM TB_SMS_VERIFY_ATTEMPT WHERE INSERT_DATE < DATE_SUB(NOW(), INTERVAL 90 DAY);
--   SELECT COUNT(1) FROM tb_sms_auth_code      WHERE INSERT_DATE < DATE_SUB(NOW(), INTERVAL 90 DAY);
--   → 확인 후 SMS_RETENTION_ENABLED=true 로 전환(기본 false).
--
-- ★T-1(동시성) 실증 — 백엔드 배포 후 반드시 수행할 것
--   SELECT @@transaction_isolation;   -- 세션 기본은 REPEATABLE-READ 여야 정상(가드만 READ COMMITTED 로 내린다)
--   SELECT @@binlog_format;           -- ★ROW 여야 한다. STATEMENT 면 READ COMMITTED 쓰기가 1665 로 거부된다
--   동일 번호로 20~50 요청을 <b>동시</b> 발사 →
--     SELECT COUNT(1) FROM tb_sms_auth_code
--      WHERE MBL_NO_HMAC = '<대상HMAC>' AND INSERT_DATE >= DATE_SUB(NOW(), INTERVAL 60 SECOND);
--     → ★1건이어야 한다. 2건 이상이면 T-1 미해소(선검사를 제거하고 재시도할 것).
--
-- ★N-1(킬스위치 자기 교착) 실증 — 백엔드 배포 후 반드시 수행할 것
--   UPDATE TB_SMS_SEND_POLICY SET GLOBAL_HOUR_LIMIT = 2 WHERE POLICY_ID = 'DEFAULT';
--   (게이트 ON 상태에서) 발송 3~4회 → 기대: KILL_SWITCH_YN='Y' 전이 / 응답 SMS_503_001 /
--   ★지연 50초 없음(있으면 수정이 반영되지 않은 것) / SHOW ENGINE INNODB STATUS 에 lock wait 0.
--   확인 후 UPDATE TB_SMS_SEND_POLICY SET GLOBAL_HOUR_LIMIT = 500, KILL_SWITCH_YN='N' WHERE POLICY_ID='DEFAULT';
--
-- ★R-2(대입 방어 롤백) 실증 — 인증대기('04') 계정으로 /comApi/login/verify-phone-auth 에 틀린 코드 10회
--   SELECT FAIL_CNT, FAIL_LOCKED_AT FROM tb_sms_auth_code ORDER BY SMS_ID DESC LIMIT 1;
--     → ★FAIL_CNT 가 증가해야 한다(0/NULL 이면 미해소).
--   SELECT COUNT(1) FROM TB_SMS_VERIFY_ATTEMPT WHERE INSERT_DATE >= DATE_SUB(NOW(), INTERVAL 10 MINUTE);
--     → ★증가해야 한다.
-- ============================================================================

-- ============================================================================
-- 롤백 (필요 시 수동 실행)
-- ----------------------------------------------------------------------------
-- ALTER TABLE `TB_SMS_VERIFY_ATTEMPT` DROP KEY `idx_sms_verify_ins`;
-- ALTER TABLE `TB_SMS_SEND_POLICY` MODIFY COLUMN `VERIFY_LOCK_SEC` int NOT NULL DEFAULT 180;
-- UPDATE `TB_SMS_SEND_POLICY` SET `VERIFY_LOCK_SEC` = 180 WHERE `POLICY_ID`='DEFAULT' AND `VERIFY_LOCK_SEC` = 60;
-- (3) COMMENT 는 되돌릴 필요가 없다(문서 성격).
-- ============================================================================
