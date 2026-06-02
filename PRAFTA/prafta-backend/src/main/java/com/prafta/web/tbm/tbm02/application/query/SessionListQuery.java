package com.prafta.web.tbm.tbm02.application.query;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.common.util.AuthRoleUtils;
import com.prafta.web.tbm.tbm02.application.param.SessionListParam;

public record SessionListQuery(
	String siteCd
	, String statusCd
	, String startDate			// YYYY-MM-DD
	, String endDate			// YYYY-MM-DD
	, String managerUserCd
	, String searchKeyword
	, boolean companyWide		// master/safe: 회사 전체, 그 외: 자기 사업장만
	, String scopeSiteCd		// companyWide=false 일 때 노출 허용 사업장(자기 사업장)
	, int offset
	, int pageSize
	, String gvCmpnyCd
){
	public static SessionListQuery from(SessionListParam param) {

		if (param == null)
			throw new ApiException(CommonErrorCode.COMMON_400_001);

		boolean companyWide = AuthRoleUtils.isCompanyWide(param.gvAuthCd());
		int offset = (param.page() - 1) * param.pageSize();

		return new SessionListQuery(
			param.siteCd()
			, param.statusCd()
			, param.startDate()
			, param.endDate()
			, param.managerUserCd()
			, param.searchKeyword()
			, companyWide
			, param.gvSiteCd()
			, offset
			, param.pageSize()
			, param.gvCmpnyCd()
		);
	}
}
