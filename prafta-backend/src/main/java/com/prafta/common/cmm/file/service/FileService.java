package com.prafta.common.cmm.file.service;

import org.springframework.web.multipart.MultipartFile;

import com.prafta.common.cmm.file.dto.FileInfoSave;

public interface FileService {
	void saveFile(FileInfoSave request, MultipartFile file);
}
