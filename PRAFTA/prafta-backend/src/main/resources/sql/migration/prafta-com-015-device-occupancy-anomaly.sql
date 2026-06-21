-- ============================================================================
-- PRAFTA-COM-015 — 디바이스 점유 재할당 이상탐지 + 감사 (015-1)
-- 작성일: 2026-06-17
-- 적용 환경: MySQL 8.0.42
-- 참조: .claude/requests/common/prafta-com-015-device-occupancy-hardening.md §3 015-1,
--       prafta-com-003(디바이스 식별 부정탐지) 계보.
--
-- 변경 요약
--   1) tb_user_device_occupancy_anomaly 신규 — 한 DEVICE_UUID 의 점유자(USER_CD)가
--      "다른 계정"으로 바뀌는 로그인을 감지해 적재하는 감사 테이블(append-only).
--      점유 재할당 자체는 막지 않고(=감사만), 흔적만 남긴다(R1 1차 방어).
--      PK = ANOMALY_NO = CONCAT(YYYYMM, FNC_CMM_SEQ_NEXTVAL(cmpnyCd,'DEVICE_ANOMALY_NO')).
--      iOS: IDFV 변경/재설치는 USER_CD 동일 → 미적재(false-positive 차단, 코드에서 게이트).
--
-- ★ 적용 선행성(중요): 본 마이그가 운영 DB 에 적용되기 전에는
--   로그인 훅의 insertOccupancyAnomaly 가 "Unknown table" 으로 실패한다(단, try-catch
--   격리되어 로그인 자체는 정상). 점유 이상만 미적재. 운영 적용 시 선적용 필수.
--
-- 신규 채번 시퀀스 'DEVICE_ANOMALY_NO' 는 FNC_CMM_SEQ_NEXTVAL 이 최초 호출 시 자동 생성한다고
--   가정한다(기존 DEVICE_LOGIN_NO 와 동일 패턴 — 별도 시드 불필요).
--
-- 멱등성: CREATE TABLE 중복 실행 시 에러. 이미 반영된 환경에서는 건너뛸 것.
-- 운영 적용: 사용자 수동 적용·운영 선적용 필수(read-only MCP). 본 파일은 작성만, DB 직접 적용 금지.
-- ============================================================================

-- 적용 전 부재 확인 (운영 적용 직전 권장):
--   SHOW TABLES LIKE 'tb_user_device_occupancy_anomaly';

-- ----------------------------------------------------------------------------
-- 1) 디바이스 점유 재할당 이상 감사 테이블 신설 (append-only)
-- ----------------------------------------------------------------------------
CREATE TABLE `tb_user_device_occupancy_anomaly` (
      `ANOMALY_NO`      varchar(20)   NOT NULL COMMENT '점유 이상 번호(PK, 회사별 채번: YYYYMM + SEQ)'
    , `CMPNY_CD`        varchar(50)   NOT NULL COMMENT '회사 코드'
    , `DEVICE_UUID`     varchar(100)  NOT NULL COMMENT '디바이스UUID(클라 제공, 네이티브 ANDROID_ID/IDFV 우선)'
    , `PREV_USER_CD`    varchar(20)   NOT NULL COMMENT '직전 점유자 사용자 코드(재할당 전 USER_CD)'
    , `NEW_USER_CD`     varchar(20)   NOT NULL COMMENT '로그인 사용자 코드(재할당 후 USER_CD)'
    , `CLIENT_TYPE`     varchar(10)            DEFAULT NULL COMMENT '클라이언트 구분[자유값] APP:앱 / WEB:웹'
    , `LOGIN_IP`        varchar(45)            DEFAULT NULL COMMENT '로그인 IP(HttpServletRequest 추출)'
    , `DETECTED_DTIME`  datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '이상 감지 일시'
    , `INSERT_NO`       varchar(50)            DEFAULT 'SYSTEM' COMMENT '입력자'
    , `INSERT_DATE`     datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시'
    , PRIMARY KEY (`ANOMALY_NO`)
    , KEY `IDX_DOA_DEVICE` (`CMPNY_CD`, `DEVICE_UUID`, `DETECTED_DTIME`)
    , KEY `IDX_DOA_NEWUSER` (`CMPNY_CD`, `NEW_USER_CD`, `DETECTED_DTIME`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='디바이스 점유 재할당 이상 감사(append-only, 다른 계정의 기존 기기 UUID 로그인 감지)';

-- ============================================================================
-- 롤백 (필요 시 수동 실행)
-- ----------------------------------------------------------------------------
-- DROP TABLE IF EXISTS `tb_user_device_occupancy_anomaly`;
-- ============================================================================
