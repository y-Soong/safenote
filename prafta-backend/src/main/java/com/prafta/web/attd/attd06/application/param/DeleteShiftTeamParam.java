package com.prafta.web.attd.attd06.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.attd.attd06.dto.request.DeleteShiftTeamRequest;

public record DeleteShiftTeamParam(
    String siteCd
    , String shiftCd
    , String shiftTeamId
    , String gvCmpnyCd
    , String gvUserCd
) {
    public static DeleteShiftTeamParam from(DeleteShiftTeamRequest request, TokenInfo tokenInfo) {

        if (request == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new DeleteShiftTeamParam(
            request.getSiteCd()
            , request.getShiftCd()
            , request.getShiftTeamId()
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
        );
    }
}
