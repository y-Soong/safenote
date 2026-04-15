package com.prafta.common.cmm.login.application.command;

import com.prafta.common.cmm.login.application.param.UserJoinParam;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

public record UserJoinCommand(
	String cmpnyCd
	, String userCd
	, String userId
	, String userPw
	, String userNm
	, String siteCd
	, String nodeCd
	, String authCd
	, String mblNoEnc
	, String mblNoHmac
	, String mblNoLast4
	, String emailEnc
	, String emailHmac
	, String emailDomain
	, String birthDtEnc
	, String gender
	, String useYn
) {
	public static UserJoinCommand from(
			UserJoinParam param
			, String userCd
			, String userPw
			, String phoneEnc
			, String phoneHmac
			, String phoneLast4
			, String emailEnc
			, String emailHmac
			, String emailDomain
			, String birthEnc
	) {
		if(param == null)
			throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - UserJoinParam");
		if(userCd == null)
			throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - userCd");
		if(userPw == null)
			throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - userPw");
		
		return new UserJoinCommand(
			param.cmpnyCd()
			, userCd
			, param.userId()
			, userPw
			, param.userNm()
			, param.siteCd()
			, param.nodeCd()
			, param.authCd()
			, phoneEnc
			, phoneHmac
			, phoneLast4
			, emailEnc
			, emailHmac
			, emailDomain
			, birthEnc
			, param.gender()
			, param.useYn()
        );
    }
}
