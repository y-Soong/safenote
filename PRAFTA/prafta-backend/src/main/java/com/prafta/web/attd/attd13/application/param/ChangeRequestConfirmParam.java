package com.prafta.web.attd.attd13.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * 관리자 연차 변경 요청 최종 확인 Param (PRAFTA-COM-008-C).
 *
 * <p>대상 요청은 path 변수 {@code changeReqId} 로만 지정. 확인자 식별은 토큰에서만 도출.
 */
public record ChangeRequestConfirmParam(
      String changeReqId
    , String gvCmpnyCd
    , String gvAuthCd
    , String gvUserCd
) {
    public static ChangeRequestConfirmParam from(String changeReqId, TokenInfo tokenInfo) {
        if (changeReqId == null || changeReqId.isBlank() || tokenInfo == null) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        if (tokenInfo.gv_cmpnyCd() == null || tokenInfo.gv_cmpnyCd().isEmpty()
                || tokenInfo.gv_authCd() == null || tokenInfo.gv_authCd().isEmpty()
                || tokenInfo.gv_userCd() == null || tokenInfo.gv_userCd().isEmpty()) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        return new ChangeRequestConfirmParam(
              changeReqId
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_authCd()
            , tokenInfo.gv_userCd()
        );
    }
}
