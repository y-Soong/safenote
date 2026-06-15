-- ============================================================================
-- PRAFTA-APP-021-2 — SYS045 (알림 유형) 디테일 신규 10종 시드 (푸시 설정 신규 트리거)
-- 작성일: 2026-06-12
-- 적용 환경: MySQL 8.0.42
-- 참조: .claude/requests/app_requests/prafta-app-021-plan.md §4(신규 SYS045 표)/§3(토글 매핑),
--       공통 정책서 §10.1(채널=PUSH), prafta-com-008-c-leave-source-consent.sql §3-7,
--       prafta-com-004-sys045-noti-type.sql (SYS045 디테일 시드 스타일 미러)
--
-- 변경 요약
--  1) SYS045(알림 유형) 디테일 신규 10종 추가(전부 VAL_D_INFO_1='PUSH', USE_YN='Y').
--     토글 단위로는 6종(W2/W3/W4/W5/M1/M5)이나, W2 가 4 NOTI_TYPE 으로 전개되어 실수는 10종이다.
--     - W2 신청 처리 결과 4종: LEAVE_RESULT_APPROVED/REJECTED, ATTD_RESULT_APPROVED/REJECTED
--     - W3 TBM 교육 2종    : TBM_STARTED, TBM_COMPLETED
--     - W4/W5 리마인더 2종 : ATTD_CHECKIN_REMINDER, ATTD_CHECKOUT_REMINDER
--     - M1 지각/조퇴 1종   : ATTD_LATE_EARLY_DETECTED
--     - M5 위험성평가 1종  : RISK_ASSESS_REQUESTED
--  2) SYS045 마스터(tb_syst_val_m)는 prafta-031 에서 이미 등록됨 → 본 파일은 디테일만 INSERT.
--     (tb_noti_outbox.NOTI_TYPE / tb_user_push_setting.NOTI_TYPE 은 카탈로그 참조용 varchar 이며
--      FK 제약 없음 — 미적용해도 INSERT 자체는 실패하지 않으나, 코드값 일관성을 위해 트리거/스케줄러
--      코드 배포 전 선적용 권장.)
--
-- SORT_IDX: 기존 SYS045 디테일 최대값(com-008-c=9) 다음으로 10~19 부여(표시 정렬용, PK 아님).
--
-- 적용 전 부재 확인:
--   SELECT 1 FROM tb_syst_val_m WHERE SYST_VAL_CD = 'SYS045';                                       -- 마스터 존재(=INSERT 생략)
--   SELECT SYST_VAL_D_CD FROM tb_syst_val_d WHERE SYST_VAL_CD = 'SYS045' AND SYST_VAL_D_CD = 'LEAVE_RESULT_APPROVED';
-- 멱등성: PK(SYST_VAL_CD, SYST_VAL_D_CD) 중복 시 에러. 운영 적용 후 보관용(재실행 금지). 이미 존재 시 건너뛸 것.
-- ============================================================================

-- ── (선택) SYS045 마스터 부재 환경 한정. prafta-031 미적용 환경에서만 주석 해제 ──
-- INSERT INTO `tb_syst_val_m` (`SYST_VAL_CD`, `SYST_VAL_NM`, `USE_YN`, `VAL_DESC`, `INSERT_NO`)
-- VALUES ('SYS045', '알림 유형', 'Y', 'tb_noti_outbox.NOTI_TYPE 코드', 'SYSTEM');

-- ── SYS045 디테일 추가 (푸시 설정 신규 트리거 10종) ──
INSERT INTO `tb_syst_val_d`
      (`SYST_VAL_CD`, `SYST_VAL_D_CD`, `SYST_VAL_D_NM`, `SORT_IDX`, `USE_YN`, `VAL_D_INFO_1`, `INSERT_NO`)
VALUES
      ('SYS045', 'LEAVE_RESULT_APPROVED',    '연차 신청 승인 결과(신청자)',          10, 'Y', 'PUSH', 'SYSTEM')
    , ('SYS045', 'LEAVE_RESULT_REJECTED',    '연차 신청 반려 결과(신청자)',          11, 'Y', 'PUSH', 'SYSTEM')
    , ('SYS045', 'ATTD_RESULT_APPROVED',     '근태/초과근무 요청 승인 결과(신청자)', 12, 'Y', 'PUSH', 'SYSTEM')
    , ('SYS045', 'ATTD_RESULT_REJECTED',     '근태/초과근무 요청 반려 결과(신청자)', 13, 'Y', 'PUSH', 'SYSTEM')
    , ('SYS045', 'TBM_STARTED',              'TBM 교육 시작(참석자)',                14, 'Y', 'PUSH', 'SYSTEM')
    , ('SYS045', 'TBM_COMPLETED',            'TBM 교육 종료(참석자)',                15, 'Y', 'PUSH', 'SYSTEM')
    , ('SYS045', 'ATTD_CHECKIN_REMINDER',    '출근 5분 전 리마인더(근로자)',         16, 'Y', 'PUSH', 'SYSTEM')
    , ('SYS045', 'ATTD_CHECKOUT_REMINDER',   '퇴근 5분 전 리마인더(근로자)',         17, 'Y', 'PUSH', 'SYSTEM')
    , ('SYS045', 'ATTD_LATE_EARLY_DETECTED', '지각/조기퇴근 감지(노드 관리자)',      18, 'Y', 'PUSH', 'SYSTEM')
    , ('SYS045', 'RISK_ASSESS_REQUESTED',    '위험성평가 검토 요청(담당/관리자)',    19, 'Y', 'PUSH', 'SYSTEM');
