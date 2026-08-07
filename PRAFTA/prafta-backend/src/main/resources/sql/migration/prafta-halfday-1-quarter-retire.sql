-- =====================================================================================
-- prafta-halfday-1-quarter-retire.sql
--   반차 시간대 도입 / 반반차(SYS025 '05') 폐지 — HB-04 (2026-08-07)
--
-- 목적
--   사용 단위를 종일 / 반차 / 2시간 / 1시간 / 30분 5종으로 정리한다.
--   코드값(SYS025 '05')과 과거 데이터 조회 경로는 그대로 두고, "신규 선택"만 닫는다.
--
-- ★ 적용 원칙: 개발·운영 양쪽에 모두 적용한다(한쪽만 적용 시 환경 불일치 장애 반복).
-- ★ 본 파일은 developer 가 실행하지 않는다. 사용자가 직접 실행한다.
--
-- 선행 조건(실행 전 반드시 0 확인 — 하나라도 0 이 아니면 중단하고 보고할 것)
--   ★ sec L-2: 사전조건 모수 = 아래 UPDATE 모수와 반드시 일치해야 한다("확인한 모수 ≠ 변경한 모수" 금지).
--     tb_leave_type_mgmt 는 DEL_YN 컬럼이 없고 USE_YN(사용여부)만 있다(실측). 사용중지된 '05' 행도
--     그대로 두면 나중에 다시 사용중으로 되살릴 때 '05' 가 부활하므로, 확인·변경 모두 무필터로 맞춘다.
--   SELECT COUNT(*) FROM tb_user_leave_use     WHERE USE_UNIT_TYPE = '05' AND DEL_YN = 'N';
--   SELECT COUNT(*) FROM tb_leave_usage_policy WHERE USAGE_UNIT    = 'QUARTER_DAY';
--   SELECT COUNT(*) FROM tb_leave_type_mgmt    WHERE USE_UNIT_TYPE = '05';
--
--   (개발 DB 실측 2026-08-07: 3건 모두 0. 운영 실측 2026-08-04 기준으로도 0 — 지시서 §1-4)
--
-- 되돌리기
--   SYS025 '05' 를 USE_YN='Y' 로 복구하면 코드값은 되살아나지만, 서버 코드가 신청 경로에서
--   '05' 를 거부(ATTD_400_102 / ATTD_400_054)하므로 신청은 열리지 않는다(코드 롤백 필요).
-- =====================================================================================

-- 1) 사용 단위 코드값 비활성화 — ★ 행 삭제 금지(과거 데이터 조회 시 코드명이 필요하다).
--    (컬럼 실측: SYST_VAL_CD = 코드그룹('SYS025') / SYST_VAL_D_CD = 코드값('05') / UPDATE_DATE 는 date 타입)
UPDATE TB_SYST_VAL_D
   SET USE_YN        = 'N'
     , UPDATE_NO     = 'SYSTEM'
     , UPDATE_DATE   = CURDATE()
 WHERE SYST_VAL_CD   = 'SYS025'
   AND SYST_VAL_D_CD = '05';

-- 2) 사용 단위 컬럼 COMMENT 갱신(QUARTER_DAY 폐지 명시).
--    ★ sec L-3: 원 정의 실측(2026-08-07 개발 DB information_schema) —
--      USAGE_UNIT    varchar(20) NOT NULL DEFAULT 'FULL_DAY', utf8mb4 / utf8mb4_unicode_ci
--      ALLOW_QUARTER char(1)     NOT NULL DEFAULT 'N',        utf8mb4 / utf8mb4_unicode_ci
--      (컬럼명은 ALLOW_QUARTER 가 맞다 — 스냅샷의 ALLOW_QUARTER_DAY 는 stale)
--      MODIFY 는 정의 전체를 다시 쓰므로 charset/collate 를 명시한다(누락 시 테이블 기본값으로
--      바뀌어 1267 콜레이션 충돌 전례가 있다). 운영 실행 전 SHOW CREATE TABLE 로 재확인할 것.
ALTER TABLE TB_LEAVE_USAGE_POLICY
    MODIFY COLUMN USAGE_UNIT varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci
    NOT NULL DEFAULT 'FULL_DAY'
    COMMENT '회사 허용 사용단위(단일): FULL_DAY/HALF_DAY/HOUR_2/HOUR_1/MIN_30. QUARTER_DAY=[폐지 2026-08-07, 서버가 HALF_DAY 로 축소 해석]';

-- 3) 구 파생 토글 컬럼 COMMENT 갱신 — ★ DROP 하지 않는다(LC-10 에서 이미 기록용으로 격하됨).
ALTER TABLE TB_LEAVE_USAGE_POLICY
    MODIFY COLUMN ALLOW_QUARTER char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci
    NOT NULL DEFAULT 'N'
    COMMENT '[폐지 2026-08-07] 반반차 허용 파생 기록(USAGE_UNIT=QUARTER_DAY 에서 파생되던 값). 판정에 사용하지 않음';

-- 4) (있을 경우) 잔존 반반차 설정 축소 정규화.
--    선행 조건이 0 이면 아래 두 문장은 0행을 갱신한다(멱등). 0 이 아니면 실행 전 보고할 것.
--    ★ sec L-2: 아래 UPDATE 의 WHERE 는 위 사전조건 SELECT 와 동일 술어(무필터)여야 한다.
--
--    ★★ Workbench 실행 시 Error 1175 (safe update mode) — 2026-08-07 실제 발생.
--       WHERE 의 USAGE_UNIT / USE_UNIT_TYPE 이 KEY 컬럼이 아니라서 클라이언트가 막는 것이고,
--       SQL 자체는 정상이다(위 1) TB_SYST_VAL_D 는 PK 술어라 통과한다).
--       ▶ 선행 조건이 0 이면 **이 두 문장은 건너뛴다**(0행 갱신이라 실행 실익이 없다). ← 권장
--       ▶ 잔존 데이터가 있어 실제로 실행해야 하면 세션 토글로만 우회한다.
--         Preferences 영구 해제 금지(메모리 feedback_mysql_workbench_safe_updates_composite_pk):
--           SET SQL_SAFE_UPDATES = 0;   -- 아래 두 UPDATE 실행
--           SET SQL_SAFE_UPDATES = 1;   -- 반드시 원복
UPDATE TB_LEAVE_USAGE_POLICY
   SET USAGE_UNIT    = 'HALF_DAY'
     , ALLOW_QUARTER = 'N'
     , UPDATE_NO     = 'SYSTEM'
     , UPDATE_DATE   = NOW()
 WHERE USAGE_UNIT = 'QUARTER_DAY';

UPDATE TB_LEAVE_TYPE_MGMT
   SET USE_UNIT_TYPE = '01'
     , UPDATE_NO     = 'SYSTEM'
     , UPDATE_DATE   = NOW()
 WHERE USE_UNIT_TYPE = '05';

-- 5) 검증(실행 후 전부 0 이어야 한다).
-- SELECT COUNT(*) AS policy_quarter FROM TB_LEAVE_USAGE_POLICY WHERE USAGE_UNIT = 'QUARTER_DAY';
-- SELECT COUNT(*) AS type_quarter   FROM TB_LEAVE_TYPE_MGMT    WHERE USE_UNIT_TYPE = '05';
-- SELECT USE_YN FROM TB_SYST_VAL_D WHERE SYST_VAL_CD = 'SYS025' AND SYST_VAL_D_CD = '05'; -- 'N' 이어야 함
