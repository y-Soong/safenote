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
    , String rankCd
    , String useYn
    , String gvUserCd
){
	public static UserInfoCommand from(UserInfoModel model, String mblNoEnc, String mblNoHmac, String emailEnc, String emailHmac, String birthDtEnc) {

        if (model == null)
        	throw new ApiException(CommonErrorCode.COMMON_400_001);
	
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
			, model.rankCd()
			, model.useYn()
			, model.gvUserCd()
		);
	}
}
