package com.prafta.web.attd.attd01.application.query;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.attd.attd01.application.param.SchInfoHistParam;
import com.prafta.web.attd.attd01.application.param.SchInfoParam;


public record SchInfoHistQuery(
	String siteCd
	, String schCd
	, String gvCmpnyCd
){
	public static SchInfoHistQuery from(SchInfoParam param) {
		
        if (param == null)
        	throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new SchInfoHistQuery(
    		param.siteCd()
    		, param.schCd()
    		, param.gvCmpnyCd()
        );
	}
	
	public static SchInfoHistQuery from(SchInfoHistParam param) {

        if (param == null)
        	throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new SchInfoHistQuery(
    		param.siteCd()
    		, param.schCd()
    		, param.gvCmpnyCd()
        );
	}

	/**
	 * 이력 적재 시 확정된 스케줄 코드(신규 발급 또는 기존)를 기준으로 이력 시퀀스를 계산하기 위한 팩토리.
	 * 신규 생성 시 param.schCd() 가 빈값이라 이력 시퀀스가 잘못 계산되는 문제를 방지한다.
	 */
	public static SchInfoHistQuery of(SchInfoParam param, String schCd) {

        if (param == null)
        	throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new SchInfoHistQuery(
    		param.siteCd()
    		, schCd
    		, param.gvCmpnyCd()
        );
	}
}
