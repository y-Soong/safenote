-- ============================================================================
-- PRAFTA-SUBCON-T3-2 — 데이터 공유 공통코드 시드 (SYS077 / SYS078 신규)
-- 작성일: 2026-07-13
-- 적용 환경: MySQL 8.0.42
-- 출처: PRAFTA-SUBCON.md §2.4(SYS 채번), PRAFTA-SUBCON-T3.plan.md §3-2
-- 참조: prafta-subcon-t2-2-sys-codes.sql (시드 스타일 미러)
--
-- 채번 확정 (2026-07-13 메인 세션 MCP 실측 — 미사용 확인):
--   - SYS077 = 공유 데이터유형 (tb_cmpny_share_req.DATA_TYPE)
--   - SYS078 = 공유요청 상태   (tb_cmpny_share_req.STATUS)
--   - SYS079 = 사업장 연동 링크 상태 (T2 점유 — 본 파일 무관)
--
-- 변경 요약
--   1) SYS077 마스터 + 디테일 1종: ATTD(근태).
--      ★ RISK/NEARMISS 는 T7 이 추가한다. T3 가 미리 넣으면 요청 생성 화면 셀렉트에
--        미구현 유형이 노출된다(서버 화이트리스트도 ATTD 만 허용).
--   2) SYS078 마스터 + 디테일 4종: REQUESTED/APPROVED/REJECTED/CANCELLED.
--
-- 적용 전 부재 확인:
--   SELECT SYST_VAL_CD FROM tb_syst_val_m WHERE SYST_VAL_CD IN ('SYS077','SYS078');  -- 0건이어야 함
-- 멱등성: PK 중복 시 에러. 이미 존재하면 건너뛸 것(재실행 금지).
-- 운영 적용: 사용자 수동(read-only MCP). 본 파일은 작성만, DB 직접 적용 금지.
-- ============================================================================

-- ── 1) SYS077 마스터 (공유 데이터유형) ──
INSERT INTO `tb_syst_val_m` (`SYST_VAL_CD`, `SYST_VAL_NM`, `USE_YN`, `VAL_DESC`, `INSERT_NO`)
VALUES ('SYS077', '공유 데이터유형', 'Y', 'tb_cmpny_share_req.DATA_TYPE 코드', 'SYSTEM');

-- ── 2) SYS077 디테일 (T3 = 근태만) ──
INSERT INTO `tb_syst_val_d`
      (`SYST_VAL_CD`, `SYST_VAL_D_CD`, `SYST_VAL_D_NM`, `SORT_IDX`, `USE_YN`, `INSERT_NO`)
VALUES
      ('SYS077', 'ATTD', '근태', 1, 'Y', 'SYSTEM');

-- ── 3) SYS078 마스터 (공유요청 상태) ──
INSERT INTO `tb_syst_val_m` (`SYST_VAL_CD`, `SYST_VAL_NM`, `USE_YN`, `VAL_DESC`, `INSERT_NO`)
VALUES ('SYS078', '공유요청 상태', 'Y', 'tb_cmpny_share_req.STATUS 코드', 'SYSTEM');

-- ── 4) SYS078 디테일 (상태 4종) ──
INSERT INTO `tb_syst_val_d`
      (`SYST_VAL_CD`, `SYST_VAL_D_CD`, `SYST_VAL_D_NM`, `SORT_IDX`, `USE_YN`, `INSERT_NO`)
VALUES
      ('SYS078', 'REQUESTED', '요청중', 1, 'Y', 'SYSTEM')
    , ('SYS078', 'APPROVED',  '승인',   2, 'Y', 'SYSTEM')
    , ('SYS078', 'REJECTED',  '거부됨', 3, 'Y', 'SYSTEM')
    , ('SYS078', 'CANCELLED', '취소됨', 4, 'Y', 'SYSTEM');

-- ============================================================================
-- 롤백 (필요 시 수동 실행)
-- ----------------------------------------------------------------------------
-- DELETE FROM `tb_syst_val_d` WHERE SYST_VAL_CD IN ('SYS077', 'SYS078');
-- DELETE FROM `tb_syst_val_m` WHERE SYST_VAL_CD IN ('SYS077', 'SYS078');
-- ============================================================================
