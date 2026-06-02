package com.prafta.app.nearmiss.nearmiss01.application.param;

import com.prafta.app.nearmiss.nearmiss01.dto.request.ChangeStatusRequest;
import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * A6 1차 확인 상태전환 Param (관리자). 앱은 100->200 만 허용(plan §4.5).
 * siteCd 는 JWT gv_siteCd 로 캐노니컬라이즈(본문값 무시).
 */
public record ChangeStatusParam(
    String siteCd
    , String nearMissId
    , String reportStatusCd
    , String adminTempActionDesc
    , String rejectReason
    , String gvCmpnyCd
    , String gvUserCd
    , String gvAuthCd
){
    public static ChangeStatusParam from(ChangeStatusRequest request, TokenInfo tokenInfo) {

        if (request == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (tokenInfo == null)
            throw new ApiException(CommonErrorCode.COMMON_400_003);

        return new ChangeStatusParam(
            tokenInfo.gv_siteCd()
            , request.getNearMissId()
            , request.getReportStatusCd()
            , request.getAdminTempActionDesc()
            , request.getRejectReason()
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
            , tokenInfo.gv_authCd()
        );
    }
}
