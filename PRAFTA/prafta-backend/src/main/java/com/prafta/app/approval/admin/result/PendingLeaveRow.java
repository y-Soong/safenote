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
     * prafta-leavemulti: 연차 기간(From-To) 신청 묶음 키(TB_USER_ATTD_REQ.LEAVE_GROUP_ID). 단일일 신청은 null.
     *
     * <p>★ MyBatis record 위치 기반 매핑 — selfYn 뒤 / borrowDays 앞 위치를 지켜야 한다.
     *   SELECT 절(pendingLeaveColumns 조각)과 순서가 어긋나면 대기목록 매핑이 통째로 밀린다.
     */
    , String leaveGroupId
    /**
     * 가불(미래 연차 당겨쓰기) 충당 일수 (가불표시-01). 항상 0 이상(null 없음).
     * ★ MyBatis record 위치 기반 매핑 — 반드시 맨 끝 유지(SELECT 맨 끝 컬럼 borrowDays 와 순서 일치).
     */
    , BigDecimal borrowDays
) {
}
