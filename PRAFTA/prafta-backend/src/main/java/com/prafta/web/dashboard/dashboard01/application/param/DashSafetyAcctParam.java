package com.prafta.web.dashboard.dashboard01.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.dashboard.dashboard01.dto.request.DashSafetyAcctRequest;

/**
 * 대시보드 안전 탭 무사고 배너 + 사고 summary 조회 파라미터 (PRAFTA-DASHBOARD-T4).
 * siteCd/ym 필수 + ym 형식(YYYY-MM) 검증.
 */
public record DashSafetyAcctParam(
    String siteCd
    , String ym
    , String gvCmpnyCd
    , String gvUserCd
    , String gvAuthCd
){
    public static DashSafetyAcctParam from(DashSafetyAcctRequest request, TokenInfo tokenInfo) {

        if (request == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (tokenInfo == null)
            throw new ApiException(CommonErrorCode.COMMON_400_003);

        // siteCd 필수 (공백 차단)
        if (request.getSiteCd() == null || request.getSiteCd().isBlank())
            throw new ApiException(CommonErrorCode.COMMON_400_001);

        // ym 필수 + 'YYYY-MM' 형식 검증 (등급 카운트 기간 산출에 사용)
        if (request.getYm() == null || !request.getYm().matches("\\d{4}-(0[1-9]|1[0-2])"))
            throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new DashSafetyAcctParam(
            request.getSiteCd()
            , request.getYm()
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
            , tokenInfo.gv_authCd()
        );
    }
}
