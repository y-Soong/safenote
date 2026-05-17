package com.prafta.web.attd.attd05.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.attd.attd05.dto.request.UserWorkPlansRequest;
import com.prafta.web.user.user01.application.command.WithdrawMyAccountCommand;


public record UserWorkPlansParam (
	String siteCd
	, String nodeCd
	, String incSubNodeYn
	, String workYm
	, String userNm
	, String gvCmpnyCd
){
	public static UserWorkPlansParam from(UserWorkPlansRequest request, TokenInfo tokenInfo) {

        if (request == null)
        	throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new UserWorkPlansParam(
    		request.getSiteCd()
    		, request.getNodeCd()
    		, request.getIncSubNodeYn()
    		, request.getWorkYm()
    		, request.getUserNm()
    		, tokenInfo.gv_cmpnyCd()
		);
    }
}
