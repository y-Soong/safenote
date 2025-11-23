package com.prafta.common.cmm.file.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.prafta.common.cmm.file.dto.FileInfoSave;

@Mapper
public interface FileMapper {
	String selectFileMgmtCd(FileInfoSave dto);
	
	void insertFileInfo(FileInfoSave dto);
}
