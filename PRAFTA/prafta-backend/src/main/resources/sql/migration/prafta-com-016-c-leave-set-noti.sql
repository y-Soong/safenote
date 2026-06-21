-- ============================================================================
-- PRAFTA-COM-016-C-2 — SYS045(알림 유형) 디테일 1종 시드
--   : 관리자 연차/월차 직접 등록 통보 PUSH (근로자 대상)
-- 작성일: 2026-06-18
-- 적용 환경: MySQL 8.0.42
-- ★ 미적용(파일만 생성). 운영 반영은 코드 배포 전 수동 선적용 권장.
--
-- 참조: .claude/requests/common/prafta-com-016-C.md §C-2(나),
--       공통 정책서 §10.1(채널=PUSH) / §10.3(중복 방지 — NOTI_TYPE 카탈로그),
--       prafta-com-004-sys045-noti-type.sql (SYS045 시드 스타일 미러)
--
-- 변경 요약
--  1) SYS045(알림 유형)에 PUSH 알림 1종 추가:
--     - LEAVE_DIRECT_SET : 관리자가 근무계획관리에서 근로자 셀에 연차/월차를 직접 등록(직접 차감)했을 때
--                          그 근로자 본인에게 통보. 여러 날 등록도 묶어 1건. VAL_D_INFO_1='PUSH', USE_YN='Y'.
--  2) SYS045 마스터(tb_syst_val_m)는 prafta-031 에서 이미 등록됨 → 본 파일은 디테일만 INSERT.
--     (tb_noti_outbox.NOTI_TYPE 은 카탈로그 참조용 varchar 이며 FK 제약 없음 — 누락 시 INSERT
--      자체는 실패하지 않으나, 코드값 카탈로그 일관성을 위해 코드 배포 전 본 시드 선적용 권장.)
--
-- 앱 푸시설정 매핑: app-021 토글 W1(연차) 하위에 매핑(PushNotiTypeConst.java).
--   기본 ON(opt-out). 사용자가 W1 토글을 끄면 회수·직접등록 통보가 함께 꺼진다.
--
-- SORT_IDX: 표시 정렬용(PK 아님). 기존 SYS045 디테일과 충돌 회피로 30 부여(여유 값).
--
-- 적용 전 부재 확인:
--   SELECT 1 FROM tb_syst_val_m WHERE SYST_VAL_CD='SYS045';                                   -- 마스터 존재(=INSERT 생략)
--   SELECT SYST_VAL_D_CD FROM tb_syst_val_d WHERE SYST_VAL_CD='SYS045' AND SYST_VAL_D_CD='LEAVE_DIRECT_SET';
-- 멱등성: PK(SYST_VAL_CD, SYST_VAL_D_CD) 중복 시 에러. 운영 적용 후 보관용(재실행 금지). 이미 존재 시 건너뛸 것.
-- ============================================================================

-- ── SYS045 디테일 추가 (관리자 연차/월차 직접 등록 통보) ──
INSERT INTO `tb_syst_val_d`
      (`SYST_VAL_CD`, `SYST_VAL_D_CD`, `SYST_VAL_D_NM`, `SORT_IDX`, `USE_YN`, `VAL_D_INFO_1`, `INSERT_NO`)
VALUES
      ('SYS045', 'LEAVE_DIRECT_SET', '관리자 연차/월차 직접 등록 통보(근로자)', 30, 'Y', 'PUSH', 'SYSTEM');
