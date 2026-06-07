-- =====================================================================
-- prafta-051 : TBM 교육 세션 상태머신 재설계 (작업 051-01)
-- ---------------------------------------------------------------------
-- 개설(DRAFT) → 교육준비(OPENED) → 교육시작(IN_PROGRESS) → 교육종료(COMPLETED)
-- 흐름 도입에 필요한 컬럼/코드/COMMENT 변경.
--
-- - 신규 코드값 추가 없음(SYS046 라벨만 변경). SYS051 에 MANAGER_DIRECT 1건 추가.
-- - 운영 선적용 필수(애플리케이션 배포 전 적용). 1회성 마이그레이션.
-- - MySQL 8.0.42 기준. ADD COLUMN 은 IF NOT EXISTS 미지원 → 1회 실행 전제.
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1) TB_TBM_SESSION : 교육준비 타이머 기준시각 컬럼 추가
--    OPENED 전이 시 NOW(), 수동연장 시 NOW() 로 리셋. 15분 자동 교육시작 기준.
--    OPENED_AT(최초 개설/준비시각)은 감사기록으로 보존(덮어쓰기 금지).
-- ---------------------------------------------------------------------
ALTER TABLE TB_TBM_SESSION
    ADD COLUMN PREP_START_AT datetime DEFAULT NULL
        COMMENT '교육준비 타이머 기준시각(15분 자동 교육시작 기준, 수동연장 시 NOW() 리셋)'
        AFTER OPENED_AT;

-- ---------------------------------------------------------------------
-- 2) TB_TBM_ATTENDANCE : 앱 포그라운드 누적초 컬럼 추가
--    SELF_DEVICE 본인앱 입실자가 '종료' 시 1회 전송하는 누적 포그라운드 시간(초).
--    대리입실/검색입실(MANAGER_DIRECT)·일용직은 사용자 앱이 없으므로 NULL.
-- ---------------------------------------------------------------------
ALTER TABLE TB_TBM_ATTENDANCE
    ADD COLUMN APP_FOREGROUND_SEC int DEFAULT NULL
        COMMENT '앱 포그라운드 누적초(SELF_DEVICE 종료 시 1회 수신, 대리/검색입실 NULL)'
        AFTER EXIT_AT;

-- ---------------------------------------------------------------------
-- 3) COMMENT 갱신 (코드성 컬럼: 설명[SYS코드] 코드값:의미 규칙)
-- ---------------------------------------------------------------------
-- 3-1) 세션상태 라벨 변경 반영
ALTER TABLE TB_TBM_SESSION
    MODIFY COLUMN STATUS_CD varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'DRAFT'
        COMMENT '세션상태[SYS046] DRAFT:개설 OPENED:교육준비 IN_PROGRESS:교육시작 COMPLETED:교육종료 CANCELLED:취소';

-- 3-2) 입실/종료 비밀번호 발급 시점 변경 반영
ALTER TABLE TB_TBM_SESSION
    MODIFY COLUMN ENTRY_PWD varchar(10) COLLATE utf8mb4_unicode_ci DEFAULT NULL
        COMMENT '입실 비밀번호(랜덤6자리, 교육준비(OPENED) 전이 시 발급)';

ALTER TABLE TB_TBM_SESSION
    MODIFY COLUMN EXIT_PWD varchar(10) COLLATE utf8mb4_unicode_ci DEFAULT NULL
        COMMENT '종료 비밀번호(랜덤6자리, 교육종료(COMPLETED) 전이 시 발급, 입실≠종료)';

-- 3-3) 입실경로에 MANAGER_DIRECT(웹 검색 대리/일용직 입실) 추가
ALTER TABLE TB_TBM_ATTENDANCE
    MODIFY COLUMN ENTRY_TYPE_CD varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL
        COMMENT '입실경로[SYS051] SELF_DEVICE:본인디바이스 MANAGER_QR_SCAN:관리자QR스캔 MANAGER_DIRECT:관리자직접입실(웹검색)';

-- ---------------------------------------------------------------------
-- 4) SYS046 세션상태 표시 라벨 변경 (코드값 불변, 라벨만)
--    작성중→개설, 개설→교육준비, 진행중→교육시작, 종료→교육종료, 취소(불변)
-- ---------------------------------------------------------------------
UPDATE TB_SYST_VAL_D SET SYST_VAL_D_NM = '개설',    UPDATE_NO = 'SYSTEM', UPDATE_DATE = NOW()
 WHERE SYST_VAL_CD = 'SYS046' AND SYST_VAL_D_CD = 'DRAFT';
UPDATE TB_SYST_VAL_D SET SYST_VAL_D_NM = '교육준비', UPDATE_NO = 'SYSTEM', UPDATE_DATE = NOW()
 WHERE SYST_VAL_CD = 'SYS046' AND SYST_VAL_D_CD = 'OPENED';
UPDATE TB_SYST_VAL_D SET SYST_VAL_D_NM = '교육시작', UPDATE_NO = 'SYSTEM', UPDATE_DATE = NOW()
 WHERE SYST_VAL_CD = 'SYS046' AND SYST_VAL_D_CD = 'IN_PROGRESS';
UPDATE TB_SYST_VAL_D SET SYST_VAL_D_NM = '교육종료', UPDATE_NO = 'SYSTEM', UPDATE_DATE = NOW()
 WHERE SYST_VAL_CD = 'SYS046' AND SYST_VAL_D_CD = 'COMPLETED';

-- ---------------------------------------------------------------------
-- 5) SYS051 입실경로에 MANAGER_DIRECT 추가 (재실행 안전: ON DUPLICATE KEY)
-- ---------------------------------------------------------------------
INSERT INTO TB_SYST_VAL_D
    (SYST_VAL_CD, SYST_VAL_D_CD, SYST_VAL_D_NM, SORT_IDX, USE_YN, INSERT_NO, INSERT_DATE, UPDATE_NO, UPDATE_DATE)
VALUES
    ('SYS051', 'MANAGER_DIRECT', '관리자직접입실', 3, 'Y', 'SYSTEM', NOW(), 'SYSTEM', NOW())
ON DUPLICATE KEY UPDATE
    SYST_VAL_D_NM = VALUES(SYST_VAL_D_NM),
    SORT_IDX      = VALUES(SORT_IDX),
    USE_YN        = VALUES(USE_YN),
    UPDATE_NO     = 'SYSTEM',
    UPDATE_DATE   = NOW();
