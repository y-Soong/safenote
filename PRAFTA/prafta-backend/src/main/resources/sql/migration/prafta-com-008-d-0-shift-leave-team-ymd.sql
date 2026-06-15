-- ============================================================================
-- PRAFTA-COM-008-D-0 — 교대팀 사용자별 탈퇴일(LEAVE_TEAM_YMD) 컬럼 추가 (마이그)
-- 작성일: 2026-06-12
-- 적용 환경: MySQL 8.0.42
-- 참조: .claude/requests/common/refs/prafta-com-008/prafta-com-008-D-decomposition.md §D-0 / §확정 D-1
--       .claude/requests/common/prafta-com-008-D-shift-lock.md §2-1(멤버십 종료 추적) / §확정 D-1/D-2
--       prafta-com-008-e-1-default-worktype-columns.sql (ALTER 스타일 미러)
--
-- 변경 요약
--  1) tb_shift_sch_team_user 확장 — 사용자별 교대팀 탈퇴일(YYYYMMDD) 추적 컬럼 추가.
--     행 보존 + 탈퇴일 마킹 모델(별도 이력 테이블 없음). NULL = 현 소속/무기한.
--     잠금 구간 = workYmd ∈ [team.STR_DATE, team.END_DATE] AND (LEAVE_TEAM_YMD IS NULL OR workYmd < LEAVE_TEAM_YMD).
--     즉 탈퇴일 당일부터 근무계획 변경 허용(잠금 구간 상한 exclusive). 가입 시작은 팀 마스터 STR_DATE.
--  2) SYS 코드 불요(자유 일자값 varchar8 — 코드성 아님). 차단 사유는 AttdErrorCode(ATTD_400_160, DDL 아님).
--  3) 기존 행 백필 불필요(NULL = 현 소속자 = 정상 의미). 데이터 무삭제.
--
-- 멱등성: ALTER 중복 실행 시 에러. 이미 반영된 환경에서는 건너뛸 것.
-- 적용 순서: BE 재기동 전 선적용 필수(미적용 시 D-1/D-2/D-3 LEAVE_TEAM_YMD 참조 SQL 전면 실패).
-- ============================================================================

-- ── tb_shift_sch_team_user 확장 (사용자별 교대팀 탈퇴일) ──
--   LEAVE_TEAM_YMD : 교대팀 탈퇴일(YYYYMMDD, NULL=현 소속/무기한).
--                    이 날짜부터 근무계획 변경 허용(잠금 구간 exclusive 상한 = 탈퇴일 미포함).
ALTER TABLE `tb_shift_sch_team_user`
      ADD COLUMN `LEAVE_TEAM_YMD` varchar(8) DEFAULT NULL
          COMMENT '교대팀 탈퇴일(YYYYMMDD, NULL=현 소속/무기한). 이 날짜부터 근무계획 변경 허용(잠금 구간 exclusive 상한)' AFTER `LEADER_YN`;
