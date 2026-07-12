package com.prafta.web.dashboard.dashboard01.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.dashboard.dashboard01.dto.request.DashSafetyRequest;

/**
 * 대시보드 안전 탭 위젯 공용 조회 파라미터 (PRAFTA-DASHBOARD-T5).
 * siteCd/ym 필수 + ym 형식(YYYY-MM) 검증 — DashSafetyAcctParam(T4)과 동일 검증 축.
 * gvAuthCd 는 사업장 접근 게이트(assertSiteAccess)의 전사 권한 판정에 사용.
 */
public record DashSafetyParam(
    String siteCd
    , String ym
    , String gvCmpnyCd
    , String gvUserCd
    , String gvAuthCd
){
    public static DashSafetyParam from(DashSafetyRequest request, TokenInfo tokenInfo) {

        if (request == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (tokenInfo == null)
            throw new ApiException(CommonErrorCode.COMMON_400_003);

        // siteCd 필수 (공백 차단)
        if (request.getSiteCd() == null || request.getSiteCd().isBlank())
            throw new ApiException(CommonErrorCode.COMMON_400_001);

        // ym 필수 + 'YYYY-MM' 형식 검증 (월 미이행/아차사고/TBM 추이 기간 산출에 사용)
        if (request.getYm() == null || !request.getYm().matches("\\d{4}-(0[1-9]|1[0-2])"))
            throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new DashSafetyParam(
            request.getSiteCd()
            , request.getYm()
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
            , tokenInfo.gv_authCd()
        );
    }
}
