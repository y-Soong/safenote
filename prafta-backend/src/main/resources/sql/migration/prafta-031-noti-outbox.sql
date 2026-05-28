-- ============================================================================
-- PRAFTA-031 — 푸시 알림 outbox 테이블 신설
-- 작성일: 2026-05-26
-- 적용 환경: MySQL 8.0.42
-- 참조: .claude/requests/prafta-031.md, 공통 정책서 §10(알림/공지)
--
-- 변경 요약
--  1) tb_noti_outbox 신규 — 모바일 push 등 알림 발송 대기/이력 outbox.
--     - 본 작업(연차 회수)에서는 회수 완료 시 1행 INSERT(SEND_STATUS='PENDING')만 한다(발송은 추후).
--     - 동일 이벤트 중복 발송 방지(§10.3): DEDUP_KEY + UNIQUE(CMPNY_CD, DEDUP_KEY).
--     - 회사 스코프(CMPNY_CD) + 표준 감사 컬럼(INSERT_NO/DATE, UPDATE_NO/DATE) 관례 준수.
--
-- 채번: NOTI_ID = 'N' + YYYYMMDD + FNC_CMM_SEQ_NEXTVAL(cmpnyCd, 'NOTI_OUTBOX_ID')
--       (selectNextGrantId 의 'LEAVE_GRANT_ID' 패턴과 동일).
--
-- 멱등성: CREATE TABLE 중복 실행 시 에러. 이미 반영된 환경에서는 건너뛸 것.
-- ============================================================================

CREATE TABLE `tb_noti_outbox` (
    `NOTI_ID`         varchar(20)  NOT NULL COMMENT '알림 ID (PK, 회사별 채번: N + YYYYMMDD + SEQ)',
    `CMPNY_CD`        varchar(50)  NOT NULL COMMENT '회사 코드',
    `SITE_CD`         varchar(50)  NULL     COMMENT '사업장 코드 (없으면 NULL)',
    `TARGET_USER_CD`  varchar(20)  NOT NULL COMMENT '수신 대상 사용자 코드',
    `NOTI_TYPE`       varchar(30)  NOT NULL COMMENT '알림 유형[SYS045] LEAVE_GRANT_RECALLED:부여 연차 회수',
    `CHANNEL`         varchar(10)  NOT NULL DEFAULT 'PUSH' COMMENT '발송 채널 PUSH:푸시',
    `TITLE`           varchar(200) NOT NULL COMMENT '알림 제목',
    `BODY`            varchar(1000) NOT NULL COMMENT '알림 본문',
    `DATA_PAYLOAD`    json         NULL     COMMENT '추가 데이터 페이로드(JSON)',
    `SEND_STATUS`     varchar(10)  NOT NULL DEFAULT 'PENDING' COMMENT '발송 상태 PENDING:대기 / SENT:완료 / FAILED:실패',
    `SENT_DATE`       datetime     NULL     COMMENT '발송 완료 일시',
    `RETRY_CNT`       int          NOT NULL DEFAULT 0 COMMENT '재시도 횟수',
    `ERROR_MSG`       varchar(500) NULL     COMMENT '발송 실패 사유',
    `DEDUP_KEY`       varchar(100) NULL     COMMENT '중복 발송 방지 키(이벤트당 1건)',
    `DEL_YN`          varchar(1)   NOT NULL DEFAULT 'N' COMMENT '삭제 여부',
    `INSERT_NO`       varchar(50)  NOT NULL COMMENT '등록자',
    `INSERT_DATE`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '등록 일시',
    `UPDATE_NO`       varchar(50)  NULL     COMMENT '수정자',
    `UPDATE_DATE`     datetime     NULL     COMMENT '수정 일시',
    PRIMARY KEY (`NOTI_ID`),
    UNIQUE KEY `UK_NOTI_OUTBOX_DEDUP` (`CMPNY_CD`, `DEDUP_KEY`),
    KEY `IX_NOTI_OUTBOX_PENDING` (`CMPNY_CD`, `SEND_STATUS`, `INSERT_DATE`),
    KEY `IX_NOTI_OUTBOX_TARGET` (`CMPNY_CD`, `TARGET_USER_CD`, `NOTI_TYPE`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='푸시 알림 outbox (발송 대기/이력)';
