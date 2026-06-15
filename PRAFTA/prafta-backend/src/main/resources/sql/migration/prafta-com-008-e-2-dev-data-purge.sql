-- ============================================================================
-- PRAFTA-COM-008-E-2 — 개발 환경 데이터 일괄삭제 DML (★개발 1회 전제 — 자동 실행 금지)
-- 작성일: 2026-06-11
-- 적용 환경: MySQL 8.0.42 (로컬/개발 DB 전용)
-- 참조: prafta-com-008-E-default-worktype.md §7(결정 5), prafta-com-008-E-decomposition.md §3 D-E4 / §4 ②
--
-- ★★★ 경고 ★★★
--  - 본 파일은 "연차-스케줄 모델 전환(E-2)" 으로 인해 기존 WORK_PLAN_CD=LEAVE_CD 적재분 등
--    테스트 데이터를 정합 마이그 대신 일괄삭제하기 위한 DML 이다.
--  - 운영 DB 에 절대 적용하지 않는다. 개발 환경에서 1회만, 사용자가 직접 검토 후 수동 적용한다.
--  - developer / 자동화 도구는 본 파일을 실행하지 않는다(MCP read-only · DB write 금지).
--  - 삭제 후 연차 부여(tb_user_leave_grant) 재생성이 필요하면 연차부여 배치/수동부여로 별도 수행한다.
--
-- 삭제 대상(테스트 데이터 — 근무계획/연차/근태/요청). 외래 의존 역순으로 비운다.
-- 필요에 따라 회사/사업장 스코프(WHERE CMPNY_CD=...)를 추가해 일부만 비울 수 있다.
-- ============================================================================

-- 1) 근태 요청/이력 (연차/근태/스케줄 수정/초과근무 요청)
-- DELETE FROM `tb_user_attd_req`;

-- 2) 근태 실적 + GPS
-- DELETE FROM `tb_user_attd_gps`;
-- DELETE FROM `tb_user_attd_mgmt`;

-- 3) 연차 사용/부여 + 변경요청 + 노무수령거부 로그
-- DELETE FROM `tb_leave_change_request`;
-- DELETE FROM `tb_leave_refusal_log`;
-- DELETE FROM `tb_user_leave_use`;
-- DELETE FROM `tb_user_leave_grant`;

-- 4) 근무계획(연차 LEAVE_CD 적재분 포함 전체)
-- DELETE FROM `tb_user_work_plan`;

-- ※ 위 DELETE 들은 기본적으로 주석 처리되어 있다. 사용자가 삭제 범위를 확정한 뒤 주석을 해제하여 적용한다.
--   (자동 실행 방지를 위한 안전장치 — 그대로 source 하면 아무 것도 삭제되지 않는다.)
