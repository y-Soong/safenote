package com.prafta.web.attd.attd06.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.attd.attd06.dto.request.UpdateShiftTeamNmRequest;

public record UpdateShiftTeamNmParam(
    String siteCd
    , String shiftCd
    , String shiftTeamId
    , String shiftTeamNm
    , String gvCmpnyCd
    , String gvUserCd
    , String gvAuthCd
) {
    public static UpdateShiftTeamNmParam from(UpdateShiftTeamNmRequest request, TokenInfo tokenInfo) {

        if (request == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new UpdateShiftTeamNmParam(
            request.getSiteCd()
            , request.getShiftCd()
            , request.getShiftTeamId()
            , request.getShiftTeamNm()
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
            , tokenInfo.gv_authCd()
        );
    }
}
