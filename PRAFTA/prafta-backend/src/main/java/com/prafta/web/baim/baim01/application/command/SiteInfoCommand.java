package com.prafta.web.baim.baim01.application.command;

import java.math.BigDecimal;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.baim.baim01.application.model.SiteInfoModel;

public record SiteInfoCommand(
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
	, String gvCmpnyCd
	, String gvUserCd
) {
	public static SiteInfoCommand from(SiteInfoModel model, String siteCd) {
		// PRAFTA-COM-001-T2-2: useYn/endDate 무보정 기본 경로(하위 호환). 보정은 from(model, siteCd, endDate, useYn) 사용.
		return from(model, siteCd, model.endDate(), model.useYn());
	}

	/**
	 * PRAFTA-COM-001-T2-2 — 서비스(Baim01ServiceImpl)에서 A안 종료일 경계로 보정한
	 * endDate/useYn 을 주입받아 Command 를 생성한다(SiteInfoModel 은 불변 record 라 직접 보정 불가).
	 */
	public static SiteInfoCommand from(SiteInfoModel model, String siteCd, String endDate, String useYn) {

		if(model == null)
			throw new ApiException(CommonErrorCode.COMMON_400_001);
		if(siteCd == null)
			throw new ApiException(CommonErrorCode.COMMON_400_001);

		return new SiteInfoCommand(
			model.cmpnyCd()
			, siteCd
			, model.siteNo()
			, model.siteNm()
			, model.addr1()
			, model.addr2()
			, model.zipCode()
			, model.strDate()
			, endDate
			, useYn
			, model.siteAdminCd()
			, model.telNo()
			, model.gpsRange()
			, model.siteDesc()
			, model.lat()
			, model.lon()
			, model.gvCmpnyCd()
			, model.gvUserCd()
		);
	}
}

