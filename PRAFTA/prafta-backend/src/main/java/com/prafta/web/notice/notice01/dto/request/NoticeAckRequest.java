package com.prafta.web.notice.notice01.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 공지 확인(CONFIRMED) / 한시숨김(SNOOZED) / 열람(read) 처리 요청.
 */
@Getter
@Setter
@NoArgsConstructor
public class NoticeAckRequest {
    private String noticeId;
}
