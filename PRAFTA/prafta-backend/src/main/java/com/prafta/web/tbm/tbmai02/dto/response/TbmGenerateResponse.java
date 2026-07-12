package com.prafta.web.tbm.tbmai02.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * TBM AI 교육안 생성 응답.
 *
 * <ul>
 *   <li>{@code genContent} — 라인프로토콜 4섹션 리치HTML 렌더(작업개요/핵심 위험요인/안전수칙/오늘의 확인사항).
 *       세션 CONTENT_BODY(RICH_HTML) 대상 초안 — DB 미기록, FE 에디터가 채운 뒤 기존 세션 저장으로 영속한다.</li>
 *   <li>{@code genAt} — 생성 시각(yyyy-MM-dd HH:mm:ss).</li>
 *   <li>{@code qualityDegraded} — 관리자 교육내용 미입력으로 확정 안전정보만으로 추정 생성한 경우 true(FE 품질저하 안내용).</li>
 *   <li>{@code includedItemCount} — 통합에 반영된 CONFIRMED 확정 서술 항목 수(FE 제외항목 안내 문구용).</li>
 * </ul>
 */
@Getter
@Builder
public class TbmGenerateResponse {

    private String genContent;
    private String genAt;
    private boolean qualityDegraded;
    private int includedItemCount;
}
