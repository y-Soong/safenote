package com.prafta.web.baim.baim01.application.model;

import java.math.BigDecimal;

public record SiteInfoModel(
		String cmpnyCd
		, String siteCd
		, String siteNo
		, String siteNm
		, String addr1
		, String addr2
		, String zipCode
		, String strDate
		, String endDate
		, String useYn
		, String siteAdminCd
		, String telNo
		, String gpsRange
		, String siteDesc
		, BigDecimal lat
		, BigDecimal lon
		// 통상근로자 주 소정근로 분(사업장 오버라이드). null = 회사 기본값 상속(행 삭제).
		, Integer weekStdMinutes

		, String gvCmpnyCd
		, String gvUserCd
) {

}
