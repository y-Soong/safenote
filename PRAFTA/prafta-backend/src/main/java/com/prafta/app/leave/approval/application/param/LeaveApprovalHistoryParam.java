package com.prafta.app.leave.approval.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * 사용자연차결재-01 (3-C 이력): 내가 처리한 연차 결재 내역 조회 Param.
 *
 * <p>startDate/endDate(YYYYMMDD, 기본 최근 30일은 서비스가 보정) + keyword 만 수신한다.
 * 식별자(cmpny/user)는 토큰에서만 운반한다(IDOR 차단).
 */
public record LeaveApprovalHistoryParam(
      String startDate
    , String endDate
    , String keyword
    , String gvCmpnyCd
    , String gvUserCd
) {
    public static LeaveApprovalHistoryParam of(String startDate, String endDate, String keyword, TokenInfo token) {
        if (token == null || token.gv_cmpnyCd() == null || token.gv_userCd() == null) {
            throw new ApiException(CommonErrorCode.COMMON_400_003);
        }
        String s = (startDate == null || startDate.isBlank()) ? null : startDate.trim();
        String e = (endDate == null || endDate.isBlank()) ? null : endDate.trim();
        String kw = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        return new LeaveApprovalHistoryParam(s, e, kw, token.gv_cmpnyCd(), token.gv_userCd());
    }
}
