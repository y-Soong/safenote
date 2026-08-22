package com.prafta.web.attd.attd09.application.param;

import com.prafta.common.cmm.leave.command.CoverGrantCommand;
import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.attd.attd09.dto.request.CoverGrantRequest;

/**
 * 입사일 기준 차액 보전(법정 수기부여) 진입 Param (경력인정 이원화 Phase 2 §2-3).
 *
 * <p>cmpnyCd는 JWT에서만 취득(요청 body의 cmpnyCd 미신뢰 — 가드레일 3).
 */
public record CoverGrantParam(
      CoverGrantCommand command
    , String gvCmpnyCd
    , String gvUserCd
    , String gvAuthCd
) {

    public static CoverGrantParam from(CoverGrantRequest request, TokenInfo tokenInfo) {
        if (tokenInfo == null) {
            throw new ApiException(CommonErrorCode.COMMON_400_003);
        }
        if (tokenInfo.gv_cmpnyCd() == null || tokenInfo.gv_cmpnyCd().isEmpty()) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        if (tokenInfo.gv_userCd() == null || tokenInfo.gv_userCd().isEmpty()) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        if (tokenInfo.gv_authCd() == null || tokenInfo.gv_authCd().isEmpty()) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        if (request == null) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }

        CoverGrantCommand command = new CoverGrantCommand(
              request.getUserCd()
            , request.getGrantDays()
            , request.getReason()
            , request.getBaseYmd()
        );

        return new CoverGrantParam(
              command
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
            , tokenInfo.gv_authCd()
        );
    }
}
