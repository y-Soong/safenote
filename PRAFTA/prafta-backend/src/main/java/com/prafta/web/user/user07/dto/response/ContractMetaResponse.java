package com.prafta.web.user.user07.dto.response;

import lombok.Builder;
import lombok.Value;

/**
 * 계약서 형식/페이지 수 응답 (GET /webApi/user07/contract-meta — 멀티페이지 지원 T4).
 *
 * <p>활성 계약서 카드의 "PDF · N페이지" 표시 전용이다. 목록(contract-lists)은 FILE_EXT 조인으로
 * 형식만 내려 주고(PDF 파싱 0회 — plan §4 D-P11), 페이지 수는 활성 1건만 본 EP 로 조회한다.
 *
 * <p>파일코드/경로는 포함하지 않는다(미리보기는 contract-image 스트림 전용).
 */
@Value
@Builder
public class ContractMetaResponse {
    String formatType;      // 'PDF' | 'IMG'
    Integer pageCount;      // 이미지 양식은 1
}
