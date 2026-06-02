package com.prafta.web.tbm.tbm02.application.query;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.common.util.AuthRoleUtils;
import com.prafta.web.tbm.tbm02.application.param.OptionParam;

public record OptionQuery(
	String siteCd
	, String searchKeyword
	, String processCd
	, boolean companyWide		// master/safe: 회사 전체, 그 외: 자기 사업장 + 회사공통만
	, String scopeSiteCd		// companyWide=false 일 때 노출 허용 사업장(자기 사업장)
	, String gvCmpnyCd
){
	public static OptionQuery from(OptionParam param) {

		if (param == null)
			throw new ApiException(CommonErrorCode.COMMON_400_001);

		boolean companyWide = AuthRoleUtils.isCompanyWide(param.gvAuthCd());

		return new OptionQuery(
			param.siteCd()
			, param.searchKeyword()
			, param.processCd()
			, companyWide
			, param.gvSiteCd()
			, param.gvCmpnyCd()
		);
	}
}
