package com.prafta.common.cmm.file.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.prafta.common.cmm.file.application.command.FileInfoCommand;
import com.prafta.common.cmm.file.application.query.FileInfoQuery;

@Mapper
public interface FileMapper {
	String selectFileMgmtCd(FileInfoQuery query);
	
	void insertFileInfo(FileInfoCommand dto);
}
