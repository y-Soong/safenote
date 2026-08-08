-- ============================================================================
-- PRAFTA-SMS-PPURIO-1 — TB_SMS_AUTH_CODE 발송결과 컬럼(뿌리오 연동)
-- 작성일: 2026-08-07
-- 적용 환경: MySQL 8.0.42
-- 참조: 작업지시서_SMS-발송-뿌리오-연동.md §5(D3 단일 테이블 유지) / plan §1 SMS-PPURIO-01
--
-- 변경 요약
--   1) tb_sms_auth_code — 발송 상태/추적키/벤더 응답 컬럼 6개 추가(AFTER VERIFIED_YN).
--   2) tb_sms_auth_code — idx_sms_auth_send_ref 인덱스 추가(발송결과 UPDATE 의 단건 조회키).
--
-- ★설계 메모
--   - SEND_DATE 를 별도 컬럼으로 둔 이유: 발송 결과 기록이 UPDATE_DATE 를 건드리면
--     플랫폼 위치열람 10분 창(selectPlatformSmsVerified) 과 앱 검증토큰 5분 창(selectRecentVerifiedSmsId)
--     판정이 통째로 오염된다. UPDATE_DATE 는 "인증 통과 시각" 전용이다(절대 발송으로 갱신 금지).
--   - D3: 인증번호 1행 = 1발송의 엄격한 1:1 이므로 별도 발송로그 테이블(TB_SMS_SEND_LOG)을 만들지 않는다.
--
-- ★★[4차 / qa R-5] 선행 의존 명시 — 이 파일보다 먼저 적용돼 있어야 하는 것이 있다.
--   [prafta-platform-4-location-console.sql]  ← ★선행 필수
--        ↓  (PURPOSE_CD / FAIL_CNT 컬럼을 만든다 — 1~3차가 만드는 것이 아니다)
--   [본 파일: prafta-sms-ppurio-1] → [2차] → [3차] → [4차] → [백엔드] → [웹 프론트]
--
--   ★2·3·4차의 거의 모든 SMS statement 가 PURPOSE_CD 를 참조하고, 3차 DDL 은 `AFTER FAIL_CNT` 로
--     FAIL_CNT 의 존재에 의존한다. platform-4 미적용 환경이면 3차 DDL 이 1054 로 실패하고
--     2~4차 코드 전체가 동작하지 않는다.
--   ★메모리 project_prafta_platform_customer_list_location_view 에 "마이그 platform-4 미적용" 기록이 있다.
--     개발 DB 에는 두 컬럼 모두 존재함을 실측 확인(2026-08-08). ★운영은 미확인 — 반드시 아래 쿼리로 확인할 것.
--
--   선행 확인 쿼리(개발·운영 양쪽):
--     SHOW COLUMNS FROM tb_sms_auth_code LIKE 'PURPOSE_CD';   -- 1건이어야 한다
--     SHOW COLUMNS FROM tb_sms_auth_code LIKE 'FAIL_CNT';     -- 1건이어야 한다
--     → 0건이면 prafta-platform-4-location-console.sql 부터 적용할 것.
--
-- ★★[4차 / sec T-10] 테이블명 대소문자 혼용 전제
--   SMS DDL 4벌은 TB_SMS_SEND_POLICY(대문자) 와 tb_sms_auth_code(소문자) 를 섞어 쓴다.
--   lower_case_table_names=1 전제가 성립해야 하며, 개발·운영 양쪽에서 적용 전에 확인할 것:
--     SELECT @@lower_case_table_names;   -- 1 이어야 한다(0 이면 대소문자 구분 → 1146 발생)
--   (선례: RDS 8.4 업그레이드 시 lower_case_table_names 불일치로 복원 실패 — project_prafta_rds_mysql_84_upgrade)
--
-- ⚠️ 적용 순서 (엄수) — [DB 적용 → 백엔드 배포]
--   SMS-PPURIO-04/05/06 코드는 INSERT 문에 SEND_REF_KEY 를, 레이트리밋 조회에 SEND_STATUS 를 참조한다.
--   따라서 본 DDL 없이 코드를 먼저 기동하면 1054(Unknown column) 로 5개 인증 흐름이 전부 사망한다.
--   반대로 DDL 만 먼저 적용하는 것은 전면 무회귀다(구 코드는 신규 컬럼을 참조하지 않고, 전부 DEFAULT 로 채워진다).
--
-- 적용 전 확인(권장):
--   SHOW COLUMNS FROM tb_sms_auth_code LIKE 'SEND_%';           -- 0건이어야 신규 적용 대상
--   SELECT COUNT(1) FROM tb_sms_auth_code;                      -- 기존 행 수(2026-08-07 기준 272행)
--
-- 멱등성: ALTER 중복 실행 시 1060(Duplicate column) 에러. 이미 반영된 환경에서는 건너뛸 것.
-- ⚠️ 개발 DB·운영 DB 양쪽에 적용한다(feedback_db_migration_apply_both_envs).
-- ⚠️ 운영 적용은 사용자 수동(read-only MCP). 본 파일은 작성만, DB 직접 적용 금지.
-- ============================================================================

-- ── 1) 발송 결과 컬럼 6개 ──
--   기존 272행은 DEFAULT 'PENDING' 으로 백필된다(발송 연동 전 데이터이므로 의미상 정확).
--   신규 행은 INSERT 시 SEND_REF_KEY 만 채우고 PENDING 으로 시작 → 발송 후 SENT/FAILED/SKIPPED 로 전이.
ALTER TABLE `tb_sms_auth_code`
    ADD COLUMN `SEND_STATUS` varchar(10) NOT NULL DEFAULT 'PENDING'
        COMMENT '발송상태 PENDING:발송전(기본) SENT:발송성공 FAILED:발송실패 SKIPPED:게이트OFF로 미발송'
        AFTER `VERIFIED_YN`,
    ADD COLUMN `SEND_REF_KEY` varchar(32) NULL
        COMMENT '뿌리오 refKey(요청 추적/발송결과 UPDATE 조인키, 32자 영숫자 랜덤). 인증코드 INSERT 시 선생성 저장'
        AFTER `SEND_STATUS`,
    ADD COLUMN `SEND_MSG_KEY` varchar(64) NULL
        COMMENT '뿌리오 응답 messageKey(추후 도달결과 대사용. 본 작업은 적재만)'
        AFTER `SEND_REF_KEY`,
    ADD COLUMN `SEND_ERR_CD` varchar(50) NULL
        COMMENT '발송 실패코드(벤더 원문 그대로. 코드 체계 미상이라 매핑하지 않음)'
        AFTER `SEND_MSG_KEY`,
    ADD COLUMN `SEND_ERR_MSG` varchar(500) NULL
        COMMENT '발송 실패사유(벤더 원문 500자 절단. ★인증번호·휴대폰 평문 적재 금지)'
        AFTER `SEND_ERR_CD`,
    ADD COLUMN `SEND_DATE` datetime NULL
        COMMENT '발송 결과 확정 시각(성공/실패/스킵 공통). ★UPDATE_DATE 와 별개 — UPDATE_DATE 는 인증통과 시각이라 발송으로 갱신 금지'
        AFTER `SEND_ERR_MSG`;

-- ── 2) 발송결과 UPDATE 용 인덱스 ──
--   결과 기록은 WHERE SEND_REF_KEY = ? 로 단건을 찍는다. 인덱스가 없으면 테이블 증가에 따라 풀스캔이 된다.
ALTER TABLE `tb_sms_auth_code`
    ADD KEY `idx_sms_auth_send_ref` (`SEND_REF_KEY`);

-- ============================================================================
-- 적용 후 검증 (권장)
-- ----------------------------------------------------------------------------
-- SHOW COLUMNS FROM tb_sms_auth_code LIKE 'SEND_%';
--   → 6개(SEND_STATUS/SEND_REF_KEY/SEND_MSG_KEY/SEND_ERR_CD/SEND_ERR_MSG/SEND_DATE),
--     SEND_STATUS 는 Null=NO / Default=PENDING 확인.
-- SELECT SEND_STATUS, COUNT(1) FROM tb_sms_auth_code GROUP BY SEND_STATUS;
--   → 기존 행 전량 'PENDING' 백필 확인.
-- SHOW INDEX FROM tb_sms_auth_code WHERE Key_name='idx_sms_auth_send_ref';
-- ============================================================================

-- ============================================================================
-- 롤백 (필요 시 수동 실행)
-- ----------------------------------------------------------------------------
-- ALTER TABLE `tb_sms_auth_code`
--     DROP KEY `idx_sms_auth_send_ref`,
--     DROP COLUMN `SEND_DATE`,
--     DROP COLUMN `SEND_ERR_MSG`,
--     DROP COLUMN `SEND_ERR_CD`,
--     DROP COLUMN `SEND_MSG_KEY`,
--     DROP COLUMN `SEND_REF_KEY`,
--     DROP COLUMN `SEND_STATUS`;
-- ============================================================================
