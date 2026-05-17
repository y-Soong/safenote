package com.prafta.common.cmm.login.application.param;

import com.prafta.common.cmm.login.dto.request.UserJoinRequest;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

public record UserJoinParam(
	String cmpnyCd
	, String userId
	, String userPw
	, String userNm
	, String siteCd
	, String mblNo
	, String birthDt
	, String nodeCd
	, String authCd
	, String email
	, String gender
	, String useYn
) {
	public static UserJoinParam from(UserJoinRequest request) {
		
		if(request == null)
			throw new ApiException(CommonErrorCode.COMMON_400_001);
		
        return new UserJoinParam(
    		request.getCmpnyCd()
    		, request.getUserId()
    		, request.getUserPw()
    		, request.getUserNm()
    		, request.getSiteCd()
    		, request.getMblNo()
    		, request.getBirthDt()
    		, request.getNodeCd()
    		, request.getAuthCd()
    		, request.getEmail()
    		, request.getGender()
    		, request.getUseYn()
        );
    }
}
