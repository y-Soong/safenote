-- ============================================================================
-- PRAFTA-058-1 — 아차사고 경미사고 행 삭제 + 처리상태 재매핑 (step-down DML)
-- 적용 환경: MySQL 8.0.42 / 출처: T6-findings D3·D4·F, T6-P3 §마이그
-- 적용 순서: 058-1(본 파일) → 058-2(코드표) → 058-3(DROP). 본 파일 검증 후 다음 단계.
-- 멱등성: 1회성 정리(재실행 시 (1)/(2) 대상 0건이면 무영향). 운영 적용 후 보관용.
-- ⚠️ Workbench safe update mode(1175) 회피 위해 세션 한정 해제 후 복구.
-- ============================================================================
SET SQL_SAFE_UPDATES = 0;

-- (1) 경미사고(INCIDENT_TYPE_CD='200') 행 삭제
--     방어적: 삭제 대상 near-miss 를 참조하는 연결행(tb_risk_near_miss_link) 먼저 정리.
--     (findings F: 실제 미참조이나 다른 환경/향후 데이터 대비 멱등 정리)
DELETE L
  FROM `tb_risk_near_miss_link` L
  JOIN `tb_near_miss` N
    ON  L.CMPNY_CD = N.CMPNY_CD
    AND L.SITE_CD  = N.SITE_CD
    AND L.NEAR_MISS_ID = N.NEAR_MISS_ID
 WHERE N.INCIDENT_TYPE_CD = '200';

DELETE FROM `tb_near_miss`
 WHERE `INCIDENT_TYPE_CD` = '200';

-- (2) 처리상태 재매핑 (단일 CASE UPDATE on 잔존 행)
--     100→100, 200(검토중)→200(조치중), 300(조치중)→200, 400(완료)→300, 900(반려)→400(미처리대상)
--     순서 안전: 원본값 기준 CASE 단일 평가라 연쇄 오염 없음.
UPDATE `tb_near_miss`
   SET `REPORT_STATUS_CD` = CASE `REPORT_STATUS_CD`
            WHEN '100' THEN '100'
            WHEN '200' THEN '200'
            WHEN '300' THEN '200'
            WHEN '400' THEN '300'
            WHEN '900' THEN '400'
            ELSE `REPORT_STATUS_CD`
        END
     , `UPDATE_NO`   = 'SYSTEM'
     , `UPDATE_DATE` = NOW()
 WHERE `REPORT_STATUS_CD` IN ('100','200','300','400','900');

SET SQL_SAFE_UPDATES = 1;
-- 검증:
--   SELECT COUNT(*) FROM tb_near_miss WHERE INCIDENT_TYPE_CD='200';       -- 0
--   SELECT REPORT_STATUS_CD, COUNT(*) FROM tb_near_miss GROUP BY 1;       -- {100,200,300,400}만
