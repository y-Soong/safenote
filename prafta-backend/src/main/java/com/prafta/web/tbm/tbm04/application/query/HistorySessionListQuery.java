package com.prafta.web.tbm.tbm04.application.query;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.common.util.AuthRoleUtils;
import com.prafta.web.tbm.tbm04.application.param.HistorySessionListParam;

public record HistorySessionListQuery(
	String siteCd
	, String startDate			// YYYY-MM-DD
	, String endDate			// YYYY-MM-DD
	, String managerUserCd
	, String searchKeyword
	, String statusCd			// 미지정 시 mapper 가 COMPLETED/CANCELLED 위주로 필터
	, boolean companyWide		// master/safe: 회사 전체, 그 외: 자기 사업장만
	, String scopeSiteCd		// companyWide=false 일 때 노출 허용 사업장(자기 사업장)
	, int offset
	, int pageSize
	, String gvCmpnyCd
){
	public static HistorySessionListQuery from(HistorySessionListParam param) {

		if (param == null)
			throw new ApiException(CommonErrorCode.COMMON_400_001);

		boolean companyWide = AuthRoleUtils.isCompanyWide(param.gvAuthCd());
		int offset = (param.page() - 1) * param.pageSize();

		return new HistorySessionListQuery(
			param.siteCd()
			, param.startDate()
			, param.endDate()
			, param.managerUserCd()
			, param.searchKeyword()
			, param.statusCd()
			, companyWide
			, param.gvSiteCd()
			, offset
			, param.pageSize()
			, param.gvCmpnyCd()
		);
	}
}
