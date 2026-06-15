package com.prafta.app.notice.notice02.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * 앱 자료실 생성 응답(채번된 noticeId 반환).
 */
@Getter
@Builder
public class AppArchiveSaveResponse {
    private String noticeId;
}
