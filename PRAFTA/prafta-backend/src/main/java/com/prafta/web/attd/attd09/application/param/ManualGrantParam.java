package com.prafta.web.attd.attd09.application.param;

import java.util.Collections;
import java.util.List;

import com.prafta.common.cmm.leave.command.ManualGrantCommand;
import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.attd.attd09.dto.request.BulkManualGrantRequest;
import com.prafta.web.attd.attd09.dto.request.ManualGrantRequest;

/**
 * 연차 수동 부여(단일/일괄 공통) 진입 Param.
 *
 * <p>JWT 클레임(권한/회사/수행자)을 함께 운반하여 서비스 계층이 권한 가드 + 스코프 격리를
 * 수행할 수 있도록 한다(정책서 §8.5.7 + PRAFTA-017 권한 가드 패턴).
 *
 * <p>cmpnyCd는 JWT에서만 취득(요청 body의 cmpnyCd 미신뢰 — 가드레일 3).
 */
public record ManualGrantParam(
      ManualGrantCommand command
    , String gvCmpnyCd
    , String gvUserCd
    , String gvAuthCd
) {

    /** 단일 부여 요청 → Param. */
    public static ManualGrantParam fromSingle(ManualGrantRequest request, TokenInfo tokenInfo) {
        validateToken(tokenInfo);
        if (request == null) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        List<String> userCds = (request.getUserCd() == null)
                ? Collections.emptyList()
                : Collections.singletonList(request.getUserCd());

        ManualGrantCommand command = new ManualGrantCommand(
              userCds
            , request.getLeaveCd()
            , request.getGrantDays()
            , request.getAvailFromDate()
            , request.getReason()
        );
        return build(command, tokenInfo);
    }

    /** 일괄 부여 요청 → Param. */
    public static ManualGrantParam fromBulk(BulkManualGrantRequest request, TokenInfo tokenInfo) {
        validateToken(tokenInfo);
        if (request == null) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        ManualGrantCommand command = new ManualGrantCommand(
              request.getUserCds()
            , request.getLeaveCd()
            , request.getGrantDays()
            , request.getAvailFromDate()
            , request.getReason()
        );
        return build(command, tokenInfo);
    }

    private static void validateToken(TokenInfo tokenInfo) {
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
    }

    private static ManualGrantParam build(ManualGrantCommand command, TokenInfo tokenInfo) {
        return new ManualGrantParam(
              command
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
            , tokenInfo.gv_authCd()
        );
    }

    public ManualGrantCommand toCommand() {
        return command;
    }
}
