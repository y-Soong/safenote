-- ============================================================================
-- PRAFTA-daily-contract-2 — 일용직 입장 승인제 시스템코드 시드
--                           (SYS081/SYS082 신설 + SYS045 디테일 1건)
-- 작성일: 2026-07-16
-- 적용 환경: MySQL 8.0.42
-- 출처: .claude/requests/common/작업지시서_일용직-계약서-서명-승인제.plan.md §3-4
-- 참조 스타일: prafta-subcon-t7-2-sys-codes.sql(SYS080), prafta-app-021-2-sys045-noti-type.sql
--
-- 변경 요약
--   1) tb_syst_val_m : SYS081(입장 승인요청 유형), SYS082(입장 승인요청 상태) 신설.
--   2) tb_syst_val_d : SYS081 디테일 2종 / SYS082 디테일 5종.
--   3) tb_syst_val_d : SYS045(알림 유형) 디테일 1건 — DAILY_ENTRY_REQ (사업장 관리자 푸시).
--
-- ★ SYS 채번 주의: 마이그레이션 기준 SYS080(prafta-subcon-t7)까지 선점됨.
--   .claude/context/schema-full.sql 은 stale 스냅샷(SYS043까지)이므로 채번 근거로 쓰지 말 것.
--   반드시 아래 "적용 전 부재 확인"을 운영 DB 실측으로 수행한 뒤 적용한다.
--
-- 적용 전 부재 확인 (운영 적용 직전 필수):
--   -- 1) SYS081/SYS082 부재 확인 — 0건이어야 함(있으면 채번 충돌 → 작업 중단 후 보고):
--   SELECT SYST_VAL_CD, SYST_VAL_NM FROM tb_syst_val_m WHERE SYST_VAL_CD IN ('SYS081','SYS082');
--   SELECT SYST_VAL_CD, SYST_VAL_D_CD FROM tb_syst_val_d WHERE SYST_VAL_CD IN ('SYS081','SYS082');
--   -- 2) 현재 최대 SYS 코드 실측(SYS080 이하인지 확인):
--   SELECT MAX(SYST_VAL_CD) FROM tb_syst_val_m WHERE SYST_VAL_CD LIKE 'SYS0__';
--   -- 3) SYS045 마스터 존재(prafta-031에서 등록됨 — 1건이어야 함) + 디테일 부재 확인:
--   SELECT 1 FROM tb_syst_val_m WHERE SYST_VAL_CD = 'SYS045';
--   SELECT SYST_VAL_D_CD FROM tb_syst_val_d WHERE SYST_VAL_CD = 'SYS045' AND SYST_VAL_D_CD = 'DAILY_ENTRY_REQ';  -- 0건
--   -- 4) SYS045 디테일 SORT_IDX 현황(아래 32와 충돌 시 다음 순번으로 조정 — 표시 정렬용, PK 아님):
--   SELECT MAX(SORT_IDX) FROM tb_syst_val_d WHERE SYST_VAL_CD = 'SYS045';   -- 마이그 기준 31(SHIFT_SCH_CHANGED)
--
-- 멱등성: PK(SYST_VAL_CD[, SYST_VAL_D_CD]) 중복 시 에러. 이미 존재 시 해당 구문 건너뛸 것.
-- 운영 적용: 사용자 수동(read-only MCP). 본 파일은 작성만, DB 직접 적용 금지.
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1) 마스터 등록 — SYS081 / SYS082
-- ----------------------------------------------------------------------------
INSERT INTO `tb_syst_val_m` (`SYST_VAL_CD`, `SYST_VAL_NM`, `USE_YN`, `VAL_DESC`, `INSERT_NO`) VALUES
    ('SYS081', '일용직 입장 승인요청 유형', 'Y', 'TB_DAILY_ENTRY_REQUEST.REQ_TYPE 코드', 'SYSTEM')
  , ('SYS082', '일용직 입장 승인요청 상태', 'Y', 'TB_DAILY_ENTRY_REQUEST.REQ_STATUS 코드', 'SYSTEM');

-- ----------------------------------------------------------------------------
-- 2) 디테일 등록 — SYS081(유형 2종) / SYS082(상태 5종)
-- ----------------------------------------------------------------------------
INSERT INTO `tb_syst_val_d` (`SYST_VAL_CD`, `SYST_VAL_D_CD`, `SYST_VAL_D_NM`, `SORT_IDX`, `USE_YN`, `INSERT_NO`) VALUES
    ('SYS081', '01', '신규가입', 1, 'Y', 'SYSTEM')
  , ('SYS081', '02', '재입장',   2, 'Y', 'SYSTEM');

INSERT INTO `tb_syst_val_d` (`SYST_VAL_CD`, `SYST_VAL_D_CD`, `SYST_VAL_D_NM`, `SORT_IDX`, `USE_YN`, `INSERT_NO`) VALUES
    ('SYS082', '01', '대기', 1, 'Y', 'SYSTEM')   -- 생성 직후, 관리자 처리 전
  , ('SYS082', '02', '승인', 2, 'Y', 'SYSTEM')   -- 관리자 승인, 로그인 소진 대기
  , ('SYS082', '03', '거부', 3, 'Y', 'SYSTEM')   -- 관리자 거부(사유 기록), 최종 상태
  , ('SYS082', '04', '만료', 4, 'Y', 'SYSTEM')   -- 대기/승인 상태로 당일 자정 경과 (D7)
  , ('SYS082', '05', '소진', 5, 'Y', 'SYSTEM');  -- 승인 후 실제 로그인 성공 (D6)

-- ----------------------------------------------------------------------------
-- 3) SYS045(알림 유형) 디테일 1건 — 승인요청 발생 시 사업장 관리자 푸시(tb_noti_outbox).
--    SORT_IDX=32: 마이그 기준 기존 최대 31(SHIFT_SCH_CHANGED) 다음. DB 실측 후 충돌 시 조정.
-- ----------------------------------------------------------------------------
INSERT INTO `tb_syst_val_d` (`SYST_VAL_CD`, `SYST_VAL_D_CD`, `SYST_VAL_D_NM`, `SORT_IDX`, `USE_YN`, `VAL_D_INFO_1`, `INSERT_NO`) VALUES
    ('SYS045', 'DAILY_ENTRY_REQ', '일용직 입장 승인요청(사업장 관리자)', 32, 'Y', 'PUSH', 'SYSTEM');

-- ============================================================================
-- 적용 후 검증 (운영 적용 후 1회 실행)
-- ----------------------------------------------------------------------------
-- 1) 마스터 2건:
--    SELECT SYST_VAL_CD, SYST_VAL_NM, USE_YN FROM tb_syst_val_m WHERE SYST_VAL_CD IN ('SYS081','SYS082');
-- 2) 디테일 SYS081=2건 / SYS082=5건:
--    SELECT SYST_VAL_CD, COUNT(*) FROM tb_syst_val_d WHERE SYST_VAL_CD IN ('SYS081','SYS082') GROUP BY SYST_VAL_CD;
-- 3) SYS045 디테일 1건:
--    SELECT SYST_VAL_D_CD, SYST_VAL_D_NM, VAL_D_INFO_1 FROM tb_syst_val_d
--     WHERE SYST_VAL_CD = 'SYS045' AND SYST_VAL_D_CD = 'DAILY_ENTRY_REQ';
-- ============================================================================
-- 롤백 (필요 시 수동 실행)
-- ----------------------------------------------------------------------------
-- DELETE FROM tb_syst_val_d WHERE SYST_VAL_CD IN ('SYS081','SYS082');
-- DELETE FROM tb_syst_val_m WHERE SYST_VAL_CD IN ('SYS081','SYS082');
-- DELETE FROM tb_syst_val_d WHERE SYST_VAL_CD = 'SYS045' AND SYST_VAL_D_CD = 'DAILY_ENTRY_REQ';
-- ============================================================================
