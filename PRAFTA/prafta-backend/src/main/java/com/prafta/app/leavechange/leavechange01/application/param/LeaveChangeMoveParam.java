package com.prafta.app.leavechange.leavechange01.application.param;

import com.prafta.app.leavechange.leavechange01.dto.request.LeaveChangeMoveRequest;
import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.attd.AttdErrorCode;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * 근로자 연차 이동 발의 Param (PRAFTA-COM-008-C, C-5a).
 *
 * <p>발의자(근로자) 식별은 토큰에서만 도출. 대상은 본인 소유 LEAVE_ID(서버 재검증).
 */
public record LeaveChangeMoveParam(
      String targetLeaveId
    , String moveTargetDate
    , String reqReason
    , String gvCmpnyCd
    , String gvUserCd
) {
    public static LeaveChangeMoveParam from(LeaveChangeMoveRequest request, TokenInfo tokenInfo) {
        if (request == null || tokenInfo == null) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        if (tokenInfo.gv_cmpnyCd() == null || tokenInfo.gv_cmpnyCd().isEmpty()
                || tokenInfo.gv_userCd() == null || tokenInfo.gv_userCd().isEmpty()) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        if (request.getTARGET_LEAVE_ID() == null || request.getTARGET_LEAVE_ID().isBlank()) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        if (request.getMOVE_TARGET_DATE() == null || request.getMOVE_TARGET_DATE().isBlank()) {
            throw new ApiException(AttdErrorCode.ATTD_400_122);
        }
        if (request.getREQ_REASON() == null || request.getREQ_REASON().isBlank()) {
            throw new ApiException(AttdErrorCode.ATTD_400_120);
        }
        return new LeaveChangeMoveParam(
              request.getTARGET_LEAVE_ID()
            , request.getMOVE_TARGET_DATE()
            , request.getREQ_REASON()
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
        );
    }
}
