-- ============================================================================
-- PRAFTA-COM-002 — FCM 전송 워커: tb_user_device.DEL_YN 추가 + outbox SEND_STATUS 카탈로그 정합
-- 작성일: 2026-06-03
-- 적용 환경: MySQL 8.0.42
-- 참조: .claude/requests/common/prafta-com-002-plan.md §1, PRAFTA-COM-002-1
--       .claude/requests/common/refs/prafta-com-002/01_작업지시서_FCM전송워커.md §1, §4
--       .claude/requests/common/prafta-com-002-decisions.md B-2(무효토큰 soft-delete), B-3(claim 전이)
--
-- 변경 요약
--   1) tb_user_device.DEL_YN char(1) NOT NULL DEFAULT 'N' 추가 (무효 토큰 soft-delete, B-2 옵션A).
--      - FCM UNREGISTERED/INVALID_ARGUMENT 응답 시 해당 디바이스 행을 DEL_YN='Y' 마킹.
--      - 토큰 조회는 DEL_YN='N' AND PUSH_TOKEN IS NOT NULL 만 대상으로 한다.
--      - PUSH_TOKEN 자체는 보존(감사). 조회에서 DEL_YN 으로만 제외.
--      - 기존 행은 NOT NULL DEFAULT 'N' 으로 전부 'N' 백필된다.
--      - 위치: UPDATE_DATE 다음 (AFTER UPDATE_DATE) — 감사 컬럼 뒤 관례.
--   2) tb_noti_outbox.SEND_STATUS COMMENT 에 'SENDING' 코드값 추가 (claim 임시상태 정합).
--      - 워커가 PENDING → SENDING(claim) → SENT/FAILED 또는 PENDING(재시도 복귀) 으로 전이한다.
--      - 컬럼 타입/제약 변경 없음(varchar10, CHECK 없음). COMMENT 카탈로그만 갱신한다.
--      - 코드성 컬럼 COMMENT 규칙: '설명[코드] 코드값:의미' 형식으로 4종 모두 등재.
--
-- 적용 전 부재/현황 확인 (운영 적용 직전 권장):
--   SHOW COLUMNS FROM tb_user_device LIKE 'DEL_YN';
--   SHOW FULL COLUMNS FROM tb_noti_outbox LIKE 'SEND_STATUS';
--
-- ⚠️ 적용 선행성(중요): 본 ALTER(특히 DEL_YN) 가 운영 DB 에 적용되기 전에는
--   PushOutboxMapper.selectDeviceTokens / softDeleteDeviceToken 의 DEL_YN 참조가
--   "Unknown column" 으로 실패한다(워커 토큰 조회 전면 실패). 워커 게이트
--   (prafta.push.worker.enabled) 는 기본 false 라 운영 사고는 없으나,
--   ★ 워커를 ON 하기 전에 반드시 본 마이그를 선행 적용할 것. ★
--
-- 멱등성: 본 파일은 운영 적용 후 보관용(재실행 금지). 적용 전 상태 확인 후 수행.
-- 운영 적용: 사용자 수동(read-only MCP). 본 파일은 작성만, DB 직접 적용 금지.
-- ============================================================================

-- 1) tb_user_device.DEL_YN 컬럼 추가 (무효 토큰 soft-delete)
ALTER TABLE `tb_user_device`
    ADD COLUMN `DEL_YN` char(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'N'
        COMMENT '삭제 여부 N:정상 / Y:무효토큰 soft-delete' AFTER `UPDATE_DATE`;

-- 2) tb_noti_outbox.SEND_STATUS COMMENT 에 SENDING 코드값 추가 (타입/기본값 불변, COMMENT 만 갱신)
ALTER TABLE `tb_noti_outbox`
    MODIFY COLUMN `SEND_STATUS` varchar(10) NOT NULL DEFAULT 'PENDING'
        COMMENT '발송 상태 PENDING:대기 / SENDING:발송중(claim) / SENT:완료 / FAILED:실패';

-- ============================================================================
-- 롤백 (필요 시 수동 실행)
-- ----------------------------------------------------------------------------
-- ALTER TABLE `tb_user_device` DROP COLUMN `DEL_YN`;
-- ALTER TABLE `tb_noti_outbox`
--     MODIFY COLUMN `SEND_STATUS` varchar(10) NOT NULL DEFAULT 'PENDING'
--         COMMENT '발송 상태 PENDING:대기 / SENT:완료 / FAILED:실패';
-- ============================================================================
