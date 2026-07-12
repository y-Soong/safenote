-- ============================================================
-- 순회점검 문항관리: 시행월(YYYYMM) → 시행일(YYYYMMDD) 전환 + 문항 변경이력(감사추적) 신설
--  1) TB_CHKPT_INSPECT_ITEM.STR_DATE varchar(6)→varchar(8), 기존값은 월초(01)로 보정
--  2) TB_CHKPT_INSPECT_ITEM_HIST 신설: 등록/수정/사용중지/재사용 시점 스냅샷 적재
--  3) 기존 문항 이력 백필: 등록(01) 1건 + 현재 미사용(N) 문항은 사용중지(03) 1건
--     (백필 이력의 시각은 문항의 INSERT_DATE/UPDATE_DATE 를 그대로 사용 → 확인서 회색
--      게이팅이 기존 UPDATE_DATE 기반 동작과 동일하게 재현됨)
-- 실행: 1회성. 재실행 시 3)이 중복 적재되므로 이력 존재 여부를 먼저 확인할 것.
-- ============================================================

-- 1) 시행월 → 시행일
ALTER TABLE TB_CHKPT_INSPECT_ITEM
    MODIFY COLUMN STR_DATE varchar(8) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '시행일(YYYYMMDD)';

UPDATE TB_CHKPT_INSPECT_ITEM
   SET STR_DATE = CONCAT(STR_DATE, '01')
 WHERE LENGTH(STR_DATE) = 6;

-- 2) 문항 변경이력 테이블
CREATE TABLE IF NOT EXISTS TB_CHKPT_INSPECT_ITEM_HIST (
    HIST_ID             bigint       NOT NULL AUTO_INCREMENT COMMENT '이력ID',
    CMPNY_CD            varchar(50)  COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '회사코드',
    CHKLST_TYPE         varchar(10)  COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '체크리스트 타입[COM001]',
    INSPECT_ITEM_CD     varchar(20)  COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '점검항목코드',
    CHG_TYPE            varchar(2)   COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '변경유형(01:등록, 02:수정, 03:사용중지, 04:재사용)',
    INSPECT_ITEM_SUBJ   varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '변경 후 점검항목명칭',
    STR_DATE            varchar(8)   COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '변경 후 시행일(YYYYMMDD)',
    SORT_IDX            int          DEFAULT NULL COMMENT '변경 후 정렬순서',
    USE_YN              varchar(2)   COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'Y' COMMENT '변경 후 사용유무(Y/N)',
    INSERT_NO           varchar(50)  COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '입력자(tb_user.USER_CD)',
    INSERT_DATE         datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시(변경 시각)',
    PRIMARY KEY (HIST_ID),
    KEY IX_CHKPT_INSPECT_ITEM_HIST (CMPNY_CD, CHKLST_TYPE, INSPECT_ITEM_CD, INSERT_DATE)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='순회점검 문항 변경이력(감사추적)';

-- 3) 기존 문항 이력 백필 (이력이 비어 있을 때만 실행할 것)
INSERT INTO TB_CHKPT_INSPECT_ITEM_HIST
    (CMPNY_CD, CHKLST_TYPE, INSPECT_ITEM_CD, CHG_TYPE, INSPECT_ITEM_SUBJ, STR_DATE, SORT_IDX, USE_YN, INSERT_NO, INSERT_DATE)
SELECT A.CMPNY_CD, A.CHKLST_TYPE, A.INSPECT_ITEM_CD, '01', A.INSPECT_ITEM_SUBJ, A.STR_DATE, A.SORT_IDX, 'Y', A.INSERT_NO, IFNULL(A.INSERT_DATE, NOW())
  FROM TB_CHKPT_INSPECT_ITEM A
 WHERE NOT EXISTS (
        SELECT 1 FROM TB_CHKPT_INSPECT_ITEM_HIST H
         WHERE H.CMPNY_CD = A.CMPNY_CD AND H.CHKLST_TYPE = A.CHKLST_TYPE AND H.INSPECT_ITEM_CD = A.INSPECT_ITEM_CD
       );

INSERT INTO TB_CHKPT_INSPECT_ITEM_HIST
    (CMPNY_CD, CHKLST_TYPE, INSPECT_ITEM_CD, CHG_TYPE, INSPECT_ITEM_SUBJ, STR_DATE, SORT_IDX, USE_YN, INSERT_NO, INSERT_DATE)
SELECT A.CMPNY_CD, A.CHKLST_TYPE, A.INSPECT_ITEM_CD, '03', A.INSPECT_ITEM_SUBJ, A.STR_DATE, A.SORT_IDX, 'N', A.UPDATE_NO, IFNULL(A.UPDATE_DATE, NOW())
  FROM TB_CHKPT_INSPECT_ITEM A
 WHERE A.USE_YN = 'N'
   AND NOT EXISTS (
        SELECT 1 FROM TB_CHKPT_INSPECT_ITEM_HIST H
         WHERE H.CMPNY_CD = A.CMPNY_CD AND H.CHKLST_TYPE = A.CHKLST_TYPE AND H.INSPECT_ITEM_CD = A.INSPECT_ITEM_CD
           AND H.CHG_TYPE = '03'
       );
