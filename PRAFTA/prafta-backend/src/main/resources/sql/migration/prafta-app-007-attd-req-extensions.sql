-- ============================================================================
-- PRAFTA-APP-007-1 — 모바일 앱 근태 요청 폼 3종 지원용 스키마 확장
-- 작성일: 2026-05-29
-- 적용 환경: MySQL 8.0.42
-- 참조: .claude/requests/app_requests/prafta-app-007-plan.md §3.1, §11, Q1/P8
--
-- 변경 요약
--   1) SYS032 (요청 유형) 코드그룹에 '10 = 스케줄 수정 요청' 디테일 1행 추가.
--      - 기존 01 근태생성 / 02 근태수정 (시각 보정) 과 구분되는 "스케줄 자체 변경" 요청 유형.
--      - 모바일 폼 `POST /appApi/req07/sched-modify` 가 본 코드값으로 INSERT 한다.
--   2) tb_user_attd_req 테이블에 SCH_CD 컬럼(varchar20, NULL 허용) 추가.
--      - REQ_TYPE='10' 일 때만 값을 채우며, 다른 REQ_TYPE 행에서는 NULL.
--      - 위치: LEAVE_DAYS 다음 (AFTER LEAVE_DAYS).
--
-- 적용 전 부재 확인 (운영 적용 직전 권장):
--   SELECT COUNT(*) FROM tb_syst_val_d WHERE SYST_VAL_CD='SYS032' AND SYST_VAL_D_CD='10';
--   SHOW COLUMNS FROM tb_user_attd_req LIKE 'SCH_CD';
--
-- 멱등성: 본 파일은 운영 적용 후 보관용(재실행 금지). 적용 전 상태 확인 후 수행.
-- 운영 적용: 사용자 수동(read-only MCP). 본 파일은 작성만, DB 직접 적용 금지.
--
-- 참고
--   - tb_syst_val_d 스키마는 CMPNY_CD / DEL_YN 컬럼이 존재하지 않는다 (전사 공통 코드 테이블).
--     plan §3.1 골격의 CMPNY_CD='*' 표기는 실제 스키마 기준으로 본 파일에서 제거.
--   - 마스터 행(tb_syst_val_m.SYS032)은 prafta 기존 적재분이 존재하므로 본 파일에서 INSERT 하지 않는다.
-- ============================================================================

-- 1) SYS032=10 "스케줄 수정 요청" 디테일 행 추가
INSERT INTO `tb_syst_val_d` (
      `SYST_VAL_CD`
    , `SYST_VAL_D_CD`
    , `SYST_VAL_D_NM`
    , `SORT_IDX`
    , `USE_YN`
    , `INSERT_NO`
) VALUES (
      'SYS032'
    , '10'
    , '스케줄 수정 요청'
    , 10
    , 'Y'
    , 'SYSTEM'
);

-- 2) tb_user_attd_req 에 SCH_CD 컬럼 추가
--    REQ_TYPE='10' (스케줄 수정 요청) 일 때만 값 채움. 다른 REQ_TYPE 은 NULL.
ALTER TABLE `tb_user_attd_req`
    ADD COLUMN `SCH_CD` varchar(20) COLLATE utf8mb4_unicode_ci NULL
    COMMENT '스케줄 코드 (REQ_TYPE=10 스케줄 수정 요청 시 변경 목표 SCH_CD, 그 외 NULL)'
    AFTER `LEAVE_DAYS`;
