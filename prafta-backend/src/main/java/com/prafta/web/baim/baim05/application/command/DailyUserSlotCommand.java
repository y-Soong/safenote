package com.prafta.web.baim.baim05.application.command;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.baim.baim05.application.param.LinkPoliciesParam;

public record DailyUserSlotCommand(
	String siteCd
	, int slotNo
	, String slotType
	, String useYn
	, String slotStatus
	, String gvCmpnyCd
	, String gvUserCd
){
	public static DailyUserSlotCommand from(LinkPoliciesParam param, int slotNo, String useYn) {

        if (param == null)
        	throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - LinkPoliciesParam");
        if (useYn == null)
        	throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - useYn");

        return new DailyUserSlotCommand(
        		param.siteCd()
        		, slotNo
        		, "01"					// 01:일반사용자, 02:QR사용자
        		, useYn
        		, "01"					// 01:비점유중, 02:점유중
        		, param.gvCmpnyCd()
        		, param.gvUserCd()
        );        
    }
}
