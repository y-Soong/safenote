package com.prafta.app.notice.notice01.dto.request;

import lombok.Getter;
import lombok.Setter;

/**
 * 앱 공지 확인/한시숨김/열람 요청(POST 바디). noticeId 만 수신, 식별값은 세션(JWT)에서 도출(IDOR 차단).
 */
@Getter
@Setter
public class AppNoticeAckRequest {
    private String noticeId;
}
