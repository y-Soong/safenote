-- ============================================================================
-- PRAFTA-031 — SYS045 (알림 유형) 공통코드 시드
-- 작성일: 2026-05-26
-- 적용 환경: MySQL 8.0.42
-- 참조: .claude/requests/prafta-031.md, 공통 정책서 §10(알림/공지)
--
-- 변경 요약
--  1) SYS045 마스터 1건 + 디테일 1건 시드.
--     - tb_noti_outbox.NOTI_TYPE 코드 카탈로그.
--     - LEAVE_GRANT_RECALLED = 관리자 수동 부여 연차 회수 알림(추후 모바일 push).
--     - VAL_D_INFO_1 = 기본 발송 채널(PUSH) 참조용.
--
-- 적용 전 부재 확인: SELECT 1 FROM tb_syst_val_m WHERE SYST_VAL_CD='SYS045';
-- 멱등성: PK 중복 시 에러. 운영 적용 후 보관용(재실행 금지). 이미 존재 시 건너뛸 것.
-- ============================================================================

INSERT INTO `tb_syst_val_m` (`SYST_VAL_CD`, `SYST_VAL_NM`, `USE_YN`, `VAL_DESC`, `INSERT_NO`)
VALUES ('SYS045', '알림 유형', 'Y', 'tb_noti_outbox.NOTI_TYPE 코드', 'SYSTEM');

INSERT INTO `tb_syst_val_d`
  (`SYST_VAL_CD`, `SYST_VAL_D_CD`, `SYST_VAL_D_NM`, `SORT_IDX`, `USE_YN`, `VAL_D_INFO_1`, `INSERT_NO`)
VALUES
  ('SYS045', 'LEAVE_GRANT_RECALLED', '부여 연차 회수', 1, 'Y', 'PUSH', 'SYSTEM');
