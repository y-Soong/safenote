-- =====================================================================================
-- prafta-leavemulti-1 : 앱 연차 기간(From-To) 신청 — 묶음키 컬럼 추가
-- 작성일 : 2026-08-14
-- 지시서 : .claude/requests/app_requests/작업지시서_앱-연차-기간신청-및-묶음승인.md (T1)
--
-- ★ 개발 DB / 운영 DB 양쪽에 동시 적용한다 (한쪽만 적용해 장애가 반복된 전례 있음).
-- ★ 운영 적용은 사용자가 Workbench 로 직접 수행한다 (자동 실행 금지).
--
-- [배경]
--   종일 연차 기간신청은 "1 신청 → 날짜별 REQ N건" 으로 분해한다(설계 C안).
--   그 N건이 하나의 신청에서 나왔음을 표시하기 위한 묶음 식별자다.
--   용도는 ① 승인 목록에서 N건을 1행으로 접어 일괄 처리 ② PUSH 알림을 1건으로 수렴.
--
-- [무회귀 근거]
--   · nullable 컬럼 추가이며 기존 행은 전부 NULL 로 남는다.
--   · TB_USER_ATTD_REQ 참조 매퍼 18개를 전수 확인한 결과 SELECT * 실사용이 0건이고
--     모든 쿼리가 명시 컬럼이다 → 기존 조회/매핑에 영향 없음.
--   · 단일일 신청 경로는 이 값을 NULL 로 둔다 → 동작 완전 동일.
--
-- [값 형식]
--   'LG' + YYYYMMDD + SEQ(5) = 15자.  예) LG2026081400001
--   채번은 FNC_CMM_SEQ_NEXTVAL(cmpnyCd, 'LEAVE_GROUP_ID') 사용.
--   ※ 이 함수는 tb_cmm_seq 에 키가 없으면 INSERT ... ON DUPLICATE KEY 로 자동 생성하므로
--     본 마이그레이션에 시퀀스 시드 행을 넣을 필요가 없다.
-- =====================================================================================


-- -------------------------------------------------------------------------------------
-- STEP 1. 적용 전 확인 — 이미 적용됐는지 (0행이면 미적용)
-- -------------------------------------------------------------------------------------
SELECT COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE
  FROM information_schema.COLUMNS
 WHERE TABLE_SCHEMA = 'prafta'
   AND TABLE_NAME   = 'tb_user_attd_req'
   AND COLUMN_NAME  = 'LEAVE_GROUP_ID';


-- -------------------------------------------------------------------------------------
-- STEP 2. 컬럼 추가
--   SCH_CD 뒤에 둔다(요청 유형별 부가 식별자들이 모여 있는 위치).
-- -------------------------------------------------------------------------------------
ALTER TABLE TB_USER_ATTD_REQ
    ADD COLUMN LEAVE_GROUP_ID VARCHAR(20)
        CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci
        NULL
        COMMENT '연차 기간신청 묶음 ID (LG+YYYYMMDD+SEQ). 같은 신청에서 분해된 REQ 들이 동일 값. 단일일 신청·기타 요청유형은 NULL'
        AFTER SCH_CD;


-- -------------------------------------------------------------------------------------
-- STEP 3. 인덱스 추가 — 묶음 단위 조회(목록 접기 / 일괄 승인) 용
--   기존 인덱스 명명 규칙(IDX_ATTD_REQ_*) 을 따른다.
--   NULL 이 대다수인 컬럼이라 선택도가 높다(묶음 건만 조회).
-- -------------------------------------------------------------------------------------
ALTER TABLE TB_USER_ATTD_REQ
    ADD INDEX IDX_ATTD_REQ_LEAVE_GROUP (CMPNY_CD, LEAVE_GROUP_ID);


-- -------------------------------------------------------------------------------------
-- STEP 4. 적용 확인
-- -------------------------------------------------------------------------------------
SELECT COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE, COLUMN_COMMENT
  FROM information_schema.COLUMNS
 WHERE TABLE_SCHEMA = 'prafta'
   AND TABLE_NAME   = 'tb_user_attd_req'
   AND COLUMN_NAME  = 'LEAVE_GROUP_ID';
-- 기대: 1행 / varchar(20) / YES

SELECT INDEX_NAME, SEQ_IN_INDEX, COLUMN_NAME
  FROM information_schema.STATISTICS
 WHERE TABLE_SCHEMA = 'prafta'
   AND TABLE_NAME   = 'tb_user_attd_req'
   AND INDEX_NAME   = 'IDX_ATTD_REQ_LEAVE_GROUP'
 ORDER BY SEQ_IN_INDEX;
-- 기대: 2행 (CMPNY_CD, LEAVE_GROUP_ID)

SELECT COUNT(*) AS 전체행, SUM(CASE WHEN LEAVE_GROUP_ID IS NULL THEN 1 ELSE 0 END) AS NULL행
  FROM TB_USER_ATTD_REQ;
-- 기대: 전체행 == NULL행 (기존 데이터는 전부 NULL)


-- =====================================================================================
-- [롤백]
-- =====================================================================================
-- ALTER TABLE TB_USER_ATTD_REQ DROP INDEX IDX_ATTD_REQ_LEAVE_GROUP;
-- ALTER TABLE TB_USER_ATTD_REQ DROP COLUMN LEAVE_GROUP_ID;
