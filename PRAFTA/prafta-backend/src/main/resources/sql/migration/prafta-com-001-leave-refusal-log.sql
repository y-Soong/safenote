-- ============================================================================
-- PRAFTA-COM-001 — 노무수령거부 통지/감지/알림 사실 기록 테이블 신설
-- 작성일: 2026-06-02
-- 적용 환경: MySQL 8.0.42
-- 참조: .claude/requests/common/refs/prafta-com-001/01_작업지시서_노무수령거부_PUSH.md §1
--       근태관리 정책서 attd §8.3(출근 차단 = 노무 수령 거부), §15(근태 알림),
--       공통 정책서 §10(알림/공지)
--
-- 변경 요약
--  1) tb_leave_refusal_log 신규 — 노무수령거부 관련 3개 이벤트(NOTICED / CHECKIN_DETECTED
--     / ADMIN_ALERTED)를 append 위주로 영구 기록하는 사실 로그.
--     - 출근 원본(tb_user_attd_mgmt)은 절대 수정하지 않는다. 본 테이블은 관찰/통지 사실만 적재.
--     - NOTICED 행이 곧 "노무수령거부 대상일(=연차촉진 사용지정일)" 데이터 소스다(메인세션 A-3=옵션1).
--       기능2(출근 감지)는 별도 지정일 테이블이 아니라 이 테이블의 NOTICED 행을 조회한다.
--     - 중복 발송/감지 방지: DEDUP_KEY + UNIQUE(CMPNY_CD, DEDUP_KEY).
--     - 회사 스코프(CMPNY_CD) + 표준 감사 컬럼(INSERT_NO/DATE, UPDATE_NO/DATE) 관례 준수.
--
-- 채번: REFUSAL_ID = CONCAT('LR', DATE_FORMAT(NOW(),'%Y%m%d'),
--                          FNC_CMM_SEQ_NEXTVAL(cmpnyCd, 'LEAVE_REFUSAL_ID'))
--       (LeaveDashboardMapper.selectNextNotiId / selectNextGrantId 패턴과 동일.
--        SEQ_KEY='LEAVE_REFUSAL_ID' 는 회사별 자동 INSERT 됨. 신규 채번 로직 생성 아님.)
--
-- EVENT_TYPE 코드 그룹: [SYS064] (메인세션 B-1 확정). 코드값 시드는
--   prafta-com-001-sys-codes.sql 에서 등록한다.
--
-- 멱등성: CREATE TABLE 중복 실행 시 에러. 이미 반영된 환경에서는 건너뛸 것.
-- ============================================================================

CREATE TABLE `tb_leave_refusal_log` (
      `REFUSAL_ID`      varchar(20)   NOT NULL COMMENT '노무수령거부 로그 ID (PK, 회사별 채번: LR + YYYYMMDD + SEQ)'
    , `CMPNY_CD`        varchar(50)   NOT NULL COMMENT '회사 코드'
    , `SITE_CD`         varchar(50)   NOT NULL COMMENT '사업장 코드'
    , `USER_CD`         varchar(20)   NOT NULL COMMENT '대상 근로자 코드'
    , `TARGET_YMD`      varchar(8)    NOT NULL COMMENT '노무수령거부 대상일 (YYYYMMDD, =연차촉진 사용지정일)'
    , `EVENT_TYPE`      varchar(20)   NOT NULL COMMENT '이벤트 유형[SYS064] NOTICED:통지발송 / CHECKIN_DETECTED:대상일출근감지 / ADMIN_ALERTED:관리자알림발송'
    , `RELATED_NOTI_ID` varchar(20)            DEFAULT NULL COMMENT '연관 알림 ID (tb_noti_outbox.NOTI_ID, NOTICED/ADMIN_ALERTED 시)'
    , `RELATED_ATTD_ID` varchar(20)            DEFAULT NULL COMMENT '연관 근태 ID (tb_user_attd_mgmt.ATTD_ID, CHECKIN_DETECTED 시)'
    , `DETECT_DTIME`    datetime               DEFAULT NULL COMMENT '출근 감지 일시 (CHECKIN_DETECTED 시)'
    , `DETAIL`          json                   DEFAULT NULL COMMENT '추가 페이로드 (PII 평문 금지)'
    , `DEL_YN`          varchar(1)    NOT NULL DEFAULT 'N' COMMENT '삭제 여부 (사실기록 무삭제 원칙) Y:삭제 / N:정상'
    , `INSERT_NO`       varchar(50)   NOT NULL COMMENT '등록자 (관리자 USER_CD or SYSTEM)'
    , `INSERT_DATE`     datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '등록 일시'
    , `UPDATE_NO`       varchar(50)            DEFAULT NULL COMMENT '수정자'
    , `UPDATE_DATE`     datetime               DEFAULT NULL COMMENT '수정 일시'
    , `DEDUP_KEY`       varchar(120)           DEFAULT NULL COMMENT '중복 방지 키 ({CMPNY_CD}_{USER_CD}_{TARGET_YMD}_{EVENT_TYPE})'
    , PRIMARY KEY (`REFUSAL_ID`)
    , KEY `IX_REFUSAL_LOG_USER`   (`CMPNY_CD`, `SITE_CD`, `USER_CD`, `TARGET_YMD`)
    , KEY `IX_REFUSAL_LOG_TARGET` (`CMPNY_CD`, `SITE_CD`, `TARGET_YMD`, `EVENT_TYPE`)
    , UNIQUE KEY `UK_REFUSAL_LOG_DEDUP` (`CMPNY_CD`, `DEDUP_KEY`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='노무수령거부 통지/감지/알림 사실 기록 (출퇴근 원본 무수정)';
