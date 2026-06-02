package com.prafta.common.cmm.file.service;

import com.prafta.common.cmm.file.dto.param.FileInfoParam;

public interface FileService {
//	void saveFile(FileInfoCommand request, MultipartFile file);
	
	void fileSave(FileInfoParam param);
}
