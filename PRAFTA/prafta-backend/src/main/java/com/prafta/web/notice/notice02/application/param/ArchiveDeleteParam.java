package com.prafta.web.notice.notice02.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.notice.notice02.dto.request.ArchiveDeleteRequest;

public record ArchiveDeleteParam(
    String noticeId
    , String editPwd
    , String gvCmpnyCd
    , String gvUserCd
    , String gvAuthCd
){
    public static ArchiveDeleteParam from(ArchiveDeleteRequest request, TokenInfo tokenInfo) {

        if (request == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (tokenInfo == null)
            throw new ApiException(CommonErrorCode.COMMON_400_003);

        return new ArchiveDeleteParam(
            request.getNoticeId()
            , request.getEditPwd()
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
            , tokenInfo.gv_authCd()
        );
    }
}
