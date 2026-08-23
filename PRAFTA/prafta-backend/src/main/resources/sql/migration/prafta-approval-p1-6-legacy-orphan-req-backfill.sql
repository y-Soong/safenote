-- =============================================================================
-- 근태결재선통합 P1-6 : 레거시 orphan REQ(결재선 없음) 보조 백필
--
-- 배경(작업지시서 §4·plan.md §0-3 보정②):
--   결재선(TB_USER_ATTD_REQ_APPROVAL) 없이 생성된 대기('01') 근태보정/스케줄수정/초과근무 REQ 는
--   신규 게이트(ApprovalStepGateService.resolveProcessableStep)가 "처리 시점"에 lazy 로 1단계
--   결재선을 자동 생성한다 — 주 메커니즘은 이미 P1-1 로 구현되어 있어 본 스크립트 없이도 정합하다.
--
--   본 스크립트는 P2(ReqInbox 결재선 기반 목록 조회 전환) 준비용 보조 수단이다: "아직 아무도
--   열어보지 않은 orphan REQ" 가 결재선 기반 목록에 뜨지 않는 사각지대를 배포 시점에 미리 없앤다.
--   lazy 폴백과 완전히 동일한 형식(정 관리자 MAIN_ADMIN_CD 우선, 없으면 부 관리자 SUB_ADMIN_CD)을
--   사용한다 — ApprovalLineMapper.selectDefaultApproverOfNode 와 동일 공식.
--
-- 적용 대상: REQ_STATUS='01'(대기) AND REQ_TYPE IN ('01','02','03','04','10')
--           AND TB_USER_ATTD_REQ_APPROVAL 에 매칭 결재선 행이 아직 없는 REQ.
--
-- 안전성(idempotent/additive): 이미 결재선이 있는 REQ 는 LEFT JOIN 조건(A.REQ_ID IS NULL)으로
--   완전히 제외되므로 재실행해도 중복 INSERT 되지 않는다. 기존 데이터는 전혀 건드리지 않는다.
--
-- 대상 노드에 정/부 관리자가 모두 없으면(IFNULL 결과 NULL) 이 스크립트는 그 REQ 를 건너뛴다
--   (fail-closed — 잘못된 결재자를 합성하지 않는다). 그런 REQ 는 배포 후 처리 시도 시점에
--   lazy 폴백이 ATTD_400_105("승인 가능한 부서 관리자가 지정되어 있지 않습니다")로 안내한다.
--
-- 실행 전 필수: developer 가 DESCRIBE TB_USER_ATTD_REQ / TB_USER_ATTD_REQ_APPROVAL / TB_SITE_NODE 로
--   컬럼명을 재확인했음(2026-08-23, 기존 ApprovalLineMapper.xml·AttdCloseMapper.xml SQL과 동일 컬럼 재사용).
--
-- 적용 순서: 근태결재선통합 P1(백엔드 엔진) 배포 이후, P2(ReqInbox 전환) 배포 직전.
-- 개발/운영 동시 적용 원칙(메모리 feedback_db_migration_apply_both_envs) 준수 — 운영은 Workbench 로 직접 실행.
-- =============================================================================

INSERT INTO TB_USER_ATTD_REQ_APPROVAL (
      REQ_ID
    , APPROVAL_STEP
    , CMPNY_CD
    , APPROVER_USER_CD
    , APPROVAL_STATUS
    , APPROVAL_COMMENT
    , APPROVAL_DATE
    , INSERT_NO
    , INSERT_DATE
    , UPDATE_NO
    , UPDATE_DATE
)
SELECT
      R.REQ_ID
    , 1                                              -- 1단계(단일 결재선)
    , R.CMPNY_CD
    , IFNULL(N.MAIN_ADMIN_CD, N.SUB_ADMIN_CD)         -- 정 관리자 우선, 없으면 부 관리자
    , '01'                                            -- SYS044 '01' 신청(=처리 가능한 현재 단계)
    , NULL
    , NULL
    , 'SYSTEM'
    , NOW()
    , 'SYSTEM'
    , NOW()
  FROM TB_USER_ATTD_REQ R
  JOIN TB_SITE_NODE N
    ON N.CMPNY_CD = R.CMPNY_CD
   AND N.SITE_CD  = R.SITE_CD
   AND N.NODE_CD  = R.NODE_CD
  LEFT JOIN TB_USER_ATTD_REQ_APPROVAL A
    ON A.CMPNY_CD = R.CMPNY_CD
   AND A.REQ_ID   = R.REQ_ID
 WHERE R.REQ_STATUS = '01'
   AND R.REQ_TYPE IN ('01', '02', '03', '04', '10')
   AND A.REQ_ID IS NULL
   AND IFNULL(N.MAIN_ADMIN_CD, N.SUB_ADMIN_CD) IS NOT NULL;

-- =============================================================================
-- 검증
--   -- 백필 후 잔존 orphan(관리자 미지정 노드) 카운트 — 0이 아니면 그 노드에 관리자 지정 필요
--   SELECT R.REQ_TYPE, COUNT(*) AS cnt
--     FROM TB_USER_ATTD_REQ R
--     LEFT JOIN TB_USER_ATTD_REQ_APPROVAL A
--       ON A.CMPNY_CD = R.CMPNY_CD AND A.REQ_ID = R.REQ_ID
--    WHERE R.REQ_STATUS = '01'
--      AND R.REQ_TYPE IN ('01','02','03','04','10')
--      AND A.REQ_ID IS NULL
--    GROUP BY R.REQ_TYPE;
-- =============================================================================
