-- ============================================================================
-- PRAFTA-COM-008-C2 — 연차 이동 확장: 대상일 위치(반차 파트·시간차 시각) 선택 컬럼 추가
-- 작성일: 2026-08-18
-- 적용 환경: MySQL 8
-- 참조: .claude/requests/app_requests/작업지시서_연차이동_위치선택_확장.md §4-0
--       .claude/requests/app_requests/작업지시서_연차이동_위치선택_확장.plan.md (연차이동확장-01)
--       prafta-com-008-c-leave-source-consent.sql (tb_leave_change_request 원 DDL)
--
-- 변경 요약
--  1) tb_leave_change_request 에 nullable 컬럼 2개 추가 (기존 행 = NULL = 종전 의미, 기본값 없음 — 무회귀).
--     - MOVE_TARGET_HALF_PART  : 이동 대상 반차 파트(반차 건 MOVE 만 사용. NULL 이면 원 파트 유지)
--     - MOVE_TARGET_START_TIME : 이동 대상 시간차 시작 시각(시간차 건 MOVE 만 사용. NULL 이면 원 시각 유지.
--                                종료 시각은 원 분량(LEAVE_MINUTES)으로 서버가 파생 — 분량 변조 원천 차단)
--     컬럼 위치는 MOVE_TARGET_DATE 뒤(AFTER 명시) — 의미 인접. STORED 생성컬럼(ACTIVE_LEAVE_KEY) 뒤에
--     일반 컬럼을 추가할 수 없으므로 AFTER 명시가 필수다.
--
-- 멱등성: 멱등 아님 — 재실행 시 1060(Duplicate column name). 적용 여부 확인 후 실행할 것
--         (확인: DESCRIBE tb_leave_change_request → 2컬럼 존재 여부).
-- 적용 순서: ★BE 재기동 전 선적용 필수 (개발·운영 동시 적용 — 07-26 사용자 지시).
--            미적용 상태로 신규 코드가 뜨면 insertChangeRequest 가 전면 1054(Unknown column)
--            → 발의(관리자/근로자 이동·취소) 전부 실패 (선례: sojeong-2-1 §적용 순서 함정).
--            롤백 시: 코드만 롤백하면 신규 컬럼은 NULL 로 무해 잔존 — DDL 롤백 불필요.
-- ============================================================================

ALTER TABLE `tb_leave_change_request`
      ADD COLUMN `MOVE_TARGET_HALF_PART` varchar(8) NULL DEFAULT NULL
          COMMENT '이동 대상 반차 파트 (START:시작기준-늦게출근 / END:종료기준-일찍퇴근 / NULL:원 파트 유지)'
          AFTER `MOVE_TARGET_DATE`
    , ADD COLUMN `MOVE_TARGET_START_TIME` varchar(4) NULL DEFAULT NULL
          COMMENT '이동 대상 시간차 시작 시각 (HHMM / NULL:원 시각 유지. 종료는 원 분량으로 서버 파생)'
          AFTER `MOVE_TARGET_HALF_PART`;
