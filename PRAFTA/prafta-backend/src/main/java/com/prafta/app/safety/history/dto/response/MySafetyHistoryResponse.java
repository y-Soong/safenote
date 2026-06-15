package com.prafta.app.safety.history.dto.response;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

/**
 * 내 안전활동 이력 조회 응답 (prafta-app-025 J1-10 B-6).
 *
 * <p>점검+위험성 본인 이력을 시간순(occurredDate DESC) 병합·슬라이스한 한 페이지.
 *    hasMore=true 면 다음 페이지(page+1)가 존재한다(무한 스크롤).
 */
@Getter
@Builder
public class MySafetyHistoryResponse {

    /** 현재 페이지 이력 행(시간순). 빈 리스트 가능(정상 200). */
    private List<MySafetyHistoryItem> items;

    /** 다음 페이지 존재 여부. */
    private boolean hasMore;
}
