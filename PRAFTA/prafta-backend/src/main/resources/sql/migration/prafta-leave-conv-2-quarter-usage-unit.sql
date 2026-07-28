-- ============================================================================
-- prafta-leave-conv-2 : 반반차(0.25일)를 "사용 단위 선택지"로 편입 (LC-10)
--
-- 배경
--   기존(LC-06)에는 반반차가 USAGE_UNIT 계층과 독립인 회사 토글(ALLOW_QUARTER)이었다.
--   그 결과 USAGE_UNIT='FULL_DAY' + ALLOW_QUARTER='Y' 조합에서
--   "종일과 반반차는 되는데 그 사이 반차는 안 되는" 허용집합 [00,05] 이 만들어졌고
--   (LeaveUnitGranularity.withQuarter 가 '01' 부재 시 맨 뒤에 삽입), 이를 막는 검증도 없었다.
--   또한 USAGE_UNIT='HOUR_2' 이하면 시간차 2시간(=8시간 기준 0.25일)이 이미 열리므로
--   반반차 토글은 기능적으로 중복이었다.
--
-- 변경
--   USAGE_UNIT 에 'QUARTER_DAY' 값을 추가한다(비법정 타입의 USE_UNIT_TYPE='05' 모델과 동일).
--   허용집합은 [00,01,05] = 종일/반차/반반차이며 시간차(02~04)는 열리지 않는다.
--   ALLOW_QUARTER 컬럼은 삭제하지 않고 남기되, 더 이상 입력값이 아니라
--   USAGE_UNIT='QUARTER_DAY' 에서 파생된 기록용 값으로만 쓴다(구 이력 스냅샷 비교 연속성).
--
-- 대상: 개발 DB + 운영 DB 양쪽 모두 적용 (한쪽만 적용 금지)
-- 선행: prafta-leave-conv-1-ddl.sql (SYS025 '05' 반반차 코드 + ALLOW_QUARTER 컬럼)
-- 재실행 안전: 모든 문장이 멱등(idempotent)
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1) USAGE_UNIT 컬럼 COMMENT 갱신
--    - 'QUARTER_DAY' 를 열거값에 추가한다.
--    - 기존 COMMENT 는 한글이 깨진 상태(mojibake)로 저장되어 있어 함께 정정한다.
--    - 컬럼 정의(varchar(20) NOT NULL DEFAULT 'FULL_DAY')는 변경하지 않는다.
--      'QUARTER_DAY' = 11자로 varchar(20) 안에 들어간다.
-- ----------------------------------------------------------------------------
ALTER TABLE tb_leave_usage_policy
    MODIFY COLUMN USAGE_UNIT varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'FULL_DAY'
    COMMENT '회사 허용 사용 단위(단일): FULL_DAY/HALF_DAY/QUARTER_DAY/HOUR_2/HOUR_1/MIN_30. QUARTER_DAY=반반차(허용집합 종일/반차/반반차, 시간차 미포함)';

-- ----------------------------------------------------------------------------
-- 2) ALLOW_QUARTER 컬럼 COMMENT 갱신 (폐기 표시)
--    입력 컬럼이 아님을 스키마에 명시한다. 컬럼 자체는 유지(구 이력 비교용).
-- ----------------------------------------------------------------------------
ALTER TABLE tb_leave_usage_policy
    MODIFY COLUMN ALLOW_QUARTER char(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'N'
    COMMENT '[LC-10 폐기] 반반차 허용 Y/N. 입력값 아님 — USAGE_UNIT=QUARTER_DAY 파생값으로만 기록됨';

-- ----------------------------------------------------------------------------
-- 3) 기존 데이터 정리 — ALLOW_QUARTER='Y' 행의 파생값 정합화
--
--    적용 전 실측(개발 DB, 2026-07-28):
--      ALLOW_QUARTER='Y' 6행 = 전부 회사 001(테스트)의 정책 버전이며 USAGE_UNIT='MIN_30'.
--      MIN_30 은 시간차 2시간(=0.25일)이 이미 열려 있으므로 'N' 으로 내려도 기능 손실이 없다.
--
--    ★ 주의: USAGE_UNIT 은 건드리지 않는다.
--      'Y' 행을 QUARTER_DAY 로 바꾸면 그 회사의 시간차(02~04)가 닫혀 회귀가 된다.
--      반반차를 실제로 쓰려는 회사는 화면(Baim_07)에서 [0.25일 (반반차)] 를 명시 선택한다.
--
--    운영 DB 적용 전 아래 확인 쿼리로 'Y' 행 분포를 반드시 재확인할 것.
--      SELECT CMPNY_CD, POLICY_SEQ, USAGE_UNIT
--        FROM tb_leave_usage_policy WHERE ALLOW_QUARTER = 'Y' ORDER BY CMPNY_CD, POLICY_SEQ;
--    'Y' 행 중 USAGE_UNIT 이 FULL_DAY/HALF_DAY 인 회사가 있다면(= 결함 조합 [00,05] 사용 중)
--    아래 UPDATE 전에 해당 회사에 반반차 유지 여부를 확인하고,
--    유지해야 하면 그 회사만 USAGE_UNIT='QUARTER_DAY' 로 별도 전환할 것.
--
--    ★ Workbench 실행 시 1175 (safe update mode)
--      WHERE 조건이 ALLOW_QUARTER / USAGE_UNIT 뿐이라 키 컬럼(PK=POLICY_SEQ)을 쓰지 않는다.
--      아래처럼 세션 한정으로만 해제하고 원복한다.
--      Preferences 에서 영구 해제하지 말 것(다른 작업에서 안전망이 사라진다).
-- ----------------------------------------------------------------------------
SET @old_safe_updates := @@SESSION.sql_safe_updates;
SET SESSION sql_safe_updates = 0;

UPDATE tb_leave_usage_policy
   SET ALLOW_QUARTER = 'N'
     , UPDATE_NO     = 'SYSTEM'
     , UPDATE_DATE   = NOW()
 WHERE ALLOW_QUARTER = 'Y'
   AND USAGE_UNIT   <> 'QUARTER_DAY';

-- 원복(이 세션에 한함). 재연결해도 Preferences 설정으로 되돌아온다.
SET SESSION sql_safe_updates = @old_safe_updates;

-- ----------------------------------------------------------------------------
-- 적용 후 확인 쿼리 (수동)
-- ----------------------------------------------------------------------------
-- SELECT COLUMN_NAME, COLUMN_TYPE, COLUMN_DEFAULT, COLUMN_COMMENT
--   FROM INFORMATION_SCHEMA.COLUMNS
--  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_leave_usage_policy'
--    AND COLUMN_NAME IN ('USAGE_UNIT','ALLOW_QUARTER');
--   → USAGE_UNIT COMMENT 에 QUARTER_DAY 포함, ALLOW_QUARTER COMMENT 에 [LC-10 폐기] 표시
--
-- SELECT USAGE_UNIT, ALLOW_QUARTER, COUNT(*) FROM tb_leave_usage_policy
--  GROUP BY USAGE_UNIT, ALLOW_QUARTER;
--   → ALLOW_QUARTER='Y' 는 USAGE_UNIT='QUARTER_DAY' 행에만 남아야 한다(현재는 0행)

-- ============================================================================
-- 롤백 (필요 시 수동 실행)
--   코드 롤백과 함께 수행해야 한다. 데이터는 되돌리지 않는다
--   (기존 'Y' 행은 MIN_30 이라 'N' 으로 내려도 기능 동일 — 3) 주석 참조).
-- ----------------------------------------------------------------------------
-- ALTER TABLE tb_leave_usage_policy
--     MODIFY COLUMN USAGE_UNIT varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'FULL_DAY'
--     COMMENT '회사 허용 사용 단위(단일): FULL_DAY/HALF_DAY/HOUR_2/HOUR_1/MIN_30';
-- ALTER TABLE tb_leave_usage_policy
--     MODIFY COLUMN ALLOW_QUARTER char(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'N'
--     COMMENT '반반차(0.25일) 허용 Y/N (SYS025-05)';
-- ============================================================================
