-- ============================================================================
-- PRAFTA-COM-008-C — 연차 사용 출처(지정주체/촉진단계) + 관리자 연차수정 동의/거부 기반
-- 작성일: 2026-06-10
-- 적용 환경: MySQL 8.0.42
-- 참조: .claude/requests/common/prafta-com-008-C-use-source-consent.md §2/§3
--       prafta-com-001-sys-codes.sql / prafta-com-004-sys045-noti-type.sql (SYS 시드 스타일 미러)
--       prafta-com-001-leave-refusal-log.sql (테이블 DDL/채번 스타일 미러)
--
-- 변경 요약
--  1) tb_user_leave_use 확장 — 연차 일자별 사용 행에 "지정주체/촉진단계/최초지정일" 추가.
--     A(촉진)·B(노무수령거부) 가 이 값을 읽어 동작한다. GRANT_ID/DIRECT_USE_KEY 는 무변경.
--  2) tb_leave_change_request 신규 — 관리자/근로자 발의 연차 변경(이동)/삭제의 1:1 동의/거부 추적.
--     활성요청 멱등: 생성컬럼 ACTIVE_LEAVE_KEY(REQUESTED/AGREED 일 때만 TARGET_LEAVE_ID) + UNIQUE.
--  3) SYS068~073 신규 그룹 + 코드값 시드, SYS045 디테일 4종 추가(LEAVE_CHANGE_REQUEST/RESPONSE/CONFIRMED/REJECTED).
--
-- 채번: CHANGE_REQ_ID = CONCAT('LC', DATE_FORMAT(NOW(),'%Y%m%d'),
--                            FNC_CMM_SEQ_NEXTVAL(cmpnyCd, 'LEAVE_CHANGE_REQ_ID'))
--       (LeaveDashboardMapper.selectNextNotiId / selectNextGrantId 패턴과 동일.
--        SEQ_KEY 는 회사별 자동 INSERT 됨. 신규 채번 로직 생성 아님.)
--
-- 멱등성: ALTER/CREATE/INSERT 중복 실행 시 에러. 이미 반영된 환경에서는 건너뛸 것.
-- 적용 순서: BE 재기동 전 선적용 필수(미적용 시 attd13/leavechange 전면 실패).
-- ============================================================================

-- ── 1) tb_user_leave_use 확장 (연차 일자별 출처/촉진단계/최초지정일) ──
--   PROMOTION_STAGE  : 촉진단계[SYS068] NONE:비촉진 / FIRST:1차 / SECOND:2차
--                      → 노무수령거부 대상 판정의 단일 근거(∈{FIRST,SECOND}).
--   DESIGNATOR_TYPE  : 지정주체[SYS069] VOLUNTARY:자발(근로자) / COMPANY:회사직권
--                      → 입증강도·메시지 차이에만 사용(차단 판정엔 미사용).
--   ORIG_DESIGNATED_DATE : 최초 촉진 지정일(YYYYMMDD). 이동해도 보존(audit). START_DATE=최종확정일.
ALTER TABLE `tb_user_leave_use`
      ADD COLUMN `PROMOTION_STAGE`      varchar(10) NOT NULL DEFAULT 'NONE'
          COMMENT '촉진단계[SYS068] NONE:비촉진 / FIRST:1차 / SECOND:2차' AFTER `LEAVE_STATUS`
    , ADD COLUMN `DESIGNATOR_TYPE`      varchar(12) NOT NULL DEFAULT 'VOLUNTARY'
          COMMENT '지정주체[SYS069] VOLUNTARY:자발(근로자) / COMPANY:회사직권' AFTER `PROMOTION_STAGE`
    , ADD COLUMN `ORIG_DESIGNATED_DATE` varchar(8)  NULL DEFAULT NULL
          COMMENT '최초 촉진 지정일(YYYYMMDD, 이동해도 보존. START_DATE=최종확정일)' AFTER `DESIGNATOR_TYPE`
    , ADD KEY `IX_LEAVE_USE_PROMOTION` (`CMPNY_CD`, `USER_CD`, `PROMOTION_STAGE`, `START_DATE`);

-- ── 2) tb_leave_change_request 신규 (연차 변경/삭제 1:1 동의/거부) ──
CREATE TABLE `tb_leave_change_request` (
      `CHANGE_REQ_ID`     varchar(20)   NOT NULL COMMENT '변경요청 ID (PK, 회사별 채번: LC + YYYYMMDD + SEQ)'
    , `CMPNY_CD`          varchar(50)   NOT NULL COMMENT '회사 코드'
    , `SITE_CD`           varchar(50)   NOT NULL COMMENT '사업장 코드'
    , `TARGET_USER_CD`    varchar(20)   NOT NULL COMMENT '대상 근로자 코드'
    , `TARGET_LEAVE_ID`   varchar(20)   NOT NULL COMMENT '대상 연차 사용 ID (tb_user_leave_use.LEAVE_ID)'
    , `INITIATOR_TYPE`    varchar(8)    NOT NULL COMMENT '발의 주체[SYS070] ADMIN:관리자 / WORKER:근로자'
    , `REQ_TYPE`          varchar(8)    NOT NULL COMMENT '요청 유형[SYS071] MOVE:이동 / DELETE:삭제(관리자 발의만)'
    , `MOVE_TARGET_DATE`  varchar(8)             DEFAULT NULL COMMENT '이동 대상일 (YYYYMMDD, MOVE 시 필수 / DELETE 시 NULL)'
    , `REQ_REASON`        varchar(500)  NOT NULL COMMENT '요청 사유 (필수)'
    , `WORKER_RESPONSE`   varchar(8)    NOT NULL DEFAULT 'PENDING' COMMENT '근로자 응답[SYS073] PENDING:대기 / AGREE:동의 / REJECT:거부'
    , `RESPONSE_REASON`   varchar(500)           DEFAULT NULL COMMENT '근로자 응답 사유 (근로자 REJECT 시 필수)'
    , `REJECT_REASON`     varchar(500)           DEFAULT NULL COMMENT '관리자 반려 사유 (WORKER 발의건 관리자 reject 시 필수)'
    , `REQ_STATUS`        varchar(12)   NOT NULL DEFAULT 'REQUESTED' COMMENT '요청 상태[SYS072] REQUESTED→AGREED/REJECTED→CONFIRMED/CLOSED'
    , `INITIATOR_USER_CD` varchar(20)   NOT NULL COMMENT '발의자 코드 (관리자 또는 근로자 본인)'
    , `CONFIRM_USER_CD`   varchar(20)            DEFAULT NULL COMMENT '관리자 최종 확인자/반려자 코드'
    , `CONFIRM_DATE`      datetime               DEFAULT NULL COMMENT '관리자 최종 확인/반려 일시'
    , `DEL_YN`            varchar(1)    NOT NULL DEFAULT 'N' COMMENT '삭제 여부 Y:삭제 / N:정상'
    , `INSERT_NO`         varchar(50)   NOT NULL COMMENT '등록자'
    , `INSERT_DATE`       datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '등록 일시'
    , `UPDATE_NO`         varchar(50)            DEFAULT NULL COMMENT '수정자'
    , `UPDATE_DATE`       datetime               DEFAULT NULL COMMENT '수정 일시'
    -- 활성요청 멱등: REQUESTED/AGREED + DEL_YN='N' 일 때만 TARGET_LEAVE_ID, 아니면 NULL → UNIQUE 로 동시 활성요청 1건 강제.
    , `ACTIVE_LEAVE_KEY`  varchar(20)   GENERATED ALWAYS AS (
          CASE WHEN `DEL_YN` = 'N' AND `REQ_STATUS` IN ('REQUESTED','AGREED')
               THEN `TARGET_LEAVE_ID` ELSE NULL END
      ) STORED COMMENT '활성요청 멱등 키 (활성 상태일 때만 TARGET_LEAVE_ID)'
    , PRIMARY KEY (`CHANGE_REQ_ID`)
    , KEY `IX_LEAVE_CHG_TARGET_USER` (`CMPNY_CD`, `SITE_CD`, `TARGET_USER_CD`, `REQ_STATUS`)
    , KEY `IX_LEAVE_CHG_LEAVE`       (`CMPNY_CD`, `TARGET_LEAVE_ID`)
    , KEY `IX_LEAVE_CHG_STATUS`      (`CMPNY_CD`, `SITE_CD`, `REQ_STATUS`, `INITIATOR_TYPE`)
    , UNIQUE KEY `UK_LEAVE_CHG_ACTIVE` (`CMPNY_CD`, `ACTIVE_LEAVE_KEY`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='연차 변경/삭제 1:1 동의/거부 요청 (관리자/근로자 발의)';

-- ── 3-1) SYS068 촉진단계 (LEAVE_PROMOTION_STAGE) ──
INSERT INTO `tb_syst_val_m` (`SYST_VAL_CD`, `SYST_VAL_NM`, `USE_YN`, `VAL_DESC`, `INSERT_NO`)
VALUES ('SYS068', '연차 촉진단계', 'Y', 'tb_user_leave_use.PROMOTION_STAGE 코드 (LEAVE_PROMOTION_STAGE)', 'SYSTEM');
INSERT INTO `tb_syst_val_d`
      (`SYST_VAL_CD`, `SYST_VAL_D_CD`, `SYST_VAL_D_NM`, `SORT_IDX`, `USE_YN`, `INSERT_NO`)
VALUES
      ('SYS068', 'NONE',   '비촉진(일반)', 1, 'Y', 'SYSTEM')
    , ('SYS068', 'FIRST',  '1차 촉진',     2, 'Y', 'SYSTEM')
    , ('SYS068', 'SECOND', '2차 촉진',     3, 'Y', 'SYSTEM');

-- ── 3-2) SYS069 지정주체 (LEAVE_DESIGNATOR) ──
INSERT INTO `tb_syst_val_m` (`SYST_VAL_CD`, `SYST_VAL_NM`, `USE_YN`, `VAL_DESC`, `INSERT_NO`)
VALUES ('SYS069', '연차 지정주체', 'Y', 'tb_user_leave_use.DESIGNATOR_TYPE 코드 (LEAVE_DESIGNATOR)', 'SYSTEM');
INSERT INTO `tb_syst_val_d`
      (`SYST_VAL_CD`, `SYST_VAL_D_CD`, `SYST_VAL_D_NM`, `SORT_IDX`, `USE_YN`, `INSERT_NO`)
VALUES
      ('SYS069', 'VOLUNTARY', '자발(근로자)', 1, 'Y', 'SYSTEM')
    , ('SYS069', 'COMPANY',   '회사직권',     2, 'Y', 'SYSTEM');

-- ── 3-3) SYS070 발의주체 (LEAVE_CHANGE_INITIATOR) ──
INSERT INTO `tb_syst_val_m` (`SYST_VAL_CD`, `SYST_VAL_NM`, `USE_YN`, `VAL_DESC`, `INSERT_NO`)
VALUES ('SYS070', '연차변경 발의주체', 'Y', 'tb_leave_change_request.INITIATOR_TYPE 코드 (LEAVE_CHANGE_INITIATOR)', 'SYSTEM');
INSERT INTO `tb_syst_val_d`
      (`SYST_VAL_CD`, `SYST_VAL_D_CD`, `SYST_VAL_D_NM`, `SORT_IDX`, `USE_YN`, `INSERT_NO`)
VALUES
      ('SYS070', 'ADMIN',  '관리자 발의', 1, 'Y', 'SYSTEM')
    , ('SYS070', 'WORKER', '근로자 발의', 2, 'Y', 'SYSTEM');

-- ── 3-4) SYS071 변경요청유형 (LEAVE_CHANGE_REQ_TYPE) ──
INSERT INTO `tb_syst_val_m` (`SYST_VAL_CD`, `SYST_VAL_NM`, `USE_YN`, `VAL_DESC`, `INSERT_NO`)
VALUES ('SYS071', '연차변경 요청유형', 'Y', 'tb_leave_change_request.REQ_TYPE 코드 (LEAVE_CHANGE_REQ_TYPE)', 'SYSTEM');
INSERT INTO `tb_syst_val_d`
      (`SYST_VAL_CD`, `SYST_VAL_D_CD`, `SYST_VAL_D_NM`, `SORT_IDX`, `USE_YN`, `INSERT_NO`)
VALUES
      ('SYS071', 'MOVE',   '이동', 1, 'Y', 'SYSTEM')
    , ('SYS071', 'DELETE', '삭제', 2, 'Y', 'SYSTEM');

-- ── 3-5) SYS072 변경요청상태 (LEAVE_CHANGE_STATUS) ──
INSERT INTO `tb_syst_val_m` (`SYST_VAL_CD`, `SYST_VAL_NM`, `USE_YN`, `VAL_DESC`, `INSERT_NO`)
VALUES ('SYS072', '연차변경 요청상태', 'Y', 'tb_leave_change_request.REQ_STATUS 코드 (LEAVE_CHANGE_STATUS)', 'SYSTEM');
INSERT INTO `tb_syst_val_d`
      (`SYST_VAL_CD`, `SYST_VAL_D_CD`, `SYST_VAL_D_NM`, `SORT_IDX`, `USE_YN`, `INSERT_NO`)
VALUES
      ('SYS072', 'REQUESTED', '요청됨',       1, 'Y', 'SYSTEM')
    , ('SYS072', 'AGREED',    '동의됨',       2, 'Y', 'SYSTEM')
    , ('SYS072', 'REJECTED',  '거부됨',       3, 'Y', 'SYSTEM')
    , ('SYS072', 'CONFIRMED', '확인됨(반영)', 4, 'Y', 'SYSTEM')
    , ('SYS072', 'CLOSED',    '종료됨',       5, 'Y', 'SYSTEM');

-- ── 3-6) SYS073 응답 (LEAVE_CHANGE_RESPONSE) ──
INSERT INTO `tb_syst_val_m` (`SYST_VAL_CD`, `SYST_VAL_NM`, `USE_YN`, `VAL_DESC`, `INSERT_NO`)
VALUES ('SYS073', '연차변경 응답', 'Y', 'tb_leave_change_request.WORKER_RESPONSE 코드 (LEAVE_CHANGE_RESPONSE)', 'SYSTEM');
INSERT INTO `tb_syst_val_d`
      (`SYST_VAL_CD`, `SYST_VAL_D_CD`, `SYST_VAL_D_NM`, `SORT_IDX`, `USE_YN`, `INSERT_NO`)
VALUES
      ('SYS073', 'PENDING', '대기', 1, 'Y', 'SYSTEM')
    , ('SYS073', 'AGREE',   '동의', 2, 'Y', 'SYSTEM')
    , ('SYS073', 'REJECT',  '거부', 3, 'Y', 'SYSTEM');

-- ── 3-7) SYS045 디테일 추가 (연차 변경 동의/거부 PUSH 알림 4종: REQUEST/RESPONSE/CONFIRMED/REJECTED) ──
--   SYS045 마스터(tb_syst_val_m)는 prafta-031 에서 이미 등록됨 → 디테일만 INSERT.
--   SORT_IDX: 기존 최대(com-004=5) 다음으로 6/7/8/9 부여(표시정렬용, PK 아님).
INSERT INTO `tb_syst_val_d`
      (`SYST_VAL_CD`, `SYST_VAL_D_CD`, `SYST_VAL_D_NM`, `SORT_IDX`, `USE_YN`, `VAL_D_INFO_1`, `INSERT_NO`)
VALUES
      ('SYS045', 'LEAVE_CHANGE_REQUEST',   '연차 변경/삭제 요청(근로자)',   6, 'Y', 'PUSH', 'SYSTEM')
    , ('SYS045', 'LEAVE_CHANGE_RESPONSE',  '연차 변경 응답(관리자)',       7, 'Y', 'PUSH', 'SYSTEM')
    , ('SYS045', 'LEAVE_CHANGE_CONFIRMED', '연차 변경 확인 결과(근로자)',   8, 'Y', 'PUSH', 'SYSTEM')
    , ('SYS045', 'LEAVE_CHANGE_REJECTED',  '연차 변경 반려 결과(근로자)',   9, 'Y', 'PUSH', 'SYSTEM');
