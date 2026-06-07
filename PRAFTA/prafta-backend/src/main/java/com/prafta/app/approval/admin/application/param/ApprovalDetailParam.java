package com.prafta.app.approval.admin.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * 001-P2-B3: 앱 관리자 승인 상세(A-2) 조회 Param.
 *
 * <p>reqId 는 리소스 키이며 식별자가 아니다. 서버가 토큰 스코프 내인지 재검증한다(IDOR 차단).
 */
public record ApprovalDetailParam(
      String reqId
    , String group
    , String gvCmpnyCd
    , String gvUserCd
    , String gvSiteCd
    , String gvAuthCd
) {
    public static ApprovalDetailParam of(String reqId, String group, TokenInfo token) {
        if (token == null || token.gv_cmpnyCd() == null || token.gv_userCd() == null) {
            throw new ApiException(CommonErrorCode.COMMON_400_003);
        }
        if (reqId == null || reqId.isBlank()) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        return new ApprovalDetailParam(reqId.trim(),
                (group == null || group.isBlank()) ? null : group.trim().toUpperCase(),
                token.gv_cmpnyCd(), token.gv_userCd(), token.gv_siteCd(), token.gv_authCd());
    }
}
