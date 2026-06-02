package com.prafta.web.nearmiss.nearmiss01.application.command;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.nearmiss.nearmiss01.application.param.SaveIncidentParam;

public record SaveIncidentCommand(
    String siteCd
    , String nearMissId
    , String causeDesc
    , String preventionDesc
    , String immediateActionDesc
    , String gvCmpnyCd
    , String gvUserCd
){
    public static SaveIncidentCommand from(SaveIncidentParam param) {

        if (param == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new SaveIncidentCommand(
            param.siteCd()
            , param.nearMissId()
            , param.causeDesc()
            , param.preventionDesc()
            , param.immediateActionDesc()
            , param.gvCmpnyCd()
            , param.gvUserCd()
        );
    }
}
