package com.prafta.web.attd.attd06.application.query;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.attd.attd06.application.param.UserListsParam;

public record SchCdListQuery(
    String cmpnyCd
    , String siteCd
    , String shiftCd
) {
    public static SchCdListQuery from(String cmpnyCd, String siteCd, String shiftCd) {

        if (cmpnyCd == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (siteCd == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (shiftCd == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new SchCdListQuery(
    		cmpnyCd
            , siteCd
            , shiftCd
        );
    }
}
