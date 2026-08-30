package com.prafta.web.tbm.tbm04.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * tbm04-manager-sign — 주관자 서명 이미지 스트림 파라미터.
 *
 * <p>클라는 sessionCd 만 보내고, 파일 식별자(MANAGER_SIGN_FILE_MGMT_CD)는 서버가 세션 행에서
 * 재조회한다(클라 파일코드 신뢰 금지 — IDOR 방지). W-13 attendance-sign-image 패턴 미러.
 */
public record ManagerSignImageParam(
	String sessionCd
	, String gvCmpnyCd
	, String gvSiteCd
	, String gvAuthCd
	, String gvUserCd
){
	public static ManagerSignImageParam from(String sessionCd, TokenInfo tokenInfo) {

		if (tokenInfo == null)
			throw new ApiException(CommonErrorCode.COMMON_400_003);
		if (sessionCd == null || sessionCd.isBlank() || sessionCd.length() > 50)
			throw new ApiException(CommonErrorCode.COMMON_400_001);

		return new ManagerSignImageParam(
			sessionCd
			, tokenInfo.gv_cmpnyCd()
			, tokenInfo.gv_siteCd()
			, tokenInfo.gv_authCd()
			, tokenInfo.gv_userCd()
		);
	}
}
