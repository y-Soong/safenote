package com.prafta.web.baim.baim05.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.baim.baim05.dto.request.InsertDailyQrUserRequest;

public record InsertDailyQrUserParam(
    String siteCd
    , String userNm
    , String mblNo
    , String slotNo
    , String gvCmpnyCd
    , String gvUserCd
) {
    public static InsertDailyQrUserParam from(InsertDailyQrUserRequest request, TokenInfo tokenInfo) {

        if (request == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new InsertDailyQrUserParam(
            request.getSiteCd()
            , request.getUserNm()
            , request.getMblNo()
            , request.getSlotNo()
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
        );
    }
}
