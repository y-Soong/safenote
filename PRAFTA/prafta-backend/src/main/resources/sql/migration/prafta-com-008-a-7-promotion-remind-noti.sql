-- ============================================================================
-- PRAFTA-COM-008-A-7 — 연차 사용촉진 1차 독촉(재안내) PUSH 알림유형 시드
-- 작성일: 2026-07-30
-- 적용 환경: MySQL 8.0.42
-- 참조: .claude/requests/web_requests/작업지시서_연차촉진-1차현황-화면-및-배치활성화.md §5-2(T2), 확정 D4
--       .claude/requests/web_requests/작업지시서_연차촉진-1차현황-화면-및-배치활성화.plan.md T2-3
--       prafta-com-008-a-0-promotion-master.sql §3 (시드 스타일 미러 — SYS045 디테일)
--
-- 변경 요약
--  1) SYS045 디테일 1행 추가 — LEAVE_PROMOTION_REMIND(1차 계획 제출 독촉/재안내, 근로자 PUSH).
--     관리자가 웹 [연차 사용촉진 > 1차 현황] 탭에서 미제출자에게 재발송하는 알림의 유형 코드다.
--     SYS045 마스터(tb_syst_val_m)는 prafta-031 에서 이미 등록됨 → 디테일만 INSERT.
--     SORT_IDX=33 은 실측값(현재 SYS045 최대 SORT_IDX=32). 표시정렬용이며 PK 아님.
--     VAL_D_INFO_1='PUSH' (채널) — 기존 촉진 시드(A-0 §3)와 동일 컬럼 사용.
--
--  ※ 본 작업의 DB 변경은 이 시드 1행이 전부다(DDL 0건 — 확정 D1 덕분에 스냅샷 컬럼 불필요).
--
-- 코드값 동일성(중요): 아래 SYST_VAL_D_CD 는 백엔드 3곳과 문자열이 완전히 같아야 한다.
--   1) LeavePromotionNotiConst.NOTI_TYPE_PROMOTION_REMIND
--   2) WebLeavePromo01ServiceImpl 의 독촉 outbox 적재 NOTI_TYPE
--   3) WebLeavePromo01Mapper.xml selectFirstTargets 의 독촉 집계 NOTI_TYPE 리터럴
--   하나만 어긋나면 독촉 이력 집계가 조용히 0 이 된다.
--
-- 멱등성: 중복 실행 시 에러(UNIQUE 충돌). 이미 반영된 환경에서는 건너뛸 것.
-- 적용: 사용자(운영자)가 직접 적용한다(MCP read-only). 개발·운영 양쪽 모두 적용 필수
--       (메모리 feedback_db_migration_apply_both_envs — 한쪽만 적용 시 환경 불일치 장애).
--       미적용 상태로 독촉 기능을 사용하면 코드값 미등록 알림이 outbox 에 쌓인다.
-- ============================================================================

-- ── 1) SYS045 디테일 추가 (1차 독촉 PUSH) ──
INSERT INTO `tb_syst_val_d`
      (`SYST_VAL_CD`, `SYST_VAL_D_CD`, `SYST_VAL_D_NM`, `SORT_IDX`, `USE_YN`, `VAL_D_INFO_1`, `INSERT_NO`)
VALUES
      ('SYS045', 'LEAVE_PROMOTION_REMIND', '연차 사용촉진 1차 독촉(근로자)', 33, 'Y', 'PUSH', 'SYSTEM');

-- ── 검증(적용 후 개발·운영 각각 실행) ──
-- SELECT SYST_VAL_D_CD, SYST_VAL_D_NM, SORT_IDX, USE_YN, VAL_D_INFO_1
--   FROM tb_syst_val_d
--  WHERE SYST_VAL_CD   = 'SYS045'
--    AND SYST_VAL_D_CD = 'LEAVE_PROMOTION_REMIND';
