package com.prafta.common.cmm.menu.application.param;

import com.prafta.common.cmm.menu.dto.request.MenuFavoriteToggleRequest;
import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * 즐겨찾기 토글 파라미터. menuDId 는 body 에서, cmpnyCd/userCd 는 JWT 도출값만 신뢰(IDOR 방지).
 */
public record MenuFavoriteToggleParam(
	String cmpnyCd
	, String userCd
	, String menuDId
) {
	public static MenuFavoriteToggleParam from(MenuFavoriteToggleRequest request, TokenInfo tokenInfo) {

		if (request == null || request.getMenuDId() == null || request.getMenuDId().isBlank())
			throw new ApiException(CommonErrorCode.COMMON_400_001);
		if (tokenInfo == null)
			throw new ApiException(CommonErrorCode.COMMON_400_003);

		return new MenuFavoriteToggleParam(
			tokenInfo.gv_cmpnyCd()
			, tokenInfo.gv_userCd()
			, request.getMenuDId().trim()
		);
	}
}
