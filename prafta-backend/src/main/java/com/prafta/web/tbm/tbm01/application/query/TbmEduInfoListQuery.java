package com.prafta.web.tbm.tbm01.application.query;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.common.util.AuthRoleUtils;
import com.prafta.web.tbm.tbm01.application.param.TbmEduInfoListParam;

public record TbmEduInfoListQuery(
	String mtrlCd
	, String mtrlType
	, String title
	, String useYn
	, String siteCd			// 스코프 필터(특정 사업장). 비어 있으면 미적용
	, boolean companyWide	// true(master/safe): 회사 전체 콘텐츠 조회, false: 자기사업장+회사공통
	, String scopeSiteCd	// companyWide=false 일 때 노출 허용 사업장(자기 사업장)
	, String gvCmpnyCd
){
	public static TbmEduInfoListQuery from(TbmEduInfoListParam param) {

		if(param == null)
			throw new ApiException(CommonErrorCode.COMMON_400_001);

		// master/safe 는 회사 전체(공통+모든 사업장), 그 외는 자기 사업장 + 회사공통만 노출
		boolean companyWide = AuthRoleUtils.isCompanyWide(param.gvAuthCd());

		return new TbmEduInfoListQuery(
			param.mtrlCd()
			, param.mtrlType()
			, param.title()
			, param.useYn()
			, param.siteCd()
			, companyWide
			, param.gvSiteCd()
			, param.gvCmpnyCd()
		);

	}
}
