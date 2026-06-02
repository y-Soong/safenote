package com.prafta.web.baim.baim05.application.command;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.baim.baim05.application.param.InsertDailyQrUserParam;

public record DailyUserSlotUpdCommand(
	String siteCd
	, String slotNo
	, String userCd
	, String slotType
	, String slotStatus
	, String gvCmpnyCd
	, String gvUserCd
){
	public static DailyUserSlotUpdCommand from(InsertDailyQrUserParam param, String userCd) {

        if (param == null)
        	throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new DailyUserSlotUpdCommand(
        		param.siteCd()
        		, param.slotNo()
        		, userCd
        		, "02"
        		, "02"
        		, param.gvCmpnyCd()
        		, param.gvUserCd()
        );        
    }
}
