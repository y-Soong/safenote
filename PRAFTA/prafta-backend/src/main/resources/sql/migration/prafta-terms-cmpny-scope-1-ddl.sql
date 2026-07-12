-- prafta: tb_terms_user_agr_mgmt 회사 스코프(CMPNY_CD) 추가
--   배경: USER_CD 는 회사별 채번(FNC_CMM_SEQ_NEXTVAL(cmpnyCd,'USER_CD'))이라 전역 유일이 아니다.
--         같은 연월에 생성된 서로 다른 회사의 사용자가 동일한 USER_CD(예: 20260600001)를 가질 수 있는데,
--         약관 동의 기록(tb_terms_user_agr_mgmt)이 USER_CD 만으로 키잉되어 회사 간 동의가 오염되었다.
--         (A사 master 동의 → B사 master(동일 USER_CD)가 "이미 동의"로 오판되어 약관 게이트가 스킵됨)
--   조치: CMPNY_CD 컬럼 추가 + (CMPNY_CD, USER_CD, TERMS_ID, TERMS_VERSION) 로 PK 재정의.
--
--   ★ 본 마이그레이션은 회사 귀속이 불가능한 동의행(고아/모호)을 삭제한다.
--      - 고아: USER_CD 가 현재 TB_USER 에 없음(삭제된 회사의 잔재) — 신규 계정 약관 오판의 직접 원인.
--      - 모호: USER_CD 가 2개 이상 회사의 TB_USER 에 매칭 — 어느 회사 동의인지 결정 불가.
--      두 경우 모두 신뢰할 수 있는 회사 귀속이 없어 동의 근거로 쓸 수 없으므로 삭제한다.
--      (운영 적용 전 백업 권장. 적용 후 해당 사용자는 다음 로그인 시 약관 재동의 필요.)

-- 1) CMPNY_CD 컬럼 추가 (백필 위해 우선 nullable)
--   ★ MySQL 8.0 은 ADD COLUMN IF NOT EXISTS 미지원 → 이미 추가된 상태에서 재실행 시 1060 발생.
--     INFORMATION_SCHEMA 로 존재 여부를 확인해 없을 때만 추가하도록 동적 실행(재실행 안전).
SET @col_exists := (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'tb_terms_user_agr_mgmt'
       AND COLUMN_NAME = 'CMPNY_CD'
);
SET @ddl := IF(@col_exists = 0,
    'ALTER TABLE tb_terms_user_agr_mgmt ADD COLUMN CMPNY_CD VARCHAR(50) NULL COMMENT ''회사코드'' AFTER USER_CD',
    'DO 0'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2) 백필: USER_CD 가 TB_USER 에서 "정확히 한 회사"로만 매칭되는 행만 회사코드 채움
--   ★ Workbench 안전모드(Error 1175) 회피: UPDATE/DELETE 조건이 KEY 컬럼이 아니라
--     서브쿼리 기반이므로, 세션 단위로 SQL_SAFE_UPDATES 를 잠시 끄고 복구한다.
SET SQL_SAFE_UPDATES = 0;

UPDATE tb_terms_user_agr_mgmt A
   SET A.CMPNY_CD = (SELECT U.CMPNY_CD FROM TB_USER U WHERE U.USER_CD = A.USER_CD)
 WHERE (SELECT COUNT(*) FROM TB_USER U WHERE U.USER_CD = A.USER_CD) = 1;

-- 3) 정리: 회사 귀속 불가(고아/모호) 동의행 삭제
DELETE FROM tb_terms_user_agr_mgmt WHERE CMPNY_CD IS NULL;

SET SQL_SAFE_UPDATES = 1;

-- 4) PK 재정의: CMPNY_CD NOT NULL 전환 + 복합 PK 재구성
ALTER TABLE tb_terms_user_agr_mgmt
    MODIFY COLUMN CMPNY_CD VARCHAR(50) NOT NULL COMMENT '회사코드';

ALTER TABLE tb_terms_user_agr_mgmt
    DROP PRIMARY KEY,
    ADD PRIMARY KEY (CMPNY_CD, USER_CD, TERMS_ID, TERMS_VERSION);
