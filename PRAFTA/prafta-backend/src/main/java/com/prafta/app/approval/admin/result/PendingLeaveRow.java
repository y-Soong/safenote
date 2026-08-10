package com.prafta.app.approval.admin.result;

import java.math.BigDecimal;

/**
 * 001-P2-B2: 연차(05/06) 대기 요청 1행(web LeaveFlowMapper.selectMyPendingLeaveApprovals 포팅).
 *
 * <p>연차는 결재선(APPROVER_USER_CD) 기반 — "내가 현재 단계 결재자"인 요청만 노출(web 동일).
 */
public record PendingLeaveRow(
      String reqId
    , Integer approvalStep
    , String reqType
    , String requesterUserCd
    , String requesterUserNm
    , String nodeNm
    , String workYmd
    , String leaveType
    , String leaveCd
    , String leaveNo
    , String leaveNm
    , String useUnitType
    , String unitNm
    , BigDecimal leaveDays
    , Integer leaveMinutes
    , String startTime
    , String endTime
    , String reqReason
    , String reqDate
    , String selfYn
    /**
     * 가불(미래 연차 당겨쓰기) 충당 일수 (가불표시-01). 항상 0 이상(null 없음).
     * ★ MyBatis record 위치 기반 매핑 — 반드시 맨 끝 유지(SELECT 맨 끝 컬럼 borrowDays 와 순서 일치).
     */
    , BigDecimal borrowDays
) {
}
