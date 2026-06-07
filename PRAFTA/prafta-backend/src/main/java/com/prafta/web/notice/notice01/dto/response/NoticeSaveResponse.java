package com.prafta.web.notice.notice01.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * 공지 생성 응답(채번된 noticeId 반환).
 */
@Getter
@Builder
public class NoticeSaveResponse {
    private String noticeId;
}
