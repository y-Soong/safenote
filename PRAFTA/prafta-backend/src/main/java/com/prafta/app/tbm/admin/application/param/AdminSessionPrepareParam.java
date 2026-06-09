package com.prafta.app.tbm.admin.application.param;

import com.prafta.app.tbm.admin.dto.request.AdminSessionPrepareRequest;
import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * 교육준비(OPENED) 전이 파라미터(prafta-051 R-A, E2).
 *
 * <p>sessionCd 는 path, 관리자 좌표는 바디에서 받되 식별자(회사/사용자/사업장/권한)는 JWT 클레임에서만
 * 도출한다(IDOR 차단).
 */
public record AdminSessionPrepareParam(
    String sessionCd
    , String managerGpsLat
    , String managerGpsLon
    , String gvCmpnyCd
    , String gvUserCd
    , String gvSiteCd
    , String gvAuthCd
){
    public static AdminSessionPrepareParam from(
            String sessionCd, AdminSessionPrepareRequest request, TokenInfo tokenInfo) {

        if (tokenInfo == null)
            throw new ApiException(CommonErrorCode.COMMON_400_003);

        String lat = request != null ? request.getManagerGpsLat() : null;
        String lon = request != null ? request.getManagerGpsLon() : null;

        return new AdminSessionPrepareParam(
            sessionCd
            , lat
            , lon
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
            , tokenInfo.gv_siteCd()
            , tokenInfo.gv_authCd()
        );
    }
}
