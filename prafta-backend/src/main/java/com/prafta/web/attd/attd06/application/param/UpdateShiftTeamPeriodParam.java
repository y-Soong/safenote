package com.prafta.web.attd.attd06.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.attd.attd06.dto.request.UpdateShiftTeamPeriodRequest;

public record UpdateShiftTeamPeriodParam(
    String siteCd
    , String shiftCd
    , String shiftTeamId
    , String strDate
    , String endDate
    , String gvCmpnyCd
    , String gvUserCd
) {
    public static UpdateShiftTeamPeriodParam from(UpdateShiftTeamPeriodRequest request, TokenInfo tokenInfo) {

        if (request == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new UpdateShiftTeamPeriodParam(
            request.getSiteCd()
            , request.getShiftCd()
            , request.getShiftTeamId()
            , request.getStrDate()
            , request.getEndDate()
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
        );
    }
}
