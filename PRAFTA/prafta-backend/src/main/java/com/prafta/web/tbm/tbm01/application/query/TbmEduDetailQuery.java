package com.prafta.web.tbm.tbm01.application.query;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.tbm.tbm01.application.param.TbmEduDetailParam;

/**
 * prafta-033-A: W-03 상세 조회 Query.
 */
public record TbmEduDetailQuery(
	String mtrlCd
	, String gvCmpnyCd
){
	public static TbmEduDetailQuery from(TbmEduDetailParam param) {

		if(param == null)
			throw new ApiException(CommonErrorCode.COMMON_400_001);

		return new TbmEduDetailQuery(
			param.mtrlCd()
			, param.gvCmpnyCd()
		);
	}
}
