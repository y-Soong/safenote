package com.prafta.app.leave.approval.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * 사용자연차결재-01 (3-A 대기): 결재 대기 목록 조회 Param.
 *
 * <p>식별자(cmpny/user)는 토큰에서만 운반한다(IDOR 차단). keyword(요청자명/사번)만 쿼리로 수신한다.
 * 대기는 자연 소량이라 v1 은 LeaveFlowService.getMyPendingLeaveApprovals 전체 반환 + 메모리 keyword 필터(F-PG).
 */
public record LeaveApprovalPendingParam(
      String keyword
    , String gvCmpnyCd
    , String gvUserCd
) {
    public static LeaveApprovalPendingParam from(TokenInfo token, String keyword) {
        if (token == null || token.gv_cmpnyCd() == null || token.gv_userCd() == null) {
            throw new ApiException(CommonErrorCode.COMMON_400_003);
        }
        String kw = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        return new LeaveApprovalPendingParam(kw, token.gv_cmpnyCd(), token.gv_userCd());
    }
}
