package com.prafta.common.cmm.file.application.command;

import java.util.Objects;

import com.prafta.common.cmm.file.dto.param.FileInfoParam;

public record FileInfoCommand(
    String cmpnyCd
    , String userId
    , String siteCd
    , String fileType   	// 001: 일일점검, 002: 위험성평가
    , String filePath		// 파일 저장 경로
    , String fileName   	// 클라에서 온 파일명(선택)
    , String fileMgmtCd 	// 생성된 파일관리코드
    , String fileExt		// 파일확장자
) {
	public static FileInfoCommand from(FileInfoParam param, String filePath, String fileName, String fileExt) {
		Objects.requireNonNull(param, "FileInfoParam is required");
		Objects.requireNonNull(filePath, "filePath is required");
		Objects.requireNonNull(fileName, "fileName is required");
		Objects.requireNonNull(fileExt, "fileExt is required");
		
		return new FileInfoCommand(
			param.cmpnyCd()
			, param.userId()
			, param.siteCd()
			, param.fileType()
			, filePath
			, fileName
			, param.fileMgmtCd()
			, fileExt
		);
	}
}