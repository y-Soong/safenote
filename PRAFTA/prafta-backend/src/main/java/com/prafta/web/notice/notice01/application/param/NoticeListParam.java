package com.prafta.web.notice.notice01.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.notice.notice01.dto.request.NoticeListRequest;

public record NoticeListParam(
    String titleKeyword
    , String popupYn
    , String pinYn
    , String startDate
    , String endDate
    , String siteCd
    , String nodeCd
    , String gvCmpnyCd
    , String gvUserCd
    , String gvAuthCd
){
    public static NoticeListParam from(NoticeListRequest request, TokenInfo tokenInfo) {

        if (request == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (tokenInfo == null)
            throw new ApiException(CommonErrorCode.COMMON_400_003);

        return new NoticeListParam(
            request.getTitleKeyword()
            , request.getPopupYn()
            , request.getPinYn()
            , request.getStartDate()
            , request.getEndDate()
            , request.getSiteCd()
            , request.getNodeCd()
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
            , tokenInfo.gv_authCd()
        );
    }
}
