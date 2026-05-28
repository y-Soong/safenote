package com.prafta.web.attd.attd01.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.attd.attd01.dto.request.SchInfoRequest;

public record SchInfoParam(
	String cmpnyCd
	, String siteCd
	, String schCd
	, String schNo
	, String schType
	, String applyDate
	
	, String fstSchStrTime
	, String fstSchEndTime
	, String fstSchBrkMin
	, String fstBrkStrTime
	, String fstBrkEndTime

	, String secSchStrTime
	, String secSchEndTime
	, String secSchBrkMin
	, String secBrkStrTime
	, String secBrkEndTime

	, String useYn
	, String gvCmpnyCd
	, String gvUserCd
){	
	public static SchInfoParam from(SchInfoRequest request, TokenInfo tokenInfo) {
		
        if (request == null)
        	throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new SchInfoParam(
    		request.getCmpnyCd()
    		, request.getSiteCd()
    		, request.getSchCd()
    		, request.getSchNo()
    		, request.getSchType()
    		, request.getApplyDate()
    		, request.getFstSchStrTime()
    		, request.getFstSchEndTime()
    		, request.getFstSchBrkMin()
    		, request.getFstBrkStrTime()
    		, request.getFstBrkEndTime()
    		, request.getSecSchStrTime()
    		, request.getSecSchEndTime()
    		, request.getSecSchBrkMin()
    		, request.getSecBrkStrTime()
    		, request.getSecBrkEndTime()
    		, request.getUseYn()
    		, tokenInfo.gv_cmpnyCd()
    		, tokenInfo.gv_userCd()
        );
	}
}
