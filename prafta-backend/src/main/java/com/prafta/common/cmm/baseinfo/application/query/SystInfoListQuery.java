package com.prafta.common.cmm.baseinfo.application.query;

import java.util.List;

import com.prafta.common.cmm.baseinfo.application.param.SystInfoListParam;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

public record SystInfoListQuery (
	List<String> systCodeList
){
	public static SystInfoListQuery from(SystInfoListParam param) {
		
		if(param == null)
			throw new ApiException(CommonErrorCode.COMMON_400_001);
		
		return new SystInfoListQuery(
				param.systCodeList()
		); 
	}
}
