-- ============================================================================
-- 앱 셀프가입 승인 화면 A3 — SYS045(알림 유형) 디테일 1종 시드
-- 작성일: 2026-08-16
-- 적용 환경: MySQL 8.0.42
-- 참조: .claude/requests/app_requests/작업지시서_통합테스트-결함_앱-셀프가입-승인화면.plan.md §0.9 / A3,
--       공통 정책서 §10.1(채널=PUSH) / §10.2(요청 등록 → 승인권자 알림),
--       prafta-com-016-d-shift-noti.sql (SYS045 시드 스타일 미러)
--
-- 변경 요약
--  1) SYS045(알림 유형)에 PUSH 알림 1종 추가:
--     'SELFJOIN_PENDING' — 셀프가입 승인 대기(부서 정/부 관리자 대상)
--     · 생산자: common/cmm/push/impl/SelfJoinPendingNotiServiceImpl (셀프가입 접수 afterCommit)
--     · 토글  : M6_SELFJOIN_PENDING (app/notiset/notiset01/application/PushNotiTypeConst)
--     · dedup : SELFJOIN_PEND_{targetUserCd}_{yyyyMMdd}_{siteCd} (수신자 1명당 하루 1건)
--  2) SYS045 마스터(tb_syst_val_m)는 prafta-031 에서 이미 등록됨 → 본 파일은 디테일만 INSERT.
--     (tb_noti_outbox.NOTI_TYPE / tb_user_push_setting.NOTI_TYPE 은 카탈로그 참조용 varchar 이며
--      FK 제약 없음 — 미적용해도 INSERT 자체는 실패하지 않으나, 알림 설정 화면의 코드 일관성을 위해
--      트리거 코드 배포 전 선적용 권장.)
--
-- ★적용 범위: 개발 DB 와 운영 DB 양쪽에 모두 적용해야 한다.
--   한쪽만 적용하면 이후 코드 배포 때 환경별 동작 차이로 장애가 반복된다
--   (feedback_db_migration_apply_both_envs).
--   ★본 파일은 작성만 되어 있고 실행되지 않았다. 실행은 사용자가 Workbench 로 직접 수행한다.
--
-- SORT_IDX: 표시 정렬용(PK 아님). 개발 DB 실측 최대값은 33 이다(2026-08-16 조회).
--   31=SHIFT_SCH_CHANGED, 32=DAILY_ENTRY_REQ, 33=LEAVE_PROMOTION_REMIND 가 이미 점유 →
--   본 시드는 34 를 부여한다.
--   ※운영 DB 는 값이 다를 수 있으므로 적용 전 아래 쿼리로 실제 최대값을 재확인하고 충돌 시 조정할 것.
--     SELECT MAX(SORT_IDX) FROM tb_syst_val_d WHERE SYST_VAL_CD = 'SYS045';
--
-- 적용 전 부재 확인:
--   SELECT 1 FROM tb_syst_val_m WHERE SYST_VAL_CD = 'SYS045';                                          -- 마스터 존재(=INSERT 생략)
--   SELECT SYST_VAL_D_CD FROM tb_syst_val_d WHERE SYST_VAL_CD='SYS045' AND SYST_VAL_D_CD='SELFJOIN_PENDING';
-- 멱등성: PK(SYST_VAL_CD, SYST_VAL_D_CD) 중복 시 에러. 운영 적용 후 보관용(재실행 금지). 이미 존재 시 건너뛸 것.
-- ============================================================================

-- ── SYS045 디테일 추가 (셀프가입 승인 대기 통보) ──
INSERT INTO `tb_syst_val_d`
      (`SYST_VAL_CD`, `SYST_VAL_D_CD`, `SYST_VAL_D_NM`, `SORT_IDX`, `USE_YN`, `VAL_D_INFO_1`, `INSERT_NO`)
VALUES
      ('SYS045', 'SELFJOIN_PENDING', '셀프가입 승인 대기(부서 관리자)', 34, 'Y', 'PUSH', 'SYSTEM');
