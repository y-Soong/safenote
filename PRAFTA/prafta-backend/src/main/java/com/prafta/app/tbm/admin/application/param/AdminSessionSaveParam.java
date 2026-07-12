package com.prafta.app.tbm.admin.application.param;

import java.util.Collections;
import java.util.List;

import com.prafta.app.tbm.admin.application.model.AdminSessionContentModel;
import com.prafta.app.tbm.admin.application.model.AdminSessionRiskModel;
import com.prafta.app.tbm.admin.dto.request.AdminSessionSaveRequest;
import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * T-A3 관리자 TBM 세션 개설/임시저장 파라미터.
 *
 * <p>siteCd 는 바디로 받되 서버가 접근가능 사업장(USE_YN='Y')인지 검증 후 채택한다(T6).
 * 그 외 식별자(cmpnyCd/userCd/authCd)는 JWT 클레임에서만 도출한다(D1).
 */
public record AdminSessionSaveParam(
    String saveMode
    , String siteCd
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
    public static AdminSessionSaveParam from(AdminSessionSaveRequest request, TokenInfo tokenInfo) {

        if (request == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (tokenInfo == null)
            throw new ApiException(CommonErrorCode.COMMON_400_003);

        return new AdminSessionSaveParam(
            request.getSaveMode()
            , request.getSiteCd()
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
