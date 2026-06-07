package com.prafta.web.notice.notice01.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.notice.notice01.dto.request.NoticeAckRequest;

/**
 * 공지 확인(CONFIRMED)/한시숨김(SNOOZED)/열람(read) 파라미터.
 * userCd 는 JWT 에서만 도출(IDOR 차단).
 */
public record NoticeAckParam(
    String noticeId
    , String gvCmpnyCd
    , String gvUserCd
    , String gvAuthCd
){
    public static NoticeAckParam from(NoticeAckRequest request, TokenInfo tokenInfo) {

        if (request == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (tokenInfo == null)
            throw new ApiException(CommonErrorCode.COMMON_400_003);

        return new NoticeAckParam(
            request.getNoticeId()
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
            , tokenInfo.gv_authCd()
        );
    }
}
