-- ============================================================================
-- SOJEONG-1-3 — 법정 연차 자동 부여 on/off 토글 컬럼 추가 (TB_LEAVE_POLICY)
-- 작성일: 2026-08-12
-- 적용 환경: MySQL 8.0.42 이상 (★개발·운영 동시 적용 — feedback_db_migration_apply_both_envs)
-- 참조: 작업지시서_근로자별-소정근로시간-관리-도입.md §연차 부여 on/off 토글 (Baim_07)
--       작업지시서_근로자별-소정근로시간-관리-도입.plan.md §1.3
--
-- 변경 요약
--  1) TB_LEAVE_POLICY 에 STATUTORY_AUTO_GRANT_YN char(1) NOT NULL DEFAULT 'Y' 추가.
--     5인 미만 사업장(근기법 60조 적용 제외) 대응 — 회사 단위 토글.
--
-- 규약
--  - ★기본값 'Y' = 기존 동작 보존. 기존 전 회사가 현행과 동일하게 자동 부여를 유지한다.
--  - 타입/NOT NULL/COMMENT 스타일은 같은 테이블의 APRV_USE_YN(법정연차 결재 여부)과 동형.
--  - 저장처 선정 근거: 회사당 활성 정책 1행(UX_TB_LEAVE_POLICY_ACTIVE 함수 UNIQUE)이므로
--    회사 레벨 토글이 성립하고, 부여 엔진·촉진 배치가 이미 이 테이블을 읽어 join 추가 비용이
--    없다. 별도 회사 토글 테이블은 이력 축 중복이라 기각.
--  - ★★감사 이력 주의 (소정-05 담당자 필수 작업)
--    tb_leave_policy_history 의 PREV/NEW_SNAPSHOT 은 테이블 전체 덤프가 아니라
--    LeavePolicyServiceImpl.serializePolicyForSnapshot(common/cmm/leave/service/impl,
--    line 675 부근)의 **명시 화이트리스트**다. 따라서 본 컬럼을 추가하는 것만으로는
--    변경 이력에 절대 포함되지 않는다.
--    → 소정-05 에서 해당 메서드에 snap.put("statutoryAutoGrantYn", ...) 를 추가할 것.
--      누락 시 5인 미만 토글의 on/off 변경이 노무 감사 이력에 남지 않는다.
--  - off 시 동작(지시서 확정): 법정 자동 부여 엔진/스케줄러 중지, 촉진 배치 skip,
--    가불(선차감) 부여 차단(Q3), 앱·웹 연차 진입점 숨김(부여 이력 0 조건 병행).
--    ★수동(약정) 부여와 이미 부여된 연차는 유지 — off 는 신규 자동 부여 중지일 뿐이다.
--  - 승격 경로: "5인 미만 적용 제외 축"(주52·가산 제외 등)의 첫 사례. 상위 개념으로
--    승격할 때는 본 컬럼을 별도 적용제외 정책 테이블로 이관한다(plan §1.3).
--
-- 멱등성: ALTER 중복 실행 시 에러(Duplicate column). 이미 반영된 환경에서는 건너뛸 것.
-- 적용 순서: sojeong-1-1 → 1-2 → 1-3 → 1-4 → 1-5.
--            ★BE 재기동 전 선적용 필수 — 미적용 상태로 신규 코드가 뜨면 Baim_07 정책
--            조회/저장이 신규 컬럼 참조로 전면 실패한다(1054 Unknown column).
-- 운영 적용: 사용자 수동(Workbench). 본 파일은 작성만, DB 직접 적용 금지.
-- ============================================================================

ALTER TABLE `tb_leave_policy`
  ADD COLUMN `STATUTORY_AUTO_GRANT_YN` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci
      NOT NULL DEFAULT 'Y'
      COMMENT '법정 연차 자동 부여 사용 Y/N (5인 미만 사업장 대응. N: 부여엔진·정기부여 배치·사용촉진·가불 부여 중지, 약정 수동부여와 기부여분은 유지)'
      AFTER `AXIS7_USE_PROMOTION`;
