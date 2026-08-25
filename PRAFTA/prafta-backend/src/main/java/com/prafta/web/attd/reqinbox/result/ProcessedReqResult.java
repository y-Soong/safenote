package com.prafta.web.attd.reqinbox.result;

import java.math.BigDecimal;

/**
 * 요청 승인 관리 — 내가 승인/반려 처리한 요청 이력 1건 (내 처리 이력 팝업).
 *
 * <p>근태보정/초과/스케줄 탭과 연차 탭 모두 결재라인 TB_USER_ATTD_REQ_APPROVAL(APPROVER_USER_CD=본인)
 * 기준으로 조회한다(근태결재선통합 P2-2 — 종전 근태보정/초과/스케줄 탭은 TB_USER_ATTD_REQ
 * (PROCESS_USER_CD=본인) 기준이었으나, 다단계 결재의 중간 단계 승인자 처리 이력이 누락되는
 * 결함이 있어 연차 탭과 동형 패턴으로 통일했다). 두 statement 가 동일 컬럼 세트를 반환해
 * 본 record 하나로 받는다.
 *
 * <p>★ record 위치 기반 매핑 — SELECT 컬럼 순서와 컴포넌트 순서가 1:1 이어야 한다
 * (두 statement 모두 이 순서를 지킬 것).
 */
public record ProcessedReqResult(
      String reqId
    , String reqType
    , String userCd
    , String userNm
    , String nodeNm
    , String workYmd
    , String startDate
    , String startTime
    , String endDate
    , String endTime
    , String leaveTypeNm     // 연차 탭 전용(연차종류명, 미매치 시 R.LEAVE_TYPE 폴백) — 그 외 NULL
    , BigDecimal leaveDays   // 연차 탭 전용 — 그 외 NULL
    , String unitNm          // 연차 탭 전용(사용 단위명 SYS025) — 그 외 NULL
    , String schNo           // 스케줄 수정 탭 전용(요청 스케줄 번호) — 그 외 NULL
    , String reqReason
    , String reqDate
    , String procStatus      // AP.APPROVAL_STATUS(SYS044 02승인/03반려) — 두 statement 공통
    , String processDate
    , String processComment  // 처리 코멘트(반려 사유 등, AP.APPROVAL_COMMENT)
    , Integer approvalStep   // 내가 처리한 결재 단계(AP.APPROVAL_STEP) — 두 statement 공통
    // 접수함다중사업장권한확장-002: "전체 사업장" 선택 시 행별 사업장 구분 표시용. 컬럼 순서상 항상 끝에 추가
    // (selectProcessedRequests / selectProcessedLeaveApprovals 두 statement 모두 동일 위치에 채운다).
    , String siteNm
) {
}
