package com.prafta.web.dashboard.dashboard01.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.dashboard.dashboard01.dto.request.LeaveUsageRequest;

/**
 * 대시보드 근태 탭 A4 법정연차 3분할 조회 파라미터 (PRAFTA-DASHBOARD-T3).
 * siteCd 필수, incSubNodeYn 은 'Y' 외 전부 'N' 정규화. 현재 시점 스냅샷 — baseYm 미사용.
 */
public record LeaveUsageParam(
    String siteCd
    , String nodeCd
    , String incSubNodeYn
    , String gvCmpnyCd
    , String gvUserCd
    , String gvAuthCd
){
    public static LeaveUsageParam from(LeaveUsageRequest request, TokenInfo tokenInfo) {

        if (request == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (tokenInfo == null)
            throw new ApiException(CommonErrorCode.COMMON_400_003);

        // siteCd 필수 (공백 차단)
        if (request.getSiteCd() == null || request.getSiteCd().isBlank())
            throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new LeaveUsageParam(
            request.getSiteCd()
            , request.getNodeCd()
            // 'Y' 만 하위부서 확장, 그 외 값(null 포함)은 전부 'N' 으로 정규화
            , "Y".equals(request.getIncSubNodeYn()) ? "Y" : "N"
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
            , tokenInfo.gv_authCd()
        );
    }
}
