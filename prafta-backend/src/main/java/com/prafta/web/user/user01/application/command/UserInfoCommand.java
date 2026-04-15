package com.prafta.web.user.user01.application.command;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.user.user01.application.model.UserInfoModel;

public record UserInfoCommand(
	String cmpnyCd
	, String userCd
    , String userId
    , String userPw
    , String userNm
    , String mblNoEnc
    , String mblNoHmac
    , String emailEnc
    , String emailHmac
    , String gender
    , String birthDtEnc
    , String siteCd
    , String nodeCd
    , String authCd
    , String useYn
    , String accountStatus
    , String withdrawalDate
    , String gvUserCd
){
	public static UserInfoCommand from(UserInfoModel model, String mblNoEnc, String mblNoHmac, String emailEnc, String emailHmac, String birthDtEnc) {

        if (model == null)
        	throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - UserInfoModel");
	
	    return new UserInfoCommand(
			model.cmpnyCd()
			, model.userCd()
			, model.userId()
			, model.userPw()
			, model.userNm()
			, mblNoEnc
			, mblNoHmac
			, emailEnc
			, emailHmac
			, model.gender()
			, birthDtEnc
			, model.siteCd()
			, model.nodeCd()
			, model.authCd()
			, model.useYn()
			, model.accountStatus()
			, model.withdrawalDate()
			, model.gvUserCd()
		);
	}
}
