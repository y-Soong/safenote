package com.prafta.web.attd.leaveflow.dto.response;

import java.util.List;

import com.prafta.web.attd.leaveflow.vo.MyLeaveApprovalVO;

import lombok.Builder;
import lombok.Value;

/**
 * 내 결재함(연차) 목록 응답 (prafta-019-E 후속).
 */
@Value
@Builder
public class MyApprovalListResponse {
    List<MyLeaveApprovalVO> approvalList;
}
