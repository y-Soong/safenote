package com.prafta.common.cmm.file.service.impl;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.prafta.common.cmm.file.application.command.FileInfoCommand;
import com.prafta.common.cmm.file.application.model.FileBytesResult;
import com.prafta.common.cmm.file.application.model.FileReadInfo;
import com.prafta.common.cmm.file.application.model.ImageBytesResult;
import com.prafta.common.cmm.file.application.query.FileReadQuery;
import com.prafta.common.cmm.file.dto.param.FileInfoParam;
import com.prafta.common.cmm.file.mapper.FileMapper;
import com.prafta.common.cmm.file.service.FileService;
import com.prafta.common.cmm.file.support.PdfSupport;
import com.prafta.common.error.ai.AiErrorCode;
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
	 * 보호 파일 루트 디렉토리 (SEC-1 — 일용직 근로계약서 분리 저장).
	 * 환경변수 FILE_UPLOAD_SECURE_BASE_DIR 로 덮어쓰며, 미지정 시 {file.upload.base-dir}-secure(형제 디렉토리).
	 * 무인증 정적 서빙(/uploads/**) 마운트가 가리키는 base 밖이므로 정적 URL 로 접근 불가하다
	 * (인증 스트림 API 로만 서빙).
	 */
	@Value("${file.upload.secure-base-dir}")
	private String secureUploadBaseDir;

	/**
	 * 보호 파일타입 — 무인증 정적 서빙 제외 대상 (SEC-1).
	 * 007: 일용직계약서(계약서 원본/서명 PNG/합성본) — 성명+자필서명 포함 PII 법정 문서.
	 * 여기 포함된 타입만 secure base 에 저장되며, 그 외(001~006 등)는 기존 저장 동작 불변.
	 */
	private static final Set<String> PROTECTED_FILE_TYPES = Set.of("007");

	/** 공개 파일 FILE_PATH 선두 프리픽스(정적 서빙 마운트 경로와 동일). */
	private static final String PUBLIC_PATH_PREFIX = "uploads";

	/**
	 * 보호 파일 FILE_PATH 선두 프리픽스 — '/uploads/**' 정적 핸들러 패턴과 불일치해
	 * URL 로 해석될 수 없다. 로드 시 base 판별 키로도 사용(저장/로드 단일 결정 로직).
	 */
	private static final String SECURE_PATH_PREFIX = "uploads-secure";

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

	        // SEC-1: 보호 파일타입(007 일용직계약서)은 정적 서빙 마운트 밖 secure base 에 분리 저장.
	        //   그 외 타입은 기존 base 그대로(타 모듈 저장 동작 불변).
	        boolean protectedType = PROTECTED_FILE_TYPES.contains(param.fileType());
	        String baseDir = protectedType ? secureUploadBaseDir : uploadBaseDir;

	        // 파일 절대경로 — 디스크 저장 위치는 공통 설정(file.upload.base-dir / secure-base-dir) 기준.
	        Path absoluteFilePath = Paths.get(
	        		baseDir,
	        		param.cmpnyCd(), today,
	        		param.siteCd(), param.fileType()
			).toAbsolutePath().normalize();

	        // DB 저장용 상대경로 — 공개 파일은 '/uploads'(정적 서빙 마운트 경로, 디스크 위치와 무관) 고정,
	        // 보호 파일은 '/uploads-secure'(정적 핸들러 패턴 '/uploads/**' 과 불일치 → URL 해석 불가).
	        // 로드(resolveSavePath)는 이 선두 프리픽스로 base 를 판별하므로 저장/로드가 같은 결정 로직을 탄다.
	        Path filePath = Paths.get(
	        		protectedType ? "/" + SECURE_PATH_PREFIX : "/" + PUBLIC_PATH_PREFIX,
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

	/**
	 * 이미지 원본 read 용 확장자 → media_type 화이트리스트(소문자, 점 제외).
	 * ★svg 는 스크립트 삽입 위험으로 제외. jpg/jpeg/png/webp 만 허용.
	 */
	private static final Map<String, String> IMAGE_MEDIA_TYPES = Map.of(
			"jpg", "image/jpeg",
			"jpeg", "image/jpeg",
			"png", "image/png",
			"webp", "image/webp"
	);

	@Override
	public ImageBytesResult loadImageBytes(FileReadQuery query) {
		if (query == null || query.cmpnyCd() == null || query.fileMgmtCd() == null
				|| query.cmpnyCd().isBlank() || query.fileMgmtCd().isBlank()) {
			return null;
		}

		// fileMgmtCd 자체에 경로 구분자/traversal 이 섞이면 즉시 차단(파일명 조립 오염 방지).
		String fileMgmtCd = query.fileMgmtCd();
		if (fileMgmtCd.contains("/") || fileMgmtCd.contains("\\") || fileMgmtCd.contains("..")) {
			log.warn("파일 read 차단(fileMgmtCd 경로문자 포함) - {}", fileMgmtCd);
			throw new ApiException(FileErrorCode.FILE_400_001);
		}

		FileReadInfo info = fileMapper.selectFileInfoForRead(query);
		if (info == null || info.filePath() == null || info.fileExt() == null) {
			// DB 행 없음(대상 없음) → null(호출부에서 404 매핑).
			return null;
		}

		// 확장자 이미지 화이트리스트 검증 + media_type 매핑.
		//   ★DB FILE_EXT 는 선행 점 포함(예: ".JPG", fileSave 가 "." + ext 로 저장) →
		//   점 제거 + 소문자 정규화해야 화이트리스트 키(jpg/png/webp, 점 없음)와
		//   파일명 조립(fileMgmtCd + "." + ext)이 올바르게 맞는다.
		String ext = info.fileExt().toLowerCase();
		if (ext.startsWith(".")) {
			ext = ext.substring(1);
		}
		String mediaType = IMAGE_MEDIA_TYPES.get(ext);
		if (mediaType == null) {
			log.warn("이미지 read 차단(허용되지 않은 확장자) - fileMgmtCd={}, ext={}", fileMgmtCd, ext);
			throw new ApiException(FileErrorCode.FILE_400_001);
		}

		// 디스크 절대경로 재조립 + base-dir 컨테인먼트(traversal 최종 차단). 공용 헬퍼로 위임.
		Path savePath = resolveSavePath(info, fileMgmtCd, ext);

		try {
			java.io.File f = savePath.toFile();
			if (!f.exists() || !f.isFile()) {
				// 디스크에 파일 없음(대상 없음) → null.
				log.warn("이미지 원본 파일 없음 - {}", savePath);
				return null;
			}
			byte[] bytes = Files.readAllBytes(savePath);
			return new ImageBytesResult(bytes, mediaType);
		} catch (ApiException ae) {
			throw ae;
		} catch (Exception e) {
			log.error("이미지 원본 read 실패 - path={}, 원인={}", savePath, e.getMessage());
			return null;
		}
	}

	/**
	 * 범용 파일 read 확장자 → content_type 매핑(PRAFTA-SUBCON-T7 Q4 첨부 복제용).
	 * ALLOWED_EXTENSIONS(업로드 화이트리스트) 를 통과한 파일만 이 경로에 들어오므로 스크립트성 형식은 이미 배제된다.
	 * 표에 없는 확장자는 application/octet-stream 으로 폴백(바이트 복제는 유형 무관이라 무방).
	 */
	private static final Map<String, String> GENERIC_MEDIA_TYPES = Map.ofEntries(
			Map.entry("jpg", "image/jpeg"),
			Map.entry("jpeg", "image/jpeg"),
			Map.entry("png", "image/png"),
			Map.entry("gif", "image/gif"),
			Map.entry("bmp", "image/bmp"),
			Map.entry("webp", "image/webp"),
			Map.entry("heic", "image/heic"),
			Map.entry("heif", "image/heif"),
			Map.entry("tif", "image/tiff"),
			Map.entry("tiff", "image/tiff"),
			Map.entry("pdf", "application/pdf")
	);

	@Override
	public FileBytesResult loadFileBytes(FileReadQuery query) {
		if (query == null || query.cmpnyCd() == null || query.fileMgmtCd() == null
				|| query.cmpnyCd().isBlank() || query.fileMgmtCd().isBlank()) {
			return null;
		}

		// fileMgmtCd 자체에 경로 구분자/traversal 이 섞이면 즉시 차단(파일명 조립 오염 방지).
		String fileMgmtCd = query.fileMgmtCd();
		if (fileMgmtCd.contains("/") || fileMgmtCd.contains("\\") || fileMgmtCd.contains("..")) {
			log.warn("파일 read 차단(fileMgmtCd 경로문자 포함) - {}", fileMgmtCd);
			throw new ApiException(FileErrorCode.FILE_400_001);
		}

		FileReadInfo info = fileMapper.selectFileInfoForRead(query);
		if (info == null || info.filePath() == null || info.fileExt() == null) {
			// DB 행 없음(대상 없음) → null(호출부에서 빈 값/404 매핑).
			return null;
		}

		// 확장자 정규화(선행 점 제거 + 소문자) + 업로드 화이트리스트 재검증(스크립트성 형식 차단).
		String ext = info.fileExt().toLowerCase();
		if (ext.startsWith(".")) {
			ext = ext.substring(1);
		}
		if (!ALLOWED_EXTENSIONS.contains(ext)) {
			log.warn("파일 read 차단(허용되지 않은 확장자) - fileMgmtCd={}, ext={}", fileMgmtCd, ext);
			throw new ApiException(FileErrorCode.FILE_400_001);
		}
		String contentType = GENERIC_MEDIA_TYPES.getOrDefault(ext, "application/octet-stream");

		// 디스크 절대경로 재조립 + base-dir 컨테인먼트(traversal 최종 차단). 공용 헬퍼로 위임.
		Path savePath = resolveSavePath(info, fileMgmtCd, ext);

		try {
			java.io.File f = savePath.toFile();
			if (!f.exists() || !f.isFile()) {
				// 디스크에 파일 없음(대상 없음) → null.
				log.warn("파일 원본 없음 - {}", savePath);
				return null;
			}
			byte[] bytes = Files.readAllBytes(savePath);
			return new FileBytesResult(bytes, contentType, ext, info.fileNm());
		} catch (ApiException ae) {
			throw ae;
		} catch (Exception e) {
			log.error("파일 원본 read 실패 - path={}, 원인={}", savePath, e.getMessage());
			return null;
		}
	}

	/** PDF read 전용 확장자(소문자, 점 제외). */
	private static final String PDF_EXT = "pdf";

	/**
	 * PDF 페이지 렌더 DPI. VLM 가독성 vs 토큰/용량 균형(추후 config 승격 여지, 현재 상수).
	 */
	private static final float PDF_RENDER_DPI = 120f;

	@Override
	public List<ImageBytesResult> loadPdfPageImages(FileReadQuery query, int pageStride, int maxPages) {
		// TBM AI 계약 유지(회귀 금지): DPI=120f 고정 + PDF 처리 실패는 AI_502_005 로 매핑.
		//   렌더 결과 바이트는 4-arg 오버로드와 동일하다(같은 코드 경로).
		try {
			return loadPdfPageImages(query, pageStride, maxPages, PDF_RENDER_DPI);
		} catch (ApiException ae) {
			if (FileErrorCode.FILE_400_002.equals(ae.getErrorCode())
					|| FileErrorCode.FILE_500_001.equals(ae.getErrorCode())) {
				// 암호화/렌더 실패 → 기존 호출부(TbmAi01) 가 기대하는 AI 에러코드로 remap.
				throw new ApiException(AiErrorCode.AI_502_005);
			}
			throw ae;
		}
	}

	@Override
	public List<ImageBytesResult> loadPdfPageImages(FileReadQuery query, int pageStride, int maxPages, float dpi) {
		byte[] pdfBytes = readPdfBytesOrNull(query);
		if (pdfBytes == null) {
			// 대상 없음(DB 행/디스크 파일 부재) → null(호출부에서 404 매핑).
			return null;
		}
		return PdfSupport.renderPagesToPng(pdfBytes, pageStride, maxPages, dpi).stream()
				.map(png -> new ImageBytesResult(png, "image/png"))
				.toList();
	}

	@Override
	public ImageBytesResult loadPdfPageImage(FileReadQuery query, int pageIndex1Base, float dpi) {
		byte[] pdfBytes = readPdfBytesOrNull(query);
		if (pdfBytes == null) {
			return null;
		}
		// 1-base → 0-base. 범위 밖이면 PdfSupport 가 null 을 반환한다(도메인이 400_007 등으로 매핑).
		byte[] png = PdfSupport.renderPageToPng(pdfBytes, pageIndex1Base - 1, dpi);
		if (png == null) {
			return null;
		}
		return new ImageBytesResult(png, "image/png");
	}

	@Override
	public int loadPdfPageCount(FileReadQuery query) {
		byte[] pdfBytes = readPdfBytesOrNull(query);
		if (pdfBytes == null) {
			return 0;
		}
		return PdfSupport.readPageCount(pdfBytes);
	}

	/**
	 * PDF 원본 바이트 로드 — 확장자 화이트리스트(pdf 전용) + base-dir 컨테인먼트(traversal 방어) 후 read.
	 *
	 * <p>기존 {@code loadPdfPageImages} 의 전처리(파라미터/fileMgmtCd 경로문자/확장자/파일 존재 검증)를
	 *    그대로 옮긴 공용 헬퍼다. 대상 없음(DB 행 부재/디스크 파일 부재)은 {@code null} 을 반환한다.
	 *
	 * @throws ApiException fileMgmtCd 경로문자·비 PDF 확장자·traversal(FILE_400_001), read 실패(FILE_500_001)
	 */
	private byte[] readPdfBytesOrNull(FileReadQuery query) {
		if (query == null || query.cmpnyCd() == null || query.fileMgmtCd() == null
				|| query.cmpnyCd().isBlank() || query.fileMgmtCd().isBlank()) {
			return null;
		}

		// fileMgmtCd 자체에 경로 구분자/traversal 이 섞이면 즉시 차단(파일명 조립 오염 방지).
		String fileMgmtCd = query.fileMgmtCd();
		if (fileMgmtCd.contains("/") || fileMgmtCd.contains("\\") || fileMgmtCd.contains("..")) {
			log.warn("PDF read 차단(fileMgmtCd 경로문자 포함) - {}", fileMgmtCd);
			throw new ApiException(FileErrorCode.FILE_400_001);
		}

		FileReadInfo info = fileMapper.selectFileInfoForRead(query);
		if (info == null || info.filePath() == null || info.fileExt() == null) {
			// DB 행 없음(대상 없음) → null(호출부에서 404 매핑).
			return null;
		}

		// 확장자 화이트리스트(pdf 전용). DB FILE_EXT 는 선행 점 포함 가능(".PDF") → 점 제거+소문자화.
		String ext = info.fileExt().toLowerCase();
		if (ext.startsWith(".")) {
			ext = ext.substring(1);
		}
		if (!PDF_EXT.equals(ext)) {
			log.warn("PDF read 차단(허용되지 않은 확장자) - fileMgmtCd={}, ext={}", fileMgmtCd, ext);
			throw new ApiException(FileErrorCode.FILE_400_001);
		}

		// 디스크 절대경로 재조립 + base-dir 컨테인먼트(traversal 최종 차단). 공용 헬퍼로 위임.
		Path savePath = resolveSavePath(info, fileMgmtCd, ext);

		java.io.File f = savePath.toFile();
		if (!f.exists() || !f.isFile()) {
			// 디스크에 파일 없음(대상 없음) → null.
			log.warn("PDF 원본 파일 없음 - {}", savePath);
			return null;
		}

		try {
			return Files.readAllBytes(savePath);
		} catch (Exception e) {
			log.error("PDF 원본 read 실패 - path={}, 원인={}", savePath, e.getMessage());
			throw new ApiException(FileErrorCode.FILE_500_001);
		}
	}

	/**
	 * DB FILE_PATH(공개 상대경로) + fileMgmtCd + 확장자로 디스크 절대경로를 재조립하고,
	 * base-dir 하위 컨테인먼트를 강제한다(path traversal 최종 차단).
	 *
	 * <p>{@code loadImageBytes}/{@code loadPdfPageImages} 공용 헬퍼. FILE_PATH 의 '/uploads'
	 *    프리픽스는 정적 서빙 마운트 경로이므로 디스크 경로에서 제외한다(fileSave 저장규칙 역산).
	 *    FILE_PATH 는 저장 OS 에 따라 '/' 또는 '\\' 구분자를 가질 수 있어 둘 다 처리한다.
	 *
	 * @param ext 점 제외 소문자 확장자(예: "png", "pdf")
	 * @throws ApiException traversal 세그먼트/ base-dir 이탈 시(FILE_400_001)
	 */
	private Path resolveSavePath(FileReadInfo info, String fileMgmtCd, String ext) {
		// SEC-1: FILE_PATH 선두 프리픽스로 저장 base 판별 — '/uploads-secure'(보호 파일)는 secure base,
		//   '/uploads'(공개 파일)는 기존 base. fileSave 의 저장 결정 로직과 동일(단일 출처 = DB FILE_PATH).
		String baseDir = isSecureFilePath(info.filePath()) ? secureUploadBaseDir : uploadBaseDir;
		Path baseCanon = Paths.get(baseDir).toAbsolutePath().normalize();
		Path dir = baseCanon;
		for (String seg : info.filePath().split("[/\\\\]")) {
			if (seg == null || seg.isBlank()) {
				continue;
			}
			if (PUBLIC_PATH_PREFIX.equalsIgnoreCase(seg) || SECURE_PATH_PREFIX.equalsIgnoreCase(seg)) {
				continue;   // 마운트/보호 프리픽스 — 디스크 경로에서는 제외
			}
			if (".".equals(seg) || "..".equals(seg)) {
				log.warn("파일 read 차단(경로 traversal 세그먼트) - filePath={}", info.filePath());
				throw new ApiException(FileErrorCode.FILE_400_001);
			}
			dir = dir.resolve(seg);
		}
		Path savePath = dir.resolve(fileMgmtCd + "." + ext).normalize();

		// ★base-dir 하위 강제(canonicalize 후 컨테인먼트) — path traversal 최종 차단.
		if (!savePath.startsWith(baseCanon)) {
			log.warn("파일 read 차단(base-dir 이탈) - savePath={}, baseDir={}", savePath, baseCanon);
			throw new ApiException(FileErrorCode.FILE_400_001);
		}
		return savePath;
	}

	/**
	 * FILE_PATH 의 첫 유효 세그먼트가 보호 파일 프리픽스('/uploads-secure')인지 판별한다 (SEC-1).
	 *
	 * <p>FILE_PATH 는 저장 OS 에 따라 '/' 또는 '\\' 구분자를 가질 수 있어 둘 다 처리한다.
	 */
	private boolean isSecureFilePath(String filePath) {
		for (String seg : filePath.split("[/\\\\]")) {
			if (seg == null || seg.isBlank()) {
				continue;
			}
			return SECURE_PATH_PREFIX.equalsIgnoreCase(seg);
		}
		return false;
	}
}