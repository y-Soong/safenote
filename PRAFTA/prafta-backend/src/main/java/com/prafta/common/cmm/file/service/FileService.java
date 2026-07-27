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
	 * 계약서 멀티페이지 T1: {@link #loadPdfPageImages(FileReadQuery, int, int)} 의 DPI 지정 오버로드.
	 *
	 * <p>기존 3-arg 는 TBM AI 계약(120 DPI + 실패 시 AI_502_005 매핑)을 유지하기 위해 본 메서드에
	 *    위임하며, 열람용(150 DPI) 등 다른 DPI 가 필요한 신규 경로만 본 메서드를 직접 호출한다.
	 *
	 * @param dpi 렌더 DPI
	 * @throws com.prafta.common.exception.ApiException 확장자 위반/traversal(FILE_400_001),
	 *         암호 PDF(FILE_400_002), 렌더 실패(FILE_500_001)
	 */
	List<ImageBytesResult> loadPdfPageImages(FileReadQuery query, int pageStride, int maxPages, float dpi);

	/**
	 * 계약서 멀티페이지 T1: 저장된 PDF 의 <b>단일 페이지</b>를 PNG 로 렌더한다(1-base 페이지 번호).
	 *
	 * <p>확장자 화이트리스트(pdf 전용) + traversal 방어는 {@link #loadPdfPageImages} 와 동일하다.
	 *    대상 없음(DB 행/디스크 파일 부재) 또는 페이지 범위 밖이면 {@code null} 을 반환한다
	 *    (범위 초과의 에러코드 매핑은 도메인 계층 책임).
	 *
	 * @param pageIndex1Base 1-base 페이지 번호
	 * @throws com.prafta.common.exception.ApiException 확장자 위반/traversal(FILE_400_001),
	 *         암호 PDF(FILE_400_002), 렌더 실패(FILE_500_001)
	 */
	ImageBytesResult loadPdfPageImage(FileReadQuery query, int pageIndex1Base, float dpi);

	/**
	 * 계약서 멀티페이지 T1: 저장된 PDF 의 페이지 수를 반환한다.
	 *
	 * @return 페이지 수. 대상 없음(DB 행/디스크 파일 부재)이면 0
	 * @throws com.prafta.common.exception.ApiException 확장자 위반/traversal(FILE_400_001),
	 *         암호 PDF(FILE_400_002), 파싱 실패(FILE_500_001)
	 */
	int loadPdfPageCount(FileReadQuery query);

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
