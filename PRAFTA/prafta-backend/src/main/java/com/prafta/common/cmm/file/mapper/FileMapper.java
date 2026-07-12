package com.prafta.common.cmm.file.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.prafta.common.cmm.file.application.command.FileInfoCommand;
import com.prafta.common.cmm.file.application.model.FileReadInfo;
import com.prafta.common.cmm.file.application.query.FileInfoQuery;
import com.prafta.common.cmm.file.application.query.FileReadQuery;

@Mapper
public interface FileMapper {
	String selectFileMgmtCd(FileInfoQuery query);

	void insertFileInfo(FileInfoCommand dto);

	// PRAFTA-WEB_003: 원본 파일 read 를 위한 경로/확장자 조회(회사코드+파일코드 스코프). 없으면 null.
	FileReadInfo selectFileInfoForRead(FileReadQuery query);
}
