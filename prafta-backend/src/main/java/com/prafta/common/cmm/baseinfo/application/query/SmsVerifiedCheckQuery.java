package com.prafta.common.cmm.baseinfo.application.query;

import com.prafta.common.cmm.baseinfo.application.param.UserPasswordParam;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * 비밀번호 재설정(update-user-password) 진입 시 대상 사용자의
 * 최근 SMS 인증 성공 레코드를 조회하기 위한 Query.
 * 대상 사용자는 cmpnyCd + userCd 로 식별하고, 매퍼에서 TB_USER 와 조인하여
 * 해당 사용자의 MBL_NO_HMAC 에 대한 인증 레코드를 검증한다.
 */
public record SmsVerifiedCheckQuery(
	String cmpnyCd
	, String userCd
) {
	public static SmsVerifiedCheckQuery from(UserPasswordParam param) {

		if(param == null)
			throw new ApiException(CommonErrorCode.COMMON_400_001);
		if(param.cmpnyCd() == null || param.cmpnyCd().isBlank())
			throw new ApiException(CommonErrorCode.COMMON_400_001);
		if(param.userCd() == null || param.userCd().isBlank())
			throw new ApiException(CommonErrorCode.COMMON_400_001);

		return new SmsVerifiedCheckQuery(
			param.cmpnyCd()
			, param.userCd()
		);
	}
}
