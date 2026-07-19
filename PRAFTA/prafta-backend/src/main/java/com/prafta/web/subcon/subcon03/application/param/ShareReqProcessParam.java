package com.prafta.web.subcon.subcon03.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.subcon.subcon03.dto.request.ShareReqProcessRequest;

/**
 * 데이터 공유 요청 상태 전이(취소/거부) 파라미터.
 *
 * <p>행위자 소속 회사(gvCmpnyCd)/행위자(gvUserCd)는 JWT 클레임에서만 도출한다.
 * 당사자 조건(취소=REQ_CMPNY_CD, 거부=PRV_CMPNY_CD)은 매퍼 조건부 UPDATE 로 강제한다.
 */
public record ShareReqProcessParam(
    Long shareReqId
    , String comment
    , String gvCmpnyCd
    , String gvUserCd
    , String gvAuthCd
){
    public static ShareReqProcessParam from(ShareReqProcessRequest request, TokenInfo tokenInfo) {

        if (request == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (tokenInfo == null)
            throw new ApiException(CommonErrorCode.COMMON_400_003);

        return new ShareReqProcessParam(
            request.getShareReqId()
            , request.getComment()
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
            , tokenInfo.gv_authCd()
        );
    }
}
