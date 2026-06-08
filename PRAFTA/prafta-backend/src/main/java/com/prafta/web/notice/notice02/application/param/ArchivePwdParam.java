package com.prafta.web.notice.notice02.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.notice.notice02.dto.request.ArchivePwdRequest;

public record ArchivePwdParam(
    String noticeId
    , String editPwd
    , String gvCmpnyCd
    , String gvUserCd
    , String gvAuthCd
){
    public static ArchivePwdParam from(ArchivePwdRequest request, TokenInfo tokenInfo) {

        if (request == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (tokenInfo == null)
            throw new ApiException(CommonErrorCode.COMMON_400_003);

        return new ArchivePwdParam(
            request.getNoticeId()
            , request.getEditPwd()
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
            , tokenInfo.gv_authCd()
        );
    }
}
