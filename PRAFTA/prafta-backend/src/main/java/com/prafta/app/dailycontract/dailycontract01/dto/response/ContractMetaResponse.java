package com.prafta.app.dailycontract.dailycontract01.dto.response;

import lombok.Builder;
import lombok.Value;

/**
 * 활성 계약서 메타 응답 (GET /appApi/dailycontract01/contract-meta — 멀티페이지 pager 초기화 T4).
 *
 * <p>앱은 이 응답의 {@code pageCount} 만큼 {@code GET contract-page?page=N} 을 호출해 페이지를 표시한다
 * (전 페이지 방문 강제 — P4). 파일코드/경로는 포함하지 않는다(스트림 EP 전용 — 경로 미노출).
 */
@Value
@Builder
public class ContractMetaResponse {
    Integer contractVer;
    String contractNm;
    String formatType;      // 'PDF' | 'IMG'
    Integer pageCount;      // 이미지 원본은 1
}
