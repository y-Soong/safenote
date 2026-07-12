-- ============================================================================
-- prafta-058-4-sys011-005.sql
-- 위험성평가 진행상태(SYS011)에 '005' 지속개선대상 코드 시드 (DML)
--   T6-P2 / findings §E D5. 항목 14.3.3 신규 진행상태.
--
-- 현행 SYS011: 001 검토요청 / 002 개선예정 / 003 개선완료 / 004 미처리대상 ('005' 미사용)
--   ※ 과거 prafta-040(near-miss)에서 005 '아차사고로 이관'을 추가했으나
--     prafta-054-2-cleanup-data.sql 에서 삭제됨 → 현재 005 코드값은 비어 있음.
--   ※ SORT_IDX=6: 기존 004 가 SORT_IDX 5 (001/1, 002/3, 003/4, 004/5)이며,
--     prafta-near-miss-deploy 선례에서도 005 를 SORT_IDX 6 으로 부여함.
--
-- 적용 순서 (058 계열): P3 nearmiss 058-1~3 → (본) 058-4 → 058-5(테이블 DDL)
-- ============================================================================

-- 적용 전 부재 확인:
--   SELECT SYST_VAL_D_CD FROM tb_syst_val_d WHERE SYST_VAL_CD='SYS011' AND SYST_VAL_D_CD='005'; -- 0건 기대
INSERT INTO `tb_syst_val_d`
    (`SYST_VAL_CD`, `SYST_VAL_D_CD`, `SYST_VAL_D_NM`, `SORT_IDX`, `USE_YN`, `INSERT_NO`) VALUES
    ('SYS011', '005', '지속개선대상', 6, 'Y', 'SYSTEM');

-- 적용 후 검증:
--   SELECT SYST_VAL_D_CD, SYST_VAL_D_NM, SORT_IDX, USE_YN
--     FROM tb_syst_val_d WHERE SYST_VAL_CD='SYS011' ORDER BY SORT_IDX;
