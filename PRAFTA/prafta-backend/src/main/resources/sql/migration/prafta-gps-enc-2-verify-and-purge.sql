-- ============================================================================
-- GPS좌표-암호화-전환-05 — 백필 검증 + 평문 소거 (수동 실행용, 3테이블)
-- 작성일: 2026-07-25
-- 적용 환경: MySQL 8.4 (prafta 운영)
-- 참조: .claude/requests/web_requests/작업지시서_GPS좌표-암호화-전환.md §4-T3
--       .claude/requests/web_requests/작업지시서_GPS좌표-암호화-전환.plan.md -05
--
-- 실행 전제(배포 순서 plan §4 — 엄수)
--  1) prafta-gps-enc-1-ddl.sql 적용 완료
--  2) 백엔드 코드(암호화 쓰기 + fallback 읽기) 배포 + 무회귀 확인 완료
--  3) GpsEncBackfillRunner 백필 실행(prafta.gps-enc.backfill.enabled=true 1회 기동) +
--     로그로 대상건수=성공건수 확인 + 플래그 OFF 복귀 완료
--  4) ★ 소거(§2) 실행 직전 운영 DB 스냅샷 필수 — 소거 후 복구는 스냅샷 복원만 가능하다
--     (암호문은 SQL 로 복호화 불가 — AES 키는 서버 환경변수에만 존재).
--
-- 주의
--  - 좌표 평문 값을 화면/로그에 출력하지 않는다 — 본 파일의 검증 쿼리는 건수(COUNT)만 조회.
--  - ★ 소거 후 구버전 코드 롤백 절대 금지(전 행 좌표 null 표시 + TBM AUTO 입실 전면 차단, plan §4-4).
-- ============================================================================

-- ============================================================================
-- §1. 백필 완료 검증 — 3개 쿼리 모두 0 이어야 소거 진행 가능(값 미출력, 건수만)
-- ============================================================================

-- (1-1) 근태 GPS: 평문은 있는데 암호문이 없는 행(=백필 누락) → 0 확인
--   ※ 술어는 LAT/LON 쌍 OR 기준 — 짝 불일치 행(한쪽만 평문)도 누락 없이 검출
--     (sec 리뷰 gps좌표-암호화-전환-001 반영, 2026-07-26).
SELECT COUNT(*) AS attd_gps_unfilled
  FROM TB_USER_ATTD_GPS
 WHERE (LAT IS NOT NULL OR LON IS NOT NULL)
   AND LAT_ENC IS NULL;

-- (1-2) TBM 입실 좌표: 동일 → 0 확인
SELECT COUNT(*) AS tbm_attendance_unfilled
  FROM TB_TBM_ATTENDANCE
 WHERE (ENTRY_GPS_LAT IS NOT NULL OR ENTRY_GPS_LON IS NOT NULL)
   AND ENTRY_GPS_LAT_ENC IS NULL;

-- (1-3) TBM 세션 관리자 좌표: 동일 → 0 확인
SELECT COUNT(*) AS tbm_session_unfilled
  FROM TB_TBM_SESSION
 WHERE (MANAGER_GPS_LAT IS NOT NULL OR MANAGER_GPS_LON IS NOT NULL)
   AND MANAGER_GPS_LAT_ENC IS NULL;

-- (참고) 표본 복호화 대조는 SQL 로 불가(키는 서버에만) — 백필 러너의 행별 자가검증
-- (암호화 직후 재복호화 = 원 평문 equals, 불일치 시 전체 롤백)이 이를 대체한다.

-- ============================================================================
-- §2. 평문 소거 — 컬럼 유지, 값만 NULL (실행 직전 운영 스냅샷 필수)
--
-- WHERE 에 키 컬럼이 없어 Workbench safe updates(1175) 에 걸린다 → 아래 세션 토글로 우회
-- (Preferences 영구 해제 금지 — 세션 한정).
-- ============================================================================

-- SET SQL_SAFE_UPDATES = 0;   -- 세션 한정 해제(소거 실행 시에만)

-- (2-1) 근태 GPS 평문 소거
UPDATE TB_USER_ATTD_GPS
   SET LAT = NULL
     , LON = NULL
 WHERE LAT_ENC IS NOT NULL;

-- (2-2) TBM 입실 좌표 평문 소거
UPDATE TB_TBM_ATTENDANCE
   SET ENTRY_GPS_LAT = NULL
     , ENTRY_GPS_LON = NULL
 WHERE ENTRY_GPS_LAT_ENC IS NOT NULL;

-- (2-3) TBM 세션 관리자 좌표 평문 소거
UPDATE TB_TBM_SESSION
   SET MANAGER_GPS_LAT = NULL
     , MANAGER_GPS_LON = NULL
 WHERE MANAGER_GPS_LAT_ENC IS NOT NULL;

-- SET SQL_SAFE_UPDATES = 1;   -- 세션 토글 원복

-- ============================================================================
-- §3. 소거 후 최종 검증 — 3개 쿼리 모두 0 이어야 완료(잔존 평문 없음)
-- ============================================================================

-- ※ 술어는 LAT/LON 쌍 OR 기준(sec 리뷰 gps좌표-암호화-전환-001 반영, 2026-07-26).
SELECT COUNT(*) AS attd_gps_plain_left
  FROM TB_USER_ATTD_GPS
 WHERE LAT IS NOT NULL OR LON IS NOT NULL;

SELECT COUNT(*) AS tbm_attendance_plain_left
  FROM TB_TBM_ATTENDANCE
 WHERE ENTRY_GPS_LAT IS NOT NULL OR ENTRY_GPS_LON IS NOT NULL;

SELECT COUNT(*) AS tbm_session_plain_left
  FROM TB_TBM_SESSION
 WHERE MANAGER_GPS_LAT IS NOT NULL OR MANAGER_GPS_LON IS NOT NULL;

-- 이후 화면 최종 검증: 웹 attd-gps-trail / Platform_04 열람(TBM 행 누락 없음) / TBM 입실·세션 상세.

-- ============================================================================
-- 롤백 노트
--  - 소거 후 평문 복구는 §2 실행 직전 스냅샷 복원만 가능(암호문 SQL 복호화 불가 — 키는 서버에만).
--  - 소거 이전 단계 롤백은 prafta-gps-enc-1-ddl.sql 하단 롤백 구문/전제 참조.
-- ============================================================================
