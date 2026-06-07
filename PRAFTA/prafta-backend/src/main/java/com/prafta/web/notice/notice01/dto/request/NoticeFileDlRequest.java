package com.prafta.web.notice.notice01.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 공지 첨부 다운로드 토큰 발급 요청.
 */
@Getter
@Setter
@NoArgsConstructor
public class NoticeFileDlRequest {
    private String noticeId;
    private String fileMgmtCd;
}
