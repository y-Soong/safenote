package com.prafta.common.cmm.file.service.impl;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.prafta.common.cmm.file.dto.FileInfoSave;
import com.prafta.common.cmm.file.mapper.FileMapper;
import com.prafta.common.cmm.file.service.FileService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {
	
	private final FileMapper fileMapper;

    @Override
    public void saveFile(FileInfoSave request, MultipartFile file) {
        try {
        	System.out.println("getOriginalFilename :: " + file.getOriginalFilename());
        	
            String today = LocalDate.now()
                    .format(DateTimeFormatter.ofPattern("yyyyMMdd"));

            String projectRoot = System.getProperty("user.dir");
            
            // 파일 절대경로
            Path absoluteFilePath = Paths.get(
            		projectRoot,"/uploads",
                    request.getCmpnyCd(), today,
                    request.getSiteCd(), request.getFileType()
    		).toAbsolutePath().normalize();
            
            // Project Root 디렉토리 기준 파일 상대경로
            Path filePath = Paths.get(
            		"/uploads",
                    request.getCmpnyCd(), today,
                    request.getSiteCd(), request.getFileType()
    		).normalize();
            
            System.out.println(filePath);
            System.out.println("file :: " + file);
            
            
            Files.createDirectories(absoluteFilePath);

            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }

            // 파일명: fileMgmtCd + 확장자
            String saveFileName = request.getFileMgmtCd() + extension;

            Path savePath = absoluteFilePath.resolve(saveFileName);
            
            // 부모 폴더 한번 더 보장 (가끔 normalize 영향 대비)
            Files.createDirectories(savePath.getParent());
            
            file.transferTo(savePath.toFile());
            
            request = request.toBuilder()
            			.filePath(filePath.toString())
            			.fileName(originalFilename)
            			.fileExt(extension)
            			.build();
                        
            fileMapper.insertFileInfo(request);

            log.info("파일 저장 완료: {}", savePath.toAbsolutePath());
        } catch (Exception e) {
            log.error("파일 저장 중 오류. request={}", request, e);
            // 프로젝트 공통 예외가 있으면 거기로 래핑해서 던져도 됨
            throw new RuntimeException("파일 저장 실패", e);
        }
    }
}
