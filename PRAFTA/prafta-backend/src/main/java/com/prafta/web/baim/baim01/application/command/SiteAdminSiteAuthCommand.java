package com.prafta.web.baim.baim01.application.command;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.baim.baim01.application.model.SiteInfoModel;

public record SiteAdminSiteAuthCommand(
	String cmpnyCd
	, String siteAdminCd
	, String siteCd
	, String gvUserCd
) {
	public static SiteAdminSiteAuthCommand from(SiteInfoModel model, String siteCd) {

		if(model == null)
			throw new ApiException(CommonErrorCode.COMMON_400_001);
		if(siteCd == null)
			throw new ApiException(CommonErrorCode.COMMON_400_001);

		// 테넌트 격리: cmpnyCd 는 저장 컨텍스트의 gvCmpnyCd(JWT) 사용(body 아님).
		//   siteAdminCd 는 화면에서 지정한 사업장 관리자 계정. 동일 회사 사용자만 부여된다(매퍼 WHERE).
		return new SiteAdminSiteAuthCommand(
			model.gvCmpnyCd()
			, model.siteAdminCd()
			, siteCd
			, model.gvUserCd()
		);
	}
}
