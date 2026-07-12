-- ============================================================================
-- PRAFTA-leave-usedays-resync-1 — GRANT 원장(USED_DAYS) 전수 재동기화 (1회성 정리)
-- 작성일: 2026-07-11
-- 적용 환경: MySQL 8.0.42
--
-- 배경
--  연차 시간차 환산 개편(LC-12) 검증 V3에서 USED_DAYS ≠ CONFIRMED use 합계인
--  GRANT 2건 발견 (G2026052600230/231, 사용자 20260400013 월차).
--  원인: com-008 QA 시드/롤백 스크립트(sql/qa/prafta-com-008-qa-seed-rollback.sql)가
--  tb_user_leave_use 행을 물리 DELETE 하면서 GRANT 재집계(recomputeGrantUsedDays)를
--  다시 돌리지 않아 USED_DAYS=1.0 이 잔류 — 테스트 데이터 드리프트(운영 로직 무관).
--  이번 개편 마이그레이션(conv-1/conv-2)과는 무관하며, conv-2 는 이 GRANT 들을 건드리지 않았다.
--
-- 보정 규칙
--  USED_DAYS = 해당 GRANT 에 연결된 use 합계 (LEAVE_STATUS='CONFIRMED' AND DEL_YN='N')
--  — 런타임 정본 산식(LeaveFlowMapper.recomputeGrantUsedDays)과 동일.
--  STATUS 는 건드리지 않는다(발견 2건 모두 ACTIVE — 전이 불필요.
--  향후 다른 상태의 불일치가 잡히면 사전 리포트에서 확인 후 별도 판단).
--
-- 멱등성: 재계산식이 결정적 + `USED_DAYS <> 재집계값` 조건 → 재실행 시 변경 0건.
-- ⚠️ 실행은 사용자 수동. 실행 전 [사전 리포트] 로 대상 확인 후 진행.
-- ============================================================================

-- 세션 안전모드 해제 (Workbench SQL_SAFE_UPDATES=1 → 1175 방지), 말미 원복
SET @old_safe_updates := @@SQL_SAFE_UPDATES;
SET SQL_SAFE_UPDATES = 0;

START TRANSACTION;

-- ----------------------------------------------------------------------------
-- 1) [사전 리포트] 불일치 GRANT 전수 — 보정 전 값 보관용 (결과 저장 권장)
--    STATUS 가 ACTIVE 외인 행이 있으면 COMMIT 전에 별도 검토할 것.
-- ----------------------------------------------------------------------------
SELECT
      G.CMPNY_CD
    , G.GRANT_ID
    , G.USER_CD
    , G.LEAVE_CD
    , G.STATUS
    , G.GRANT_DAYS
    , G.USED_DAYS                                   AS OLD_USED_DAYS
    , IFNULL((
        SELECT SUM(U.LEAVE_DAYS)
          FROM tb_user_leave_use U
         WHERE U.CMPNY_CD     = G.CMPNY_CD
           AND U.GRANT_ID     = G.GRANT_ID
           AND U.LEAVE_STATUS = 'CONFIRMED'
           AND U.DEL_YN       = 'N'
      ), 0)                                         AS NEW_USED_DAYS
  FROM tb_user_leave_grant G
 WHERE G.USED_DAYS <> IFNULL((
        SELECT SUM(U.LEAVE_DAYS)
          FROM tb_user_leave_use U
         WHERE U.CMPNY_CD     = G.CMPNY_CD
           AND U.GRANT_ID     = G.GRANT_ID
           AND U.LEAVE_STATUS = 'CONFIRMED'
           AND U.DEL_YN       = 'N'
      ), 0);

-- ----------------------------------------------------------------------------
-- 2) 본 보정 — 불일치 GRANT 만 재집계값으로 UPDATE (멱등)
-- ----------------------------------------------------------------------------
UPDATE tb_user_leave_grant G
   SET G.USED_DAYS = IFNULL((
           SELECT SUM(U.LEAVE_DAYS)
             FROM tb_user_leave_use U
            WHERE U.CMPNY_CD     = G.CMPNY_CD
              AND U.GRANT_ID     = G.GRANT_ID
              AND U.LEAVE_STATUS = 'CONFIRMED'
              AND U.DEL_YN       = 'N'
       ), 0)
     , G.UPDATE_NO   = 'SYSTEM'
     , G.UPDATE_DATE = NOW()
 WHERE G.USED_DAYS <> IFNULL((
        SELECT SUM(U2.LEAVE_DAYS)
          FROM tb_user_leave_use U2
         WHERE U2.CMPNY_CD     = G.CMPNY_CD
           AND U2.GRANT_ID     = G.GRANT_ID
           AND U2.LEAVE_STATUS = 'CONFIRMED'
           AND U2.DEL_YN       = 'N'
      ), 0);

-- ----------------------------------------------------------------------------
-- 3) [사후 확인] 불일치 잔존 0건이어야 함
-- ----------------------------------------------------------------------------
SELECT COUNT(*) AS `불일치_잔존(0이어야)`
  FROM tb_user_leave_grant G
 WHERE G.USED_DAYS <> IFNULL((
        SELECT SUM(U.LEAVE_DAYS)
          FROM tb_user_leave_use U
         WHERE U.CMPNY_CD     = G.CMPNY_CD
           AND U.GRANT_ID     = G.GRANT_ID
           AND U.LEAVE_STATUS = 'CONFIRMED'
           AND U.DEL_YN       = 'N'
      ), 0);

-- 사후 확인이 0건이면 확정, 아니면 ROLLBACK; 후 보고.
COMMIT;

-- 세션 안전모드 원복
SET SQL_SAFE_UPDATES = @old_safe_updates;
