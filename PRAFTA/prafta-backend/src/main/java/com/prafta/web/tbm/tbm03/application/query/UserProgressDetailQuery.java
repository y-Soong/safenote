package com.prafta.web.tbm.tbm03.application.query;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.common.util.AuthRoleUtils;
import com.prafta.web.tbm.tbm03.application.param.UserProgressDetailParam;

/**
 * T7 드릴다운 Query.
 *
 * <p>PRAFTA-SUBCON-T5 M3: 목록(progressWhere)과 <b>동일한 사업장 스코프 술어</b>를 쓰기 위해
 * companyWide/scopeSiteCd 를 함께 운반한다(목록 카운트 ≠ 드릴다운 건수 ≠ 요약 불일치 제거).
 * 사업장 조건은 <b>자사 세션에만</b> 적용된다 — 타사(연동) 세션의 SITE_CD 는 개설사 네임스페이스라
 * 내 사업장코드와 비교하는 것 자체가 무의미하다(우연 일치로 포함/제외되는 것을 막는다).
 */
public record UserProgressDetailQuery(
	String userCd
	, String userTypeCd
	, String startDate			// 이수일(STATUS_UPDATED_AT) YYYY-MM-DD
	, String endDate
	, String completionStatusCd
	, int offset
	, int pageSize
	, String gvCmpnyCd
	, boolean companyWide		// master/safe: 회사 전체, 그 외: 자기 사업장만
	, String scopeSiteCd		// companyWide=false 일 때 노출 허용 사업장(자기 사업장)
){
	public static UserProgressDetailQuery from(UserProgressDetailParam param) {

		if (param == null)
			throw new ApiException(CommonErrorCode.COMMON_400_001);

		int offset = (param.page() - 1) * param.pageSize();
		boolean companyWide = AuthRoleUtils.isCompanyWide(param.gvAuthCd());

		return new UserProgressDetailQuery(
			param.userCd()
			, param.userTypeCd()
			, param.startDate()
			, param.endDate()
			, param.completionStatusCd()
			, offset
			, param.pageSize()
			, param.gvCmpnyCd()
			, companyWide
			, param.gvSiteCd()
		);
	}
}
