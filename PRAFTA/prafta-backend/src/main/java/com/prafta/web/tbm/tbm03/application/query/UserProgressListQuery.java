package com.prafta.web.tbm.tbm03.application.query;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.common.util.AuthRoleUtils;
import com.prafta.web.tbm.tbm03.application.param.UserProgressListParam;

public record UserProgressListQuery(
	String siteCd
	, String startDate			// 이수일(STATUS_UPDATED_AT) YYYY-MM-DD
	, String endDate
	, String searchKeyword
	, boolean companyWide		// master/safe: 회사 전체, 그 외: 자기 사업장만
	, String scopeSiteCd		// companyWide=false 일 때 노출 허용 사업장(자기 사업장)
	, int offset
	, int pageSize
	, String gvCmpnyCd
){
	public static UserProgressListQuery from(UserProgressListParam param) {

		if (param == null)
			throw new ApiException(CommonErrorCode.COMMON_400_001);

		boolean companyWide = AuthRoleUtils.isCompanyWide(param.gvAuthCd());
		int offset = (param.page() - 1) * param.pageSize();

		return new UserProgressListQuery(
			param.siteCd()
			, param.startDate()
			, param.endDate()
			, param.searchKeyword()
			, companyWide
			, param.gvSiteCd()
			, offset
			, param.pageSize()
			, param.gvCmpnyCd()
		);
	}
}
