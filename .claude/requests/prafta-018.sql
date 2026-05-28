-- ============================================================================
-- PRAFTA-018 단계 1 DDL — 법정 연차 부여 정책 도메인 설계
-- 작성일: 2026-05-20
-- 참조 정책서: .claude/context/policies/attd/08-leave.md §8.5
-- 적용 순서: 본 파일을 위에서 아래로 순차 실행. 트랜잭션 단위는 섹션별 권장.
--
-- 결정사항 요약 (정책서 §8.5 + 사용자 결정):
--   - 7개 axis 통합본 채택(작업지시서 §5 기반, §1.3.3 9개 axis 폐기)
--   - 프리셋 4번 '입사일 일괄선부여'는 AXIS3_PREGRANT_YN 보조 플래그로 표현
--   - tb_user_leave_grant PK는 기존 GRANT_ID(varchar(20)) 유지 (GRANT_SEQ 도입 X)
--   - STATUS 컬럼 신설 + EXPIRE_YN/DEL_YN과 동기화. EXPIRE_YN deprecation은 단계 2
--   - USED_DAYS 캐시 컬럼 도입 (대시보드 성능)
--   - 시스템 시드 식별은 tb_leave_type_mgmt.SYSTEM_YN 신규 컬럼
--   - 신규 SYS코드는 SYS035~SYS042 (현재 최대 SYS034 다음부터)
--   - 시스템 LEAVE_CD 시드 회사별 INSERT는 단계 2로 분리
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 섹션 1. TB_USER 컬럼 추가 (입사일/고용형태/계약종료일)
-- 위치: BIRTH_DT_ENC 다음에 추가
-- 주의: LEAVE_GROUP_CD/WORK_TYPE_CD는 v2에서 제거되어 추가하지 않음
-- ----------------------------------------------------------------------------
ALTER TABLE `tb_user`
  ADD COLUMN `HIRE_DATE` varchar(8) COLLATE utf8mb4_unicode_ci DEFAULT NULL
    COMMENT '입사일 (YYYYMMDD) — 연차 부여 기준' AFTER `BIRTH_DT_ENC`
, ADD COLUMN `EMPLOYMENT_TYPE` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL
    COMMENT '고용형태[SYS041] REGULAR/CONTRACT/DAILY/EXECUTIVE' AFTER `HIRE_DATE`
, ADD COLUMN `CONTRACT_END_DATE` varchar(8) COLLATE utf8mb4_unicode_ci DEFAULT NULL
    COMMENT '계약 종료일 (YYYYMMDD, EMPLOYMENT_TYPE=CONTRACT일 때 필수)' AFTER `EMPLOYMENT_TYPE`
, ADD INDEX `IX_TB_USER_CONTRACT` (`CMPNY_CD`,`CONTRACT_END_DATE`);


-- ----------------------------------------------------------------------------
-- 섹션 2. tb_leave_type_mgmt — SYSTEM_YN 컬럼 추가 (시스템 시드 식별)
-- ----------------------------------------------------------------------------
ALTER TABLE `tb_leave_type_mgmt`
  ADD COLUMN `SYSTEM_YN` char(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'N'
    COMMENT '시스템 시드 여부 (Y: PRAFTA-018 법정 연차용, 화면 편집 불가)' AFTER `USE_YN`
, ADD INDEX `IX_TB_LEAVE_TYPE_MGMT_SYSTEM` (`CMPNY_CD`,`SYSTEM_YN`,`USE_YN`);


-- ----------------------------------------------------------------------------
-- 섹션 3. tb_user_leave_grant — 신규 컬럼 추가
--   기존: GRANT_ID(varchar PK), GRANT_BY_TYPE(AUTO/ADMIN), EXPIRE_YN, DEL_YN 유지
--   신규: GRANT_TYPE(STATUTORY_*/MANUAL_* prefix), USED_DAYS 캐시,
--         POLICY_SEQ, IDEMPOTENCY_KEY(UNIQUE), STATUS
-- ----------------------------------------------------------------------------
ALTER TABLE `tb_user_leave_grant`
  ADD COLUMN `GRANT_TYPE` varchar(40) COLLATE utf8mb4_unicode_ci DEFAULT NULL
    COMMENT '부여 분류[SYS035] STATUTORY_*/MANUAL_* prefix (법정/약정 구분)' AFTER `LEAVE_CD`
, ADD COLUMN `USED_DAYS` decimal(5,1) NOT NULL DEFAULT 0.0
    COMMENT '사용 일수 캐시 (tb_user_leave_use 합계와 동기화)' AFTER `GRANT_DAYS`
, ADD COLUMN `POLICY_SEQ` bigint DEFAULT NULL
    COMMENT '적용 정책 (TB_LEAVE_POLICY.POLICY_SEQ, 수동 부여는 NULL)' AFTER `GRANT_BY_TYPE`
, ADD COLUMN `IDEMPOTENCY_KEY` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL
    COMMENT '중복 부여 방지 키 ({USER_CD}_{YYYY}_ANNUAL 등). 자동부여 시 필수' AFTER `AVAIL_TO_DATE`
, ADD COLUMN `STATUS` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'ACTIVE'
    COMMENT '상태[SYS040] ACTIVE/EXHAUSTED/EXPIRED/CANCELED (EXPIRE_YN과 동기화 — 정책서 §8.5.8)' AFTER `IDEMPOTENCY_KEY`
, ADD UNIQUE KEY `UK_LEAVE_GRANT_IDEMPOTENCY` (`CMPNY_CD`,`IDEMPOTENCY_KEY`)
, ADD INDEX `IX_LEAVE_GRANT_STATUS` (`CMPNY_CD`,`USER_CD`,`STATUS`,`AVAIL_TO_DATE`)
, ADD INDEX `IX_LEAVE_GRANT_GTYPE` (`CMPNY_CD`,`GRANT_TYPE`,`GRANT_DATE`);

-- 주의: IDEMPOTENCY_KEY는 NULL 허용 (기존 데이터 호환). 자동부여 신규 INSERT에서만 필수.
--       MySQL UNIQUE KEY는 NULL 중복 허용하므로 정합성 OK.


-- ----------------------------------------------------------------------------
-- 섹션 4. tb_user_leave_grant — STATUS 보정 UPDATE
-- 기존 row의 EXPIRE_YN/DEL_YN 상태를 신규 STATUS 컬럼에 반영
-- 순서: DEL_YN 우선 (CANCELED), 그다음 EXPIRE_YN (EXPIRED), 나머지는 DEFAULT 'ACTIVE' 유지
-- ----------------------------------------------------------------------------
UPDATE `tb_user_leave_grant`
   SET `STATUS` = 'CANCELED'
 WHERE `DEL_YN` = 'Y';

UPDATE `tb_user_leave_grant`
   SET `STATUS` = 'EXPIRED'
 WHERE `EXPIRE_YN` = 'Y'
   AND `DEL_YN` = 'N';

-- 'EXHAUSTED' 상태는 USED_DAYS=GRANT_DAYS 도달 시 부여되어야 하나, USED_DAYS 자체가
-- 신규 컬럼이라 단계 1에서는 ACTIVE로만 분류. 단계 2 서비스 레이어에서 트랜잭션 동기화.


-- ----------------------------------------------------------------------------
-- 섹션 5. tb_user_leave_use — 변경 없음
-- 현재 LEAVE_CD/GRANT_ID/USE_UNIT_TYPE(SYS025)/LEAVE_DAYS/LEAVE_MINUTES로 충분
-- 정책서 §8.5에서 신규 컬럼 요구 없음
-- ----------------------------------------------------------------------------


-- ----------------------------------------------------------------------------
-- 섹션 6. 신규 테이블 5종
-- ----------------------------------------------------------------------------

-- 6.1 사용자 경력 인정 (점진 부여 전용)
CREATE TABLE `TB_USER_SERVICE_CREDIT` (
    `CREDIT_ID` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '경력 인정 ID (PK)'
  , `CMPNY_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '회사 코드'
  , `USER_CD` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사용자 코드'
  , `CREDIT_MONTHS` int NOT NULL COMMENT '인정 개월 수 (0 이상)'
  , `REASON_TYPE` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사유 유형[SYS042]'
  , `REASON_DETAIL` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '상세 설명'
  , `USE_YN` char(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'Y' COMMENT '사용 여부'
  , `INSERT_NO` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '입력자'
  , `INSERT_DATE` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시'
  , `UPDATE_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '수정자'
  , `UPDATE_DATE` datetime DEFAULT NULL COMMENT '수정일시'
  , PRIMARY KEY (`CREDIT_ID`)
  , KEY `IX_TB_USER_SERVICE_CREDIT_USER` (`CMPNY_CD`,`USER_CD`,`USE_YN`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='사용자 경력 인정 (점진 부여 전용)';

-- 6.2 입사일 변경 이력 (노무 감사용)
CREATE TABLE `TB_USER_HIRE_DATE_HISTORY` (
    `HIST_ID` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '이력 ID (PK)'
  , `CMPNY_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '회사 코드'
  , `USER_CD` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사용자 코드'
  , `PREV_HIRE_DATE` varchar(8) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '변경 전 입사일'
  , `NEW_HIRE_DATE` varchar(8) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '변경 후 입사일'
  , `CHANGE_REASON` varchar(1000) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '변경 사유 (자유 텍스트, 필수)'
  , `HANDLING_TYPE` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '처리 방식[SYS039] KEEP_AND_BACKFILL/KEEP_AND_APPLY_NEW/RESET_ALL'
  , `AFFECTED_GRANT_SNAPSHOT` json DEFAULT NULL COMMENT '영향받은 부여 이력 스냅샷'
  , `INSERT_NO` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '변경자 (AUTH_MASTER 또는 AUTH_HR_MANAGER)'
  , `INSERT_DATE` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '변경일시'
  , PRIMARY KEY (`HIST_ID`)
  , KEY `IX_TB_HIRE_HIST_USER` (`CMPNY_CD`,`USER_CD`,`INSERT_DATE`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='입사일 변경 이력 (노무 감사용)';

-- 6.3 회사 법정 연차 부여 정책 (회사당 활성 1개, 7개 axis)
CREATE TABLE `TB_LEAVE_POLICY` (
    `POLICY_SEQ` bigint NOT NULL AUTO_INCREMENT COMMENT '정책 일련번호 (PK)'
  , `CMPNY_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '회사 코드'
  , `POLICY_PRESET` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '프리셋: HIRE_DATE/FISCAL_PRORATE/FISCAL_MONTHLY/HIRE_DATE_PREGRANT/CUSTOM'
  -- 7개 axis (정책서 §8.5.2)
  , `AXIS1_GRANT_BASE` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '1번: HIRE_DATE/FISCAL_YEAR [SYS036]'
  , `AXIS2_FISCAL_START_MM` char(2) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '2번: 회계연도 시작월 (01~12)'
  , `AXIS2_FISCAL_START_DD` char(2) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '2번: 회계연도 시작일 (01~31)'
  , `AXIS3_FIRST_YEAR_METHOD` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '3번: MONTHLY_ONLY/PRORATE/NEXT_YEAR_BULK [SYS037]'
  , `AXIS3_PREGRANT_YN` char(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'N' COMMENT '3번 보조: 입사일 일괄선부여 여부 (프리셋 4번 표현)'
  , `AXIS4_PRORATE_ROUNDING` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'CEIL' COMMENT '4번: CEIL/ROUND/FLOOR/HALF_DAY [SYS038] (AXIS3=PRORATE 시만 유효, 그 외는 CEIL 강제)'
  , `AXIS5_TENURE_MODE` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'LEGAL' COMMENT '5번: LEGAL/CUSTOM'
  , `AXIS5_START_YEAR` int NOT NULL DEFAULT 3 COMMENT '5번: 가산 시작 연차 (1~3, LEGAL 시 3 강제)'
  , `AXIS5_INTERVAL` int NOT NULL DEFAULT 2 COMMENT '5번: 가산 주기 (1~2, LEGAL 시 2 강제)'
  , `AXIS5_MAX_DAYS` int NOT NULL DEFAULT 25 COMMENT '5번: 최대 연차일수 (25 이상, 법정)'
  , `AXIS6_VALIDITY_MONTHS` int NOT NULL DEFAULT 12 COMMENT '6번: 유효기간(개월) 12 또는 24'
  , `AXIS7_USE_PROMOTION` char(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'N' COMMENT '7번: 사용촉진 Y/N'
  -- 활성 관리
  , `USE_YN` char(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'Y' COMMENT '활성 여부 (회사당 Y는 1건, 서비스 레이어에서 보장)'
  , `APPLY_FROM_DATE` varchar(8) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '정책 적용 시작일 (YYYYMMDD)'
  , `INSERT_NO` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL
  , `INSERT_DATE` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP
  , `UPDATE_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL
  , `UPDATE_DATE` datetime DEFAULT NULL
  , PRIMARY KEY (`POLICY_SEQ`)
  , KEY `IX_TB_LEAVE_POLICY_ACTIVE` (`CMPNY_CD`,`USE_YN`,`APPLY_FROM_DATE`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='회사 법정 연차 부여 정책 (7개 axis)';

-- 6.4 정책 변경 이력 (스냅샷)
CREATE TABLE `TB_LEAVE_POLICY_HISTORY` (
    `HIST_ID` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '이력 ID (PK)'
  , `CMPNY_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '회사 코드'
  , `POLICY_SEQ` bigint NOT NULL COMMENT '변경된 TB_LEAVE_POLICY.POLICY_SEQ'
  , `CHANGE_TYPE` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'CREATE/UPDATE/PRESET_CHANGE'
  , `PREV_SNAPSHOT` json DEFAULT NULL COMMENT '변경 전 정책 전체 스냅샷'
  , `NEW_SNAPSHOT` json NOT NULL COMMENT '변경 후 정책 전체 스냅샷'
  , `CHANGE_REASON` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL
  , `IMPACT_SUMMARY` json DEFAULT NULL COMMENT '영향 분석 결과 (영향 인원, 추가 부담)'
  , `INSERT_NO` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL
  , `INSERT_DATE` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP
  , PRIMARY KEY (`HIST_ID`)
  , KEY `IX_TB_LEAVE_POLICY_HIST` (`CMPNY_CD`,`INSERT_DATE`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='연차 정책 변경 이력';

-- 6.5 사용 단위 정책 (TB_LEAVE_POLICY와 1:1)
CREATE TABLE `TB_LEAVE_USAGE_POLICY` (
    `POLICY_SEQ` bigint NOT NULL COMMENT 'TB_LEAVE_POLICY.POLICY_SEQ 1:1'
  , `CMPNY_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '회사 코드'
  , `ALLOW_FULL_DAY` char(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'Y' COMMENT '1일 단위 (항상 Y, 변경불가)'
  , `ALLOW_HALF_DAY` char(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'Y' COMMENT '0.5일 단위 (AXIS4=HALF_DAY 시 Y 강제)'
  , `ALLOW_QUARTER_DAY` char(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'Y' COMMENT '0.25일 단위'
  , `ALLOW_HOURLY` char(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'N' COMMENT '0.125일(1시간) 단위'
  , `MAX_DAILY_REQUEST` int NOT NULL DEFAULT 3 COMMENT '같은 날 최대 신청 건수 (0=불허)'
  , `INSERT_NO` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL
  , `INSERT_DATE` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP
  , `UPDATE_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL
  , `UPDATE_DATE` datetime DEFAULT NULL
  , PRIMARY KEY (`POLICY_SEQ`)
  , CONSTRAINT `FK_TB_LEAVE_USAGE_POLICY` FOREIGN KEY (`POLICY_SEQ`) REFERENCES `TB_LEAVE_POLICY` (`POLICY_SEQ`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='연차 사용 단위 정책 (휴게시간/시간단위 시작시각/1일환산은 시스템 강제)';


-- ----------------------------------------------------------------------------
-- 섹션 7. TB_SYST_VAL_M / TB_SYST_VAL_D 신규 코드 INSERT
-- 현재 최대 SYS코드: SYS034 → 신규 할당은 SYS035~SYS042 (총 8개)
-- ----------------------------------------------------------------------------

-- 7.1 SYST_VAL_M 마스터 (8건)
INSERT INTO `tb_syst_val_m` (`SYST_VAL_CD`, `SYST_VAL_NM`, `USE_YN`, `INSERT_NO`, `INSERT_DATE`) VALUES
   ('SYS035', '연차 부여 분류',         'Y', 'SYSTEM', NOW())
 , ('SYS036', '연차 정책 AXIS1',         'Y', 'SYSTEM', NOW())
 , ('SYS037', '연차 정책 AXIS3',         'Y', 'SYSTEM', NOW())
 , ('SYS038', '연차 정책 AXIS4',         'Y', 'SYSTEM', NOW())
 , ('SYS039', '입사일 변경 처리 방식',   'Y', 'SYSTEM', NOW())
 , ('SYS040', '연차 부여 상태',           'Y', 'SYSTEM', NOW())
 , ('SYS041', '고용 형태',                'Y', 'SYSTEM', NOW())
 , ('SYS042', '경력 인정 사유',           'Y', 'SYSTEM', NOW());

-- 7.2 SYST_VAL_D 상세
-- 회사 단위 코드인 경우 CMPNY_CD를 함께 등록. 본 시드는 시스템 공통이므로 prafta 기존 패턴에 따라 등록.
-- (실제 INSERT 시 CMPNY_CD 컬럼 존재 여부 / DEFAULT 값은 schema-full.sql tb_syst_val_d 정의에 맞춰 보정 필요)

-- SYS035 GRANT_TYPE prefix
INSERT INTO `tb_syst_val_d` (`SYST_VAL_CD`, `SYST_VAL_D_CD`, `SYST_VAL_D_NM`, `USE_YN`, `INSERT_NO`, `INSERT_DATE`) VALUES
   ('SYS035', 'STATUTORY_ANNUAL',        '법정 본연차',          'Y', 'SYSTEM', NOW())
 , ('SYS035', 'STATUTORY_MONTHLY',       '법정 월차',            'Y', 'SYSTEM', NOW())
 , ('SYS035', 'STATUTORY_TENURE_BONUS',  '법정 근속 가산',       'Y', 'SYSTEM', NOW())
 , ('SYS035', 'MANUAL_BONUS',            '포상 휴가',            'Y', 'SYSTEM', NOW())
 , ('SYS035', 'MANUAL_CONDOLENCE',       '경조사 휴가',          'Y', 'SYSTEM', NOW())
 , ('SYS035', 'MANUAL_LONG_SERVICE',     '장기근속 휴가',        'Y', 'SYSTEM', NOW())
 , ('SYS035', 'MANUAL_OTHER',            '기타 약정 휴가',       'Y', 'SYSTEM', NOW());

-- SYS036 AXIS1 연차 부여 기준
INSERT INTO `tb_syst_val_d` (`SYST_VAL_CD`, `SYST_VAL_D_CD`, `SYST_VAL_D_NM`, `USE_YN`, `INSERT_NO`, `INSERT_DATE`) VALUES
   ('SYS036', 'HIRE_DATE',       '입사일 기준',     'Y', 'SYSTEM', NOW())
 , ('SYS036', 'FISCAL_YEAR',     '회계연도 기준',   'Y', 'SYSTEM', NOW());

-- SYS037 AXIS3 입사 첫해 처리 방식
INSERT INTO `tb_syst_val_d` (`SYST_VAL_CD`, `SYST_VAL_D_CD`, `SYST_VAL_D_NM`, `USE_YN`, `INSERT_NO`, `INSERT_DATE`) VALUES
   ('SYS037', 'MONTHLY_ONLY',    '월차만 부여',             'Y', 'SYSTEM', NOW())
 , ('SYS037', 'PRORATE',         '회계연도 시점 비례부여',  'Y', 'SYSTEM', NOW())
 , ('SYS037', 'NEXT_YEAR_BULK',  '차년도 시점 일괄부여',    'Y', 'SYSTEM', NOW());

-- SYS038 AXIS4 비례 부여 반올림
INSERT INTO `tb_syst_val_d` (`SYST_VAL_CD`, `SYST_VAL_D_CD`, `SYST_VAL_D_NM`, `USE_YN`, `INSERT_NO`, `INSERT_DATE`) VALUES
   ('SYS038', 'CEIL',            '올림 (근로자 유리)',     'Y', 'SYSTEM', NOW())
 , ('SYS038', 'ROUND',           '반올림 (표준)',          'Y', 'SYSTEM', NOW())
 , ('SYS038', 'FLOOR',           '내림 (회사 유리)',       'Y', 'SYSTEM', NOW())
 , ('SYS038', 'HALF_DAY',        '0.5일 단위 절사',        'Y', 'SYSTEM', NOW());

-- SYS039 HANDLING_TYPE 입사일 변경 처리 방식
INSERT INTO `tb_syst_val_d` (`SYST_VAL_CD`, `SYST_VAL_D_CD`, `SYST_VAL_D_NM`, `USE_YN`, `INSERT_NO`, `INSERT_DATE`) VALUES
   ('SYS039', 'KEEP_AND_BACKFILL',    '기존 유지 + 누락 소급',   'Y', 'SYSTEM', NOW())
 , ('SYS039', 'KEEP_AND_APPLY_NEW',   '기존 유지 + 신규만 적용', 'Y', 'SYSTEM', NOW())
 , ('SYS039', 'RESET_ALL',            '전체 재계산 (위험)',      'Y', 'SYSTEM', NOW());

-- SYS040 STATUS 연차 부여 상태
INSERT INTO `tb_syst_val_d` (`SYST_VAL_CD`, `SYST_VAL_D_CD`, `SYST_VAL_D_NM`, `USE_YN`, `INSERT_NO`, `INSERT_DATE`) VALUES
   ('SYS040', 'ACTIVE',          '사용중',     'Y', 'SYSTEM', NOW())
 , ('SYS040', 'EXHAUSTED',       '소진완료',   'Y', 'SYSTEM', NOW())
 , ('SYS040', 'EXPIRED',         '만료',       'Y', 'SYSTEM', NOW())
 , ('SYS040', 'CANCELED',        '취소됨',     'Y', 'SYSTEM', NOW());

-- SYS041 EMPLOYMENT_TYPE 고용 형태
INSERT INTO `tb_syst_val_d` (`SYST_VAL_CD`, `SYST_VAL_D_CD`, `SYST_VAL_D_NM`, `USE_YN`, `INSERT_NO`, `INSERT_DATE`) VALUES
   ('SYS041', 'REGULAR',         '정규직',     'Y', 'SYSTEM', NOW())
 , ('SYS041', 'CONTRACT',        '계약직',     'Y', 'SYSTEM', NOW())
 , ('SYS041', 'DAILY',           '일용직',     'Y', 'SYSTEM', NOW())
 , ('SYS041', 'EXECUTIVE',       '임원',       'Y', 'SYSTEM', NOW());

-- SYS042 SERVICE_CREDIT_REASON 경력 인정 사유
INSERT INTO `tb_syst_val_d` (`SYST_VAL_CD`, `SYST_VAL_D_CD`, `SYST_VAL_D_NM`, `USE_YN`, `INSERT_NO`, `INSERT_DATE`) VALUES
   ('SYS042', 'MA_TRANSFER',          'M&A 고용승계',          'Y', 'SYSTEM', NOW())
 , ('SYS042', 'EXPERIENCE_SAME',      '동종업계 경력 인정',     'Y', 'SYSTEM', NOW())
 , ('SYS042', 'EXPERIENCE_DIFF',      '이종업계 경력 인정',     'Y', 'SYSTEM', NOW())
 , ('SYS042', 'CONTRACT_TO_REGULAR',  '계약→정규 전환',         'Y', 'SYSTEM', NOW())
 , ('SYS042', 'GROUP_MOVE',           '그룹사 이동',           'Y', 'SYSTEM', NOW())
 , ('SYS042', 'OTHER',                '기타',                  'Y', 'SYSTEM', NOW());


-- ----------------------------------------------------------------------------
-- 섹션 8. 시스템 LEAVE_CD 시드 (tb_leave_type_mgmt) — 단계 2로 분리
-- ----------------------------------------------------------------------------
-- 정책서 §8.5.5에 정의된 6종 시스템 시드(SYS_ANNUAL/SYS_MONTHLY/SYS_TENURE_BONUS/
-- SYS_PROMOTION/SYS_PREGRANT/SYS_BIRTHDAY)는 회사별로 INSERT되어야 한다.
-- 본 단계 1에서는 SYSTEM_YN 컬럼만 추가하고, 실제 시드 INSERT는 단계 2의 회사
-- 생성 트리거 또는 일회성 백필 스크립트로 처리한다.
--
-- 시드 INSERT 시 사용할 코드값(쿼리 결과로 확정):
--   LEAVE_TYPE='02'         (관리자 부여)
--   GRANT_TYPE='01'         (자동부여)
--   PAID_TYPE='01'          (유급)
--   LEAVE_NATURE_TYPE='01'  (법정) — SYS_BIRTHDAY만 '02'(특별)
--   SYSTEM_YN='Y'
--   USE_YN='Y'
--
-- ※ 정책서/외부 문서 일부에 "약정"으로 표기된 부분이 있으나, 실제 DB SYS024는
--   '01=법정 / 02=특별'이다. 단계 2에서 표기 정합성 일괄 점검.


-- ============================================================================
-- 끝.
-- 후속 단계 2에서 다룰 항목:
--   - EXPIRE_YN ↔ STATUS 동기화 서비스/배치 로직 + EXPIRE_YN deprecation 계획
--   - 시스템 LEAVE_CD 시드 회사별 INSERT (백필 스크립트 + 회사 생성 트리거)
--   - 활성 정책 1개 보장 트랜잭션 로직
--   - tb_leave_type_mgmt SYS022/023/024 매핑이 §8.5.5 가정과 일치하는지 단계 2 시드 작성 시 재검증
--   - SYS024 "약정/특별" 표기 정합성 일괄 점검
--   - schema-full.sql 스냅샷 갱신 (mysqldump 재실행)
-- ============================================================================
