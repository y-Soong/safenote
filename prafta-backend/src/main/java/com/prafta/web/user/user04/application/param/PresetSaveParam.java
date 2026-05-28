package com.prafta.web.user.user04.application.param;

import java.util.Collections;
import java.util.List;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.user.user04.dto.request.PresetSaveRequest;

/**
 * 프리셋 저장 Param (prafta-020). 소유자/회사/사업장은 토큰으로 강제.
 */
public record PresetSaveParam(
      String presetId
    , String presetNm
    , String defaultYn
    , List<String> approverUserCds
    , String gvCmpnyCd
    , String gvSiteCd
    , String gvUserCd
) {
    public static PresetSaveParam from(PresetSaveRequest request, TokenInfo tokenInfo) {
        validate(request, tokenInfo);
        String defaultYn = "Y".equals(request.getDefaultYn()) ? "Y" : "N";
        List<String> approvers = (request.getApproverUserCds() == null)
                ? Collections.emptyList()
                : request.getApproverUserCds();
        return new PresetSaveParam(
              blankToNull(request.getPresetId())
            , request.getPresetNm() == null ? null : request.getPresetNm().trim()
            , defaultYn
            , approvers
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_siteCd()
            , tokenInfo.gv_userCd()
        );
    }

    private static void validate(PresetSaveRequest request, TokenInfo tokenInfo) {
        if (request == null || tokenInfo == null) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        if (tokenInfo.gv_cmpnyCd() == null || tokenInfo.gv_cmpnyCd().isEmpty()
                || tokenInfo.gv_siteCd() == null || tokenInfo.gv_siteCd().isEmpty()
                || tokenInfo.gv_userCd() == null || tokenInfo.gv_userCd().isEmpty()) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
    }

    private static String blankToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
