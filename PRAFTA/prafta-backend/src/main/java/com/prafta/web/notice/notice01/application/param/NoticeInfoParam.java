package com.prafta.web.notice.notice01.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.notice.notice01.dto.request.NoticeInfoRequest;

public record NoticeInfoParam(
    String noticeId
    , String gvCmpnyCd
    , String gvUserCd
    , String gvAuthCd
){
    public static NoticeInfoParam from(NoticeInfoRequest request, TokenInfo tokenInfo) {

        if (request == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (tokenInfo == null)
            throw new ApiException(CommonErrorCode.COMMON_400_003);

        return new NoticeInfoParam(
            request.getNoticeId()
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
            , tokenInfo.gv_authCd()
        );
    }
}
