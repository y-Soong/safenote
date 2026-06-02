-- =============================================================================
-- prafta-029 연차 테스트 데이터 정리 (개발/로컬 DB 전용)
-- 생성: 2026-05-25
-- 목적: 반복 테스트(입사일 변경 + RESET_ALL/정책기준부여)로 누적된 연차 부여/이력/사용
--       데이터를 모두 제거해 재테스트를 clean 상태에서 시작.
--
-- 대상 회사: CMPNY_CD = '001'  (현재 연차 데이터가 있는 유일한 회사)
--   - tb_user_leave_grant   : 134행 / 2명  (STATUTORY_* + MANUAL_* 전부)
--   - tb_user_hire_date_history : 28행 / 3명
--   - tb_user_leave_use     : 2행 / 1명
--
-- 보존(삭제하지 않음 — 설정 데이터):
--   - TB_LEAVE_POLICY / TB_LEAVE_POLICY_HISTORY  (회사 연차 부여 정책)
--   - TB_LEAVE_TYPE_MGMT                         (연차 타입 정의 + 시스템 시드 SYS_ANNUAL 등)
--   - TB_LEAVE_USAGE_POLICY                      (사용 단위 정책)
--   - TB_USER_SERVICE_CREDIT                     (경력 인정) — 필요 시 별도 삭제
--   - TB_USER.HIRE_DATE                          (사용자 입사일 자체 — 속성이라 유지)
--
-- ⚠️ 실행 전 백업 권장. 운영 DB 금지(로컬/개발 DB만). 되돌릴 수 없음.
-- ⚠️ MCP(read-only)로는 실행 불가 — MySQL 클라이언트/워크벤치에서 직접 실행.
-- =============================================================================

SET @cmpny := '001';

-- (선택) 특정 사용자만 정리하려면 아래 주석을 풀고 @user 지정 + 각 DELETE에 AND USER_CD=@user 추가
-- SET @user := '20260400013';

-- 1) 사용 이력 먼저 삭제 (tb_user_leave_use.GRANT_ID → tb_user_leave_grant FK 대비)
DELETE FROM tb_user_leave_use         WHERE CMPNY_CD = @cmpny;

-- 2) 연차 부여 이력 삭제 (STATUTORY_* / MANUAL_* 모두 — 멱등키/STATUS 누적 흔적 제거)
DELETE FROM tb_user_leave_grant       WHERE CMPNY_CD = @cmpny;

-- 3) 입사일 변경 이력 삭제 (테스트 churn)
DELETE FROM tb_user_hire_date_history WHERE CMPNY_CD = @cmpny;

-- 4) 결과 확인 (모두 0이어야 함)
SELECT 'grant'     AS tbl, COUNT(*) AS rows_left FROM tb_user_leave_grant       WHERE CMPNY_CD = @cmpny
UNION ALL
SELECT 'leave_use' AS tbl, COUNT(*)              FROM tb_user_leave_use         WHERE CMPNY_CD = @cmpny
UNION ALL
SELECT 'hire_hist' AS tbl, COUNT(*)              FROM tb_user_hire_date_history WHERE CMPNY_CD = @cmpny;

-- 만약 FK 제약으로 (2)가 막히면(다른 테이블이 GRANT_ID 참조 시), 같은 세션에서 일시적으로:
--   SET FOREIGN_KEY_CHECKS = 0;  -- DELETE 3건 실행  SET FOREIGN_KEY_CHECKS = 1;
-- 다만 참조 무결성을 깨므로 개발 DB에서만, 참조 테이블도 함께 정리할 때만 사용.
