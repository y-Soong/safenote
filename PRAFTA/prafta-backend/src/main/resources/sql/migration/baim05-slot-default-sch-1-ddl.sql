-- ============================================================
-- Baim_05 계정슬롯 개선 (2026-08-30)
--  1) 슬롯별 기본 근무타입(DEFAULT_SCH_CD) 컬럼 신설
--     - 관리자(Baim_05)가 슬롯에 기본 근무타입을 미리 지정하고,
--       점유 시(QR 발급/직접가입 승인 후 첫 로그인) TB_USER.DEFAULT_SCH_CD 로 복사한다.
--     - NULL 이면 종전 동작(근로자 본인이 로그인 게이트에서 선택) 유지.
--  2) CURR_USER_CD 빈문자열('') 정규화
--     - 링크정책 저장(saveDailyUserSlot)이 신규 슬롯에 CURR_USER_CD='' 를 적재해 와서,
--       활성 계정 수 축소 가드(selectOccupiedSlotCntOverLimit)가 미점유 슬롯을
--       점유중으로 오판(BAIM_400_008 오차단)하던 결함의 데이터 보정.
--     - 코드도 NULL 적재 + '' 허용 판정으로 동시 수정됨.
--  ★ 개발/운영 DB 모두 적용할 것 (Workbench 수동 실행).
-- ============================================================

-- 1) 슬롯 기본 근무타입 컬럼 신설
ALTER TABLE TB_DAILY_USER_SLOT
    ADD COLUMN DEFAULT_SCH_CD varchar(20) NULL
        COMMENT '슬롯 기본 근무타입(tb_sch_mgmt.SCH_CD 참조, 점유 시 TB_USER.DEFAULT_SCH_CD 로 복사, NULL=근로자 본인 선택)'
        AFTER NODE_CD;

-- 2) 미점유 슬롯 CURR_USER_CD 빈문자열 → NULL 정규화
--    (Workbench safe updates 모드에서는 키 조건이 아니라 1175 발생 → 세션 토글로 우회)
SET SQL_SAFE_UPDATES = 0;
UPDATE TB_DAILY_USER_SLOT
SET CURR_USER_CD = NULL
WHERE CURR_USER_CD = '';
SET SQL_SAFE_UPDATES = 1;

-- 검증: 빈문자열 잔존 0건이어야 함
SELECT COUNT(*) AS remain_empty_string
FROM TB_DAILY_USER_SLOT
WHERE CURR_USER_CD = '';
