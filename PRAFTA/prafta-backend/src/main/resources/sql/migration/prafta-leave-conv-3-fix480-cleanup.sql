-- ============================================================================
-- PRAFTA-leave-conv-3 — 시간차 1일 환산시간 480분(8시간) 고정 전환: 정책 행 정리
-- 작성일: 2026-07-21
-- 적용 환경: MySQL 8.0.42 (운영 + 개발 공통)
-- 출처: 2026-07-21 사용자 결정 — 회사별 환산시간 설정 폐기, 480분 전사 고정.
--       배경: 유한소수 검증(R2) 때문에 현실적 대안값(420/360/450 등)이 입력 불가라
--       설정의 실용성이 없었고, 설정 변경이 잔여 표기 변동·혼합 이력·최소단위 미만
--       끝수의 원천이었음. 코드 측은 LeaveConversionPolicyService 가 480 상수 반환으로
--       봉인되고 Baim_07 설정 화면·/baim07/conversion 엔드포인트는 제거됨.
--
-- 변경 요약
--  1) tb_leave_conversion_policy 전 행 백업 후 삭제 (모든 회사).
--     - 삭제 후에는 코드 폴백(480)과 동일하게 동작하므로, 구버전 백엔드가 떠 있는
--       배포 전 시점에도 본 파일을 먼저 실행하면 즉시 480으로 수렴한다.
--     - 특히 운영 001 회사의 20260801→400 행은 방치 시 08-01부터 400 재발효되므로
--       배포와 무관하게 시급히 제거해야 함.
--  2) 테이블 자체는 드랍하지 않음(dormant) — 설정형 복원 대비 + DDL 최소화.
--
-- 부수 효과(승인된 트레이드오프)
--  - 대상일 20260714~20260720 구간의 시간차 신청은 400 분모로 차감된 상태로 남는다
--    (소급 재계산 없음 원칙). 해당 건이 이후 취소·반려로 재정산되면 그날 전체가
--    480 분모로 재계산되어 수렴한다(방향: 차감 감소 = 근로자 유리).
--  - 본 파일 §3 리포트로 비480 분모 차감 잔존 건을 확인하고, 정리가 필요하면
--    관리자 취소→재신청 경로로 처리한다. tb_user_leave_use / tb_user_leave_grant
--    원장 직접 DML 은 캐시(USED_DAYS) 정합이 깨지므로 절대 금지.
--
-- 멱등성(재실행 안전):
--  1) 백업 테이블 CREATE TABLE IF NOT EXISTS ... LIKE — 존재 시 no-op.
--  2) 백업 INSERT 는 INSERT IGNORE (PK 중복 skip) — 재실행 시 중복 적재 없음.
--  3) DELETE 는 잔존 행만 삭제 — 0건이어도 무해.
--
-- 운영 적용: 사용자 수동(read-only MCP). 본 파일은 작성만, DB 직접 적용 금지.
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 0) 사전 리포트 — 현재 정책 행 확인 (실행 전 눈으로 확인)
-- ----------------------------------------------------------------------------
SELECT CMPNY_CD
     , APPLY_FROM_DATE
     , DAILY_CONV_MINUTES
     , INSERT_NO
     , INSERT_DATE
     , UPDATE_NO
     , UPDATE_DATE
  FROM tb_leave_conversion_policy
 ORDER BY CMPNY_CD, APPLY_FROM_DATE
 LIMIT 100;

-- ----------------------------------------------------------------------------
-- 1) 백업 — tb_leave_conv_policy_bak_fix480 (원 PK 유지, 재실행 시 중복 skip)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS tb_leave_conv_policy_bak_fix480 LIKE tb_leave_conversion_policy;

INSERT IGNORE INTO tb_leave_conv_policy_bak_fix480
SELECT CMPNY_CD
     , APPLY_FROM_DATE
     , DAILY_CONV_MINUTES
     , INSERT_NO
     , INSERT_DATE
     , UPDATE_NO
     , UPDATE_DATE
  FROM tb_leave_conversion_policy;

-- ----------------------------------------------------------------------------
-- 2) 정책 행 전량 삭제 — 480 행 포함 전부 삭제해도 코드 폴백(480)과 동작 동일.
--    safe updates(1175) 대응: IS NOT NULL 은 키-상수 비교가 아니라 거부됨 →
--    PK 선두 컬럼의 상수 범위 비교(>= '')로 전 행 매치 (safe updates OFF 불필요).
-- ----------------------------------------------------------------------------
DELETE FROM tb_leave_conversion_policy
 WHERE CMPNY_CD >= '';

-- ----------------------------------------------------------------------------
-- 3) 사후 리포트 — 비480 분모로 차감된 시간차 원장 잔존 건 (정보성, 수동 확인)
--    implied_conv = LEAVE_MINUTES / LEAVE_DAYS. 480 이외 값이면 과거 분모의 잔재:
--      · 420.0 → 개편 전 구공식(스케줄 분모) 잔재 — 감안하고 유지하기로 결정(07-21)
--      · 400.0 → 20260714~20260720 / 20260801 예약분 대상일 신청 — 필요 시
--                관리자 취소→재신청으로 480 재계산 유도 (원장 직접 DML 금지)
-- ----------------------------------------------------------------------------
SELECT u.CMPNY_CD
     , u.USER_CD
     , u.LEAVE_ID
     , u.START_DATE
     , u.USE_UNIT_TYPE
     , u.LEAVE_MINUTES
     , u.LEAVE_DAYS
     , ROUND(u.LEAVE_MINUTES / NULLIF(u.LEAVE_DAYS, 0), 1) AS IMPLIED_CONV
     , u.LEAVE_STATUS
  FROM tb_user_leave_use u
 WHERE u.DEL_YN = 'N'
   AND u.LEAVE_STATUS IN ('CONFIRMED', 'PENDING')
   AND u.LEAVE_MINUTES IS NOT NULL
   AND u.LEAVE_DAYS > 0
   AND ROUND(u.LEAVE_MINUTES / u.LEAVE_DAYS, 1) <> 480.0
 ORDER BY u.CMPNY_CD, u.START_DATE, u.USER_CD
 LIMIT 200;

-- ----------------------------------------------------------------------------
-- 적용 후 확인 쿼리 (수동)
-- ----------------------------------------------------------------------------
-- SELECT COUNT(*) FROM tb_leave_conversion_policy;              -- → 0 확인
-- SELECT COUNT(*) FROM tb_leave_conv_policy_bak_fix480;         -- → 백업 건수 확인
--   (운영 예상: 001 회사 3행 — 20260714/400, 20260721/480, 20260801/400)

-- ============================================================================
-- 롤백 (필요 시 수동 실행 — 480 고정 코드 배포 후에는 복원해도 무시됨에 유의)
-- ----------------------------------------------------------------------------
-- INSERT IGNORE INTO tb_leave_conversion_policy
-- SELECT * FROM tb_leave_conv_policy_bak_fix480;
-- ============================================================================
