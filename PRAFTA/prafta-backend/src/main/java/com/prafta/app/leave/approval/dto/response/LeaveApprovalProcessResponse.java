package com.prafta.app.leave.approval.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * 사용자연차결재-01 (3-D 처리): 연차 결재 단계 승인/반려 결과 응답.
 */
@Getter
@Builder
public class LeaveApprovalProcessResponse {

    private final String reqId;
    private final Integer approvalStep;
    private final String decision;
    private final boolean processed;
}
