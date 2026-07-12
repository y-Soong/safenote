package com.prafta.app.tbm.admin.application.param;

import java.util.Collections;
import java.util.List;

import com.prafta.app.tbm.admin.application.model.AdminSessionContentModel;
import com.prafta.app.tbm.admin.application.model.AdminSessionRiskModel;
import com.prafta.app.tbm.admin.dto.request.AdminSessionUpdateRequest;
import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * T-A4 관리자 TBM 세션 수정 파라미터. sessionCd 는 path 에서 받는다.
 */
public record AdminSessionUpdateParam(
    String sessionCd
    , String title
    , String contentBody
    , String gpsVerifyTypeCd
    , String managerGpsLat
    , String managerGpsLon
    , Integer gpsVerifyRadiusM
    , Integer eduMinutes
    , String gpsManualConfirmYn
    , List<AdminSessionContentModel> contents
    , List<AdminSessionRiskModel> risks
    , String gvCmpnyCd
    , String gvUserCd
    , String gvSiteCd
    , String gvAuthCd
){
    public static AdminSessionUpdateParam from(String sessionCd, AdminSessionUpdateRequest request, TokenInfo tokenInfo) {

        if (request == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (tokenInfo == null)
            throw new ApiException(CommonErrorCode.COMMON_400_003);

        return new AdminSessionUpdateParam(
            sessionCd
            , request.getTitle()
            , request.getContentBody()
            , request.getGpsVerifyTypeCd()
            , request.getManagerGpsLat()
            , request.getManagerGpsLon()
            , request.getGpsVerifyRadiusM()
            , request.getEduMinutes()
            , request.getGpsManualConfirmYn()
            , request.getContents() != null ? request.getContents() : Collections.emptyList()
            , request.getRisks() != null ? request.getRisks() : Collections.emptyList()
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
            , tokenInfo.gv_siteCd()
            , tokenInfo.gv_authCd()
        );
    }
}
