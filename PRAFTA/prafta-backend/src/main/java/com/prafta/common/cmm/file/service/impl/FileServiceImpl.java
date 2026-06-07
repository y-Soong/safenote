package com.prafta.common.cmm.file.service.impl;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.prafta.common.cmm.file.application.command.FileInfoCommand;
import com.prafta.common.cmm.file.dto.param.FileInfoParam;
import com.prafta.common.cmm.file.mapper.FileMapper;
import com.prafta.common.cmm.file.service.FileService;
import com.prafta.common.error.file.FileErrorCode;
import com.prafta.common.exception.ApiException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

	private final FileMapper fileMapper;

	/**
	 * 업로드 파일 루트 디렉토리.
	 * 환경변수 FILE_UPLOAD_BASE_DIR 로 덮어쓰며, 미지정 시 ${user.dir}/uploads.
	 * 정적 서빙 핸들러(ApiPrefixConfig)와 동일한 값을 사용해 저장/서빙 경로 불일치를 방지한다.
	 */
	@Value("${file.upload.base-dir}")
	private String uploadBaseDir;

	/**
	 * 업로드 허용 확장자 화이트리스트(소문자, 점 제외).
	 *
	 * <p>텍스트/이미지/동영상/음성 파일만 허용한다. 설치/실행 파일(exe, msi, apk, jar,
	 * bat, sh 등)과 브라우저에서 실행 가능한 스크립트성 형식(html, svg, js 등)은 제외한다.
	 */
	private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
			// 텍스트
			"txt", "csv", "tsv", "log", "md", "json", "xml", "rtf",
			// 이미지 (svg 는 스크립트 삽입 위험으로 제외)
			"jpg", "jpeg", "png", "gif", "bmp", "webp", "heic", "heif", "tif", "tiff",
			// 동영상
			"mp4", "mov", "avi", "mkv", "webm", "m4v", "3gp", "wmv", "flv", "mpeg", "mpg",
			// 음성
			"mp3", "wav", "m4a", "aac", "ogg", "flac", "amr", "wma", "opus",
			// 문서 — PDF 교육자료 지원(SYS018 04). 스펙상 정식 지원 형식.
			"pdf"
	);

	/**
	 * 원본 파일명에서 확장자를 추출해 화이트리스트 검증을 수행한다.
	 *
	 * <p>경로 구분자(/, \\)를 제거해 path traversal 을 차단하고, 확장자가 없거나
	 * 허용 목록에 없으면 {@link FileErrorCode#FILE_400_001} 로 차단한다.
	 *
	 * @return 검증을 통과한 확장자(점 포함, 예: ".jpg")
	 */
	private String resolveAllowedExtension(String originalFilename) {
		if (originalFilename == null) {
			throw new ApiException(FileErrorCode.FILE_400_001);
		}
		// 경로 구분자 제거(파일명만 사용) — path traversal 방어
		String safeName = originalFilename
				.replace("\\", "/");
		safeName = safeName.substring(safeName.lastIndexOf('/') + 1);

		int dotIdx = safeName.lastIndexOf('.');
		if (dotIdx < 0 || dotIdx == safeName.length() - 1) {
			// 확장자 없음
			throw new ApiException(FileErrorCode.FILE_400_001);
		}
		String ext = safeName.substring(dotIdx + 1).toLowerCase();
		if (!ALLOWED_EXTENSIONS.contains(ext)) {
			log.warn("허용되지 않은 파일 형식 업로드 차단: 파일명={}, 확장자={}", originalFilename, ext);
			throw new ApiException(FileErrorCode.FILE_400_001);
		}
		return "." + ext;
	}

	public void fileSave(FileInfoParam param) {
		try {
			String today = LocalDate.now()
	                .format(DateTimeFormatter.ofPattern("yyyyMMdd"));

	        // 파일 절대경로 — 디스크 저장 위치는 공통 설정(file.upload.base-dir) 기준.
	        Path absoluteFilePath = Paths.get(
	        		uploadBaseDir,
	        		param.cmpnyCd(), today,
	        		param.siteCd(), param.fileType()
			).toAbsolutePath().normalize();

	        // 공개 URL/DB 저장용 상대경로 — '/uploads' 는 정적 서빙 마운트 경로(디스크 위치와 무관)이므로 고정.
	        Path filePath = Paths.get(
	        		"/uploads",
	        		param.cmpnyCd(), today,
	        		param.siteCd(), param.fileType()
			).normalize();
	        
	        //Files.createDirectories(absoluteFilePath);

	        String originalFilename = param.file().getOriginalFilename();
	        // 확장자 화이트리스트 검증(텍스트/이미지/동영상/음성만 허용) + path traversal 방어
	        String extension = resolveAllowedExtension(originalFilename);

	        // 파일명: fileMgmtCd + 확장자
	        String saveFileName = param.fileMgmtCd() + extension;

	        Path savePath = absoluteFilePath.resolve(saveFileName).normalize();
	        
	        // 부모 폴더 한번 더 보장 (가끔 normalize 영향 대비)
	        Files.createDirectories(savePath.getParent());
	        
	        param.file().transferTo(savePath.toFile());
	                    
	        fileMapper.insertFileInfo(FileInfoCommand.from(param, filePath.toString(), originalFilename, extension));

	        log.info("파일 저장 완료: {}", savePath.toAbsolutePath());
        } catch (ApiException ae) {
        	// 확장자 검증 실패 등 의도된 4xx 예외는 그대로 전파(500 으로 묻히지 않도록)
        	throw ae;
        } catch (Exception e) {
        	log.error("파일 저장 중 오류. request={}", param, e);
        	// 프로젝트 공통 예외가 있으면 거기로 래핑해서 던져도 됨
        	throw new RuntimeException("파일 저장 실패", e);
      }
	}
}