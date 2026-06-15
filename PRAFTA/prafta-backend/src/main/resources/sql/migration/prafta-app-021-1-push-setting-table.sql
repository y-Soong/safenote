-- ============================================================================
-- PRAFTA-APP-021-1 — 사용자별 푸시 알림 수신 설정 테이블 신설 (tb_user_push_setting)
-- 작성일: 2026-06-12
-- 적용 환경: MySQL 8.0.42
-- 참조: .claude/requests/app_requests/prafta-app-021-plan.md §5(DDL)/§6(enforce),
--       prafta-031-noti-outbox.sql (테이블 DDL/감사 컬럼 스타일 미러)
--
-- 변경 요약
--  1) tb_user_push_setting 신규 — 사용자별 푸시 수신 설정(마스터 + NOTI_TYPE별 토글).
--     - opt-out 모델: 행이 없으면 수신 ON 으로 간주(USE_YN='N' 행이 있을 때만 발송 생략).
--       신규/미설정 사용자는 정상 수신(전부 ON). enforce 는 "OFF 행 존재"만 확인한다.
--     - 마스터 스위치는 특수 NOTI_TYPE 키 '__MASTER__' 1행으로 표현(별도 컬럼/테이블 신설 회피).
--       마스터 OFF(='N') 면 전 타입 발송 생략(§6 enforce 단일 쿼리).
--     - PK(CMPNY_CD, USER_CD, NOTI_TYPE) — outbox/스코프 관례 정합. NOTI_TYPE 폭은 outbox(varchar30)와 동일.
--     - FK 제약 없음(SYS045 카탈로그 참조용 varchar — outbox.NOTI_TYPE 과 동일 정책).
--
-- 적용 전 부재 확인:
--   SELECT 1 FROM information_schema.tables
--    WHERE table_schema = DATABASE() AND table_name = 'tb_user_push_setting';   -- 결과 있으면 CREATE 생략
--
-- 멱등성: CREATE TABLE 중복 실행 시 에러. 이미 반영된 환경에서는 건너뛸 것.
-- 적용 순서: 021-1(설정 CRUD EP)/021-2(워커 enforce) 코드 배포 전 선적용 필수.
--           (테이블 부재 시 설정 조회/저장 및 enforce 쿼리가 실패한다.)
-- ============================================================================

CREATE TABLE `tb_user_push_setting` (
      `CMPNY_CD`     varchar(50)  NOT NULL COMMENT '회사 코드'
    , `USER_CD`      varchar(20)  NOT NULL COMMENT '사용자 코드'
    , `NOTI_TYPE`    varchar(30)  NOT NULL COMMENT '알림 유형[SYS045] 또는 마스터 키 __MASTER__'
    , `USE_YN`       char(1)      NOT NULL DEFAULT 'Y' COMMENT '수신 여부 Y:수신 / N:미수신(=OFF 행)'
    , `INSERT_NO`    varchar(50)           DEFAULT NULL COMMENT '등록자'
    , `INSERT_DATE`  datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '등록 일시'
    , `UPDATE_NO`    varchar(50)           DEFAULT NULL COMMENT '수정자'
    , `UPDATE_DATE`  datetime              DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 일시'
    , PRIMARY KEY (`CMPNY_CD`, `USER_CD`, `NOTI_TYPE`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='사용자별 푸시 알림 수신 설정(opt-out: 행 없음=수신ON, USE_YN=N 행만 발송 생략)';
