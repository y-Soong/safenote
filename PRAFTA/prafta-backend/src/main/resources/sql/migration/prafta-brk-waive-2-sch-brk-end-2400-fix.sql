-- ============================================================================
-- prafta-brk-waive-2-sch-brk-end-2400-fix.sql
-- 근무타입 휴게 종료 시각 '2400' 클램프 보정 (기존 결함 — 요청서 §3 G-1)
--   원인: 웹 SchInfoPop.vue addMinutesToHHmm 이 (휴게 시작 + 휴게분) 을 24:00 상한으로 클램프해 저장
--         (23:30 + 60분 → '2400', 시각 폭 30 ≠ 휴게분 60). BE DateTimeUtils.hhmmToMinutes 가 '2400' 을
--         거부(null)해 그 휴게가 반차 경계 산식·시간차 가로지름 판정에서 사라졌다.
--         운영 4개 타입(2026-09-03 prafta-mysql-prod 집계), 개발 1개 타입(R 야간 휴게2330-2400).
--   보정값: (휴게 시작 + 휴게분) mod 1440 을 HHMM 으로. 23:30+60 → '0030', 23:00+60 → '0000'.
--           BW-02 의 휴게 종료 파서(DateTimeUtils.brkEndToMinutes + wrap)가 "종료 < 시작" 을 익일로 해석한다.
--           '0000'~'0000'(0폭) 행은 대상이 아니며 그대로 둔다(휴게 없음 의미 보존).
--   요청서: .claude/requests/app_requests/작업지시서_부분휴가-휴게무시.md §3 G-1
--   plan  : .claude/requests/app_requests/작업지시서_부분휴가-휴게무시.plan.md (BW-01 §3-2)
-- 작성일: 2026-09-04
-- 적용 환경: MySQL 8
--
-- ★ 개발 DB · 운영 DB 양쪽에 "동시" 적용한다(2026-07-26 사용자 지시). 실행은 사용자가 Workbench 로 수행.
-- ★ 적용 창: BW-02 배포와 같은 창. 파서 배포 전에 먼저 적용해도 악화는 없다 — 지금은 '2400' 이 거부되어
--    휴게 시각이 무시되고, 보정값 '0030' 도 현행 파서에서는 (0030 < 2330 이라) 무시되어 결과가 같다.
--    파서 배포 후에는 양쪽 다 인식되므로 '2400'(폭 30 오판) 상태로 두면 경계가 어긋난다 → 반드시 보정.
-- ★ 멱등: 있음(대상 행이 없으면 0행 갱신).
-- ★ Workbench: 복합 PK 테이블 UPDATE 라 safe updates 모드에서 1175 가 난다 → 세션에서
--      SET SQL_SAFE_UPDATES = 0;
--    후 실행(메모리 feedback_mysql_workbench_safe_updates_composite_pk). 사용자 변수로 문자열 컬럼 비교 금지(1267).
-- ★ 이력 테이블(tb_sch_mgmt_hist)은 "과거 시점 값" 이므로 같은 규칙으로 값만 보정하고 새 이력 행은 INSERT
--    하지 않는다(HIST_IDX 채번은 화면 저장 경로 전용). tb_sch_mgmt_hist 에는 UPDATE_NO/UPDATE_DATE 컬럼이 없다.
-- ============================================================================

-- ----------------------------------------------------------------------------
-- [0] 사전 조회 — 대상 확인(운영 기대 4행 / 개발 기대 1행). 결과를 보관해 두고 [4] 와 대조한다.
-- ----------------------------------------------------------------------------
SELECT CMPNY_CD
     , SITE_CD
     , SCH_CD
     , FST_SCH_STR_TIME
     , FST_SCH_END_TIME
     , FST_SCH_BRK_MIN
     , FST_BRK_STR_TIME
     , FST_BRK_END_TIME
     , SEC_SCH_STR_TIME
     , SEC_SCH_END_TIME
     , SEC_SCH_BRK_MIN
     , SEC_BRK_STR_TIME
     , SEC_BRK_END_TIME
  FROM tb_sch_mgmt
 WHERE FST_BRK_END_TIME = '2400'
    OR SEC_BRK_END_TIME = '2400'
 LIMIT 50;

SELECT COUNT(*) AS hist_target_cnt
  FROM tb_sch_mgmt_hist
 WHERE FST_BRK_END_TIME = '2400'
    OR SEC_BRK_END_TIME = '2400';

-- (0-1) 보정 예정값 미리보기 — 1구간. 기대: new_end = 시작+분 mod 1440 (예 2330+60 → 0030)
SELECT CMPNY_CD
     , SITE_CD
     , SCH_CD
     , FST_BRK_STR_TIME
     , FST_SCH_BRK_MIN
     , FST_BRK_END_TIME AS old_end
     , CONCAT(
           LPAD(((CAST(SUBSTR(FST_BRK_STR_TIME, 1, 2) AS SIGNED) * 60
                 + CAST(SUBSTR(FST_BRK_STR_TIME, 3, 2) AS SIGNED)
                 + CAST(FST_SCH_BRK_MIN AS SIGNED)) MOD 1440) DIV 60, 2, '0')
         , LPAD(((CAST(SUBSTR(FST_BRK_STR_TIME, 1, 2) AS SIGNED) * 60
                 + CAST(SUBSTR(FST_BRK_STR_TIME, 3, 2) AS SIGNED)
                 + CAST(FST_SCH_BRK_MIN AS SIGNED)) MOD 1440) MOD 60, 2, '0')) AS new_end
  FROM tb_sch_mgmt
 WHERE FST_BRK_END_TIME = '2400'
   AND FST_BRK_STR_TIME REGEXP '^[0-9]{4}$'
   AND FST_SCH_BRK_MIN  REGEXP '^[0-9]+$'
 LIMIT 50;

-- ----------------------------------------------------------------------------
-- [1] 현행 테이블 보정 — 1구간
-- ----------------------------------------------------------------------------
UPDATE tb_sch_mgmt
   SET FST_BRK_END_TIME = CONCAT(
           LPAD(((CAST(SUBSTR(FST_BRK_STR_TIME, 1, 2) AS SIGNED) * 60
                 + CAST(SUBSTR(FST_BRK_STR_TIME, 3, 2) AS SIGNED)
                 + CAST(FST_SCH_BRK_MIN AS SIGNED)) MOD 1440) DIV 60, 2, '0')
         , LPAD(((CAST(SUBSTR(FST_BRK_STR_TIME, 1, 2) AS SIGNED) * 60
                 + CAST(SUBSTR(FST_BRK_STR_TIME, 3, 2) AS SIGNED)
                 + CAST(FST_SCH_BRK_MIN AS SIGNED)) MOD 1440) MOD 60, 2, '0'))
     , UPDATE_NO   = 'BRK_WAIVE_2_FIX'
     , UPDATE_DATE = NOW()
 WHERE FST_BRK_END_TIME = '2400'
   AND FST_BRK_STR_TIME REGEXP '^[0-9]{4}$'
   AND FST_SCH_BRK_MIN  REGEXP '^[0-9]+$';

-- ----------------------------------------------------------------------------
-- [2] 현행 테이블 보정 — 2구간
-- ----------------------------------------------------------------------------
UPDATE tb_sch_mgmt
   SET SEC_BRK_END_TIME = CONCAT(
           LPAD(((CAST(SUBSTR(SEC_BRK_STR_TIME, 1, 2) AS SIGNED) * 60
                 + CAST(SUBSTR(SEC_BRK_STR_TIME, 3, 2) AS SIGNED)
                 + CAST(SEC_SCH_BRK_MIN AS SIGNED)) MOD 1440) DIV 60, 2, '0')
         , LPAD(((CAST(SUBSTR(SEC_BRK_STR_TIME, 1, 2) AS SIGNED) * 60
                 + CAST(SUBSTR(SEC_BRK_STR_TIME, 3, 2) AS SIGNED)
                 + CAST(SEC_SCH_BRK_MIN AS SIGNED)) MOD 1440) MOD 60, 2, '0'))
     , UPDATE_NO   = 'BRK_WAIVE_2_FIX'
     , UPDATE_DATE = NOW()
 WHERE SEC_BRK_END_TIME = '2400'
   AND SEC_BRK_STR_TIME REGEXP '^[0-9]{4}$'
   AND SEC_SCH_BRK_MIN  REGEXP '^[0-9]+$';

-- ----------------------------------------------------------------------------
-- [3] 이력 테이블 — 현행/이력 쌍 원칙(CLAUDE.md). 값만 같은 규칙으로 보정(새 이력 행 INSERT 없음).
-- ----------------------------------------------------------------------------
UPDATE tb_sch_mgmt_hist
   SET FST_BRK_END_TIME = CONCAT(
           LPAD(((CAST(SUBSTR(FST_BRK_STR_TIME, 1, 2) AS SIGNED) * 60
                 + CAST(SUBSTR(FST_BRK_STR_TIME, 3, 2) AS SIGNED)
                 + CAST(FST_SCH_BRK_MIN AS SIGNED)) MOD 1440) DIV 60, 2, '0')
         , LPAD(((CAST(SUBSTR(FST_BRK_STR_TIME, 1, 2) AS SIGNED) * 60
                 + CAST(SUBSTR(FST_BRK_STR_TIME, 3, 2) AS SIGNED)
                 + CAST(FST_SCH_BRK_MIN AS SIGNED)) MOD 1440) MOD 60, 2, '0'))
 WHERE FST_BRK_END_TIME = '2400'
   AND FST_BRK_STR_TIME REGEXP '^[0-9]{4}$'
   AND FST_SCH_BRK_MIN  REGEXP '^[0-9]+$';

UPDATE tb_sch_mgmt_hist
   SET SEC_BRK_END_TIME = CONCAT(
           LPAD(((CAST(SUBSTR(SEC_BRK_STR_TIME, 1, 2) AS SIGNED) * 60
                 + CAST(SUBSTR(SEC_BRK_STR_TIME, 3, 2) AS SIGNED)
                 + CAST(SEC_SCH_BRK_MIN AS SIGNED)) MOD 1440) DIV 60, 2, '0')
         , LPAD(((CAST(SUBSTR(SEC_BRK_STR_TIME, 1, 2) AS SIGNED) * 60
                 + CAST(SUBSTR(SEC_BRK_STR_TIME, 3, 2) AS SIGNED)
                 + CAST(SEC_SCH_BRK_MIN AS SIGNED)) MOD 1440) MOD 60, 2, '0'))
 WHERE SEC_BRK_END_TIME = '2400'
   AND SEC_BRK_STR_TIME REGEXP '^[0-9]{4}$'
   AND SEC_SCH_BRK_MIN  REGEXP '^[0-9]+$';

-- ----------------------------------------------------------------------------
-- [4] 사후 검증 ① '2400' 잔존 — 현행·이력 모두 기대 0건
--     (0건이 아니면 휴게 시작/휴게분이 숫자 4자리/숫자가 아닌 행 — 사람이 확인)
-- ----------------------------------------------------------------------------
SELECT 'tb_sch_mgmt' AS tbl
     , COUNT(*) AS remain_2400
  FROM tb_sch_mgmt
 WHERE FST_BRK_END_TIME = '2400'
    OR SEC_BRK_END_TIME = '2400'
UNION ALL
SELECT 'tb_sch_mgmt_hist' AS tbl
     , COUNT(*) AS remain_2400
  FROM tb_sch_mgmt_hist
 WHERE FST_BRK_END_TIME = '2400'
    OR SEC_BRK_END_TIME = '2400';

-- ----------------------------------------------------------------------------
-- [5] 사후 검증 ② 휴게 시각 폭(자정 wrap 반영) ≠ 휴게분 행 — 기대 0건
--     (G-6 서버 검증(BW-10)이 앞으로 막을 상태를 데이터로 선확인. 0폭('0000'~'0000' 등 시작=종료)은 제외)
-- ----------------------------------------------------------------------------
-- (5-1) 1구간
SELECT CMPNY_CD
     , SITE_CD
     , SCH_CD
     , FST_BRK_STR_TIME
     , FST_BRK_END_TIME
     , FST_SCH_BRK_MIN
     , ((CAST(SUBSTR(FST_BRK_END_TIME, 1, 2) AS SIGNED) * 60 + CAST(SUBSTR(FST_BRK_END_TIME, 3, 2) AS SIGNED)
        - CAST(SUBSTR(FST_BRK_STR_TIME, 1, 2) AS SIGNED) * 60 - CAST(SUBSTR(FST_BRK_STR_TIME, 3, 2) AS SIGNED)
        + 1440) MOD 1440) AS brk_width
  FROM tb_sch_mgmt
 WHERE FST_BRK_STR_TIME REGEXP '^[0-9]{4}$'
   AND FST_BRK_END_TIME REGEXP '^[0-9]{4}$'
   AND FST_BRK_STR_TIME <> FST_BRK_END_TIME
   AND FST_SCH_BRK_MIN  REGEXP '^[0-9]+$'
   AND ((CAST(SUBSTR(FST_BRK_END_TIME, 1, 2) AS SIGNED) * 60 + CAST(SUBSTR(FST_BRK_END_TIME, 3, 2) AS SIGNED)
        - CAST(SUBSTR(FST_BRK_STR_TIME, 1, 2) AS SIGNED) * 60 - CAST(SUBSTR(FST_BRK_STR_TIME, 3, 2) AS SIGNED)
        + 1440) MOD 1440) <> CAST(FST_SCH_BRK_MIN AS SIGNED)
 LIMIT 50;

-- (5-2) 2구간
SELECT CMPNY_CD
     , SITE_CD
     , SCH_CD
     , SEC_BRK_STR_TIME
     , SEC_BRK_END_TIME
     , SEC_SCH_BRK_MIN
     , ((CAST(SUBSTR(SEC_BRK_END_TIME, 1, 2) AS SIGNED) * 60 + CAST(SUBSTR(SEC_BRK_END_TIME, 3, 2) AS SIGNED)
        - CAST(SUBSTR(SEC_BRK_STR_TIME, 1, 2) AS SIGNED) * 60 - CAST(SUBSTR(SEC_BRK_STR_TIME, 3, 2) AS SIGNED)
        + 1440) MOD 1440) AS brk_width
  FROM tb_sch_mgmt
 WHERE SEC_BRK_STR_TIME REGEXP '^[0-9]{4}$'
   AND SEC_BRK_END_TIME REGEXP '^[0-9]{4}$'
   AND SEC_BRK_STR_TIME <> SEC_BRK_END_TIME
   AND SEC_SCH_BRK_MIN  REGEXP '^[0-9]+$'
   AND ((CAST(SUBSTR(SEC_BRK_END_TIME, 1, 2) AS SIGNED) * 60 + CAST(SUBSTR(SEC_BRK_END_TIME, 3, 2) AS SIGNED)
        - CAST(SUBSTR(SEC_BRK_STR_TIME, 1, 2) AS SIGNED) * 60 - CAST(SUBSTR(SEC_BRK_STR_TIME, 3, 2) AS SIGNED)
        + 1440) MOD 1440) <> CAST(SEC_SCH_BRK_MIN AS SIGNED)
 LIMIT 50;

-- (5-3) 보정 대상이었던 행의 최종 상태 — [0-1] 의 new_end 와 대조
SELECT CMPNY_CD
     , SITE_CD
     , SCH_CD
     , FST_BRK_STR_TIME
     , FST_BRK_END_TIME
     , SEC_BRK_STR_TIME
     , SEC_BRK_END_TIME
     , UPDATE_NO
     , UPDATE_DATE
  FROM tb_sch_mgmt
 WHERE UPDATE_NO = 'BRK_WAIVE_2_FIX'
 LIMIT 50;
