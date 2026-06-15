-- ============================================================================
-- PRAFTA-COM-008-A-0 — 연차 사용촉진 진행 마스터 테이블 + SYS045 알림코드 시드
-- 작성일: 2026-06-12
-- 적용 환경: MySQL 8.0.42
-- 참조: .claude/requests/common/prafta-com-008-A-promotion.md §4(데이터 개념)/§5(PUSH)/§6(멱등)
--       .claude/requests/common/refs/prafta-com-008/prafta-com-008-A-decomposition.md D-A6/D-A7
--       prafta-com-008-c-leave-source-consent.sql (DDL/시드 스타일 미러 — SYS068/069 는 C 가 이미 시드)
--       prafta-com-001-leave-refusal-log.sql (테이블 DDL/감사컬럼 스타일 미러)
--
-- 변경 요약
--  1) tb_leave_promotion_log 신규 — 사용자별 1차 통지/2차 직권지정 진행 상태 추적.
--     A-1(판정·잔여)·A-2(1차 통지)·A-4(2차 직권지정)가 이 마스터를 읽고/쓴다.
--     1차 통지/2차 지정의 멱등은 UNIQUE(CMPNY_CD, DEDUP_KEY)로 강제.
--  2) SYS045 디테일 2종 추가 — LEAVE_PROMOTION_NOTICE(1차 통지) / LEAVE_PROMOTION_DESIGNATED(2차 통보).
--     SYS045 마스터(tb_syst_val_m)는 prafta-031 에서 이미 등록됨 → 디테일만 INSERT.
--     SORT_IDX: C(prafta-com-008-C)가 6~9 를 사용했으므로 다음 번호 10/11 부여(표시정렬용, PK 아님).
--
-- 채번: PROMO_ID = CONCAT('LP', DATE_FORMAT(NOW(),'%Y%m%d'),
--                        FNC_CMM_SEQ_NEXTVAL(cmpnyCd, 'LEAVE_PROMOTION_ID'))
--       (LeaveDashboardMapper.selectNextNotiId / C selectNextChangeReqId 패턴 동일.
--        SEQ_KEY 는 회사별 자동 INSERT 됨. 신규 채번 로직 생성 아님.)
--
-- 촉진단계/지정주체 코드(SYS068 NONE/FIRST/SECOND, SYS069 VOLUNTARY/COMPANY)는
--   prafta-com-008-C 가 이미 시드함 → 본 마이그에서 재시드하지 않는다(중복 INSERT 금지).
--
-- 멱등성: ALTER/CREATE/INSERT 중복 실행 시 에러. 이미 반영된 환경에서는 건너뛸 것.
-- 적용: 사용자(운영자)가 직접 적용한다(MCP read-only). BE 재기동 전 선적용 필수
--       (미적용 시 A-1/A-2 촉진 배치·통지 전면 실패).
-- ============================================================================

-- ── 1) tb_leave_promotion_log 신규 (촉진 진행 마스터) ──
--   1차 통지는 "연차일 등록"이 아니라 "통지 발송 사실"이므로 tb_user_leave_use 행으로 표현 불가.
--   (통지 시점엔 근로자가 아직 날짜 미지정) → 별도 마스터에 진행 상태를 기록한다.
--   확정된 연차일 자체는 tb_user_leave_use 에 들어가고(PROMOTION_STAGE/DESIGNATOR_TYPE 마커, 문서 C),
--   본 마스터는 "회차별 통지/지정/상태/멱등" 만 담는다.
CREATE TABLE `tb_leave_promotion_log` (
      `PROMO_ID`               varchar(20)   NOT NULL COMMENT '촉진 진행 ID (PK, 회사별 채번: LP + YYYYMMDD + SEQ)'
    , `CMPNY_CD`               varchar(50)   NOT NULL COMMENT '회사 코드'
    , `SITE_CD`                varchar(50)            DEFAULT NULL COMMENT '사업장 코드 (도래 판정 시점 사용자 소속, NULL 허용)'
    , `USER_CD`                varchar(20)   NOT NULL COMMENT '대상 근로자 코드'
    , `BASE_GRANT_ID`          varchar(20)            DEFAULT NULL COMMENT '역산 기준 본연차 부여 ID (tb_user_leave_grant.GRANT_ID)'
    , `BASE_AVAIL_TO_DATE`     varchar(8)    NOT NULL COMMENT '역산 기준 본연차 사용가능 종료일(YYYYMMDD, 촉진 시기 역산의 단일 기준)'
    , `PROMO_STAGE`            varchar(10)   NOT NULL COMMENT '촉진 단계[SYS068] FIRST:1차 / SECOND:2차'
    , `NOTICED_DATE`           varchar(8)             DEFAULT NULL COMMENT '1차 통지 발송일(YYYYMMDD, 1차 행에만 기록)'
    , `STAGE1_DESIGNATED_DAYS` decimal(5,1)  NOT NULL DEFAULT '0.0' COMMENT '1차 자발 지정 일수 스냅샷(근로자 계획서 제출분 누적)'
    , `STAGE2_TARGET_DAYS`     decimal(5,1)  NOT NULL DEFAULT '0.0' COMMENT '2차 직권 지정 대상 잔여 일수(2차 도래 시 산정)'
    , `STAGE2_DESIGNATED_DATE` varchar(8)             DEFAULT NULL COMMENT '2차 직권 지정 통보일(YYYYMMDD, 2차 행에 기록)'
    , `STATUS`                 varchar(12)   NOT NULL DEFAULT 'NOTICED' COMMENT '진행 상태[SYS075] NOTICED:통지됨 / DESIGNATED:직권지정됨 / COMPLETED:완료 / CLOSED:종료'
    , `LOGIN_NOTIFIED_YN`      varchar(1)    NOT NULL DEFAULT 'N' COMMENT '앱 로그인 안내 1회 노출 완료 여부 Y:노출함 / N:미노출(확정-3)'
    , `DEDUP_KEY`              varchar(80)   NOT NULL COMMENT '중복 통지/지정 방지 키 (예: PROMO_NOTICE_{USER}_{availTo} / PROMO_DESIG_{USER}_{availTo})'
    , `DEL_YN`                 varchar(1)    NOT NULL DEFAULT 'N' COMMENT '삭제 여부 Y:삭제 / N:정상'
    , `INSERT_NO`              varchar(50)   NOT NULL COMMENT '등록자'
    , `INSERT_DATE`            datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '등록 일시'
    , `UPDATE_NO`              varchar(50)            DEFAULT NULL COMMENT '수정자'
    , `UPDATE_DATE`            datetime               DEFAULT NULL COMMENT '수정 일시'
    , PRIMARY KEY (`PROMO_ID`)
    -- 멱등: 같은 회사·같은 회차(통지/지정) 키 1건만 허용 → 1차 통지/2차 지정 중복 차단(§6).
    , UNIQUE KEY `UK_LEAVE_PROMO_DEDUP` (`CMPNY_CD`, `DEDUP_KEY`)
    , KEY `IX_LEAVE_PROMO_USER`   (`CMPNY_CD`, `USER_CD`, `PROMO_STAGE`, `STATUS`)
    , KEY `IX_LEAVE_PROMO_AVAIL`  (`CMPNY_CD`, `USER_CD`, `BASE_AVAIL_TO_DATE`)
    , KEY `IX_LEAVE_PROMO_STATUS` (`CMPNY_CD`, `SITE_CD`, `PROMO_STAGE`, `STATUS`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='연차 사용촉진 진행 마스터(1차 통지/2차 직권지정 상태·멱등)';

-- ── 2) SYS075 촉진 진행상태 (LEAVE_PROMOTION_STATUS) ──
--   tb_leave_promotion_log.STATUS 코드. (C 가 SYS068~073, E-1 이 SYS074(근무계획 생성출처) 사용 → 본 작업 신규 그룹은 SYS075)
INSERT INTO `tb_syst_val_m` (`SYST_VAL_CD`, `SYST_VAL_NM`, `USE_YN`, `VAL_DESC`, `INSERT_NO`)
VALUES ('SYS075', '연차 촉진 진행상태', 'Y', 'tb_leave_promotion_log.STATUS 코드 (LEAVE_PROMOTION_STATUS)', 'SYSTEM');
INSERT INTO `tb_syst_val_d`
      (`SYST_VAL_CD`, `SYST_VAL_D_CD`, `SYST_VAL_D_NM`, `SORT_IDX`, `USE_YN`, `INSERT_NO`)
VALUES
      ('SYS075', 'NOTICED',    '통지됨(1차)',    1, 'Y', 'SYSTEM')
    , ('SYS075', 'DESIGNATED', '직권지정됨(2차)', 2, 'Y', 'SYSTEM')
    , ('SYS075', 'COMPLETED',  '완료',           3, 'Y', 'SYSTEM')
    , ('SYS075', 'CLOSED',     '종료',           4, 'Y', 'SYSTEM');

-- ── 3) SYS045 디테일 추가 (촉진 PUSH 알림 2종) ──
--   SYS045 마스터(tb_syst_val_m)는 prafta-031 에서 이미 등록됨 → 디테일만 INSERT.
--   SORT_IDX: C 가 6/7/8/9 사용 → 다음 10/11 부여(표시정렬용, PK 아님).
--   VAL_D_INFO_1='PUSH' (채널) — C 시드와 동일 컬럼 사용.
INSERT INTO `tb_syst_val_d`
      (`SYST_VAL_CD`, `SYST_VAL_D_CD`, `SYST_VAL_D_NM`, `SORT_IDX`, `USE_YN`, `VAL_D_INFO_1`, `INSERT_NO`)
VALUES
      ('SYS045', 'LEAVE_PROMOTION_NOTICE',     '연차 사용촉진 1차 통지(근로자)',     10, 'Y', 'PUSH', 'SYSTEM')
    , ('SYS045', 'LEAVE_PROMOTION_DESIGNATED', '연차 사용촉진 2차 직권지정 통보(근로자)', 11, 'Y', 'PUSH', 'SYSTEM');
