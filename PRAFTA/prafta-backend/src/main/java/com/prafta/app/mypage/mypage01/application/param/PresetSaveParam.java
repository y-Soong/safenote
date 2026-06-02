package com.prafta.app.mypage.mypage01.application.param;

import java.util.List;

import com.prafta.app.mypage.mypage01.dto.request.PresetSaveRequest;
import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * prafta-app-010-05: 프리셋 저장 Param.
 */
public record PresetSaveParam(
      String presetId
    , String presetNm
    , String defaultYn
    , List<String> approverUserCds
    , TokenInfo tokenInfo
) {
    public static PresetSaveParam from(PresetSaveRequest request, TokenInfo tokenInfo) {
        if (request == null) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        if (tokenInfo == null
                || tokenInfo.gv_cmpnyCd() == null || tokenInfo.gv_userCd() == null) {
            throw new ApiException(CommonErrorCode.COMMON_400_003);
        }
        return new PresetSaveParam(
              request.getPresetId()
            , request.getPresetNm()
            , request.getDefaultYn()
            , request.getApproverUserCds()
            , tokenInfo
        );
    }
}
