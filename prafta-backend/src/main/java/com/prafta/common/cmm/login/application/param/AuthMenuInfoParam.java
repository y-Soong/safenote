package com.prafta.common.cmm.login.application.param;

import java.util.List;
import java.util.Objects;

import com.prafta.common.cmm.login.dto.request.AuthMenuInfoRequest;
import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

public record AuthMenuInfoParam(
        List<String> systValDCdList
        , String userId
) {
    public static AuthMenuInfoParam from(List<AuthMenuInfoRequest> requests, TokenInfo tokenInfo) {
        
        if(requests == null)
			throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - AuthMenuInfoRequest");
		if(tokenInfo == null)
			throw ApiException.appendf(CommonErrorCode.COMMON_400_003,"\n필수값 누락 - TokenInfo");

        if (requests.isEmpty()) {
            throw new IllegalArgumentException("request list must not be empty");
        }
        if (requests.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("request item must not be null");
        }

        List<String> codes = requests.stream()
                .map(AuthMenuInfoRequest::getSystValDCd)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .toList();

        if (codes.isEmpty()) {
            throw new IllegalArgumentException("systValDCd is required");
        }

        return new AuthMenuInfoParam(
    		codes
    		, tokenInfo.gv_userCd()
		);
    }
}