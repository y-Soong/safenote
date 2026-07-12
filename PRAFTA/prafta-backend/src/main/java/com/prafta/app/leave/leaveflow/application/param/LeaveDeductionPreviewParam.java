package com.prafta.app.leave.leaveflow.application.param;

import com.prafta.app.leave.leaveflow.dto.request.LeaveDeductionPreviewRequest;
import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * 앱 예상 차감액 미리보기 Param (LC-07). 웹 미러.
 * 신청자(userCd)·회사·사업장은 토큰에서 강제(본문 식별값 신뢰 금지 — IDOR 차단).
 */
public record LeaveDeductionPreviewParam(
      String leaveCd
    , String workYmd
    , String useUnitType
    , String startTime
    , String endTime
    , String gvCmpnyCd
    , String gvSiteCd
    , String gvUserCd
) {
    public static LeaveDeductionPreviewParam from(LeaveDeductionPreviewRequest request, TokenInfo tokenInfo) {
        if (request == null || tokenInfo == null) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        if (tokenInfo.gv_cmpnyCd() == null || tokenInfo.gv_cmpnyCd().isEmpty()
                || tokenInfo.gv_siteCd() == null || tokenInfo.gv_siteCd().isEmpty()
                || tokenInfo.gv_userCd() == null || tokenInfo.gv_userCd().isEmpty()) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        if (request.getLeaveCd() == null || request.getLeaveCd().isBlank()
                || request.getWorkYmd() == null || request.getWorkYmd().isBlank()
                || request.getUseUnitType() == null || request.getUseUnitType().isBlank()) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        return new LeaveDeductionPreviewParam(
              request.getLeaveCd()
            , request.getWorkYmd()
            , request.getUseUnitType()
            , request.getStartTime()
            , request.getEndTime()
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_siteCd()
            , tokenInfo.gv_userCd()
        );
    }
}
