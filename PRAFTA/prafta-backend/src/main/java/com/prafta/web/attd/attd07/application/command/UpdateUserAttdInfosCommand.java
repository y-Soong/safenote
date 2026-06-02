package com.prafta.web.attd.attd07.application.command;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.attd.attd07.application.model.UpdateUserAttdInfosModel;

public record UpdateUserAttdInfosCommand(
	String attdId
    , String siteCd
    , String nodeCd
    , String userCd
    , String userId
    , String workSeq
    , String workYmd
    , String checkInDate
    , String checkInTime
    , String checkInMethod
    , String checkOutDate
    , String checkOutTime
    , String checkOutMethod
    , String reason
    , String gvCmpnyCd
    , String gvUserCd
) {
    public static UpdateUserAttdInfosCommand from(String attdId, UpdateUserAttdInfosModel model) {

    	if (attdId == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);
    	
        if (model == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new UpdateUserAttdInfosCommand(
        	attdId
            , model.siteCd()
            , model.nodeCd()
            , model.userCd()
            , model.userId()
            , model.workSeq()
            , model.workYmd()
            , model.checkInDate()
            , model.checkInTime()
            , model.checkInMethod()
            , model.checkOutDate()
            , model.checkOutTime()
            , model.checkOutMethod()
            , model.reason()
            , model.gvCmpnyCd()
            , model.gvUserCd()
        );
    }
}
