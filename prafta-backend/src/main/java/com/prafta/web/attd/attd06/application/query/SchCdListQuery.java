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
            throw ApiException.appendf(CommonErrorCode.COMMON_400_001, "\nRequired param missing - cmpnyCd");
        if (siteCd == null)
            throw ApiException.appendf(CommonErrorCode.COMMON_400_001, "\nRequired param missing - siteCd");
        if (shiftCd == null)
            throw ApiException.appendf(CommonErrorCode.COMMON_400_001, "\nRequired param missing - shiftCd");

        return new SchCdListQuery(
    		cmpnyCd
            , siteCd
            , shiftCd
        );
    }
}
