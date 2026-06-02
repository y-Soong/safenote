-- ============================================================================
-- PRAFTA-APP-008 — 외근(근무지 외) 출퇴근 사유 저장 컬럼 추가
-- 작성일: 2026-05-29
-- 적용 환경: MySQL 8.0.42
-- 참조: .claude/requests/app_requests/prafta-app-008-plan.md §1-2, §4 / P2-D1, P2-D3
--       .claude/requests/app_requests/prafta-app-008.md §7.2~§7.3
--
-- 변경 요약
--   1) tb_user_attd_gps 테이블에 OFFSITE_REASON 컬럼(varchar500, NULL 허용) 추가.
--      - GPS 행은 "지오펜스 밖(외근)"일 때만 INSERT 되므로(GPS 행 존재 = 외근),
--        외근 사유를 본 컬럼에 저장한다(P2-D1 확정: GPS 행에 동거).
--      - 외근 출퇴근 시 사유 필수(P2-D3). 온사이트(정상)는 GPS 행 자체가 없어 사유도 없음.
--      - 위치: IP_ADDR 다음 (AFTER IP_ADDR).
--
-- 적용 전 부재 확인 (운영 적용 직전 권장):
--   SHOW COLUMNS FROM tb_user_attd_gps LIKE 'OFFSITE_REASON';
--
-- ⚠️ 적용 선행성(중요): 본 ALTER 가 운영 DB 에 적용되기 전에는
--   AppAttd01Mapper.insertCheckInGps / insertCheckOutGps 의 OFFSITE_REASON 컬럼 INSERT 가
--   "Unknown column" 으로 실패한다(외근 출퇴근 등록 전체 실패). 반드시 본 마이그를 선행 적용할 것.
--
-- 멱등성: 본 파일은 운영 적용 후 보관용(재실행 금지). 적용 전 상태 확인 후 수행.
-- 운영 적용: 사용자 수동(read-only MCP). 본 파일은 작성만, DB 직접 적용 금지.
-- ============================================================================

-- 1) tb_user_attd_gps.OFFSITE_REASON 컬럼 추가 (외근 사유, varchar500 NULL)
ALTER TABLE `tb_user_attd_gps`
    ADD COLUMN `OFFSITE_REASON` varchar(500) COLLATE utf8mb4_unicode_ci NULL
        COMMENT '외근(근무지 외) 사유' AFTER `IP_ADDR`;
