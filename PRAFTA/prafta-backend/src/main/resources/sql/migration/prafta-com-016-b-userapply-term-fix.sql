-- ============================================================================
-- PRAFTA-COM-016-B (3-1) — 사용자 신청 타입 "사용 가능 기간" 2모드 재정의
--   '03'(기간설정) 옵션 폐지에 따른 기존 데이터 정정
-- 작성일: 2026-06-18
-- 적용 환경: MySQL 8.0.42
-- 참조: .claude/requests/common/prafta-com-016-B.md (3-1)
--       .claude/context/policies/attd/08-leave.md §8.1.1 (사용가능기간 속성)
--
-- 변경 요약
--   사용자 신청 타입(LEAVE_TYPE='01')의 사용가능기간을 다음 2모드로 한정한다:
--     '01' 설정안함  = 전체 기간 누적 총량(lifetime, 연도 리셋 없음)
--     '02' 해당연도내 = 회계연도 매년(기존 동작)
--   '03'(기간설정, MMDD 절대범위) 옵션을 폐지하므로, 기존 '03' 행을 '02'로 정정하고
--   AVAIL_FROM_DT/AVAIL_TO_DT 를 NULL 로 비운다.
--
--   대상 실데이터(확인됨): LEAVE_CD 00022(test01) / 00023(test02), 0101~0331 (2건).
--
-- 데이터 손실 위험: 낮음. 폐지된 '03' MMDD 범위값(0101~0331)은 더 이상 사용처가 없으므로
--   NULL 로 비운다. 정정 후 두 타입은 '해당연도내'(회계연도) 모드로 동작한다.
--
-- 적용 전 현재 상태 확인 (운영 적용 직전 권장):
--   SELECT LEAVE_CD, LEAVE_NM, AVAIL_TERM_TYPE, AVAIL_FROM_DT, AVAIL_TO_DT
--     FROM tb_leave_type_mgmt
--    WHERE LEAVE_TYPE='01' AND AVAIL_TERM_TYPE='03';  -- 정정 대상 행 확인
--
-- 멱등성: 정정 후 '03' 행이 0건이 되므로 재실행해도 영향 없음(0행 업데이트).
-- 운영 적용: 사용자 수동(read-only MCP). 본 파일은 작성만, DB 직접 적용 금지.
-- ============================================================================

UPDATE tb_leave_type_mgmt
   SET AVAIL_TERM_TYPE = '02'
     , AVAIL_FROM_DT   = NULL
     , AVAIL_TO_DT     = NULL
 WHERE LEAVE_TYPE      = '01'
   AND AVAIL_TERM_TYPE = '03';
