package com.prafta.web.user.user04.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.user.user04.dto.request.PresetActionRequest;

/**
 * 프리셋 단건 액션(삭제/기본지정) Param (prafta-020). 소유자/회사는 토큰으로 강제.
 */
public record PresetActionParam(
      String presetId
    , String gvCmpnyCd
    , String gvUserCd
) {
    public static PresetActionParam from(PresetActionRequest request, TokenInfo tokenInfo) {
        if (request == null || tokenInfo == null) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        if (tokenInfo.gv_cmpnyCd() == null || tokenInfo.gv_cmpnyCd().isEmpty()
                || tokenInfo.gv_userCd() == null || tokenInfo.gv_userCd().isEmpty()) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        return new PresetActionParam(
              request.getPresetId()
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
        );
    }
}
