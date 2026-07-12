package com.prafta.common.cmm.menu.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * 내 즐겨찾기 목록 조회 파라미터. cmpnyCd/userCd 는 JWT 도출값만 신뢰(IDOR 방지).
 */
public record MenuFavoriteListParam(
	String cmpnyCd
	, String userCd
) {
	public static MenuFavoriteListParam from(TokenInfo tokenInfo) {

		if (tokenInfo == null)
			throw new ApiException(CommonErrorCode.COMMON_400_003);

		return new MenuFavoriteListParam(
			tokenInfo.gv_cmpnyCd()
			, tokenInfo.gv_userCd()
		);
	}
}
