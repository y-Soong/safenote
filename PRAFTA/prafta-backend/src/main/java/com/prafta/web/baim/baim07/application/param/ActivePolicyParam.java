package com.prafta.web.baim.baim07.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * 활성 정책 단건 조회 Param.
 *
 * <p>GET endpoint이며, JWT의 CMPNY_CD만 사용한다.
 */
public record ActivePolicyParam(String gvCmpnyCd) {

    public static ActivePolicyParam from(TokenInfo tokenInfo) {
        if (tokenInfo == null) {
            throw new ApiException(CommonErrorCode.COMMON_400_003);
        }
        if (tokenInfo.gv_cmpnyCd() == null || tokenInfo.gv_cmpnyCd().isEmpty()) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        return new ActivePolicyParam(tokenInfo.gv_cmpnyCd());
    }
}
