package com.prafta.web.notice.notice02.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.notice.notice02.dto.request.ArchiveListRequest;

public record ArchiveListParam(
    String archiveTypeCd
    , String titleKeyword
    , String startDate
    , String endDate
    , String gvCmpnyCd
    , String gvUserCd
    , String gvAuthCd
){
    public static ArchiveListParam from(ArchiveListRequest request, TokenInfo tokenInfo) {

        if (request == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (tokenInfo == null)
            throw new ApiException(CommonErrorCode.COMMON_400_003);

        return new ArchiveListParam(
            request.getArchiveTypeCd()
            , request.getTitleKeyword()
            , request.getStartDate()
            , request.getEndDate()
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
            , tokenInfo.gv_authCd()
        );
    }
}
