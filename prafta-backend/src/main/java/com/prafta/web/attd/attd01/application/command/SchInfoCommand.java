package com.prafta.web.attd.attd01.application.command;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.attd.attd01.application.param.SchInfoParam;

public record SchInfoCommand(
	String cmpnyCd
	, String siteCd
	, String schCd
	, String schNo
	, String schType
	, String applyDate
	
	, String fstSchStrTime
	, String fstSchEndTime
	, String fstSchBrkMin
	
	, String secSchStrTime
	, String secSchEndTime
	, String secSchBrkMin
	
	, String useYn
	, String gvCmpnyCd
	, String gvUserCd
){
	public static SchInfoCommand from(SchInfoParam param, String schCd) {
		
        if (param == null)
        	throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - SchInfoParam");

        return new SchInfoCommand(
    		param.cmpnyCd()
    		, param.siteCd()
    		, schCd
    		, param.schNo()
    		, param.schType()
    		, param.applyDate()
    		, param.fstSchStrTime()
    		, param.fstSchEndTime()
    		, param.fstSchBrkMin()
    		, param.secSchStrTime()
    		, param.secSchEndTime()
    		, param.secSchBrkMin()
    		, param.useYn()
    		, param.gvCmpnyCd()
    		, param.gvUserCd()
        );
	}
}
