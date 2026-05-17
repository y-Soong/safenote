package com.prafta.common.cmm.file.application.query;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

public record FileInfoQuery(
    String cmpnyCd
    , String fileType   	// 001: 일일점검, 002: 위험성평가
) {
	public static FileInfoQuery from(String cmpnyCd, String fileType) {
		if (cmpnyCd == null)
        	throw new ApiException(CommonErrorCode.COMMON_400_001);
		if (fileType == null)
        	throw new ApiException(CommonErrorCode.COMMON_400_001);
		
		return new FileInfoQuery(
			cmpnyCd
			,fileType
		);
	}
}
