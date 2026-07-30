-- ============================================================================
-- PRAFTA-PC-1 — 연차 짜투리 잔여 보전(T4) DDL
-- 작성일: 2026-07-28
-- 적용 환경: MySQL 8.x
-- 참조: .claude/requests/web_requests/작업지시서_연차-개인분모-전환-및-짜투리-보전.md (D3~D7·D9)
--       같은 이름 .plan.md §1(기록처 결정 — 별도 이력 테이블 채택) / PC-04
--
-- ⚠ 적용 원칙: 개발 DB · 운영 DB 에 "동시" 적용한다 (한쪽만 적용 금지 —
--    환경 불일치 장애 재발 방지, 메모리 feedback_db_migration_apply_both_envs).
--    실행은 사용자 수동(Workbench). 본 파일은 코드 배포 전 적용되어야 한다
--    (ALLOW_REMNANT_ROUND_UP / TB_LEAVE_REMNANT_COVER 참조 코드가 PC-05~07 에 포함).
--
-- 변경 요약
--  1) tb_leave_usage_policy: 짜투리 잔여 보전 옵션 컬럼(ALLOW_REMNANT_ROUND_UP) 신설.
--     기본 'N'(OFF) — 기존 회사 전부 OFF 로 시작(연말 수당 정산 대상 리포트 지원만).
--  2) TB_LEAVE_REMNANT_COVER 신설 — 짜투리 발동 시 회사 부담분 이력(D6 기록 / D7 회수 /
--     D9-② 연간 집계의 단일 출처).
--
-- 멱등성: MySQL 8 은 ADD COLUMN IF NOT EXISTS 미지원 — 이미 반영된 환경에서는
--         1) 구문을 건너뛸 것. CREATE TABLE 은 IF NOT EXISTS 로 멱등.
-- 롤백(참고, 필요 시 수동 실행):
--   ALTER TABLE tb_leave_usage_policy DROP COLUMN ALLOW_REMNANT_ROUND_UP;
--   DROP TABLE IF EXISTS TB_LEAVE_REMNANT_COVER;
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1) 짜투리 잔여 보전 옵션 (Baim_07 연차부여정책 — D3)
--    USAGE_UNIT 다음 위치. 'Y' = 잔여 < 최소 사용단위 요금일 때 최소단위 1건 사용 허용
--    + 부족분 회사 부담. 'N' = 시스템 미개입(소멸 임박 리포트로 지원).
-- ----------------------------------------------------------------------------
ALTER TABLE tb_leave_usage_policy
    ADD COLUMN ALLOW_REMNANT_ROUND_UP char(1) NOT NULL DEFAULT 'N'
        COMMENT '짜투리 잔여 보전 (Y:최소단위 발동+회사부담 / N:연말 수당 정산 대상 리포트)'
        AFTER USAGE_UNIT;

-- ----------------------------------------------------------------------------
-- 2) 짜투리 회사 부담 보전 이력 (plan §1 DDL 안 — D3/D6/D7/D9-②)
--    - 발동 1건 = 1행. use 원장은 무변경(잔여 전액 차감분만 use 에 기록 — 원장 음수 금지 D6).
--    - D7 회수 판정 = COVER_STATUS='ACTIVE' AND WORK_YMD > 오늘(미도래).
--    - D9-② 연간 집계 = WORK_YMD LIKE '{YYYY}%' AND DEL_YN='N' AND COVER_DAYS > 0.
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS TB_LEAVE_REMNANT_COVER (
  COVER_ID       varchar(20)  NOT NULL COMMENT '보전 이력 ID (RC+YYYYMMDD+회사별 시퀀스 — 회사 간 중복 가능, PK는 CMPNY_CD 복합)',
  CMPNY_CD       varchar(50)  NOT NULL COMMENT '회사 코드',
  SITE_CD        varchar(50)  NOT NULL COMMENT '사업장 코드',
  USER_CD        varchar(20)  NOT NULL COMMENT '사용자 코드',
  REQ_ID         varchar(20)  DEFAULT NULL COMMENT '발동 신청 ID (tb_user_attd_req.REQ_ID)',
  WORK_YMD       varchar(8)   NOT NULL COMMENT '짜투리 발동 건 근무일 (YYYYMMDD, D7 도래/미도래 판정 기준)',
  USE_UNIT_TYPE  varchar(2)   NOT NULL COMMENT '발동 최소 사용단위 (SYS025)',
  CHARGE_DAYS    decimal(8,5) NOT NULL COMMENT '최소단위 정상 요금(일)',
  REMNANT_DAYS   decimal(8,5) NOT NULL COMMENT '발동 시 전액 차감한 잔여(일) — use 행 합과 일치',
  COVER_DAYS     decimal(8,5) NOT NULL COMMENT '회사 부담분(일) = CHARGE_DAYS - REMNANT_DAYS. 부분 회수 시 감소',
  COVER_MINUTES  int          NOT NULL COMMENT '회사 부담분(분) = COVER_DAYS × CONV_MINUTES (DOWN 절사, 표기·preview용)',
  CONV_MINUTES   int          NOT NULL COMMENT '발동 시 개인 분모(분) — 감사·회수 재계산 기준 고정',
  COVER_STATUS   varchar(10)  NOT NULL DEFAULT 'ACTIVE' COMMENT '상태 (ACTIVE:유효 / RECLAIMED:전액 회수 / 도래 확정은 ACTIVE 유지+WORK_YMD 경과로 판별)',
  RECLAIM_DATE   datetime     DEFAULT NULL COMMENT '최종 회수 일시',
  DEL_YN         varchar(1)   NOT NULL DEFAULT 'N',
  INSERT_NO      varchar(50)  NOT NULL,
  INSERT_DATE    datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UPDATE_NO      varchar(50)  DEFAULT NULL,
  UPDATE_DATE    datetime     DEFAULT NULL,
  -- 보안리뷰 H-1: COVER_ID 채번이 회사별 시퀀스(FNC_CMM_SEQ_NEXTVAL)라 회사 간 동일 ID 가능 —
  --   멀티테넌시 복합 PK 관례(prafta-tenant-1-composite-pk-ddl.sql, tb_user_leave_use 등)와 동일하게
  --   (CMPNY_CD, COVER_ID) 복합 PK. 참조 코드는 전 쿼리 WHERE 에 CMPNY_CD 포함(코드 무수정).
  PRIMARY KEY (CMPNY_CD, COVER_ID),
  KEY IDX_REMNANT_COVER_USER (CMPNY_CD, USER_CD, COVER_STATUS),
  KEY IDX_REMNANT_COVER_YMD  (CMPNY_CD, WORK_YMD),
  KEY IDX_REMNANT_COVER_REQ  (REQ_ID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='연차 짜투리 회사 부담 보전 이력 (D3/D6/D7/D9-②)';

-- ----------------------------------------------------------------------------
-- 3) 적용 확인 (수동 검증용)
-- ----------------------------------------------------------------------------
-- DESCRIBE tb_leave_usage_policy;   -- ALLOW_REMNANT_ROUND_UP 존재 + 기존 행 전부 'N'
-- DESCRIBE TB_LEAVE_REMNANT_COVER;  -- plan §1 컬럼 1:1 대조
