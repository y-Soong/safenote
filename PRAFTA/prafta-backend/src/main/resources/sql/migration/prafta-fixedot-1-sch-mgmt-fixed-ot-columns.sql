-- ============================================================================
-- PRAFTA-FIXEDOT-1 — 근무타입 고정연장근무 도입 (스키마 1단계: 전방·후방 고정연장 4컬럼)
-- 작성일: 2026-08-11
-- 적용 환경: MySQL 8.0.42 이상 (개발·운영 동시 적용 — feedback_db_migration_apply_both_envs)
-- 참조: .claude/requests/web_requests/작업지시서_근무타입-고정연장근무-도입.md §확정 정책 ④·⑤
--       .claude/requests/web_requests/작업지시서_근무타입-고정연장근무-도입.plan.md §1-1
--
-- 변경 요약
--  1) tb_sch_mgmt 확장 — 전방(출근 전)·후방(퇴근 후) 고정연장근무 FROM/TO 2쌍(HHMM).
--     소정 구간(FST/SEC)과 분리된 별도 축. 명칭은 "고정연장근무"(법정 야간 22~06시 축과 무관).
--  2) tb_sch_mgmt_hist 동일 4컬럼 확장(이력 스냅샷 정합).
--
-- 규약
--  - varchar(4) HHMM, NULL 허용(NULL=고정연장 없음 — 기존 근무타입 전부 NULL 로 현행 유지).
--  - 쌍 완결성(V1): 전방·후방 각각 STR/END 둘 다 입력 또는 둘 다 NULL (서버 검증 ATTD_400_198).
--  - 후방은 자정 넘김 허용: 종료<=시작이면 +1440 해석(기존 스케줄 오버나이트 규약 준용).
--  - 전방은 당일 내만(전일 걸침 미지원 — plan §1-2 V2).
--  - 위치 anchor: plan 원문의 AFTER SEC_SCH_BRK_MIN 은 prafta-019-E 에서 추가된
--    SEC_BRK_STR_TIME/SEC_BRK_END_TIME 사이에 끼어들게 되므로, 2구간 그룹 말미
--    (SEC_BRK_END_TIME) 뒤로 보정한다.
--
-- 멱등성: ALTER 중복 실행 시 에러(Duplicate column). 이미 반영된 환경에서는 건너뛸 것.
-- 적용 순서: BE 재기동 전 선적용 필수(미적용 시 Attd_01 근무타입 조회/저장 및
--            하도급 미러 전파가 신규 컬럼 참조로 전면 실패).
-- ============================================================================

-- ── 1) tb_sch_mgmt 확장 (전방·후방 고정연장근무) ──
ALTER TABLE `tb_sch_mgmt`
      ADD COLUMN `PRE_FIXED_OT_STR_TIME` varchar(4) COLLATE utf8mb4_unicode_ci DEFAULT NULL
          COMMENT '전방 고정연장근무 시작시간(HHMM, 소정 1구간 시작 이전·당일 내)' AFTER `SEC_BRK_END_TIME`
    , ADD COLUMN `PRE_FIXED_OT_END_TIME` varchar(4) COLLATE utf8mb4_unicode_ci DEFAULT NULL
          COMMENT '전방 고정연장근무 종료시간(HHMM, 소정 1구간 시작 이하)' AFTER `PRE_FIXED_OT_STR_TIME`
    , ADD COLUMN `FIXED_OT_STR_TIME` varchar(4) COLLATE utf8mb4_unicode_ci DEFAULT NULL
          COMMENT '후방 고정연장근무 시작시간(HHMM, 소정 마지막 구간 종료 이상)' AFTER `PRE_FIXED_OT_END_TIME`
    , ADD COLUMN `FIXED_OT_END_TIME` varchar(4) COLLATE utf8mb4_unicode_ci DEFAULT NULL
          COMMENT '후방 고정연장근무 종료시간(HHMM, 종료<=시작이면 자정 넘김 +1440 해석)' AFTER `FIXED_OT_STR_TIME`;

-- ── 2) tb_sch_mgmt_hist 동일 확장 (이력 스냅샷) ──
ALTER TABLE `tb_sch_mgmt_hist`
      ADD COLUMN `PRE_FIXED_OT_STR_TIME` varchar(4) COLLATE utf8mb4_unicode_ci DEFAULT NULL
          COMMENT '전방 고정연장근무 시작시간(HHMM)' AFTER `SEC_BRK_END_TIME`
    , ADD COLUMN `PRE_FIXED_OT_END_TIME` varchar(4) COLLATE utf8mb4_unicode_ci DEFAULT NULL
          COMMENT '전방 고정연장근무 종료시간(HHMM)' AFTER `PRE_FIXED_OT_STR_TIME`
    , ADD COLUMN `FIXED_OT_STR_TIME` varchar(4) COLLATE utf8mb4_unicode_ci DEFAULT NULL
          COMMENT '후방 고정연장근무 시작시간(HHMM)' AFTER `PRE_FIXED_OT_END_TIME`
    , ADD COLUMN `FIXED_OT_END_TIME` varchar(4) COLLATE utf8mb4_unicode_ci DEFAULT NULL
          COMMENT '후방 고정연장근무 종료시간(HHMM, 종료<=시작이면 자정 넘김 +1440 해석)' AFTER `FIXED_OT_STR_TIME`;
