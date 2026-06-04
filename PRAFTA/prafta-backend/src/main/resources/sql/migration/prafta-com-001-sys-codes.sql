-- ============================================================================
-- PRAFTA-COM-001 — 노무수령거부 관련 공통코드 시드 (SYS045 추가 + SYS064 신규)
-- 작성일: 2026-06-02
-- 적용 환경: MySQL 8.0.42
-- 참조: .claude/requests/common/refs/prafta-com-001/01_작업지시서_노무수령거부_PUSH.md §B-1
--       prafta-031-sys045-noti-type.sql (SYS045 시드 스타일 미러)
--
-- 변경 요약
--  1) SYS045(알림 유형)에 노무수령거부 알림 2종 추가:
--     - LEAVE_REFUSAL_NOTICE        : 노무수령거부 통지(근로자 대상)
--     - LEAVE_REFUSAL_CHECKIN_ALERT : 노무수령거부일 출근 감지(관리자 대상)
--     (SYS045 마스터는 prafta-031 에서 이미 등록됨 — 본 파일은 디테일만 추가.
--      혹시 마스터 부재 환경이면 아래 주석의 마스터 INSERT 를 먼저 1회 수행.)
--  2) SYS064(노무수령거부 이벤트 유형) 신규 그룹 + 디테일 3종:
--     - NOTICED          : 통지 발송됨
--     - CHECKIN_DETECTED : 대상일 출근 감지됨
--     - ADMIN_ALERTED    : 관리자 알림 발송됨
--     (tb_leave_refusal_log.EVENT_TYPE 코드 카탈로그)
--
-- 적용 전 부재 확인:
--   SELECT 1 FROM tb_syst_val_m WHERE SYST_VAL_CD='SYS064';
--   SELECT 1 FROM tb_syst_val_d WHERE SYST_VAL_CD='SYS045' AND SYST_VAL_D_CD='LEAVE_REFUSAL_NOTICE';
-- 멱등성: PK 중복 시 에러. 운영 적용 후 보관용(재실행 금지). 이미 존재 시 건너뛸 것.
-- ============================================================================

-- ── (선택) SYS045 마스터 부재 환경 한정. prafta-031 미적용 환경에서만 주석 해제 ──
-- INSERT INTO `tb_syst_val_m` (`SYST_VAL_CD`, `SYST_VAL_NM`, `USE_YN`, `VAL_DESC`, `INSERT_NO`)
-- VALUES ('SYS045', '알림 유형', 'Y', 'tb_noti_outbox.NOTI_TYPE 코드', 'SYSTEM');

-- ── 1) SYS045 디테일 추가 (알림 유형 2종) ──
INSERT INTO `tb_syst_val_d`
      (`SYST_VAL_CD`, `SYST_VAL_D_CD`, `SYST_VAL_D_NM`, `SORT_IDX`, `USE_YN`, `VAL_D_INFO_1`, `INSERT_NO`)
VALUES
      ('SYS045', 'LEAVE_REFUSAL_NOTICE',        '노무수령거부 통지(근로자)',   2, 'Y', 'PUSH', 'SYSTEM')
    , ('SYS045', 'LEAVE_REFUSAL_CHECKIN_ALERT', '노무수령거부일 출근감지(관리자)', 3, 'Y', 'PUSH', 'SYSTEM');

-- ── 2) SYS064 마스터 (노무수령거부 이벤트 유형) ──
INSERT INTO `tb_syst_val_m` (`SYST_VAL_CD`, `SYST_VAL_NM`, `USE_YN`, `VAL_DESC`, `INSERT_NO`)
VALUES ('SYS064', '노무수령거부 이벤트 유형', 'Y', 'tb_leave_refusal_log.EVENT_TYPE 코드', 'SYSTEM');

-- ── 3) SYS064 디테일 (이벤트 유형 3종) ──
INSERT INTO `tb_syst_val_d`
      (`SYST_VAL_CD`, `SYST_VAL_D_CD`, `SYST_VAL_D_NM`, `SORT_IDX`, `USE_YN`, `INSERT_NO`)
VALUES
      ('SYS064', 'NOTICED',          '통지 발송됨',       1, 'Y', 'SYSTEM')
    , ('SYS064', 'CHECKIN_DETECTED', '대상일 출근 감지됨', 2, 'Y', 'SYSTEM')
    , ('SYS064', 'ADMIN_ALERTED',    '관리자 알림 발송됨', 3, 'Y', 'SYSTEM');
