package com.prafta.app.notice.notice01.dto.request;

import lombok.Getter;
import lombok.Setter;

/**
 * 앱 공지 첨부 다운로드 토큰 발급 요청(GET 쿼리). noticeId + fileMgmtCd 만 수신, 식별값은 세션(JWT)에서 도출.
 */
@Getter
@Setter
public class AppNoticeFileDlRequest {
    private String noticeId;
    private String fileMgmtCd;
}
