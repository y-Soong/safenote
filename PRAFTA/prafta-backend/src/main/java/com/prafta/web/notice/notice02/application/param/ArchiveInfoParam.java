package com.prafta.web.notice.notice02.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.notice.notice02.dto.request.ArchiveInfoRequest;

public record ArchiveInfoParam(
    String noticeId
    , String gvCmpnyCd
    , String gvUserCd
    , String gvAuthCd
){
    public static ArchiveInfoParam from(ArchiveInfoRequest request, TokenInfo tokenInfo) {

        if (request == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (tokenInfo == null)
            throw new ApiException(CommonErrorCode.COMMON_400_003);

        return new ArchiveInfoParam(
            request.getNoticeId()
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
            , tokenInfo.gv_authCd()
        );
    }
}
