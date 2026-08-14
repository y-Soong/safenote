-- =====================================================================================
-- prafta-schedmodify-1 : 스케줄수정요청 적용일 미검증으로 생성된 근무계획 오염행 보정
-- 작성일 : 2026-08-14
-- 대상   : 운영 RDS (prafta) — ★실행은 사용자가 Workbench 로 직접 수행한다(자동 실행 금지)
--
-- [배경]
--   앱 스케줄수정요청(REQ_TYPE='10') 경로에 근무타입 적용일(effective-dating) 검증이 없어,
--   근무일에 아직 유효하지 않은 근무타입이 발의·승인되어 TB_USER_WORK_PLAN 에 반영되었다.
--   재현: 적용일 2026-08-01 인 TH-A(SCH_CD='00014') 를 2026-07-21 근무일로 요청 → 승인까지 통과.
--   코드 측 재발방지는 동일 커밋의 3중 가드로 처리했다:
--     ① AppReq07Mapper.selectSchedOptions  — 근무일 기준 유효 타입만 노출(선택지에서 숨김)
--     ② AppReq07ServiceImpl.registerSchedModify — 발의 시 fail-closed 검증(ATTD_400_203)
--     ③ Attd07ServiceImpl.approveSchedModifyRequest — 승인 시 fail-closed 재검증(ATTD_400_203)
--
-- [오염 범위 — 2026-08-14 운영 전수 스캔 결과]
--   "근무일 < 근무타입 최초 적용일" 인 TB_USER_WORK_PLAN 행 = 2건 / 2명.
--   대기(REQ_STATUS='01') 상태로 남은 동일 결함 요청 = 0건(추가 반영 위험 없음).
--
-- [보정 방침 — 두 건의 처리가 다르다. 반드시 아래 근거를 확인하고 실행할 것]
--   A) 2026-07-21 / USER_CD='20260800041' / TH-A('00014')  → ★삭제
--      · 승인 마커 PROCESS_COMMENT='SCHED_MODIFY_APPROVED:OLD=' (OLD 빈값)
--        = 승인 전에 근무계획 행이 아예 없었고, 이 승인이 행을 새로 만들었다.
--      · 근태실적(TB_USER_ATTD_MGMT) 없음 / 초과근무 없음 / 연차사용 없음.
--      → 원래 없던 행이므로 삭제가 원상복구다.
--
--   B) 2026-06-01 / USER_CD='20260400013' / ST001_1('00011') → ★삭제 아님, 원복(ST001='00002')
--      · 승인 마커 PROCESS_COMMENT='SCHED_MODIFY_APPROVED:OLD=00002'
--        = 승인 전 ST001('00002')이 배정돼 있었고, 이 승인이 그것을 덮어썼다.
--      · 그날 실제 근태실적이 있다(2026-06-01 출근 0751 / 퇴근 1831).
--      · ST001('00002')은 2026-06-01 기준 유효(유효버전 APPLY_DATE='20260417', USE_YN='Y').
--      → 행을 삭제하면 실제 근무한 날의 근무계획이 사라져 소정/연장 계산과 마감이 훼손된다.
--        덮어쓰기 이전 값으로 되돌리는 것이 규칙에 맞는 상태다.
--      ※ 그래도 삭제를 원하면 STEP 3 대신 파일 하단 [대안] 블록을 사용할 것.
--
-- [요청 이력(TB_USER_ATTD_REQ)은 건드리지 않는다]
--   승인('02')된 요청은 "그때 실제로 일어난 일"이므로 감사 추적상 남겨둔다.
--   본 스크립트는 근무계획(TB_USER_WORK_PLAN) 상태만 규칙에 맞게 되돌린다.
--
-- [실행 전 필수]
--   1) RDS 콘솔에서 수동 스냅샷 생성(자동 백업 보존 1일).
--   2) Workbench 연결이 '🔴 PRAFTA [PROD]' 인지 창 제목으로 확인.
--   3) STEP 1 → STEP 2 → STEP 3 → STEP 4 순서대로, 각 결과를 눈으로 확인하며 실행.
-- =====================================================================================


-- -------------------------------------------------------------------------------------
-- STEP 1. 실행 전 스냅샷 — 결과를 캡처해 두면 롤백 근거가 된다(2행이어야 정상)
-- -------------------------------------------------------------------------------------
SELECT CMPNY_CD, SITE_CD, USER_CD, WORK_YMD, WORK_PLAN_CD, GEN_SOURCE
     , INSERT_NO, INSERT_DATE, UPDATE_NO, UPDATE_DATE
  FROM TB_USER_WORK_PLAN
 WHERE CMPNY_CD = '001'
   AND SITE_CD  = '00001'
   AND (   (USER_CD = '20260800041' AND WORK_YMD = '20260721')
        OR (USER_CD = '20260400013' AND WORK_YMD = '20260601'));


-- -------------------------------------------------------------------------------------
-- STEP 2. [A] 2026-07-21 TH-A — 삭제 (승인 전 행이 없던 건)
--         WORK_PLAN_CD 조건을 함께 걸어, 그 사이 값이 바뀐 경우 0행 영향으로 안전하게 실패시킨다.
--         ※ Workbench Safe Updates 로 막히면(PK 복합키) 세션에서만 SET SQL_SAFE_UPDATES=0; 후 실행하고
--           작업이 끝나면 반드시 1 로 되돌릴 것.
-- -------------------------------------------------------------------------------------
DELETE FROM TB_USER_WORK_PLAN
 WHERE CMPNY_CD     = '001'
   AND SITE_CD      = '00001'
   AND USER_CD      = '20260800041'
   AND WORK_YMD     = '20260721'
   AND WORK_PLAN_CD = '00014';
-- 기대 영향 행수: 1


-- -------------------------------------------------------------------------------------
-- STEP 3. [B] 2026-06-01 ST001_1 — 덮어쓰기 이전 값(ST001='00002')으로 원복
--         GEN_SOURCE 는 'MANUAL' 그대로 둔다(관리자 배정분이라는 사실은 변하지 않음).
-- -------------------------------------------------------------------------------------
UPDATE TB_USER_WORK_PLAN
   SET WORK_PLAN_CD = '00002'
     , UPDATE_NO    = 'SYSTEM'
     , UPDATE_DATE  = NOW()
 WHERE CMPNY_CD     = '001'
   AND SITE_CD      = '00001'
   AND USER_CD      = '20260400013'
   AND WORK_YMD     = '20260601'
   AND WORK_PLAN_CD = '00011';
-- 기대 영향 행수: 1


-- -------------------------------------------------------------------------------------
-- STEP 4. 검증 — 아래 쿼리가 0행이면 보정 완료
--         (근무일이 해당 근무타입의 최초 적용일보다 앞서는 근무계획 행이 전사에 없는지 재확인)
-- -------------------------------------------------------------------------------------
SELECT WP.CMPNY_CD, WP.SITE_CD, WP.USER_CD, WP.WORK_YMD, WP.WORK_PLAN_CD
     , M.SCH_NO      AS SCH_NO
     , V.MIN_APPLY   AS FIRST_APPLY_DATE
  FROM TB_USER_WORK_PLAN WP
  JOIN TB_SCH_MGMT M
    ON M.CMPNY_CD = WP.CMPNY_CD AND M.SITE_CD = WP.SITE_CD AND M.SCH_CD = WP.WORK_PLAN_CD
  JOIN (
        SELECT CMPNY_CD, SITE_CD, SCH_CD, MIN(APPLY_DATE) AS MIN_APPLY
          FROM (
                SELECT CMPNY_CD, SITE_CD, SCH_CD, APPLY_DATE FROM TB_SCH_MGMT
                UNION ALL
                SELECT CMPNY_CD, SITE_CD, SCH_CD, APPLY_DATE FROM TB_SCH_MGMT_HIST
               ) U
         GROUP BY CMPNY_CD, SITE_CD, SCH_CD
       ) V
    ON V.CMPNY_CD = WP.CMPNY_CD AND V.SITE_CD = WP.SITE_CD AND V.SCH_CD = WP.WORK_PLAN_CD
 WHERE WP.WORK_YMD < V.MIN_APPLY;
-- 기대 결과: 0행


-- =====================================================================================
-- [롤백] STEP 2/3 을 되돌려야 할 때 (STEP 1 스냅샷 값 기준)
-- =====================================================================================
-- INSERT INTO TB_USER_WORK_PLAN
--        (CMPNY_CD, SITE_CD, USER_CD, WORK_YMD, WORK_PLAN_CD, GEN_SOURCE, INSERT_NO, INSERT_DATE, UPDATE_NO, UPDATE_DATE)
-- VALUES ('001', '00001', '20260800041', '20260721', '00014', 'MANUAL', '20260400010', '2026-08-14 01:33:37', '20260400010', '2026-08-14 01:33:37');
--
-- UPDATE TB_USER_WORK_PLAN
--    SET WORK_PLAN_CD = '00011', UPDATE_NO = '20260400010', UPDATE_DATE = '2026-06-21 21:30:57'
--  WHERE CMPNY_CD='001' AND SITE_CD='00001' AND USER_CD='20260400013' AND WORK_YMD='20260601';


-- =====================================================================================
-- [대안] STEP 3 을 "원복" 이 아니라 "삭제" 로 처리하려는 경우에만 사용.
--        ★권장하지 않음 — 2026-06-01 은 실제 근태실적(출근 0751/퇴근 1831)이 있는 근무일이라,
--          근무계획 행을 지우면 그날 소정근로 기준이 사라져 근태 계상·마감이 훼손된다.
-- =====================================================================================
-- DELETE FROM TB_USER_WORK_PLAN
--  WHERE CMPNY_CD='001' AND SITE_CD='00001' AND USER_CD='20260400013'
--    AND WORK_YMD='20260601' AND WORK_PLAN_CD='00011';
