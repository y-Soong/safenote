package com.prafta.common.cmm.baseinfo.application.query;

import com.prafta.common.cmm.baseinfo.application.param.SiteNodeListParam;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

public record SiteNodeListQuery(
	String cmpnyCd
	, String userCd
	, String siteCd
	, String nodeCd
	, String nodeType
	, String nodeNm
	, String parentNodeNm
	// PRAFTA-WEB_002-T1-02(1.3-3/1.4-1): 담당 미지정 노드 포함 여부(true=포함). mapper <if test="!includeNoAdmin"> 에서 사용.
	, boolean includeNoAdmin
) {
	public static SiteNodeListQuery from(SiteNodeListParam param) {

		if(param == null)
			throw new ApiException(CommonErrorCode.COMMON_400_001);

		return new SiteNodeListQuery(
			param.cmpnyCd()
			, param.userCd()
			, param.siteCd()
			, param.nodeCd()
			, param.nodeType()
			, param.nodeNm()
			, param.parentNodeNm()
			, param.includeNoAdmin()
		);
	}
}
