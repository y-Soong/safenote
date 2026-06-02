package com.prafta.web.attd.leaveflow.application.param;

import java.util.List;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.attd.leaveflow.dto.request.LeaveApplyRequest;

/**
 * 연차 신청 Param (prafta-019-E). 신청자(userCd)·회사·사업장은 토큰에서 강제(요청 body 신뢰 금지).
 */
public record LeaveApplyParam(
      String leaveCd
    , String leaveType
    , String workYmd
    , String useUnitType
    , String startTime
    , String endTime
    , String reason
    , String nodeCd
    , List<String> approverUserCds
    , String gvCmpnyCd
    , String gvSiteCd
    , String gvUserCd
) {
    public static LeaveApplyParam from(LeaveApplyRequest request, TokenInfo tokenInfo) {
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
        return new LeaveApplyParam(
              request.getLeaveCd()
            , request.getLeaveType()
            , request.getWorkYmd()
            , request.getUseUnitType()
            , request.getStartTime()
            , request.getEndTime()
            , request.getReason()
            , request.getNodeCd()
            , request.getApproverUserCds()
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_siteCd()
            , tokenInfo.gv_userCd()
        );
    }
}
