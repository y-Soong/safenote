package com.prafta.app.leave.leaveflow.application.param;

import java.util.List;

import com.prafta.app.leave.leaveflow.dto.request.LeaveApplyRequest;
import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * prafta-app-018-B: 앱 연차 신청 Param.
 *
 * <p>웹 {@code com.prafta.web.attd.leaveflow.application.param.LeaveApplyParam} 미러.
 * 신청자(cmpny/site/user)는 토큰에서만 강제한다(요청 body 식별값 신뢰 금지 — IDOR 차단).
 *   nodeCd 는 본문으로 받지 않으며, 결재선 자기승인 판정은 서버 USER→NODE 조인으로 독립 수행하므로 불필요하다.
 *   approverUserCds/presetId 만 본문 신뢰(소유권은 서버 검증).
 */
public record LeaveApplyParam(
      String leaveCd
    , String leaveType
    , String workYmd
    , String useUnitType
    , String startTime
    , String endTime
    , String reason
    , List<String> approverUserCds
    , String presetId
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
            , request.getApproverUserCds()
            , request.getPresetId()
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_siteCd()
            , tokenInfo.gv_userCd()
        );
    }
}
