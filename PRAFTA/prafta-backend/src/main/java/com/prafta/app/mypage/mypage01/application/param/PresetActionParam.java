package com.prafta.app.mypage.mypage01.application.param;

import com.prafta.app.mypage.mypage01.dto.request.PresetActionRequest;
import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * prafta-app-010-05: 프리셋 단건 액션(기본지정/삭제) Param.
 */
public record PresetActionParam(
      String presetId
    , TokenInfo tokenInfo
) {
    public static PresetActionParam from(PresetActionRequest request, TokenInfo tokenInfo) {
        if (request == null) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        if (tokenInfo == null
                || tokenInfo.gv_cmpnyCd() == null || tokenInfo.gv_userCd() == null) {
            throw new ApiException(CommonErrorCode.COMMON_400_003);
        }
        return new PresetActionParam(request.getPresetId(), tokenInfo);
    }
}
