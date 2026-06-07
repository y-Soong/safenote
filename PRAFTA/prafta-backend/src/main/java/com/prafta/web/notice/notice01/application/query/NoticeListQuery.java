package com.prafta.web.notice.notice01.application.query;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.notice.notice01.application.param.NoticeListParam;

public record NoticeListQuery(
    String titleKeyword
    , String popupYn
    , String pinYn
    , String startDate
    , String endDate
    , String siteCd
    , String nodeCd
    , String gvCmpnyCd
    , String gvUserCd
){
    public static NoticeListQuery from(NoticeListParam param) {

        if (param == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new NoticeListQuery(
            param.titleKeyword()
            , param.popupYn()
            , param.pinYn()
            , param.startDate()
            , param.endDate()
            , param.siteCd()
            , param.nodeCd()
            , param.gvCmpnyCd()
            , param.gvUserCd()
        );
    }
}
