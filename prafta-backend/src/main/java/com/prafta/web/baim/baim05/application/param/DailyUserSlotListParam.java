package com.prafta.web.baim.baim05.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.baim.baim05.dto.request.DailyUserSlotListRequest;

public record DailyUserSlotListParam(
	String siteCd
	, String slotType
	, String slotStatus
	, String useYn
	, String currUserId
	, String gvCmpnyCd
	, String gvUserCd
){
	public static DailyUserSlotListParam from(DailyUserSlotListRequest request, TokenInfo tokenInfo) {
		
		if(request == null)
			throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - TermsInfoRequest");
		if(tokenInfo == null)
			throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - TokenInfo");
		
		return new DailyUserSlotListParam(
			request.getSiteCd()
			, request.getSlotType()
			, request.getSlotStatus()
			, request.getUseYn()
			, request.getCurrUserId()
			, tokenInfo.gv_cmpnyCd()
			, tokenInfo.gv_userCd()
		);
	}
}
