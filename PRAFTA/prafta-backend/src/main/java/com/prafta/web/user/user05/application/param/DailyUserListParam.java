package com.prafta.web.user.user05.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.user.user05.dto.request.DailyUserListRequest;

/**
 * 일일사용자 관리(조회) 검색 파라미터.
 *
 * <p>JWT 클레임 도출값(gvCmpnyCd/gvUserCd/gvAuthCd)만 신뢰한다(클라 바디 신뢰 금지).
 * mblNo 는 평문 입력이며, 서비스에서 정규화→HMAC/LAST4 파생값으로 변환해 쿼리에 전달한다.
 */
public record DailyUserListParam(
    String siteCd
    , String nodeCd
    , String incSubNodeYn
    , String userNm
    , String mblNo
    , String occupyFrom
    , String occupyTo
    , String gvCmpnyCd
    , String gvUserCd
    , String gvAuthCd
){
    public static DailyUserListParam from(DailyUserListRequest request, TokenInfo tokenInfo) {

        if (request == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (tokenInfo == null)
            throw new ApiException(CommonErrorCode.COMMON_400_003);

        return new DailyUserListParam(
            request.getSiteCd()
            , request.getNodeCd()
            , request.getIncSubNodeYn()
            , request.getUserNm()
            , request.getMblNo()
            , request.getOccupyFrom()
            , request.getOccupyTo()
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
            , tokenInfo.gv_authCd()
        );
    }
}
