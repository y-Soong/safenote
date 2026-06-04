-- ============================================================================
-- PRAFTA-APP-009-1 — SYS045 (알림 유형) 근태 결재 PUSH 디테일 2종 시드
-- 작성일: 2026-06-04
-- 적용 환경: MySQL 8.0.42
-- 참조: .claude/requests/app_requests/prafta-app-009-plan.md §1.3(D9), §2.2(009-1)
--       공통 정책서 §10.1(채널=PUSH) / §10.3(중복 방지 — NOTI_TYPE 카탈로그)
--       prafta-com-004-sys045-noti-type.sql (연차 결재 PUSH 시드 스타일 미러)
--
-- 변경 요약
--  1) SYS045(알림 유형)에 근태 결재 PUSH 알림 2종 추가:
--     - ATTD_APPROVAL_TURN    : 근태 결재 차례 도래(결재자 대상)         — 'N' 결재라인 차례도래
--     - ATTD_APPROVAL_REQUEST : 자체근태승인('Y') 승인 요망(노드 관리자 대상) — 'Y' 단일승인
--     두 건 모두 VAL_D_INFO_1='PUSH', USE_YN='Y'.
--  2) SYS045 마스터(tb_syst_val_m)는 prafta-031 에서 이미 등록됨 → 본 파일은 디테일만 INSERT.
--     (tb_noti_outbox.NOTI_TYPE 은 카탈로그 참조용 varchar 이며 FK 제약 없음 — 누락 시 INSERT
--      자체는 실패하지 않으나, 코드값 카탈로그 일관성을 위해 코드 배포 전 본 시드 선적용 권장.)
--
-- ⚠️ prafta-app-020 공유: 본 마이그가 두 알림 유형의 단일 출처다. app-020 은 본 시드를
--    재사용하며 중복 INSERT 하지 않는다.
--
-- SORT_IDX: 기존 SYS045 디테일 최대값(prafta-031=1, near-miss=2, com-001=2/3, com-004=4/5)
--           다음으로 6, 7 부여. (SORT_IDX 는 PK 가 아니므로 표시 정렬용.)
--
-- 적용 전 부재 확인:
--   SELECT 1 FROM tb_syst_val_m WHERE SYST_VAL_CD='SYS045';                                       -- 마스터 존재(=INSERT 생략)
--   SELECT COUNT(*) FROM tb_syst_val_d WHERE SYST_VAL_CD='SYS045'
--          AND SYST_VAL_D_CD IN ('ATTD_APPROVAL_TURN', 'ATTD_APPROVAL_REQUEST');                  -- 0 이어야 함
-- 멱등성: PK(SYST_VAL_CD, SYST_VAL_D_CD) 중복 시 에러. 운영 적용 후 보관용(재실행 금지).
--
-- ─────────────────────────────────────────────────────────────────────────
-- F16 운영 적용 순서 (엄수): ① 본 마이그 SQL → ② 백엔드 배포 → ③ Flutter APK 재빌드
--   - 운영 DB 직접 실행 금지(사용자 수동, read-only MCP). 본 파일은 작성만.
-- ─────────────────────────────────────────────────────────────────────────
-- ============================================================================

-- ── (선택) SYS045 마스터 부재 환경 한정. prafta-031 미적용 환경에서만 주석 해제 ──
-- INSERT INTO `tb_syst_val_m` (`SYST_VAL_CD`, `SYST_VAL_NM`, `USE_YN`, `VAL_DESC`, `INSERT_NO`)
-- VALUES ('SYS045', '알림 유형', 'Y', 'tb_noti_outbox.NOTI_TYPE 코드', 'SYSTEM');

-- ── SYS045 디테일 추가 (근태 결재 PUSH 알림 2종) ──
INSERT INTO `tb_syst_val_d`
      (`SYST_VAL_CD`, `SYST_VAL_D_CD`, `SYST_VAL_D_NM`, `SORT_IDX`, `USE_YN`, `VAL_D_INFO_1`, `INSERT_NO`)
VALUES
      ('SYS045', 'ATTD_APPROVAL_TURN',    '근태 결재 차례 도래(결재자)',        6, 'Y', 'PUSH', 'SYSTEM')
    , ('SYS045', 'ATTD_APPROVAL_REQUEST', '자체근태승인 승인 요망(노드 관리자)', 7, 'Y', 'PUSH', 'SYSTEM');
