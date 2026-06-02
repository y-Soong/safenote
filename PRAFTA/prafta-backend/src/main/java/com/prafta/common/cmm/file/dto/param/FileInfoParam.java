package com.prafta.common.cmm.file.dto.param;

import org.springframework.web.multipart.MultipartFile;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

public record FileInfoParam(
	String cmpnyCd
    , String userId
    , String siteCd
    , String fileType   	// 001: 일일점검, 002: 위험성평가
    , String fileMgmtCd 	// 생성된 파일관리코드
    , MultipartFile file
) {
	public static FileInfoParam from(
			String cmpnyCd
		    , String userId
		    , String siteCd
		    , String fileType
		    , String fileMgmtCd
		    , MultipartFile file
    ) {
		if (cmpnyCd == null)
        	throw new ApiException(CommonErrorCode.COMMON_400_001);
		if (userId == null)
        	throw new ApiException(CommonErrorCode.COMMON_400_001);
		if (siteCd == null)
        	throw new ApiException(CommonErrorCode.COMMON_400_001);
		if (fileType == null)
        	throw new ApiException(CommonErrorCode.COMMON_400_001);
		if (fileMgmtCd == null)
        	throw new ApiException(CommonErrorCode.COMMON_400_001);
		if (file == null)
        	throw new ApiException(CommonErrorCode.COMMON_400_001);
		
		return new FileInfoParam(
			cmpnyCd
			, userId
			, siteCd
			, fileType
			, fileMgmtCd
			, file
		);
	}
}