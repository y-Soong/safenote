-- ============================================================================
-- PRAFTA-COM-008-B-0 — 노무수령거부 detect→block 전환 스키마/코드 확장
-- 작성일: 2026-06-12
-- 적용 환경: MySQL 8.0.42
-- 참조: .claude/requests/common/prafta-com-008-B-labor-refusal.md §4
--       refs/prafta-com-008/prafta-com-008-B-migration-design.md §6/§8
--       refs/prafta-com-008/prafta-com-008-B-decomposition.md PRAFTA-com-008-B-0
--       선행: prafta-com-001-leave-refusal-log.sql / prafta-com-001-sys-codes.sql (적용 전제)
--             prafta-com-008-c-leave-source-consent.sql (PROMOTION_STAGE 적용 전제)
--             prafta-com-008-a-0-promotion-master.sql   (SYS045 SORT_IDX 10/11 사용)
--
-- 변경 요약
--  1) tb_leave_refusal_log 컬럼 2종 추가(BLOCKED 이벤트 운반):
--     - ATTEMPT_TYPE     : 차단 시도 유형(CHECK_IN/CHECK_OUT/ATTD_CREATE/ADMIN_ENTRY), nullable
--     - RELATED_LEAVE_ID : 연계 연차사용 ID(tb_user_leave_use.LEAVE_ID), nullable
--     기존 행(NOTICED/CHECKIN_DETECTED/ADMIN_ALERTED)은 두 컬럼 NULL 로 호환 유지.
--  2) SYS064(이벤트 유형)에 'BLOCKED'(시도→노무수령거부 차단, SORT_IDX 4) 추가.
--     기존 NOTICED/CHECKIN_DETECTED 는 보존(Deprecated — 차단 전환 후 신규 생성 안 함).
--  3) SYS045(알림 유형)에 'LEAVE_REFUSAL_BLOCK_ALERT'(차단 시도 관리자 알림, SORT_IDX 20) 신설.
--     기존 LEAVE_REFUSAL_CHECKIN_ALERT(SORT_IDX 3)는 보존(Deprecated — 감지→차단으로 의미 이관).
--     ※ SORT_IDX 는 표시정렬용(PK 아님·인덱스 없음). 현재 SYS045 최대=19(prafta-app-021 푸시트리거 등 반영) → 다음 20 부여.
--
-- 적용 전 부재 확인(권장):
--   SHOW COLUMNS FROM tb_leave_refusal_log LIKE 'ATTEMPT_TYPE';
--   SELECT 1 FROM tb_syst_val_d WHERE SYST_VAL_CD='SYS064' AND SYST_VAL_D_CD='BLOCKED';
--   SELECT 1 FROM tb_syst_val_d WHERE SYST_VAL_CD='SYS045' AND SYST_VAL_D_CD='LEAVE_REFUSAL_BLOCK_ALERT';
--   SELECT MAX(SORT_IDX) FROM tb_syst_val_d WHERE SYST_VAL_CD='SYS045';
--
-- 멱등성: ALTER ADD COLUMN / INSERT 중복 실행 시 에러. 운영 1회 적용 후 보관용(재실행 금지).
--         이미 반영된 환경에서는 해당 구문을 건너뛸 것.
-- ★ 운영 선적용 필수: 미적용 상태로 BE 기동 시 BLOCKED INSERT/ATTEMPT_TYPE 참조 SQL 전면 실패.
-- ============================================================================

-- ── 1) tb_leave_refusal_log 컬럼 2종 추가 (BLOCKED 이벤트 운반) ──
ALTER TABLE `tb_leave_refusal_log`
      ADD COLUMN `ATTEMPT_TYPE`     varchar(20) DEFAULT NULL
          COMMENT '차단 시도 유형[SYS064 BLOCKED 부가] CHECK_IN:출근 / CHECK_OUT:퇴근 / ATTD_CREATE:근태생성·보정 / ADMIN_ENTRY:관리자등록'
          AFTER `EVENT_TYPE`
    , ADD COLUMN `RELATED_LEAVE_ID` varchar(20) DEFAULT NULL
          COMMENT '연계 연차사용 ID (tb_user_leave_use.LEAVE_ID, BLOCKED 시 차단 대상 연차)'
          AFTER `RELATED_ATTD_ID`;

-- ── 2) SYS064 디테일 추가 (BLOCKED) ──
--   기존 NOTICED(1)/CHECKIN_DETECTED(2)/ADMIN_ALERTED(3) 는 보존(데이터 무삭제).
--   detect→block 전환 후 신규 생성은 BLOCKED 만(NOTICED/CHECKIN_DETECTED 는 Deprecated).
INSERT INTO `tb_syst_val_d`
      (`SYST_VAL_CD`, `SYST_VAL_D_CD`, `SYST_VAL_D_NM`, `SORT_IDX`, `USE_YN`, `INSERT_NO`)
VALUES
      ('SYS064', 'BLOCKED', '노무수령거부 차단됨', 4, 'Y', 'SYSTEM');

-- ── 3) SYS045 디테일 추가 (LEAVE_REFUSAL_BLOCK_ALERT) ──
--   기존 LEAVE_REFUSAL_CHECKIN_ALERT(3)는 보존(Deprecated — 감지→차단 의미 이관).
--   VAL_D_INFO_1='PUSH'(채널) — com-001/촉진 시드와 동일 컬럼 사용.
INSERT INTO `tb_syst_val_d`
      (`SYST_VAL_CD`, `SYST_VAL_D_CD`, `SYST_VAL_D_NM`, `SORT_IDX`, `USE_YN`, `VAL_D_INFO_1`, `INSERT_NO`)
VALUES
      ('SYS045', 'LEAVE_REFUSAL_BLOCK_ALERT', '노무수령거부일 시도 차단(관리자)', 20, 'Y', 'PUSH', 'SYSTEM');
