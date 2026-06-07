package com.prafta.app.tbm.tbm01.dto.response;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

/**
 * prafta-app-tbm: 탭별 세션 리스트(A1/A2/A3) 응답.
 *
 * <p>탭별로 채워지는 item 필드가 다르다(AVAILABLE: 기본 / IN_PROGRESS: +startedAt /
 *   COMPLETED: +startedAt/endedAt/completionStatusCd). 미해당 필드는 null.
 */
@Getter
@Builder
public class TbmSessionListResponse {

    private final List<Item> sessions;

    @Getter
    @Builder
    public static class Item {
        private final String sessionCd;
        private final String title;
        private final String managerUserNm;
        private final String openedAt;
        private final String startedAt;            // A2/A3
        private final String endedAt;              // A3
        private final String completionStatusCd;   // A3 SYS053
    }
}
