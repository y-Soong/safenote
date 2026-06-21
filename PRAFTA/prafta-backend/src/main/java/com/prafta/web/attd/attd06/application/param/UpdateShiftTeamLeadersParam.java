package com.prafta.web.attd.attd06.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.attd.attd06.dto.request.UpdateShiftTeamLeadersRequest;

public record UpdateShiftTeamLeadersParam(
    String siteCd
    , String userCd
    , String leaderYn
    , String shiftCd
    , String shiftTeamId
    , String teamIdx
    , String gvCmpnyCd
    , String gvUserCd
    , String gvAuthCd
) {
    public static UpdateShiftTeamLeadersParam from(UpdateShiftTeamLeadersRequest request, TokenInfo tokenInfo) {

        if (request == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new UpdateShiftTeamLeadersParam(
            request.getSiteCd()
            , request.getUserCd()
            , request.getLeaderYn()
            , request.getShiftCd()
            , request.getShiftTeamId()
            , request.getTeamIdx()
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
            , tokenInfo.gv_authCd()
        );
    }
}
