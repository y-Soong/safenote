package com.prafta.web.baim.baim05.application.query;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.baim.baim05.application.param.DailyUserSlotListParam;

public record DailyUserSlotListQuery(
		String siteCd
		, String slotType
		, String slotStatus
		, String useYn
		, String currUserNm
		, String gvCmpnyCd
		, String gvUserCd
){
	public static DailyUserSlotListQuery from(DailyUserSlotListParam param) {
		
		if(param == null)
			throw new ApiException(CommonErrorCode.COMMON_400_001);
		
		return new DailyUserSlotListQuery(
			param.siteCd()
			, param.slotType()
			, param.slotStatus()
			, param.useYn()
			, param.currUserNm()
			, param.gvCmpnyCd()
			, param.gvUserCd()
		);
	}
}
