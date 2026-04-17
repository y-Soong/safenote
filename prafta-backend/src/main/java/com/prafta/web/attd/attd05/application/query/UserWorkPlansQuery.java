package com.prafta.web.attd.attd05.application.query;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.attd.attd05.application.param.UserWorkPlansParam;


public record UserWorkPlansQuery (
	String siteCd
	, String nodeCd
	, String incSubNodeYn
	, String workYm
	, String userNm
	, String gvCmpnyCd
){
	public static UserWorkPlansQuery from(UserWorkPlansParam param) {

        if (param == null)
        	throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - UserWorkPlansParam");

        return new UserWorkPlansQuery(
    		param.siteCd()
    		, param.nodeCd()
    		, param.incSubNodeYn()
    		, param.workYm()
    		, param.userNm()
    		, param.gvCmpnyCd()
		);
    }
}
