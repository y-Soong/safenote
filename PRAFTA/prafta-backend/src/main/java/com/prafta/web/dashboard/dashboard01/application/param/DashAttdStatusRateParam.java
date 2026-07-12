package com.prafta.web.dashboard.dashboard01.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.dashboard.dashboard01.dto.request.DashAttdStatusRateRequest;

/**
 * 대시보드 근태 탭 A2 정상/비정상 근무율 조회 파라미터 (PRAFTA-DASHBOARD-T2).
 * siteCd/workYm 필수 + workYm 형식(YYYY-MM) 검증, incSubNodeYn 은 'Y' 외 전부 'N' 정규화.
 */
public record DashAttdStatusRateParam(
    String siteCd
    , String nodeCd
    , String incSubNodeYn
    , String workYm
    , String gvCmpnyCd
    , String gvUserCd
    , String gvAuthCd
){
    public static DashAttdStatusRateParam from(DashAttdStatusRateRequest request, TokenInfo tokenInfo) {

        if (request == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (tokenInfo == null)
            throw new ApiException(CommonErrorCode.COMMON_400_003);

        // siteCd 필수 (공백 차단)
        if (request.getSiteCd() == null || request.getSiteCd().isBlank())
            throw new ApiException(CommonErrorCode.COMMON_400_001);

        // workYm 필수 + 'YYYY-MM' 형식 검증 (월 프리픽스 검색에 사용)
        if (request.getWorkYm() == null || !request.getWorkYm().matches("\\d{4}-(0[1-9]|1[0-2])"))
            throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new DashAttdStatusRateParam(
            request.getSiteCd()
            , request.getNodeCd()
            // 'Y' 만 하위부서 확장, 그 외 값(null 포함)은 전부 'N' 으로 정규화
            , "Y".equals(request.getIncSubNodeYn()) ? "Y" : "N"
            , request.getWorkYm()
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
            , tokenInfo.gv_authCd()
        );
    }
}
