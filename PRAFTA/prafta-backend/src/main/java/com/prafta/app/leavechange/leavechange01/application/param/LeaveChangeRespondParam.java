package com.prafta.app.leavechange.leavechange01.application.param;

import com.prafta.app.leavechange.leavechange01.dto.request.LeaveChangeRespondRequest;
import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.attd.AttdErrorCode;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * 근로자 연차 변경 요청 응답(동의/거부) Param (PRAFTA-COM-008-C).
 *
 * <p>응답자(근로자) 식별은 토큰에서만 도출. 대상 요청은 path 변수 changeReqId.
 */
public record LeaveChangeRespondParam(
      String changeReqId
    , String workerResponse
    , String responseReason
    , String gvCmpnyCd
    , String gvUserCd
) {
    public static final String RESPONSE_AGREE = "AGREE";
    public static final String RESPONSE_REJECT = "REJECT";

    public static LeaveChangeRespondParam from(String changeReqId, LeaveChangeRespondRequest request, TokenInfo tokenInfo) {
        if (changeReqId == null || changeReqId.isBlank() || request == null || tokenInfo == null) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        if (tokenInfo.gv_cmpnyCd() == null || tokenInfo.gv_cmpnyCd().isEmpty()
                || tokenInfo.gv_userCd() == null || tokenInfo.gv_userCd().isEmpty()) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        String response = request.getWORKER_RESPONSE();
        if (!RESPONSE_AGREE.equals(response) && !RESPONSE_REJECT.equals(response)) {
            throw new ApiException(AttdErrorCode.ATTD_400_124);
        }
        // 거부 시 사유 필수(§3-1)
        if (RESPONSE_REJECT.equals(response)
                && (request.getRESPONSE_REASON() == null || request.getRESPONSE_REASON().isBlank())) {
            throw new ApiException(AttdErrorCode.ATTD_400_121);
        }
        return new LeaveChangeRespondParam(
              changeReqId
            , response
            , RESPONSE_REJECT.equals(response) ? request.getRESPONSE_REASON() : null
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
        );
    }
}
