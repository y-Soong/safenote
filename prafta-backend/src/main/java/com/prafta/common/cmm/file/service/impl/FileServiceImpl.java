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
            String today = LocalDate.now()
                    .format(DateTimeFormatter.ofPattern("yyyyMMdd"));

            String projectRoot = System.getProperty("user.dir");
            Path uploadRoot = Paths.get(
                    projectRoot, "uploads",
                    request.getCmpnyCd(), today,
                    request.getSiteCd(), request.getFileType()
            );
            Files.createDirectories(uploadRoot);

            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }

            // 파일명: fileMgmtCd + 확장자
            String saveFileName = request.getFileMgmtCd() + extension;

            Path savePath = uploadRoot.resolve(saveFileName);
            file.transferTo(savePath.toFile());
            
            System.out.println("uploadRoot.toString() :: " + uploadRoot.toString());
            
            request = request.toBuilder()
            			.filePath(uploadRoot.toString())
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
