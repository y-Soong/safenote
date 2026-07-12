package com.prafta.common.cmm.login.application.param;

import java.util.List;
import java.util.Objects;

import com.prafta.common.cmm.login.dto.request.AuthMenuInfoRequest;
import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

public record AuthMenuInfoParam(
        List<String> systValDCdList
        , String cmpnyCd
        , String userId
) {
    public static AuthMenuInfoParam from(List<AuthMenuInfoRequest> requests, TokenInfo tokenInfo) {
        
        if(requests == null)
			throw new ApiException(CommonErrorCode.COMMON_400_001);
		if(tokenInfo == null)
			throw new ApiException(CommonErrorCode.COMMON_400_003);

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

        // cmpnyCd/userId 모두 JWT 클레임에서만 도출(회사 스코프 약관 동의 적재, IDOR 차단).
        return new AuthMenuInfoParam(
    		codes
    		, tokenInfo.gv_cmpnyCd()
    		, tokenInfo.gv_userCd()
		);
    }
}