package com.prafta.app.notice.notice01.dto.request;

import lombok.Getter;
import lombok.Setter;

/**
 * 앱 공지 상세 조회 요청(GET 쿼리). noticeId 만 수신, 식별값은 세션(JWT)에서 도출.
 */
@Getter
@Setter
public class AppNoticeInfoRequest {
    private String noticeId;
}
