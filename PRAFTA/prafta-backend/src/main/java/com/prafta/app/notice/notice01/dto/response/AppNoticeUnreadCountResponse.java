package com.prafta.app.notice.notice01.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * 앱 미열람 공지 카운트 응답(독립 호출용).
 * unreadCount: 대상 공지(DEL_YN='N' + 대상매칭) 중 본인 LAST_READ_DATE NULL 개수(Q2).
 */
@Getter
@Builder
public class AppNoticeUnreadCountResponse {
    private int unreadCount;
}
