package com.prafta.web.tbm.tbm01.application.query;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.tbm.tbm01.application.param.TbmEduInfoListParam;

public record TbmEduInfoListQuery(
	String mtrlCd
	, String mtrlType
	, String title
	, String useYn
	, String gvCmpnyCd
){
	public static TbmEduInfoListQuery from(TbmEduInfoListParam param) {
		
		if(param == null)
			throw new ApiException(CommonErrorCode.COMMON_400_001);
		
		return new TbmEduInfoListQuery(
			param.mtrlCd()
			, param.mtrlType()
			, param.title()
			, param.useYn()
			, param.gvCmpnyCd()
		);
		
	}
}
