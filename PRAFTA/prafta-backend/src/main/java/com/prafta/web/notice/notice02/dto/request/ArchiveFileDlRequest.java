package com.prafta.web.notice.notice02.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 자료실 첨부 다운로드 토큰 발급 요청.
 */
@Getter
@Setter
@NoArgsConstructor
public class ArchiveFileDlRequest {
    private String noticeId;
    private String fileMgmtCd;
}
