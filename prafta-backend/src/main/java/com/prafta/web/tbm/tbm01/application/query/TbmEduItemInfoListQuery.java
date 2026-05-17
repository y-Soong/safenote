package com.prafta.web.tbm.tbm01.application.query;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.tbm.tbm01.application.param.TbmEduInfoListParam;
import com.prafta.web.tbm.tbm01.application.param.TbmEduItemInfoListParam;

public record TbmEduItemInfoListQuery(
	String mtrlCd
	, String gvCmpnyCd
){
	public static TbmEduItemInfoListQuery from(TbmEduInfoListParam param) {
		
		if(param == null)
			throw new ApiException(CommonErrorCode.COMMON_400_001);
		
		return new TbmEduItemInfoListQuery(
			param.mtrlCd()
			, param.gvCmpnyCd()
		);
	}
	
	public static TbmEduItemInfoListQuery from(TbmEduItemInfoListParam param) {
		
		if(param == null)
			throw new ApiException(CommonErrorCode.COMMON_400_001);
		
		return new TbmEduItemInfoListQuery(
			param.mtrlCd()
			, param.gvCmpnyCd()
		);
	}
}
