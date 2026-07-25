-- ============================================================================
-- GPS좌표-암호화-전환-01 — 개인 위치정보(GPS 좌표) 암호문 컬럼 추가 (3테이블)
-- 작성일: 2026-07-25
-- 적용 환경: MySQL 8.4 (prafta)
-- 참조: .claude/requests/web_requests/작업지시서_GPS좌표-암호화-전환.md §4-T1
--       .claude/requests/web_requests/작업지시서_GPS좌표-암호화-전환.plan.md -01
--
-- 변경 요약
--  1) TB_USER_ATTD_GPS    — LAT_ENC/LON_ENC(TEXT NULL) 추가 + 평문 LAT/LON NULL 허용 전환
--  2) TB_TBM_ATTENDANCE   — ENTRY_GPS_LAT_ENC/ENTRY_GPS_LON_ENC(TEXT NULL) 추가
--  3) TB_TBM_SESSION      — MANAGER_GPS_LAT_ENC/MANAGER_GPS_LON_ENC(TEXT NULL) 추가
--
-- 규약 / 주의
--  - 암호문 포맷: AES-GCM "v1.<Base64URL>" (AesGcmCrypto, 키 PRAFTA_AES_DATA_KEY — 기존 PII 동일).
--  - 평문 컬럼 DROP 금지(작업지시서 §2.3 — 전환기 fallback, 소거는 값만 NULL, 후속 별도).
--  - TBM 4컬럼(ENTRY_GPS_*, MANAGER_GPS_*)은 이미 NULL 허용 — MODIFY 불필요(plan §0.6-3).
--  - 배포 순서(plan §4): 본 DDL은 1단계 선행 단독 적용. 기존 코드와 완전 호환(추가 컬럼 무시), 무중단.
--  - ⚠️ 운영 적용은 사용자 수동(SSH 경유 mysql). 본 파일은 작성만, DB 직접 적용 금지.
-- ============================================================================

-- (1) 근태 출퇴근 GPS: 암호문 컬럼 추가 + 평문 NULL 허용(신규 행은 평문 미기록 전제)
ALTER TABLE TB_USER_ATTD_GPS
      ADD COLUMN LAT_ENC TEXT NULL COMMENT '위도 암호문(AES-GCM v1., 평문 LAT 대체)' AFTER LON
    , ADD COLUMN LON_ENC TEXT NULL COMMENT '경도 암호문(AES-GCM v1., 평문 LON 대체)' AFTER LAT_ENC
    , MODIFY COLUMN LAT DECIMAL(10,7) NULL COMMENT '위도(평문, 전환기 fallback — 백필 후 소거)'
    , MODIFY COLUMN LON DECIMAL(10,7) NULL COMMENT '경도(평문, 전환기 fallback — 백필 후 소거)';

-- (2) TBM 근로자 입실 좌표: 암호문 컬럼 추가(평문 컬럼은 이미 NULL 허용)
ALTER TABLE TB_TBM_ATTENDANCE
      ADD COLUMN ENTRY_GPS_LAT_ENC TEXT NULL COMMENT '입실 위도 암호문(AES-GCM v1.)' AFTER ENTRY_GPS_LON
    , ADD COLUMN ENTRY_GPS_LON_ENC TEXT NULL COMMENT '입실 경도 암호문(AES-GCM v1.)' AFTER ENTRY_GPS_LAT_ENC;

-- (3) TBM 세션 관리자(개설) 좌표: 암호문 컬럼 추가(평문 컬럼은 이미 NULL 허용)
ALTER TABLE TB_TBM_SESSION
      ADD COLUMN MANAGER_GPS_LAT_ENC TEXT NULL COMMENT '개설 위도 암호문(AES-GCM v1.)' AFTER MANAGER_GPS_LON
    , ADD COLUMN MANAGER_GPS_LON_ENC TEXT NULL COMMENT '개설 경도 암호문(AES-GCM v1.)' AFTER MANAGER_GPS_LAT_ENC;

-- ============================================================================
-- 롤백 (필요 시 수동 실행 — 실행 전 아래 전제 확인 필수)
--
-- 전제: TB_USER_ATTD_GPS 의 NOT NULL 복원은 LAT/LON IS NULL 행이 0건일 때만 가능하다.
--       2단계(백엔드 코드) 배포 "이전" 시점이면 신규 행이 전부 평문으로 기록되므로 항상 안전.
--       2단계 배포 "이후"에는 평문 NULL 행(암호문 전용 행)이 생길 수 있어 NOT NULL 복원 불가 —
--       그 경우 ENC 컬럼 DROP 도 데이터 유실이므로 롤백 대신 신버전 재배포가 원칙(plan §4).
--
-- ALTER TABLE TB_USER_ATTD_GPS
--       DROP COLUMN LAT_ENC
--     , DROP COLUMN LON_ENC
--     , MODIFY COLUMN LAT DECIMAL(10,7) NOT NULL COMMENT '위도'
--     , MODIFY COLUMN LON DECIMAL(10,7) NOT NULL COMMENT '경도';
--
-- ALTER TABLE TB_TBM_ATTENDANCE
--       DROP COLUMN ENTRY_GPS_LAT_ENC
--     , DROP COLUMN ENTRY_GPS_LON_ENC;
--
-- ALTER TABLE TB_TBM_SESSION
--       DROP COLUMN MANAGER_GPS_LAT_ENC
--     , DROP COLUMN MANAGER_GPS_LON_ENC;
-- ============================================================================
