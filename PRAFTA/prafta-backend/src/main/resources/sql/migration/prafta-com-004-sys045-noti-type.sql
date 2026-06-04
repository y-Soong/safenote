-- ============================================================================
-- PRAFTA-COM-004 — SYS045 (알림 유형) 디테일 2종 시드 (연차 결재 PUSH)
-- 작성일: 2026-06-03
-- 적용 환경: MySQL 8.0.42
-- 참조: .claude/requests/common/prafta-com-004-plan.md §4(COM-004-1),
--       공통 정책서 §10.1(채널=PUSH) / §10.3(중복 방지 — NOTI_TYPE 카탈로그),
--       prafta-031-sys045-noti-type.sql / prafta-com-001-sys-codes.sql (SYS045 시드 스타일 미러)
--
-- 변경 요약
--  1) SYS045(알림 유형)에 연차 결재 PUSH 알림 2종 추가:
--     - LEAVE_APPROVAL_TURN  : 연차 결재 차례 도래(결재자 대상)   — 시나리오 A
--     - LEAVE_USED_NO_APRV   : 무결재 연차 사용 통보(노드 관리자 대상) — 시나리오 B
--     두 건 모두 VAL_D_INFO_1='PUSH', USE_YN='Y'.
--  2) SYS045 마스터(tb_syst_val_m)는 prafta-031 에서 이미 등록됨 → 본 파일은 디테일만 INSERT.
--     (tb_noti_outbox.NOTI_TYPE 은 카탈로그 참조용 varchar 이며 FK 제약 없음 — 누락 시 INSERT
--      자체는 실패하지 않으나, 코드값 카탈로그 일관성을 위해 코드 배포 전 본 시드 선적용 권장.)
--
-- SORT_IDX: 기존 SYS045 디테일 최대값(prafta-031=1, near-miss=2, com-001=2/3) 다음으로 4, 5 부여.
--           (SORT_IDX 는 PK 가 아니므로 표시 정렬용. 중복돼도 무방하나 충돌 회피로 4/5 사용.)
--
-- 적용 전 부재 확인:
--   SELECT 1 FROM tb_syst_val_m WHERE SYST_VAL_CD='SYS045';                                   -- 마스터 존재(=INSERT 생략)
--   SELECT SYST_VAL_D_CD FROM tb_syst_val_d WHERE SYST_VAL_CD='SYS045' AND SYST_VAL_D_CD='LEAVE_APPROVAL_TURN';
--   SELECT SYST_VAL_D_CD FROM tb_syst_val_d WHERE SYST_VAL_CD='SYS045' AND SYST_VAL_D_CD='LEAVE_USED_NO_APRV';
-- 멱등성: PK(SYST_VAL_CD, SYST_VAL_D_CD) 중복 시 에러. 운영 적용 후 보관용(재실행 금지). 이미 존재 시 건너뛸 것.
-- ============================================================================

-- ── (선택) SYS045 마스터 부재 환경 한정. prafta-031 미적용 환경에서만 주석 해제 ──
-- INSERT INTO `tb_syst_val_m` (`SYST_VAL_CD`, `SYST_VAL_NM`, `USE_YN`, `VAL_DESC`, `INSERT_NO`)
-- VALUES ('SYS045', '알림 유형', 'Y', 'tb_noti_outbox.NOTI_TYPE 코드', 'SYSTEM');

-- ── SYS045 디테일 추가 (연차 결재 PUSH 알림 2종) ──
INSERT INTO `tb_syst_val_d`
      (`SYST_VAL_CD`, `SYST_VAL_D_CD`, `SYST_VAL_D_NM`, `SORT_IDX`, `USE_YN`, `VAL_D_INFO_1`, `INSERT_NO`)
VALUES
      ('SYS045', 'LEAVE_APPROVAL_TURN', '연차 결재 차례 도래(결재자)',   4, 'Y', 'PUSH', 'SYSTEM')
    , ('SYS045', 'LEAVE_USED_NO_APRV',  '무결재 연차 사용 통보(노드 관리자)', 5, 'Y', 'PUSH', 'SYSTEM');
