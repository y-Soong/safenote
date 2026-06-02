package com.prafta.app.nearmiss.nearmiss01.application.param;

import com.prafta.app.nearmiss.nearmiss01.dto.request.IncidentDetailRequest;
import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * A5 사건 단건 상세 Param.
 *
 * <p>접근 주체 판정(D-A2): 본인 보고건(REPORTER_ID=gvUserCd) OR 사업장 권한 보유 관리자.
 *    siteCd 는 JWT gv_siteCd 로 캐노니컬라이즈(본문값 무시).
 */
public record IncidentDetailParam(
    String siteCd
    , String nearMissId
    , String gvCmpnyCd
    , String gvUserCd
    , String gvAuthCd
){
    public static IncidentDetailParam from(IncidentDetailRequest request, TokenInfo tokenInfo) {

        if (request == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (tokenInfo == null)
            throw new ApiException(CommonErrorCode.COMMON_400_003);

        return new IncidentDetailParam(
            tokenInfo.gv_siteCd()
            , request.getNearMissId()
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
            , tokenInfo.gv_authCd()
        );
    }
}
