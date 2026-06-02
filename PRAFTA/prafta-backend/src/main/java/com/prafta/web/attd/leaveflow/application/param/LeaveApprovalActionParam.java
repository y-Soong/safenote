package com.prafta.web.attd.leaveflow.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.attd.leaveflow.dto.request.LeaveApprovalActionRequest;

/**
 * 연차 결재 단계 처리 Param (prafta-019-E). 처리자(결재자)는 토큰에서 강제.
 */
public record LeaveApprovalActionParam(
      String reqId
    , Integer approvalStep
    , String comment
    , String gvCmpnyCd
    , String gvUserCd
) {
    public static LeaveApprovalActionParam from(LeaveApprovalActionRequest request, TokenInfo tokenInfo) {
        if (request == null || tokenInfo == null) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        if (tokenInfo.gv_cmpnyCd() == null || tokenInfo.gv_cmpnyCd().isEmpty()
                || tokenInfo.gv_userCd() == null || tokenInfo.gv_userCd().isEmpty()) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        if (request.getReqId() == null || request.getReqId().isBlank()
                || request.getApprovalStep() == null) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        return new LeaveApprovalActionParam(
              request.getReqId()
            , request.getApprovalStep()
            , request.getComment()
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
        );
    }
}
