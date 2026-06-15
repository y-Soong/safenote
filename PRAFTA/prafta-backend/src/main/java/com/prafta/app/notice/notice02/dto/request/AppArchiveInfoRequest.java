package com.prafta.app.notice.notice02.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 앱 자료실 단건 상세 조회 요청. cmpnyCd 는 JWT 에서만 도출(IDOR 차단).
 */
@Getter
@Setter
@NoArgsConstructor
public class AppArchiveInfoRequest {
    private String noticeId;
}
