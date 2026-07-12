-- ============================================================================
-- PRAFTA-058-2 — SYS061(아차사고 사건유형) 폐기 + SYS063 재번호 (코드표 DML)
-- 적용 환경: MySQL 8.0.42 / 출처: T6-findings D3·D4, T6-P3 §마이그
-- 적용 순서: 058-1 → 058-2(본 파일) → 058-3.
-- 멱등성: DELETE/UPDATE 기반(재실행 시 대상 0건 무영향). 단 SYS063 SORT_IDX 는 최종값 고정.
--
-- ★★ SYS061 코드그룹 충돌 주의 (developer 발견, 메인 세션/사용자 확인 요망) ★★
--    prafta-near-miss-deploy.sql 은 SYS061 을 '아차사고 사건유형'(상세 100/200)으로 시드한다.
--    그런데 prafta-037-F6-user-upload-job.sql 도 동일 코드그룹 'SYS061' 을
--    '사용자 업로드 잡 상태'(상세 PENDING/RUNNING/SUCCESS/PARTIAL/FAILED)로 사용한다(코드 UploadJobStatus.java).
--    → 두 도메인이 같은 SYST_VAL_CD='SYS061' 을 공유하는 코드값 충돌 상태.
--    본 마이그는 아차사고 사건유형 상세('100','200')만 정밀 삭제하고,
--    마스터(tb_syst_val_m.SYS061)는 다른 상세(업로드 잡 상태)가 남아 있으면 보존한다
--    (남은 상세가 0건일 때만 마스터 삭제 → 업로드 잡 기능 무회귀).
--    실DB(findings F, 2026-06-23)는 SYS061 에 near-miss 100/200 만 확인됨.
-- ============================================================================
SET SQL_SAFE_UPDATES = 0;

-- (A) SYS061 '아차사고 사건유형' 상세코드(100/200)만 폐기 (업로드 잡 상태 상세는 비대상)
DELETE FROM `tb_syst_val_d`
 WHERE `SYST_VAL_CD` = 'SYS061'
   AND `SYST_VAL_D_CD` IN ('100','200');

-- (A') SYS061 마스터는 잔존 상세가 0건일 때만 삭제(업로드 잡 상태 상세가 있으면 보존).
DELETE M
  FROM `tb_syst_val_m` M
 WHERE M.`SYST_VAL_CD` = 'SYS061'
   AND NOT EXISTS (
        SELECT 1 FROM `tb_syst_val_d` D
         WHERE D.`SYST_VAL_CD` = 'SYS061'
   );

-- (B) SYS063 재정의: 기존 200(검토중)/300(조치중)/400(완료)/900(반려) 삭제
--     → 200 조치중 / 300 완료 / 400 미처리대상 (100 접수 유지). SORT_IDX 1~4 연속.
DELETE FROM `tb_syst_val_d`
 WHERE `SYST_VAL_CD` = 'SYS063'
   AND `SYST_VAL_D_CD` IN ('200','300','400','900');

INSERT INTO `tb_syst_val_d`
    (`SYST_VAL_CD`, `SYST_VAL_D_CD`, `SYST_VAL_D_NM`, `SORT_IDX`, `USE_YN`, `INSERT_NO`) VALUES
    ('SYS063', '200', '조치중',     2, 'Y', 'SYSTEM')
  , ('SYS063', '300', '완료',       3, 'Y', 'SYSTEM')
  , ('SYS063', '400', '미처리대상', 4, 'Y', 'SYSTEM');

-- (C) 100 접수 SORT_IDX 정합(이미 1이면 무영향)
UPDATE `tb_syst_val_d`
   SET `SORT_IDX` = 1, `SYST_VAL_D_NM` = '접수', `USE_YN` = 'Y'
 WHERE `SYST_VAL_CD` = 'SYS063' AND `SYST_VAL_D_CD` = '100';

SET SQL_SAFE_UPDATES = 1;
-- 검증:
--   SELECT SYST_VAL_D_CD, SYST_VAL_D_NM, SORT_IDX FROM tb_syst_val_d
--    WHERE SYST_VAL_CD='SYS063' ORDER BY SORT_IDX;  -- 100접수1/200조치중2/300완료3/400미처리대상4
--   SELECT SYST_VAL_D_CD FROM tb_syst_val_d WHERE SYST_VAL_CD='SYS061';  -- near-miss 100/200 제거됨
