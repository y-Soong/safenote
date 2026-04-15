package com.prafta.web.baim.baim05.application.query;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.baim.baim05.application.param.DailyUserSlotListParam;

public record DailyUserSlotListQuery(
		String siteCd
		, String slotType
		, String slotStatus
		, String useYn
		, String currUserId
		, String gvCmpnyCd
		, String gvUserCd
){
	public static DailyUserSlotListQuery from(DailyUserSlotListParam param) {
		
		if(param == null)
			throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - DailyUserSlotListParam");
		
		return new DailyUserSlotListQuery(
			param.siteCd()
			, param.slotType()
			, param.slotStatus()
			, param.useYn()
			, param.currUserId()
			, param.gvCmpnyCd()
			, param.gvUserCd()
		);
	}
}
