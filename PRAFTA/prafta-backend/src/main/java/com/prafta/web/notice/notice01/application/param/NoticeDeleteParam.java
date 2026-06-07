package com.prafta.web.notice.notice01.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.notice.notice01.dto.request.NoticeDeleteRequest;

public record NoticeDeleteParam(
    String noticeId
    , String editPwd
    , String gvCmpnyCd
    , String gvUserCd
    , String gvAuthCd
){
    public static NoticeDeleteParam from(NoticeDeleteRequest request, TokenInfo tokenInfo) {

        if (request == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (tokenInfo == null)
            throw new ApiException(CommonErrorCode.COMMON_400_003);

        return new NoticeDeleteParam(
            request.getNoticeId()
            , request.getEditPwd()
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
            , tokenInfo.gv_authCd()
        );
    }
}
