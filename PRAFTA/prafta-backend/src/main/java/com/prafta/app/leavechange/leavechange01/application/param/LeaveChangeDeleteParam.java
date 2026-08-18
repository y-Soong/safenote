package com.prafta.app.leavechange.leavechange01.application.param;

import com.prafta.app.leavechange.leavechange01.dto.request.LeaveChangeDeleteRequest;
import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.attd.AttdErrorCode;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * 근로자 연차 취소(삭제) 발의 Param (2026-08-18 개방).
 *
 * <p>발의자(근로자) 식별은 토큰에서만 도출. 대상은 본인 소유 LEAVE_ID(서버 재검증).
 */
public record LeaveChangeDeleteParam(
      String targetLeaveId
    , String reqReason
    , String gvCmpnyCd
    , String gvUserCd
) {
    public static LeaveChangeDeleteParam from(LeaveChangeDeleteRequest request, TokenInfo tokenInfo) {
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
        if (request.getREQ_REASON() == null || request.getREQ_REASON().isBlank()) {
            throw new ApiException(AttdErrorCode.ATTD_400_120);
        }
        return new LeaveChangeDeleteParam(
              request.getTARGET_LEAVE_ID()
            , request.getREQ_REASON()
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
        );
    }
}
