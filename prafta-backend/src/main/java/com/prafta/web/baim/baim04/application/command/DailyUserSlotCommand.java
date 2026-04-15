package com.prafta.web.baim.baim04.application.command;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.baim.baim04.application.model.LinkPoliciesModel;

public record DailyUserSlotCommand(
	String cmpnyCd
	, String siteCd
	, int slotNo
	, String slotType
	, String useYn
	, String slotStatus
	, String gvCmpnyCd
	, String gvUserCd
){
	public static DailyUserSlotCommand from(LinkPoliciesModel model, int slotNo, String useYn) {

        if (model == null)
        	throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - LinkPoliciesModel");
        if (useYn == null)
        	throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - useYn");

        return new DailyUserSlotCommand(
        		model.cmpnyCd()
        		, model.siteCd()
        		, slotNo
        		, "01"					// 01:일반사용자, 02:QR사용자
        		, useYn
        		, "01"					// 01:비점유중, 02:점유중
        		, model.gvCmpnyCd()
        		, model.gvUserCd()
        );        
    }
	
	public static DailyUserSlotCommand from(LinkPoliciesModel model) {

        if (model == null)
        	throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - LinkPoliciesModel");

        return new DailyUserSlotCommand(
        		model.cmpnyCd()
        		, model.siteCd()
        		, 0
        		, "01"					// 01:일반사용자, 02:QR사용자
        		, ""
        		, "01"					// 01:비점유중, 02:점유중
        		, model.gvCmpnyCd()
        		, model.gvUserCd()
        );        
    }
}
