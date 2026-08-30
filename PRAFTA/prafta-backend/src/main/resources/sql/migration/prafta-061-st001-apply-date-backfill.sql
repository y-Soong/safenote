-- ============================================================================
-- prafta-061 R1: 기존 회사 기본 근무타입(ST001) 적용일(APPLY_DATE) 소급 조정 템플릿
-- ============================================================================
-- ★실행 방식: 이 파일은 "회사별 수동 실행 템플릿"이다. 일괄 UPDATE 금지.
--   1) §A 현황 조회로 대상 회사의 현재본/이력본 APPLY_DATE 를 먼저 확인한다.
--   2) HIST 유무에 따라 §B(HIST 없음) 또는 §C(HIST 있음)의 템플릿을 복사해,
--      플레이스홀더 3개를 실제 값으로 치환한 뒤 1개 회사(사업장)씩 실행한다.
--        {CMPNY_CD}          — 대상 회사코드
--        {SITE_CD}           — 대상 사업장코드
--        {TARGET_APPLY_DATE} — 소급할 적용일 YYYYMMDD (예: '20250101')
--      ※ MySQL 사용자변수(SET @x)를 컬럼과 비교하면 collation 오류(1267)가 실발생한
--        이력이 있어(메모리 feedback_mysql_user_variable_collation_1267) 사용자변수
--        대신 리터럴 치환 방식을 쓴다.
--   3) 실행 후 §D 사후 검증으로 UNION MIN(APPLY_DATE)을 재확인한다.
--
-- ★실행 주체: 사용자가 Workbench 로 직접 실행(CLAUDE.md 원칙 — Claude 는 어떤 DB에도
--   실행하지 않는다). 개발·운영 DB 동시 적용 원칙(feedback_db_migration_apply_both_envs).
--   Workbench safe updates 모드에서 복합 PK 테이블 UPDATE 가 1175 로 막히면 세션에서
--   SET SQL_SAFE_UPDATES = 0; 후 실행하고 끝나면 되돌린다.
--
-- ★★소급(effective-dating) 주의사항 4종 — 실행 전 반드시 확인 (planner §4 확정):
--   ① 적용일을 과거로 당기면, 당겨진 과거 구간 전체에 "그 버전"의 근무/휴게 시각이
--      effective-dating 으로 소급 적용된다. 그 기간의 실제 근무 시각이 지금 버전과
--      달랐다면 과거 근태(지각/조퇴/연장 판정)의 해석이 바뀐다 — 시각 이력을 먼저 확인.
--   ② 갱신 후에도 버전 간 APPLY_DATE 오름차순 정합이 유지되어야 한다. 소급 대상
--      버전의 새 APPLY_DATE 가 다른 버전(현재본/이력본)의 APPLY_DATE 보다 뒤로
--      넘어가는 값이면 실행 금지(버전 순서가 꼬인다).
--   ③ 동일 APPLY_DATE 가 여러 버전에 중복되면 판정 시 HIST_IDX 최댓값(현재본 우선)
--      버전이 채택된다 — 소급으로 다른 버전과 APPLY_DATE 가 같아지지 않게 한다.
--   ④ 마감된 기간에는 근무 유형 수정을 소급 적용하지 않는다(근태관리 정책서
--      attd/03-work-types.md §3.3 — 이력 보존 원칙 포함). 마감월 이전으로 당길 때는
--      노무 검토 후 실행한다.
--
-- 배경: 프로비저닝이 ST001 의 APPLY_DATE 를 회사 생성일로 고정 시드해 왔다.
--   BEFORE_CREATE 판정(웹 Attd_05 / 앱 ATTD_400_203)은 "현재본+이력본 UNION 의
--   MIN(APPLY_DATE)" 이전 날짜의 스케줄 지정을 차단하므로, 과거 입사자의 재직 기간에
--   스케줄을 지정하려면 가장 이른 버전의 APPLY_DATE 를 소급해야 한다.
-- ============================================================================


-- ============================================================================
-- §A. 현황 조회 — 회사·사업장별 ST001 현재본/이력본 APPLY_DATE (읽기 전용)
-- ============================================================================
-- SRC='CURRENT' 행만 있으면 §B(단순 케이스), SRC='HIST' 행이 있으면 §C 로 간다.
SELECT T.CMPNY_CD
     , T.SITE_CD
     , T.SCH_CD
     , T.SRC
     , T.HIST_IDX
     , T.APPLY_DATE
     , T.USE_YN
     , T.FST_SCH_STR_TIME
     , T.FST_SCH_END_TIME
     , T.FST_SCH_BRK_MIN
  FROM (
        SELECT M.CMPNY_CD
             , M.SITE_CD
             , M.SCH_CD
             , 'CURRENT' AS SRC
             , NULL      AS HIST_IDX
             , M.APPLY_DATE
             , M.USE_YN
             , M.FST_SCH_STR_TIME
             , M.FST_SCH_END_TIME
             , M.FST_SCH_BRK_MIN
          FROM tb_sch_mgmt M
         WHERE M.SCH_CD = 'ST001'
        UNION ALL
        SELECT H.CMPNY_CD
             , H.SITE_CD
             , H.SCH_CD
             , 'HIST'    AS SRC
             , H.HIST_IDX
             , H.APPLY_DATE
             , H.USE_YN
             , H.FST_SCH_STR_TIME
             , H.FST_SCH_END_TIME
             , H.FST_SCH_BRK_MIN
          FROM tb_sch_mgmt_hist H
         WHERE H.SCH_CD = 'ST001'
       ) T
 ORDER BY T.CMPNY_CD, T.SITE_CD, T.APPLY_DATE, T.HIST_IDX
 LIMIT 500;


-- ============================================================================
-- §B. 단순 케이스 — HIST 이력이 없는 회사(현재본 1행뿐)
-- ============================================================================
-- 조건: §A 에서 해당 회사·사업장에 SRC='HIST' 행이 없을 것(아래 NOT EXISTS 가 2차 방어).
-- APPLY_DATE > {TARGET_APPLY_DATE} 술어 — 이미 target 이하로 소급돼 있으면 무동작(멱등).
/*
UPDATE tb_sch_mgmt
   SET APPLY_DATE  = '{TARGET_APPLY_DATE}'
     , UPDATE_NO   = 'SYSTEM'
     , UPDATE_DATE = NOW()
 WHERE CMPNY_CD = '{CMPNY_CD}'
   AND SITE_CD  = '{SITE_CD}'
   AND SCH_CD   = 'ST001'
   AND APPLY_DATE > '{TARGET_APPLY_DATE}'
   AND NOT EXISTS (
        SELECT 1
          FROM tb_sch_mgmt_hist H
         WHERE H.CMPNY_CD = '{CMPNY_CD}'
           AND H.SITE_CD  = '{SITE_CD}'
           AND H.SCH_CD   = 'ST001'
       );
*/


-- ============================================================================
-- §C. HIST 이력이 있는 회사 — "가장 이른 버전" 1행만 소급
-- ============================================================================
-- BEFORE_CREATE 판정 기준은 현재본+이력본 UNION 의 MIN(APPLY_DATE) 이므로,
-- 소급 대상은 가장 이른 버전(최소 APPLY_DATE, 동률이면 최소 HIST_IDX) 1행이다.
-- §A 결과에서 가장 이른 버전이 어느 쪽인지 확인 후 C-1/C-2 중 하나만 실행한다.
-- ★실행 전 상단 주의사항 ①~④ 전부 확인. 특히 ②: {TARGET_APPLY_DATE} 는 반드시
--   그 회사·사업장의 기존 MIN(APPLY_DATE) 보다 이른 값이어야 한다(순서 역전 금지).

-- C-1) 가장 이른 버전이 "이력본(tb_sch_mgmt_hist)" 인 경우 — HIST_IDX 를 특정해 1행만.
/*
UPDATE tb_sch_mgmt_hist
   SET APPLY_DATE = '{TARGET_APPLY_DATE}'
 WHERE CMPNY_CD = '{CMPNY_CD}'
   AND SITE_CD  = '{SITE_CD}'
   AND SCH_CD   = 'ST001'
   AND HIST_IDX = {EARLIEST_HIST_IDX}   -- §A 에서 확인한 최소 APPLY_DATE(동률 시 최소 HIST_IDX) 행
   AND APPLY_DATE > '{TARGET_APPLY_DATE}';
*/

-- C-2) 가장 이른 버전이 "현재본(tb_sch_mgmt)" 인 경우(이력본이 모두 그보다 늦음).
/*
UPDATE tb_sch_mgmt
   SET APPLY_DATE  = '{TARGET_APPLY_DATE}'
     , UPDATE_NO   = 'SYSTEM'
     , UPDATE_DATE = NOW()
 WHERE CMPNY_CD = '{CMPNY_CD}'
   AND SITE_CD  = '{SITE_CD}'
   AND SCH_CD   = 'ST001'
   AND APPLY_DATE > '{TARGET_APPLY_DATE}';
*/


-- ============================================================================
-- §D. 사후 검증 — UNION MIN(APPLY_DATE) 재확인 (읽기 전용)
-- ============================================================================
-- 실행한 회사·사업장의 EARLIEST_APPLY_DATE 가 {TARGET_APPLY_DATE} 로 내려갔는지 확인.
SELECT T.CMPNY_CD
     , T.SITE_CD
     , MIN(T.APPLY_DATE) AS EARLIEST_APPLY_DATE
     , COUNT(*)          AS VERSION_CNT
  FROM (
        SELECT M.CMPNY_CD, M.SITE_CD, M.APPLY_DATE
          FROM tb_sch_mgmt M
         WHERE M.SCH_CD = 'ST001'
        UNION ALL
        SELECT H.CMPNY_CD, H.SITE_CD, H.APPLY_DATE
          FROM tb_sch_mgmt_hist H
         WHERE H.SCH_CD = 'ST001'
       ) T
 GROUP BY T.CMPNY_CD, T.SITE_CD
 ORDER BY T.CMPNY_CD, T.SITE_CD
 LIMIT 500;
