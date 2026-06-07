package com.prafta.app.tbm.tbm01.dto.response;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

/**
 * prafta-app-tbm-A4: 참석자 리스트 응답.
 *
 * <p>PII 최소: 이름 + 입실시각만(연락처/부서/끝4자리 미노출).
 */
@Getter
@Builder
public class TbmAttendeeListResponse {

    private final List<Item> attendees;

    @Getter
    @Builder
    public static class Item {
        private final String userNm;
        private final String entryAt;
    }
}
