package com.prafta.common.cmm.baseinfo.application.query;

import com.prafta.common.cmm.baseinfo.application.param.MenuListParam;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

public record MenuListQuery(
	String cmpnyCd
	, String userCd
	, String menuSrc
) {
	public static MenuListQuery from(MenuListParam param) {
		
		if(param == null)
			throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - MenuListParam");
		
		return new MenuListQuery(
			param.cmpnyCd()
			, param.userCd()
			, param.menuSrc()
		); 
	}
}
