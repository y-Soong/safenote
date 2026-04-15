package com.prafta.web.attd.attd01.application.command;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.attd.attd01.application.param.SchInfoParam;

public record SchInfoHistCommand(
	String cmpnyCd
	, String siteCd
	, int histIdx
	, String schCd
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
	public static SchInfoHistCommand from(SchInfoParam param, int histIdx, String schCd) {
		
        if (param == null)
        	throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - SchInfoParam");

        return new SchInfoHistCommand(
    		param.cmpnyCd()
    		, param.siteCd()
    		, histIdx
    		, schCd
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
