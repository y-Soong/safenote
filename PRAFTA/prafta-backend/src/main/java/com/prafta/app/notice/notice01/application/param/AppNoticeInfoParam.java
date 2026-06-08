package com.prafta.app.notice.notice01.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.app.notice.notice01.dto.request.AppNoticeInfoRequest;

/**
 * 앱 공지 상세 조회 파라미터.
 * cmpnyCd/userCd 는 JWT 에서만 도출(IDOR 차단). curSiteCd/curNodeCd 는 상세 진입 전 노출 대상 재검증용(§0.3).
 */
public record AppNoticeInfoParam(
    String noticeId
    , String gvCmpnyCd
    , String gvUserCd
    , String gvAuthCd
    , String curSiteCd
    , String curNodeCd
){
    public static AppNoticeInfoParam from(AppNoticeInfoRequest request, TokenInfo tokenInfo) {

        if (request == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (tokenInfo == null)
            throw new ApiException(CommonErrorCode.COMMON_400_003);

        return new AppNoticeInfoParam(
            request.getNoticeId()
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
            , tokenInfo.gv_authCd()
            , tokenInfo.gv_siteCd()
            , tokenInfo.gv_nodeCd()
        );
    }
}
