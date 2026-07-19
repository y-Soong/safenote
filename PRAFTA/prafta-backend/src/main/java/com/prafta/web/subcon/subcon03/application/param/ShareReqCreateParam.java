package com.prafta.web.subcon.subcon03.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.subcon.subcon03.dto.request.ShareReqCreateRequest;

/**
 * 데이터 공유 요청 생성 파라미터(PRAFTA-SUBCON-T3 §5-3).
 *
 * <p>요청측 회사(gvCmpnyCd)/요청자(gvUserCd)는 JWT 클레임에서만 도출한다.
 * 관계 ACCEPTED·사업장 소유·체인 해석·기간·중복 가드는 서비스가 서버 강제한다.
 */
public record ShareReqCreateParam(
    String prvCmpnyCd
    , String siteCd
    , String dataType
    , String periodStr
    , String periodEnd
    , String closedOnlyYn
    , String purpose
    , String gvCmpnyCd
    , String gvUserCd
    , String gvAuthCd
){
    public static ShareReqCreateParam from(ShareReqCreateRequest request, TokenInfo tokenInfo) {

        if (request == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (tokenInfo == null)
            throw new ApiException(CommonErrorCode.COMMON_400_003);

        return new ShareReqCreateParam(
            request.getPrvCmpnyCd()
            , request.getSiteCd()
            , request.getDataType()
            , request.getPeriodStr()
            , request.getPeriodEnd()
            , request.getClosedOnlyYn()
            , request.getPurpose()
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
            , tokenInfo.gv_authCd()
        );
    }
}
