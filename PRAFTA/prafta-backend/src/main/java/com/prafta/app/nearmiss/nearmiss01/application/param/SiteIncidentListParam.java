package com.prafta.app.nearmiss.nearmiss01.application.param;

import com.prafta.app.nearmiss.nearmiss01.dto.request.SiteIncidentListRequest;
import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * A3 사업장 사건 목록 / A4 상태 카운트 Param (관리자).
 * siteCd 는 JWT gv_siteCd 로 캐노니컬라이즈(본문값 무시, cross-site IDOR 차단).
 */
public record SiteIncidentListParam(
    String siteCd
    , String reportStatusCd
    , String potentialSeverityCd
    , String startDate
    , String endDate
    , String gvCmpnyCd
    , String gvUserCd
    , String gvAuthCd
){
    public static SiteIncidentListParam from(SiteIncidentListRequest request, TokenInfo tokenInfo) {

        if (request == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (tokenInfo == null)
            throw new ApiException(CommonErrorCode.COMMON_400_003);

        return new SiteIncidentListParam(
            tokenInfo.gv_siteCd()
            , request.getReportStatusCd()
            , request.getPotentialSeverityCd()
            , request.getStartDate()
            , request.getEndDate()
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
            , tokenInfo.gv_authCd()
        );
    }
}
