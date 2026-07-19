package com.prafta.common.cmm.file.service;

import java.util.List;

import com.prafta.common.cmm.file.application.model.FileBytesResult;
import com.prafta.common.cmm.file.application.model.ImageBytesResult;
import com.prafta.common.cmm.file.application.query.FileReadQuery;
import com.prafta.common.cmm.file.dto.param.FileInfoParam;

public interface FileService {
//	void saveFile(FileInfoCommand request, MultipartFile file);

	void fileSave(FileInfoParam param);

	/**
	 * PRAFTA-WEB_003: 저장된 이미지 원본을 서버측에서 바이트로 로드한다(Claude vision 전송용).
	 *
	 * <p>확장자 이미지 화이트리스트(jpg/jpeg/png/webp; svg 금지) 검증 + 경로 traversal 방어(base-dir 하위 강제)를
	 *    수행한다. DB 행이 없거나 디스크에 파일이 없으면 {@code null} 을 반환한다(호출부가 404 로 매핑).
	 *
	 * @throws com.prafta.common.exception.ApiException 확장자 위반/traversal 등 보안 위반 시(FILE_400_001)
	 */
	ImageBytesResult loadImageBytes(FileReadQuery query);

	/**
	 * TBM_AI: 저장된 PDF 원본을 페이지 이미지(PNG)로 렌더링해 바이트 목록으로 로드한다(VLM 전송용).
	 *
	 * <p>확장자 화이트리스트(pdf 전용) + 경로 traversal 방어(base-dir 하위 강제)를 수행한다.
	 *    페이지는 {@code pageStride} 간격으로 성기게 샘플하며 최대 {@code maxPages}장까지만 렌더한다.
	 *    DB 행이 없거나 디스크에 파일이 없으면 {@code null} 을 반환한다(호출부가 AI_404_002 로 매핑).
	 *
	 * @param query      회사코드+파일코드 스코프(cmpnyCd 는 호출부 JWT 도출)
	 * @param pageStride 페이지 샘플링 간격(≥1, AiProperties.Tbm.pdfPageStride)
	 * @param maxPages   렌더 페이지 상한(≥1, AiProperties.Tbm.pdfMaxPages)
	 * @return 렌더 페이지 PNG 바이트 목록(mediaType="image/png"), 대상 없음이면 null
	 * @throws com.prafta.common.exception.ApiException 확장자 위반/traversal(FILE_400_001), PDF 렌더 실패(AI_502_005)
	 */
	List<ImageBytesResult> loadPdfPageImages(FileReadQuery query, int pageStride, int maxPages);

	/**
	 * PRAFTA-SUBCON-T7(Q4): 저장된 파일 원본을 <b>유형 무관</b>으로 바이트로 로드한다(cross-tenant 첨부 복제용).
	 *
	 * <p>{@link #loadImageBytes} 는 이미지 화이트리스트 전용이라 PDF 등을 조용히 누락시킨다. 위험성평가/아차사고
	 *    첨부는 확장자 종류와 무관하게 전부 복제해야 하므로, 업로드 허용 확장자 화이트리스트(스크립트성 형식 제외)를
	 *    통과한 파일이면 확장자를 보존해 바이트로 읽는다. base-dir traversal 방어(하위 강제)는 동일하게 수행한다.
	 *
	 * <p>DB 행이 없거나 디스크에 파일이 없으면 {@code null} 을 반환한다(호출부가 빈 값/404 로 매핑).
	 *
	 * @throws com.prafta.common.exception.ApiException 확장자 위반/traversal 등 보안 위반 시(FILE_400_001)
	 */
	FileBytesResult loadFileBytes(FileReadQuery query);
}
