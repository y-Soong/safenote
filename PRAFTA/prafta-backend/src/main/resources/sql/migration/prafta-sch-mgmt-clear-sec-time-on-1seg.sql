-- 1구간(SCH_TYPE='01') 근무타입인데 2구간 시각이 잘못 저장된 행 정정.
-- 원인: 웹 SchInfoPop.vue fnSave 가 secSchStrTime/secSchEndTime 을 schType 가드 없이
--       항상 전송 → ref 기본값 "00:00"/"18:00" 이 '0000'/'1800' 으로 저장됨.
--       (앱 스케줄 수정요청 화면에서 1구간 타입이 2구간으로 오표시되던 결함의 데이터 잔재)
-- 정상 1구간 행의 컨벤션과 동일하게 빈 문자열로 비운다(정상행 예: ST001 = '').
-- 적용 전 영향행 확인:
--   SELECT SCH_CD, SCH_NO FROM TB_SCH_MGMT
--    WHERE SCH_TYPE = '01'
--      AND ((SEC_SCH_STR_TIME IS NOT NULL AND SEC_SCH_STR_TIME != '')
--        OR (SEC_SCH_END_TIME IS NOT NULL AND SEC_SCH_END_TIME != ''));

UPDATE TB_SCH_MGMT
   SET SEC_SCH_STR_TIME = ''
     , SEC_SCH_END_TIME = ''
     , SEC_SCH_BRK_MIN  = ''
     , SEC_BRK_STR_TIME = NULL
     , SEC_BRK_END_TIME = NULL
 WHERE SCH_TYPE = '01'
   AND ((SEC_SCH_STR_TIME IS NOT NULL AND SEC_SCH_STR_TIME != '')
     OR (SEC_SCH_END_TIME IS NOT NULL AND SEC_SCH_END_TIME != ''));
