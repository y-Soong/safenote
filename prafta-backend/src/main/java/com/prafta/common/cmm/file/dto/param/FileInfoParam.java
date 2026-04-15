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
        	throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - cmpnyCd");
		if (userId == null)
        	throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - userId");
		if (siteCd == null)
        	throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - siteCd");
		if (fileType == null)
        	throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - fileType");
		if (fileMgmtCd == null)
        	throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - fileMgmtCd");
		if (file == null)
        	throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - file");
		
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