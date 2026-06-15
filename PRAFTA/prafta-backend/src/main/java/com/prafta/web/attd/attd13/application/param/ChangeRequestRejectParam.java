package com.prafta.web.attd.attd13.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.attd.attd13.dto.request.ChangeRequestRejectRequest;

/**
 * 관리자 연차 변경 요청 반려 Param (PRAFTA-COM-008-C 작업2).
 *
 * <p>대상 요청은 path 변수 {@code changeReqId} 로만 지정. 반려자 식별은 토큰에서만 도출(IDOR 차단).
 * 반려 사유는 필수.
 */
public record ChangeRequestRejectParam(
      String changeReqId
    , String rejectReason
    , String gvCmpnyCd
    , String gvAuthCd
    , String gvUserCd
) {
    public static ChangeRequestRejectParam from(String changeReqId, ChangeRequestRejectRequest request, TokenInfo tokenInfo) {
        if (changeReqId == null || changeReqId.isBlank() || request == null || tokenInfo == null) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        if (request.getREJECT_REASON() == null || request.getREJECT_REASON().isBlank()) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        if (tokenInfo.gv_cmpnyCd() == null || tokenInfo.gv_cmpnyCd().isEmpty()
                || tokenInfo.gv_authCd() == null || tokenInfo.gv_authCd().isEmpty()
                || tokenInfo.gv_userCd() == null || tokenInfo.gv_userCd().isEmpty()) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        return new ChangeRequestRejectParam(
              changeReqId
            , request.getREJECT_REASON()
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_authCd()
            , tokenInfo.gv_userCd()
        );
    }
}
