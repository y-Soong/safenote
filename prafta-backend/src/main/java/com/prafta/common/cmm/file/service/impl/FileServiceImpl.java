package com.prafta.common.cmm.file.service.impl;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Service;

import com.prafta.common.cmm.file.application.command.FileInfoCommand;
import com.prafta.common.cmm.file.dto.param.FileInfoParam;
import com.prafta.common.cmm.file.mapper.FileMapper;
import com.prafta.common.cmm.file.service.FileService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {
	
	private final FileMapper fileMapper;
	
	public void fileSave(FileInfoParam param) {
		try {
			System.out.println("getOriginalFilename :: " + param.file().getOriginalFilename());
			
			String today = LocalDate.now()
	                .format(DateTimeFormatter.ofPattern("yyyyMMdd"));

	        String projectRoot = System.getProperty("user.dir");
	        
	        // 파일 절대경로
	        Path absoluteFilePath = Paths.get(
	        		projectRoot,"/uploads",
	        		param.cmpnyCd(), today,
	        		param.siteCd(), param.fileType()
			).toAbsolutePath().normalize();
	        
	        // Project Root 디렉토리 기준 파일 상대경로
	        Path filePath = Paths.get(
	        		"/uploads",
	        		param.cmpnyCd(), today,
	        		param.siteCd(), param.fileType()
			).normalize();
	        
	        //Files.createDirectories(absoluteFilePath);

	        String originalFilename = param.file().getOriginalFilename();
	        String extension = "";
	        if (originalFilename != null && originalFilename.contains(".")) {
	            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
	        }

	        // 파일명: fileMgmtCd + 확장자
	        String saveFileName = param.fileMgmtCd() + extension;

	        Path savePath = absoluteFilePath.resolve(saveFileName).normalize();
	        
	        // 부모 폴더 한번 더 보장 (가끔 normalize 영향 대비)
	        Files.createDirectories(savePath.getParent());
	        
	        param.file().transferTo(savePath.toFile());
	                    
	        fileMapper.insertFileInfo(FileInfoCommand.from(param, filePath.toString(), originalFilename, extension));

	        log.info("파일 저장 완료: {}", savePath.toAbsolutePath());
        } catch (Exception e) {
        	log.error("파일 저장 중 오류. request={}", param, e);
        	// 프로젝트 공통 예외가 있으면 거기로 래핑해서 던져도 됨
        	throw new RuntimeException("파일 저장 실패", e);
      }
	}
}