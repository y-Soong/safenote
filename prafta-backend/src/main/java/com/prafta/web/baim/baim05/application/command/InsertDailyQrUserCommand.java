package com.prafta.web.baim.baim05.application.command;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.baim.baim05.application.param.InsertDailyQrUserParam;

public record InsertDailyQrUserCommand(
	String siteCd
	, String userCd
    , String userNm
    , String userPw
    
    , String mblNoEnc
    , String mblNoHmac
    , String mblNoLast4
    , String regType
    
    , String useYn
    , String accountStatus
    
    , int pwdFailCnt
    , String pwdLockYn
    
    , String gvCmpnyCd
    , String gvUserCd
) {
    public static InsertDailyQrUserCommand from(
		String userCd
		, InsertDailyQrUserParam param
		, String userPw
		
		, String mblNoEnc
		, String mblNoHmac
		, String mblNoLast4
	) {

        if (param == null)
            throw ApiException.appendf(CommonErrorCode.COMMON_400_001, "\nRequired param missing - InsertDailyQrUserParam");

        return new InsertDailyQrUserCommand(
            param.siteCd()
            , userCd
            , param.userNm()
            , userPw
            
            , mblNoEnc
            , mblNoHmac
            , mblNoLast4
            , "02"				// 01 : 직접가입, 02 : QR 사용자
            
            , "Y"
            , "01"
            
            , 0
            , "N"
            
            , param.gvCmpnyCd()
            , param.gvUserCd()
        );
    }
}
