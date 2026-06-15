package com.prafta.app.notice.notice02.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.app.notice.notice02.dto.request.AppArchiveInfoRequest;

/**
 * 앱 자료실 단건 상세 파라미터. cmpnyCd 는 JWT 에서만 도출(IDOR 차단).
 */
public record AppArchiveInfoParam(
    String noticeId
    , String gvCmpnyCd
    , String gvUserCd
    , String gvAuthCd
){
    public static AppArchiveInfoParam from(AppArchiveInfoRequest request, TokenInfo tokenInfo) {

        if (request == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (tokenInfo == null)
            throw new ApiException(CommonErrorCode.COMMON_400_003);

        return new AppArchiveInfoParam(
            request.getNoticeId()
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
            , tokenInfo.gv_authCd()
        );
    }
}
