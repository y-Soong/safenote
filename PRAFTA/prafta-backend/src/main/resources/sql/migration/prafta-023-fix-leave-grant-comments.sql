-- ============================================================================
-- prafta-023: TB_USER_LEAVE_GRANT 컬럼 COMMENT 정정
--
-- 규칙: 코드성 컬럼의 COMMENT는 "설명[SYS코드] 코드값:의미 / ..." 형식을 따른다.
--
-- 발견(테스트 중): GRANT_BY_TYPE 은 실제로 SYS043 코드값(01/02)을 저장하는데,
--   COMMENT에 [SYS043]·코드값이 누락되고 'AUTO/ADMIN' 라벨로만 적혀 있었음 → 규칙 위반, 정정.
--   (근거: LeaveGrantEngineServiceImpl.GRANT_BY_TYPE_AUTO="01"(자동),
--          LeaveDashboardServiceImpl.GRANT_BY_TYPE_ADMIN="02"(관리자 수동) / SYS043)
--
-- 컬럼 정의(타입/NULL/기본값/charset)는 변경하지 않고 COMMENT만 정정한다(MODIFY는 정의 전체를 재지정하므로
--   기존 정의를 그대로 복기). 로컬/개발 DB에서 실행. 운영 적용은 백업 후.
-- ============================================================================

-- (정정) GRANT_BY_TYPE: SYS043 코드값(01/02) 명시
ALTER TABLE `tb_user_leave_grant`
  MODIFY COLUMN `GRANT_BY_TYPE` varchar(2) COLLATE utf8mb4_unicode_ci NOT NULL
    COMMENT '부여 방식[SYS043] 01:자동(AUTO) / 02:관리자 수동(ADMIN)';

-- (멱등 재확인) 아래 두 코드성 컬럼은 스냅샷상 이미 정상이나, 운영 DB가 구버전 COMMENT일 수 있어
--   캐노니컬 COMMENT로 재지정한다. 이미 정상이면 무변경(동일 COMMENT 재적용).
ALTER TABLE `tb_user_leave_grant`
  MODIFY COLUMN `GRANT_TYPE` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL
    COMMENT '부여 분류[SYS035] STATUTORY_*/MANUAL_* prefix (법정/약정 구분)';

ALTER TABLE `tb_user_leave_grant`
  MODIFY COLUMN `STATUS` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'ACTIVE'
    COMMENT '상태[SYS040] ACTIVE/EXHAUSTED/EXPIRED/CANCELED (EXPIRE_YN과 동기화 — 정책서 §8.5.8)';

-- 확인용:
--   SHOW FULL COLUMNS FROM tb_user_leave_grant WHERE Field IN ('GRANT_BY_TYPE','GRANT_TYPE','STATUS');
