package com.prafta.web.user.user10.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.user.user10.dto.request.StdWorkUserListRequest;

/**
 * 소정-10: 소정근로시간 관리 대상 목록 조회 파라미터.
 *
 * <p>회사/요청자/권한/토큰 사업장은 JWT 클레임에서만 도출한다. 조회 사업장 인가와 부서 스코프는
 * 서비스 계층에서 {@code SiteAccessService} + {@code canManageNodeExcludeSafe} 로 강제한다.
 */
public record StdWorkUserListParam(
        String siteCd
        , String nodeCd
        , String incSubNodeYn
        , String userKeyword
        , String gvCmpnyCd
        , String gvAuthCd
        , String gvUserCd
        , String gvSiteCd
) {
    public static StdWorkUserListParam from(StdWorkUserListRequest request, TokenInfo tokenInfo) {

        if (request == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (tokenInfo == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (request.getSiteCd() == null || request.getSiteCd().isBlank())
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (tokenInfo.gv_cmpnyCd() == null || tokenInfo.gv_cmpnyCd().isEmpty())
            throw new ApiException(CommonErrorCode.COMMON_400_003);

        return new StdWorkUserListParam(
                request.getSiteCd()
                , request.getNodeCd()
                , "Y".equals(request.getIncSubNodeYn()) ? "Y" : "N"
                , request.getUserKeyword()
                , tokenInfo.gv_cmpnyCd()
                , tokenInfo.gv_authCd()
                , tokenInfo.gv_userCd()
                , tokenInfo.gv_siteCd()
        );
    }
}
